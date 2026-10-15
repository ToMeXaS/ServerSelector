package lt.tomexas.serverselector.Listeners;

import lt.tomexas.serverselector.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;

public class PlayerKickListener implements Listener {

    private final Main plugin = Main.getInstance();

    // With the camera on another entity the client can target its own body when clicking, which the server kicks for
    @EventHandler
    public void onKick(PlayerKickEvent event) {
        if (event.getCause() == PlayerKickEvent.Cause.SELF_INTERACTION
                && plugin.getSelectorManager().isAttached(event.getPlayer())) {
            event.setCancelled(true);
        }
    }
}
