package net.exoego.intellij.digdag

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object DigdagIcons {
    private fun loadIcon(path: String) = IconLoader.getIcon(path, DigdagIcons::class.java)

    val FILE: Icon = loadIcon("/icons/dig-dag-logo-transparent-16x16.png")
}