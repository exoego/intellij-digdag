package net.exoego.intellij.digdag

import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

object DigdagFileType : LanguageFileType(DigdagLanguage) {
    override fun getName(): String = "Digdag"

    override fun getDescription(): String = "Digdag files"

    override fun getDefaultExtension(): String = "dig"

    override fun getIcon(): Icon = DigdagIcons.FILE

    override fun getCharset(file: VirtualFile, content: ByteArray) = "UTF-8"

}

