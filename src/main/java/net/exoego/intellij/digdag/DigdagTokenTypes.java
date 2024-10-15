package net.exoego.intellij.digdag;

public interface DigdagTokenTypes {
    DigdagElementType COMMENT = new DigdagElementType("comment");
    DigdagElementType WHITESPACE = new DigdagElementType("whitespace");
    DigdagElementType INDENT = new DigdagElementType("indent");
    DigdagElementType EOL = new DigdagElementType("Eol");
    DigdagElementType SCALAR_EOL = new DigdagElementType("block scalar EOL");

    DigdagElementType LBRACE = new DigdagElementType("{");
    DigdagElementType RBRACE = new DigdagElementType("}");
    DigdagElementType LBRACKET = new DigdagElementType("[");
    DigdagElementType RBRACKET = new DigdagElementType("]");
    DigdagElementType COMMA = new DigdagElementType(",");
    DigdagElementType COLON = new DigdagElementType(":");
    DigdagElementType QUESTION = new DigdagElementType("?");
    DigdagElementType AMPERSAND = new DigdagElementType("&");
    DigdagElementType STAR = new DigdagElementType("*");

    DigdagElementType DOCUMENT_MARKER = new DigdagElementType("---");
    DigdagElementType DOCUMENT_END = new DigdagElementType("...");
    DigdagElementType SEQUENCE_MARKER = new DigdagElementType("-");

//    DigdagElementType TAG = new DigdagElementType("tag");
    DigdagElementType SCALAR_KEY = new DigdagElementType("scalar key");

    // sequential TEXT tokens will merge for parser into one token
    DigdagElementType TEXT = new DigdagElementType("text");

    DigdagElementType SCALAR_STRING = new DigdagElementType("scalar string");
    DigdagElementType SCALAR_DSTRING = new DigdagElementType("scalar dstring");

    DigdagElementType SCALAR_LIST = new DigdagElementType("scalar list");
    DigdagElementType SCALAR_TEXT = new DigdagElementType("scalar text");

    DigdagElementType ANCHOR = new DigdagElementType("anchor");
    DigdagElementType ALIAS = new DigdagElementType("alias");
}
