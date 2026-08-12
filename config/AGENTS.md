# Minecraft Mod Creator - Project Memory & Directives

## Role Identity
You act as a **Minecraft Mod Creator** specialized in multi-loader mod development for Minecraft (Fabric & NeoForge) with UI Lib integration.

## Project Structure
- `common`: Shared mod logic, UI screens using UI Lib, items, blocks, recipes, shop catalog, profession logic, and common network payloads.
- `fabric`: Fabric loader entrypoints (`AdvancedEconomicsFabric`), Fabric client listeners (`EconomicsFabricClient`), command registrations, and `fabric.mod.json`.
- `neoforge`: NeoForge loader entrypoints, NeoForge event listeners, and `neoforge.mods.toml`.
- `build`: Target output directory for build artifacts and jar collection.
- `config`: Contains project configuration properties (`gradle.properties`), memory docs, and guidelines.

## Versioning System Rules
1. **Development Builds (Default)**:
   - Initial dev version starts at `0.1`.
   - Sequential development increments: `0.1` -> `0.2` -> `0.3` -> ... -> `1.0`.
   - Update `mod_version` in `config/gradle.properties` and `fabric.mod.json` on each dev build step.
2. **Release Builds**:
   - Triggered whenever the user mentions "realeasy", "release", "make a release", "deploy release", etc.
   - Transitions versioning to official releases: `1.1`, `1.2`, `2.0` release.
   - Sets `mod_version_type=release` in `config/gradle.properties`.

## Economy & Profession System Directives
1. **Pricing**: Base prices are double precision scaled 100x starting at `$0.01` ($0.01 to $500.00). Formatted in UI as `$0.01`, `$1.50`, etc. Cents are processed internally as `long` (`dollarAmount * 100.0`).
2. **Professions**: 5 Careers (Lumberjack, Miner, Farmer, Hunter, Weaponsmith). Grants +2% sell bonus per level on matching items, track XP on green Minecraft progress bar.
3. **Unified Commands Suite**: `/ae <action> <quantity/item> [player]` (defaults to current player if `[player]` is omitted).
4. **UI Hover Cursor Rule**: All UI buttons MUST maintain `active = true` during mouse hover calculation so the system cursor NEVER shows the unavailable/cross icon. Visually disabled buttons temporarily toggle `active = false` only during `extractRenderState` rendering pass.
5. **Scrollable Views**: Scrollable containers feature 6px draggable web scrollbars and 12x10px `▲` / `▼` scroll buttons.
