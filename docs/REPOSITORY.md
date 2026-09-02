# Repository and release workflow

## Current repository

The 1.18.2 source is the `master-1.18.2` branch of this repository. The clone
location is developer-selected and is not part of the build or repository
contract.

The public repository topology is:

- `origin`: the SkyBlade1978 fork at
  `https://github.com/SkyBlade1978/SkysGrassSlabs.git`
- `upstream`: the MMD repository at
  `https://github.com/MinecraftModDevelopmentMods/SkysGrassSlabs.git`

Ordinary pushes go to `origin`; MMD work is proposed through a pull request to
`upstream`. The local pre-push guard blocks direct updates to the MMD repository.

## Branching

- Current target branch: `master-1.18.2`; routine work may remain directly on
  this branch while the project is in early local development.
- Create a focused branch only when parallel or isolated work genuinely needs
  one. Keep branch, folder, commit, artifact, and other project-facing names
  free of coding-agent or tool branding.
- Keep each Minecraft version and loader lineage on a distinct branch and,
  when concurrent work begins, a distinct checkout or Git worktree.
- Do not mix a future NeoForge line into a Forge branch.
- Use the newest accepted feature-complete product branch as the product source
  for a forward port, then overlay only the target MDK scaffold and deliberate
  API/resource adaptations.

An Eclipse workspace may live in any developer-selected directory outside the
checkout; import the checkout itself as an existing Gradle project.

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

## CI and manual release dispatcher

Branch CI builds, tests, audits, and checksums the Java 17 candidate and
verifies the ForgeGradle 7 Eclipse configuration. Separate workflows validate
the Gradle wrapper and run CodeQL.

The manual release dispatcher lives on `master-1.18.2` so GitHub can route a
four-component version to its matching version/loader branch. It checks the
branch metadata and successful audited CI result, builds one immutable bundle,
then requires the exact live publication confirmation. Only that bundle may be
sent to Maven, CurseForge project `1677588`, and the GitHub release. GitHub
publication runs last.

The dispatcher uses the existing organisation Maven and CurseForge secrets.
No secret belongs in source control.

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
