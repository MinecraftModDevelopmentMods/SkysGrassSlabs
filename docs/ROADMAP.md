# Version and compatibility roadmap

## 1.18.2 gameplay betas

Beta `0.1.0.118021` established the permanent namespace and initial gameplay
contract:
`dirt_slab`, `grass_slab`, `path_slab`, `grass_slab_smoothing`, common worldgen
configuration, and world schema version 1. It has no BuildingBricks, OreSpawn,
or Mineralogy runtime dependency or Java API.

Beta `0.2.0.118021` retains that contract, fixes grass-slab overlay rendering,
and adds the permanent `turf` block/item plus the `turf_cutting` recipe
serializer. Turf is a dirt-supported carpet-height grass source and can be used
to green dry dirt slabs. The schema remains version 1 because no existing
save-facing identity or data shape is migrated.

## 1.10.2 BuildingBricks and Sylvester compatibility

After the beta is manually accepted and its public identity is secured, port
the product down to Minecraft 1.10.2 and validate it with BuildingBricks
2.0.13 in complete disposable copies of the Sylvester world.

- Detect BuildingBricks without making it a required dependency.
- Establish a single owner for grass-slab world generation. Prefer switching
  BuildingBricks' generator off through supported configuration and letting
  Sky's Grass Slabs take over; otherwise disable this mod's overlapping pass.
- Accept BuildingBricks dirt/grass slabs in the seed helper recipes.
- Replace BuildingBricks dirt and grass slab blocks and items with the stable
  Sky's Grass Slabs identities while preserving metadata, orientation,
  inventories, entities, and reload idempotence.
- Inventory unsupported BuildingBricks shapes rather than silently deleting or
  coercing them.
- Prove the immutable source fixture's hashes remain unchanged.

## Forward ports through 26.2

Port forward one supported Minecraft version at a time from the newest accepted
product implementation. At every step test both a fresh world and disposable
copies of worlds saved by the preceding Sky's Grass Slabs version. Continue to
carry a converted Sylvester fixture so every data-flattening, registry, loader,
and worldgen boundary is tested rather than assumed.

Keep each Minecraft/loader line on its own branch and checkout. Preserve the
permanent mod ID and save-facing IDs unless an explicit versioned migration is
implemented. No port, migration candidate, or fixture result is published
without fresh approval for that exact artifact.
