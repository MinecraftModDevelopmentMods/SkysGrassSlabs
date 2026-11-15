# Snow appearance update for Minecraft 1.12.2

Dirt and grass slabs now pick up snow on higher terraces as well as beside
them. Exposed cold slopes receive their snow cap without waiting for a snow
layer; roofs and strong block light keep that climate-only cap away.

Lowered path slabs no longer support snow at full-block height, so snow layers
cannot hover above them. Existing unsupported layers clear when the path receives
a normal block update.

Other Sky mods can register their physical snow shapes for the same appearance
rules. Grass Slabs still works on its own. The four permanent block and item
IDs, saved slab metadata, configuration and migration state are unchanged.
