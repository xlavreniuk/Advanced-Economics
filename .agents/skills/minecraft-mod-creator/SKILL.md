---
name: minecraft-mod-creator
description: >-
  Expert skill for creating, building, and maintaining Minecraft mods with a multi-loader architecture
  (common, fabric, neoforge), UI Lib UI framework integration, and automated build versioning rules.
---

# Minecraft Mod Creator Skill

This skill provides operational procedures for building and managing this Minecraft mod project.

## 1. Subproject Architecture

- **`common/`**: Contains loader-agnostic Java code, UI Lib screens, items, blocks, and data models.
- **`fabric/`**: Contains Fabric `ModInitializer`, Fabric mixins, and `fabric.mod.json`.
- **`neoforge/`**: Contains NeoForge `@Mod` entrypoint, NeoForge event handlers, and `neoforge.mods.toml`.
- **`build/`**: Target directory where compiled jar artifacts reside after build runs.

---

## 2. Versioning & Release Workflow

### Dev Version Sequence
- Dev versions follow `0.1` -> `0.2` -> `0.3` ... up to `1.0`.
- To increment dev version:
  1. Open `gradle.properties`.
  2. Increment `mod_version` (e.g. `0.1` to `0.2`).
  3. Ensure `mod_version_type=dev`.

### Release Version Sequence ("realeasy" / "release")
- When user states "realeasy", "release", "make a release", or similar:
  1. Open `gradle.properties`.
  2. Transition `mod_version` to release format: `1.1`, `1.2`, `2.0` release.
  3. Update `mod_version_type=release`.

---

## 3. UI Lib Integration

- **Mod Page**: [UI Lib on Modrinth](https://modrinth.com/mod/ui-lib)
- **Maven Repo**: `https://api.modrinth.com/maven`
- **Gradle Dependency**: `maven.modrinth:${project.ui_lib_modrinth_slug}:${project.ui_lib_version}`
- **Common UI Class**: `com.example.advancedeconomics.ui.EconomicsUI`

When creating new user interfaces:
1. Define UI layout and components inside `common/src/main/java/com/example/advancedeconomics/ui/`.
2. Inherit/use UI Lib visual components and HUD helpers.
3. Call UI opening handlers from client networking packets or keybindings in Fabric & NeoForge.

---

## 4. Build & Verification Commands

To compile all modules and copy final jars into `build/libs/`:
```bash
./gradlew build copyArtifactsToBuildFolder
```
