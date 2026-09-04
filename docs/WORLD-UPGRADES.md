# Upgrading an older world

Always make a complete copy of the world before changing its Minecraft or
Forge version. Test the copy first and keep the original until the upgraded
world has been checked in game.

## Sky's Grass Slabs content

Install `SkysGrassSlabs-1.0.1.114041.jar` before opening a world that used the
Minecraft 1.10.2, 1.11.2, 1.12.2 or 1.13.2 release. The permanent dirt slab,
grass slab, path slab and turf IDs are unchanged.

Upgrade checks cover top and bottom slabs, turf, paths, container and dropped
item stacks, custom item data and the saved world migration state. Saving and
loading the upgraded world a second time must not change that content again.

Worlds from Minecraft 1.10.2 through 1.12.2 still need the numeric block
conversion introduced for Minecraft 1.13. Sky's Grass Slabs prepares that
conversion before Minecraft updates each chunk, including block IDs above the
vanilla numeric range. Old top and bottom orientations are retained and begin
dry. Keep the mod installed during the first upgraded start so unopened chunks
can be converted when they are later visited.

Minecraft 1.13.2 worlds already use named block states. Their orientation,
snowy and waterlogged states load directly in Minecraft 1.14.4 without the
numeric conversion step.

World smoothing still affects only newly generated chunks. Indexed chunks from
the older world are never treated as new terrain by the smoothing feature.

## Older mod content

Sky's Grass Slabs can automatically recover these supported historical IDs
when their original mod is not present:

- `buildingbricks:grass_slab`;
- `buildingbricks:dirt_slab`;
- `buildingbrickscompatvanilla:grass_slab`.

They are replaced with the matching Sky slabs as upgraded chunks and item
stacks load. Slab orientation, stack counts and custom item data are retained.

Other shapes from the older mod are not part of this migration. Forge may
report stairs, vertical slabs, corners, steps, walls or other content as
missing and may remove it after offering its normal backup and confirmation
screen. Do not continue unless losing those unsupported blocks is acceptable
for that copy of the world.

If an important world uses other content without a Minecraft 1.14.4 version,
keep playing it on its existing Minecraft version until a suitable migration
is available.

## Modpack checklist

1. Back up the complete instance and world.
2. Update Forge and every required mod to a matching Minecraft 1.14.4 build.
3. Install the 1.14.4 Sky's Grass Slabs jar before the first upgraded start.
4. Read the complete Forge missing content list before confirming it.
5. Check representative slabs, turf, paths, inventories and previously
   generated terrain.
6. Stop and start the upgraded copy once more, then repeat the checks.

Never open the upgraded save again with an older Minecraft version.
