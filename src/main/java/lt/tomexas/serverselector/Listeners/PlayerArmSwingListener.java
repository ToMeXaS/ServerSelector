package lt.tomexas.serverselector.Listeners;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import io.papermc.paper.event.player.PlayerArmSwingEvent;
import lt.tomexas.serverselector.Main;
import lt.tomexas.serverselector.Utils.HudManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;

public class PlayerArmSwingListener implements Listener {

    private final Main plugin = Main.getInstance();
    private final HudManager hudManager = plugin.getHudManager();

    @EventHandler
    public void onPlayerInteract(PlayerArmSwingEvent event) {
        Player player = event.getPlayer();
        if (event.getHand().equals(EquipmentSlot.HAND)) {
            int currentIndex = hudManager.getPlayerServerIndex().get(player);
            ByteArrayDataOutput stream = ByteStreams.newDataOutput();
            stream.writeUTF("Connect");
            stream.writeUTF(plugin.getServers().get(currentIndex));
            player.sendPluginMessage(plugin, "BungeeCord", stream.toByteArray());
        }
    }

}
