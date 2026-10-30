# Building and checking this branch

Use the Gradle wrapper with Java 17 and set `SGS_JAVA8_HOME` to the Java 8
installation used for Minecraft. Run these commands from the checkout:

```text
gradlew clean check build javadoc genEclipseRuns verifyEclipseProductionClasspath verifyReleaseArtifacts writeReleaseChecksums
```

The checks include unit tests and disposable fresh/reload servers. Test mods
are not included in the main jar or ordinary Eclipse launches.

## Runtime logs

Each integration server log is saved and checked before the next process can
overwrite it. An unexpected warning, error, crash, missing recovery notice or
remaining legacy-prefix warning fails the check.

To check another captured verification log:

```text
gradlew verifyRuntimeLog -PruntimeLog=<log> -PruntimeDirectory=<disposable game folder> -PruntimeKind=packaged -PruntimeSide=server
```

Use `development` only for a mapped development launch and `client` for a
client log. Offline-server warnings are accepted only when that test folder
is configured for loopback networking on an automatically allocated port.
Use `-PrecoveryMessage=false` when the original legacy mod is installed.

The development allowances cover specific old-Forge launcher and class-scanner
diagnostics, with their expected file names and paired messages. They do not
permit arbitrary corrupt jars or missing classes. Packaged runs do not inherit
those allowances. Unrelated warnings remain visible in the game logs.

Cross-version fixtures have separate, reviewed diagnostics tied to their archive
checksums. These cover the test-only generator mod and exact vanilla numeric-ID
changes. They are not applied to ordinary worlds or used to silence the game.

## Recipe-book resources

The build generates unlock advancements from `src/main/recipe-book/recipes.json`.
The runtime checks compare those entries with the registered recipes and exercise
inventory discovery, crafting-table discovery and recipe-book persistence.
Optional catalogue entries load only when their recipe exists.

Recipe unlocks have no shared advancement parent. Minecraft 1.12 walks that
visibility tree recursively when loading completed progress, which can overflow
with a large catalogue. The inventory and crafting unlock criteria are unchanged.
