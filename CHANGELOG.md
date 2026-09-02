# Changelog

## 1.0.0.110021 - Minecraft 1.10.2

- First stable release for Minecraft 1.10.2.
- Added dirt, grass and lowered path slabs that can be placed in either half of
  a block and combine into their matching vanilla blocks.
- Added natural grass spreading, covered grass decay, plant support and
  bonemeal support.
- Added one pixel high turf that spreads grass, turns dirt slabs into grass
  slabs and can be eaten by sheep.
- Added shovel flattening for dirt and grass slabs.
- Added grass slabs to suitable height changes in newly generated terrain.
- Added optional migration for compatible grass and dirt slabs from older
  worlds. Replacement is disabled by default and other shapes are left alone.
- Kept completed legacy conversions marked across repeated world reloads.
- Added complete visual snow caps for grass and dirt slabs.
- Added translations for the supported block names.

Snow on slabs is visual only in Minecraft 1.10.2. It adds no height, does not
melt by itself and drops no snowballs.

## 0.3.0.110021 - Minecraft 1.10.2 beta

- Brought dirt, grass and lowered path slabs, along with turf, to Minecraft
  1.10.2.
- Dirt, grass and path slabs can be placed in either half of a block. Two
  matching slabs combine into the corresponding vanilla block.
- Grass spreads naturally between full blocks, slabs and turf. Covered grass
  slabs turn back to dirt, while top grass slabs support plants and bonemeal.
- Using a compatible shovel on dirt or grass slabs creates lowered path slabs.
- Turf is one pixel high, can turn dirt slabs into grass slabs and can be cut
  from grass without using up the shovel.
- Snowy areas give grass and dirt slabs a complete snow cap, including the
  snowy edge down each side.
- Grass slabs can appear in newly generated terrain to smooth suitable one
  block height changes.
- Added migration support for compatible grass and dirt slabs and items from
  older worlds. Automatic replacement while the older content remains
  installed is optional and disabled by default. Other block shapes are left
  alone.
- Grass no longer remains beneath grass slabs or turf.
- Sheep can eat turf and regrow their wool. The normal `mobGriefing` game rule
  is respected.

## 0.2.0.118021 - Turf beta

- Fixed the dark texture on the sides of grass slabs.
- Added thin, biome tinted turf with the shape and movement of carpet.
- Turf spreads grass while it is supported by dirt. If it is placed on another
  full block, it eventually breaks and drops itself.
- Placing turf on a dry dirt slab turns it into a grass slab and keeps the slab
  in the same half of the block.
- Crafting grass with a compatible shovel produces turf. The matching dirt and
  the unchanged shovel remain in the crafting grid.

## 0.1.0.118021 - Minecraft 1.18.2 beta

- Added dirt, biome tinted grass and lowered path slabs.
- Added natural grass spreading and decay, snow support, plants and bonemeal.
- Added shovel flattening while preserving slab orientation and tool behaviour.
- Added slab recipes and `forge:seeds` grass helper recipes.
- Added grass slabs to suitable one block height changes in newly generated
  Overworld terrain.
- Added world state `skysgrassslabs_world_state` with schema version 1.
