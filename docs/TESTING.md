# Testing and evidence

## Environment

- Minecraft: 1.10.2
- Forge: 12.18.3.2511
- ForgeGradle: 7.0.34
- mappings: stable `29-1.10.2`
- Gradle wrapper: 9.6.1
- Gradle runtime: Java 17
- production/test/Minecraft toolchain: Temurin `8.0.502+7`
- Mavenizer build toolchain: Java 25
- Gradle user home: a dedicated shared cache outside the checkout

Run Gradle sequentially from the nested checkout. Do not run concurrent builds
against the shared cache.

## Primary clean gate

```powershell
$env:JAVA_HOME='<Java 17 JDK>'
$env:GRADLE_USER_HOME='<dedicated Gradle cache>'
./gradlew.bat clean check build javadoc verifyReleaseArtifacts `
  writeReleaseChecksums verifyEclipseProductionClasspath --no-daemon --stacktrace
```

`check` includes focused Java tests plus a Forge runtime mod used only during
the build. The runtime creates a fresh world, executes 72 gameplay and 8 world
generation assertions,
stops, reloads the same world, and verifies schema state. Probe classes and
resources are excluded from distributable jars.

## Focused automated coverage

- permanent registry IDs, configuration and version metadata, recipe sorter identity,
  resources, legacy model parents, and repository hygiene;
- slab metadata conversions, top and bottom geometry, combination normalization,
  drops, Silk Touch, plants, bonemeal, grass lifecycle, and turf support;
- grass and dirt snow states that are not saved, untinted snow tops, matching snow edged
  dirt sides, and unchanged slab metadata while snow appears and disappears
  above or beside a slab;
- top and bottom grass slab support dirtification during placement, world smoothing,
  neighbour updates, and random ticks;
- covered target rejection and immediate repair after both mod and vanilla
  spread paths, including each grass covering's own support exclusion;
- attaching only one turf eating task, vanilla animation timing, destruction
  without a drop, wool regrowth, child growth, and `mobGriefing` behavior;
- turf recipe matching and exact unchanged shovel/dirt remainders;
- compatible Forge shovel detection and flattening interactions;
- migration mappings, orientation counts, schema persistence, configuration
  backup/arbitration, and failure fallback;
- worldgen eligibility, cliffs, flat terrain, fluid, occupied targets, block
  entities, loaded east and south borders, west and north omission, and second pass
  idempotence;
- fixed seed 9×9 chunk decisions across forward, reverse, and shuffled orders;
- a 256-column benchmark with a 5 ms regression ceiling.

The focused benchmark measured approximately 1,134 ns per synthetic
256-column decision pass on the qualification machine. It is a regression
signal, not a performance promise for other machines.

## Eclipse and development launches

Use the checkout's parent directory as the Eclipse workspace and import the
nested project as an existing Gradle project.

```powershell
./gradlew.bat genEclipseRuns verifyEclipseProductionClasspath --no-daemon
```

Then refresh the Gradle project and clean it in Eclipse. Buildship owns the
classpath; do not manually add a second Gradle dependency container.

ForgeGradle's 1.10 Slime Launcher is a Java multirelease jar. Forge 1.10's old
ASM scanner logs and ignores its Java 11 entry while the launcher continues.
The same warning occurs in the qualified OreSpawn 1.10 development launch. A
client pass requires Sky's Grass Slabs to be identified, OpenAL initialized,
the 512×512 texture atlas built, and Forge to report all four mods loaded with
no missing project model or texture.

## Packaged runtime matrix

The exact reobfuscated candidate must pass Java 8 dedicated server checks with
a fresh world and after a reload:

1. Forge plus Sky's Grass Slabs.
2. Sky's Grass Slabs plus BuildingBricks 1.10.2-2.0.13, including config backup
   and ownership by only one generator.
3. Sky's Grass Slabs plus the current OreSpawn and Mineralogy 1.10 candidates.
4. The complete disposable Sylvester fixture.

Completed local evidence includes:

- solo and BuildingBricks fresh/reload: 72 gameplay and 8 worldgen checks;
- OreSpawn `4.0.8.110021` plus Mineralogy `6.0.1.110021`: fresh/reload, same
  72/8 checks, active Mineralogy provider, no error or crash directory;
- complete Sylvester first and second migrations with the exact totals in
  `LEGACY-MIGRATION.md` and unchanged source fingerprint.

## Final artifact gate

Build twice from a clean state and require identical SHA-256 values for the main
jar. Audit all release jars for expected metadata, resources, licenses, LF line
endings, absence of tests, probes, local paths and local context, and exact
checksums.

Manual visual acceptance remains pending for grass tinting; visual grass/dirt
snow caps in several mountain arrangements; top/bottom joins; support
dirtification and stable turf fields; sheep animation; turf geometry and
spread; path height; shovel interaction; placement; breaking; and generated
slopes.
