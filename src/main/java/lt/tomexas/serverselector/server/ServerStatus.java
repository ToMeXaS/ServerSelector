package lt.tomexas.serverselector.server;

import java.util.Locale;
import java.util.Set;

/** A server's availability, as reported by ajQueue's status placeholder. */
public enum ServerStatus {
    ONLINE(true),
    // Full, whitelisted, paused or restricted: ajQueue still queues players for these
    LIMITED(true),
    OFFLINE(false),
    UNKNOWN(false);

    private static final Set<String> LIMITED_LABELS = Set.of("full", "whitelisted", "paused", "restricted");

    private final boolean joinable;

    ServerStatus(boolean joinable) {
        this.joinable = joinable;
    }

    public boolean joinable() {
        return joinable;
    }

    /** Maps ajQueue's status text (e.g. "&aOnline") to a status. Anything unrecognised counts as offline. */
    public static ServerStatus fromAjQueue(String raw) {
        String plain = plainText(raw).toLowerCase(Locale.ROOT);
        if (plain.isEmpty()) return UNKNOWN;
        if (plain.equals("online")) return ONLINE;
        if (LIMITED_LABELS.contains(plain)) return LIMITED;
        return OFFLINE;
    }

    /** Strips legacy colour codes and MiniMessage tags. */
    public static String plainText(String raw) {
        if (raw == null) return "";
        return raw.replaceAll("(?i)[&\u00A7][0-9a-fk-orx]", "").replaceAll("<[^>]+>", "").trim();
    }
}
