package lt.tomexas.serverselector;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import lt.tomexas.serverselector.Listeners.*;
import lt.tomexas.serverselector.Utils.HudManager;
import lt.tomexas.serverselector.Utils.Spacer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Logger;

public final class Main extends JavaPlugin implements PluginMessageListener {

    private static Main instance;
    private ProtocolManager protocolManager;
    private Database database;
    private final PluginManager pluginManager = Bukkit.getPluginManager();
    private HudManager hudManager;
    private ArmorStand armorStand;
    private final Map<Player, BossBar> playerHud = new HashMap<>();
    private final List<String> servers = new ArrayList<>();
    private final Map<UUID, Boolean> playerQueueStatus = new HashMap<>();

    @Override
    public void onEnable() {
        getServer().getMessenger().registerOutgoingPluginChannel(this, "lobby:server_selector");
        getServer().getMessenger().registerIncomingPluginChannel(this, "lobby:server_selector", this);

        instance = this;
        protocolManager = ProtocolLibrary.getProtocolManager();
        hudManager = new HudManager();

        this.setupDatabase();

        if (database.getUUID() != null)
            armorStand = (ArmorStand) Bukkit.getEntity(UUID.fromString(database.getUUID()));

        pluginManager.registerEvents(new PlayerJoinListener(), this);
        pluginManager.registerEvents(new PlayerQuitListener(), this);
        pluginManager.registerEvents(new PlayerToggleSneakListener(), this);
        pluginManager.registerEvents(new PlayerItemHeldListener(), this);
        pluginManager.registerEvents(new PlayerArmSwingListener(), this);

        pluginManager.registerEvents(new QueueJoinListener(), this);
        pluginManager.registerEvents(new QueueLeaveListener(), this);

        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);
    }

    @Override
    public void onPluginMessageReceived(String channel, @NotNull Player player, byte @NotNull [] message) {
        if (!channel.equals("BungeeCord") && !channel.equals("lobby:server_selector")) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();

        // GetServers subchannel
        if (subChannel.equals("GetServers")) {
            List<String> servers = new ArrayList<>(Arrays.asList(in.readUTF().split(", ")));
            servers.remove("lobby");
            servers.remove("limbo0");
            Collections.reverse(servers);
            setServers(servers);
        }

        // PlayerInQueue subchannel
        if (subChannel.equals("PlayerInQueue")) {
            String playerUUID = in.readUTF();
            boolean inQueue = in.readBoolean();
            //Logger.getLogger("Minecraft").info("Player " + playerUUID + " is in queue: " + inQueue);
            playerQueueStatus.put(UUID.fromString(playerUUID), inQueue);
        }

        // PlayerPositionChanged subchannel
        if (subChannel.equals("PlayerPositionChanged")) {
            int position = in.readInt();
            int total = in.readInt();
            //Logger.getLogger("Minecraft").info("Player " + playerUUID + " is now in position " + position + " out of " + total);

            BossBar bossBar = getPlayerHud().get(player);
            // Then in your onPluginMessageReceived method
            Component component = hudManager.getPlayerQueueTitle().get(player)
                    .replaceText(builder -> builder
                            .match(Spacer.getNegativeSpacer(163) + "Pick your realm and make it unforgettable!")
                            .replacement(getQueueText(position, total))
                    );
            bossBar.name(component);
        }
    }

    public static String getQueueText(int position, int total) {
        String text = "Your position in queue " + position + " out of " + total;

        // Base text lengths in pixels (approximate Minecraft font widths)
        int baseTextWidth = "Your position in queue 1 out of 1".length() * 6;
        int currentTextWidth = text.length() * 6;

        // Calculate the difference in width
        int widthDifference = currentTextWidth - baseTextWidth;

        // Adjust spacers to maintain centering
        int negativeSpacer = 143 + (widthDifference / 2);
        int positiveSpacer = 26 - (widthDifference / 2);

        return Spacer.getNegativeSpacer(negativeSpacer) +
                text +
                Spacer.getPositiveSpacer(positiveSpacer);
    }

    @Override
    public void onDisable() {
        protocolManager.removePacketListeners(this);
        try {
            database.closeConnection();
        } catch (SQLException e) {
            Logger.getLogger("Minecraft").severe("Failed to close the database connection! " + e.getMessage());
        }
    }

    private void setupDatabase() {
        try {
            if (!getDataFolder().exists()) {
                if (!getDataFolder().mkdirs()) {
                    getLogger().severe("Failed to create data folder!");
                    Bukkit.getPluginManager().disablePlugin(this);
                    return;
                }
            }
            database = new Database(getDataFolder().getAbsolutePath() + "/main.db");
        } catch (SQLException e) {
            getLogger().severe("Failed to connect to the database! " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    public void cleanupQueueCache(UUID uuid) {
        playerQueueStatus.remove(uuid);
    }

    public static Main getInstance() {
        return instance;
    }

    public ProtocolManager getProtocolManager() {
        return protocolManager;
    }

    public HudManager getHudManager() {
        return hudManager;
    }

    public ArmorStand getArmorStand() {
        return armorStand;
    }

    public Map<Player, BossBar> getPlayerHud() {
        return playerHud;
    }

    public List<String> getServers() {
        return servers;
    }

    public void setServers(List<String> servers) {
        this.servers.clear();
        this.servers.addAll(servers);
    }

    public Map<UUID, Boolean> getPlayerQueueStatus() {
        return playerQueueStatus;
    }
}
