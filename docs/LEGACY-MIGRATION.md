# Legacy BuildingBricks migration

## Safety boundary

The authoritative legacy fixture is:

`<immutable Sylvester fixture>`

It is read-only. Never launch, upgrade, rewrite, repair, or add files to that
directory. Every test must use a complete disposable copy and must verify that
the source fixture's bytes remain unchanged.

The Sylvester configuration has `generateGrassSlabs=true` and uses
`BuildingBricks-1.10.2-2.0.13.jar`. Its `level.dat` Forge registry snapshot
contains `buildingbricks:grass_slab` and other BuildingBricks registrations.
Read saved numeric IDs from the selected world's own registry snapshot; never
hardcode IDs observed in one copy.

## Two migration paths are required

Forge missing-mapping remaps are needed for already-flattened worlds, but they
are insufficient for a raw Minecraft 1.10 world. Pre-flattening chunks store
numeric block IDs plus metadata, and vanilla's later data fixer does not know
arbitrary mod block states.

Support both:

1. Registry remapping for modern named block and item IDs.
2. Pre-flattening state recovery before vanilla chunk datafixing, using the
   saved Forge block registry to install the correct legacy state mapping.

The strongest local reference implementation is:

`<Minecraft Mineralogy 1.18.2 checkout>/src/main/java/zone/moddev/mc/mineralogy/patching/LegacyWorldDataHook.java`

Also inspect:

- `patching/PatchHandler.java`
- `src/main/resources/coremods/mineralogy_legacy_world_fix.js`
- Base Metals 1.18's `Legacy112WorldMigrator` under
  `<Base Metals 1.18.2 checkout>`

Mineralogy's hook currently selects Mineralogy states; it does not automatically
migrate BuildingBricks. Any Sky's Grass Slabs hook must coexist with it in the
same launch. Treat transform order, expansion of Minecraft's legacy state
table, capture of `level.dat` registry data, chunk status preservation, and
item/entity restoration as integration contracts rather than assumptions.

## Initial aliases and state mapping

At minimum investigate and cover:

- `buildingbricks:grass_slab` to `skysgrassslabs:grass_slab`
- `buildingbricks:dirt_slab` to `skysgrassslabs:dirt_slab`
- historical `buildingbrickscompatvanilla:grass_slab`, if present in older
  supported saves
- matching block-item aliases

The BuildingBricks 2.0.13 slab implementation decodes metadata bit zero as:

- metadata 0: top
- metadata 1: bottom

World generation placed its default bottom state. Preserve player-placed top
states as well; verify the mapping against the exact tagged source before
coding it.

## Migration inventory

"The world starts" is not sufficient proof. Inventory and preserve relevant
occurrences in:

- chunk section block arrays and metadata
- player inventories and ender chests
- ordinary containers
- dropped item entities
- item-bearing block entities and mod inventories
- scheduled ticks or other data that names the old block, if present

Before removing BuildingBricks from a real world, inventory every actually
used `buildingbricks:*` block and item. Grass stairs, steps, corners, vertical
slabs, and other material shapes were not world-generated, but players may
have placed or stored them. Do not silently convert unimplemented shapes to
air. Report them as a compatibility blocker or add an explicitly agreed alias.

## Required migration evidence

Use a disposable Sylvester copy and record:

1. Hash/inventory of the untouched source fixture.
2. Saved registry IDs and pre-conversion counts by old ID, metadata, and data
   location.
3. First 1.18.2 start and conversion log with no unresolved BuildingBricks
   grass/dirt slab mappings.
4. Post-conversion counts and representative top/bottom block states.
5. Player/container/item preservation evidence.
6. Clean save/stop and second start/reload with identical converted counts and
   no repeated migration.
7. No new smoothing writes in already-generated chunks unless an explicit
   retrogen test was requested.
8. Source-fixture hashes unchanged after all testing.
