package lt.tomexas.serverselector.selector;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import lt.tomexas.serverselector.Queue.QueueState;
import lt.tomexas.serverselector.Queue.QueueTracker;
import lt.tomexas.serverselector.selector.screen.CarouselScreen;
import lt.tomexas.serverselector.selector.screen.QueuePanel;
import lt.tomexas.serverselector.selector.screen.Screen;
import lt.tomexas.serverselector.selector.ui.CursorTracker;
import lt.tomexas.serverselector.selector.ui.Glyph;
import lt.tomexas.serverselector.selector.ui.UiElement;
import lt.tomexas.serverselector.selector.ui.UiMath;
import lt.tomexas.serverselector.selector.ui.UiPlane;
import lt.tomexas.serverselector.server.SelectorConfig;
import lt.tomexas.serverselector.server.ServerEntry;
import lt.tomexas.serverselector.server.ServerRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.List;

/** One player's open selector: seat, camera, cursor and the active screens. */
final class SelectorSession {

    private static final long CLICK_DELAY_MS = 250;
    // ajQueue may not report the new queue straight away; don't close the panel in the meantime
    private static final long JOIN_GRACE_MS = 3000;
    private static final double CURSOR_DEPTH = 1.8;
    private static final float CURSOR_SCALE = 1.0f;          // 16 px glyph -> 0.4
    // The cursor glyph's tip is its top-left corner, so its centre sits down and to the right of the cursor point
    private static final double CURSOR_TIP_X = 0.2;
    private static final double CURSOR_TIP_Y = -0.2;
    private static final double VIGNETTE_DEPTH = 2.4;
    private static final float VIGNETTE_SCALE = 1.6f;        // 256x144 px -> 10.2 x 5.8, wider than any FOV
    private static final double TITLE_Y = 1.15;
    private static final float TITLE_SCALE = 1.4f;
    private static final int QUEUE_POLL_TICKS = 20;

    private final Plugin plugin;
    private final Player player;
    private final ServerRegistry registry;
    private final QueueTracker queue;
    private final UiPlane plane;
    private final Pig seat;
    private final UiElement vignette;
    private final UiElement title;
    private final CarouselScreen carousel;
    private final UiElement cursor;
    private final CursorTracker tracker;
    private QueuePanel queuePanel;
    private ServerEntry queuedFor;
    private double cursorX;
    private double cursorY;
    private boolean pointing;
    private long lastClick;
    private long joinPendingUntil;
    private int ticks;
    private boolean closed;

    SelectorSession(Plugin plugin, Player player, ArmorStand camera, SelectorConfig config, ServerRegistry registry,
                    QueueTracker queue, int selected) {
        this.plugin = plugin;
        this.player = player;
        this.registry = registry;
        this.queue = queue;

        Location eye = camera.getEyeLocation();
        this.plane = new UiPlane(eye);
        this.seat = spawnSeat(eye);

        // Face the player the same way as the camera with a level head, so the cursor starts in the middle
        Location start = seat.getLocation();
        start.setYaw(eye.getYaw());
        start.setPitch(0f);
        player.teleport(start);
        seat.addPassenger(player);
        lockCamera();

        vignette = UiElement.glyph(plugin, player, plane, Glyph.VIGNETTE, 0, 0, VIGNETTE_DEPTH, VIGNETTE_SCALE);
        title = UiElement.spawn(plugin, player, plane,
                Component.text(config.title(), NamedTextColor.GOLD, TextDecoration.BOLD), 1,
                0, TITLE_Y, UiMath.REFERENCE_DISTANCE, TITLE_SCALE);
        title.shadow(true);
        carousel = new CarouselScreen(plugin, player, plane, registry, selected, this::joinQueue);
        cursor = UiElement.glyph(plugin, player, plane, Glyph.CURSOR_ARROW,
                CURSOR_TIP_X * CURSOR_SCALE, CURSOR_TIP_Y * CURSOR_SCALE, CURSOR_DEPTH, CURSOR_SCALE);
        // The player is teleported to pitch 0 at open, so anchor the tracker there
        tracker = new CursorTracker(config.cursorBoundsX(), config.cursorBoundsY(), plane.yaw(), 0);
    }

    void tick() {
        if (closed) return;
        Location location = player.getLocation();
        tracker.update(location.getYaw(), location.getPitch());
        double x = tracker.x();
        double y = tracker.y();
        if (x != cursorX || y != cursorY) {
            cursorX = x;
            cursorY = y;
            cursor.moveTo(x + CURSOR_TIP_X * CURSOR_SCALE, y + CURSOR_TIP_Y * CURSOR_SCALE, CURSOR_SCALE, 1);
        }

        carousel.tick();
        if (queuePanel != null) queuePanel.tick();
        boolean clickable = activeScreen().hover(cursorX, cursorY);
        if (clickable != pointing) {
            pointing = clickable;
            cursor.text((clickable ? Glyph.CURSOR_HAND : Glyph.CURSOR_ARROW).component());
        }

        if (++ticks % QUEUE_POLL_TICKS == 0) queue.poll(player, this::onQueueState);
    }

    void click() {
        long now = System.currentTimeMillis();
        if (now - lastClick < CLICK_DELAY_MS) return;
        lastClick = now;
        activeScreen().click(cursorX, cursorY);
    }

    void scroll(int direction) {
        if (queuePanel == null) carousel.scroll(direction);
    }

    int selectedIndex() {
        return carousel.selectedIndex();
    }

    void close() {
        closed = true;
        if (queuePanel != null) queuePanel.close();
        carousel.close();
        vignette.remove();
        title.remove();
        cursor.remove();
        seat.eject();
        seat.remove();
    }

    private Pig spawnSeat(Location eye) {
        // The pig is both seat and camera: a riding client keeps sending its head rotation, and only keeps its
        // camera on its own vehicle. Its eyes go where the armor stand's are, so the view is unchanged.
        Pig pig = eye.getWorld().spawn(eye, Pig.class, p -> {
            p.setInvisible(true);
            p.setAI(false);
            p.setGravity(false);
            p.setSilent(true);
            p.setInvulnerable(true);
            p.setCollidable(false);
            p.setPersistent(false);
        });
        pig.teleport(eye.clone().subtract(0, pig.getEyeHeight(), 0));
        pig.setRotation(eye.getYaw(), eye.getPitch());
        return pig;
    }

    private void lockCamera() {
        // The client ignores a camera packet for an entity it has not been sent yet, so wait for the pig
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (closed || !player.isOnline() || !seat.isValid()) return;
            PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.CAMERA);
            packet.getIntegers().write(0, seat.getEntityId());
            ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
        }, 2L);
    }

    private Screen activeScreen() {
        return queuePanel != null ? queuePanel : carousel;
    }

    private void joinQueue(ServerEntry server) {
        queue.join(player, server);
        joinPendingUntil = System.currentTimeMillis() + JOIN_GRACE_MS;
        openQueuePanel(server);
    }

    private void leaveQueue() {
        if (queuedFor != null) queue.leave(player, queuedFor.id());
        joinPendingUntil = 0;
        closeQueuePanel();
    }

    private void onQueueState(QueueState state) {
        if (closed) return;
        if (!state.available()) {
            if (queuePanel != null) queuePanel.showUnavailable();
            return;
        }
        if (state.inQueue()) {
            ServerEntry server = registry.entry(state.server());
            if (server == null && state.server() != null) {
                server = new ServerEntry(state.server(), state.server(), Glyph.ICON_STAR, List.of());
            }
            if (server == null) server = queuedFor;
            if (server == null) return;
            if (queuePanel == null || !server.id().equalsIgnoreCase(queuedFor.id())) openQueuePanel(server);
            queuePanel.update(state);
        } else if (queuePanel != null && System.currentTimeMillis() > joinPendingUntil) {
            closeQueuePanel();
        }
    }

    private void openQueuePanel(ServerEntry server) {
        if (queuePanel != null) queuePanel.close();
        queuedFor = server;
        carousel.dim(true);
        queuePanel = new QueuePanel(plugin, player, plane, server, this::leaveQueue);
    }

    private void closeQueuePanel() {
        if (queuePanel == null) return;
        queuePanel.close();
        queuePanel = null;
        queuedFor = null;
        carousel.dim(false);
    }
}
