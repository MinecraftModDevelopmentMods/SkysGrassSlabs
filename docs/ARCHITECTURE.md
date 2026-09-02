# Architecture and scope

## Product contract

Version `1.0.0.110021` is the Minecraft 1.10.2 release of the complete grass
slab and turf product. It retains mod ID `skysgrassslabs`, package root
`zone.moddev.mc.skysgrassslabs`, all established block/item IDs, common config
keys `worldgen.generateGrassSlabs` and
`compat.forceReplaceBuildingBricksSlabs`, and schema-1 world state
`skysgrassslabs_world_state`.

The four permanent blocks are `dirt_slab`, `grass_slab`, `path_slab`, and
`turf`. Slabs store only their half in metadata: `0 = top`, `1 = bottom`.
There are no double slab registry entries. Combining matching items replaces
the slab with vanilla dirt, grass, or grass path.

Minecraft 1.10 has no waterlogged block state. Slabs reject fluid placement and
ordinary fluid behaviour is left to Minecraft.

## Block lifecycle

`LegacySlabBlock` owns the slab half, shape, placement, metadata, drops and
combination contract. Dirt and grass use eight pixel half shapes. Path slabs
use seven pixel shapes at `0-7/16` and `8-15/16`. Turf uses a one pixel carpet
shape.

`GrassSpread` contains the shared loaded area spread rules used by grass slabs
and turf. It targets vanilla dirt and dry dirt slabs; a converted slab keeps
its half. Grass slabs decay to matching dirt slabs when covered or too dark.
Turf has no dirt stage: low light merely pauses it, while support other than
dirt makes it break and drop on a random tick.

Grass coverings never sustain a vanilla grass support. Placing a grass slab,
along with later neighbour changes and random ticks, converts a directly
supporting grass block to dirt. Spread targets with turf or a grass slab above
them are
rejected, and each covering excludes its own support from its spread attempts.
If vanilla spreading temporarily changes covered dirt to grass, the covering's
neighbour update changes it back to dirt. A turf block already loaded above
grass remains deliberately invalid and removes itself on its random tick unless
a later neighbour update repairs the support first.

Grass and dirt slabs expose a calculated `snowy` property that is not saved.
Snow directly above or in any of the four horizontal neighbours selects a model
with an untinted vanilla snow top and vanilla dirt sides with snowy edges. Snowy
dirt and grass slabs deliberately share that complete cap treatment so the dirt
version does not look like a white top pasted onto an ordinary dirt side. The
property is never serialized and cannot affect the `0 = top`, `1 = bottom`
metadata contract. This is visual only because Minecraft 1.10.2 cannot support
an ordinary snow layer when the half slab is not an opaque full block without
invasive engine changes.

Only top grass slabs provide the plant and bonemeal behaviour unique to grass.
Dirt, path and turf do not impersonate a full grass block for plant support.

`TurfEatingAI` is added once to each server side sheep at the vanilla grass
eating priority and mutex setting. It mirrors vanilla attempt rates,
animation timing, navigation pause, wool regrowth, and child growth. Eating
destroys turf without a drop when `mobGriefing=true`; with mob griefing disabled
the turf remains while the ordinary vanilla eating bonus still applies.

Shovel flattening is implemented through `RightClickBlock` and Forge tool
classes. It requires any face except the underside, an editable position, a
clear block above and a compatible shovel. Successful conversion plays the
vanilla sound and uses one durability unless the player is in creative mode.

## Recipes

Vanilla seeds are registered under the legacy OreDictionary key `listAllseed`.
Dirt plus a seed makes grass, and a dirt slab plus a seed makes a grass slab.

`TurfCuttingRecipe` is registered with RecipeSorter key
`skysgrassslabs:turf_cutting`. Exactly one grass block or grass slab and one
compatible shovel produce one turf. The matching dirt input and an unchanged
copy of the exact shovel remain in the crafting grid.

## World smoothing

`GrassSlabSmoothingHandler` runs at `DecorateBiomeEvent.Pre` with lowest event
priority. It applies only in the Overworld when smoothing is active.

Each pass:

1. Resolves the owning loaded chunk without requesting neighbours.
2. Scans its 256 columns into a reusable boolean decision buffer.
3. Accepts only natural grass surfaces with a clear, dry, supported target and
   no block entity.
4. Requires an adjacent natural grass surface exactly one block higher.
5. Writes bottom grass slabs only inside the owning chunk in a separate pass,
   then converts each slab's supporting grass surface to dirt.

Interior neighbours are always considered. East and south border comparisons
are considered only when those chunks are already loaded; comparisons across
the west and north borders are deliberately omitted. The handler never loads
or writes a neighbouring chunk and performs no retrogen.

## Legacy compatibility

Compatibility is internal and driven by registries and configuration. No class
from another mod is linked and no public compatibility API is exposed.

When older grass slab smoothing is present and Sky smoothing is enabled, the
mod runs first, makes a one time backup of the old configuration, and disables
the overlapping generator. If configuration arbitration fails, Sky smoothing
is disabled for that process so duplicate generation cannot occur.

Replacement of supported installed legacy slabs is a separate decision.
`compat.forceReplaceBuildingBricksSlabs` defaults to false. When false, chunk,
placement, entity and inventory migration handlers make no changes and do not
mark chunks. Recipes still accept the supported legacy slabs. When true, the
existing migration operates independently of the world generation setting.
Removing the legacy mod always leaves missing mapping recovery active for the
supported IDs so those blocks and items do not disappear.

Chunk migration reads the original serialized 1.10 section arrays to locate
numeric legacy IDs efficiently, writes only supported grass and dirt slab
states into `ExtendedBlockStorage`, and marks each processed chunk with
`buildingbricks_migration_version=1`. Tile/entity stacks are migrated through
serialized NBT so unopened loot containers are not forced open. Player and
ender chest inventories are checked on login. Current and future migration
markers are copied back into the chunk data on every later save so a completed
chunk remains complete across repeated reloads. See `LEGACY-MIGRATION.md` for
the exact supported IDs and qualification evidence.

## Save state

`ModWorldState` is stored in the Overworld map storage so all dimensions share
one aggregate. It records schema version, migration version, processed chunks,
top and bottom block conversions, item conversions, and unsupported shape
totals. While forced replacement is active, the readable migration report is
written atomically under the world's `serverconfig` directory and records the
active replacement mode. Disabling replacement does not erase historical
state or an earlier report.
