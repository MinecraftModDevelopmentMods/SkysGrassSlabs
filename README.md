# Sky's Grass Slabs

Sky's Grass Slabs adds dirt, grass and lowered path slabs, along with a thin
layer of cuttable turf. Grass spreads naturally between full blocks and slabs,
and newly generated Overworld terrain uses grass slabs to soften small steps.

This branch targets Minecraft 1.10.2 with Forge 12.18.3.2511. The current beta
version is `0.3.0.110021`.

## Features

- Dirt, grass and path slabs that can be placed in the top or bottom half of a
  block. Combining two matching slabs produces the vanilla full block.
- Grass that matches the biome, spreads naturally and turns back to dirt when
  covered. Snowy areas give grass and dirt slabs a complete snow cap.
- Plant and bonemeal support on top grass slabs.
- Use a compatible shovel on a dirt or grass slab to make a lowered path slab.
- Carpet height turf that matches the biome, spreads grass when placed on dirt
  and can be eaten by sheep.
- Craft turf from grass with any compatible shovel. The shovel is returned
  unchanged and the matching dirt block or slab remains in the crafting grid.
- Place turf on a dirt slab to turn it into a grass slab.
- Grass slabs in new terrain smooth suitable one block height changes.
- Migration support for compatible grass and dirt slabs from older worlds.

World smoothing is enabled by default. It can be disabled in the common Forge
configuration with `worldgen.generateGrassSlabs=false`; the setting affects
only chunks generated afterwards.

## Development

Run Gradle with Java 17. Production code, tests, and Minecraft launches use the
Java 8 toolchain pinned in `gradle.properties`.

```powershell
./gradlew.bat clean check build javadoc verifyReleaseArtifacts `
  writeReleaseChecksums verifyEclipseProductionClasspath --no-daemon
```

For Eclipse, use the checkout's parent directory as the workspace, import this
directory as an existing Gradle project, then run `genEclipseRuns` and refresh
the Gradle project.

## License

Sky's Grass Slabs is licensed under `LGPL-2.1-only`; see `LICENSE`,
`LICENSE.spdx`, and `NOTICE`. Copyright is held by SkyBlade1978.
