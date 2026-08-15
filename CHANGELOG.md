# Changelog

All notable changes to this project are documented here.

Releases are also published automatically to GitHub Releases when a `v*` tag is pushed
(see `.github/workflows/release.yml`).

## [1.0.0] - 2026-08-15

### Added
- Initial release: duel plugin with arenas, kits, parties, queues, ranked ladder, tournaments, FFA, and more.
- RTP queue with configurable world list and countdown.
- CI build workflow (`.github/workflows/build.yml`).
- Auto-release workflow with changelog generation (`.github/workflows/release.yml`).

### Fixed
- Arena boundary no longer yanks players on the Y axis (fixes "ghost" players that couldn't be hit).
- Kit editing now enforces owner/`updraftduels.kit.edit` permissions.
- Public kit management gated behind `updraftduels.kit.managepublic`.
