# Beta 0.3.0.110021

## CurseForge description

Sky's Grass Slabs adds dirt, grass and lowered path slabs to Minecraft 1.10.2,
along with a thin layer of turf. Grass spreads naturally between full blocks,
slabs and turf. Covered grass slabs turn back to dirt, while top grass slabs
support plants and bonemeal. Use a compatible shovel on dirt or grass slabs to
make lowered path slabs.

Snowy areas give grass and dirt slabs a complete snow cap. Grass will not grow
under grass slabs or turf, and sheep can eat turf to regrow their wool.

Craft turf from a grass block or grass slab with a compatible shovel. The
shovel is returned unchanged, and the matching dirt block or slab remains in
the crafting grid. Placing turf on a dirt slab turns it into a grass slab while
keeping it in the same half of the block.

New Overworld terrain uses grass slabs to soften suitable one block height
changes. This is enabled by default and can be disabled with
`worldgen.generateGrassSlabs=false`. The setting only affects chunks generated
after it is changed.

The mod requires Minecraft 1.10.2, Forge 12.18.3.2511 or a compatible 1.10.2
Forge build, and Java 8.

## Release notes

- Added dirt, grass and lowered path slabs, along with turf, to Minecraft
  1.10.2.
- Slabs can be placed in either half of a block. Two matching slabs combine
  into the corresponding vanilla block.
- Grass spreads naturally between full blocks, slabs and turf. Covered grass
  slabs turn back to dirt, while top slabs support plants and bonemeal.
- Using a compatible shovel on dirt or grass slabs creates lowered path slabs.
- Added one pixel high turf, direct dirt slab conversion and grass recipes that
  accept compatible seeds.
- Added grass slabs to suitable height changes in newly generated terrain.
- Added migration support for compatible grass and dirt slabs and items from
  older worlds. Other block shapes are left unchanged.
- Grass no longer remains beneath grass slabs or turf.
- Snowy grass and dirt slabs now have a complete snow cap with snowy side
  edges.
- Sheep can eat turf and regrow their wool. The normal `mobGriefing` game rule
  is respected.

Snow on slabs is visual only in Minecraft 1.10.2. It adds no height, does not
melt by itself and drops no snowballs. The cap appears and disappears with
nearby vanilla snow.

License: GNU Lesser General Public License 2.1 only. Copyright SkyBlade1978.

## Candidate evidence

The candidate was built with Gradle 9.6.1 on Java 17, then compiled, tested and
launched with Temurin Java 8. All 24 focused tests in seven suites pass. The
Forge test runtime passes fresh world and reload checks with 72 gameplay
assertions and 8 world generation assertions, including occupied replaceable
targets. The fixed seed 9×9 chunk comparison passes in forward, reverse and
shuffled orders. The focused 256 column benchmark measured approximately
1,134 ns per pass on the qualification machine.

The exact packaged jar passes Java 8 dedicated server checks for a fresh world
and a reload. It passes alone, with supported older slab content and with the
current geology integration candidates. The compatibility check disables the
overlapping older generator and keeps an exact backup of its original
configuration.

A disposable copy of the complete older world passes its first migration and
a second load without making any further conversions. It converts 1,656,276
grass slabs and 2,968 dirt slabs while preserving their orientation, along
with 7,186 dirt slab items. It reports and leaves unchanged 6,663 unsupported
shapes across 13 IDs. The source fixture contains 1,080 files and 1,380,450,555
bytes. Its aggregate SHA-256 remained
`45C0A84913A71D0F7832F76719A4C3B745DAEA9BE5C8EFD9FEF6B97077EFBF44`.

Two clean builds produced identical main, sources and Javadocs jars. The
release files are:

- `SkysGrassSlabs-0.3.0.110021.jar` - 75,300 bytes - SHA-256
  `DBF2D4EDECCD853600420656846B028D689CB8091459E75D2EB095AFFABF25A5`
- `SkysGrassSlabs-0.3.0.110021-sources.jar` - 46,457 bytes - SHA-256
  `8CBFBE18F5739D7D4428AEBFACDFCB7B039AC6B1A9A65D2F49C01C4741B28369`
- `SkysGrassSlabs-0.3.0.110021-javadoc.jar` - 141,607 bytes - SHA-256
  `14A2136ECF76148A2DED53A5B0B5876C51D7FC90D02FD37CF8B1EA32109A66F9`

Eclipse run generation and production classpath verification pass. The
development client identifies the mod, initializes OpenAL, builds the 512x512
texture atlas, loads all four development mods, and reports no missing project
model or texture. The old ForgeGradle 1.10 launcher logs and ignores a Java 11
multirelease entry. The same harmless warning appears in the qualified 1.10
reference setup.

The jars contain the expected metadata, resources, license notices, and Java 8
classes, with no tests, probes, local paths, credentials, caches, evidence, or
local development context. Publication remains pending manual in game visual
acceptance of grass tinting, grass/dirt snow caps, joins, support dirtification,
stable turf fields, sheep animation, turf, paths, placement, breaking, and
generated slopes, plus separate approval of the exact candidate.
