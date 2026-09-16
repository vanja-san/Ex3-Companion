<div align="center">

<!--
    BANNER — drop your logo / preview here when ready:
    <img src="screenshots/banner.png" alt="Ex³ Companion" width="640">
-->

# Ex³ Companion

*A loyal floating companion for Minecraft: Java Edition — craft its core, slot it in, and it follows you on your adventures.*

[![Version](https://img.shields.io/badge/Version-1.3.5-181717)](https://github.com/vanja-san/Ex3Companion)
[![Minecraft](https://img.shields.io/badge/Minecraft-26.3-red)](#)
[![Mod Loader](https://img.shields.io/badge/Mod%20Loader-Fabric-blue)](https://fabricmc.net)
[![License](https://img.shields.io/badge/License-CC0--1.0-brightgreen)](LICENSE)

</div>

<!--
    SCREENSHOTS — add your preview images here:
    ![Companion following the player](screenshots/follow.png)
    ![Crafting recipe](screenshots/crafting.png)
    ![Config screen](screenshots/config.png)
-->

## Features

- **4-state AI** — follows you, explores nearby points of interest, attacks mobs, and idles when you are far away
- **Combat modes** — Defender, Aggressive, and Strategic (per-mob-type tactics)
- **Progression** — gains XP from kill assists, levels up health, damage, regen, and unlocks healing
- **Passive healing** — heals the owner out of combat after level 10
- **Anvil naming** — rename the Companion Core in an anvil to give your companion a custom name
- **Dynamic lighting** — glows like a torch at night and in caves (optional, needs LambDynamicLights)
- **Projectile dodging** — weaves away from incoming arrows and projectiles
- **Exploration memory** — remembers explored chunks and ore Y-levels
- **Dimensions** — follows you through the Overworld, Nether, and End
- **Fully configurable** — JSON config plus an in-game settings screen

## Getting Started

1. Craft a **Companion Core** (glass + redstone block + emerald block in a ring pattern).
2. Right-click the core in your hand to equip it into the dedicated companion slot (below your armor).
3. Your companion spawns and follows you. Kill assists give it XP; it levels up and grows stronger.

**Naming:** put the core in an anvil and type a name. Your companion adopts it — the name persists across saves and respawns.

## Configuration

Settings are edited in-game via **Mod Menu → Ex³ Companion** and split into two categories:

- **Server** — gameplay tuning, stored in `config/ex3-companion.json` on the server. The server syncs its values to every connected player; operators can edit them live (changes apply and save server-side), non-operators see the page as read-only/inactive.
- **Client** — local-only options, stored in `config/ex3-companion-client.json` on your machine (e.g. the companion's dynamic light).

| Group | Key settings |
|-------|--------------|
| Health | base, per-level growth, cap, regen, revive delay, invulnerable |
| Combat | damage, attack interval, search radius, mode, XP per kill |
| Healing | unlock level, rate, range |
| Movement | follow speed, catch-up speed, teleport distance |
| Exploration | cooldown, chance, duration, search radius |
| XP | level cap, formula multiplier |
| Client · Rendering | dynamic light toggle |

## Requirements

| Dependency | Version | Type |
|------------|---------|------|
| Minecraft | 26.3 | Required |
| Fabric Loader | ≥ 0.19.5 | Required |
| Fabric API | 0.160.5+26.3 | Required |
| Fabric Language Kotlin | 1.14.1+kotlin.2.4.20 | Required |
| YACL (YetAnotherConfigLib) | 3.9.6+26.3-fabric | Required |
| Java | ≥ 25 | Required |
| LambDynamicLights | 4.13.0+26.3 | Optional |
| Mod Menu | 21.0.0-beta.1+26.3 | Optional |

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 26.3.
2. Copy `Ex3Companion-v1.3.5-mc26.3-Fabric.jar` into `mods/` (on your server **and** every client to see it on multiplayer).
3. Add the required dependencies from the table above to the same `mods/` folder.
4. Launch the game.

## Building & Development

See [DEVELOPMENT.md](DEVELOPMENT.md).

## License

[CC0 1.0 Universal](LICENSE) — public domain.

## Author

vanja-san — [GitHub](https://github.com/vanja-san)