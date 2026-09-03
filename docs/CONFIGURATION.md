# Configuration

Forge creates `config/skysgrassslabs.cfg` after the mod is started for the
first time. Stop the game or server before editing it, then restart to apply
changes. On a dedicated server, the server's configuration controls world
generation and world upgrades.

## World smoothing

`worldgen.generateGrassSlabs=true`

When enabled, suitable one block slopes in newly generated Overworld chunks
may receive grass slabs. Set this to `false` to leave new terrain unchanged.
The setting never adds or removes slabs in chunks that already exist.

When the supported legacy slab mod is also installed, enabling Sky's world
smoothing turns off its overlapping grass slab generator. A backup of the
legacy configuration is made before it is changed. This affects generation
ownership only; it does not by itself replace existing blocks.

## Replacing supported legacy slabs

`compat.forceReplaceBuildingBricksSlabs=false`

The default keeps installed BuildingBricks grass and dirt slabs unchanged so
the two mods can be used together. Set the option to `true` only when you want
supported BuildingBricks grass and dirt slabs and item stacks replaced with
the Sky equivalents as their chunks and containers load.

Replacement is not automatically reversible. Make a world backup first and
read [Existing worlds and legacy slabs](WORLD-UPGRADES.md) before enabling it.
This option is independent from world smoothing.

## Related game rules

Turf and grass spreading use Minecraft random ticks. Setting
`randomTickSpeed` to zero pauses spreading, grass decay and delayed turf
support checks.

Sheep respect the `mobGriefing` game rule. With mob griefing enabled they eat
and remove turf without a drop. With it disabled they can still perform their
normal eating behaviour and regrow wool, but the turf remains.
