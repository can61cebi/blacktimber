# BlackTimber

Smart tree felling for Minecraft Java Edition 26.1.2 on Folia. Break one log and the
whole tree comes down, while wooden houses and builds stay exactly where they were.

## The problem it solves

Classic timber plugins fell anything made of logs. That makes them risky near a base:
one stray swing with an axe and a wall, a floor, or a whole tree house is gone.
BlackTimber only fells real trees. It tells a tree apart from a build by checking for
natural leaves, so the logs in a player build are never touched.

## How it tells a tree from a build

The check rests on one fact about how Minecraft stores leaves. Every leaf block carries
a `persistent` flag:

- Leaves on a natural or player grown tree are `persistent = false`. They decay once the
  tree is cut.
- Leaves a player places by hand are `persistent = true`. They never decay.

When a player breaks a log, BlackTimber does three things:

1. Flood fills the connected logs from the broken block, using the vanilla
   `minecraft:logs` block tag so every wood species is covered. The search follows
   diagonals, so offset branches (acacia, cherry) and 2x2 trunks (dark oak, pale oak,
   giant spruce and jungle) are all found. A hard cap stops it from ever running away.
2. Looks for natural leaves (`persistent = false`) attached to the cluster. If there are
   none, the cluster is treated as a build and nothing happens. This single rule is what
   keeps houses safe.
3. If it is a tree, breaks the remaining logs, drops them with the held tool, and lets
   the leaves decay on their own the way vanilla does.

Because detection is built on block tags and the leaf flag instead of a hardcoded list,
new wood types are picked up automatically. The poplar tree announced for a later 2026
drop will work with no code change.

## Folia and performance

Folia splits the world into regions that tick in parallel on separate threads, with no
single main thread. Block data may only be touched by the thread that owns its region.

BlackTimber is built for that model:

- The break event already runs on the region thread that owns the log, so a normal tree
  is felled in place with no cross thread access.
- A very large fell is spread across ticks with the region scheduler, breaking a fixed
  budget of logs per tick so one region never stalls.
- The hot path avoids waste: material sets are checked through cached block tags, the
  search reuses an `ArrayDeque` frontier, visited positions are packed into a `long`, and
  no temporary objects are created until a block is actually changed.
- The plugin declares `folia-supported: true` and never calls the legacy Bukkit
  scheduler, which Folia does not provide.

## Features

- Whole tree felling that leaves player builds untouched.
- Works with every wood species through vanilla tags, including future ones.
- Handles diagonal trunks, offset branches, and 2x2 mega trees.
- Per player on and off switch that survives restarts.
- Optional tool durability cost that respects the Unbreaking enchantment.
- Optional sapling replanting for single sapling species.
- A safety cap on logs per fell, plus tick spreading for huge trees.
- No external dependencies and no database.

## Requirements

- Folia or Paper 26.1.2 (`api-version: 26`).
- Java 25.

## Installation

1. Download `BlackTimber` from the releases page, or build it from source (below).
2. Place the jar in the server `plugins` folder.
3. Start the server. A `config.yml` is written on first run.

## Commands

The plugin has one command, `/blacktimber`, with the alias `/bt`.

| Command | What it does | Permission |
| --- | --- | --- |
| `/blacktimber status` | Show whether felling is on for you | `blacktimber.use` |
| `/blacktimber on` | Turn felling on for you | `blacktimber.use` |
| `/blacktimber off` | Turn felling off for you | `blacktimber.use` |
| `/blacktimber toggle` | Flip your setting | `blacktimber.use` |
| `/blacktimber reload` | Reload `config.yml` | `blacktimber.admin` |

## Permissions

| Node | Default | Description |
| --- | --- | --- |
| `blacktimber.use` | everyone | Use felling and the on, off, toggle, and status commands |
| `blacktimber.admin` | operators | Reload the configuration |

## Configuration

`config.yml` is reloadable with `/blacktimber reload`.

| Key | Default | Description |
| --- | --- | --- |
| `require-natural-leaves` | `true` | Only fell clusters that carry natural leaves. This protects builds |
| `min-natural-leaves` | `1` | Natural leaves needed to count as a tree |
| `leaf-search-radius` | `1` | Blocks around a log to search for leaves |
| `search-diagonal` | `true` | Connect logs diagonally for branches and 2x2 trunks |
| `max-logs` | `150` | Hard cap on logs removed in one fell |
| `require-axe` | `true` | Only trigger while holding an axe |
| `sneak-requirement` | `ignore` | `ignore`, `required`, or `forbidden` |
| `survival-only` | `true` | Skip creative and spectator |
| `default-enabled` | `true` | Per player default before they toggle |
| `apply-durability` | `true` | Damage the axe per extra log |
| `respect-unbreaking` | `true` | Honor the Unbreaking enchantment |
| `break-tool` | `false` | Allow the axe to break; false stops it at 1 durability |
| `fell-leaves` | `false` | Also break leaves; false lets them decay naturally |
| `replant-sapling` | `false` | Replant a matching sapling for single sapling species |
| `stagger-threshold` | `64` | Fells larger than this are spread across ticks |
| `logs-per-tick` | `16` | Logs broken per tick while spreading |

## Building from source

```
git clone https://github.com/can61cebi/blacktimber.git
cd blacktimber
./gradlew build
```

The jar is written to `build/libs`. The build uses the Gradle wrapper, so only a
Java 25 toolchain is required.

## Background notes

These are the facts the plugin is built on, verified against the live game and server.

- Versioning. Mojang moved to a year based scheme in late 2025. Version 26.1.2 means
  year 2026, first drop, second hotfix. The Bukkit API string is `26.1.2-R0.1-SNAPSHOT`
  and `plugin.yml` takes `api-version: 26`.
- Block tags. `minecraft:logs` covers all overworld wood plus crimson and warped stems,
  exposed in Bukkit as `Tag.LOGS`. Using the tag means new species are handled without a
  code change. Bamboo is not in this tag and is left out by design.
- Leaf persistence. Natural leaves are `persistent = false` and decay; placed leaves are
  `persistent = true` and do not. Bukkit exposes this through
  `org.bukkit.block.data.type.Leaves`. This flag is the core of build detection.
- Tree shapes. Small trees are a single trunk. Dark oak and pale oak are always 2x2, and
  mega spruce and giant jungle can be 2x2 as well. Acacia, cherry, and azalea trees grow
  at an angle with offset canopies. A connected, diagonal aware search handles all of
  these, which is why a simple vertical scan is not used.

## Notes and limits

- Logs past the first are removed directly, so block protection plugins do not see them.
  Use on servers where that is acceptable.
- Nether fungi (crimson and warped) have no leaves, so they are never felled.
- Mangrove roots and the pale oak creaking heart are not logs and are left in place.

## License

Released under the MIT License. See `LICENSE`.
