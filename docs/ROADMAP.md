# Version and compatibility roadmap

## Completed product lines

- `0.1.0.118021` established the permanent IDs, dirt, grass and path slab
  gameplay, smoothing for new chunks, common config and schema 1 world state on
  1.18.2.
- `0.2.0.118021` fixed the grass overlay and added permanent turf and
  `turf_cutting` identities.
- `0.3.0.110021` backports the complete product to 1.10.2 and adds the
  Sylvester migration anchor. Its complete disposable fixture migration and
  second load check pass locally. The 1.10 contract also includes keeping grass
  coverings on dirt, preventing covered targets, transient visual grass and
  dirt snow caps, sheep eating turf, and replacement of installed compatible
  legacy slabs only when explicitly enabled.
- `1.0.0.110021` promotes the manually accepted 1.10.2 implementation without
  changing gameplay, save identities, migration rules or world generation.

## 1.10.2 release handoff

Remaining external steps are deliberately separate from implementation:

- review and merge of the approved source pull request;
- tag and publication only after separate approval of the exact artifact.

## Forward ports through 26.2

Port one supported Minecraft version at a time from the newest accepted
implementation with the complete feature set. Each port must load:

- a fresh world;
- a world saved by the preceding Sky's Grass Slabs version; and
- a disposable copy descended from the converted Sylvester fixture whenever
  the target version can load that format.

Keep each Minecraft/loader line on its own branch and checkout. Preserve the
mod ID, block/item IDs, smoothing config, recipe identity appropriate to the
target version, and schema-1 migration anchor unless an explicit versioned
migration changes them.

Every later port must also preserve the gameplay intent established here:
grass coverings keep their supporting full block as dirt; grass cannot persist
beneath turf or grass slabs; slab snow presentation covers the top and sides
without corrupting saved orientation; and sheep can eat turf using the least
invasive hook supported by the target version.

Do not assume data flattening, registry, loader or world generation boundaries
work because compilation succeeds. Prove each boundary with tests using the
saved world fixtures before advancing to the next version.
