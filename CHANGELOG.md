# Changelog

## 0.1.0.118021 - 1.18.2 beta

- Added dirt, biome-tinted grass, and lowered path slabs.
- Added vanilla-style grass spreading, decay, snow, plants, and bonemeal rules.
- Added shovel flattening while preserving slab orientation and tool behaviour.
- Added slab recipes and `forge:seeds` grass helper recipes.
- Added deterministic smoothing of one-block grass transitions in newly
  generated Overworld chunks, controlled by `worldgen.generateGrassSlabs`.
- Added world schema marker `skysgrassslabs_world_state` version 1.
- Added unit/resource tests and Forge GameTests for gameplay and smoothing.

BuildingBricks detection and migration are planned for the Minecraft 1.10.2
compatibility release and are not included in this beta.
