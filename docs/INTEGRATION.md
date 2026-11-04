# Optional content integration

Grass Slabs works on its own. Content and terrain mods can use version 1 of
`zone.moddev.mc.skysgrassslabs.api.GrassSlabsApi` without changing its saved IDs.
Minecraft 1.20.1 uses native `BlockState` values in this interface.

A terrain owner calls `claimSmoothing` during mod construction. Ownership
suppresses the separate Grass Slabs generation pass, including when the
owner's master switch is off. Forge loads configuration after construction,
so an owner can supply its enabled setting through a `BooleanSupplier`.
`grassGenerationAllowed()` still respects the player's grass setting.

During common setup, register each ordinary dirt/grass orientation with
`registerGrassForm`. Register waterlogged forms separately so decay preserves
their water. Wet forms do not spread grass. Snow is treated as a visual
property and does not change the material mapping.

`registerGrassVariant` lets another grass skin share a dirt state without
changing its default regrowth. Non-spreading skins can opt out of spreading.
`registerGrassSupport` supplies native grass/dirt pairs for support repair.
`registerSpreadRule` can match grass to its source family; return null to use
the default registered pair. Callbacks must not load chunks or write to the
world. Conflicting mappings and registration after load completion are rejected.

`tickDirt`, `tickGrass` and `repairSupport` share the grass lifecycle while
retaining the registered shape. `coversSupport` reports whether a piece covers
the horizontal footprint. Grass beneath narrower steps and corners remains
exposed and can regrow; full-cover slabs and turf retain their support rules.

Content mods should keep this integration optional. Without a terrain owner,
the standalone generator continues to handle suitable new Overworld slopes.
