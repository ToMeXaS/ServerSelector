package lt.tomexas.serverselector.selector.ui;

import net.kyori.adventure.text.Component;

/**
 * The card backgrounds tools/build_selector_assets.py pre-renders in a grid of sizes, so a card can use
 * the smallest one its text fits on.
 */
public final class CardArt {

    /** Art sizes in texture pixels, matching the script's CARD_WIDTHS and CARD_HEIGHTS. */
    public static final int[] WIDTHS = {128, 160, 192, 224};
    public static final int[] HEIGHTS = {144, 164, 184};
    /** Element scale the art is drawn at: one art pixel is half a font pixel. */
    public static final float PIXEL_SCALE = 0.5f;

    private static final char FIRST_CHAR = '\uE320';
    private static final int STYLE_STRIDE = WIDTHS.length * HEIGHTS.length;

    private CardArt() {
    }

    public record Size(int widthIndex, int heightIndex) {

        /** Width on the plane, in units. */
        public double width() {
            return WIDTHS[widthIndex] * UiMath.PIXEL * PIXEL_SCALE;
        }

        /** Height on the plane, in units. */
        public double height() {
            return HEIGHTS[heightIndex] * UiMath.PIXEL * PIXEL_SCALE;
        }
    }

    /** The smallest art at least this big in each direction, or the largest if nothing is. */
    public static Size fit(double neededWidth, double neededHeight) {
        return new Size(fitIndex(WIDTHS, neededWidth), fitIndex(HEIGHTS, neededHeight));
    }

    public static Component component(Size size, boolean selected) {
        int code = FIRST_CHAR + (selected ? STYLE_STRIDE : 0) + size.widthIndex() * HEIGHTS.length + size.heightIndex();
        return Glyph.glyphText((char) code, "");
    }

    private static int fitIndex(int[] pixels, double needed) {
        for (int i = 0; i < pixels.length; i++) {
            if (pixels[i] * UiMath.PIXEL * PIXEL_SCALE >= needed) return i;
        }
        return pixels.length - 1;
    }
}
