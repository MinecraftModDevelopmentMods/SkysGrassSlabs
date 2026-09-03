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

## Legacy replacement setting

`compat.forceReplaceBuildingBricksSlabs=false`

This key is retained so configurations and saved migration state remain
consistent with the Minecraft 1.10.2 release. There is no supported
BuildingBricks release for Minecraft 1.11.2, so ordinary 1.11.2 installations
should leave it set to `false`.

Recovery of supported legacy grass and dirt slab IDs from an upgraded 1.10.2
world is automatic and does not require this option. See
[Upgrading a Minecraft 1.10.2 world](WORLD-UPGRADES.md) before opening an old
modded world.

## Related game rules

Turf and grass spreading use Minecraft random ticks. Setting
`randomTickSpeed` to zero pauses spreading, grass decay and delayed turf
support checks.

Sheep respect the `mobGriefing` game rule. With mob griefing enabled they eat
and remove turf without a drop. With it disabled they can still perform their
normal eating behaviour and regrow wool, but the turf remains.
