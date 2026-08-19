# Changelog

All notable changes to this project are documented here.

Releases are published automatically: when a new version lands on `main`, `auto-release.yml` tags it, builds
the plugin, and publishes the GitHub Release without any manual steps. Manually pushed `v*` tags are still
handled by `release.yml`.

## Unreleased

## [3.0.1] - 2026-08-16

### Fixed
- Spectators can no longer leave the duel arena (boundary enforcement now applies to spectators).

## [3.0.0] - 2026-08-16

### Changed
- Duel request expiry is now configurable via `duel.request-expire-seconds` in `config.yml` (default 60s).
- Expired duel requests now clean up automatically every 10 seconds instead of only when a new request is created.
- ELO rank sub-levels now use standard convention: I = lowest, III = highest (was reversed).

### Added
- PlaceholderAPI placeholder `%updraftduels_duel_arena%` — shows the arena name during a duel, or "None".
- PlaceholderAPI placeholder `%updraftduels_duel_gamemode%` — shows the gamemode (SOLO, DUO, TRIO, QUAD) during a duel, or "None".
- Configurable scoreboard (`scoreboard.title`, `scoreboard.lines`) with placeholders: %team_a%, %team_b%, %arena%, %duration%, %ruleset%, %gamemode%, %score%, %elo%, %wins%, %losses%, %wl%, %kills%, %deaths%.

## [1.2.0] - 2026-08-16

### Added
- Settings GUI expanded to 54 slots with new per-player toggles:
  - **Party Invites**: block incoming party invites when off.
  - **Spectators**: prevent others from spectating your duels when off.
  - **Scoreboard**: toggle the TAB scoreboard display (runs `/sb`).
  - **Chat Mentions**: receive a sound and styled message when someone types `@YourName` in chat.
  - **Auto Requeue**: automatically rejoin the same queue after a duel ends.
  - **Duel Requests**: block incoming duel requests when off.

### Fixed
- Cosmetics now persist across server restarts (saved to `cosmetics.yml`).
- Trails now play continuously while standing in the lobby/spawn.

## [1.1.0] - 2026-08-16

### Removed
- FFA feature (command, manager, model, config, permissions).

### Fixed
- Players who fall through the arena floor are pulled back up to their spawn instead of dropping into
  the void (configurable via `duel.arena-floor-pull-margin`); the teleport forces a client re-sync so
  other players no longer see a "fake body" where the player fell. Sumo/spleef rulesets are exempt
  (falling is the loss condition there).
- Anti-spam cooldowns now also apply to the kit-GUI duel flow, `/friend duel`, and the RTP queue.
- Knockback/no-damage rulesets (e.g. sumo) can now end: void damage is no longer cancelled.
- Game mode and flight state are saved/restored across duels (creative/spectator players are no longer forced to survival).
- A disconnect during the gate-rise animation no longer resurrects a cancelled duel.
- Duel start rejects unconfigured arenas on the explicit-name path (players no longer fight at the lobby).
- Queue matchmaking no longer loops infinitely when a queued player is already in a duel; stale requests from failed matches are cleaned up.
- Ranked/kit preference is preserved when a match requeues due to no available arena.
- Spectators are cleaned up on quit and teleported to the lobby at duel end (no more stuck in the arena world).
- Pending respawn locations are cleared on disconnect.
- Kit editor blocks shift/drag clicks and clears the cursor on close (closes duplication vector).
- Duplicate `spectator:` message keys merged (spectator messages no longer show raw keys).
- Database writes drain gracefully on shutdown instead of being silently dropped.
- Tournament matches now record their tournament id, so draws advance the bracket instead of hanging.
- `/settings set <ruleset>` now actually stores your preferred ruleset (used for duels without an explicit ruleset).
- Playtime is no longer discarded when a player rejoins before the previous session finishes saving.
- Arena snapshots/regeneration load chunks asynchronously and process blocks in chunks, so big arenas no longer freeze the server thread.
- Duel scoreboards no longer flicker (objective is kept instead of recreated every second).
- RTP matches load chunks asynchronously before teleporting (no more server freeze on match start).

### Added
- `arena.block-edit-whitelist` config option: arenas where players may freely break/place blocks during a duel.
- Automatic releases: pushing a new version to `main` creates the release tag, builds the plugin, and publishes
  the GitHub Release automatically (`.github/workflows/auto-release.yml`), so no manual tagging is needed.
- Update checker: on startup the plugin checks the GitHub releases API for a newer version and logs the current
  version, the latest version, and a download link to the console. Admins are reminded on join when an update is
  available, and `/uduels update` performs a manual check (all configurable under `update-checker` in `config.yml`,
  messages in `messages.yml`).
- Arena voting for direct duels: with `duel.vote-for-arena` enabled and 2+ arenas available, fighters pick the
  arena via a vote GUI before the countdown (`duel.vote-seconds` controls the timeout). Voting resolves early once
  everyone has voted.
- Auto-GG setting: per-player toggle in Settings that automatically sends "GG" to the opponent at the end of each duel.

### Changed
- Party and spectator messages are now fully configurable via `messages.yml` (party chat toggle, ready confirmation,
  party-duel accept/decline/challenge, free-cam and vanish toggles, plus various error messages).
- The duel sender is now told when their request is accepted; expired requests and opponents leaving during the
  countdown/teleport are announced; a post-duel stats line (wins/losses/ELO) is shown after every match.
- Performance: duel scoreboards no longer re-query stats and reschedule a sync task every second per player;
  ELO/W-L lines only refresh when the cached stats actually change. The anticheat exemption loop (several plugin
  lookups per move event) now runs once per player instead of on every move.

## [1.0.0] - 2026-08-15

### Added
- Initial release: duel plugin with arenas, kits, parties, queues, ranked ladder, tournaments, and more.
- RTP queue with configurable world list and countdown.
- CI build workflow (`.github/workflows/build.yml`).
- Auto-release workflow with changelog generation (`.github/workflows/release.yml`).

### Fixed
- Arena boundary no longer yanks players on the Y axis (fixes "ghost" players that couldn't be hit).
- Kit editing now enforces owner/`updraftduels.kit.edit` permissions.
- Public kit management gated behind `updraftduels.kit.managepublic`.
