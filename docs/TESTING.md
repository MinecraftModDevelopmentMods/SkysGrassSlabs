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

`check` includes 35 focused Java tests plus a Forge runtime mod used only during
the build. The runtime creates a fresh world, executes 74 gameplay and 8 world
generation assertions,
stops, reloads the same world, and verifies schema state. Probe classes and
resources are excluded from distributable jars.

## Focused automated coverage

- permanent registry IDs, both independent configuration settings and version metadata, recipe sorter identity,
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
- migration mappings, orientation counts, schema persistence, replacement
  gating, chunk markers, configuration backup/arbitration, and failure fallback;
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
   and ownership by only one generator. Test both default retained content and
   explicitly enabled replacement.
3. Sky's Grass Slabs plus the current OreSpawn and Mineralogy 1.10 candidates.
4. Complete disposable Sylvester copies with BuildingBricks present and
   replacement disabled, present and replacement enabled, and absent for
   missing mapping recovery.

Completed local evidence includes:

- solo and BuildingBricks fresh/reload: 74 gameplay and 8 worldgen checks;
- OreSpawn `4.0.8.110021` plus Mineralogy `6.0.1.110021`: fresh/reload, same
  74/8 checks, active Mineralogy provider, no audit failure or crash directory;
- complete Sylvester default coexistence, first and second forced migrations,
  and missing mapping recovery with the exact totals in
  `LEGACY-MIGRATION.md` and unchanged source fingerprint.
- accepted manual new terrain testing in a disposable Sylvester copy: 1,694
  new chunks contained 46,692 correctly oriented Sky grass slabs, no newly
  generated BuildingBricks slabs, and active OreSpawn and Mineralogy terrain.

The default Sylvester coexistence check retained the same supported legacy
block and item totals without creating migration markers or changing counters.
The missing mod check remapped the supported IDs and preserved orientation.
Forge's warning contained 110 remaining BuildingBricks block and item registry
entries, but neither supported slab ID. The disposable run confirmed the
warning, automatic backup, and subsequent removal path rather than hiding any
unsupported content.

New terrain takeover must use normal player chunk tracking. The build only
probe starts with virgin Overworld region `r.122.0.mca` and may try a bounded
series of further unused regions until eligible grass terrain is encountered.
It requires Sky grass slabs, no newly generated BuildingBricks grass slabs,
and an unchanged already disabled BuildingBricks generator setting. A real
player journey to new terrain remains part of manual acceptance.

## Final artifact gate

Build twice from a clean state and require identical SHA-256 values for the main
jar. Audit all release jars for expected metadata, resources, licenses, LF line
endings, absence of tests, probes, local paths and local context, and exact
checksums.

Manual visual and gameplay acceptance is complete for grass tinting, grass and
dirt snow caps, joins, support dirtification, stable turf fields, turf, paths,
placement, breaking and generated slopes. The accepted disposable world test
used the same current OreSpawn and Mineralogy candidates as the integration
matrix.

## Qualified 1.0.0.110021 artifacts

Independent Linux builds on the fork and MMD produced byte for byte identical
publication artifacts:

- main jar: `78,749` bytes,
  `C51C44B21814445DAC643C0221F91D0E4A5B36CE9E69D11636A1C0FBCFAE552F`
- sources jar: `48,056` bytes,
  `17B98EF5D8790D373C211E7D5EF3D6F89BDB9AB2ADB0CE32CBB36E671DB4DD1F`
- Javadocs jar: `141,691` bytes,
  `E6C99AB73B9896B9EC634968E6415F80FD5E818A1DBE9D8C2183871A70BF6A59`

Two local Windows clean builds were also identical. Their main and sources
jars match the Linux artifacts above. The Java 8 Javadoc formatter has a small
platform specific whitespace difference: the Windows Javadocs jar is `141,729`
bytes with SHA-256
`D657E256ECCADE5250B21C9067AFEC74B79E1A7666C61DE197F8E4C0857FEA38`.
The release dispatcher runs on Linux and uses the publication artifact listed
above.

All production classes use Java 8 bytecode. The archive audit found the
expected licences and resources and found no credentials, machine paths,
agent material, test probes or local evidence. Licence entries are normalized,
which makes the distributable main and sources jars identical on Windows and
Linux.
