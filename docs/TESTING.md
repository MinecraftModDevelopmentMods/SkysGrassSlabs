# Testing and evidence

## Environment

- Minecraft: 1.18.2
- Forge: 40.3.0
- ForgeGradle: 7.0.34
- Mappings: official 1.18.2
- Gradle wrapper: 9.6.1
- Java runtime and bytecode: 17
- Verified local JDK: `<Java 17 JDK>`
- Build-only Mavenizer toolchain: Java 25
- Verified local Mavenizer JDK: `<Java 25 JDK>`
- Gradle user home:
  `<repository>/.gradle-verify-cache`

Do not run Gradle concurrently against this checkout/cache. Review complete
logs and crash-report directories after every client, server, or GameTest run.

## Eclipse

Use `<Eclipse workspace>` as the Eclipse workspace and import
`SkysGrassSlabs` as an existing Gradle project. Generate or refresh the launch
configuration with the prescribed Gradle environment:

```powershell
./gradlew.bat genEclipseRuns eclipse --no-daemon
```

After the command completes, use **Gradle > Refresh Gradle Project**. The
`eclipse` task keeps Forge's mapped libraries in the FG7 model so Buildship can
consume them; the refresh then exposes them through one **Project and External
Dependencies** container. The verification also pins Buildship to the
prescribed Gradle home, checks processed resources, and excludes test code from
production launches. ForgeGradle 7 generates root `run*.launch` files using
`net.minecraftforge.launcher.Main` and Slime Launcher; use those launches
rather than any obsolete ForgeGradle 6 launch groups.

Do not manually add Gradle libraries to the Eclipse build path; Buildship owns
the dependency container. Adding a second dependency path duplicates named
Forge modules at startup. After regenerating files outside Eclipse, use
**Gradle > Refresh Gradle Project** and **Project > Clean** to replace stale
compiler markers and launch data.

## Development loop

For focused work, begin with red-to-green unit or contract tests, then run the
smallest proportional compile/resource gate. Before handing off a candidate:

```powershell
./gradlew.bat clean test processResources build --no-daemon
./gradlew.bat genEclipseRuns eclipse --no-daemon
```

Run GameTests in a fresh disposable directory so saved state, configs, and
world output cannot inherit an earlier result:

```powershell
./gradlew.bat test runGameTestServer `
  -PskysGrassSlabsGameTestRunDirectory=run-gametest-candidate --no-daemon
```

The beta suite has seven independently batched runtime tests. The controlled
world-generation test runs the whole owning chunk twice and requires the
second pass to make no change.

## Required automated coverage

### Blocks and recipes

- top, bottom, double, and waterlogged placement
- collision, occlusion, pathing, light, sound, tool and drop behaviour
- Silk Touch and ordinary grass-slab drops
- snow appearance and survival
- dirt/grass and dirt-slab/grass-slab seed recipes
- slab creation and combining behaviour
- server-only classloading and resource completeness

### Grass lifecycle

- vanilla grass to dirt slab
- grass slab to dirt slab
- grass slab decay when covered
- top/bottom orientation preservation
- waterlogged rejection
- double-slab normalization
- loaded-area guards that never force neighbour chunk loads

### World generation

- one-block transition places exactly one bottom slab
- flat terrain, two-block cliffs, unsupported edges, water and occupied targets
  remain unchanged
- structures and block entities remain unchanged
- chunk edges produce the same result regardless of generation order
- only the owning chunk is written
- only intended dimensions/biomes/surface blocks participate
- no second-pass duplicate output
- no writes to existing chunks by default

Use a fixed seed and compare at least several generation orders around the same
chunk boundary. Benchmark the 256-column pass separately and together with
Mineralogy/OreSpawn; cache predicates and avoid allocation in the hot loop.

### Compatibility

Run fresh and reload integration with:

1. Forge plus Sky's Grass Slabs only.
2. Sky's Grass Slabs plus Mineralogy.
3. Sky's Grass Slabs plus the exact local OreSpawn candidate and Mineralogy.
4. Starting with the 1.10.2 compatibility release, a disposable legacy
   Sylvester conversion as specified in `LEGACY-MIGRATION.md`.

The current reference OreSpawn checkout may move. Re-read its ignored handover,
verify its `git status`, branch, exact commit and jar hash, and never assume the
snapshot recorded in local agent notes is still current.

## Runtime and release evidence

A development launch does not prove the distributable jar. A final candidate
requires:

- deterministic unit/contract tests
- data/resource parsing and locale-key audit
- clean build, Javadocs, `genEclipseRuns`, and `eclipse`
- artifact contents audit with no test classes, fixtures, agent files, local
  paths, or unintended dependencies
- exact reobfuscated jar in a clean launcher-like Forge 40 client and server
- fresh world, clean save/stop, and reload
- complete log and crash-directory review
- manual visual acceptance of slab joins, tinting, snow, placement, breaking,
  walking, and generated slopes
- legacy migration evidence where applicable

Keep local pass, PR readiness, hosted CI, deployment/publication, packaged
runtime proof, manual acceptance, and release approval as distinct states.
