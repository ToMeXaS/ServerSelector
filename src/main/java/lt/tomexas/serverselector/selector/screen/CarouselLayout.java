package lt.tomexas.serverselector.selector.screen;

/** Where each server card sits in the carousel, relative to the selected one. */
public final class CarouselLayout {

    public enum Slot { CENTER, LEFT, RIGHT, HIDDEN_LEFT, HIDDEN_RIGHT }

    public static final double SIDE_X = 1.45;
    public static final double HIDDEN_X = 1.9;
    public static final float SIDE_SCALE = 0.7f;
    public static final float HIDDEN_SCALE = 0.4f;

    private CarouselLayout() {
    }

    /** Signed steps from the selected card, wrapped into (-count/2, count/2]. */
    public static int offset(int index, int selected, int count) {
        int offset = Math.floorMod(index - selected, count);
        if (offset > count / 2) offset -= count;
        return offset;
    }

    public static Slot slot(int index, int selected, int count) {
        int offset = offset(index, selected, count);
        if (offset == 0) return Slot.CENTER;
        if (offset == -1) return Slot.LEFT;
        if (offset == 1) return Slot.RIGHT;
        return offset < 0 ? Slot.HIDDEN_LEFT : Slot.HIDDEN_RIGHT;
    }

    public static double x(Slot slot) {
        return switch (slot) {
            case CENTER -> 0.0;
            case LEFT -> -SIDE_X;
            case RIGHT -> SIDE_X;
            case HIDDEN_LEFT -> -HIDDEN_X;
            case HIDDEN_RIGHT -> HIDDEN_X;
        };
    }

    public static float scale(Slot slot) {
        return switch (slot) {
            case CENTER -> 1.0f;
            case LEFT, RIGHT -> SIDE_SCALE;
            case HIDDEN_LEFT, HIDDEN_RIGHT -> HIDDEN_SCALE;
        };
    }

    public static boolean visible(Slot slot) {
        return slot != Slot.HIDDEN_LEFT && slot != Slot.HIDDEN_RIGHT;
    }

    public static int wrap(int index, int count) {
        return Math.floorMod(index, count);
    }
}
