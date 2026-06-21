![BlackTimber, smart tree felling for Minecraft on Folia](https://raw.githubusercontent.com/can61cebi/blacktimber/main/assets/png/banner.png)

BlackTimber is a smart tree felling plugin for Minecraft Java Edition, written from the
ground up for Folia. Break a single log and the whole tree comes down in one motion,
gathered with the axe in your hand. Wooden houses, tree houses and hand built trees are
never touched. There is nothing to install alongside it: no library plugin, no database,
no setup.

## What it does

Classic timber plugins fell anything made of logs, which makes them a liability near a
base. BlackTimber fells only what nature grew, and it proves a tree is wild before a single
extra log is removed.

![Three checks tell a wild tree from a player build](https://raw.githubusercontent.com/can61cebi/blacktimber/main/assets/png/detection.png)

A cluster of logs comes down only when all three checks agree it is wild:

1. **Natural leaves.** Wild leaves are `persistent = false`. A cluster with none is treated
   as a build and left alone.
2. **Placed logs.** Every log a player places is remembered. One placed log protects the
   whole tree.
3. **Attached structures.** Planks, stairs, slabs, fences, walls or glass touching the logs
   mark the cluster as a build.

## Why you want it

- It protects builds. A stray swing near your base will not delete a wall, a floor or a
  tree house.
- It is fast and Folia native. Felling runs on the region thread that owns the log, large
  fells are spread across ticks, and the legacy scheduler is never used.
- It is self contained. No external dependencies and no database, so there is nothing to
  install or maintain alongside it.

## In game menus

Every player flips three switches for themselves, saved per player.

![The per player menu](https://raw.githubusercontent.com/can61cebi/blacktimber/main/assets/png/menu-player.png)

Admins tune eighteen settings live, written straight to config with no reload.

![The live admin panel](https://raw.githubusercontent.com/can61cebi/blacktimber/main/assets/png/menu-admin.png)

Bonus leaf loot is built by hand in a drag and drop editor.

![The drag and drop leaf loot editor](https://raw.githubusercontent.com/can61cebi/blacktimber/main/assets/png/menu-loot.png)

## Requirements

- Folia or Paper `26.1.2` (`api-version: 26.1`).
- Java `25`.

## Commands and permissions

| Command | Permission |
| --- | --- |
| `/blacktimber` open your menu | `blacktimber.use` |
| `/blacktimber on / off / toggle / leaves / pickup / status` | `blacktimber.use` |
| `/blacktimber admin` open the admin panel | `blacktimber.admin` |
| `/blacktimber reload` | `blacktimber.admin` |

`blacktimber.use` defaults to everyone, `blacktimber.admin` defaults to operators.

## Telemetry and privacy

BlackTimber reports anonymous usage stats every fifteen minutes: a random server id, the
online player count, and the software and version strings. No IP address is stored, no
player names or UUIDs are ever sent, and nothing is linked to any person. The data is
anonymous and aggregate, in the same spirit as bStats. Opt out completely with
`telemetry: false` in `config.yml`. Full disclosure is in
[TELEMETRY.md](https://github.com/can61cebi/blacktimber/blob/main/TELEMETRY.md).

## Links

A full write up lives at [docs.cebi.tr](https://docs.cebi.tr/blacktimber); the
configuration reference, the source code and the issue tracker are on
[GitHub](https://github.com/can61cebi/blacktimber).
