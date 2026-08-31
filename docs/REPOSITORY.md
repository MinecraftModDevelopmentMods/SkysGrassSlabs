# Repository and release workflow

## Current repository

`<workspace>` is the multi-version workspace wrapper and is not a Git
repository. The 1.18.2 Git/Gradle checkout is
`<repository>` on branch
`master-1.18.2`. It has no remotes. Do not invent remote URLs or create hosted
repositories without the user's direction.

The intended hosted topology, once those repositories actually exist, is:

- `origin`: the SkyBlade1978 fork, normally
  `https://github.com/SkyBlade1978/SkysGrassSlabs.git`
- `upstream`: the MMD repository, normally
  `https://github.com/MinecraftModDevelopmentMods/SkysGrassSlabs.git`

Verify both URLs and permissions before adding them. Ordinary pushes go to
`origin`; MMD work is proposed through a pull request to `upstream`.

## Branching

- Current target branch: `master-1.18.2`; routine work may remain directly on
  this branch while the project is in early local development.
- Create a focused branch only when parallel or isolated work genuinely needs
  one. Keep branch, folder, commit, artifact, and other project-facing names
  free of coding-agent or tool branding.
- Keep each Minecraft version and loader lineage on a distinct branch and,
  when concurrent work begins, a distinct version checkout under
  `<workspace>\SkysGrassSlabs<version>\SkysGrassSlabs`.
- Do not mix a future NeoForge line into a Forge branch.
- Use the newest accepted feature-complete product branch as the product source
  for a forward port, then overlay only the target MDK scaffold and deliberate
  API/resource adaptations.

The outer version folder can also serve as that version's Eclipse workspace,
matching the established OreSpawn and Mineralogy layout.

## Publication guardrails

Automated success never authorizes publication. Do not push, open an MMD PR,
tag, publish Maven/CurseForge/GitHub artifacts, or update an MMD branch until:

1. The exact commit and artifact hashes are recorded.
2. Required automated and packaged-runtime gates pass.
3. The user manually tests that exact candidate.
4. The user gives fresh, explicit approval naming this version/branch and the
   requested external action.

Approval for a different version or an earlier candidate does not carry
forward. Once an upstream remote exists, install a local ignored pre-push guard
that permits ordinary fork pushes but rejects direct upstream pushes unless
the exact action has been freshly authorized.

Never force-push or delete an MMD branch as routine cleanup.

## Commits and local context

Keep commits focused and leave the worktree clean at handoff unless a candidate
is intentionally awaiting manual testing. Do not include generated run data,
Gradle caches, Eclipse metadata, evidence directories, or local paths.

`AGENTS.md`, `agent-notes/`, and tool-specific working state are local and must
remain untracked at every repository depth. Durable technical and contributor
knowledge belongs in tracked `README.md` and `docs/`. Before any public push,
verify that no local guidance or tool-state file is tracked or present in the
candidate jar.

The project is licensed `LGPL-2.1-only`, copyright SkyBlade1978. The root
license, SPDX marker, and notice must be present in source and packaged jars.
If substantial BuildingBricks code is copied, carry its MIT copyright and
permission notice as required.
