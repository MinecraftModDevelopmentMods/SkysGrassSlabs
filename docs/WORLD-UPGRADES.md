# Upgrading an older world

Always make a complete copy of the world before changing its Minecraft or
Forge version. Test the copy first and keep the original until the upgraded
world has been checked in game.

## Sky's Grass Slabs content

Install `SkysGrassSlabs-1.0.1.118021.jar` before opening a world that used an
earlier release. The permanent dirt slab, grass slab, path slab and turf IDs
are unchanged.

Worlds from Minecraft 1.10.2 through 1.12.2 require numeric block conversion
when their chunks are first opened. Old top and bottom slab orientations are
retained and begin dry. Keep the mod installed during the first upgraded start
so unopened chunks can be converted when they are later visited.

Minecraft 1.13.2 through 1.17.1 worlds already use named block states. Their
orientation, snowy and waterlogged states load through Minecraft's normal
upgrade process.

World smoothing still affects only newly generated chunks. Chunks indexed as
part of an older world are not treated as new terrain by the smoothing
feature.

## Supported older slab IDs

The mod can recover supported historical grass and dirt slab IDs when their
original mod is absent. Slab orientation, stack counts and custom item data are
retained.

Other older shapes such as stairs, vertical slabs, corners and steps are not
part of this migration. Forge may report them as missing and may remove them
after offering its normal backup and confirmation screen. Do not continue
unless losing those unsupported blocks is acceptable for that copy of the
world.

## Modpack checklist

1. Back up the complete instance and world.
2. Update Forge and every required mod to a matching Minecraft 1.18.2 build.
3. Install the 1.18.2 Sky's Grass Slabs jar before the first upgraded start.
4. Read the complete Forge missing content list before confirming it.
5. Check representative slabs, turf, paths, inventories and old terrain.
6. Stop and start the upgraded copy once more, then repeat the checks.

Never open the upgraded save again with an older Minecraft version.
