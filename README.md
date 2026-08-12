# Advanced Economics Mod (v0.39)

A powerful multi-loader Minecraft economy & profession mod built for **Fabric** & **NeoForge** (Minecraft 26.2) using **UI Lib**.

---

## 🌟 Key Features

### 🛒 1. Expanded Marketplace & 100x Decimal Pricing
- **85+ Item Shop Catalog**: Includes beginner resources, ores, crops, mob drops, weapons, armor, end-game artifacts (`Elytra`, `Dragon Egg`).
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

### ⚙️ 3. Scrollable Settings & Feature Toggles
- **Full Scrollable View**: Featuring sleek 6px draggable web scrollbars and 12x10px `▲` / `▼` scroll buttons.
- **Toggle Switches**:
  - `Allow Selling Items` `[ON/OFF]`
  - `Allow Buying Items` `[ON/OFF]`
  - `Allow Unlocking Items` `[ON/OFF]`
  - `Enable Professions System` `[ON/OFF]`
  - `Enable XP & Leveling` `[ON/OFF]`
- **Price Multipliers**: `Sell Multiplier`, `Buy Multiplier`, `Unlock Multiplier` (-/+).

### ⚡ 4. Unified `/ae` Command Suite
- **Standardized Syntax**: `/ae <action> <quantity/item> [player]` (defaults to current player if `[player]` is omitted):
  - `/ae` ➔ Main UI & shortcut hint
  - `/ae help` ➔ Displays help menu in chat
  - `/ae send <amount> <player>` ➔ Transfer money to online player
  - `/ae buy <item> [quantity]` ➔ Buy item(s) via chat
  - `/ae sell [item] [quantity]` ➔ Sell held or inventory item(s)
  - `/ae unlock <item>` ➔ Unlock item in shop
  - `/ae give <amount> [player]` ➔ (Admin) Give money
  - `/ae take <amount> [player]` ➔ (Admin) Take money
  - `/ae setmoney <amount> [player]` ➔ (Admin) Set money balance
  - `/ae setlevel <level> [player]` ➔ (Admin) Set profession level
  - `/ae addxp <amount> [player]` ➔ (Admin) Grant profession XP

### 🖱️ 5. Clean UI Styling & Hover Cursor Behavior
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
