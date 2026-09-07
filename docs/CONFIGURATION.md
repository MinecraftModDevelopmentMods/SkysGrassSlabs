# Configuration

Forge creates `config/skysgrassslabs-common.toml` after the mod starts for the
first time. Stop the game or server before editing it, then restart to apply
changes. On a dedicated server, the server's configuration controls world
generation and world upgrades.

When upgrading from an earlier release, the mod copies valid settings from
`skysgrassslabs.cfg` if the TOML file does not already exist. The old file is
kept. An existing TOML file always takes priority.

## World smoothing

`worldgen.generateGrassSlabs=true`

When enabled, suitable one block slopes in newly generated Overworld chunks
may receive grass slabs. Set this to `false` to leave new terrain unchanged.
The setting never adds or removes slabs in chunks that already exist.

## Legacy slab replacement

`compat.forceReplaceBuildingBricksSlabs=false`

This setting controls replacement only when the older slabs are still
installed. It is disabled by default so existing content remains unchanged.
If enabled, supported grass and dirt slabs and item stacks are changed to the
matching Sky's Grass Slabs versions as they load. Other shapes are left alone.

Recovery of the supported slab IDs when their original mod is absent is
automatic and does not require this option. See
[Upgrading an older world](WORLD-UPGRADES.md) before opening an old modded
world.

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

Turf and grass behaviour use Minecraft random ticks. Setting `randomTickSpeed`
to zero pauses spreading, grass decay and delayed turf support checks.

Sheep respect the `mobGriefing` game rule. With mob griefing enabled they eat
and remove turf without a drop. With it disabled they can still perform their
normal eating behaviour and regrow wool, but the turf remains.
