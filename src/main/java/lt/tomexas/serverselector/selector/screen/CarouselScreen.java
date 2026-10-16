package lt.tomexas.serverselector.selector.screen;

import lt.tomexas.serverselector.selector.screen.CarouselLayout.Slot;
import lt.tomexas.serverselector.selector.ui.Glyph;
import lt.tomexas.serverselector.selector.ui.Rect;
import lt.tomexas.serverselector.selector.ui.UiElement;
import lt.tomexas.serverselector.selector.ui.UiPlane;
import lt.tomexas.serverselector.server.ServerEntry;
import lt.tomexas.serverselector.server.ServerRegistry;
import lt.tomexas.serverselector.server.ServerStatus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** One focused server card in the middle, neighbours at the sides, arrows to rotate. */
public final class CarouselScreen implements Screen {

    private static final double DEPTH = 2.0;
    private static final double CONTENT_DEPTH = 1.99;
    private static final double CARD_Y = -0.1;
    private static final double CARD_WIDTH = 1.6;
    private static final double CARD_HEIGHT = 1.8;
    private static final float CARD_SCALE = 0.5f;          // 128x144 px glyph -> 1.6 x 1.8
    private static final double ICON_Y = 0.5;
    private static final float ICON_SCALE = 0.6f;
    private static final double NAME_Y = 0.1;
    private static final float NAME_SCALE = 0.9f;
    private static final double STATUS_Y = -0.15;
    private static final float STATUS_SCALE = 0.5f;
    private static final double DESCRIPTION_Y = -0.5;
    private static final float DESCRIPTION_SCALE = 0.42f;
    private static final double ARROW_X = 2.2;
    private static final float ARROW_SCALE = 0.9f;         // 24 px glyph -> 0.54
    private static final double ARROW_SIZE = 0.54;
    private static final float ARROW_HOVER_SCALE = 1.05f;
    private static final float CENTER_HOVER_GROW = 1.04f;
    private static final float SIDE_HOVER_GROW = 1.08f;
    private static final int SLIDE_TICKS = 6;
    private static final int HOVER_TICKS = 2;
    private static final int SIDE_ALPHA = 150;
    private static final int DIM_ALPHA = 80;
    private static final double SHAKE = 0.06;
    private static final int SHAKE_TICKS = 7;
    private static final int STATUS_REFRESH_TICKS = 20;
    private static final double PLAY_Y = -1.2;
    private static final float PLAY_SCALE = 0.35f;          // 120x40 px glyph -> 1.05 x 0.35
    private static final float PLAY_LABEL_SCALE = 0.6f;
    private static final double PLAY_LABEL_DEPTH = 1.98;
    private static final Rect PLAY = new Rect(0, PLAY_Y, 1.05, 0.35);

    private static final int NONE = -1;
    private static final int LEFT_ARROW = -2;
    private static final int RIGHT_ARROW = -3;
    private static final int PLAY_BUTTON = -4;

    private final Plugin plugin;
    private final Player player;
    private final UiPlane plane;
    private final ServerRegistry registry;
    private final Consumer<ServerEntry> onJoin;
    private final List<Card> cards = new ArrayList<>();
    private final UiElement leftArrow;
    private final UiElement rightArrow;
    private final UiElement playButton;
    private final UiElement playLabel;
    private int selected;
    private int hovered = NONE;
    private boolean dimmed;
    private int shakeTicks;
    private int ticks;

    public CarouselScreen(Plugin plugin, Player player, UiPlane plane, ServerRegistry registry, int selected,
                          Consumer<ServerEntry> onJoin) {
        this.plugin = plugin;
        this.player = player;
        this.plane = plane;
        this.registry = registry;
        this.onJoin = onJoin;

        List<ServerEntry> servers = registry.servers();
        this.selected = servers.isEmpty() ? 0 : CarouselLayout.wrap(selected, servers.size());
        for (int i = 0; i < servers.size(); i++) {
            cards.add(new Card(servers.get(i), CarouselLayout.slot(i, this.selected, servers.size())));
        }

        leftArrow = UiElement.glyph(plugin, player, plane, Glyph.ARROW_LEFT, -ARROW_X, CARD_Y, DEPTH, ARROW_SCALE);
        rightArrow = UiElement.glyph(plugin, player, plane, Glyph.ARROW_RIGHT, ARROW_X, CARD_Y, DEPTH, ARROW_SCALE);
        boolean arrows = servers.size() > 1;
        leftArrow.visible(arrows);
        rightArrow.visible(arrows);

        playButton = UiElement.glyph(plugin, player, plane, Glyph.PLAY, 0, PLAY_Y, CONTENT_DEPTH, PLAY_SCALE);
        playLabel = UiElement.spawn(plugin, player, plane, Component.text("PLAY", NamedTextColor.WHITE, TextDecoration.BOLD),
                1, 0, PLAY_Y, PLAY_LABEL_DEPTH, PLAY_LABEL_SCALE);
        playLabel.shadow(true);
        boolean hasServers = !servers.isEmpty();
        playButton.visible(hasServers);
        playLabel.visible(hasServers);
        stylePlayButton();
    }

    public int selectedIndex() {
        return selected;
    }

    @Override
    public void tick() {
        if (shakeTicks > 0 && !cards.isEmpty()) {
            shakeTicks--;
            double offset = shakeTicks == 0 ? 0 : (shakeTicks % 2 == 0 ? SHAKE : -SHAKE);
            cards.get(selected).place(1, offset);
        }
        if (++ticks % STATUS_REFRESH_TICKS == 0) {
            cards.forEach(Card::refreshStatus);
            stylePlayButton();
        }
    }

    @Override
    public boolean hover(double x, double y) {
        int target = dimmed ? NONE : targetAt(x, y);
        if (target == hovered) return target != NONE;
        int previous = hovered;
        hovered = target;
        restyle(previous);
        restyle(target);
        if (target != NONE) player.playSound(player, Sound.BLOCK_NOTE_BLOCK_HAT, 0.3f, 1.6f);
        return target != NONE;
    }

    @Override
    public void click(double x, double y) {
        if (dimmed) return;
        int target = targetAt(x, y);
        if (target == LEFT_ARROW) rotate(-1);
        else if (target == RIGHT_ARROW) rotate(1);
        else if (target == PLAY_BUTTON) join();
        else if (target >= 0 && target != selected) rotate(CarouselLayout.offset(target, selected, cards.size()));
    }

    public void scroll(int direction) {
        if (!dimmed && cards.size() > 1) rotate(direction);
    }

    /** Fades the carousel behind an overlay and stops it reacting to the cursor. */
    public void dim(boolean dimmed) {
        if (this.dimmed == dimmed) return;
        this.dimmed = dimmed;
        hovered = NONE;
        cards.forEach(card -> card.place(HOVER_TICKS, 0));
        int alpha = dimmed ? DIM_ALPHA : 255;
        leftArrow.opacity(alpha);
        rightArrow.opacity(alpha);
        playButton.opacity(alpha);
        playLabel.opacity(alpha);
        leftArrow.moveTo(-ARROW_X, CARD_Y, ARROW_SCALE, HOVER_TICKS);
        rightArrow.moveTo(ARROW_X, CARD_Y, ARROW_SCALE, HOVER_TICKS);
        stylePlayButton();
    }

    @Override
    public void close() {
        cards.forEach(Card::remove);
        leftArrow.remove();
        rightArrow.remove();
        playButton.remove();
        playLabel.remove();
    }

    private void rotate(int steps) {
        if (cards.isEmpty()) return;
        selected = CarouselLayout.wrap(selected + steps, cards.size());
        hovered = NONE;
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            card.slot = CarouselLayout.slot(i, selected, cards.size());
            card.place(SLIDE_TICKS, 0);
        }
        stylePlayButton();
        player.playSound(player, Sound.ITEM_BOOK_PAGE_TURN, 0.6f, 1.2f);
    }

    private void join() {
        Card card = cards.get(selected);
        if (registry.live(card.server.id()).status().joinable()) {
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
            onJoin.accept(card.server);
        } else {
            shakeTicks = SHAKE_TICKS;
            player.playSound(player, Sound.ENTITY_VILLAGER_NO, 0.6f, 1.0f);
        }
    }

    private int targetAt(double x, double y) {
        if (cards.size() > 1) {
            if (new Rect(-ARROW_X, CARD_Y, ARROW_SIZE, ARROW_SIZE).contains(x, y)) return LEFT_ARROW;
            if (new Rect(ARROW_X, CARD_Y, ARROW_SIZE, ARROW_SIZE).contains(x, y)) return RIGHT_ARROW;
        }
        if (cards.isEmpty()) return NONE;
        if (PLAY.contains(x, y)) return PLAY_BUTTON;
        // The centre card is drawn over the side cards, so it wins where they overlap
        if (cards.get(selected).rect().contains(x, y)) return selected;
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            if (i != selected && CarouselLayout.visible(card.slot) && card.rect().contains(x, y)) return i;
        }
        return NONE;
    }

    private void restyle(int target) {
        if (target == LEFT_ARROW) {
            leftArrow.moveTo(-ARROW_X, CARD_Y, hovered == LEFT_ARROW ? ARROW_HOVER_SCALE : ARROW_SCALE, HOVER_TICKS);
        } else if (target == RIGHT_ARROW) {
            rightArrow.moveTo(ARROW_X, CARD_Y, hovered == RIGHT_ARROW ? ARROW_HOVER_SCALE : ARROW_SCALE, HOVER_TICKS);
        } else if (target == PLAY_BUTTON) {
            stylePlayButton();
        } else if (target >= 0) {
            cards.get(target).place(HOVER_TICKS, 0);
        }
    }

    /** Glyph/label follow the centre server's joinability and hover state. */
    private void stylePlayButton() {
        if (cards.isEmpty()) return;
        boolean joinable = registry.live(cards.get(selected).server.id()).status().joinable();
        boolean hover = hovered == PLAY_BUTTON;
        Glyph glyph = !joinable ? Glyph.PLAY_DISABLED : hover ? Glyph.PLAY_HOVER : Glyph.PLAY;
        playButton.text(glyph.component());
        NamedTextColor labelColor = joinable ? NamedTextColor.WHITE : NamedTextColor.GRAY;
        playLabel.text(Component.text("PLAY", labelColor, TextDecoration.BOLD));
    }

    private Component statusLine(ServerEntry server) {
        ServerRegistry.Live live = registry.live(server.id());
        NamedTextColor color = switch (live.status()) {
            case ONLINE -> NamedTextColor.GREEN;
            case LIMITED -> NamedTextColor.YELLOW;
            case OFFLINE, UNKNOWN -> NamedTextColor.RED;
        };
        String label = live.status() == ServerStatus.UNKNOWN ? "Checking..." : live.label();
        Component line = Component.text("\u25CF " + label, color);
        if (live.status().joinable()) {
            line = line.append(Component.text("  \u00B7  " + live.players() + " playing", NamedTextColor.GRAY));
        }
        return line;
    }

    private final class Card {

        private final ServerEntry server;
        private final UiElement background;
        private final UiElement icon;
        private final UiElement name;
        private final UiElement status;
        private final UiElement description;
        private Slot slot;

        private Card(ServerEntry server, Slot slot) {
            this.server = server;
            this.slot = slot;
            double x = CarouselLayout.x(slot);
            float s = CarouselLayout.scale(slot);
            background = UiElement.glyph(plugin, player, plane, slot == Slot.CENTER ? Glyph.CARD_SELECTED : Glyph.CARD,
                    x, CARD_Y, DEPTH, CARD_SCALE * s);
            icon = UiElement.spawn(plugin, player, plane, server.icon().component(), 1,
                    x, CARD_Y + ICON_Y * s, CONTENT_DEPTH, ICON_SCALE * s);
            name = UiElement.spawn(plugin, player, plane,
                    Component.text(server.displayName(), NamedTextColor.WHITE, TextDecoration.BOLD), 1,
                    x, CARD_Y + NAME_Y * s, CONTENT_DEPTH, NAME_SCALE * s);
            status = UiElement.spawn(plugin, player, plane, statusLine(server), 1,
                    x, CARD_Y + STATUS_Y * s, CONTENT_DEPTH, STATUS_SCALE * s);
            description = UiElement.spawn(plugin, player, plane,
                    Component.text(String.join("\n", server.description()), NamedTextColor.GRAY),
                    Math.max(1, server.description().size()),
                    x, CARD_Y + DESCRIPTION_Y * s, CONTENT_DEPTH, DESCRIPTION_SCALE * s);
            name.shadow(true);
            status.shadow(true);
            description.shadow(true);
            place(0, 0);
        }

        private void place(int ticks, double shake) {
            boolean center = slot == Slot.CENTER;
            boolean shown = CarouselLayout.visible(slot);
            boolean hover = !dimmed && hovered == cards.indexOf(this);
            float s = CarouselLayout.scale(slot) * (hover ? (center ? CENTER_HOVER_GROW : SIDE_HOVER_GROW) : 1f);
            double x = CarouselLayout.x(slot) + shake;

            background.text((center ? Glyph.CARD_SELECTED : Glyph.CARD).component());
            background.moveTo(x, CARD_Y, CARD_SCALE * s, ticks);
            icon.moveTo(x, CARD_Y + ICON_Y * s, ICON_SCALE * s, ticks);
            name.moveTo(x, CARD_Y + NAME_Y * s, NAME_SCALE * s, ticks);
            status.moveTo(x, CARD_Y + STATUS_Y * s, STATUS_SCALE * s, ticks);
            description.moveTo(x, CARD_Y + DESCRIPTION_Y * s, DESCRIPTION_SCALE * s, ticks);

            background.visible(shown);
            icon.visible(shown);
            name.visible(shown);
            status.visible(shown && center);
            description.visible(shown && center);

            int alpha = dimmed ? DIM_ALPHA : (center || hover ? 255 : SIDE_ALPHA);
            for (UiElement element : List.of(background, icon, name, status, description)) element.opacity(alpha);
        }

        private void refreshStatus() {
            status.text(statusLine(server));
        }

        private Rect rect() {
            float s = CarouselLayout.scale(slot);
            return new Rect(CarouselLayout.x(slot), CARD_Y, CARD_WIDTH * s, CARD_HEIGHT * s);
        }

        private void remove() {
            for (UiElement element : List.of(background, icon, name, status, description)) element.remove();
        }
    }
}
