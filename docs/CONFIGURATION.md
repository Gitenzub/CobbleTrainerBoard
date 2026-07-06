# Configuration

The config is generated at:

```text
config/cobbletrainerboard.json
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
