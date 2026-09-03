# Upgrading a Minecraft 1.10.2 world

Always make a complete copy of the world before changing its Minecraft or
Forge version. Test the copy first and keep the original until the upgraded
world has been checked in game.

## Sky's Grass Slabs content

Install `SkysGrassSlabs-1.0.0.111021.jar` before opening a world that used the
Minecraft 1.10.2 release. The permanent dirt slab, grass slab, path slab and
turf IDs are unchanged.

The upgrade has been tested with top and bottom slabs, turf, paths, container
and dropped item stacks, custom item data and the saved world migration state.
The same content remained intact after saving and loading the upgraded world a
second time.

World smoothing still affects only newly generated chunks. Opening an existing
chunk on 1.11.2 does not add smoothing slabs to it.

## BuildingBricks content from 1.10.2

There is no supported BuildingBricks release for Minecraft 1.11.2. Sky's Grass
Slabs can automatically recover these supported IDs when the old mod is not
present:

- `buildingbricks:grass_slab`;
- `buildingbricks:dirt_slab`;
- `buildingbrickscompatvanilla:grass_slab`.

They are replaced with the matching Sky slabs as the upgraded chunks and item
stacks load. Slab orientation, stack counts and custom item data are retained.

Other BuildingBricks shapes are not part of this migration. Forge may report
stairs, vertical slabs, corners, steps, walls or other content as missing and
may remove it after offering its normal backup and confirmation screen. Do not
continue unless losing those unsupported blocks is acceptable for that copy of
the world.

If an important world uses other blocks from a mod without a 1.11.2 version,
keep playing it on 1.10.2 until a suitable migration is available.

## Modpack checklist

1. Back up the complete 1.10.2 instance and world.
2. Update Forge and every required mod to a matching 1.11.2 build.
3. Install the 1.11.2 Sky's Grass Slabs jar before the first upgraded start.
4. Read the complete Forge missing content list before confirming it.
5. Check representative slabs, turf, paths, inventories and previously
   generated terrain.
6. Stop and start the upgraded copy once more, then repeat the checks.

Never open the upgraded save again with Minecraft 1.10.2.
