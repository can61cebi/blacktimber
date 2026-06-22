# Publishing BlackTimber to Modrinth

This is the complete, ready to paste field guide for listing BlackTimber on
[Modrinth](https://modrinth.com). Everything below is filled in to match the project and
its premium presentation, so the listing carries the same look and tone as the README.

Create the project at <https://modrinth.com/dashboard/projects> with **Create a project**,
then fill each field with the values in this document.

---

## 1. At a glance checklist

| Step | Field | Value |
| --- | --- | --- |
| 1 | Project name | `BlackTimber` |
| 2 | Project type | Plugin (set by choosing the Paper and Folia loaders) |
| 3 | Vanity URL / slug | `blacktimber` |
| 4 | Summary | see [Summary](#3-summary) |
| 5 | Description | see [Description body](#5-description-body) |
| 6 | Categories | `Utility`, `Game mechanics`, `Management` |
| 7 | Environment | Client `Unsupported`, Server `Required` |
| 8 | License | `MIT` |
| 9 | Links | source, issues (see [Links](#7-external-links)) |
| 10 | Gallery | seven images (see [Gallery](#8-gallery)) |
| 11 | First version | `1.4.0` (see [Version](#9-first-version-upload)) |

Modrinth requires a project to be reasonably complete before it can be submitted for
review: a clear title, a real summary, a meaningful description, at least one category, the
environment set, and a license. All of that is provided below.

---

## 2. Core identity

| Field | Value | Notes |
| --- | --- | --- |
| Name | `BlackTimber` | Shown as the project title |
| Icon | `assets/icon-512.png` | Upload as the project icon. The square master is `assets/icon.svg` |
| Slug | `blacktimber` | Final URL becomes `modrinth.com/plugin/blacktimber`. 3 to 64 characters |
| Project type | Plugin | Selected indirectly: add the Paper and Folia loaders and Modrinth lists it as a plugin |
| Visibility | Public, Listed | Set after the first version passes review |

---

## 3. Summary

The summary is the short, one line description shown in search and at the top of the page.
Keep it plain text, no markdown. Paste this:

```
Smart tree felling for Folia and Paper. Break one log and the whole tree falls, while houses, tree houses and player builds stay standing. No dependencies, no database, every setting editable in game.
```

---

## 4. Tags and environment

**Categories** (pick the three that fit best):

- `Utility`
- `Game mechanics`
- `Management`

**Loaders** (this is what makes it a plugin):

- `Paper`
- `Folia`

Purpur is a Paper fork and will run the same jar, so you may add it as well. The jar is
tested and declared against Paper and Folia.

**Environment:**

| Side | Setting | Why |
| --- | --- | --- |
| Client | `Unsupported` | BlackTimber is server only, nothing is installed on the client |
| Server | `Required` | All logic runs on the server |

---

## 5. Description body

This is the long form description, written in Modrinth flavored markdown. It mirrors the
README and uses the same premium artwork. Paste the whole block below into the description
editor. The images load from this repository over raw GitHub, so they appear without any
extra upload. If you prefer Modrinth to host them, upload the same files to the gallery and
swap the links.

````markdown
<div align="center">

![BlackTimber](https://raw.githubusercontent.com/can61cebi/blacktimber/main/assets/png/banner.png)

![Minecraft 26.1.2, Folia and Paper, Java 25, MIT, no dependencies](https://raw.githubusercontent.com/can61cebi/blacktimber/main/assets/png/chips.png)

</div>

BlackTimber is a smart tree felling plugin for Minecraft Java Edition, written from the
ground up for Folia. Break a single log and the whole tree comes down in one motion,
gathered with the axe in your hand. Wooden houses, tree houses and hand built trees are
never touched. There is nothing to install alongside it: no library plugin, no database,
no setup.

## The problem it solves

Classic timber plugins fell anything made of logs, which makes them a liability near a
base. BlackTimber fells only what nature grew, and it proves a tree is wild before a single
extra log is removed.

![How BlackTimber tells a tree from a build](https://raw.githubusercontent.com/can61cebi/blacktimber/main/assets/png/detection.png)

A cluster of logs comes down only when all three checks agree it is wild:

1. **Natural leaves.** Wild leaves are `persistent = false`. A cluster with none is treated
   as a build and left alone.
2. **Placed logs.** Every log a player places is remembered. One placed log protects the
   whole tree.
3. **Attached structures.** Planks, stairs, slabs, fences, walls or glass touching the logs
   mark the cluster as a build.

## In game menus

Every player flips three switches for themselves, saved per player.

![The BlackTimber player menu](https://raw.githubusercontent.com/can61cebi/blacktimber/main/assets/png/menu-player.png)

Admins tune eighteen settings live, written straight to config with no reload.

![The BlackTimber admin panel](https://raw.githubusercontent.com/can61cebi/blacktimber/main/assets/png/menu-admin.png)

Bonus leaf loot is built by hand in a drag and drop editor.

![The BlackTimber leaf loot editor](https://raw.githubusercontent.com/can61cebi/blacktimber/main/assets/png/menu-loot.png)

## Highlights

- Whole tree felling that leaves player builds untouched.
- Protects tree houses and hand built trees by remembering placed logs and attached builds.
- Works with every wood species through vanilla tags, including future ones.
- Handles diagonal trunks, offset branches and 2x2 mega trees.
- Per player menu for felling, leaf breaking and auto pickup, saved across restarts.
- Optional leaf breaking that drops biome, species and size themed bonus loot.
- In game admin panel for every setting, with a drag and drop leaf loot editor.
- Built for Folia: regionized, tick spread, no legacy scheduler.
- Anonymous, opt-out usage stats that power a live network chart.
- No external dependencies and no database.

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

## Telemetry

BlackTimber reports anonymous usage stats every fifteen minutes: a random server id, the
online player count, and the software and version strings. No IP address is stored, no
player names or UUIDs are ever sent, and nothing is linked to any person. It powers the
live usage chart and follows the bStats model. Opt out completely with `telemetry: false`
in `config.yml`. Full disclosure is in
[TELEMETRY.md](https://github.com/can61cebi/blacktimber/blob/main/TELEMETRY.md).

A full write up lives at [docs.cebi.tr](https://docs.cebi.tr/blacktimber); the
configuration reference and source are on
[GitHub](https://github.com/can61cebi/blacktimber).
````

---

## 6. Why these images

Modrinth renders the same markdown and artwork the README uses, so the listing keeps the
piano black, ivory and brass design. The banner sets the tone, the detection diagram
explains the core idea at a glance, and the three menu shots carry the option by option
descriptions without a wall of text.

---

## 7. External links

| Field | Value |
| --- | --- |
| Source | `https://github.com/can61cebi/blacktimber` |
| Issue tracker | `https://github.com/can61cebi/blacktimber/issues` |
| Wiki | `https://docs.cebi.tr/blacktimber` |
| Discord | leave empty unless you run one |

License: choose `MIT` from the SPDX list. It matches the `LICENSE` file in the repository.

---

## 8. Gallery

Upload these from `assets/png/` in this order. Captions are ready to paste.

| Order | File | Caption | Featured |
| --- | --- | --- | --- |
| 1 | `banner.png` | BlackTimber, smart tree felling for Folia and Paper | Yes |
| 2 | `detection.png` | Three checks tell a wild tree from a player build | No |
| 3 | `menu-player.png` | The per player menu: felling, leaves, auto pickup | No |
| 4 | `menu-admin.png` | The admin panel: eighteen settings, edited live | No |
| 5 | `menu-loot.png` | The drag and drop leaf loot editor | No |
| 6 | `stack.png` | A lean, modern stack | No |
| 7 | `chips.png` | Minecraft 26.1.2, Folia and Paper, Java 25, MIT | No |

Modrinth gallery images should be at least 1280 pixels wide. Every file above is rendered
at well over that, so they stay crisp.

---

## 9. First version upload

After the project page is filled in, add the first version.

| Field | Value |
| --- | --- |
| Version number | `1.4.0` |
| Version title | `BlackTimber 1.4.0` |
| Release channel | `Release` |
| Loaders | `Paper`, `Folia` |
| Game versions | `26.1.2` |
| Dependencies | None |
| File | `BlackTimber-1.4.0.jar` from `build/libs` or the GitHub release |

Changelog: paste the contents of [`modrinth/version-changelog.md`](modrinth/version-changelog.md).
It covers the 1.4.0 fixes (vanilla-accurate leaf clearing and durability-bounded felling).
When publishing a new version later, follow [`modrinth/UPDATE.md`](modrinth/UPDATE.md), which
is the short update-only flow.

---

## 10. Review and rules

Modrinth reviews every new project before it is listed. To pass on the first try:

- The summary and description are in English and clearly explain what the plugin does. Both
  are provided above.
- The artwork is original to this project. The item and block textures inside the menu
  illustrations are rendered from Minecraft assets, which belong to Mojang Studios; this is
  noted in the README credits, and BlackTimber is marked as an independent project.
- The license, environment, source link and category are all set.
- The version file is the real plugin jar and matches the version number.

Once the project passes review, set visibility to Public and Listed.

---

## 11. Optional: publish through the API

If you prefer to script the upload, the create call needs these fields. Use a personal
access token with the `Create projects` scope from
<https://modrinth.com/settings/pats>.

```jsonc
{
  "slug": "blacktimber",
  "title": "BlackTimber",
  "description": "Smart tree felling for Folia and Paper. Break one log and the whole tree falls, while houses, tree houses and player builds stay standing. No dependencies, no database, every setting editable in game.",
  "body": "<the markdown from section 5>",
  "categories": ["utility", "game-mechanics", "management"],
  "client_side": "unsupported",
  "server_side": "required",
  "license_id": "MIT",
  "issues_url": "https://github.com/can61cebi/blacktimber/issues",
  "source_url": "https://github.com/can61cebi/blacktimber",
  "is_draft": true
}
```

The version, its loaders, game versions and the jar file are attached in a second call to
the versions endpoint. Full reference: <https://docs.modrinth.com/api/>.
