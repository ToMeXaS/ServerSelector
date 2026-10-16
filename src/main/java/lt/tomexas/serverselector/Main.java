package lt.tomexas.serverselector;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import lt.tomexas.serverselector.Listeners.*;
import lt.tomexas.serverselector.selector.SelectorManager;
import lt.tomexas.serverselector.selector.ui.Glyph;
import lt.tomexas.serverselector.server.ProxyPoller;
import lt.tomexas.serverselector.server.SelectorConfig;
import lt.tomexas.serverselector.server.ServerRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Logger;

public final class Main extends JavaPlugin implements PluginMessageListener {

    private static Main instance;
    private ProtocolManager protocolManager;
    private Database database;
    private ArmorStand armorStand;
    private SelectorManager selectorManager;
    private ProxyPoller proxyPoller;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        protocolManager = ProtocolLibrary.getProtocolManager();

        this.setupDatabase();

        boolean nexo = getServer().getPluginManager().isPluginEnabled("Nexo");
        Glyph.setNexoAvailable(nexo);
        if (!nexo) getLogger().info("Nexo not found, the selector uses plain-text glyphs.");

        SelectorConfig config = SelectorConfig.from(getConfig(), getLogger());
        ServerRegistry registry = new ServerRegistry(config.servers());
        selectorManager = new SelectorManager(this, this::getArmorStand, config, registry);
        proxyPoller = new ProxyPoller(this, registry);
        proxyPoller.start();

        PluginManager pluginManager = getServer().getPluginManager();
        pluginManager.registerEvents(new PlayerJoinListener(), this);
        pluginManager.registerEvents(new PlayerQuitListener(), this);
        pluginManager.registerEvents(new PlayerToggleSneakListener(), this);
        pluginManager.registerEvents(new PlayerItemHeldListener(), this);
        pluginManager.registerEvents(new PlayerArmSwingListener(), this);
        pluginManager.registerEvents(new PlayerDismountListener(), this);
        pluginManager.registerEvents(new PlayerKickListener(), this);

        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] message) {
        if (channel.equals("BungeeCord")) proxyPoller.handle(message);
    }

    @Override
    public void onDisable() {
        if (selectorManager != null) selectorManager.closeAll();
        if (protocolManager != null) protocolManager.removePacketListeners(this);
        try {
            if (database != null) database.closeConnection();
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

    public static Main getInstance() {
        return instance;
    }

    public ProtocolManager getProtocolManager() {
        return protocolManager;
    }

    /** The camera armor stand, looked up again if it was not loaded yet when the plugin enabled. */
    public ArmorStand getArmorStand() {
        if ((armorStand == null || !armorStand.isValid()) && database != null && database.getUUID() != null) {
            Entity entity = Bukkit.getEntity(UUID.fromString(database.getUUID()));
            armorStand = entity instanceof ArmorStand stand ? stand : null;
        }
        return armorStand;
    }

    public SelectorManager getSelectorManager() {
        return selectorManager;
    }
}
