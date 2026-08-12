# Advanced Economics Mod (v0.42)

A powerful multi-loader Minecraft economy & profession mod built for **Fabric** & **NeoForge** (Minecraft 26.2) using **UI Lib**.

---

## 🌟 Key Features

### 🛒 1. Universal Marketplace & 100% Item Index (1,400+ Items)
- **100% Minecraft Item Indexing**: Automatically scans `BuiltInRegistries.ITEM` on startup to price every single vanilla item in Minecraft (1,400+ items: ores, crops, blocks, tools, spawn eggs, discs, trim templates, etc.).
- **$0.01 Decimal Precision**: All prices scaled down 100x with 2-decimal formatting (e.g. `Stick` **$0.01**, `Iron Ingot` **$0.25**, `Diamond` **$1.50**, `Dragon Egg` **$500.00**).
- **Search & Auto-Unlock System**: Integrated instant search bar and continuous inventory scanning that auto-unlocks gathered items.

### 🪓 2. Profession Career System & Level Progression
- **5 Careers**:
  - 🪵 **Lumberjack** (`Oak Log` icon) -> Wood & forestry
  - ⛏️ **Miner** (`Iron Pickaxe` icon) -> Stones & ores
  - 🌾 **Farmer** (`Wheat` icon) -> Crops & food
  - 🏹 **Hunter** (`Leather` icon) -> Mob drops & meats
  - ⚔️ **Weaponsmith** (`Iron Sword` icon) -> Weapons & armor
- **Level Bonuses**: **+2% Sell Price Bonus** per level on matching category items.
- **Green XP Progress Bar**: Minecraft-style XP progress bar placed neatly in the Profession tab header (`XP: 350 / 500`).

### 🛡️ 3. Anti-Abuse & Anti-Arbitrage Protection Suite
- **Anti-Arbitrage Price Ceiling**: Sell prices are capped at a maximum of **80% of buy prices** to ensure infinite money duplication loops are mathematically impossible.
- **100ms Packet Transaction Rate Limiter**: Server-side per-player cooldown (100ms) drops macro/auto-clicker packet spam.
- **Anti-Self Transfer Check**: Prevents sending money to oneself via `/ae send`.
- **Anti-XP Exploit Cap**: Caps maximum XP gain to 500 XP per transaction to prevent XP farming glitches.

### ⚙️ 4. Scrollable Settings & Feature Toggles
- **Full Scrollable View**: Featuring sleek 6px draggable web scrollbars and 12x10px `▲` / `▼` scroll buttons.
- **Toggle Switches**:
  - `Allow Selling Items` `[ON/OFF]`
  - `Allow Buying Items` `[ON/OFF]`
  - `Allow Unlocking Items` `[ON/OFF]`
  - `Enable Professions System` `[ON/OFF]`
  - `Enable XP & Leveling` `[ON/OFF]`
- **Price Multipliers**: `Sell Multiplier`, `Buy Multiplier`, `Unlock Multiplier` (-/+).

### ⚡ 5. Unified `/ae` Command Suite
- **Standardized Syntax**: `/ae <action> <quantity/item> [player]` (defaults to current player if `[player]` is omitted):
  - `/ae` ➔ Main UI & shortcut hint
  - `/ae help` ➔ Displays help menu in chat
  - `/ae send <amount> <player>` ➔ Transfer money to online player (with self-transfer check)
  - `/ae buy <item> [quantity]` ➔ Buy item(s) via chat
  - `/ae sell [item] [quantity]` ➔ Sell held or inventory item(s)
  - `/ae unlock <item>` ➔ Unlock item in shop
  - `/ae give <amount> [player]` ➔ (Admin) Give money
  - `/ae take <amount> [player]` ➔ (Admin) Take money
  - `/ae setmoney <amount> [player]` ➔ (Admin) Set money balance
  - `/ae setlevel <level> [player]` ➔ (Admin) Set profession level
  - `/ae addxp <amount> [player]` ➔ (Admin) Grant profession XP

### 🖱️ 6. Clean UI Styling & Hover Cursor Behavior
- **Spacious 315px Container**: Roomy, clean row layout with header tab buttons.
- **Normal Hover Cursor**: Hovering over any button (active or disabled) maintains a normal mouse cursor (never shows the unavailable/cross cursor).
- **Locked Button Sprite**: Visually disabled buttons render using Minecraft's official locked button texture sprite.

---

## 🛠️ Build & Installation

```bash
# Build dev jars for Fabric and NeoForge
./gradlew build copyArtifactsToBuildFolder
```
- Outputs built jars directly to `./build/libs/`.
