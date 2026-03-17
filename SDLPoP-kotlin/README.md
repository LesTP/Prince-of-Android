# SDLPoP Kotlin Port

Kotlin/JVM port of SDLPoP (Prince of Persia) game logic, targeting Android.

## Project Structure

```
src/main/kotlin/com/sdlpop/
├── replay/     # .P1R replay file parser
├── game/       # Ported game logic (seg000-seg008)
└── oracle/     # State trace writer for replay validation

src/test/kotlin/com/sdlpop/
└── ...         # Unit tests
```

## Build Configuration

- **Kotlin version:** 1.9.22
- **JVM target:** 17 (Android-compatible)
- **Build tool:** Gradle with Kotlin DSL

## Building

```bash
./gradlew build
```

## Testing

```bash
./gradlew test
```

## Current Status

Phase 3, Step 4: Initial project structure created. Game logic porting has not yet begun.

See `../DEVPLAN.md` for the full implementation plan.
