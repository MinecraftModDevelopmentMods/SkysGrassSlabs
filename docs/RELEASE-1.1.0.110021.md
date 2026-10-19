# Sky's Grass Slabs 1.1.0 for Minecraft 1.10.2

Adds optional integration with Sky's Building Pieces and Sky's Terrain Smoother.
Grass can spread between their matching dirt and grass pieces, and the smoother
can manage terrain generation without running a second grass slab pass.

The mod still works on its own. Existing blocks, recipes, configuration defaults
and saved migration progress are retained. This release also includes improved
recovery of legacy slabs in mixed worlds. Back up existing worlds before updating.

Two matching dirt or grass slabs can now be crafted back into a full block.
When BuildingBricks is absent, three hidden recovery blocks keep historical
slab IDs separate while the world loads. They do not appear in creative mode,
but modpacks close to Minecraft 1.10's block-ID limit should allow room for them.

Requires Minecraft 1.10.2, Forge 12.18.3.2511 and Java 8.
