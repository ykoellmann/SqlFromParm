package com.ykoellmann.sqlfromparm

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.xdebugger.impl.ui.tree.actions.XDebuggerTreeActionBase
import com.intellij.xdebugger.impl.ui.tree.nodes.XValueNodeImpl
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
                        NodeType.SqlText -> builder.sql = node.rawValue?.toString().orEmpty()
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
}
