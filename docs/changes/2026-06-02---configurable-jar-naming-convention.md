# Configurable jar-naming convention

■ **Created:** 2026-06-02 10:36 pm (America/Detroit)

■ **Last Updated:** 2026-06-02 10:42 pm (America/Detroit)

> Update: the **obfuscated** build now takes the clean base name and the
> **unobfuscated** build gets a `-unobf` suffix (see "Artifact names" below).

## What

Added a single-source-of-truth naming scheme so the built jar filenames — and the
`plugin.yml` / in-game version — derive automatically from four properties in
`pom.xml`. Edit them once; every build (manual or agent) follows.

```xml
<plugin.name>OBX</plugin.name>
<plugin.version>1.0.0</plugin.version>
<release.tag>beta</release.tag>          <!-- alpha | beta | rc | release | … -->
<build.number>1</build.number>
<plugin.fullversion>${plugin.version}-${release.tag}-b${build.number}</plugin.fullversion>
```

- `finalName` → `${plugin.name}-${plugin.fullversion}`.

**Artifact names** (the obfuscated build is the headline deliverable, so it takes
the clean base name; the plain build is suffixed `-unobf`):
- **`OBX-1.0.0-beta-b1.jar`** — obfuscated (ProGuard output).
- **`OBX-1.0.0-beta-b1-unobf.jar`** — unobfuscated (jar-plugin `unobf` classifier;
  ProGuard reads it as input).
- `plugin.yml` `name:`/`version:` now resolve from `${plugin.name}` /
  `${plugin.fullversion}` (resource filtering), so `/obx version` shows
  `1.0.0-beta-b1`.
- `build-info.properties` carries `version`, `releaseTag`, `buildNumber`,
  `releaseDate` for inspection / future use.

The Maven `<version>` coordinate (`1.0.0-SNAPSHOT`) is now internal plumbing only;
it no longer drives the artifact name or the plugin version.

## Files

- `pom.xml` (naming properties + `finalName`)
- `src/main/resources/plugin.yml` (`name`/`version` from properties)
- `src/main/resources/build-info.properties` (added version/tag/build fields)

## Verification

- `./maven/bin/mvn -DskipTests clean package` → **BUILD SUCCESS**; produced
  `OBX-1.0.0-beta-b1.jar` + `OBX-1.0.0-beta-b1-obfuscated.jar`.
- Confirmed inside the jar: `plugin.yml` → `version: 1.0.0-beta-b1`;
  `build-info.properties` → `version=1.0.0-beta-b1`, `releaseTag=beta`,
  `buildNumber=1`.

## How to use

To cut a new build, edit the four properties (e.g. bump `build.number` to `2`, or
flip `release.tag` to `release`) and run `./maven/bin/mvn -DskipTests clean package`.
The jars, `plugin.yml`, and `/obx version` all update together.

## Suggested Commit Message

```
Build: configurable jar naming (OBX-<ver>-<tag>-b<build>) from pom properties

Drive finalName + plugin.yml name/version + build-info from plugin.name/
plugin.version/release.tag/build.number, so one edit renames the jars and the
in-game version consistently.
```
