# Build and release

The version source is `gradle.properties`. A 0.x release is not a blanket stable-API promise: preserve and check the intended public descriptors, and document any migration boundary. Minecraft version and loader remain part of each distribution identity.

## Build and verify

Use JDK 17 for common tests and Forge, and JDK 21 for Fabric/NeoForge. `tools/run_packaging.py` supports `JAVA17_HOME` and `JAVA21_HOME`. Supply an existing host font for the source tests.

```powershell
.\gradlew.bat :core:test :runtime:test :ui:test --no-daemon "-PatomlibTestFont=C:/path/to/font.ttf"
python tools/run_packaging.py all --jobs 1
.\gradlew.bat :publishToTestRepository --no-daemon
python tools/package_maven_input.py
```

The packaging command builds the three platform distributions, publishes their merged coordinates to `build/test-maven`, verifies JAR contents/natives, and checks six isolated example consumers plus pure Maven probes. Run the root publication task with JDK 17 to add the standalone core, runtime, UI and frontend checker to the same Maven tree. Fabric consumer checks clear the relevant Loom remap cache before resolving a same-version input.

`package_maven_input.py` archives the local Maven tree and writes its checksum. Before running it for a release, ensure that tree contains the intended version and no unrelated historical artifacts.

## Publish to GitHub Releases

1. Check the current diff, HEAD and remote branch. Include only intended source, tests and maintained documentation; keep temporary plans, logs and review snapshots outside the commit.
2. Keep `gradle.properties`, bilingual `CHANGELOG.md`, `RELEASE_NOTES.md`, README files and target metadata consistent. Extract the complete matching version section from `RELEASE_NOTES.md` into a temporary notes file, preserving both languages.
3. Reuse valid verification only when the relevant source, dependencies, configuration and target are unchanged. Recheck current artifacts and required CI. Record game, hardware or compatibility coverage separately from offline checks; the release notes must not imply unperformed in-game validation.
4. Commit the reviewed state, push the intended branch, and create/push the matching annotated tag. Verify that the remote tag resolves to that commit before creating the release with `gh release create --verify-tag`.
5. Attach the Forge, Fabric and NeoForge installation JARs, the matching `atomlib-input-VERSION.zip`, and its `.sha256` file. The Maven archive is for developers and must not be presented as an installation mod.
6. Read back the release, tag, body and uploaded asset digests; compare them with the local verified files. If a digest is unavailable, download the asset and compare its contents. Confirm the published state before announcing completion.

The manual `release` workflow creates a draft with its selected label; it does not by itself establish that a final version has been published. Public Maven hosting and mod-store publication are separate distribution operations and are not configured by this guide.

## Host upgrades and validation scope

A consuming mod must select a compatible API/target and update any coordinate or artifact lock it maintains when it adopts a new release. Publishing AtomLib does not require editing consumer repositories or game instances.

Test real clients, GUI scales, IME, renderer combinations, resource shutdown and supported hardware as coverage becomes available, and state exactly what was tested. ARM64 packaging alone is not game validation; dedicated servers must not initialize the client rendering/native path.
