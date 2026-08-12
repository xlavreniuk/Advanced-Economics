---
name: minecraft-mod-creator
description: >-
  Expert skill for creating, building, and maintaining Minecraft mods with a multi-loader architecture
  (common, fabric, neoforge), UI Lib UI framework integration, decimal economy pricing, career professions,
  and automated build versioning rules.
---

# Minecraft Mod Creator Skill

This skill provides operational procedures for building and managing this Minecraft mod project.

## 1. Subproject Architecture

- **`common/`**: Contains loader-agnostic Java code, UI Lib screens, items, blocks, shop catalog, profession career logic, and data models.
- **`fabric/`**: Contains Fabric `ModInitializer` (`AdvancedEconomicsFabric`), Fabric client handlers (`EconomicsFabricClient`), network payloads, `/ae` command suite, and `fabric.mod.json`.
- **`neoforge/`**: Contains NeoForge `@Mod` entrypoint, NeoForge event handlers, and `neoforge.mods.toml`.
- **`build/`**: Target directory where compiled jar artifacts reside after build runs.

---

## 2. Versioning & Release Workflow

### Dev Version Sequence
- Dev versions follow `0.1` -> `0.2` -> `0.3` ... up to `1.0`.
- To increment dev version:
  1. Open `config/gradle.properties` and `fabric/src/main/resources/fabric.mod.json`.
  2. Increment `mod_version` (e.g. `0.38` to `0.39`).
  3. Ensure `mod_version_type=dev`.

### Release Version Sequence ("realeasy" / "release")
- When user states "realeasy", "release", "make a release", or similar:
  1. Open `config/gradle.properties`.
  2. Transition `mod_version` to release format: `1.1`, `1.2`, `2.0` release.
  3. Update `mod_version_type=release`.

---

## 3. UI Lib & Screen Conventions

- **Hover Cursor Rule**: Always maintain `active = true` on buttons during mouse input passes so hover cursor stays normal (never shows unavailable/cross cursor).
- **Disabled Rendering**: Visually disabled buttons temporarily toggle `active = false` only during `extractRenderState` rendering pass to render Minecraft's locked button sprite texture.
- **Scrollbar Pattern**: Sleek 6px draggable track with 12x10px `▲` and `▼` scroll buttons.

---

## 4. Unified `/ae` Command Pattern

All in-game commands follow the unified pattern: `/ae <action> <quantity/item> [player]` (targeting self when `[player]` is omitted):
- `/ae send <amount> <player>`
- `/ae give <amount> [player]`
- `/ae take <amount> [player]`
- `/ae setmoney <amount> [player]`
- `/ae setlevel <level> [player]`
- `/ae addxp <amount> [player]`
- `/ae buy <item> [quantity]`
- `/ae sell [item] [quantity]`
- `/ae unlock <item>`

---

## 5. Build & Verification Commands

To compile all modules and copy final jars into `build/libs/` and Prism Launcher:
```bash
./gradlew build copyArtifactsToBuildFolder
```
