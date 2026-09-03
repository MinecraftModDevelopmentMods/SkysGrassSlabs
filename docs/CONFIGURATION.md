# Configuration

Forge creates `config/skysgrassslabs-common.toml` after the mod is started for
the first time. Stop the game or server before editing it, then restart to
apply changes. On a dedicated server, the server's configuration controls
world generation and world upgrades.

When upgrading from an earlier Sky's Grass Slabs release, the mod copies valid
settings from `skysgrassslabs.cfg` if the TOML file does not already exist. The
old file is kept as a backup. An existing TOML file always takes priority.

## World smoothing

`worldgen.generateGrassSlabs=true`

When enabled, suitable one block slopes in newly generated Overworld chunks
may receive grass slabs. Set this to `false` to leave new terrain unchanged.
The setting never adds or removes slabs in chunks that already exist.

## Legacy replacement setting

`compat.forceReplaceBuildingBricksSlabs=false`

This key is retained so configurations and saved migration state remain
consistent with the Minecraft 1.10.2 release. It does not enable replacement
of content from unofficial Minecraft 1.13 ports, so ordinary installations
should leave it set to `false`.

Recovery of supported legacy grass and dirt slab IDs from an upgraded 1.10.2
world is automatic and does not require this option. See
[Upgrading an older world](WORLD-UPGRADES.md) before opening an old
modded world.

## Related game rules

Turf, grass spreading and snowy slab repairs use Minecraft random ticks. Setting
`randomTickSpeed` to zero pauses spreading, grass decay and delayed turf
support checks.

Sheep respect the `mobGriefing` game rule. With mob griefing enabled they eat
and remove turf without a drop. With it disabled they can still perform their
normal eating behaviour and regrow wool, but the turf remains.
