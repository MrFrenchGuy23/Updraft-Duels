# UpdraftDuels

A comprehensive Minecraft duels plugin for Paper 1.21+ featuring arenas, kits, parties, friends,
ranked queues, tournaments, and more.

## Features

- **Duels** – 1v1 and team duels with configurable rounds, score, and win conditions
- **Arenas** – WorldEdit-style corner selection, team spawns, auto-regeneration, gates
- **Kits** – Personal kits (`/kit`) and public/pre-made kits (`/pk`) with permission gating
- **Queues** – Arena queues, ranked queues, and RTP (random-teleport) queues that start matches in a configured world
- **Parties** – Create, invite, chat, ready-check, and party-vs-party duels
- **Friends** – Add/remove friends, auto-accept duel requests, online notifications
- **Ranked ladder** – ELO, ranks, divisions, seasons, and inactivity decay
- **Tournaments** – Create, join, bracket-style eliminations
- **Cosmetics** – Kill effects, victory animations, trails, death messages
- **Spectating** – Follow ongoing duels, free-cam, vanish
- **PlaceholderAPI** – Expansion with stats, rank, and season placeholders

## Requirements

- Java 21
- Paper 1.21+ (or compatible Spigot fork)
- Optional: [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/)

## Building

```sh
mvn clean package
```

The shaded jar is written to `target/UpdraftDuels-1.0.0.jar`. Drop it into your server's `plugins/` folder and restart.

## Installation

1. Put the jar in `plugins/`
2. Start the server – the plugin generates `config.yml`, `messages.yml`, and `gamemodes.yml`
3. Set the lobby: `/uduels setlobby`
4. Create an arena with `/duelarena`, set corners and spawns, then fight

## Getting Started

```text
/duelarena create myarena        create an arena
/duelarena setpos1 myarena       mark corner 1 (bottom)
/duelarena setpos2 myarena       mark corner 2 (top)
/duelarena setspawn myarena a    set team A spawn
/duelarena setspawn myarena b    set team B spawn
/uduels setlobby                 set the lobby location
/duel <player>                   challenge someone
```

## Commands

| Command | Description |
| --- | --- |
| `/duel <player\|accept\|deny\|spectate>` | Duel requests, accepts, spectating |
| `/duelarena <create\|delete\|setpos1\|setpos2\|setspawn\|list\|info>` | Arena management |
| `/kit <create\|edit\|delete\|list>` | Personal kits (`/k1`–`/k9` shortcuts) |
| `/pk <create\|edit\|delete\|list>` | Public pre-made kits |
| `/party <create\|invite\|accept\|leave\|disband\|duel\|list\|chat>` | Party system |
| `/friend <add\|remove\|list\|duel\|toggleautoaccept>` | Friends system |
| `/profile [player]` | View duel stats |
| `/queue <arena>`, `/queue ranked` | Join the queue |
| `/rtpqueue` | Join the random-teleport queue |
| `/ranked` | Open the ranked queue |
| `/tournament <create\|join\|leave\|start\|info\|list>` | Tournaments |
| `/cosmetics` | Kill effects, trails, victory animations, death messages |
| `/leaderboard [kills\|deaths\|playtime]` | Leaderboards |
| `/season [info\|startnew\|resetelo]` | Season management |
| `/uduels <setlobby\|lobby\|reload\|setpos1\|setpos2\|gateinfo>` | Admin commands |
| `/settings [rules]` | View/set rulesets |
| `/refill`, `/anvil` | Utility commands |

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `updraftduels.duel` | everyone | Send duel requests |
| `updraftduels.duel.spectate` | everyone | Spectate duels |
| `updraftduels.kit.create` | everyone | Create personal kits |
| `updraftduels.party` | everyone | Use the party system |
| `updraftduels.friend` | everyone | Use the friends system |
| `updraftduels.tournament` | everyone | Join tournaments |
| `updraftduels.cosmetics` | everyone | Use cosmetics |
| `updraftduels.arena.create` | op | Create arenas |
| `updraftduels.arena.manage` | op | Manage arenas |
| `updraftduels.kit.createpublic` | op | Create public kits |
| `updraftduels.kit.managepublic` | op | Manage public kits |
| `updraftduels.kit.edit.<name>` | – | Edit a specific kit |
| `updraftduels.kit.public.<name>` | op | Access a specific public kit |
| `updraftduels.admin` | op | Admin commands |
| `updraftduels.tournament.create` | op | Create tournaments |
| `updraftduels.tourney.bc` | false | Tournament broadcast |
| `updraftduels.signs.create` | op | Create queue signs |

## Configuration

The plugin generates three files on first run:

- **`config.yml`** – gameplay settings: duel countdown, RTP queue worlds/radius, gate, seasons, ranks, decay, cosmetics, database (SQLite or MySQL), and more
- **`messages.yml`** – all plugin messages, supports `&` color codes
- **`gamemodes.yml`** – gamemode/arena assignments for the queue

### RTP queue

The RTP queue teleports matched players to a configured world and starts a match after a countdown,
without loading a kit (players keep their own gear).

```yaml
rtpqueue:
  world:
    - "rtp_world_1"
    - "rtp_world_2"
  radius: 1000
  min-distance: 100
  countdown-seconds: 5
```

### Database

```yaml
database:
  type: SQLITE          # or MYSQL
  sqlite-file: data.db
  mysql:
    host: localhost
    port: 3306
    database: updraftduels
    username: root
    password: ""
```

## Placeholders

With PlaceholderAPI installed, use `%updraftduels_<placeholder>%` in messages/scoreboards
(ELO, wins, losses, kills, win streak, rank tier, season, and more).

## Troubleshooting

- **Players get teleported to the wrong world / can't be hit during duels** – enable `duel.debug: true` in
  `config.yml` and check the `[DuelDebug]` console lines for the arena's spawn world vs. its `boxX/boxZ` bounds.
- **Arena boundary warnings while standing inside** – ensure `pos1`/`pos2` horizontally cover your spawn points.

## License

GPLv3 – see [LICENSE](LICENSE).
