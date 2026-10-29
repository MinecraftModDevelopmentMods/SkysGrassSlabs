# Configuration

Open **Mods**, select **Sky's Grass Slabs** and choose **Config** to change
world generation and the legacy replacement option. Save with **Done**, then
restart Minecraft to apply your choices. Settings are not changed mid-world.

You can also edit `config/skysgrassslabs.cfg` while the game or server is
stopped. On a dedicated server, edit the server's file and restart it; the
client's Config screen does not change a remote server.

## World smoothing

`worldgen.generateGrassSlabs=true`

When enabled, suitable one block slopes in newly generated Overworld chunks
may receive grass slabs. Set this to `false` to leave new terrain unchanged.
The setting never adds or removes slabs in chunks that already exist.

With Sky's Terrain Smoother installed, this setting controls its grass material.
Terrain Smoother owns the only Sky terrain pass, even when its master switch is
off. Removing Terrain Smoother restores standalone grass slab generation.

## Legacy replacement setting

`compat.forceReplaceBuildingBricksSlabs=false`

This key is retained so configurations and saved migration state remain
consistent with the Minecraft 1.10.2 release. There is no supported
BuildingBricks release for Minecraft 1.12.2, so ordinary 1.12.2 installations
should leave it set to `false`.

Recovery of supported legacy grass and dirt slab IDs from an upgraded 1.10.2
world is automatic and does not require this option. See
[Upgrading an older world](WORLD-UPGRADES.md) before opening an old
modded world.

## Related game rules

Turf and grass spreading use Minecraft random ticks. Setting
`randomTickSpeed` to zero pauses spreading, grass decay and delayed turf
support checks.

Sheep respect the `mobGriefing` game rule. With mob griefing enabled they eat
and remove turf without a drop. With it disabled they can still perform their
normal eating behaviour and regrow wool, but the turf remains.
