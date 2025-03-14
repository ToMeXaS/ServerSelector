package lt.tomexas.serverselector.Utils;

import lt.tomexas.serverselector.Main;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class HudManager {

    private final Main plugin = Main.getInstance();
    private final Map<Player, BossBar> playerHud = plugin.getPlayerHud();

    private final Map<Player, Integer> playerServerIndex = new HashMap<>();

    private final Component bossBarTitle = MiniMessage.miniMessage().deserialize("<font:hud>" +
            "\uE003"
            + Spacer.getNegativeSpacer(1200)
            + Spacer.getPositiveSpacer(70) +
            "\uE000"
            + Spacer.getNegativeSpacer(200) +
            "\uE001"
            + Spacer.getPositiveSpacer(50) +
            "\uE002</font><font:hud_desc>"
            + Spacer.getPositiveSpacer(30)
            + Spacer.getNegativeSpacer(345) +
            "<gray>A world of adventure awaits! Choose your server and continue building,</gray></font><font:hud_desc2>"
            + Spacer.getNegativeSpacer(350) +
            "<gray>exploring, and conquering. Whether you're crafting your legacy or</gray></font><font:hud_desc3>"
            + Spacer.getNegativeSpacer(330) +
            "<gray>battling through challenges, today is another chance to create</gray></font><font:hud_desc4>"
            + Spacer.getNegativeSpacer(215) +
            "<gray>something legendary.</gray></font><font:hud_realm>"
            + Spacer.getNegativeSpacer(163) +
            "Pick your realm and make it unforgettable!</font><font:hud_buttons>"
            + Spacer.getNegativeSpacer(250) +
            "\uE007"
            + Spacer.getPositiveSpacer(20) +
            "\uE004</font>");

    public void sendHud(Player player) {
        if (playerHud.get(player) == null) return;
        player.showBossBar(playerHud.get(player));
    }

    public Component getBossBarTitle() {
        return bossBarTitle;
    }

    public Map<Player, Integer> getPlayerServerIndex() {
        return playerServerIndex;
    }

}
