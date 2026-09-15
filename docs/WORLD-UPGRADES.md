# Upgrading an older world

Always make a complete copy of the world before changing its Minecraft or
NeoForge version. Test the copy first and keep the original until the upgraded
world has been checked in game.

## Sky's Grass Slabs content

Install `SkysGrassSlabs-1.1.2.2601022.jar` before opening a world that used an
earlier release. The permanent dirt slab, grass slab, path slab and turf IDs
are unchanged.

Worlds from Minecraft 1.10.2 through 1.12.2 require numeric block conversion
when their chunks are first opened. Old top and bottom slab orientations are
retained and begin dry. Keep the mod installed during the first upgraded start
so unopened chunks can be converted when they are later visited.

Minecraft 1.13.2 through 1.21.11 worlds already use named block states. Their
orientation, snowy and waterlogged states load through Minecraft's normal
upgrade process.

Minecraft 26.1 stores dimension data in namespaced folders. The mod copies its
older Overworld state into the new location before loading it and leaves the
original file untouched as an additional recovery point.

World smoothing still affects only newly generated chunks. Chunks indexed as
part of an older world are not treated as new terrain by the smoothing
feature.

## Supported older slab IDs

The mod can recover supported historical grass and dirt slab IDs when their
original mod is absent. Slab orientation, stack counts and custom item data are
retained.

Other older shapes such as stairs, vertical slabs, corners and steps are not
part of this migration. NeoForge may report them as missing and may remove them
after offering its normal backup and confirmation screen. Do not continue
unless losing those unsupported blocks is acceptable for that copy of the
world.

## Grass Slabs, Carpets & Stairs

Minecraft 1.18 worlds that used Grass Slabs, Carpets & Stairs can recover these
four IDs after that mod is removed:

- `grassslabs:grass_slab` becomes `skysgrassslabs:grass_slab`;
- `grassslabs:dirt_slab` becomes `skysgrassslabs:dirt_slab`;
- `grassslabs:dirt_path_slab` becomes `skysgrassslabs:path_slab`;
- `grassslabs:grass_carpet` becomes `skysgrassslabs:turf` when it is on vanilla
  dirt.

Top and bottom slabs retain their orientation and waterlogging. Double slabs
become the related vanilla full block. Stack counts and custom item data are
retained.

Grass carpet placed on another block is kept under its original ID so the
decoration does not break under turf's stricter support rules. Breaking that
carpet produces an item which is converted to turf.

Stairs, dirt carpet, dirt path carpet, mycelium variants and podzol variants do
not have matching blocks in Sky's Grass Slabs. They are not converted. Read
NeoForge's complete missing content warning before continuing without the older
mod.

When both mods remain installed, their content is left alone by default. See
[Configuration](CONFIGURATION.md) for the optional replacement setting.

## Modpack checklist

1. Back up the complete instance and world.
2. Update NeoForge and every required mod to a matching Minecraft 1.21.1 build.
3. Install the 1.21.1 Sky's Grass Slabs jar before the first upgraded start.
4. Read the complete NeoForge missing content list before confirming it.
5. Check representative slabs, turf, paths, inventories and old terrain.
6. Stop and start the upgraded copy once more, then repeat the checks.

Never open the upgraded save again with an older Minecraft version.
