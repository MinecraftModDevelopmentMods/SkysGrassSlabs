# Gameplay and recipes

## Slabs

Dirt, grass and path slabs can be placed in the top or bottom half of a block.
Placing a matching slab into the empty half combines the pair into the related
vanilla block:

- two dirt slabs become a dirt block;
- two grass slabs become a grass block;
- two path slabs become a grass path block.

Grass slabs use the surrounding biome colour. They spread grass to nearby dirt
blocks and dirt slabs, and vanilla grass can spread back to dirt slabs. A grass
slab turns into a dirt slab when it is covered or cannot receive enough light.
Only a top grass slab supports plants and bonemeal growth.

Use a compatible shovel on a dirt or grass slab to make a lowered path slab.
The slab stays in the same half of the block and the shovel takes the normal
durability damage. Path slabs do not spread grass or support plants.

Grass and dirt slabs show a snow covered top and snowy side edges near snow,
including snow on the next higher terrace and registered snow pieces. Exposed
cold ground also receives its cap without waiting for a snow layer. A roof or
strong block light prevents this climate-only cap. The appearance is calculated
as the world is drawn, so removing the snow updates it without changing saved
metadata. It adds no height, does not melt independently and drops no snowballs.

Snow layers can settle on top grass and dirt slabs. Bottom slabs, lowered paths
and turf cannot support a layer at full-block height.

## Turf

Turf is a one pixel high layer that behaves much like carpet. It can be placed
on top of a full block, but only dirt can support it permanently. Unsupported
turf breaks during a later random tick.

Turf spreads grass to nearby dirt and dirt slabs. Placing turf directly on a
dirt slab consumes the turf and changes the slab into a grass slab without
changing whether it is in the top or bottom half.

Sheep can eat turf and regrow their wool. Turf eaten while `mobGriefing` is
enabled is removed without dropping an item.

## Crafting

- Three dirt blocks in a horizontal row make six dirt slabs.
- Three grass blocks in a horizontal row make six grass slabs.
- Two matching dirt or grass slabs side by side make the matching full block.
- A dirt block and a recognised seed make a grass block.
- A dirt slab and a recognised seed make a grass slab.
- A grass block or grass slab together with a compatible shovel makes one
  turf. The shovel is returned unchanged and the matching dirt block or slab
  remains in the crafting grid.

The turf recipe works in the player's 2 by 2 crafting grid as well as a
crafting table. In Minecraft 1.12.2 it appears in the recipe book after the
player obtains a grass block or grass slab. Path slabs have no survival
crafting recipe.

Grass slabs normally drop dirt slabs and drop themselves with Silk Touch. Path
slabs always drop dirt slabs, including when Silk Touch is used.

## World generation

New Overworld chunks may receive bottom grass slabs at suitable natural one
block height changes. Existing chunks are not altered by the smoothing
feature. See [Configuration](CONFIGURATION.md) if a modpack or server should
disable it.
