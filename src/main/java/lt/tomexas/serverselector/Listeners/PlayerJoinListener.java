package lt.tomexas.serverselector.Listeners;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import lt.tomexas.serverselector.Main;
import lt.tomexas.serverselector.Utils.HudManager;
import lt.tomexas.serverselector.Utils.Spacer;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class PlayerJoinListener implements Listener {

    private final Main plugin = Main.getInstance();
    private final HudManager hudManager = plugin.getHudManager();

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Increase delay to ensure entity is properly spawned
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PacketContainer packet = plugin.getProtocolManager().createPacket(PacketType.Play.Server.CAMERA);
            packet.getIntegers().write(0, plugin.getArmorStand().getEntityId());
            plugin.getProtocolManager().sendServerPacket(player, packet);

            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("GetServers");
            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());

            player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999, 1, false, false));

            BossBar bossBar = BossBar.bossBar(hudManager.getBossBarTitle(), 0, BossBar.Color.YELLOW, BossBar.Overlay.PROGRESS);

            plugin.getPlayerHud().put(player, bossBar);
            hudManager.getPlayerServerIndex().put(player, 0);
            hudManager.sendHud(player);
        }, 10L);
    }
}
