# Beta 0.1.0.118021

## CurseForge description

Sky's Grass Slabs adds natural-looking dirt, grass, and path slabs to Minecraft
1.18.2. Grass keeps its biome tint, spreads between vanilla blocks and slabs,
decays when covered or waterlogged, supports snow and plants on top slabs, and
can be grown with bonemeal. Right-click a dirt or grass slab with any compatible
shovel to make a lowered path slab.

New Overworld terrain can receive deterministic grass slabs at suitable
one-block slopes, smoothing sharp steps before vegetation is placed. The
feature is enabled by default and can be disabled with
`worldgen.generateGrassSlabs=false`; the setting affects newly generated chunks
only.

The mod is standalone and requires Minecraft 1.18.2, Forge 40.3.0 or compatible
Forge 40.x, and Java 17. It has no runtime dependency on OreSpawn, Mineralogy,
or BuildingBricks.

## Release notes

- Permanent mod ID: `skysgrassslabs`
- Blocks/items: `dirt_slab`, `grass_slab`, and `path_slab`
- Dirt and grass blocks craft into six matching slabs.
- Dirt plus any `forge:seeds` makes grass; the same helper recipe works for dirt
  slabs.
- Grass slabs drop dirt slabs normally and themselves with Silk Touch.
- Path slabs always drop dirt slabs and have no survival recipe.
- Two grass slabs normalize to vanilla grass; two path slabs normalize to
  vanilla dirt path; double dirt grows or flattens into the matching vanilla
  full block.
- World smoothing is new-generation-only and never performs automatic retrogen.

Known beta boundary: BuildingBricks detection, generator arbitration, helper
recipes, and save migration are deferred to the planned Minecraft 1.10.2
compatibility release. Manual visual acceptance must be completed on the exact
candidate jar before publication.

License: GNU Lesser General Public License 2.1 only. Copyright SkyBlade1978.

## Candidate evidence

### Retained packaged candidate

Candidate artifact:

- `SkysGrassSlabs-0.1.0.118021.jar`
- SHA-256:
  `A6410F67D68575C4D5D79CBB6D1AE5BF1F54AD0B14EADFFDB0CE91FF3DCDE845`
- The main jar was rebuilt from a clean output directory and reproduced the
  same SHA-256 byte for byte.
- The jar audit found the three license/notice files, expanded metadata,
  required classes and resources, and no test classes, GameTests, agent notes,
  local paths, run output, or bundled integration mods.

Completed local gates:

- Java 17 clean unit tests, resource processing, build, and Javadocs passed.
  Javadocs produced only missing-comment warnings.
- Eclipse run/configuration generation passed.
- All seven independently batched GameTests passed, covering block states,
  lifecycle and vanilla interoperation, shovel flattening, path geometry and
  decay, drops and recipes, saved state, and controlled whole-chunk smoothing.
- The allocation-light smoothing decision benchmark evaluated 1,048,576
  columns in 7,863,699 ns on this development machine. This is a diagnostic
  timing, not a cross-machine performance guarantee.
- A packaged dedicated server containing the exact candidate jar created a
  fresh world, reached `Done`, stopped cleanly, then reloaded the world and
  repeated the clean start/stop. The schema-versioned
  `skysgrassslabs_world_state` save data was present.
- A packaged client containing the exact candidate jar loaded the mod, baked
  its models and textures, created every texture atlas, initialized audio, and
  reached the main menu without a crash, missing model, or missing texture.
- A packaged dedicated server containing the exact candidate plus OreSpawn
  `4.0.16.118021` (source commit `4a9f9447293f4f6c088ef498a6f91216ba4c77f7`)
  and Mineralogy `6.1.0.118021` (source commit
  `c3368e6dba2263a6e3b35424af206e9cf062cc1a`) created and reloaded a fresh
  world cleanly. OreSpawn reported Mineralogy as its active provider.

The proposed Sky's Grass Slabs plus Mineralogy-only matrix row is not a valid
1.18.2 installation: Mineralogy's metadata declares OreSpawn as a mandatory
dependency. It was therefore not reported as a failed Sky's Grass Slabs gate;
the valid three-mod packaged runtime was tested instead.

### ForgeGradle 7 workspace validation

The project was subsequently migrated to ForgeGradle 7.0.34 and Gradle 9.6.1.
The current unpromoted local build is:

- `build/libs/SkysGrassSlabs-0.1.0.118021.jar`
- SHA-256:
  `94D444B7D5B4FC4FD8612B8D340A3ABB7EA284FAEFC3CAF4938AFB26550DCB64`

Two clean builds reproduced that hash byte for byte. Unit tests, all seven
GameTests, ForgeGradle 7 Eclipse generation/verification, and a development
client smoke through mod loading, OpenAL, and texture-atlas creation passed.
The original duplicate-`fmlloader` Eclipse startup failure did not recur.

This newer jar has not replaced the retained `release/` candidate because its
packaged standalone server, packaged client, and three-mod integration gates
have not yet been repeated. Promote it only after those exact-jar gates and
manual visual acceptance pass.

Pending before publication: manual in-world visual acceptance of biome tinting,
slab joins, generated slopes, snow, walking/collision, path height, shovel
interaction, placement, and breaking. No remote, PR, tag, upload, or other
publication action was performed.
