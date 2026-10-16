package lt.tomexas.serverselector.selector;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lt.tomexas.serverselector.selector.screen.CarouselScreen;
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

/** One player's open selector: seat, camera, cursor and the carousel. */
final class SelectorSession {

    private static final long CLICK_DELAY_MS = 250;
    private static final double CURSOR_DEPTH = 1.8;
    private static final float CURSOR_SCALE = 1.0f;          // 16 px glyph -> 0.4
    // The cursor glyph's tip is its top-left corner, so its centre sits down and to the right of the cursor point
    private static final double CURSOR_TIP_X = 0.2;
    private static final double CURSOR_TIP_Y = -0.2;
    private static final double TINT_DEPTH = 2.4;
    private static final float TINT_SCALE = 6.0f;             // 64x36 px -> 9.6 x 5.4, covers the screen
    private static final double TITLE_MARGIN = 0.3; // Distance from the top screen edge to the title's centre
    private static final float TITLE_SCALE = 1.4f;
    // Gliding over two ticks smooths the 20-per-second updates at the cost of ~1 tick extra lag
    private static final int CURSOR_MOVE_TICKS = 2;

    private final Plugin plugin;
    private final Player player;
    private final UiPlane plane;
    private final Pig seat;
    private final UiElement tint;
    private final UiElement title;
    private final CarouselScreen carousel;
    private final UiElement cursor;
    private final CursorTracker tracker;
    private double cursorX;
    private double cursorY;
    private boolean pointing;
    private long lastClick;
    private boolean closed;

    SelectorSession(Plugin plugin, Player player, ArmorStand camera, SelectorConfig config, ServerRegistry registry,
                    int selected) {
        this.plugin = plugin;
        this.player = player;

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

        tint = UiElement.glyph(plugin, player, plane, Glyph.TINT, 0, 0, TINT_DEPTH, TINT_SCALE);
        title = UiElement.spawn(plugin, player, plane,
                Component.text(config.title(), NamedTextColor.GOLD, TextDecoration.BOLD), 1,
                0, config.cursorBoundsY() - TITLE_MARGIN, UiMath.REFERENCE_DISTANCE, TITLE_SCALE);
        title.shadow(true);
        carousel = new CarouselScreen(plugin, player, plane, registry, selected, config.cursorBoundsY(),
                this::connect);
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
            cursor.moveTo(x + CURSOR_TIP_X * CURSOR_SCALE, y + CURSOR_TIP_Y * CURSOR_SCALE, CURSOR_SCALE, CURSOR_MOVE_TICKS);
        }

        carousel.tick();
        boolean clickable = carousel.hover(cursorX, cursorY);
        if (clickable != pointing) {
            pointing = clickable;
            cursor.text((clickable ? Glyph.CURSOR_HAND : Glyph.CURSOR_ARROW).component());
        }
    }

    void click() {
        long now = System.currentTimeMillis();
        if (now - lastClick < CLICK_DELAY_MS) return;
        lastClick = now;
        carousel.click(cursorX, cursorY);
    }

    void scroll(int direction) {
        carousel.scroll(direction);
    }

    int selectedIndex() {
        return carousel.selectedIndex();
    }

    void close() {
        closed = true;
        carousel.close();
        tint.remove();
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

    /** Sends the player to the server through the proxy; the session closes when they leave the lobby. */
    private void connect(ServerEntry server) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(server.id());
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }
}
