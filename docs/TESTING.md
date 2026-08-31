# Testing and evidence

## Environment

- Minecraft: 1.18.2
- Forge: 40.3.0
- Mappings: official 1.18.2
- Gradle wrapper: 8.8
- Java runtime and bytecode: 17
- Verified local JDK: `<Java 17 JDK>`
- Gradle user home: `<repository>/.gradle-verify-cache`

Do not run Gradle concurrently against this checkout/cache. Review complete
logs and crash-report directories after every client, server, or GameTest run.

## Development loop

For focused work, begin with red-to-green unit or contract tests, then run the
smallest proportional compile/resource gate. Before handing off a candidate:

```powershell
./gradlew.bat clean test processResources build --no-daemon
./gradlew.bat genEclipseRuns eclipse --no-daemon
```

Once GameTests exist, run `runGameTestServer` in a fresh disposable run
directory. Do not enable that gate while the project has no GameTests because
Forge's empty GameTest server is not a useful product result.

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
4. A disposable legacy Sylvester conversion as specified in
   `LEGACY-MIGRATION.md`.

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
