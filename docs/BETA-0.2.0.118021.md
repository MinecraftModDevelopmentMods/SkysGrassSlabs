# Beta 0.2.0.118021

## CurseForge description

Sky's Grass Slabs adds natural-looking dirt, grass, and path slabs to Minecraft
1.18.2, together with biome-tinted turf that is only one pixel high. Grass
spreads between vanilla blocks, slabs, and dirt-supported turf; grass slabs
decay naturally, support snow and plants on top slabs, and grow vegetation with
bonemeal. Right-click a dirt or grass slab with any compatible shovel to make a
lowered path slab.

Cut turf by crafting one vanilla grass block or grass slab with any vanilla or
Forge-compatible shovel. The shovel is returned completely unchanged, and the
matching dirt block or dirt slab remains as a crafting remainder. Turf behaves
physically like carpet. It may be placed on any full block, but on its random
tick it remains and spreads only when supported by vanilla dirt; invalid
support makes it break and drop. Using turf directly on a dry dirt slab turns
the slab into grass without changing its top or bottom orientation.

New Overworld terrain can receive deterministic grass slabs at suitable
one-block slopes, smoothing sharp steps before vegetation is placed. The
feature is enabled by default and can be disabled with
`worldgen.generateGrassSlabs=false`; the setting affects newly generated chunks
only.

The mod is standalone and requires Minecraft 1.18.2, Forge 40.3.0 or compatible
Forge 40.x, and Java 17. It has no runtime dependency on OreSpawn, Mineralogy,
or BuildingBricks.

## Release notes

- Fixed the dark side overlay on grass slabs by matching vanilla grass's
  cutout-mipped render layer.
- Added permanent block/item ID `skysgrassslabs:turf`.
- Added permanent special recipe serializer
  `skysgrassslabs:turf_cutting` and recipe `skysgrassslabs:turf`.
- Turf uses the vanilla grass-top texture with biome tinting on every face.
- Turf is carpet-height and carpet-like, but is not wool carpet, llama decor,
  or furnace fuel.
- Dirt-supported turf spreads to vanilla dirt and dry dirt slabs using the same
  four-attempt, light, water, and loaded-area rules as grass slabs. It never
  converts its own support.
- Turf has no dirt stage. Cover or low light pauses spreading; non-dirt support
  makes turf break and drop on its random tick.
- Using turf on bottom or top dirt slabs preserves orientation; using it on a
  double dirt slab produces vanilla grass; waterlogged dirt slabs reject it.
- Crafting turf returns the matching dirt block/slab and an unchanged
  compatible shovel, preserving damage, enchantments, and NBT.
- All `0.1.0.118021` block, item, feature, configuration, and world-state IDs
  are unchanged. World schema remains version 1.

Known beta boundary: BuildingBricks detection, generator arbitration, helper
recipes, and save migration remain deferred to the planned Minecraft 1.10.2
compatibility release.

License: GNU Lesser General Public License 2.1 only. Copyright SkyBlade1978.

## Candidate evidence

The candidate was built and tested with Java 17 and Forge 40.3.0. All 16 Java
tests in three suites and all 11 Forge GameTests pass. The deterministic
world-smoothing comparison passed across the fixed 9x9-chunk generation
orders, and the focused smoother benchmark processed 1,048,576 columns in
3,401,400 ns on the validation machine.

Two clean builds produced the same 61,257-byte jar and SHA-256:

`FE910235B45630D76F5C3392B9FD0D5C21BBBD576DD0CFB030566048B515A2A3`

Eclipse run generation and project verification passed. The development
client and exact packaged client both loaded the mod, initialized OpenAL,
built every texture atlas, reached the main menu, and exited cleanly. The
exact packaged jar also passed dedicated-server fresh-world and reload gates,
including creation and reuse of the schema-version-1 world state.

The three-mod packaged integration gate passed fresh-world and reload tests
with OreSpawn 4.0.16.118021 and Mineralogy 6.1.0.118021. OreSpawn's provider
reported 32 rocks, three ores, and one fluid active. The candidate jar was
audited for the turf resources, metadata, license, Java 17 bytecode, forbidden
local paths, tests, agent material, and accidental integration dependencies;
no release-blocking findings or crash reports were present.

Publication remains pending manual in-world visual acceptance of grass-slab
sides beside vanilla grass in several biomes; top, bottom, snowy, and joined
slabs; and turf tint, geometry, placement, spreading, conversion, breaking, and
invalid-support removal. No remote, tag, upload, or publication action has been
performed.
