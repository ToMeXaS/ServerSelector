package lt.tomexas.serverselector.selector.screen;

import lt.tomexas.serverselector.selector.screen.CarouselLayout.Slot;
import lt.tomexas.serverselector.selector.ui.CardArt;
import lt.tomexas.serverselector.selector.ui.Glyph;
import lt.tomexas.serverselector.selector.ui.Rect;
import lt.tomexas.serverselector.selector.ui.TextWidth;
import lt.tomexas.serverselector.selector.ui.UiElement;
import lt.tomexas.serverselector.selector.ui.UiMath;
import lt.tomexas.serverselector.selector.ui.UiPlane;
import lt.tomexas.serverselector.server.ServerEntry;
import lt.tomexas.serverselector.server.ServerRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** One focused server card in the middle, neighbours at the sides. */
public final class CarouselScreen implements Screen {

    private static final double DEPTH = 2.0;
    private static final double CONTENT_DEPTH = 1.99;
    // Every card hangs from the same top edge, so cards of different heights line up
    private static final double CARD_TOP = 0.9;
    // Content positions, measured down from the card's top edge at full size
    private static final double ICON_DOWN = 0.4;
    private static final float ICON_SCALE = 0.6f;
    private static final double NAME_DOWN = 0.85;
    private static final float NAME_SCALE = 0.9f;
    private static final double STATUS_DOWN = 1.08;
    private static final float STATUS_SCALE = 0.5f;
    private static final double DESCRIPTION_DOWN = 1.25;
    private static final float DESCRIPTION_SCALE = 0.42f;
    private static final double DESCRIPTION_LINE = 10 * UiMath.PIXEL * DESCRIPTION_SCALE;
    // Room under the description: bottom padding plus the art's 3D lip
    private static final double BOTTOM_PADDING = 0.2 + 0.075;
    private static final double SIDE_PADDING = 0.15;
    // Widest the status line can get, so a card never resizes as its player count changes
    private static final String WIDEST_STATUS = "\u25CF Online  \u00B7  999 playing";
    private static final float CENTER_HOVER_GROW = 1.04f;
    private static final float SIDE_HOVER_GROW = 1.08f;
    private static final int SLIDE_TICKS = 6;
    private static final int HOVER_TICKS = 2;
    private static final int SIDE_ALPHA = 150;
    private static final double SHAKE = 0.06;
    private static final int SHAKE_TICKS = 7;
    private static final int STATUS_REFRESH_TICKS = 20;
    // Gap between the bottom of the screen and the PLAY button's centre
    private static final double PLAY_MARGIN = 0.3;
    private static final float PLAY_SCALE = 0.35f;          // 120x40 px glyph -> 1.05 x 0.35
    private static final double PLAY_WIDTH = 1.05;
    private static final double PLAY_HEIGHT = 0.35;
    private static final float PLAY_LABEL_SCALE = 0.6f;
    private static final double PLAY_LABEL_DEPTH = 1.98;

    private static final int NONE = -1;
    private static final int PLAY_BUTTON = -4;

    private final Plugin plugin;
    private final Player player;
    private final UiPlane plane;
    private final ServerRegistry registry;
    private final Consumer<ServerEntry> onPlay;
    private final List<Card> cards = new ArrayList<>();
    private final UiElement playButton;
    private final UiElement playLabel;
    private final Rect play;
    private int selected;
    private int hovered = NONE;
    private int shakeTicks;
    private int ticks;

    /** {@code screenBottom} is how far below the centre the screen ends, in units (the cursor's y-bound). */
    public CarouselScreen(Plugin plugin, Player player, UiPlane plane, ServerRegistry registry, int selected,
                          double screenBottom, Consumer<ServerEntry> onPlay) {
        this.plugin = plugin;
        this.player = player;
        this.plane = plane;
        this.registry = registry;
        this.onPlay = onPlay;

        List<ServerEntry> servers = registry.servers();
        this.selected = servers.isEmpty() ? 0 : CarouselLayout.wrap(selected, servers.size());
        // Sizes first: where a card sits depends on how wide the centre card is
        List<CardArt.Size> sizes = servers.stream().map(CarouselScreen::cardSize).toList();
        double centerWidth = servers.isEmpty() ? 0 : sizes.get(this.selected).width();
        for (int i = 0; i < servers.size(); i++) {
            cards.add(new Card(servers.get(i), sizes.get(i), CarouselLayout.slot(i, this.selected, servers.size()),
                    centerWidth));
        }

        double playY = -screenBottom + PLAY_MARGIN;
        play = new Rect(0, playY, PLAY_WIDTH, PLAY_HEIGHT);
        playButton = UiElement.glyph(plugin, player, plane, Glyph.PLAY, 0, playY, CONTENT_DEPTH, PLAY_SCALE);
        playLabel = UiElement.spawn(plugin, player, plane, Component.text("PLAY", NamedTextColor.WHITE, TextDecoration.BOLD),
                1, 0, playY, PLAY_LABEL_DEPTH, PLAY_LABEL_SCALE);
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
        int target = targetAt(x, y);
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
        int target = targetAt(x, y);
        if (target == PLAY_BUTTON) play();
        else if (target >= 0 && target != selected) rotate(CarouselLayout.offset(target, selected, cards.size()));
    }

    public void scroll(int direction) {
        if (cards.size() > 1) rotate(direction);
    }

    @Override
    public void close() {
        cards.forEach(Card::remove);
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

    private void play() {
        Card card = cards.get(selected);
        if (registry.live(card.server.id()).status().joinable()) {
            player.playSound(player, Sound.UI_BUTTON_CLICK, 0.6f, 1.0f);
            onPlay.accept(card.server);
        } else {
            shakeTicks = SHAKE_TICKS;
            player.playSound(player, Sound.ENTITY_VILLAGER_NO, 0.6f, 1.0f);
        }
    }

    private int targetAt(double x, double y) {
        if (cards.isEmpty()) return NONE;
        if (play.contains(x, y)) return PLAY_BUTTON;
        // The centre card is drawn over the side cards, so it wins where they overlap
        if (cards.get(selected).rect().contains(x, y)) return selected;
        for (int i = 0; i < cards.size(); i++) {
            Card card = cards.get(i);
            if (i != selected && CarouselLayout.visible(card.slot) && card.rect().contains(x, y)) return i;
        }
        return NONE;
    }

    private void restyle(int target) {
        if (target == PLAY_BUTTON) {
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

    /** The smallest card art the server's name, status and description fit on. */
    private static CardArt.Size cardSize(ServerEntry server) {
        double width = textWidth(server.displayName(), true, NAME_SCALE);
        width = Math.max(width, textWidth(WIDEST_STATUS, false, STATUS_SCALE));
        for (String line : server.description()) width = Math.max(width, textWidth(line, false, DESCRIPTION_SCALE));
        double height = DESCRIPTION_DOWN + descriptionLines(server) * DESCRIPTION_LINE + BOTTOM_PADDING;
        return CardArt.fit(width + 2 * SIDE_PADDING, height);
    }

    private static double textWidth(String text, boolean bold, float scale) {
        return TextWidth.of(text, bold) * UiMath.PIXEL * scale;
    }

    private static int descriptionLines(ServerEntry server) {
        return Math.max(1, server.description().size());
    }

    private static double slotX(Slot slot, double centerWidth, double width) {
        return switch (slot) {
            case CENTER -> 0.0;
            case LEFT -> -CarouselLayout.sideX(centerWidth, width);
            case RIGHT -> CarouselLayout.sideX(centerWidth, width);
            case HIDDEN_LEFT -> -CarouselLayout.hiddenX(centerWidth);
            case HIDDEN_RIGHT -> CarouselLayout.hiddenX(centerWidth);
        };
    }

    private Component statusLine(ServerEntry server) {
        ServerRegistry.Live live = registry.live(server.id());
        NamedTextColor color = switch (live.status()) {
            case ONLINE -> NamedTextColor.GREEN;
            case OFFLINE, UNKNOWN -> NamedTextColor.RED;
        };
        String label = switch (live.status()) {
            case ONLINE -> "Online";
            case OFFLINE -> "Offline";
            case UNKNOWN -> "Checking...";
        };
        Component line = Component.text("\u25CF " + label, color);
        if (live.status().joinable()) {
            line = line.append(Component.text("  \u00B7  " + live.players() + " playing", NamedTextColor.GRAY));
        }
        return line;
    }

    private final class Card {

        private final ServerEntry server;
        private final CardArt.Size size;
        private final double centerY;
        // Content offsets from the card's centre at full size; side cards scale them about that centre
        private final double iconOffset;
        private final double nameOffset;
        private final double statusOffset;
        private final double descriptionOffset;
        private final UiElement background;
        private final UiElement icon;
        private final UiElement name;
        private final UiElement status;
        private final UiElement description;
        private Slot slot;

        private Card(ServerEntry server, CardArt.Size size, Slot slot, double centerWidth) {
            this.server = server;
            this.size = size;
            this.slot = slot;
            centerY = CARD_TOP - size.height() / 2;
            int lines = descriptionLines(server);
            iconOffset = CARD_TOP - ICON_DOWN - centerY;
            nameOffset = CARD_TOP - NAME_DOWN - centerY;
            statusOffset = CARD_TOP - STATUS_DOWN - centerY;
            descriptionOffset = CARD_TOP - DESCRIPTION_DOWN - lines * DESCRIPTION_LINE / 2 - centerY;

            // The card list is still being built, so the centre card's width is passed in
            double x = slotX(slot, centerWidth, size.width());
            float s = CarouselLayout.scale(slot);
            background = UiElement.spawn(plugin, player, plane, CardArt.component(size, slot == Slot.CENTER), 1,
                    x, centerY, DEPTH, CardArt.PIXEL_SCALE * s);
            icon = UiElement.spawn(plugin, player, plane, server.icon().component(), 1,
                    x, centerY + iconOffset * s, CONTENT_DEPTH, ICON_SCALE * s);
            name = UiElement.spawn(plugin, player, plane,
                    Component.text(server.displayName(), NamedTextColor.WHITE, TextDecoration.BOLD), 1,
                    x, centerY + nameOffset * s, CONTENT_DEPTH, NAME_SCALE * s);
            status = UiElement.spawn(plugin, player, plane, statusLine(server), 1,
                    x, centerY + statusOffset * s, CONTENT_DEPTH, STATUS_SCALE * s);
            description = UiElement.spawn(plugin, player, plane,
                    Component.text(String.join("\n", server.description()), NamedTextColor.GRAY), lines,
                    x, centerY + descriptionOffset * s, CONTENT_DEPTH, DESCRIPTION_SCALE * s);
            name.shadow(true);
            status.shadow(true);
            description.shadow(true);
            style(0, false, s, x);
        }

        private void place(int ticks, double shake) {
            boolean center = slot == Slot.CENTER;
            boolean hover = hovered == cards.indexOf(this);
            float s = CarouselLayout.scale(slot) * (hover ? (center ? CENTER_HOVER_GROW : SIDE_HOVER_GROW) : 1f);
            style(ticks, hover, s, x() + shake);
        }

        private void style(int ticks, boolean hover, float s, double x) {
            boolean center = slot == Slot.CENTER;
            boolean shown = CarouselLayout.visible(slot);

            background.text(CardArt.component(size, center));
            background.moveTo(x, centerY, CardArt.PIXEL_SCALE * s, ticks);
            icon.moveTo(x, centerY + iconOffset * s, ICON_SCALE * s, ticks);
            name.moveTo(x, centerY + nameOffset * s, NAME_SCALE * s, ticks);
            status.moveTo(x, centerY + statusOffset * s, STATUS_SCALE * s, ticks);
            description.moveTo(x, centerY + descriptionOffset * s, DESCRIPTION_SCALE * s, ticks);

            background.visible(shown);
            icon.visible(shown);
            name.visible(shown);
            status.visible(shown && center);
            description.visible(shown);

            int alpha = center || hover ? 255 : SIDE_ALPHA;
            for (UiElement element : List.of(background, icon, name, status, description)) element.opacity(alpha);
        }

        private double x() {
            return slotX(slot, cards.get(selected).size.width(), size.width());
        }

        private void refreshStatus() {
            status.text(statusLine(server));
        }

        private Rect rect() {
            float s = CarouselLayout.scale(slot);
            return new Rect(x(), centerY, size.width() * s, size.height() * s);
        }

        private void remove() {
            for (UiElement element : List.of(background, icon, name, status, description)) element.remove();
        }
    }
}
