# Placeholders

Use these with StyledPlayerList or another PB4/Text Placeholder API compatible mod.

```text
%cobbletrainerboard:tab_line%
%cobbletrainerboard:shiny_display%
%cobbletrainerboard:progress%
%cobbletrainerboard:progress_short%
%cobbletrainerboard:region%
%cobbletrainerboard:progress_key%
%cobbletrainerboard:progress_index%
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

Recommended StyledPlayerList `right_text`:

```json
"right_text": "%cobbletrainerboard:tab_line%"
```

`%cobbletrainerboard:tab_line%` is controlled by `config/cobbletrainerboard.json`:

```json
"tab": {
  "shiny_display_mode": "total"
}
```

Switch to detailed shiny display:

```json
"shiny_display_mode": "by_type"
```

No rebuild required; run:

```mcfunction
/ctb reload
/styledplayerlist reload
```
