package net.exoego.intellij.digdag.psi

import com.intellij.psi.NavigatablePsiElement

interface DigdagPsiElement : NavigatablePsiElement{
    fun getDigdagElements(): List<DigdagPsiElement> {
        val result = ArrayList<DigdagPsiElement>()
        for (node in node.getChildren(null)) {
            val psi = node.psi
            if (psi is DigdagPsiElement) {
                result.add(psi)
            }
        }
        return result
    }
}