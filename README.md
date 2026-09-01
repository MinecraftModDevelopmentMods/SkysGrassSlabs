# Sky's Grass Slabs

Sky's Grass Slabs is a standalone Minecraft mod providing dirt, grass, and path
slabs, cuttable turf, natural grass spreading between full blocks and slabs,
and smooth grass-slab transitions on newly generated terrain.

The first target is Minecraft 1.18.2 with Forge 40.3.0, ForgeGradle 7.0.34,
and Java 17. The stable mod ID is `skysgrassslabs`, the Java package root is
`zone.moddev.mc.skysgrassslabs`, and the current beta version is
`0.2.0.118021`.

## Beta features

- Top, bottom, double, and waterlogged dirt and grass slabs.
- Biome-tinted and snowy grass rendering with vanilla-style decay and spreading.
- Top-grass-slab snow, plant, and bonemeal behaviour.
- Shovel flattening of dirt and grass slabs into lowered path slabs.
- Biome-tinted, carpet-height turf that spreads grass while it remains on dirt.
- Craft turf from grass plus any Forge-compatible shovel; the unchanged shovel
  and the matching dirt block or slab remain in the crafting grid.
- Use turf on a dry dirt slab to turn it directly into the matching grass slab.
- Dirt, grass, and seed helper recipes; path slabs remain creative/tool-created.
- Deterministic, chunk-owned smoothing of one-block grass transitions during
  new Overworld generation.
- A permanent world schema marker for future save migrations.

Sky's Grass Slabs is intentionally standalone. OreSpawn is neither a required
nor optional runtime dependency for the initial implementation. Compatibility
with OreSpawn and Mineralogy must be proved through integration tests.

Read these before implementing:

- [Architecture and scope](docs/ARCHITECTURE.md)
- [Legacy BuildingBricks migration](docs/LEGACY-MIGRATION.md)
- [Testing and evidence](docs/TESTING.md)
- [Repository and release workflow](docs/REPOSITORY.md)
- [Version and compatibility roadmap](docs/ROADMAP.md)
- [Current beta description and release notes](docs/BETA-0.2.0.118021.md)
- [0.1.0 beta evidence](docs/BETA-0.1.0.118021.md)

## Local build

Use Java 17 and a project-local shared cache:

```powershell
$env:JAVA_HOME='<Java 17 JDK>'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$env:GRADLE_USER_HOME='<repository>/.gradle-verify-cache'
./gradlew.bat clean test build --no-daemon
./gradlew.bat runGameTestServer --no-daemon
./gradlew.bat genEclipseRuns eclipse --no-daemon
```

Then use **Gradle > Refresh Gradle Project** in Eclipse. Buildship consumes the
generated FG7 dependency model and presents it as one **Project and External
Dependencies** container.

ForgeGradle's build-only Minecraft Mavenizer runs with a Java 25 toolchain to
prepare Forge 40 sources. The mod, development launches, tests, and shipped
bytecode remain on Java 17.

Do not treat a successful build as gameplay, migration, packaged-runtime, or
manual acceptance evidence. The required gates are defined in `docs/TESTING.md`.

## Licensing status

Sky's Grass Slabs is licensed under `LGPL-2.1-only`; see `LICENSE`,
`LICENSE.spdx`, and `NOTICE`. Copyright is held by SkyBlade1978. BuildingBricks
compatibility is independently implemented; retain BuildingBricks' MIT notice
if substantial upstream code is ever copied.
