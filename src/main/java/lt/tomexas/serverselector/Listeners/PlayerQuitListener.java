package lt.tomexas.serverselector.Listeners;

import lt.tomexas.serverselector.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final Main plugin = Main.getInstance();

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getSelectorManager().close(event.getPlayer());
    }
}
