package com.ykoellmann.sqlfromparm

import com.intellij.internal.statistic.beans.newMetric
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.util.NlsContexts
import com.intellij.xdebugger.frame.XFullValueEvaluator
import com.intellij.xdebugger.frame.XValueCallback
import com.intellij.xdebugger.frame.XValueNode
import com.intellij.xdebugger.frame.XValuePlace
import com.intellij.xdebugger.frame.presentation.XNumericValuePresentation
import com.intellij.xdebugger.frame.presentation.XRegularValuePresentation
import com.intellij.xdebugger.frame.presentation.XStringValuePresentation
import com.intellij.xdebugger.frame.presentation.XValuePresentation
import com.intellij.xdebugger.impl.frame.XDebugView
import com.intellij.xdebugger.impl.frame.XVariablesView
import com.intellij.xdebugger.impl.ui.tree.actions.XDebuggerTreeActionBase
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodeImpl
import com.jetbrains.rider.debugger.DotNetValue
import java.awt.Font
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/**
 * IntelliJ action that extracts SQL and parameters from the debugger tree and
 * copies the final, parameter-filled SQL statement to the clipboard.
 */
class ParmEvalAction : AnAction("Evaluate Parm Variable") {
    private val logger = Logger.getInstance(ParmEvalAction::class.java)

    override fun actionPerformed(e: AnActionEvent) {
        val nodes = XDebuggerTreeActionBase.getSelectedNodes(e.dataContext)

        // Execute evaluation in background thread
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val builder = ParmSqlBuilder()

                // Iterate selected debugger nodes and classify them
                nodes.filterIsInstance<XValueNodeImpl>().forEach { node ->
                    when (builder.classifyNode(node)) {
                        NodeType.SqlText -> {
                            builder.sql = builder.fullValue(node)
                        }
                        else -> builder.collectParams(node)
                    }
                }

                // Build the final SQL and copy it to the clipboard
                val resultSql = builder.buildSql()
                copyToClipboard(resultSql)
            } catch (ex: Exception) {
                logger.error("Error evaluating parameters", ex)
            }
        }
    }


    /**
     * Copies the given text to the system clipboard.
     */
    private fun copyToClipboard(text: String) {
        Toolkit.getDefaultToolkit().systemClipboard
            .setContents(StringSelection(text), null)
    }

    class CapturingTextRenderer : XValuePresentation.XValueTextRenderer {
        val parts = StringBuilder()

        override fun renderValue(value: String) {
            parts.append(value)
        }

        override fun renderStringValue(value: String) {
            parts.append(value)
        }

        override fun renderNumericValue(value: String) {
            parts.append(value)
        }

        override fun renderKeywordValue(value: String) {
            parts.append(value)
        }

        override fun renderValue(value: String, attributes: TextAttributesKey) {
            parts.append(value)
        }

        override fun renderStringValue(value: String, additional: String?, maxLength: Int) {
            parts.append(value)
        }

        override fun renderComment(comment: String) {
            parts.append(" /* ").append(comment).append(" */ ")
        }

        override fun renderSpecialSymbol(symbol: String) {
            parts.append(symbol)
        }

        override fun renderError(error: String) {
            parts.append("[ERROR: ").append(error).append("]")
        }

        fun getResult(): String = parts.toString()
    }
}
