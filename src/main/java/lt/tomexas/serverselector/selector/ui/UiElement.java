package lt.tomexas.serverselector.selector.ui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * One text display only its viewer can see, positioned by its centre on a {@link UiPlane}.
 * Positions and scales are in reference-plane units, so an element looks the same size at any depth.
 */
public final class UiElement {

    /** Height of one text display line in font pixels. The display's origin is the bottom of its text. */
    static final double LINE_PX = 10.0;
    private static final float GROW_FROM = 0.85f;
    private static final int GROW_TICKS = 4;

    private final UiPlane plane;
    private final TextDisplay display;
    private final double depth;
    private final int lines;
    private double x;
    private double y;
    private float scaleX;
    private float scaleY;
    private boolean visible = true;

    private UiElement(UiPlane plane, TextDisplay display, double depth, int lines, double x, double y, float scale) {
        this.plane = plane;
        this.display = display;
        this.depth = depth;
        this.lines = lines;
        this.x = x;
        this.y = y;
        this.scaleX = scale;
        this.scaleY = scale;
    }

    public static UiElement spawn(Plugin plugin, Player viewer, UiPlane plane, Component text, int lines,
                                  double x, double y, double depth, float scale) {
        float start = scale * GROW_FROM * (float) (depth / UiMath.REFERENCE_DISTANCE);
        Location at = plane.point(x, originY(y, lines, scale), depth);
        TextDisplay display = at.getWorld().spawn(at, TextDisplay.class, d -> {
            d.setPersistent(false);
            d.setVisibleByDefault(false);
            d.setBillboard(Display.Billboard.CENTER);
            d.setBrightness(new Display.Brightness(15, 15));
            d.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            d.setLineWidth(1000);
            d.setShadowed(false);
            d.text(text);
            d.setTransformation(transformation(start, start));
            viewer.showEntity(plugin, d);
        });
        UiElement element = new UiElement(plane, display, depth, lines, x, y, scale);
        // Grow to full size once the client has the entity, which reads as the UI opening up
        Bukkit.getScheduler().runTaskLater(plugin, () -> element.applyScale(GROW_TICKS), 2L);
        return element;
    }

    public static UiElement glyph(Plugin plugin, Player viewer, UiPlane plane, Glyph glyph,
                                  double x, double y, double depth, float scale) {
        return spawn(plugin, viewer, plane, glyph.component(), 1, x, y, depth, scale);
    }

    public void moveTo(double x, double y, float scale, int ticks) {
        layout(x, y, scale, scale, ticks);
    }

    public void layout(double x, double y, float scaleX, float scaleY, int ticks) {
        boolean moved = x != this.x || y != this.y || scaleY != this.scaleY;
        boolean scaled = scaleX != this.scaleX || scaleY != this.scaleY;
        this.x = x;
        this.y = y;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        if (!display.isValid()) return;
        if (moved) {
            display.setTeleportDuration(Math.max(0, Math.min(59, ticks)));
            display.teleport(plane.point(x, originY(y, lines, scaleY), depth));
        }
        if (scaled) applyScale(ticks);
    }

    public void text(Component text) {
        display.text(text);
    }

    public void opacity(int alpha) {
        display.setTextOpacity((byte) Math.max(0, Math.min(255, alpha)));
    }

    public void shadow(boolean shadow) {
        display.setShadowed(shadow);
    }

    public void visible(boolean visible) {
        if (this.visible == visible) return;
        this.visible = visible;
        applyScale(0);
    }

    public void remove() {
        display.remove();
    }

    private void applyScale(int ticks) {
        if (!display.isValid()) return;
        float factor = (float) (depth / UiMath.REFERENCE_DISTANCE);
        display.setInterpolationDelay(0);
        display.setInterpolationDuration(ticks);
        display.setTransformation(visible ? transformation(scaleX * factor, scaleY * factor) : transformation(0, 0));
    }

    private static double originY(double centerY, int lines, float scale) {
        return centerY - lines * LINE_PX / 2 * UiMath.PIXEL * scale;
    }

    private static Transformation transformation(float scaleX, float scaleY) {
        return new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(scaleX, scaleY, scaleX), new AxisAngle4f());
    }
}
