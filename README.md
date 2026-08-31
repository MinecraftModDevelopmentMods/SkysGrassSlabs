# Sky's Grass Slabs

Sky's Grass Slabs is a small Minecraft mod intended to provide dirt and grass
slabs, natural grass spreading between full blocks and slabs, smooth grass-slab
transitions on newly generated terrain, and safe conversion of relevant blocks
from legacy BuildingBricks worlds.

The first target is Minecraft 1.18.2 with Forge 40.3.0 and Java 17. The stable
mod ID is `skysgrassslabs`, the Java package root is
`zone.moddev.mc.skysgrassslabs`, and the initial development version is
`0.1.0.118021`.

## Current state

This repository is an implementation-ready scaffold. It contains a minimal
Forge entry point, reproducible Gradle archive settings, a basic project
contract test, and the design, migration, testing, and repository handoff in
`docs/`. It does not yet register blocks or perform world generation.

Sky's Grass Slabs is intentionally standalone. OreSpawn is neither a required
nor optional runtime dependency for the initial implementation. Compatibility
with OreSpawn and Mineralogy must be proved through integration tests.

Read these before implementing:

- [Architecture and scope](docs/ARCHITECTURE.md)
- [Legacy BuildingBricks migration](docs/LEGACY-MIGRATION.md)
- [Testing and evidence](docs/TESTING.md)
- [Repository and release workflow](docs/REPOSITORY.md)

## Local build

Use Java 17 and a project-local shared cache:

```powershell
$env:JAVA_HOME='<Java 17 JDK>'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$env:GRADLE_USER_HOME='<repository>/.gradle-verify-cache'
./gradlew.bat clean test build --no-daemon
./gradlew.bat genEclipseRuns eclipse --no-daemon
```

Do not treat a successful build as gameplay, migration, packaged-runtime, or
manual acceptance evidence. The required gates are defined in `docs/TESTING.md`.

## Licensing status

Project metadata currently uses `All Rights Reserved` as a conservative
placeholder. Confirm the intended public project license before the first
public push or release. BuildingBricks is MIT licensed; retain its copyright
and permission notice if substantial code is copied rather than independently
reimplemented.
