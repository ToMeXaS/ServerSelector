# ServerSelector

A fullscreen, mouse-driven server selector for a Paper lobby behind a Velocity or BungeeCord proxy.
Players join the lobby and see a carousel of server cards in front of a locked camera. They move a cursor
with the mouse, pick a server by clicking a side card or scrolling, and press **PLAY** to be sent there.
No client mods are needed; the art ships in a resource pack served by [Nexo](https://nexomc.com).

## How it works

- Each player rides an invisible, silent pig that is also their camera. A riding client keeps sending its
  head rotation while its camera is on its own vehicle, so the plugin turns mouse movement into a cursor
  (`distance x tan(angle)`) on a plane in front of the camera.
- Cards, the cursor, the title and the PLAY button are text displays that only their own player can see.
  Card art comes in pre-rendered sizes and each card picks the smallest one that fits its text.
- Live status: every 5 seconds the lobby asks the proxy for each server's player count (`PlayerCount`) and
  address (`ServerIP`), then pings that address with a Minecraft status ping. Servers that don't answer
  show as offline and can't be played.
- PLAY unloads the lobby resource pack (so the hidden crosshair and selector art don't follow the player)
  and sends the player through the proxy with a BungeeCord `Connect` message.

## Requirements

- Paper 26.3 (compiled against the 1.21.4 API, Java 21)
- [ProtocolLib](https://github.com/dmulloy2/ProtocolLib) 5.4+ (a dev build for 26.3)
- [Nexo](https://nexomc.com) 1.30+ to serve the resource pack (optional: without it the selector falls back
  to plain-text symbols)
- A Velocity or BungeeCord proxy with the BungeeCord plugin channel enabled
  (`bungee-plugin-message-channel = true` on Velocity)

## Building

```bash
./gradlew build
```

The plugin jar is written to `build/libs/ServerSelector-<version>.jar`. Tests run with the build.

## Installing

1. Put the plugin jar, ProtocolLib and Nexo in the lobby's `plugins/` folder.
2. Copy the selector art into Nexo:
   - `nexo/glyphs/selector.yml` to `plugins/Nexo/glyphs/`
   - `nexo/pack/assets/` to `plugins/Nexo/pack/assets/` (merge it with what is already there)
3. Start the server once so `plugins/ServerSelector/config.yml` is created, then edit it (see below).
4. Set up the camera (see below) and restart.

### Camera

The view comes from an armor stand whose UUID is stored in `plugins/ServerSelector/main.db`. Its position
and rotation decide what players see behind the menu. There is no in-game command for this yet, so set it
up by hand:

1. Keep the chunk loaded, e.g. `/forceload add <x> <z>`.
2. Summon the stand where the view should be, e.g.
   `/summon armor_stand <x> <y> <z> {Tags:["ss_cam"],NoGravity:1b,Invisible:1b,Invulnerable:1b,Rotation:[<yaw>f,<pitch>f]}`
3. Read its UUID with `/data get entity @e[tag=ss_cam,limit=1] UUID` (four integers) and convert it to the
   usual `xxxxxxxx-xxxx-...` form.
4. With the server stopped, insert it: `sqlite3 plugins/ServerSelector/main.db "INSERT INTO armorStand (UUID) VALUES ('<uuid>');"`

## Configuration

`plugins/ServerSelector/config.yml`:

```yaml
title: "CHOOSE YOUR REALM"

# Half the screen size in plane units: y = 2 * tan(FOV / 2), x = y * window width / window height.
cursor-bounds:
  x: 3.9
  y: 2.05

servers:            # shown in this order; keys must match the proxy's server names
  survival:
    display-name: "Survival"
    icon: selector_icon_sword
    description:
      - "Classic survival with land"
      - "claims, jobs and an economy."
```

Icons: `selector_icon_sword`, `_pickaxe`, `_axe`, `_bow`, `_hammer`, `_wand`, `_bomb`, `_torch`, `_star`.
`cursor-bounds` should match the players' screen so the cursor reaches the edges; the defaults fit FOV 90
in a 1920x1009 window.

## Changing the art

All art is generated from [Kenney](https://kenney.nl)'s CC0 asset packs. Edit
`tools/build_selector_assets.py`, never the generated files, then regenerate:

```bash
python -m pip install -r tools/requirements.txt
python tools/build_selector_assets.py
```

It downloads the packs into `tools/.cache/` and writes `nexo/glyphs/selector.yml` and the PNGs under
`nexo/pack/`. Useful knobs at the top of the script are `TINT_COLOR` and `TINT_OPACITY` (the darkening
over the lobby world). The Java `Glyph` enum and `CardArt` must agree with the characters the script
assigns; `GlyphTest` and `CardArtTest` check that.

## License

The generated art is based on Kenney's CC0 assets; see `nexo/LICENSE-kenney.txt`.
