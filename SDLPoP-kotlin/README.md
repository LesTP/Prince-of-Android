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

The Layer 1 replay-regression harness has a dedicated workflow:

```bash
./gradlew layer1ReplayRegression
```

It reads the 13 C reference traces from `../SDLPoP/traces/reference`, writes
Kotlin trace artifacts under `build/oracle/layer1-regression`, compares traces
with the 310-byte state format, and reports the first divergent frame, field,
expected value, and actual value. Until Module 14 adds the Kotlin replay runner,
the workflow uses a producer hook; Module 14 owns feeding `.P1R` inputs through
the translated game loop and replacing that hook with real Kotlin trace output.

## Current Status

Module 13: Layer 1 replay-regression harness in progress.

See `../DEVPLAN.md` for the full implementation plan.
