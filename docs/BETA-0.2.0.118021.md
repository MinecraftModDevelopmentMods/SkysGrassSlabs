# Beta 0.2.0.118021

## CurseForge description

Sky's Grass Slabs adds dirt, grass and path slabs to Minecraft 1.18.2, along
with a thin layer of turf. Grass matches the biome and spreads naturally
between full blocks, slabs and turf. Grass slabs turn back to dirt when
covered, while top grass slabs support snow, plants and bonemeal. Use a
compatible shovel on a dirt or grass slab to make a lowered path slab.

Craft turf from a grass block or grass slab with a compatible shovel. The
shovel is returned unchanged, and the matching dirt block or slab remains in
the crafting grid. Turf has the shape and movement of carpet and spreads grass
while supported by dirt. Placing turf on a dry dirt slab turns it into a grass
slab while keeping it in the same half of the block.

New Overworld terrain uses grass slabs to soften suitable one block height
changes. This is enabled by default and can be disabled with
`worldgen.generateGrassSlabs=false`. The setting only affects chunks generated
after it is changed.

Requires Minecraft 1.18.2, Forge 40.3.0 or a compatible Forge 40.x build, and
Java 17.

## Release notes

- Fixed the dark texture on the sides of grass slabs.
- Added thin turf that matches the biome and has the shape and movement of
  carpet.
- Turf spreads grass while it is supported by dirt. Cover or low light pauses
  spreading, while invalid support makes the turf break and drop.
- Placing turf on a dry dirt slab turns it into a grass slab and keeps the slab
  in the same half of the block. A waterlogged dirt slab rejects it.
- Crafting grass with a compatible shovel produces turf. The matching dirt and
  unchanged shovel remain in the crafting grid.
- Existing block, item, configuration and world save identities are unchanged.

Compatibility and world migration work for Minecraft 1.10.2 is planned for a
later release.

License: GNU Lesser General Public License 2.1 only. Copyright SkyBlade1978.

## Candidate evidence

The candidate was built and tested with Java 17 and Forge 40.3.0. All 16 Java
tests in three suites and all 11 Forge GameTests pass. The deterministic
world smoothing comparison passed across the fixed 9×9 chunk generation
orders, and the focused smoother benchmark processed 1,048,576 columns in
3,401,400 ns on the validation machine.

Two clean builds produced the same 61,257 byte jar and SHA-256:

`FE910235B45630D76F5C3392B9FD0D5C21BBBD576DD0CFB030566048B515A2A3`

Eclipse run generation and project verification passed. The development
client and exact packaged client both loaded the mod, initialized OpenAL,
built every texture atlas, reached the main menu, and exited cleanly. The
exact packaged jar also passed dedicated server checks with a fresh world and
after a reload, including creation and reuse of the schema version 1 world
state.

The packaged integration check passed fresh world and reload tests
with OreSpawn 4.0.16.118021 and Mineralogy 6.1.0.118021. OreSpawn's provider
reported 32 rocks, three ores, and one fluid active. The candidate jar was
audited for the turf resources, metadata, license, Java 17 bytecode, forbidden
local paths, tests, agent material, and accidental integration dependencies;
no problems that would block a release and no crash reports were present.

Publication remains pending manual in game visual acceptance of grass slab
sides beside vanilla grass in several biomes; top, bottom, snowy, and joined
slabs; and turf tint, geometry, placement, spreading, conversion, breaking, and
invalid support removal. The source is hosted in the MMD repository and the
SkyBlade1978 fork. No tag, release, CurseForge file upload, or binary publication
has been performed.
