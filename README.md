# Sky's Grass Slabs

Sky's Grass Slabs adds dirt, grass and path slabs, plus a thin layer of turf.
Use them for gentler slopes, paths around your house and grassy details where
a full block would be too much.

## Slabs, paths and turf

Slabs fit in either half of a block. Matching halves combine into a full block.
Use a shovel on a dirt or grass slab to make a lowered path.

Grass follows the surrounding biome colours and spreads between blocks, slabs
and turf. Covered grass, or grass left in the dark, turns back into dirt.
Nearby snow gives dirt and grass slabs snowy edges.

Cut turf by crafting a grass block or grass slab with a shovel. You get the
turf, keep the shovel unchanged, and leave a dirt block or slab in the grid.
Lay turf on dirt, or use it on a dirt slab to make a grass slab. Sheep can eat
turf and regrow their wool.

Three dirt or grass blocks in a row make six matching slabs. Two matching slabs
can be crafted back into a full block. The [gameplay guide](docs/GAMEPLAY.md)
covers the other recipes, plant support and drops.

## Smoother terrain

By default, grass slabs soften suitable one-block slopes in new Overworld
chunks. Existing terrain stays as it is. To turn generation off, select the mod
in **Mods → Config**, save your choice and restart Minecraft. You can also edit
`config/skysgrassslabs.cfg`; see [configuration](docs/CONFIGURATION.md).

The mod works on its own. If you install
[Sky's Terrain Smoother](https://github.com/MinecraftModDevelopmentMods/SkysTerrainSmoother),
it handles smoothing instead and can include stone, sand and finer pieces.
[Sky's Building Pieces](https://github.com/MinecraftModDevelopmentMods/SkysBuildingPieces)
also adds dirt and grass steps and corners. Neither mod is required to use
Grass Slabs or update an existing Grass Slabs world.

## Installation

This branch is for **Minecraft 1.10.2**, **Forge 12.18.3.2511** and **Java 8**.
Put the main mod jar in your instance's `mods` folder. For multiplayer, install
it on both the client and server.

Back up your world before updating mods or changing Minecraft versions.
Read [the world-upgrade guide](docs/WORLD-UPGRADES.md) before removing a mod
whose slabs are still in the world.

See the [changelog](CHANGELOG.md) for release history.

## Licence

[LGPL-2.1-only](LICENSE). Copyright SkyBlade1978.
See [NOTICE](NOTICE) for credits.
