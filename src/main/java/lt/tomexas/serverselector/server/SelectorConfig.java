package lt.tomexas.serverselector.server;

import lt.tomexas.serverselector.selector.ui.Glyph;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public record SelectorConfig(String title, List<ServerEntry> servers, double cursorBoundsX, double cursorBoundsY) {

    private static final String DEFAULT_TITLE = "CHOOSE YOUR REALM";
    private static final double DEFAULT_CURSOR_BOUNDS_X = 3.9;
    private static final double DEFAULT_CURSOR_BOUNDS_Y = 2.05;

    public static SelectorConfig from(ConfigurationSection root, Logger logger) {
        List<ServerEntry> servers = new ArrayList<>();
        ConfigurationSection section = root.getConfigurationSection("servers");
        if (section != null) {
            for (String id : section.getKeys(false)) {
                ConfigurationSection server = section.getConfigurationSection(id);
                if (server == null) continue;
                String iconId = server.getString("icon", "");
                Glyph icon = Glyph.fromId(iconId);
                if (icon == null) {
                    logger.warning("Unknown icon '" + iconId + "' for server " + id + ", using " + Glyph.ICON_STAR.id());
                    icon = Glyph.ICON_STAR;
                }
                servers.add(new ServerEntry(id, server.getString("display-name", id), icon,
                        List.copyOf(server.getStringList("description"))));
            }
        }
        return new SelectorConfig(root.getString("title", DEFAULT_TITLE), List.copyOf(servers),
                root.getDouble("cursor-bounds.x", DEFAULT_CURSOR_BOUNDS_X),
                root.getDouble("cursor-bounds.y", DEFAULT_CURSOR_BOUNDS_Y));
    }
}
