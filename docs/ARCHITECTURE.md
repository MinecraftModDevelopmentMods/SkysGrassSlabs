# Architecture and scope

## Product contract

The minimum 1.18.2 product consists of:

1. A dirt slab and a grass slab with normal top, bottom, double, waterlogging,
   placement, collision, rendering, loot, tool, sound, and creative-tab
   behaviour.
2. Grass survival, decay, and spreading that interoperates with vanilla grass
   blocks and preserves slab orientation.
3. Convenience recipes for dirt plus a seed to make grass, including slab
   equivalents, plus ordinary slab recipes.
4. Smooth grass-slab transitions on newly generated Overworld terrain.
5. Safe conversion of BuildingBricks grass and dirt slabs in old worlds.

Do not expand the initial implementation into BuildingBricks' general material,
stairs, step, corner, vertical-slab, trowel, bag, or ladder systems. Those are
separate product decisions.

## Confirmed BuildingBricks behaviour

The exact Sylvester server mod is `BuildingBricks-1.10.2-2.0.13.jar`. Its
matching source tag is `v1.10.2-2.0.13` in
`<BuildingBricks checkout>`.

Its only world-generation implementation is:

`<BuildingBricks checkout>/src/main/java/com/hea3ven/buildingbricks/compat/vanilla/GrassSlabWorldGen.java`

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

## OreSpawn boundary

The current 1.18.2 OreSpawn source is
`<OreSpawn 1.18.2 checkout>`.

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
