---
trigger: always_on
---

# Minecraft Mod Creation & Versioning Rule

1. **Subproject Boundaries**:
   - Shared logic in `common/`.
   - Fabric initializer & Fabric metadata in `fabric/`.
   - NeoForge `@Mod` entrypoint & NeoForge metadata in `neoforge/`.
   - Built jars directed to root `build/` directory.
   - Clean project properties & documentation in `config/` directory.

2. **Version Increments**:
   - For dev builds, track version in `config/gradle.properties` (`0.1`, `0.2`, `0.3`, ... `1.0`).
   - When the user asks for a release (using `realeasy` / `release`), update version to release series (`1.1`, `1.2`, `2.0`).

3. **UI Lib Integration**:
   - Reference `https://modrinth.com/mod/ui-lib`.
   - Use Modrinth Maven `https://api.modrinth.com/maven` for `maven.modrinth:ui-lib:<version>`.
