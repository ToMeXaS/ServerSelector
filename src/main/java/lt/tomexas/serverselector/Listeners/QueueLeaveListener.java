package lt.tomexas.serverselector.Listeners;

import lt.tomexas.serverselector.Main;
import lt.tomexas.serverselector.Queue.Events.QueueLeaveEvent;
import lt.tomexas.serverselector.Utils.HudManager;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class QueueLeaveListener implements Listener {

    private final Main plugin = Main.getInstance();
    private final HudManager hudManager = plugin.getHudManager();

    @EventHandler
    public void onQueueLeave(QueueLeaveEvent event) {
        Player player = event.getPlayer();
        BossBar bossBar = plugin.getPlayerHud().get(player);
        Component component = hudManager.getPlayerBarTitle().get(player);
        bossBar.name(component);
    }
}
