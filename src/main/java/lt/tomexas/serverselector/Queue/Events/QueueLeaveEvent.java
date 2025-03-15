package lt.tomexas.serverselector.Queue.Events;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class QueueLeaveEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;

    public QueueLeaveEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
}
