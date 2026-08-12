# Minecraft Mod Creator - Project Memory & Directives

## Role Identity
You act as a **Minecraft Mod Creator** specialized in multi-loader mod development for Minecraft (Fabric & NeoForge) with UI Lib integration.

## Project Structure
- `common`: Shared mod logic, UI screens using UI Lib, items, blocks, recipes, shop catalog, profession logic, and common network payloads.
- `fabric`: Fabric loader entrypoints (`AdvancedEconomicsFabric`), Fabric client listeners (`EconomicsFabricClient`), command registrations, 1,400+ item dynamic scanner, and `fabric.mod.json`.
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

## Economy & Anti-Abuse System Directives
1. **Universal Item Catalog**: 100% of all 1,400+ Minecraft items are dynamically indexed from `BuiltInRegistries.ITEM` with explicit tier prices ($0.01 to $500.00) or smart property-calculated fallback prices.
2. **Shop 5-Mode Cycle Sort**: Icon-only sort button next to search box cycling `↑` (price low→high, default), `↓` (price high→low), `A` (name A-Z), `Z` (name Z-A), `#` (inventory quantity count scanning all 36 player slots).
3. **Anti-Arbitrage Protection**: `calculateSellPrice(base)` enforces a hard ceiling so total sell payout (after profession bonuses) never exceeds 80%-85% of buy price.
4. **Anti-Spam Rate Limiter**: Server-side per-player cooldown (100ms) drops macro/auto-clicker packet spam.
5. **Anti-Self Transfer**: `/ae send` blocks transferring money to oneself.
6. **Anti-XP Farming**: Profession XP per transaction is capped at 500 XP.
7. **UI Hover Cursor Rule**: All UI buttons MUST maintain `active = true` during mouse hover calculation so the system cursor NEVER shows the unavailable/cross icon. Inactive tabs render via semi-transparent dark rect overlay (`0x88000000`). Action buttons toggle `active = false` temporarily ONLY during `extractRenderState` rendering.
8. **Scrollable Views & Overhauled Settings**: Settings tab fills full height top-to-bottom with 0 top margin, categorized section headers (`Toggles` & `Multipliers`), short names, and distinct descriptions.
9. **Build Cleanliness**: `build.gradle` has a `doFirst` cleanup wiping `fabric/build/libs/` to prevent historical mod jar accumulation in Prism Launcher's mods folder.
10. **GitHub Sync**: Remote origin configured at `https://github.com/xlavreniuk/Advanced-Economics.git`.
