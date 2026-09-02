# Release 1.0.0.110021

## CurseForge release notes

This is the first stable Sky's Grass Slabs release for Minecraft 1.10.2.

### Highlights

- Dirt, grass and lowered path slabs can be placed in either half of a block.
- Grass spreads naturally between full blocks, slabs and turf.
- Use a compatible shovel on dirt or grass slabs to make path slabs.
- Cut one pixel high turf from grass without using up the shovel.
- Sheep can eat turf and regrow their wool.
- Grass slabs soften suitable one block height changes in newly generated
  terrain.
- Compatible grass and dirt slabs from older worlds can be migrated when the
  optional replacement setting is enabled.
- Added German, Spanish, French, Japanese, Korean, Portuguese, Russian and
  Simplified Chinese block names.

Existing compatible slabs are left unchanged by default. World smoothing only
affects newly generated chunks, and both settings can be changed independently.

Snow caps on slabs are visual in Minecraft 1.10.2. They add no height, do not
melt by themselves and do not drop snowballs.

Requires Minecraft 1.10.2, Forge 12.18.3.2511 or a compatible 1.10.2 Forge
build, and Java 8.

## Acceptance

The release candidate passed its automated gameplay, world generation,
migration, reload, Eclipse and packaged server checks. It also passed manual
testing in a large existing world and in newly generated terrain. Completed
legacy conversions remained stable across repeated reloads.

Two clean builds produced the same jars and checksums. The main jar is 83,251
bytes with SHA-256
`2030960E217C3F61AE4919C91058696B02F9FAE570BE1CD7B698696EA7BEB861`.
All production classes use Java 8 bytecode.

Publication remains a separate manual step. No tag or release has been created.
