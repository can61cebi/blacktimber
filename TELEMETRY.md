# Telemetry and privacy

BlackTimber reports anonymous usage statistics. This document is the full disclosure:
exactly what is sent, what is deliberately never sent, where it goes, how long it is
kept, and how to turn it off. It is written to be read, not to be buried.

## Why it exists

The plugin is free and open source. The only signal of how widely it is used, and the
only thing that keeps the live chart in the README honest, is a lightweight count of
active servers and players. That count is shown back to everyone, so the data serves the
community as much as the author.

## What is sent

Once every fifteen minutes, each server sends one HTTP POST to
`https://cebi.tr/api/blacktimber/telemetry` with this exact body and nothing more:

```json
{
  "server_id": "f2a7c1e0-9b3d-4a6e-8c11-7e9b2d4f6a08",
  "players": 12,
  "software": "Folia",
  "plugin_version": "1.4.0",
  "mc_version": "26.1.2"
}
```

| Field | What it is | Why |
| --- | --- | --- |
| `server_id` | A random UUID the server generates for itself on first run and stores in `config.yml` | So distinct servers can be counted without identifying them |
| `players` | The current online player count, a single number | The total players figure |
| `software` | `Folia`, `Paper`, `Purpur` and so on | Which platforms run the plugin |
| `plugin_version` | The BlackTimber version | Version adoption |
| `mc_version` | The Minecraft version | Which game versions are in use |

## What is never sent

- No IP address is stored. The server's IP is visible to the web server for the moment
  the request arrives, used only to rate limit abuse, and is never written next to the
  data. It is not in the table at all.
- No player names, no player UUIDs, no chat, no coordinates, no world data.
- No hostname, no server name, no MOTD, no plugin list.
- Nothing that identifies a person. The `server_id` is random and is not derived from
  your IP, your hardware or anything personal.

## Where it goes and how long it stays

The data lands in a PostgreSQL table on `cebi.tr`, operated by the project author. Each
server is one row holding the fields above and a last seen timestamp. A server that has
not pinged for thirty five days is deleted. A daily peak of active servers and players is
kept so the thirty day trend chart has history. That is the entire footprint.

The public endpoints are read only:

- `https://cebi.tr/api/blacktimber/badge.svg` is the chart embedded in the README.
- `https://cebi.tr/api/blacktimber/stats` returns the same totals as JSON.

## Legal basis

The data is anonymous and aggregate. It carries no personal data, so it falls outside the
scope of the GDPR and the Turkish KVKK, which both govern personal data. This is the same
model the widely used bStats service follows, and it is consistent with the Minecraft
EULA, which permits plugins that do not collect personal data or present themselves as
official. BlackTimber is an independent project, not affiliated with Mojang or Microsoft.

## How to opt out

Set one line in `config.yml` and reload or restart:

```yaml
telemetry: false
```

With telemetry off, no `server_id` is generated and not a single request is ever sent.
The plugin works exactly the same in every other respect.

## Questions

Open an issue on the [repository](https://github.com/can61cebi/blacktimber/issues) or
contact `can@cebi.tr`.
