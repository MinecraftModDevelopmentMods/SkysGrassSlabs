# Legacy BuildingBricks migration

## Supported source

The 1.10.2 compatibility target is BuildingBricks `1.10.2-2.0.13`. Migration
covers only:

- `buildingbricks:grass_slab` to `skysgrassslabs:grass_slab`
- `buildingbricks:dirt_slab` to `skysgrassslabs:dirt_slab`
- historical alias `buildingbrickscompatvanilla:grass_slab`

Block metadata is preserved as `0 = top`, `1 = bottom`. Supported item stacks
are replaced with the matching Sky item while their count and NBT are retained.
The old item damage identifies BuildingBricks material rather than slab half,
so the replacement item uses Sky's canonical item metadata; placement still
selects the half from the click exactly like an ordinary slab.

Stairs, vertical slabs, corners, steps, walls, glass shapes, rock shapes, wood
shapes, and every other BuildingBricks block remain untouched.

## Replacement setting

`compat.forceReplaceBuildingBricksSlabs` controls migration while
BuildingBricks remains installed. It defaults to false and is read during
startup, so changing it requires a restart.

When false, existing blocks and items keep their BuildingBricks identity.
Chunks are not marked, counters and reports are not updated, and newly placed
BuildingBricks slabs are not replaced. Bridge recipes remain available. This
allows both mods to run together while Sky handles only its own blocks and any
enabled new terrain smoothing.

When true, all supported block and item conversions described below run. The
setting is independent of world generation: replacement can run with smoothing
enabled or disabled. Turning it off later stops future replacement but cannot
reconstruct slabs that were already converted. Historical counters, chunk
markers and reports are retained.

## Generator ownership

Sky declares optional load ordering before BuildingBricks without requiring it.
If Sky smoothing is disabled, no BuildingBricks configuration is changed.

If both are installed and Sky smoothing is enabled:

1. Load `config/BuildingBricks/general.cfg` through Forge Configuration.
2. If `compat.vanilla.generateGrassSlabs` is already false, leave it untouched.
3. Otherwise create `general.cfg.skysgrassslabs-backup` once and set the
   property false before BuildingBricks begins its initialization.
4. Reload the configuration to verify the write.
5. Suppress Sky smoothing for the current process if any step fails.

## Chunk and inventory migration

When forced replacement is enabled, `ChunkDataEvent.Load` checks a compound
named `skysgrassslabs` in each chunk. A chunk already marked with migration
version 1 is skipped. With replacement disabled, the handler returns before
scanning or marking the chunk.

Unmarked chunks are scanned from their serialized `Blocks`, `Data`, and
optional `Add` arrays. This avoids a full registry/state lookup for every block
in the world. Supported positions are updated directly in chunk storage without
neighbour notifications. Unsupported registered shapes are counted but not
changed. A stable dimension/x/z key makes every save event carry the version
marker, including the second save emitted by 1.10 during chunk unload.

Tile entities and entities are serialized and traversed recursively for item
stacks. This covers inventories, item handlers, dropped items, item frames, and
nested capability NBT without opening legacy loot containers during the load
event. Player inventory and ender chest stacks are migrated at login. Newly
placed supported legacy slabs are replaced immediately.

Missing mappings remap supported block and item IDs when BuildingBricks is no
longer installed. This recovery is always active and does not use the force
replacement setting. Unsupported shapes follow Forge's normal missing content
warning and backup process and may then be removed. Seed and turf recipes
accept supported slab items while BuildingBricks is installed.

Forge 1.10.2 reports every remaining BuildingBricks registry entry when the
mod is removed, including entries that are not placed in the world. In the
qualified Sylvester copy the confirmation listed 110 unsupported block and
item mappings. Neither supported slab ID appeared because both were remapped.
Forge created its normal world backup before continuing. The saved world
contained 6,663 unsupported blocks across the 13 IDs listed in the report
below; those are deliberately left for Forge to report and remove.

## Sylvester safety boundary

The authoritative Sylvester fixture is held outside this repository and is
read only. Tests must copy the complete server to disposable storage and must
never launch or repair the source directory.

The source fingerprint before and after qualification was identical:

- files: `1,080`
- bytes: `1,380,450,555`
- aggregate SHA-256:
  `45C0A84913A71D0F7832F76719A4C3B745DAEA9BE5C8EFD9FEF6B97077EFBF44`

The aggregate hashes each sorted relative path followed by LF and then the
file's bytes, so path changes and content changes are both detected.

## Qualified forced migration result

The disposable complete fixture contained 87,759 existing chunk headers. Its
first pass converted:

- grass slabs: `1,656,276` (`0` top, `1,656,276` bottom)
- dirt slabs: `2,968` (`12` top, `2,956` bottom)
- grass slab items encountered: `0`
- dirt slab items encountered: `7,186`

It reported and left unchanged 6,663 unsupported block shapes across 13 IDs:

- dirt stairs `10`, dirt vertical slabs `10`
- glass slabs `336`, stairs `73`, steps `4`, vertical slabs `916`
- rock stairs `18`, steps `77`, vertical slabs `404`, walls `200`
- wood corners `95`, steps `2,244`, vertical slabs `2,276`

Minecraft completed some old boundary terrain while the fixture was loaded,
increasing the release candidate's reload traversal to 87,849 chunk headers
and its durable processed marker total to 87,867. Those newly completed chunks
were marked once but produced no supported conversions and no unsupported
recount. The second complete load retained every block, orientation, item, and
unsupported total unchanged and wrote `migration_reload_complete=true`.

The old fixture also logs its existing malformed Mineralogy metadata and a rock
furnace tile class without a public constructor that takes no arguments. Forge skips
those old tile entities. These warnings predate this mod and did not prevent the
complete server from reaching the started state or the migration audit from
passing.

The figures above were recorded with forced replacement enabled. Default side
by side retention and missing mapping recovery without BuildingBricks are
separate acceptance scenarios and used fresh disposable copies. With
replacement disabled, the copy retained exactly 1,656,276 grass slabs, 2,968
dirt slabs, and 7,186 dirt slab items without migration state or a report.
Normal player chunk tracking in virgin terrain generated 1,985 Sky grass slabs
and no BuildingBricks grass slabs. With BuildingBricks removed, the supported
blocks and items remapped with the same orientation split, and Forge created a
481,716,701 byte backup before removing unsupported content.

The later manual acceptance journey generated 1,694 new chunks in a disposable
copy. Those chunks contained 46,692 Sky grass slabs, all in the expected bottom
orientation, and no newly generated BuildingBricks grass or dirt slabs.
OreSpawn and Mineralogy generation remained active throughout the journey.
