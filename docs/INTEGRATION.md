# Optional content integration

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

Without an integration mod, these extra mappings and rules remain empty and
the standalone generator continues to handle suitable new Overworld slopes.
