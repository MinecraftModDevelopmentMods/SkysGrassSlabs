# Testing and evidence

## Environment

- Minecraft: 1.11.2
- Forge: 13.20.1.2588
- ForgeGradle: 7.0.34
- mappings: stable `32-1.11`
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

`check` includes 42 focused Java tests plus a Forge runtime mod used only during
the build. The runtime creates a fresh world, executes 74 gameplay and 8 world
generation assertions,
stops, reloads the same world, and verifies schema state. It also runs packaged
fresh/reload and 1.10.2 forward upgrade/reload servers using the exact
reobfuscated jar. Probe classes and resources are excluded from distributable
jars.

## Focused automated coverage

- permanent registry IDs, both independent configuration settings and version metadata, recipe sorter identity,
  resources, legacy model parents, and repository hygiene;
- all 18 locale files, exact key parity, reviewed regional wording, UTF-8
  validity, LF endings, and duplicate or blank value rejection;
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
- migration mappings, 1.11 compatibility holders, orientation counts, schema
  persistence, replacement gating, chunk markers, configuration
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

The development client gate requires Sky's Grass Slabs to be identified,
OpenAL to initialize, the texture atlas to build, and Forge to reach the title
screen without a missing project model, texture, or startup crash.

## Packaged runtime matrix

The exact reobfuscated 1.11.2 candidate passes Java 8 dedicated server checks
with a fresh world and after a reload. The build only runtime reports all 74
gameplay and 8 world generation checks complete in both runs, with schema-1
state readable and no cascading chunk generation warning.

The committed forward fixture was generated with the accepted
`SkysGrassSlabs-1.0.0.110021.jar`, whose SHA-256 is
`2030960E217C3F61AE4919C91058696B02F9FAE570BE1CD7B698696EA7BEB861`.
The fixture ZIP SHA-256 is
`D6923BFFE062C1F0C454190AB11F031825949DF8080D8000133A723DEC2770BF`.
The first packaged 1.11.2 load and its reload retain every fixture block,
orientation, item count, custom item NBT, turf, path and world-state value.
There is no missing Sky-owned content.

A larger optional gate copied the already converted Sylvester world into the
build directory and launched only that disposable copy. All original Sky slabs
and 7,186 saved dirt slab items survived. The schema-1 counters remained
1,656,276 migrated grass blocks, 2,968 migrated dirt blocks, 7,186 migrated
dirt items and 6,663 unsupported shapes. Minecraft completed nearby old
terrain, producing 5,251 additional correctly oriented Sky grass slabs. The
new total of 1,661,527 was unchanged on reload. See `LEGACY-MIGRATION.md`.

BuildingBricks has no supported Minecraft 1.11.2 release. Its 1.10.2
coexistence, forced replacement, missing content recovery, OreSpawn and
Mineralogy checks remain historical qualification of the source world rather
than a claimed 1.11 runtime matrix.

## Final artifact gate

Build twice from a clean state and require identical SHA-256 values for the main
jar. Audit all release jars for expected metadata, resources, licenses, LF line
endings, absence of tests, probes, local paths and local context, and exact
checksums.

Manual visual and gameplay acceptance is still required for this 1.11.2
candidate. The equivalent 1.10.2 visuals and gameplay were accepted before the
port.

## Local 1.0.0.111021 candidate

Two clean Windows builds produced byte for byte identical release files:

- main jar: `87,685` bytes,
  `FC2F47D15F7C3B02AFB700ED6D10ED0BE59CAD67C740E10B2A4F8DA9D1EF229D`
- sources jar: `53,949` bytes,
  `2989D408AB55407B30A5860C200FBFB4C04D5C924EF6363F940DD5E18D6EB80E`
- Javadocs jar: `142,233` bytes,
  `3D938767321C1CB78E813ED7E33BFABFAC8EBD48BC6B7548270D02D13A0691B1`
- checksum file:
  `1E8EDF318F39DD46C32F1B78156774060AB02A4616CD7A9B560E8568CB6CD891`

The release audit found all 18 lowercase locale resources, licences and
metadata, Java 8 production classes, and no build-only probe, local path,
credential, local evidence or agent material.

## Historical 1.0.0.110021 artifacts

Independent Linux builds on the fork and MMD produced byte for byte identical
publication artifacts:

- main jar: `83,251` bytes,
  `2030960E217C3F61AE4919C91058696B02F9FAE570BE1CD7B698696EA7BEB861`
- sources jar: `52,199` bytes,
  `DC3A19FB1A465E8B3C8CA9CB3FE95FC7A466BB0A61DAC079D5DE63F681FBBD74`
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
