# Architecture and scope

## Product contract

The 1.18.2 beta product consists of:

1. Dirt, grass, and path slabs with normal top, bottom, double, waterlogging,
   placement, collision, rendering, loot, tool, sound, and creative-tab
   behaviour.
2. Grass survival, decay, and spreading that interoperates with vanilla grass
   blocks and preserves slab orientation.
3. Convenience recipes for dirt plus a seed to make grass, including slab
   equivalents, plus ordinary slab recipes.
4. Smooth grass-slab transitions on newly generated Overworld terrain.
5. Carpet-height turf cut from grass blocks or grass slabs, with matching soil
   and an unchanged Forge-compatible shovel returned by crafting.
6. A permanent schema marker that future versions can use for save migration.

BuildingBricks migration is deliberately not part of beta `0.2.0.118021`. It
begins on the Minecraft 1.10.2 line and is then proved while the mod is ported
forward. See `ROADMAP.md` and `LEGACY-MIGRATION.md`.

Do not expand the initial implementation into BuildingBricks' general material,
stairs, step, corner, vertical-slab, trowel, bag, or ladder systems. Those are
separate product decisions.

## Confirmed BuildingBricks behaviour

The exact Sylvester server mod is `BuildingBricks-1.10.2-2.0.13.jar`. Its
matching source tag is `v1.10.2-2.0.13` in the
[BuildingBricks repository](https://github.com/SkyBlade1978/BuildingBricks).

Its only world-generation implementation is:

`src/main/java/com/hea3ven/buildingbricks/compat/vanilla/GrassSlabWorldGen.java`

Resolve that path from the tagged BuildingBricks checkout; its location on a
developer's machine is deliberately not part of this repository's contract.

The feature runs at Overworld chunk-population time. For each candidate grass
surface, it places a bottom grass slab above the lower side of a one-block
height transition when adjacent grass is exactly one block higher. It avoids
water, unsupported steep edges, occupied cells, existing grass slabs, and
recursive neighbour generation. It can remove an orphaned upper half of a
double plant. It does not generate any of BuildingBricks' other shapes.

Do not copy its old scan literally. It searches upward from Y=0 and changes
scan bounds based on currently loaded neighbouring chunks. The new feature
must be heightmap-driven, chunk-owned, deterministic, and safe at borders.

## World-generation design

Implement a feature that runs once per generated Overworld chunk at the start
of `VEGETAL_DECORATION`:

- OreSpawn and other terrain/surface work in `LOCAL_MODIFICATIONS` must finish
  first.
- The smoother must run before grass, flowers, and trees are placed.
- Scan only the owning chunk's 16 by 16 columns.
- Read a one-block halo of neighbouring terrain but never write outside the
  owning chunk.
- Use a world-generation heightmap rather than scanning the build height.
- Require eligible natural grass on both levels, exactly one block of height
  difference, a clear and dry target, and suitable support.
- Place bottom grass slabs only. Player-placed top slabs remain a block feature,
  not a world-generation output.
- Do not inspect config strings, registries, or tags repeatedly inside the hot
  column loop. Resolve/bake required predicates before placement.
- Do not smooth existing chunks silently. Existing-terrain smoothing, if ever
  desired, must be an explicit bounded command or retrogen mode with markers.

Use a pure decision function for the local height/state pattern so most edge
cases can be unit tested without launching Minecraft.

## Grass lifecycle

Minecraft 1.18.2's `SpreadingSnowyDirtBlock` only propagates to a target that
is exactly `Blocks.DIRT`; a dirt tag or subclass does not make vanilla grass
convert a custom dirt slab.

Implement target-aware custom behaviour:

- A dirt slab can detect a nearby viable vanilla grass block or this mod's
  grass slab and convert under vanilla-like light and water constraints.
- A grass slab decays to the corresponding dirt slab when it cannot remain
  grass.
- Preserve `SlabType.TOP` or `SlabType.BOTTOM` during conversion.
- Do not grow or retain grass while waterlogged.
- Decide and test double-slab semantics explicitly. The recommended rule is
  that a double dirt slab grows into a vanilla grass block and two combined
  grass slabs normalize to a vanilla grass block, avoiding a full cube with
  grass-slab side texturing.
- Snowy appearance, Silk Touch, drops, bonemeal expectations, and plant support
  all require explicit tests; do not inherit assumptions from full blocks.

Grass slabs and turf share one target-aware spreading helper. This keeps the
loaded-area guard, source light threshold, target eligibility, water checks,
four-attempt pattern, snowy state, orientation preservation, and double-slab
normalization identical for both sources. A turf block is also a viable source
when a dirt slab performs its own source search.

## Turf

Turf is a one-pixel-high `CarpetBlock` with a grass-top texture and biome tint
on every face. It intentionally has no block entity, wool/carpet tag, llama
decoration role, or furnace-fuel entry. Normal placement requires a full
collision block below, matching carpet support and support-loss behaviour.

Only exact vanilla dirt is a lasting substrate. A random tick on any other
support destroys the turf and drops its item. Valid dirt-supported turf remains
turf under cover or low light but does not spread. In adequate light it uses
the shared grass-spread helper and excludes its own supporting dirt from target
selection.

The custom turf item intercepts an upward use on a dry dirt slab: bottom and
top orientation are retained as grass slabs, a double slab normalizes to
vanilla grass, and a waterlogged slab rejects the action without consuming the
turf. Other uses delegate to normal carpet placement.

The special shapeless `turf_cutting` recipe accepts exactly one vanilla grass
block or this mod's grass slab plus one item that advertises Forge's
`SHOVEL_FLATTEN` action. It works in 2 by 2 and 3 by 3 grids, produces one turf,
returns the corresponding dirt block or slab, and returns an unchanged copy of
the shovel including damage, enchantments, and NBT.

## Path slabs

Flatten dirt and grass slabs through Forge's `ToolActions.SHOVEL_FLATTEN` block
hook. Vanilla `ShovelItem` remains untouched, so compatible third-party tools
can use the same block contract. Reject waterlogged slabs, preserve top/bottom
orientation, and normalize a double slab to vanilla dirt path. Vanilla owns
the non-downward-face, clear-space, sound, and durability checks.

A bottom path slab occupies Y 0 through 7/16. A top path slab occupies Y 8/16
through 15/16. Path slabs use vanilla dirt-path textures, always drop dirt
slabs, support no grass vegetation, and turn back into matching dirt slabs when
covered or waterlogged.

## OreSpawn boundary

The 1.18.2 OreSpawn reference is the `master-1.18` line of the
[OreSpawn repository](https://github.com/MinecraftModDevelopmentMods/OreSpawn).
Re-verify its current commit before compatibility testing.

Relevant reference points are:

- `worldgen/BiomeFeatureInstaller.java` for ordered feature stages.
- `worldgen/StoneReplacer.java` and `worldgen/BiomeSurfaceFeature.java` for
  final terrain and surface ordering.
- `api/WorldgenProvider.java` for the existing provider boundary.
- `worldgen/WorldMaterialWeather.java` for a bounded per-column pass.

OreSpawn can choose fixed top/filler/underwater/ceiling materials but cannot
currently express a neighbour-height transition that outputs a partial block.
Do not add an OreSpawn dependency or GrassSlabs-specific OreSpawn schema. If a
second independent consumer later needs a generic surface-contouring contract,
revisit that as a separately designed OreSpawn capability.

The integration requirement is behavioural: with Mineralogy and OreSpawn
installed, smoothing must observe the final grass surface, must not overwrite
provider terrain, structures, fluids, or block entities, and must remain
deterministic across chunk generation order.
