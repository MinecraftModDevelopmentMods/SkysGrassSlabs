# Changelog

## 1.0.1.114041 - Minecraft 1.14.4

- Ported dirt, grass and path slabs, turf and terrain smoothing to Minecraft
  1.14.4.
- Kept both slab orientations, waterlogging, snowy caps, path alignment,
  grass spreading, turf recipes and sheep interaction.
- Added Minecraft 1.14 loot tables for the four blocks, including Silk Touch
  behaviour for grass slabs.
- Registered terrain smoothing as a Forge feature and retained its existing
  new chunk only behaviour.
- Preserved Sky blocks, items, block states, custom item data and saved world
  state when upgrading worlds from Minecraft 1.10.2 through 1.13.2.
- Retained all 18 translations and the existing configuration keys.
- Fixed the turf recipe so it works safely in both the player crafting grid
  and crafting tables.

## 1.0.1.113021 - Minecraft 1.13.2

- Ported dirt, grass and path slabs, turf and terrain smoothing to Minecraft
  1.13.2.
- Updated blocks, recipes, translations and configuration for Minecraft
  1.13's flattened data format.
- Added native waterlogging while preserving top and bottom slab placement.
- Preserved saved Sky blocks, items and world state when upgrading worlds
  from Minecraft 1.10.2, 1.11.2 and 1.12.2.
- Kept recovery for the supported historical grass and dirt slabs while
  leaving unrelated old shapes outside the migration.
- Retained snowy slab caps, path alignment, turf recipes, sheep interaction
  and the established 18 translations.

## 1.0.1.112021 - Minecraft 1.12.2

- Ported the complete dirt, grass, path slab and turf feature set to Minecraft
  1.12.2.
- Preserved the permanent block and item names, slab orientation, recipes and
  saved world state used by the earlier releases.
- Kept the corrected path slab side texture alignment.
- Added direct upgrade and reload checks for Minecraft 1.10.2 and 1.11.2
  worlds.
- Retained all 18 translations, natural grass behaviour, snowy slab edges,
  sheep interaction and terrain smoothing.
- Made turf expose the same connection faces as vanilla carpet so fences and
  similar blocks do not join to it.
- Made the turf cutting recipe discoverable and usable through the recipe
  book.

## 1.0.1.111021 - Minecraft 1.11.2

- Corrected the side texture alignment on top and bottom path slabs so it
  follows the lowered surface in the same way as a vanilla path block.

## 1.0.0.111021 - Minecraft 1.11.2

- Ported the complete dirt, grass, path slab and turf feature set to Minecraft
  1.11.2.
- Preserved the permanent block and item names, slab orientation, recipes and world
  state used by the 1.10.2 release.
- Added a compact 1.10.2 world fixture and packaged upgrade and reload checks.
- Kept recovery for supported older grass and dirt slab identities when their
  original mod is absent.
- Updated all 18 language resources for Minecraft 1.11's lowercase locale
  paths.
- Kept the same grass lifecycle, snow appearance, sheep interaction, recipes
  and deterministic terrain smoothing.

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
