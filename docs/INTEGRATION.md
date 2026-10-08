# Optional content integration

Grass Slabs remains standalone. Terrain Smoother 0.1 uses Grass Slabs 1.1's
version 1 API in `zone.moddev.mc.skysgrassslabs.api.GrassSlabsApi`.

A terrain owner calls `claimSmoothing(modid, enabled)` before Grass Slabs
pre-initialization. Installed ownership suppresses the separate grass pass,
including when the owner is disabled. `grassGenerationAllowed()` incorporates
the player's grass setting and the legacy-generator arbitration result.

Content mods register ordinary dirt/grass state pairs with `registerGrassForm`
during initialization, one pair for each orientation. Registration rejects
conflicts and closes at load completion. Never register coarse dirt or podzol.
`tickGrass`, `tickDirt` and `repairSupport` retain matching state orientation,
grass lighting rules and covered-target rejection. Integration must be optional
when the other content mod is not installed.

`registerGrassVariant(dirt, grass, spreads)` supports additional grass skins
sharing a dirt state without replacing its default regrowth. Set `spreads` to
false for native surfaces that do not spread. `registerGrassSupport` supplies
native full grass/dirt pairs for support repair.

An add-on can register a source-aware `registerSpreadRule(modid, rule)` to retain
its native grass family. The callback receives the source grass and target dirt
states and returns a registered matching grass state, or null to use the default
pair. It must not load chunks or write to the world. All registration closes at
load completion. Without an add-on these rules leave vanilla behaviour unchanged.
