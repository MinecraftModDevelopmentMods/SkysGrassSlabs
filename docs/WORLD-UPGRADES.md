# Existing worlds and legacy slabs

Always make a complete world backup before changing its Minecraft version or
removing a mod that added blocks to the world.

## Adding Sky's Grass Slabs to a 1.10.2 world

Existing Sky block IDs are stable, and world smoothing affects only chunks
generated after the mod is installed. Ordinary existing terrain is not
retrofitted.

Sky's Grass Slabs includes targeted support for grass and dirt slabs from
BuildingBricks 1.10.2-2.0.13. Other shapes from that mod, including stairs,
vertical slabs, corners, steps and walls, are outside the migration.

### Keeping both mods installed

This is the default and least disruptive arrangement. With
`compat.forceReplaceBuildingBricksSlabs=false`, existing BuildingBricks blocks
and items retain their original identity. Sky's recipes can still accept the
supported slabs.

World generation is controlled separately. If
`worldgen.generateGrassSlabs=true`, Sky takes ownership of grass slab
generation in new chunks and disables the overlapping BuildingBricks setting
after making a one time backup of its configuration.

### Replacing supported slabs

Set `compat.forceReplaceBuildingBricksSlabs=true` and restart to replace the
supported grass and dirt slabs while BuildingBricks remains installed.
Orientation, stack counts and item data are retained where applicable.
Supported blocks in chunks and supported items in loaded inventories,
containers and entities are handled as they are encountered.

The conversion is not automatically reversible. Unsupported shapes are left
unchanged. Progress is recorded in the world so completed chunks are not
converted again, and a readable report is written to:

`<world>/serverconfig/skysgrassslabs-migration-report.txt`

Turning the setting off later stops future replacement but does not restore
blocks that were already converted.

### Removing BuildingBricks

When BuildingBricks is absent, its supported grass slab, dirt slab and
historical grass slab IDs are recovered automatically as Sky slabs. Forge may
still warn about unsupported BuildingBricks content and may remove that
content after offering its normal backup and missing block confirmation.

Do not continue past that warning unless the listed unsupported blocks are no
longer needed. Keeping BuildingBricks installed is the safest choice for a
world that still uses its other shapes.

## Moving the world to Minecraft 1.11.2

Install the matching Minecraft 1.11.2 release of Sky's Grass Slabs before
opening the upgraded copy. The permanent Sky block IDs, slab orientation,
turf, paths, item stacks and saved migration state have been tested through a
1.10.2 to 1.11.2 server upgrade and reload.

Review Forge's missing content screen for every other mod in the pack. Sky's
upgrade support cannot preserve unrelated blocks from mods that are not
available on Minecraft 1.11.2. Never use the upgraded save again with an older
Minecraft version.
