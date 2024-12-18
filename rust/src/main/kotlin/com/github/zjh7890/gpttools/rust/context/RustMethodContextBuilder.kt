package com.github.zjh7890.gpttools.rust.context

import com.github.zjh7890.gpttools.context.MethodContext
import com.github.zjh7890.gpttools.context.builder.MethodContextBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.rust.ide.presentation.presentationInfo
import org.rust.lang.core.psi.*

class RustMethodContextBuilder : MethodContextBuilder {
    override fun getMethodContext(
        psiElement: PsiElement,
        includeClassContext: Boolean,
        gatherUsages: Boolean
    ): MethodContext? {
        if (psiElement !is RsFunction) return null

        val text = psiElement.text
        val returnType = psiElement.retType?.text ?: ""
        val language = psiElement.language.displayName

        val signature = psiElement.presentationInfo?.signatureText
        val paramsName = psiElement.valueParameterList?.valueParameterList?.map {
            it.text
        } ?: emptyList()

        val enclosingClass = PsiTreeUtil.getParentOfType(psiElement, RsImplItem::class.java)

        return MethodContext(
            psiElement,
            text,
            psiElement.name,
            signature.toString(),
            enclosingClass,
            language,
            returnType,
            paramsName,
            includeClassContext,
            emptyList()
        )
    }
}
