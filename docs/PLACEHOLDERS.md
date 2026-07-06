# Placeholders

Use these with StyledPlayerList or another PB4/Text Placeholder API compatible mod.

```text
%cobbletrainerboard:tab_line%
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
"right_text": "<gray>✨</gray><yellow>%cobbletrainerboard:shiny_total%</yellow><dark_gray> | </dark_gray><gold>%cobbletrainerboard:progress_short%</gold><dark_gray> | </dark_gray><gray>Seen:</gray><white>%cobbletrainerboard:dex_seen%</white><gray> Caught:</gray><green>%cobbletrainerboard:dex_caught%</green>"
```
