package lt.tomexas.serverselector.Listeners;

import lt.tomexas.serverselector.Main;
import lt.tomexas.serverselector.Queue.Events.QueueJoinEvent;
import lt.tomexas.serverselector.Utils.HudManager;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class QueueJoinListener implements Listener {

    private final Main plugin = Main.getInstance();
    private final HudManager hudManager = plugin.getHudManager();

    @EventHandler
    public void onQueueJoin(QueueJoinEvent event) {
        Player player = event.getPlayer();
        BossBar bossBar = plugin.getPlayerHud().get(player);
        Component component = hudManager.getPlayerBarTitle().get(player);
        hudManager.getPlayerQueueTitle().put(player, component);
        bossBar.name(component);
    }

}
