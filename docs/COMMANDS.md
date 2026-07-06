# Commands

## Player commands

```mcfunction
/ctb help
/ctb progress [player]
/ctb gym [player]
/ctb shiny [player]
/ctb dex view [player]
/ctb dex catch [player]
```

## Admin commands

Permission level 2 required:

```mcfunction
/ctb shinydetail [player]
/ctb debug [player]
/ctb advancements [player]
/ctb reload
```

## What the commands do

- `/ctb shiny [player]`: total shiny + common, rare, epic, radiant split.
- `/ctb shinydetail [player]`: detailed shiny list from party and PC.
- `/ctb gym [player]`: last completed badge and current region progression.
- `/ctb dex view [player]` or `/ctb dex seen [player]`: Pokédex seen count.
- `/ctb dex catch [player]` or `/ctb dex caught [player]`: Pokédex caught count.
- `/ctb progress [player]`: current badge progression.
- `/ctb advancements [player]`: completed relevant advancement IDs, useful for config debugging.
- `/ctb reload`: reloads config and clears caches.
