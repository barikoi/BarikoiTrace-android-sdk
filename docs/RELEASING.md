# Releasing

Distribution is **JitPack from the public GitHub repo**. A release is a git
tag — there is no Maven Central staging, no review queue, nothing to submit.
JitPack builds the tagged commit the first time someone asks for it and caches
the result.

Consumers resolve `com.github.barikoi.BarikoiTrace-android-sdk:barikoitrace:<tag>`.

## Rules

- **Tags are immutable.** Once pushed, someone's build may pin it and JitPack
  caches the built artifact against it. Never move or delete a tag — ship
  `0.4.1` instead. A moved tag means two developers get different bytes for the
  same coordinate, which is the worst failure mode this system has.
- **SemVer, continuing the existing series.** Tags run `0.2.1-beta` → `0.3.0`.
  Below `1.0.0`, treat a *minor* bump as the breaking one: bugfix → patch,
  anything else → minor, until the API is committed at `1.0.0`.
- **No `v` prefix.** Tags are `0.4.0`, not `v0.4.0` — matches the existing
  series and the README's install snippet. Stay consistent.
- **Keep `version` in `barikoitrace/build.gradle.kts` equal to the tag.**
  JitPack resolves by tag name, so a mismatch is not fatal, but it makes
  `mavenLocal` builds and the POM disagree with the coordinate people type.
- **Pre-releases** (`0.4.1-rc1`) build on JitPack like any other tag. Use them
  for internal trials; nothing auto-adopts them.
- **Version in lockstep with iOS.** Both SDKs ship the same number, so
  `0.4.0` here and the `0.4.0` tag on
  [`BarikoiTrace-ios-sdk`](https://github.com/barikoi/BarikoiTrace-ios-sdk) are
  a matched pair. Cut them together, or the pairing stops meaning anything.
- **Tell integrators to pin.** `0.3.0` and earlier are the old *Java* SDK on a
  different branch lineage, with a different API. Anyone resolving `0.+` or
  `latest.release` gets moved onto the Kotlin rewrite by this release and their
  build will not compile. An exact version, or at most `0.4.+`, is the only
  safe range.

## Checklist

1. `dev-v4` builds and the unit tests pass:
   ```bash
   ./gradlew :barikoitrace:assembleRelease :barikoitrace:testDebugUnitTest
   ```
2. `barikoitrace/build.gradle.kts` — `version` matches the tag being cut.
3. `README.md` — the install snippet references that version.
4. No secrets tracked:
   ```bash
   git ls-files | grep -iE 'secret|\.env|local\.properties|\.jks|\.keystore'
   # expect no hits — local.properties is git-ignored and holds the real values
   ```
5. Tag and push:
   ```bash
   git tag -a 0.4.0 -m "0.4.0"
   git push origin dev-v4
   git push origin 0.4.0
   ```
6. Trigger the JitPack build so the first real consumer doesn't wait on it:
   open `https://jitpack.io/#barikoi/BarikoiTrace-android-sdk/0.4.0` and check
   the log turns green. A red build there is a broken release even though the
   tag pushed fine — fix forward on a new patch tag.

## Verifying as a consumer

```bash
mkdir /tmp/probe && cd /tmp/probe
gradle init --type basic
```

Add to `settings.gradle`:

```groovy
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

and to `build.gradle`:

```groovy
dependencies { implementation 'com.github.barikoi.BarikoiTrace-android-sdk:barikoitrace:0.4.0' }
```

then `gradle dependencies --configuration runtimeClasspath`. Resolving from a
clean Gradle cache (`--refresh-dependencies`) is the only way to know the
artifact really published, rather than being served from your machine.

## Not doing

- **Maven Central** — worth it only when an enterprise consumer forbids
  JitPack. It needs a Sonatype account, GPG signing, `sources`/`javadoc` jars,
  and a staging-and-release dance on every version.
- **AAR checked into the repo** — JitPack builds from source; a committed
  binary would go stale silently and doubles the review surface.
