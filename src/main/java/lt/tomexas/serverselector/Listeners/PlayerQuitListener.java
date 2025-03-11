package lt.tomexas.serverselector.Listeners;

import lt.tomexas.serverselector.Main;
import lt.tomexas.serverselector.Utils.HudManager;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final Main plugin = Main.getInstance();
    private final HudManager hudManager = plugin.getHudManager();

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        plugin.getPlayerHud().remove(player);
        hudManager.getPlayerServerIndex().remove(player);
    }
}
