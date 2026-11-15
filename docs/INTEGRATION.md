# Optional content integration

`coversSupport(state, world, pos)` reports whether a piece covers the whole
horizontal footprint. Support repair leaves grass beneath narrower steps and
corners intact, and that exposed soil can regrow. Full-cover slabs and turf
retain their previous support rules.

Grass Slabs works on its own. Content and terrain mods can use version 1 of
`zone.moddev.mc.skysgrassslabs.api.GrassSlabsApi` without changing its saved IDs.

A terrain owner calls `claimSmoothing(modid, enabled)` before Grass Slabs loads
its configuration. Ownership suppresses the separate Grass Slabs generation
pass, even when the owner's master switch is off. `grassGenerationAllowed()`
respects the player's grass setting and the legacy-generator safety checks.

During initialization, register each ordinary dirt/grass orientation with
`registerGrassForm`. Conflicting mappings and late registrations are rejected.
Do not register coarse dirt or podzol. `tickDirt`, `tickGrass` and `repairSupport`
share the grass lifecycle while retaining the registered shape and orientation.
Content mods should keep this integration optional.

`registerGrassVariant` allows other grass skins to share a dirt state without
changing its default regrowth. Non-spreading skins can opt out of spreading.
`registerGrassSupport` supplies native full-block grass/dirt pairs for support
repair. `registerSpreadRule` can match grass to its source family; return null
to use the default registered pair. Callbacks must not load chunks or write to
the world. Registration closes at load completion.

`registerSnowForm(state)` identifies a physical snow shape. Register each
orientation during initialization; do not register grass with a calculated
snow cap. Air, tile entities, duplicate entries and late registrations are
rejected. `isSnowForm(state)` recognises those states and vanilla snow, while
`hasSnowAppearance(world, pos)` shares the slab appearance rule with finer
pieces. It checks nearby physical snow, higher terraces and exposed cold
ground without loading neighbouring chunks. Derived snowy grass flags do not
become new snow sources. These additions retain API version 1.

Without an integration mod, these extra mappings and rules remain empty and
the standalone generator continues to handle suitable new Overworld slopes.
