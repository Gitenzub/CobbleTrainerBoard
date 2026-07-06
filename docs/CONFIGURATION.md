# Configuration

The config is generated at:

```text
config/cobbletrainerboard.json
```

Reload after editing:

```mcfunction
/ctb reload
/styledplayerlist reload
```

## TAB shiny display mode

Since `1.4.1`, the TAB display can be changed without rebuilding the mod.

Use this in StyledPlayerList:

```json
"right_text": "%cobbletrainerboard:tab_line%"
```

Then edit `config/cobbletrainerboard.json`:

```json
"tab": {
  "shiny_display_mode": "total",
  "include_radiant": true,
  "format_total": "✨ %shiny_display% | %progress_short% | Vue:%dex_seen% Capt:%dex_caught%",
  "format_by_type": "✨ %shiny_display% | %progress_short% | Vue:%dex_seen% Capt:%dex_caught%"
}
```

Available modes:

```text
total   -> ✨ 15 | Kanto 6/8 | Vue:123 Capt:45
by_type -> ✨ C:7 R:3 E:5 Rad:2 | Kanto 6/8 | Vue:123 Capt:45
```

`include_radiant` controls whether `%shiny_display%` includes `Rad:x` in `by_type` mode.

You can also customize the two templates. Supported tokens:

```text
%player%
%shiny_display%
%shiny_total%
%shiny_common%
%shiny_rare%
%shiny_epic%
%shiny_radiant%
%progress%
%progress_short%
%region%
%completed_gyms%
%required_gyms%
%last_badge%
%dex_seen%
%dex_caught%
```

## Region order

The mod displays the first unfinished region in the order listed in the `regions` array.

Default order:

```text
Kanto → Johto → Hoenn → Sinnoh → Unova → Kalos → Alola → Galar → Paldea
```

## Gym matching

Each gym can match completed advancements by exact ID:

```json
"exact_ids": ["example:trainer/kanto/defeat_brock"]
```

or by token matching:

```json
"match_any": ["brock", "boulder"]
```

For custom servers, run:

```mcfunction
/ctb advancements <player>
```

Then copy the correct advancement IDs into `exact_ids`.
