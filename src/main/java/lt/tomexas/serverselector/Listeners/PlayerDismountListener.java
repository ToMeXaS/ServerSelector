package lt.tomexas.serverselector.Listeners;

import lt.tomexas.serverselector.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;

public class PlayerDismountListener implements Listener {

    private final Main plugin = Main.getInstance();

    // Keeps players seated while the selector is open, otherwise their rotation stops reaching the server
    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        if (event.getEntity() instanceof Player player && plugin.getCursorManager().isAttached(player)) {
            event.setCancelled(true);
        }
    }
}
