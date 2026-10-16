package lt.tomexas.serverselector.selector.ui;

/** Advance widths of Minecraft's default font, so the UI can size things to fit their text. */
public final class TextWidth {

    private static final int DEFAULT_ADVANCE = 6;

    private TextWidth() {
    }

    /** Width of the text in font pixels at scale 1. Bold draws every glyph one pixel wider. */
    public static int of(String text, boolean bold) {
        int width = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            width += advance(c);
            if (bold && c != ' ') width++;
        }
        return width;
    }

    private static int advance(char c) {
        return switch (c) {
            case ' ' -> 4;
            case 'i', '!', '.', ',', ':', ';', '|', '\'', '\u00B7' -> 2;
            case 'l', '`' -> 3;
            case 'I', 't', '[', ']', '(', ')', '{', '}', '*', '"' -> 4;
            case 'f', 'k', '<', '>' -> 5;
            case '@', '~' -> 7;
            case '\u25CF' -> 6;
            default -> DEFAULT_ADVANCE;
        };
    }
}
