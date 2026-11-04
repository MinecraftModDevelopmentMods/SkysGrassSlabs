# Configuration

Open **Mods**, select **Sky's Grass Slabs** and choose **Config** to change the
generation and legacy options. Hover over a setting for help. Choose
**Done** to save, then restart Minecraft. Escape leaves pending choices unsaved.
The screen does not change a remote server's configuration.

The game creates `config/skysgrassslabs-common.toml` after the mod is started for
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

When Terrain Smoother is installed, this setting controls grass inclusion in
its engine. Grass Slabs does not run a second pass, even when Terrain Smoother's
master switch is off. Removing Terrain Smoother restores standalone generation.

## Legacy replacement setting

`compat.forceReplaceBuildingBricksSlabs=false`

This key is retained so configurations and saved migration state remain
consistent with the Minecraft 1.10.2 release. It does not enable replacement
of content from unofficial Minecraft 1.20.1 ports, so ordinary installations
should leave it set to `false`.

Recovery of supported legacy grass and dirt slab IDs from an upgraded 1.10.2
world is automatic and does not require this option. See
[Upgrading an older world](WORLD-UPGRADES.md) before opening an old
modded world.

`compat.forceReplaceGrassSlabsModContent=false`

This separate setting controls supported content from Grass Slabs, Carpets &
Stairs while that mod is still installed. Leave it disabled for normal side by
side use. When enabled, supported slabs, item stacks and safely supported grass
carpet are changed to the matching Sky versions as they load.

If the older mod is removed, recovery of its supported IDs is automatic and
does not require this setting. Grass carpet placed anywhere other than vanilla
dirt is kept under its old ID because changing it to turf would cause the
placement to break later. The setting is independent of world smoothing and
the other legacy replacement option.

## Related game rules

Turf, grass spreading and snowy slab repairs use Minecraft random ticks. Setting
`randomTickSpeed` to zero pauses spreading, grass decay and delayed turf
support checks.

Sheep respect the `mobGriefing` game rule. With mob griefing enabled they eat
and remove turf without a drop. With it disabled they can still perform their
normal eating behaviour and regrow wool, but the turf remains.
