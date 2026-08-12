# Advanced Economics Mod (v0.46)

A powerful multi-loader Minecraft economy & profession mod built for **Fabric** & **NeoForge** (Minecraft 26.2) using **UI Lib**.

---

## 🌟 Key Features

### 🛒 1. Universal Marketplace & 100% Item Index (1,400+ Items)
- **100% Minecraft Item Indexing**: Automatically scans `BuiltInRegistries.ITEM` on startup to price every single vanilla item in Minecraft (1,400+ items: ores, crops, blocks, tools, spawn eggs, discs, trim templates, etc.).
- **$0.01 Decimal Precision**: All prices scaled down 100x with 2-decimal formatting (e.g. `Stick` **$0.01**, `Iron Ingot` **$0.25**, `Diamond` **$1.50**, `Dragon Egg` **$500.00**).
- **Search & Auto-Unlock System**: Integrated instant search bar and continuous inventory scanning that auto-unlocks gathered items.
- **5-Mode Cycle Sort Button**: Icon-only sort button next to search box cycling:
  - `↑` **Price Low → High** (Default)
  - `↓` **Price High → Low**
  - `A` **Name A → Z**
  - `Z` **Name Z → A**
  - `#` **Inventory Quantity** (Scans all 36 inventory slots; items you carry most float to the top)

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

### ⚙️ 4. Overhauled Scrollable Settings & Categorized Toggles
- **Full Top-to-Bottom Scrollable Area**: Fills entire tab height with 0 wasted margin, 5px scrollbar track, and sleek `▲` / `▼` scroll buttons.
- **Categorized Section Dividers**: Styled blue accent header strips (`— Toggles —` and `— Multipliers —`).
- **Clean Terse Names & Distinct Descriptions**:
  - `Selling` `[ON/OFF]` ➔ *Players can sell items for money*
  - `Buying` `[ON/OFF]` ➔ *Players can buy unlocked items*
  - `Unlocking` `[ON/OFF]` ➔ *Players can unlock new items*
  - `Professions` `[ON/OFF]` ➔ *Career bonuses & profession tree*
  - `XP Leveling` `[ON/OFF]` ➔ *Gain XP and level up professions*
- **Price Multipliers**: `Sell ×`, `Buy ×`, `Unlock ×` (-/+).

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

### 🖱️ 6. Clean UI Styling & Universal Cursor Protection
- **Spacious 315px Container**: Roomy, clean row layout with header tab buttons.
- **No Unavailable Cursor**: Tab header buttons and action buttons NEVER toggle `active = false` during hover checks, preventing the OS cross/unavailable cursor icon across the entire UI.
- **Inactive Tab Overlay**: Inactive tabs render using a semi-transparent dark rect overlay (`0x88000000`) with crisp white text.

---

## 🛠️ Build & Installation

```bash
# Build dev jars for Fabric and NeoForge
./gradlew build copyArtifactsToBuildFolder
```
- Outputs built jars directly to `./build/libs/`.
