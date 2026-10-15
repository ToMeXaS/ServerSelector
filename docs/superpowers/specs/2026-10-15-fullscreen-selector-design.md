# Fullscreen Server Selector — Design

Date: 2026-10-15
Status: Approved in brainstorming, pending spec review

## Goal

Replace the bossbar HUD with a fullscreen, mouse-driven server selector rendered in the world in front of
the player's locked camera. Players see a carousel of servers with live status, click one to join its
ajQueue queue, and see their queue position in an overlay panel.

## Decisions

| Topic | Decision |
|---|---|
| Layout | Carousel: one focused card in the middle, neighbours at the sides |
| Background | The lobby world stays visible behind the UI, under a dark vignette |
| Per-server info | Icon, name, short description, live player count, online/offline status |
| Queue state | A centred queue panel over the dimmed carousel |
| Art | Nexo glyphs built from CC0 Kenney assets (Cursor Pack, UI Pack, Game Icons) |
| Rendering | Separate per-player display entities for each UI element (no single font-hacked string) |

## How it works (input)

Proven in the current prototype (`CursorManager`):

- The player rides an invisible, silent, no-AI pig whose eyes are placed at the camera armor stand's eye
  location and rotation. The camera packet (ProtocolLib) targets **that pig**: a riding client keeps
  sending its head rotation while the camera is on its own vehicle, but drops a camera on any other entity.
- Cursor position = `distance × tan(angle)`, where the angle is the head yaw relative to the camera's yaw
  (normalised to ±180°) and the negated pitch. Each angle is clamped to ±80°, and the result is clamped
  to the panel bounds.
- Clicking = `PlayerArmSwingEvent` (main hand). The `SELF_INTERACTION` kick is cancelled while a
  session is open, because the client can target its own body when the camera is on another entity.
- Dismounting is cancelled while a session is open.

## Screens

### Carousel

- The centre card is the selected server: full size, outlined, showing icon, name, status pill,
  player count and description.
- Neighbour cards sit about 70% size and dimmed at the sides, showing only icon and name. With more than
  three servers only the two neighbours show. Navigation wraps around.
- Rotation: click an arrow, click a side card, or use the scroll wheel (`PlayerItemHeldEvent`, cancelled).
  Cards slide and resize using display interpolation over about 6 ticks.
- Hover: the element under the cursor lights up with a soft tick sound, and the cursor switches from
  the arrow glyph to the hand glyph over clickables.
- Clicking the centre card:
  - Online: `AjQueueSpigotAPI.sudoQueue(uuid, server)`, then open the queue panel.
  - Offline: the card shakes (a short translation animation) and a denied sound plays. Nothing else happens.
- 250 ms click debounce.
- The last centred server is remembered per player for the session.

### Queue panel

- The carousel dims. A centred panel shows "QUEUED FOR", the server icon and name, the position as a
  large `#N`, "of M players in queue", a progress bar and a LEAVE QUEUE button.
- Refreshes once per second from ajQueue.
- LEAVE QUEUE leaves the queue and returns to the carousel.
- When ajQueue transfers the player, the quit handler cleans up the session.

### Join and quit

- Join (after the existing 10-tick delay): invisibility, seat and camera, a UI grow-in (a scale interpolation;
  text opacity is not interpolated by the client), and the title from config.
- Quit or plugin disable: remove the pig and every display, and drop the session.

## Components

All new classes live under `lt.tomexas.serverselector`. Each has one job:

- `selector/SelectorSession`: one per player. Owns the pig seat, the camera packet, the cursor, the
  active screen, the debounce, and the projection math. Routes hover and click to the active screen.
- `selector/SelectorManager`: creates and destroys sessions, runs the single 1-tick loop, and
  answers `isAttached`.
- `selector/screen/Screen`: the interface for `build`, `tick(cursor)`, `click(cursor)` and `destroy`.
- `selector/screen/CarouselScreen`, `selector/screen/QueuePanel`: the two screens.
- `selector/ui/UiPlane`: plane coordinates to world positions (forward/right/up vectors from the camera).
- `selector/ui/UiElement`: a thin wrapper around one text, item or block display. Handles position,
  scale, visibility to its single viewer, and interpolated moves. Includes a rectangle hit test.
- `selector/ui/Glyph`: an enum of the fixed glyph characters assigned in `selector.yml` (default font), with
  plain-text fallbacks when Nexo is absent. There is no compile dependency on Nexo: Nexo only builds and serves the pack.
- `server/ServerRegistry`: config-defined servers (id, display name, icon glyph, description, order)
  plus live player count and online state.
- `server/ProxyPoller`: every 5 s sends BungeeCord `PlayerCount` per configured server through any
  online player, and feeds the registry. Online state comes from ajQueue (see Verification) with a
  proxy-based fallback.
- `queue/QueueTracker`: wraps the ajQueue spigot API (in queue, queued server, position, total, leave).

Listeners stay in `Listeners/` with their current naming. `PlayerDismountListener` and
`PlayerKickListener` are kept. `PlayerItemHeldListener` and `PlayerArmSwingListener` delegate to the
session.

## Config (`config.yml`, new)

```yaml
title: "CHOOSE YOUR REALM"
servers:              # only listed servers appear, in this order
  survival:           # must match the proxy's server name
    display-name: "Survival"
    icon: selector_icon_sword
    description:
      - "Classic survival with land"
      - "claims, jobs and an economy."
```

A server that is configured but unknown to the proxy shows as offline. This replaces the hardcoded
removal of `lobby` and `limbo0`.

## Assets

- `nexo/glyphs/selector.yml`: glyph ids `selector_*`, chars ``–`` written as `\uXXXX`
  escapes, textures under `selector/`, following the conventions in the user's existing `realms.yml`.
- `nexo/pack/assets/minecraft/textures/selector/*.png`: cursor arrow and hand, arrows left and right,
  card backgrounds (normal and selected), queue panel, leave button (normal and hover), progress bar
  track and fill, vignette (generated radial gradient), and about 8 server icons.
- `nexo/LICENSE-kenney.txt`: the Kenney CC0 licence.
- `tools/build_selector_assets.py`: downloads the three Kenney zips, extracts, resizes and recolours
  the chosen files, and writes the `nexo/` tree. Regenerate the assets with it instead of hand-editing them.
- Test server: copy `nexo/` into `testserver/lobby/plugins/Nexo/`. Set Nexo pack hosting to
  self-host on localhost so nothing is uploaded externally.

## Removed

`HudManager`, `Spacer`, the bossbar and all `\uE00x` glyph swapping, `CamPath` (unused), the
prototype `CursorManager` (superseded by `selector/`), the `lobby:server_selector` plugin channel and
its `PlayerInQueue` / `PlayerPositionChanged` handlers (replaced by `QueueTracker`), the
`QueueJoinEvent`/`QueueLeaveEvent` listeners that only touched the bossbar (the events stay as a public
API), and the temporary `[camera-debug]` logging.

## Error handling

- No camera armor stand in the DB, or it is not loaded: log one warning and skip the selector, so the
  player stays in the normal lobby.
- Nexo missing: fall back to plain-text glyphs and log one info line.
- ajQueue call fails or times out: show "Queue unavailable" on the panel and keep the carousel usable.
- A configured server that is missing from the proxy: shown as offline.

## Testing

- Unit tests (JUnit 5, added to Gradle) for the pure logic: projection and clamping, `UiPlane`
  coordinate maths, carousel layout (which index sits where, wrap-around), rectangle hit testing, and
  config parsing. These use no Bukkit runtime.
- In game on the test network: join, cursor movement, hover and click, rotate by arrows, side card and
  scroll, queue join, queue panel updates, leave queue, offline card, quit cleanup. The user runs the
  client and the server logs are checked after each step.

## Verify early in the plan

1. Nexo `1.30-dev-d57c8bdf9` (provided by the user at `~/Downloads/nexo-1.30-dev-d57c8bdf9.jar`) loads on
   Paper 26.3. The unused `com.nexomc:nexo:0.7.0` compile dependency is removed.
2. ajQueue (checked 2026-10-15 against 2.9.1): `getPosition`, `getTotalPositions`, `getQueueName`, `isInQueue`
   and `getServerStatusString(server)` exist and return Futures. The status string is the proxy's
   `placeholders.status.*` message (`&aOnline`, `&cOffline`, `&cRestarting`, `&eFull`, `&eRestricted`,
   `&ePaused`, `&eWhitelisted`). There is no API to leave a queue. The backend plugin registers
   `/leavequeue [queue]`, so leaving is `player.performCommand("leavequeue " + server)`.
3. Glyph characters are fixed in `selector.yml`, so no Nexo API is needed (checked: `NexoPlugin.instance()
   .fontManager().glyphFromID(id)` exists if that is ever wanted).
