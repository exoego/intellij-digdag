package net.exoego.intellij.digdag.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.ItemPresentationProviders
import com.intellij.openapi.util.Iconable
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.StringUtil
import com.intellij.pom.PsiDeclaredTarget
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiReference
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.IncorrectOperationException
import com.intellij.util.ObjectUtils
import com.intellij.util.PlatformIcons
import javax.swing.Icon
import net.exoego.intellij.digdag.DigdagElementGenerator
import net.exoego.intellij.digdag.DigdagElementTypes
import net.exoego.intellij.digdag.DigdagTokenTypes
import net.exoego.intellij.digdag.DigdagUtil
import net.exoego.intellij.digdag.psi.DigdagCompoundValue
import net.exoego.intellij.digdag.psi.DigdagFile
import net.exoego.intellij.digdag.psi.DigdagKeyValue
import net.exoego.intellij.digdag.psi.DigdagMapping
import net.exoego.intellij.digdag.psi.DigdagPsiElementVisitor
import net.exoego.intellij.digdag.psi.DigdagScalar
import net.exoego.intellij.digdag.psi.DigdagValue
import org.jetbrains.annotations.NonNls

class DigdagKeyValueImpl(node: ASTNode):  DigdagPsiElementImpl(node) , DigdagKeyValue, PsiDeclaredTarget {
    companion object {
        val DIGDAG_KEY_ICON: Icon = PlatformIcons.PROPERTY_ICON
    }

    override fun toString(): String {
        return "Digdag key value"
    }

    override fun getKey(): PsiElement? {
        val colon: PsiElement = findChildByType(DigdagTokenTypes.COLON) ?: return null
        var node = colon.node
        do {
            node = node!!.treePrev
        } while (DigdagElementTypes.BLANK_ELEMENTS.contains(PsiUtilCore.getElementType(node)))

        return if (node == null || PsiUtilCore.getElementType(node) === DigdagTokenTypes.QUESTION) {
            null
        } else {
            node.psi
        }
    }

    override fun getParentMapping(): DigdagMapping? = ObjectUtils.tryCast(super.getParent(), DigdagMapping::class.java)

    override fun getName(): String = getKeyText()

    override fun getKeyText(): String {
        val keyElement = getKey() ?: return ""

        if (keyElement is DigdagScalar) {
            return keyElement.getTextValue()
        }
        if (keyElement is DigdagCompoundValue) {
            return keyElement.getTextValue()
        }

        val text = keyElement.text
        return StringUtil.unquoteString(text)
    }

    override fun getValue(): DigdagValue? {
        var child = lastChild
        while (child != null) {
            if (PsiUtilCore.getElementType(child) === DigdagTokenTypes.COLON) {
                return null
            }
            if (child is DigdagValue) {
                return child
            }
            child = child.prevSibling
        }
        return null
    }

    override fun getValueText(): String {
        val value: DigdagValue? = getValue()
        if (value is DigdagScalar) {
            return value.getTextValue()
        } else if (value is DigdagCompoundValue) {
            return value.getTextValue()
        }
        return ""
    }


    override fun setValue(value: DigdagValue) {
        adjustWhitespaceToContentType(value is DigdagScalar)

        val value1 = getValue()
        if (value1 != null) {
            value1.replace(value)
            return
        }

        val generator: DigdagElementGenerator = DigdagElementGenerator.getInstance(project)
        if (isExplicit()) {
            if (findChildByType<PsiElement>(DigdagTokenTypes.COLON) == null) {
                add(generator.createColon())
                add(generator.createSpace())
                add(value)
            }
        } else {
            add(value)
        }
    }

    private fun adjustWhitespaceToContentType(isScalar: Boolean) {
        val colon: PsiElement = checkNotNull(findChildByType(DigdagTokenTypes.COLON))
        while (colon.nextSibling != null && colon.nextSibling !is DigdagValue) {
            colon.nextSibling.delete()
        }
        val generator: DigdagElementGenerator = DigdagElementGenerator.getInstance(project)
        if (isScalar) {
            addAfter(generator.createSpace(), colon)
        } else {
            val indent: Int = DigdagUtil.getIndentToThisElement(this)
            addAfter(generator.createIndent(indent + 2), colon)
            addAfter(generator.createEol(), colon)
        }
    }

    override fun getElementIcon(@Iconable.IconFlags flags: Int): Icon = DIGDAG_KEY_ICON

    override fun getPresentation(): ItemPresentation {
        val custom = ItemPresentationProviders.getItemPresentation(this)
        if (custom != null) {
            return custom
        }
        val digdagFile: DigdagFile = containingFile as DigdagFile
        val value: PsiElement? = getValue()
        return object : ItemPresentation {
            override fun getPresentableText(): String? {
                if (value is DigdagScalar) {
                    val presentation = value.getPresentation()
                    return if (presentation != null) presentation.presentableText else getValueText()
                }
                return name
            }

            override fun getLocationString(): String = digdagFile.getName()

            override fun getIcon(open: Boolean): Icon? = this@DigdagKeyValueImpl.getIcon(0)
        }
    }

    @Throws(IncorrectOperationException::class)
    override fun setName(@NonNls newName: String): PsiElement {
        return DigdagUtil.rename(this, newName)
    }

    override fun getReferences(): Array<PsiReference> {
        return ReferenceProvidersRegistry.getReferencesFromProviders(this)
    }

    private fun isExplicit(): Boolean {
        val child = node.firstChildNode
        return child != null && child.elementType === DigdagTokenTypes.QUESTION
    }

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is DigdagPsiElementVisitor) {
            visitor.visitKeyValue(this)
        } else {
            super.accept(visitor)
        }
    }

    override fun getNameIdentifierRange(): TextRange? {
        val key = getKey()
        return key?.textRangeInParent
    }
}