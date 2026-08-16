# Changelog

All notable changes to this project are documented here.

Releases are also published automatically to GitHub Releases when a `v*` tag is pushed
(see `.github/workflows/release.yml`).

## Unreleased

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

### Changed
- Party and spectator messages are now fully configurable via `messages.yml` (party chat toggle, ready confirmation,
  party-duel accept/decline/challenge, free-cam and vanish toggles, plus various error messages).
- The duel sender is now told when their request is accepted; expired requests and opponents leaving during the
  countdown/teleport are announced; a post-duel stats line (wins/losses/ELO) is shown after every match.

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
