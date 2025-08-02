package com.ykoellmann.parmfiller

import com.intellij.openapi.diagnostic.Logger
import com.intellij.xdebugger.frame.XFullValueEvaluator
import com.intellij.xdebugger.impl.ui.tree.nodes.MessageTreeNode
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodeImpl
import java.awt.Font
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Builds a parameterized SQL statement by extracting parameter values from debugger nodes.
 */
class ParmSqlBuilder {
    var sql: String = ""
    private val params = mutableMapOf<String, String>()

    /**
     * Recursively collects key-value pairs from debugger nodes representing parameters.
     */
    fun collectParams(node: XValueNodeImpl) {
        if (!waitForChildren(node)) return

        val children = try {
            node.children.filterIsInstance<XValueNodeImpl>()
        } catch (e: Exception) {
            Logger.getInstance(ParmEvalAction::class.java)
                .warn("Failed to access children of ${node.name}", e)
            return
        }

        var key: String? = null
        var value: String? = null

        // Look for "Key" and "Value" child nodes, or recurse deeper
        children.forEach { child ->
            val fullValue = getNodeFullValue(child)
            when (child.name) {
                "Key" -> key = fullValue
                "Value" -> value = formatValue(child.valuePresentation!!.type!!, fullValue)
                else -> collectParams(child)
            }
        }

        // Store parameter if both key and value were found
        if (key != null && value != null) {
            params[key!!] = value!!
        }
    }

    fun formatValue(type: String, raw: String): String {
        val lowerType = type.lowercase()
        return when {
            lowerType in listOf("int", "long", "short", "decimal", "double", "float") -> raw
            lowerType == "bool" -> if (raw == "true") "1" else "0"
            lowerType == "string" || lowerType == "char" -> "'${raw.replace("'", "''")}'"
            lowerType == "datetime" || lowerType == "datetimeoffset" -> "'${tryFormatDateTime(raw)}'"
            lowerType == "guid" -> "'$raw'"
            raw == "null" || raw == "DBNull" -> "NULL"
            else -> raw // fallback
        }
    }

    fun getNodeFullValue(node: XValueNodeImpl): String {
        val latch = java.util.concurrent.CountDownLatch(1)
        var result: String? = null

        node.fullValueEvaluator!!.startEvaluation(object : XFullValueEvaluator.XFullValueEvaluationCallback {
            override fun evaluated(value: String, font: Font?) {
                result = value
                latch.countDown()
            }

            override fun errorOccurred(errorMessage: String) {
                result = "ERROR: $errorMessage"
                latch.countDown()
            }
        })

        latch.await() // Blockiert bis evaluated() oder errorOccurred() aufgerufen wurde
        return result ?: ""
    }

    /**
     * Replaces all parameter keys in the original SQL string with their corresponding values.
     */
    fun buildSql(): String = params.entries.fold(sql) { acc, (k, v) -> acc.replace(k, v) }

    /**
     * Classifies a debugger node based on its type or name to detect SQL text or parameter container.
     */
    fun classifyNode(node: XValueNodeImpl): NodeType {
        val typeText = node.valuePresentation?.type?.lowercase().orEmpty()
        val nameText = node.name?.lowercase().orEmpty()
        val rawText = node.rawValue?.lowercase().orEmpty()

        return when {
            "parmbuilder" in typeText || "parmbuilder" in rawText -> NodeType.ParmBuilder
            "string" in typeText && ("sql" in nameText || "select" in rawText) -> NodeType.SqlText
            else -> NodeType.Unknown
        }
    }

    /**
     * Waits until the children of a node are loaded (used to avoid "Collecting data" placeholders).
     */
    private fun waitForChildren(
        node: XValueNodeImpl,
        timeoutMs: Long = 2000,
        pollIntervalMs: Long = 100
    ): Boolean {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMs) {
            val loading = node.children.any { it is MessageTreeNode && it.text.toString().contains("Collecting data") }
            if (!loading) return true
            Thread.sleep(pollIntervalMs)
        }
        return false
    }

    /**
     * Trys to convert raw value to formatted dateTime
     */
    private fun tryFormatDateTime(raw: String?): String? {
        if (raw.isNullOrBlank()) return null

        val inputFormats = listOf(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME, // e.g. "2024-06-30T14:45:12.123"
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        )

        for (format in inputFormats) {
            try {
                val parsed = LocalDateTime.parse(raw, format)
                return parsed.format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH.mm.ss.SSSSSS"))
            } catch (e: DateTimeParseException) {
                continue
            }
        }

        return null // fallback: not a parsable date
    }
}