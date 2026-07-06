# CobbleTrainerBoard

CobbleTrainerBoard is a small **Fabric/Kotlin server-side mod** for Cobblemon servers.

It provides PB4/Text Placeholder API placeholders and `/ctb` commands to display, in a player list such as StyledPlayerList:

```text
<Player> <Shiny display> <Badge progression> <Pokédex seen> <Pokédex caught>
```

The shiny display can be configured as total or by type in `config/cobbletrainerboard.json`.

Example StyledPlayerList output:

```text
PlayerName ✨15 | Kanto 6/8 | Seen:123 Caught:45
```

or, with `shiny_display_mode = "by_type"`:

```text
PlayerName ✨C:7 R:3 E:5 Rad:2 | Kanto 6/8 | Seen:123 Caught:45
```

## Features

- Badge progression based on completed Minecraft advancements.
- Forced region order: Kanto → Johto → Hoenn → Sinnoh → Unova → Kalos → Alola → Galar → Paldea.
- Shiny counters from Cobblemon party + PC storage.
- Rare/Epic support through Pokémon aspects.
- Radiant counted as a separate visual bonus, not added twice to the shiny total.
- Pokédex seen/caught counters.
- PB4 placeholders for StyledPlayerList or other compatible mods.
- Configurable TAB shiny display mode: `total` or `by_type`, no rebuild needed.
- Admin debug commands.

## Requirements

- Minecraft `1.21.1`
- Fabric Loader
- Fabric API
- Fabric Language Kotlin
- Cobblemon `1.7.3+`
- Text Placeholder API / PB4 Placeholder API
- Optional: StyledPlayerList

## Build

```bash
gradle clean build
```

The jar is generated in:

```text
build/libs/cobbletrainerboard-1.4.1.jar
```

## Installation

1. Build the mod.
2. Copy the generated `.jar` into the server `mods/` folder.
3. Make sure the required dependencies are installed.
4. Start the server once.
5. Edit `config/cobbletrainerboard.json` if your advancement IDs differ.
6. Reload in game:

```mcfunction
/ctb reload
/styledplayerlist reload
```

## StyledPlayerList example

A ready-to-use example is available in:

```text
config_examples/styledplayerlist_tab_shiny_badges_dex.json
```

Main `right_text` line:

```json
"right_text": "%cobbletrainerboard:tab_line%"
```

Then choose the shiny display in `config/cobbletrainerboard.json`:

```json
"tab": {
  "shiny_display_mode": "total"
}
```

or:

```json
"shiny_display_mode": "by_type"
```

## Commands

```mcfunction
/ctb help
/ctb progress [player]
/ctb gym [player]
/ctb shiny [player]
/ctb shinydetail [player]
/ctb dex view [player]
/ctb dex seen [player]
/ctb dex catch [player]
/ctb dex caught [player]
/ctb debug [player]
/ctb advancements [player]
/ctb reload
```

Admin-only commands require permission level 2:

```text
/ctb shinydetail
/ctb debug
/ctb advancements
/ctb reload
```

## Placeholders

```text
%cobbletrainerboard:tab_line%
%cobbletrainerboard:shiny_display%
%cobbletrainerboard:progress%
%cobbletrainerboard:progress_short%
%cobbletrainerboard:region%
%cobbletrainerboard:completed_gyms%
%cobbletrainerboard:required_gyms%
%cobbletrainerboard:last_badge%
%cobbletrainerboard:shiny_total%
%cobbletrainerboard:shiny_common%
%cobbletrainerboard:shiny_rare%
%cobbletrainerboard:shiny_epic%
%cobbletrainerboard:shiny_radiant%
%cobbletrainerboard:dex_seen%
%cobbletrainerboard:dex_caught%
```

## Configuration

The mod creates this file on first start:

```text
config/cobbletrainerboard.json
```

Badge progression is based only on completed advancements. Each gym can match either:

- an exact advancement ID through `exact_ids`, or
- token matching through `match_any`.

The region order is the order of the `regions` array in the config.

TAB display is controlled by:

```json
"tab": {
  "shiny_display_mode": "total",
  "include_radiant": true,
  "format_total": "✨ %shiny_display% | %progress_short% | Vue:%dex_seen% Capt:%dex_caught%",
  "format_by_type": "✨ %shiny_display% | %progress_short% | Vue:%dex_seen% Capt:%dex_caught%"
}
```

Change `shiny_display_mode` to `by_type`, then run `/ctb reload` and `/styledplayerlist reload`.

## Notes on shiny categories

The shiny total is:

```text
common + rare + epic
```

Radiant is counted separately as a visual bonus. For example, a Pokémon that is both `epic` and `radiant` counts as:

```text
Epic +1
Radiant +1
Total shiny +1
```

## License

MIT.
