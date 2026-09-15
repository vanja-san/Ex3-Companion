# Development

## Building

Requirements: JDK 25+, Internet connection (Gradle dependencies). On Windows use `gradlew.bat` instead of `./gradlew`.

```bash
# Build the mod jar (output: build/libs/Ex3Companion-v${version}-mc26.2-Fabric.jar)
./gradlew build

# Run a dev client
./gradlew runClient

# Run a dev server
./gradlew runServer

# Regenerate language files (datagen)
./gradlew runDatagen
```

If a task reports `UP-TO-DATE` but the output looks wrong (e.g. a stale resource in `build/resources`),
force it with `./gradlew clean build` or add `--rerun-tasks`.

### Known dev caveats

- `runServer` in dev fails with *"resource 'ex3.client.mixins.json' was invalid or could not be read"*.
  This is a Loom `splitEnvironmentSourceSets` quirk: the client-only mixin config is declared in
  `fabric.mod.json` but its resources are not on the dev server classpath. Production jars include
  both mixin configs, so dedicated servers are unaffected. Use `runClient` or a production jar for testing.
- Stale/incremental Gradle output is a common cause of "resource not found" — verify
  `build/resources/**` before blaming the code.

## Project Structure

```
src/
├── main/                        # Common (server + client)
│   ├── kotlin/mod/ex3/companion/
│   │   ├── companion/           # Core logic (entity, brain, combat, flight, health, core slot manager)
│   │   ├── command/             # Dev commands
│   │   ├── config/              # Server gameplay config (JSON, synced to clients; operator check via MC 26.2 permissions)
│   │   ├── item/                # Companion Core item
│   │   ├── mixin/               # Server/common mixins (common, Kotlin)
│   │   ├── network/             # Payloads: slot sync, server config sync (S2C) / config update (C2S, op-gated)
│   │   ├── recipe/              # Custom recipes (glass dye, crafting)
│   │   └── registry/            # Items, entities, data components
│   ├── generated/               # Datagen output (lang files)
│   └── resources/               # Assets, recipes, mixin config, fabric.mod.json
└── client/                      # Client-only
    ├── kotlin/mod/ex3/companion/client/
    │   ├── config/              # YACL config screen + client-only config (ex3-companion-client.json)
    │   └── render/              # Companion renderer, dynamic lights
    ├── java/mod/ex3/companion/mixin/   # Client mixins (Java, for static shadows)
    └── resources/               # Client mixin config
```

### Config sync flow

- Server loads `config/ex3-companion.json` at startup and pushes it to every player on join
  (`ConfigSyncPayload`). Clients adopt it as the mirrored `CompanionConfig`.
- The Mod Menu screen has a **Server** tab (gameplay) and a **Client** tab (local-only options —
  stored in `config/ex3-companion-client.json`).
- Server-tab controls are enabled only when the player is an operator (MC 26.2
  `Permissions.COMMANDS_GAMEMASTER`, i.e. command-level ≥ 2); non-operators see the page read-only/greyed.
- Operator edits are sent back via `ConfigUpdatePayload`; the server re-checks operator status,
  applies the config, saves it to disk, and rebroadcasts to all players.

## Tech Stack

- **Kotlin** — main language (entrypoints, entity logic, mixins)
- **Java** — client mixins (static shadows for container screens)
- **Fabric Loom 1.17** — build toolchain with split source sets
- **SpongePowered Mixin** — class transformations (`ex3.mixins.json`, `ex3.client.mixins.json`)
- **YACL** — config screen library
- **LambDynamicLights API** — dynamic entity lighting (compiled against stubs in `libs/`, not bundled)
- **Mojang mappings** — official names, no Yarn

## Dependencies

| Dependency | Version | Type |
|------------|---------|------|
| Minecraft | 26.2 | Required |
| Fabric Loader | ≥ 0.19.5 | Required |
| Fabric API | 0.160.0+26.2 | Required |
| Fabric Language Kotlin | 1.14.1+kotlin.2.4.20 | Required |
| YACL (YetAnotherConfigLib) | 3.9.6+26.2-fabric | Required |
| Java | ≥ 25 | Required |
| LambDynamicLights | 4.12.4+26.2 | Optional (for dynamic lighting) |
| Mod Menu | 20.0.2+26.2 | Optional (for config screen) |
| JEI / Jade / Sodium / Iris / Resourcify | latest | Dev-only (`localRuntime`, never bundled) |

Versions live in `gradle.properties`. Required-mod versions are the minimum the mod is built
against; `fabric.mod.json` keeps broad `*` / range constraints so newer builds satisfy them.