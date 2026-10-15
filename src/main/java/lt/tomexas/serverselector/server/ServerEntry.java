package lt.tomexas.serverselector.server;

import lt.tomexas.serverselector.selector.ui.Glyph;

import java.util.List;

/** A server as configured in config.yml. The id must match the proxy's server name. */
public record ServerEntry(String id, String displayName, Glyph icon, List<String> description) {
}
