package lt.tomexas.serverselector.Listeners;

import lt.tomexas.serverselector.Main;
import lt.tomexas.serverselector.Utils.HudManager;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;

import java.util.List;

public class PlayerItemHeldListener implements Listener {

    private final Main plugin = Main.getInstance();
    private final HudManager hudManager = plugin.getHudManager();
    private final Component originalBossBarTitle = plugin.getHudManager().getBossBarTitle().replaceText(builder ->
            builder.matchLiteral("\uE007").replacement("\uE006"));;

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        int currentIndex = hudManager.getPlayerServerIndex().get(player);
        List<String> servers = plugin.getServers();
        event.setCancelled(true);

        if (event.getNewSlot() > event.getPreviousSlot()) {
            currentIndex = (currentIndex + 1) % servers.size();
        } else {
            currentIndex = (currentIndex - 1 + servers.size()) % servers.size();
        }

        hudManager.getPlayerServerIndex().put(player, currentIndex);
        String currentServer = servers.get(currentIndex);
        BossBar bossBar = plugin.getPlayerHud().get(player);
        bossBar.name(getBossBarTitleForServer(currentServer));
        //player.sendMessage("You scrolled to: " + currentServer);
    }

    private Component getBossBarTitleForServer(String server) {
        return switch (server.toLowerCase()) {
            case "skyblock" -> originalBossBarTitle.replaceText(builder ->
                    builder.matchLiteral("\uE004").replacement("\uE005"));
            case "nations" -> originalBossBarTitle.replaceText(builder ->
                    builder.matchLiteral("\uE006").replacement("\uE007"));
            default -> originalBossBarTitle;
        };
    }

}
