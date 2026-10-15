package lt.tomexas.serverselector.Listeners;

import io.papermc.paper.event.player.PlayerArmSwingEvent;
import lt.tomexas.serverselector.Main;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;

public class PlayerArmSwingListener implements Listener {

    private final Main plugin = Main.getInstance();

    // A left click shows up as an arm swing
    @EventHandler
    public void onPlayerInteract(PlayerArmSwingEvent event) {
        if (event.getHand() == EquipmentSlot.HAND) plugin.getSelectorManager().click(event.getPlayer());
    }
}
