package net.exoego.intellij.digdag.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.PsiTreeUtil
import net.exoego.intellij.digdag.DigdagUtil
import net.exoego.intellij.digdag.psi.DigdagKeyValue
import net.exoego.intellij.digdag.psi.DigdagMapping
import net.exoego.intellij.digdag.psi.DigdagPsiElementVisitor

abstract class DigdagMappingImpl(node: ASTNode): DigdagCompoundValueImpl(node), DigdagMapping {
    override fun getKeyValues(): Collection<DigdagKeyValue> {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, DigdagKeyValue::class.java)
    }

    override fun getKeyValueByKey(keyText: String): DigdagKeyValue? {
        for (keyValue in getKeyValues()) {
            if (keyText == keyValue.getKeyText()) {
                return keyValue
            }
        }
        return null
    }

    override fun putKeyValue(keyValueToAdd: DigdagKeyValue) {
        val existingKey: DigdagKeyValue? = getKeyValueByKey(keyValueToAdd.getKeyText())
        if (existingKey == null) {
            addNewKey(keyValueToAdd)
        } else {
            existingKey.replace(keyValueToAdd)
        }
    }

    override fun deleteKeyValue(keyValueToDelete: DigdagKeyValue) {
        require(keyValueToDelete.getParent() === this) { "KeyValue should be the child of this" }

        DigdagUtil.deleteSurroundingWhitespace(keyValueToDelete)

        keyValueToDelete.delete()
    }

    protected abstract fun addNewKey(key: DigdagKeyValue)

    override fun toString(): String {
        return "Digdag mapping"
    }

    override fun getTextValue(): String {
        return "<mapping:" + Integer.toHexString(text.hashCode()) + ">"
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DigdagPsiElementVisitor) {
            visitor.visitMapping(this)
        } else {
            super.accept(visitor)
        }
    }
}
