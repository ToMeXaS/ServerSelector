package lt.tomexas.serverselector.Listeners;

import lt.tomexas.serverselector.Main;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;

public class PlayerItemHeldListener implements Listener {

    private final Main plugin = Main.getInstance();

    // Scrolling the hotbar rotates the carousel
    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!plugin.getSelectorManager().isAttached(player)) return;
        event.setCancelled(true);

        int step = event.getNewSlot() - event.getPreviousSlot();
        // Scrolling past slot 8 wraps to 0 and the other way round
        if (step > 4) step = -1;
        else if (step < -4) step = 1;
        plugin.getSelectorManager().scroll(player, Integer.signum(step));
    }
}
