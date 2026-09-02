# Repository and release workflow

## Checkout layout

Each Minecraft version uses an independent nested checkout. The Minecraft
1.10.2 checkout uses branch `master-1.10.2`; its parent directory may be used
as the Eclipse workspace. Other versions should use sibling checkouts so they
can be opened and worked on independently.

## Repository topology

- `origin`: `https://github.com/SkyBlade1978/SkysGrassSlabs.git`
- `upstream`: `https://github.com/MinecraftModDevelopmentMods/SkysGrassSlabs.git`

Source changes are pushed only to the SkyBlade1978 fork and proposed to MMD
through pull requests. Never push implementation commits directly to upstream.
Do not use development tool names in branch, folder, commit, artifact or other
public project names.

## CI and publication

The 1.10 branch CI runs Java 8 compilation and tests under Gradle on Java 17,
the build only runtime harness, Javadocs, release artifact and checksum audits,
and Eclipse production classpath verification. Separate workflows validate the
Gradle wrapper and run CodeQL.

The tag workflow validates four part version tags and the prior audited CI
check. It does not publish. Maven, CurseForge project `1677588`, and GitHub
release publication must be performed only by the separately approved manual
dispatcher using the same immutable candidate bundle.

The manual dispatcher uses the existing organisation Maven and CurseForge
secrets. Credentials must never be committed.

## Publication guardrails

Automated success never authorizes publication. A tag, Maven upload,
CurseForge upload, GitHub release, or direct MMD update requires fresh approval
for the exact version, commit, and artifact hash.

Before a fork push or PR:

1. Run the complete local and packaged runtime checks.
2. Record exact candidate hashes and retained evidence.
3. Confirm the source fixture remains unchanged.
4. Confirm no local context, cache, run output, or evidence is tracked.
5. Keep manual visual acceptance and publication status explicit.

## Local context

`AGENTS.md`, `agent-notes/`, `.codex/`, `.claude/`, and other local tool state
are ignored at every depth. They must not be tracked or packaged. Durable
technical knowledge belongs in `README.md` and `docs/`.
