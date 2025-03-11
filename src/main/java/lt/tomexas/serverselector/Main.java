package lt.tomexas.serverselector;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lt.tomexas.serverselector.Commands.*;
import lt.tomexas.serverselector.Listeners.*;
import lt.tomexas.serverselector.Utils.HudManager;
import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.sql.SQLException;
import java.util.*;

public final class Main extends JavaPlugin implements PluginMessageListener {

    private static Main instance;
    private ProtocolManager protocolManager;
    private Database database;
    private final PluginManager pluginManager = Bukkit.getPluginManager();
    private HudManager hudManager;
    private ArmorStand armorStand;
    private final Map<Player, BossBar> playerHud = new HashMap<>();

    private final List<String> servers = new ArrayList<>();

    @Override
    public void onEnable() {
        // Plugin startup logic

        instance = this;
        protocolManager = ProtocolLibrary.getProtocolManager();
        hudManager = new HudManager();

        this.setupDatabase();

        if (database.getUUID() != null)
            armorStand = (ArmorStand) Bukkit.getEntity(UUID.fromString(database.getUUID()));

        getCommand("setBg").setExecutor(new BGCommand());

        pluginManager.registerEvents(new PlayerJoinListener(), this);
        pluginManager.registerEvents(new PlayerQuitListener(), this);
        pluginManager.registerEvents(new PlayerToggleSneakListener(), this);
        pluginManager.registerEvents(new PlayerItemHeldListener(), this);
        pluginManager.registerEvents(new PlayerArmSwingListener(), this);

        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("BungeeCord")) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();

        if (subChannel.equals("GetServers")) {
            List<String> servers = new ArrayList<>(Arrays.asList(in.readUTF().split(", ")));
            servers.remove("lobby");
            servers.remove("limbo0");
            Collections.reverse(servers);
            setServers(servers);
        }
    }

    @Override
    public void onDisable() {
        protocolManager.removePacketListeners(this);
        try {
            database.closeConnection();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupDatabase() {
        try {
            if (!getDataFolder().exists()) {
                getDataFolder().mkdirs();
            }
            database = new Database(getDataFolder().getAbsolutePath() + "/main.db");
        } catch (SQLException e) {
            e.printStackTrace();
            getLogger().severe("Failed to connect to the database! " + e.getMessage());
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    public static Main getInstance() {
        return instance;
    }

    public Database getDatabase() {
        return database;
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

    public void setArmorStand(ArmorStand armorStand) {
        this.armorStand = armorStand;
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
}
