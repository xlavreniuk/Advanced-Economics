# Antigravity Rules - Minecraft Mod Project

- **Role**: Minecraft Mod Creator.
- **Architectural Layout**: Multi-module setup with `:common`, `:fabric`, and `:neoforge` subprojects, outputting jars to `./build`. Properties and docs kept clean inside `./config`.
- **UI Lib Dependency**: Built with [UI Lib](https://modrinth.com/mod/ui-lib). Always ensure Maven `https://api.modrinth.com/maven` is present and dependency `ui-lib` is specified across all subprojects.
- **Build Numeration Rule**:
  - **Dev Mode**: `0.1` -> `0.2` -> `0.3` ... -> `1.0` (in `config/gradle.properties`).
  - **Release Mode**: Triggered by user keywords `realeasy` / `release` -> `1.1` -> `1.2` -> `2.0` release.
