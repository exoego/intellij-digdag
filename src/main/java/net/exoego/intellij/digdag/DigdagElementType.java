package net.exoego.intellij.digdag;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;

public class DigdagElementType extends IElementType {
    public DigdagElementType(@NotNull String debugName) {
        super(debugName, DigdagFileType.INSTANCE.getLanguage());
    }
}
