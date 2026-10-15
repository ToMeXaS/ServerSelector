package lt.tomexas.serverselector.Utils;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import lt.tomexas.serverselector.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

/**
 * Draws a mouse cursor and clickable server buttons in front of the camera armor stand.
 * The player's camera is locked to the armor stand. A client only keeps sending its head rotation
 * while the camera is elsewhere if it is riding something, so each player sits on an invisible pig,
 * and the angle they turn from the camera's direction is projected onto a plane facing the camera.
 * All coordinates are in "plane units" (blocks on the button plane, x right, y up from its center).
 */
public class CursorManager {

    // Plane geometry (blocks)
    private static final double BUTTON_DISTANCE = 2.0;
    private static final double CURSOR_DISTANCE = 1.9; // slightly closer so it renders over the buttons
    private static final double BUTTON_ROW_Y = -0.35;
    private static final double BUTTON_SPACING = 0.25;
    private static final double BOUNDS_X = 1.6;
    private static final double BOUNDS_Y = 0.9;

    // Text display sizing: one font pixel is 1/40 block at scale 1
    private static final double PIXEL = 1.0 / 40.0;
    private static final float BUTTON_SCALE = 0.7f;
    private static final float CURSOR_SCALE = 0.6f;
    private static final int CHAR_WIDTH_PX = 6;
    private static final int LINE_HEIGHT_PX = 10;

    // Head angles past this are clamped, as tan() runs off to infinity near 90 degrees
    private static final double MAX_ANGLE = 80.0;

    private static final String CURSOR_GLYPH = "↖";
    private static final Color BUTTON_COLOR = Color.fromARGB(160, 0, 0, 0);
    private static final Color BUTTON_HOVER_COLOR = Color.fromARGB(200, 255, 170, 0);

    private final Main plugin = Main.getInstance();
    private final Map<UUID, Session> sessions = new HashMap<>();

    public CursorManager() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);

        // DEBUG: temporary instrumentation for "camera is not locked"
        plugin.getProtocolManager().addPacketListener(new com.comphenix.protocol.events.PacketAdapter(plugin,
                com.comphenix.protocol.PacketType.Play.Server.CAMERA) {
            @Override
            public void onPacketSending(com.comphenix.protocol.events.PacketEvent event) {
                plugin.getLogger().info("[camera-debug] OUT CAMERA to " + event.getPlayer().getName()
                        + " entityId=" + event.getPacket().getIntegers().read(0)
                        + " playerId=" + event.getPlayer().getEntityId());
            }
        });
    }

    // DEBUG
    private void debugCamera(Player player, String when) {
        ArmorStand stand = plugin.getArmorStand();
        Entity fresh = stand == null ? null : Bukkit.getEntity(stand.getUniqueId());
        plugin.getLogger().info("[camera-debug] " + when
                + " stand=" + (stand == null ? "null" : stand.getEntityId() + " valid=" + stand.isValid()
                    + " dead=" + stand.isDead() + " loc=" + stand.getLocation().toVector())
                + " freshLookup=" + (fresh == null ? "null" : fresh.getEntityId())
                + " playerVehicle=" + (player.getVehicle() == null ? "none" : player.getVehicle().getType() + "#" + player.getVehicle().getEntityId())
                + " playerLoc=" + player.getLocation().toVector()
                + " canSeeStand=" + (stand != null && player.canSee(stand)));
    }

    public void attach(Player player) {
        detach(player);
        ArmorStand camera = plugin.getArmorStand();
        if (camera == null) return;

        Location eye = camera.getEyeLocation();
        Session session = new Session(player, eye);

        // The pig is both the seat and the camera: a riding client only keeps its camera on its own vehicle.
        // Its eyes go where the armor stand's are, so the view matches the configured camera.
        Location seat = eye.clone();
        session.seat = seat.getWorld().spawn(seat, Pig.class, pig -> {
            pig.setInvisible(true);
            pig.setAI(false);
            pig.setGravity(false);
            pig.setSilent(true);
            pig.setInvulnerable(true);
            pig.setCollidable(false);
            pig.setPersistent(false);
        });
        seat.subtract(0, session.seat.getEyeHeight(), 0);
        session.seat.teleport(seat);
        session.seat.setRotation(eye.getYaw(), eye.getPitch());

        // Face the player the same way as the camera with a level head, so the cursor starts in the middle
        Location start = seat.clone();
        start.setPitch(0f);
        player.teleport(start);
        session.seat.addPassenger(player);

        // The client ignores a camera packet for an entity it has not been sent yet, so wait for the pig to show up
        Pig cameraPig = session.seat;
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline() || !cameraPig.isValid()) return;
            PacketContainer packet = plugin.getProtocolManager().createPacket(PacketType.Play.Server.CAMERA);
            packet.getIntegers().write(0, cameraPig.getEntityId());
            plugin.getProtocolManager().sendServerPacket(player, packet);
        }, 2L);

        session.cursor = spawnDisplay(player, eye, Component.text(CURSOR_GLYPH, NamedTextColor.WHITE), CURSOR_SCALE);
        session.cursor.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        session.cursor.setShadowed(true);
        session.cursor.setTeleportDuration(1);
        sessions.put(player.getUniqueId(), session);
        moveCursor(session);

        debugCamera(player, "attach");
        Bukkit.getScheduler().runTaskLater(plugin, () -> debugCamera(player, "attach+5t"), 5L);
        Bukkit.getScheduler().runTaskLater(plugin, () -> debugCamera(player, "attach+60t"), 60L);
    }

    public void detach(Player player) {
        // Removed from the map first so the dismount listener lets the player off the seat
        Session session = sessions.remove(player.getUniqueId());
        if (session != null) session.remove();
    }

    public boolean isAttached(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public void detachAll() {
        sessions.values().forEach(Session::remove);
        sessions.clear();
    }

    /** Index of the server button under the cursor, or -1. */
    public int getHoveredIndex(Player player) {
        Session session = sessions.get(player.getUniqueId());
        return session == null ? -1 : session.hovered;
    }

    /** Moves the cursor onto a button, used by hotbar scrolling. */
    public void snapTo(Player player, int index) {
        Session session = sessions.get(player.getUniqueId());
        if (session == null || index < 0 || index >= session.buttons.size()) return;
        // The head still drives the cursor, so keep the jump as an offset on top of it
        Button button = session.buttons.get(index);
        session.offsetX += button.x - session.x;
        session.offsetY += button.y - session.y;
        session.x = button.x;
        session.y = button.y;
        moveCursor(session);
        updateHover(session);
    }

    private void tick() {
        List<String> servers = plugin.getServers();
        for (Session session : sessions.values()) {
            Player player = session.player;
            if (!player.isOnline()) continue;

            if (!session.serverNames.equals(servers)) buildButtons(session, servers);

            Location location = player.getLocation();
            double yaw = ((location.getYaw() - session.eye.getYaw() + 540.0) % 360.0) - 180.0;
            double x = clamp(project(yaw) + session.offsetX, -BOUNDS_X, BOUNDS_X);
            // Pitch is positive when looking down
            double y = clamp(project(-location.getPitch()) + session.offsetY, -BOUNDS_Y, BOUNDS_Y);
            if (x == session.x && y == session.y) continue;

            session.x = x;
            session.y = y;
            moveCursor(session);
            updateHover(session);
        }
    }

    /** Where the line of sight, turned by the given angle, hits the button plane. */
    private static double project(double degrees) {
        return BUTTON_DISTANCE * Math.tan(Math.toRadians(clamp(degrees, -MAX_ANGLE, MAX_ANGLE)));
    }

    private void buildButtons(Session session, List<String> servers) {
        session.buttons.forEach(button -> button.display.remove());
        session.buttons.clear();
        session.serverNames = new ArrayList<>(servers);
        session.hovered = -1;

        List<Double> widths = new ArrayList<>();
        double totalWidth = 0;
        for (String server : servers) {
            double width = (label(server).length() * CHAR_WIDTH_PX + 2) * PIXEL * BUTTON_SCALE;
            widths.add(width);
            totalWidth += width;
        }
        totalWidth += BUTTON_SPACING * Math.max(0, servers.size() - 1);

        double height = (LINE_HEIGHT_PX + 2) * PIXEL * BUTTON_SCALE;
        double left = -totalWidth / 2;
        for (int i = 0; i < servers.size(); i++) {
            double width = widths.get(i);
            Button button = new Button(left + width / 2, BUTTON_ROW_Y, width, height);
            // Text displays grow upward from their origin, so the origin sits at the bottom of the box
            Location location = session.pointOnPlane(button.x, button.y - height / 2, BUTTON_DISTANCE);
            button.display = spawnDisplay(session.player, location,
                    Component.text(label(servers.get(i)), NamedTextColor.WHITE), BUTTON_SCALE);
            button.display.setBackgroundColor(BUTTON_COLOR);
            session.buttons.add(button);
            left += width + BUTTON_SPACING;
        }
        updateHover(session);
    }

    private void updateHover(Session session) {
        int hovered = -1;
        for (int i = 0; i < session.buttons.size(); i++) {
            if (session.buttons.get(i).contains(session.x, session.y)) {
                hovered = i;
                break;
            }
        }
        if (hovered == session.hovered) return;

        if (session.hovered >= 0 && session.hovered < session.buttons.size())
            session.buttons.get(session.hovered).display.setBackgroundColor(BUTTON_COLOR);
        if (hovered >= 0)
            session.buttons.get(hovered).display.setBackgroundColor(BUTTON_HOVER_COLOR);
        session.hovered = hovered;

        // Keep the existing selection index in sync, unless the player is queued for a server
        Player player = session.player;
        if (hovered >= 0 && !plugin.getPlayerQueueStatus().getOrDefault(player.getUniqueId(), false))
            plugin.getHudManager().getPlayerServerIndex().put(player, hovered);
    }

    private void moveCursor(Session session) {
        // The glyph's tip is its top-left corner, while the display origin is its bottom-center
        double glyphWidth = CHAR_WIDTH_PX * PIXEL * CURSOR_SCALE;
        double glyphHeight = LINE_HEIGHT_PX * PIXEL * CURSOR_SCALE;
        double scale = CURSOR_DISTANCE / BUTTON_DISTANCE;
        Location target = session.pointOnPlane(
                (session.x + glyphWidth / 2) * scale,
                (session.y - glyphHeight) * scale,
                CURSOR_DISTANCE);
        session.cursor.teleport(target);
    }

    private TextDisplay spawnDisplay(Player player, Location location, Component text, float scale) {
        return location.getWorld().spawn(location, TextDisplay.class, display -> {
            display.setPersistent(false);
            display.setVisibleByDefault(false);
            display.setBillboard(Display.Billboard.CENTER);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setTeleportDuration(2);
            display.setTransformation(new Transformation(new Vector3f(), new AxisAngle4f(),
                    new Vector3f(scale, scale, scale), new AxisAngle4f()));
            display.text(text);
            player.showEntity(plugin, display);
        });
    }

    private static String label(String server) {
        if (server.isEmpty()) return server;
        return " " + Character.toUpperCase(server.charAt(0)) + server.substring(1) + " ";
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static final class Session {
        private final Player player;
        private final Location eye;
        private final Vector forward;
        private final Vector right;
        private final Vector up;
        private Pig seat;
        private TextDisplay cursor;
        private final List<Button> buttons = new ArrayList<>();
        private List<String> serverNames = new ArrayList<>();
        private double x;
        private double y;
        private double offsetX;
        private double offsetY;
        private int hovered = -1;

        private Session(Player player, Location eye) {
            this.player = player;
            this.eye = eye;
            this.forward = eye.getDirection().normalize();
            this.right = forward.clone().crossProduct(new Vector(0, 1, 0)).normalize();
            this.up = right.clone().crossProduct(forward).normalize();
        }

        private Location pointOnPlane(double x, double y, double distance) {
            return eye.clone()
                    .add(forward.clone().multiply(distance))
                    .add(right.clone().multiply(x))
                    .add(up.clone().multiply(y));
        }

        private void remove() {
            if (seat != null) {
                seat.eject();
                seat.remove();
            }
            if (cursor != null) cursor.remove();
            buttons.forEach(button -> button.display.remove());
            buttons.clear();
        }
    }

    private static final class Button {
        private final double x;
        private final double y;
        private final double halfWidth;
        private final double halfHeight;
        private TextDisplay display;

        private Button(double x, double y, double width, double height) {
            this.x = x;
            this.y = y;
            this.halfWidth = width / 2;
            this.halfHeight = height / 2;
        }

        private boolean contains(double px, double py) {
            return Math.abs(px - x) <= halfWidth && Math.abs(py - y) <= halfHeight;
        }
    }
}
