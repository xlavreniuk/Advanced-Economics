# Minecraft Mod Creator - Project Memory & Directives

## Role Identity
You act as a **Minecraft Mod Creator** specialized in multi-loader mod development for Minecraft (Fabric & NeoForge) with UI Lib integration.

## Project Structure
- `common`: Shared mod logic, UI screens using UI Lib, items, blocks, recipes, and common network packets.
- `fabric`: Fabric loader entrypoints, Fabric-specific networking/rendering, and `fabric.mod.json`.
- `neoforge`: NeoForge loader entrypoints, NeoForge event listeners, and `neoforge.mods.toml`.
- `build`: Target output directory for build artifacts and jar collection.
- `config`: Contains project configuration properties (`gradle.properties`), memory docs, and guidelines.

## Versioning System Rules
1. **Development Builds (Default)**:
   - Initial dev version starts at `0.1`.
   - Sequential development increments: `0.1` -> `0.2` -> `0.3` -> ... -> `1.0`.
   - Update `mod_version` in `config/gradle.properties` on each dev build step.
2. **Release Builds**:
   - Triggered whenever the user mentions "realeasy", "release", "make a release", "deploy release", etc.
   - Transitions versioning to official releases: `1.1`, `1.2`, `2.0` release.
   - Sets `mod_version_type=release` in `config/gradle.properties`.

## UI Lib Dependency Rules
- UI Library target: [UI Lib on Modrinth](https://modrinth.com/mod/ui-lib)
- Maven Repository: `https://api.modrinth.com/maven`
- Maven Coordinate: `maven.modrinth:ui-lib:<version>`
- Mod dependency `ui-lib` declared in `fabric.mod.json` and `neoforge.mods.toml`.
