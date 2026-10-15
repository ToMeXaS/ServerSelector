package lt.tomexas.serverselector.selector.screen;

import lt.tomexas.serverselector.Queue.QueueState;
import lt.tomexas.serverselector.selector.ui.Glyph;
import lt.tomexas.serverselector.selector.ui.Rect;
import lt.tomexas.serverselector.selector.ui.UiElement;
import lt.tomexas.serverselector.selector.ui.UiPlane;
import lt.tomexas.serverselector.server.ServerEntry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

/** Overlay shown while the player is queued: position, progress and a leave button. */
public final class QueuePanel implements Screen {

    private static final double DIM_DEPTH = 1.97;
    private static final double PANEL_DEPTH = 1.95;
    private static final double CONTENT_DEPTH = 1.93;
    private static final double FRONT_DEPTH = 1.92;
    private static final float DIM_SCALE = 4.0f;            // 64x36 px -> 6.4 x 3.6, covers the view
    private static final float PANEL_SCALE = 0.4f;          // 180x140 px -> 1.8 x 1.4
    private static final double BAR_Y = -0.38;
    private static final double BAR_WIDTH = 1.2;
    private static final float BAR_SCALE = 1.0f;            // 48x8 px -> 1.2 x 0.2
    private static final double BUTTON_Y = -0.56;
    private static final float BUTTON_SCALE = 0.35f;        // 96x32 px -> 0.84 x 0.28
    private static final Rect LEAVE = new Rect(0, BUTTON_Y, 0.84, 0.28);
    private static final int BAR_TICKS = 10;

    private final Plugin plugin;
    private final Player player;
    private final UiPlane plane;
    private final Runnable onLeave;
    private final UiElement dim;
    private final UiElement panel;
    private final UiElement heading;
    private final UiElement icon;
    private final UiElement name;
    private final UiElement position;
    private final UiElement total;
    private final UiElement track;
    private final UiElement fill;
    private final UiElement button;
    private final UiElement label;
    private boolean hovered;

    public QueuePanel(Plugin plugin, Player player, UiPlane plane, ServerEntry server, Runnable onLeave) {
        this.plugin = plugin;
        this.player = player;
        this.plane = plane;
        this.onLeave = onLeave;
        dim = UiElement.glyph(plugin, player, plane, Glyph.DIM, 0, 0, DIM_DEPTH, DIM_SCALE);
        panel = UiElement.glyph(plugin, player, plane, Glyph.PANEL, 0, 0, PANEL_DEPTH, PANEL_SCALE);
        heading = text("QUEUED FOR", NamedTextColor.GRAY, false, 0.55, 0.5f);
        icon = UiElement.spawn(plugin, player, plane, server.icon().component(), 1, 0, 0.36, CONTENT_DEPTH, 0.4f);
        name = text(server.displayName(), NamedTextColor.WHITE, true, 0.17, 0.8f);
        position = text("#\u2026", NamedTextColor.GOLD, true, -0.05, 1.6f);
        total = text("Joining queue...", NamedTextColor.GRAY, false, -0.24, 0.5f);
        track = UiElement.glyph(plugin, player, plane, Glyph.BAR_TRACK, 0, BAR_Y, CONTENT_DEPTH, BAR_SCALE);
        fill = UiElement.glyph(plugin, player, plane, Glyph.BAR_FILL, -BAR_WIDTH / 2, BAR_Y, FRONT_DEPTH, BAR_SCALE);
        button = UiElement.glyph(plugin, player, plane, Glyph.BUTTON, 0, BUTTON_Y, CONTENT_DEPTH, BUTTON_SCALE);
        label = UiElement.spawn(plugin, player, plane,
                Component.text("LEAVE QUEUE", NamedTextColor.WHITE, TextDecoration.BOLD), 1, 0, BUTTON_Y, FRONT_DEPTH, 0.5f);
        label.shadow(true);
        setProgress(0);
        player.playSound(player, Sound.UI_TOAST_IN, 0.6f, 1.0f);
    }

    public void update(QueueState state) {
        position.text(Component.text("#" + state.position(), NamedTextColor.GOLD, TextDecoration.BOLD));
        total.text(Component.text("of " + state.total() + " players in queue", NamedTextColor.GRAY));
        setProgress(state.progress());
    }

    public void showUnavailable() {
        total.text(Component.text("Queue unavailable", NamedTextColor.RED));
    }

    @Override
    public boolean hover(double x, double y) {
        boolean over = LEAVE.contains(x, y);
        if (over != hovered) {
            hovered = over;
            button.text((over ? Glyph.BUTTON_HOVER : Glyph.BUTTON).component());
            if (over) player.playSound(player, Sound.BLOCK_NOTE_BLOCK_HAT, 0.3f, 1.6f);
        }
        return over;
    }

    @Override
    public void click(double x, double y) {
        if (!LEAVE.contains(x, y)) return;
        player.playSound(player, Sound.UI_BUTTON_CLICK, 0.6f, 0.8f);
        onLeave.run();
    }

    @Override
    public void close() {
        for (UiElement element : List.of(dim, panel, heading, icon, name, position, total, track, fill, button, label)) {
            element.remove();
        }
    }

    private void setProgress(double progress) {
        // The fill grows from the left end of the track
        fill.layout(-BAR_WIDTH / 2 + BAR_WIDTH * progress / 2, BAR_Y, (float) (BAR_SCALE * progress), BAR_SCALE, BAR_TICKS);
        fill.visible(progress > 0);
    }

    private UiElement text(String text, TextColor color, boolean bold, double y, float scale) {
        Component component = Component.text(text, color);
        if (bold) component = component.decorate(TextDecoration.BOLD);
        UiElement element = UiElement.spawn(plugin, player, plane, component, 1, 0, y, CONTENT_DEPTH, scale);
        element.shadow(true);
        return element;
    }
}
