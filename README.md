# Ex³ Companion

A loyal floating companion for Minecraft: Java Edition. Craft its core, slot it into your inventory, and it follows you on your adventures — exploring caves, fighting mobs, and lighting the way.

<!-- TODO: добавить скриншоты / GIF превью -->
<!--
![Companion in action](preview/companion_showcase.gif)
![Crafting recipe](preview/crafting.png)
![Config screen](preview/config.png)
-->

## Features

### Companion Core

Craft a **Companion Core** and place it in the dedicated inventory slot (next to armor). The companion spawns automatically and stays by your side.

**Recipe:**

```
G C G
C L C
G C G
```

| G | C | L |
|---|---|---|
| Glass | Copper Ingot | Sea Lantern |

### Brain States

The companion has a 4-state AI brain:

| State | Behavior |
|-------|----------|
| **FOLLOW** | Hovers beside/behind the owner with 3-tier speed scaling (normal → fast → catch-up) |
| **EXPLORE** | Flies to nearby POIs (ores, chests, structures) and scans them with look control |
| **ATTACK** | Fires an END_ROD particle beam at hostile mobs near the owner; damage scales with level |
| **IDLE** | Slow drift when the owner is far away or offline |

### Combat Modes

Configurable via the config screen (Mod Menu → YACL):

| Mode | Behavior |
|------|----------|
| **Defender** (default) | Attacks mobs that target the owner or the companion |
| **Aggressive** | Searches for hostile mobs in an expanded radius (2×), even if unprovoked |
| **Strategic** | Adjusts behavior per mob type — keeps distance from Creepers, closes in on Skeletons/Blazes, prioritizes mobs attacking the owner |

### Leveling & Progression

- Kill assists grant XP (`xpPerKill`, default 5).
- XP to level up = `level × formulaMultiplier` (default multiplier: 15).
- Each level adds `perLevel` max health (default 2) and `damagePerLevel` beam damage (default 1).
- Health cap: 60 HP (configurable). Beam damage cap: 20.
- Level cap: 50.

### Healing

After reaching `unlockLevel` (default 10), the companion passively heals the owner when not in combat, within `range` blocks.

### Health & Death

- Base health: 20 HP. Regenerates at `regenPerTick × (1 + (level-1) × regenLevelScale)`.
- On owner death: companion despawns, core health halves, re-summons on respawn with slow regen.
- Takes damage only from mobs, projectiles, and lava. Immune to environmental damage.
- `invulnerable` config option makes it fully immune.
- Revive delay: 600 ticks (30 seconds).

### Dynamic Lighting

The companion emits light with smooth transitions:

- **Night**: full glow (light level 15).
- **Day + underground**: full glow (detects caves via sky-light level).
- **Day + above ground**: no glow.

Fade transitions are smooth (~0.24s) — no abrupt on/off flicker.

Requires [LambDynamicLights](https://modrinth.com/mod/lambdynamiclights).

### Projectile Dodging

Scans for incoming projectiles in an 8-block radius. Checks if the projectile is heading toward the companion and applies a perpendicular dodge force scaled by time-to-impact — harder dodge for closer threats.

### Visual Feedback

State-dependent particle effects:

| State | Particles |
|-------|-----------|
| FOLLOW | Happy Villager (sparkles near owner) |
| EXPLORE | Note particles (scan indication) |
| ATTACK | Smoke particles (combat) |
| IDLE | End Rod particles (ambient glow) |

### Exploration Memory

- Tracks explored chunks (up to 1024) to avoid revisiting the same areas.
- POI search deprioritizes explored chunks.
- Remembers Y-levels where ores were found.

### Dimension Travel

The companion follows the owner across dimensions (Overworld ↔ Nether ↔ End). Portal blocks do not teleport it independently.

### Renderer

Custom renderer with two block models:
- **Outer shell**: glass block (semi-transparent).
- **Inner core**: sea lantern (pulsing glow).

Animations: gentle bob, slow spin, pulsing light intensity.

## Configuration

Config file: `config/ex3-companion.json` (auto-created on first run).

All settings are also editable via **Mod Menu** → Ex³ Companion → Config Screen.

| Category | Key Settings |
|----------|-------------|
| **Health** | base, perLevel, cap, regenPerTick, regenLevelScale, reviveDelayTicks, invulnerable |
| **Combat** | damageBase, damagePerLevel, damageCap, intervalBase, searchRadius, fireRange, combatMode |
| **Healing** | unlockLevel, basePerTick, perLevelAbove, range |
| **Movement** | normalSpeed, fastSpeed, catchUpSpeed, exploreSpeed, teleportDistance, accel |
| **Exploration** | cooldownMin, cooldownVariance, chance, durationMin, searchRadius |
| **XP** | levelCap, formulaMultiplier |

## Requirements

| Dependency | Version | Type |
|------------|---------|------|
| Minecraft | 26.2 | Required |
| Fabric Loader | ≥ 0.19.3 | Required |
| Fabric API | 0.158.0+26.2 | Required |
| Fabric Language Kotlin | 1.13.13+kotlin.2.4.10 | Required |
| YACL (YetAnotherConfigLib) | 3.9.6+26.2-fabric | Required |
| Java | ≥ 25 | Required |
| LambDynamicLights | 4.12.3+26.2 | Optional |
| Mod Menu | 20.0.1+26.2 | Optional |

## Installation

### Client only (singleplayer / your side on a server)

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 26.2.
2. Copy `Ex3Companion-v1.1.0-mc26.2-Fabric.jar` into your `.minecraft/mods/` folder.
3. Also install Fabric API, Fabric Language Kotlin, and YACL into the same `mods/` folder.
4. Optionally add LambDynamicLights (dynamic lighting) and Mod Menu (config screen).
5. Launch the game.

### Client + Server (multiplayer, everyone sees the companion)

1. Install Fabric Loader on both the server and all clients.
2. Put `Ex3Companion-v1.1.0-mc26.2-Fabric.jar` + Fabric API + Fabric Language Kotlin + YACL into the `mods/` folder on **both** the server and every client.
3. Restart the server and have all players rejoin.
4. Optional mods (LambDynamicLights, Mod Menu) only need to be on the client side.

## License

[CC0 1.0 Universal](LICENSE) — public domain.

## Author

vanja-san — [GitHub](https://github.com/vanja-san)
