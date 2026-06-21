# BlackTimber on Modrinth: upload bundle and step by step guide

Everything you need to publish `modrinth.com/project/blacktimber` is in this folder. Follow
the steps in order. They match the publishing checklist shown on your project settings page,
top to bottom.

## What is in this folder

| File | Used for |
| --- | --- |
| `BlackTimber-1.3.0.jar` | The version file you upload (also on the GitHub release) |
| `description.md` | Paste into the Description editor |
| `summary.txt` | Paste into the Summary field on the General page |
| `version-changelog.md` | Paste into the version Changelog field |
| `links.txt` | The external links to paste on the Links page |
| `icon.png` | Upload as the project icon (512 x 512) |
| `gallery/` | The gallery images, named in upload order, with titles below |

---

## Step 1. Upload a version (required)

Settings sidebar, **Versions**, then **Create version**.

| Field | Value |
| --- | --- |
| Name | `BlackTimber 1.3.0` |
| Version number | `1.3.0` |
| Release channel | `Release` |
| Loaders | `Paper` and `Folia` |
| Game versions | `26.1.2` |
| Dependencies | none |
| Files | upload `BlackTimber-1.3.0.jar` |
| Changelog | paste the contents of `version-changelog.md` |

Publish the version. This also unlocks the Tags page (step 7).

## Step 2. Add a description (required)

Settings sidebar, **Description**. Open `description.md`, copy all of it, and paste it into
the editor. It is Markdown, well over the 200 character minimum, in English, and it covers
what the plugin does, why to download it, and the telemetry disclosure that Modrinth
requires for anything that talks to a remote server.

## Step 3. Select a license (required)

Settings sidebar, **License**. Choose **MIT License** from the list. Leave the License URL
field blank; Modrinth will show the license text. This matches the `LICENSE` file in the
repository.

## Step 4. Add an icon

Settings sidebar, **General**, **Upload icon**. Choose `icon.png`. It is the square
512 x 512 master; the vector source is `assets/icon.svg` in the repository.

## Step 5. Feature a gallery image

Settings sidebar, **Gallery**. Upload each image from the `gallery/` folder and give it the
title below. Set the banner as the **Featured** image, since that is the first impression.

| Order | File | Title | Featured |
| --- | --- | --- | --- |
| 1 | `gallery/1-banner.png` | BlackTimber, smart tree felling for Folia and Paper | Yes |
| 2 | `gallery/2-detection.png` | Three checks tell a wild tree from a player build | No |
| 3 | `gallery/3-menu-player.png` | The per player menu | No |
| 4 | `gallery/4-menu-admin.png` | The live admin panel, eighteen settings | No |
| 5 | `gallery/5-menu-loot.png` | The drag and drop leaf loot editor | No |
| 6 | `gallery/6-stack.png` | A lean, modern stack | No |

## Step 6. Add external links

Settings sidebar, **Links**. Paste from `links.txt`.

| Field | Value |
| --- | --- |
| Source code | `https://github.com/can61cebi/blacktimber` |
| Issue tracker | `https://github.com/can61cebi/blacktimber/issues` |
| Wiki page | `https://docs.cebi.tr/blacktimber` |
| Discord invite | leave blank |
| Donation links | leave blank |

Save changes.

## Step 7. Tags and environment

Settings sidebar, **Tags** (available once the version from step 1 exists).

- Categories: **Utility**, **Game mechanics**, **Management**.
- Environment: **Client: Unsupported**, **Server: Required**. BlackTimber runs only on the
  server, nothing is installed on the client.

The loaders (Paper, Folia) come from the version you uploaded, so the project shows up as a
plugin automatically.

## Step 8. Fix the summary

Settings sidebar, **General**, **Summary**. Replace the current text with the contents of
`summary.txt`. The current summary repeats the project name, which Modrinth rule 5 asks you
to avoid; the new one is a clean, formatting free sentence that does not repeat the title.

## Step 9. Submit for review

Back on the project settings page, the checklist should now be all green. Press **Submit for
review**. A moderator will review it, and once it passes you can set visibility to Public and
Listed.

---

## Why this passes review

The Modrinth Content Rules are already satisfied by this bundle:

- **Clear and honest function (rule 2).** The description states plainly what the plugin
  does, why to download it, and what to know first, with no jargon padding.
- **English description (rule 2.2).** The description and summary are in English, with a
  plain text reading throughout.
- **Remote data disclosure (rule 1).** The telemetry is disclosed in full in the
  description and in `TELEMETRY.md`, and it is opt out.
- **No cheats (rule 3).** It is a server side utility, not a client cheat.
- **Original content (rule 4).** The artwork is original to this project. The Minecraft item
  and block textures inside the menu shots belong to Mojang and are credited in the README.
- **Correct metadata (rule 5).** License, environment, loaders, tags and links all match the
  information on GitHub, and the summary does not repeat the title.
