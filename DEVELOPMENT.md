# Development

## Building

Requirements: JDK 25+, Internet connection (for Gradle dependencies).

```bash
# Build the mod jar
./gradlew build

# Run the mod in a dev client
./gradlew runClient

# Run a dev server
./gradlew runServer

# Generate language files
./gradlew runDatagenClient
```

The built jar is in `build/libs/`.

## Project Structure

```
src/
├── main/                    # Common (server + client)
│   ├── kotlin/              # Kotlin source
│   │   ├── companion/       # Core logic (entity, brain, combat, flight, etc.)
│   │   ├── config/          # JSON config (YACL-compatible)
│   │   ├── registry/        # Items, entities, components
│   │   └── mixin/           # Server-side mixins
│   ├── java/                # Java source (network payloads)
│   └── resources/           # Assets, recipes, mixin configs
├── client/                  # Client-only
│   ├── kotlin/              # Renderer, config screen, dynamic lights
│   ├── java/                # Client mixins (Java, for static shadows)
│   └── resources/           # Client mixin config
└── generated/               # Datagen output (lang files)
```

## Tech Stack

- **Kotlin** — main language (entrypoints, entity logic, mixins)
- **Java** — network payloads (low-level NBT)
- **Fabric Loom 1.17** — build toolchain with split source sets
- **SpongePowered Mixin** — class transformations
- **YACL** — config screen library
- **LambDynamicLights API** — dynamic entity lighting

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
