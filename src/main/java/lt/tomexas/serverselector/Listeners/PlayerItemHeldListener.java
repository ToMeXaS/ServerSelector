package lt.tomexas.serverselector.Listeners;

import lt.tomexas.serverselector.Main;
import lt.tomexas.serverselector.Utils.HudManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;

import java.util.List;

public class PlayerItemHeldListener implements Listener {

    private final Main plugin = Main.getInstance();
    private final HudManager hudManager = plugin.getHudManager();

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        int currentIndex = hudManager.getPlayerServerIndex().get(player);
        List<String> servers = plugin.getServers();
        event.setCancelled(true);

        if (plugin.getPlayerQueueStatus().getOrDefault(player.getUniqueId(), false)) return;

        if (event.getNewSlot() > event.getPreviousSlot()) {
            currentIndex = (currentIndex + 1) % servers.size();
        } else {
            currentIndex = (currentIndex - 1 + servers.size()) % servers.size();
        }

        hudManager.getPlayerServerIndex().put(player, currentIndex);
        plugin.getCursorManager().snapTo(player, currentIndex);
    }

}
