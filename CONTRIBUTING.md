# Contributing to BlackTimber

Thanks for your interest in the project. BlackTimber aims to stay small, fast and
dependency free, so contributions are weighed against that goal.

## Reporting

Open an issue with the bug report or feature request template. For a bug, the BlackTimber
version and the exact server build matter most, since behaviour can differ between Folia
and Paper.

## Building

You need a Java 25 toolchain. Everything else comes through the Gradle wrapper.

```
git clone https://github.com/can61cebi/blacktimber.git
cd blacktimber
./gradlew build
```

The jar lands in `build/libs`.

## Code style

- Keep it dependency free. No new runtime dependencies and no database.
- Stay Folia safe. Touch block data only on the region thread that owns it, and never call
  the legacy Bukkit scheduler.
- Match the surrounding style: small classes, clear names, comments only where intent is
  not obvious from the code.
- Avoid allocation on the felling hot path.

## Pull requests

Keep each pull request focused on one change. Describe what it does and why, and note any
configuration or behaviour that changes. If it affects the menus or detection, a short note
on how you tested it on a live server helps a lot.
