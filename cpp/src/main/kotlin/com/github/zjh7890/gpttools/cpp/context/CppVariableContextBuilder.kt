package com.github.zjh7890.gpttools.cpp.context

import com.github.zjh7890.gpttools.context.VariableContext
import com.github.zjh7890.gpttools.context.builder.VariableContextBuilder
import com.intellij.psi.PsiElement
import com.jetbrains.cidr.lang.psi.*
import com.intellij.psi.util.PsiTreeUtil

class CppVariableContextBuilder : VariableContextBuilder {
    override fun getVariableContext(
        psiElement: PsiElement,
        withMethodContext: Boolean,
        withClassContext: Boolean,
        gatherUsages: Boolean
    ): VariableContext? {
        if (psiElement !is OCDeclaration) return null

        val declarators = psiElement.declarators
        val symbols = declarators.filterIsInstance<OCDeclarator>()
            .mapNotNull { it.symbol }
            .filter { it.kind.isVariable }

        val variableContexts = symbols.map { declarator ->
            val text = psiElement.text
            val enclosingMethod = PsiTreeUtil.getParentOfType(psiElement, OCFunctionDeclaration::class.java, true)
            val enclosingClass = PsiTreeUtil.getParentOfType(psiElement, OCStructLike::class.java, true)

            VariableContext(
                psiElement,
                text,
                declarator.name,
                enclosingMethod,
                enclosingClass,
                emptyList(),
                withMethodContext,
                withClassContext
            )
        }

        return variableContexts.firstOrNull()
    }
}
