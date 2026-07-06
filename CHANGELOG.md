# Changelog

## 1.4.1

- Added configurable TAB shiny display mode.
- Added `tab.shiny_display_mode` in `config/cobbletrainerboard.json`:
  - `total`: displays only total shiny count.
  - `by_type`: displays common, rare, epic and optionally radiant counts.
- Added `%cobbletrainerboard:shiny_display%` placeholder.
- Updated `%cobbletrainerboard:tab_line%` to use the config file.
- Config migration now preserves existing region/gym settings and only adds missing TAB settings.

## 1.4.0

- Added shiny counters from Cobblemon party + PC.
- Added Pokédex seen/caught counters.
- Added TAB placeholder line.
- Added `/ctb shiny`, `/ctb shinydetail`, `/ctb dex view`, `/ctb dex catch`.

## 1.3.5

- Added exact CobbleVerse gym advancement IDs.
- Fixed region progression order.
