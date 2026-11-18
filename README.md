# Sky's Grass Slabs

Sky's Grass Slabs adds dirt, grass and lowered dirt path slabs, plus a thin
layer of turf. Grass spreads between blocks, slabs and turf, and grass slabs
soften one block steps in newly generated Overworld terrain.

This branch is for Minecraft 26.2. Version `1.2.1.2602002` requires NeoForge
26.2.0.45-beta or a compatible newer 26.2 build, and Java 25.

## Installation

Place the mod jar in the `mods` folder. It must be installed on both the client
and server for multiplayer. Back up an existing world before adding, removing
or updating world changing mods.

When Sky’s Terrain Smoother is installed, it handles new terrain for both mods.
Grass Slabs still controls whether it places grass pieces.

## Using the mod

- Place slabs in either half of a block. Two matching slabs combine into the
  corresponding vanilla block.
- Use a shovel on dirt or grass slabs to make a dirt path slab.
- Craft grass and a shovel into turf. The shovel is returned unchanged.
- Let sheep eat turf to regrow their wool.

Grass follows the surrounding biome colour, and slabs show snow when it is
nearby. Slabs can also be waterlogged. Block names are translated into 18
language variants.

World smoothing affects new chunks only and can be turned off in the config.
The mod can also recover supported slabs and grass carpet from older mods;
read the upgrade guide before removing one from an existing world.

## This update

Grass slabs now pick up snow on higher terraces and exposed cold ground.
Combining slabs respects entity collisions, and laying turf on a dirt slab
respects adventure-mode placement permissions. Existing worlds, orientations
and configuration files stay compatible.

## Guides

- [Gameplay and recipes](docs/GAMEPLAY.md)
- [Configuration](docs/CONFIGURATION.md)
- [Upgrading an older world](docs/WORLD-UPGRADES.md)
- [Release notes](docs/RELEASE-1.2.1.2602002.md)

## License

Sky's Grass Slabs is licensed under `LGPL-2.1-only`; see `LICENSE`,
`LICENSE.spdx`, and `NOTICE`. Copyright is held by SkyBlade1978.
