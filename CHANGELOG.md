# Changelog

## 0.2.0.118021 - Turf beta

- Fixed dark grass-slab sides by using vanilla grass's cutout-mipped render
  layer for its transparent biome-tinted overlay.
- Added biome-tinted, carpet-height turf with carpet collision, support,
  breaking, sound, and flammability behaviour but no wool-carpet integrations.
- Added grass spreading from dirt-supported turf without a dirt/decay stage;
  invalid substrates remove and drop the turf on its random tick.
- Added direct turf conversion for dry dirt slabs, preserving top/bottom
  orientation and normalizing double slabs to vanilla grass.
- Added the `turf_cutting` special recipe for grass plus any Forge-compatible
  shovel, returning matching dirt and the completely unchanged shovel.
- Shared the target-aware grass spreading implementation between grass slabs
  and turf, including loaded-area, light, water, orientation, and double-slab
  rules.
- Preserved every 0.1.0 registry ID, world-generation rule, configuration key,
  and world schema version.

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
