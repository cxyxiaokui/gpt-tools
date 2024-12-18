package com.github.zjh7890.gpttools.toolWindow.treePanel

import com.github.zjh7890.gpttools.utils.ClipboardUtils
import com.github.zjh7890.gpttools.utils.FileUtil
import com.github.zjh7890.gpttools.utils.PsiUtils.getDependencies
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.treeStructure.Tree
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class FileTreeListPanel(private val project: Project) : JPanel() {
    private val root = DefaultMutableTreeNode("Files")
    val tree = Tree(root)
    private val addedFiles = mutableSetOf<VirtualFile>()

    init {
        layout = java.awt.BorderLayout()
        add(JScrollPane(tree), java.awt.BorderLayout.CENTER)

        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val node = tree.selectionPath?.lastPathComponent as? DefaultMutableTreeNode
                    val fileName = node?.userObject as? String
                    val virtualFile = addedFiles.find { it.name == fileName }
                    if (virtualFile != null) {
                        FileEditorManager.getInstance(project).openFile(virtualFile, true)
                    }
                    e.consume()
                }
            }
        })
    }

    fun addFile(file: VirtualFile) {
        if (addedFiles.add(file)) {
            val expandedPaths = getExpandedPaths()
            val node = DefaultMutableTreeNode(file.name)
            root.add(node)
            (tree.model as DefaultTreeModel).reload(root)
            restoreExpandedPaths(expandedPaths)
        }
    }

    fun addFileRecursively(file: VirtualFile, project: Project) {
        val expandedPaths = getExpandedPaths()
        val rootNode = DefaultMutableTreeNode(file.name)
        val queue = LinkedList<Pair<VirtualFile, DefaultMutableTreeNode>>()
        queue.add(file to rootNode)
        addedFiles.add(file)

        ApplicationManager.getApplication().runReadAction {
            while (queue.isNotEmpty()) {
                val (currentFile, currentNode) = queue.poll()
                val dependencies = getDependencies(currentFile, project)
                dependencies.filter { it.isValid }.forEach { dependency ->
                    if (addedFiles.add(dependency)) {
                        val childNode = DefaultMutableTreeNode(dependency.name)
                        currentNode.add(childNode)
                        queue.add(dependency to childNode)
                    }
                }
            }
        }

        root.add(rootNode)
        (tree.model as DefaultTreeModel).reload(root)
        restoreExpandedPaths(expandedPaths)
    }

    private fun getExpandedPaths(): Set<TreePath> {
        val expandedPaths = mutableSetOf<TreePath>()
        ApplicationManager.getApplication().invokeAndWait {
            for (i in 0 until tree.rowCount) {
                val path = tree.getPathForRow(i)
                if (tree.isExpanded(path)) {
                    expandedPaths.add(path)
                }
            }
        }
        return expandedPaths
    }

    private fun restoreExpandedPaths(expandedPaths: Set<TreePath>) {
        for (path in expandedPaths) {
            tree.expandPath(path)
        }
    }

    fun removeSelectedNodes() {
        val expandedPaths = getExpandedPaths()
        val selectedPaths = tree.selectionPaths
        if (selectedPaths != null) {
            val model = tree.model as DefaultTreeModel
            selectedPaths.forEach { path ->
                val selectedNode = path.lastPathComponent as? DefaultMutableTreeNode
                if (selectedNode != null && selectedNode != root) { // 防止移除根节点
                    removeNodeAndChildren(selectedNode)
                }
            }
            model.reload(root)
            restoreExpandedPaths(expandedPaths) // 恢复展开路径
        }
    }

    private fun removeNodeAndChildren(node: DefaultMutableTreeNode) {
        // Create a queue to hold nodes to be removed
        val nodesToRemove = LinkedList<DefaultMutableTreeNode>()
        nodesToRemove.add(node)

        while (nodesToRemove.isNotEmpty()) {
            val currentNode = nodesToRemove.poll()
            // Add all children to the queue
            for (i in 0 until currentNode.childCount) {
                nodesToRemove.add(currentNode.getChildAt(i) as DefaultMutableTreeNode)
            }
            // Remove the current node's file reference if it exists
            addedFiles.removeIf { it.name == currentNode.userObject as String }
            // Remove the current node from its parent
            currentNode.removeFromParent()
        }
    }

    fun copyAllFiles(project: Project) {
        val files = addedFiles.map { FileUtil.readFileInfoForLLM(it, project) }.joinToString ("\n\n")

        val sb: StringBuilder = StringBuilder()
        sb.append("下面是提供的信息：\n" + FileUtil.wrapBorder(files))
        ClipboardUtils.copyToClipboard(sb.toString())
    }
}