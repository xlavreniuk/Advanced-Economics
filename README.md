# Advanced Economics

Fabric economy mod with shop UI, money transfers, investments, quests, professions, paid services, and risk systems.

## Mod Image

Put your mod image at `src/main/resources/image.jpg`.
Use a square PNG, for example `128x128`.
This image is also referenced by `fabric.mod.json`, so it appears as the mod preview in Minecraft and launchers that read Fabric metadata.

## Commands

- Press `N` in game to open the Advanced Economics screen. The keybind appears under the Advanced Economics category in Minecraft Controls and can be rebound like any other key.
- The first time a player joins a world with the mod, chat tells them to press `N` to open Advanced Economics.
- The screen has Minecraft-style tabs for shop, selling, wallet, investments, unlocks, quests/professions, settings, and gambling/services.
- `/sell` sells one supported item from your main hand.
- `/sell <count>` sells that many supported items from your main hand.
- `/sell all` sells all supported items from your main hand stack.
- `/sell inventory` sells every supported item in your inventory.
- `/sell <item> <count|all>` sells a chosen supported item from your inventory. The Sell tab also has per-item sell buttons.
- `/buy dirt`, `/buy dirst`, or `/buy minecraft:dirt` buys one dirt.
- `/buy <item> <count>` buys that many items.
- `/buy <item> all` buys as many as you can afford and fit in your inventory.
- `/unlock <item>` unlocks buying that item for 10x its buy price.
- `/unlocks` lists the items you can currently buy.
- `/buy stone` or `/buy minecraft:stone` buys one stone.
- `/buy log`, `/buy oak_log`, or `/buy minecraft:oak_log` buys one oak log.
- `/buy planks`, `/buy stick`, `/buy coal`, `/buy diamond`, and many other common item names are supported.
- `/buy iron_nugget` or `/buy minecraft:iron_nugget` buys one iron nugget.
- `/buy iron`, `/buy iron_ingot`, or `/buy minecraft:iron_ingot` buys one iron ingot.
- `/buy gold_nugget` or `/buy minecraft:gold_nugget` buys one gold nugget.
- `/buy gold`, `/buy gold_ingot`, or `/buy minecraft:gold_ingot` buys one gold ingot.
- `/money` shows your current balance.
- `/money send <player> <amount>` sends money to another player with a 5% sender fee.
- `/money send <player> all` sends your whole balance to another player.
- `/money help` shows the command list.
- `/price` shows buy and sell prices for the item in your main hand.
- `/invest <amount>` moves money into an investment deposit with a 1% deposit fee.
- `/invest all` invests your whole money balance.
- `/invest balance` shows your investment balance.
- `/invest withdraw` withdraws the whole investment balance with a 1% withdrawal fee.
- `/termdeposit <7|14|30> <amount>` starts a fixed-term deposit with a 1% entry fee. Use `/termdeposit claim` after maturity.
- `/loan take <amount>` takes an emergency loan up to `$5000.00`; `/loan repay <amount|all>` repays it with the included 20% interest.
- `/duel <player> <amount>` sends a duel request. If accepted with `/duel accept`, both players stake the amount and one random winner receives the full pot. `/duel deny` rejects it.
- `/paidtp <player>` teleports to another player for `$1500.00`.
- `/effectbuy <effect>` buys a level I potion effect for 1 minute. Supported effects include speed, haste, strength, jump_boost, regeneration, resistance, fire_resistance, water_breathing, night_vision, invisibility, luck, and slow_falling.
- `/dailyquest` shows the daily resource quest; `/dailyquest claim` turns in the requested items for money.
- `/dailyquest hide` closes the pinned daily quest card in the economy screen. `/dailyquest show` shows it again.
- `/aesettings` shows enabled feature groups. `/aesettings toggle <feature>` turns a group on or off, such as `shop_sell`, `gambling`, `quests`, `professions`, `investments`, or `item_services`.
- `/profession choose <profession>` chooses lumberjack, miner, farmer, fisherman, hunter, or builder. The first profession is free; changing later costs `$500.00`. Matching sales start with a 5% bonus and grow to 25%.
- `/profession quest` shows a larger profession quest; `/profession quest claim` completes it once.
- `/xpmoney buy <levels>` buys XP levels using scaling prices. `/xpmoney sell <levels|all>` converts XP levels back into money at half value.
- `/itemservice repair` repairs the held item for money. `/itemservice disenchant` removes held-item enchantments. `/itemservice rename <name>` renames the held item.
- `/pet sell` sells the nearest owned tamed pet within 8 blocks. `/pet insure` insures it for a death payout.
- `/insurance buy` buys one-use death insurance. Death normally removes 10% of wallet money only, not investments; insured death removes 2%.
- `/gamble` has a 40% win chance. On win, the held item or stack is doubled and overflow goes to another slot or drops nearby. On loss, the held item or stack disappears. If the lost item has a configured price, you get 5% cashback from its sell value. Unpriced items give `$0.01` cashback.
- One daily deal item sells for 2x each Minecraft day and is shown in the economy UI.

Selling prices are loaded from `config/advanced-economics-prices.txt`.
Feature toggles are loaded from `config/advanced-economics-settings.json`.
Fabric also checks hard dependencies from `fabric.mod.json`; if Fabric API, the target Minecraft version, Java, or Fabric Loader are missing/wrong, Minecraft states that before the mod runs.
JEI, REI, EMI, and Mod Menu are optional. They are listed as suggested integrations, not required dependencies, so the mod still runs when they are absent.
Datapacks can override sell prices by adding `data/advanced-economics/economy/prices.json` with item names mapped to dollar values, for example `{ "diamond": "8.00" }`.
The mod creates this file the first time the server starts:

```txt
# Advanced Economics sell prices.
# Edit these values, then restart the server.
# Buy prices are always 4x the sell price.
# Values are dollars, so 0.01 means one cent.
dirt=0.01
iron_nugget=0.10
iron_ingot=0.90
gold_nugget=0.10
gold_ingot=0.90
stone=0.50
oak_log=0.10
oak_planks=0.03
stick=0.01
coal=0.20
diamond=5.00
```

Only list sell prices in this file. Buy prices are always calculated as four times the sell price.
The generated file includes common entries for logs, planks, stone variants, ores, ingots, gems, food, armor, decorations, music discs, mob drops, basic blocks, and common tools.

Player money, investment deposits, loans, fixed deposits, professions, quests, insurance, unlocked items, unlock sources, gamble history, and the first-login hint state are saved per world in `<world save>/advanced-economics/player-data.json`.
Each world has its own economy state, so money earned in one world does not carry into another world.
Keep that world save file when updating the mod jar if you want player economy progress to carry over.
Investment deposits grow by 1% at the end of each Minecraft day.
A player can sell supported items at any time, but can only buy an item after it has appeared in their inventory once or after they pay `/unlock <item>`. The `N` screen syncs money, investment balance, prices, inventory counts, and locked/unlocked item states from the server.

## Build

Install Gradle or open this folder as a Gradle project in IntelliJ IDEA, then run:

```sh
gradle build
```

The mod jar will be created in `build/libs/`.

This project targets Minecraft `26.1.2`, Fabric Loader `0.19.2`, and Fabric API `0.150.0+26.1.2`.
Minecraft `26.1.2` needs Java `25` for mod development.
Gradle is configured to use a Java `25` toolchain.

## Real-time development

Use Fabric Loom's dev client instead of building a jar and manually copying it into Minecraft:

```sh
./gradlew runClient
```

That starts Minecraft directly from the project classes and resources. You still need a restart for many structural Java changes, such as adding classes, fields, methods, entrypoints, or changing mixins. For small UI code edits inside existing methods, run `runClient` from IntelliJ in debug mode and use Java HotSwap after recompiling; Minecraft can stay open if the JVM accepts the change. Resource-only changes can often be reloaded in game with `F3 + T`.

## License

Advanced Economics is authored by `xlvr`.
Advanced Economics is licensed under the Business Source License 1.1 (`BSL-1.1`).
Copyright (c) Andrii Lavreniuk.

You may use, modify, compile, fork, distribute, and run this mod free of charge for personal, educational, community, server, and non-commercial purposes. Monetized YouTube videos, Twitch streams, reviews, tutorials, and gameplay content featuring the mod are permitted. Free public and private modpacks are permitted.

Commercial use requires prior written permission from the copyright holder. Commercial use includes selling the mod, selling modified versions, selling access to the mod, redistributing it as part of a paid product, bundling it into commercial software or services, using substantial portions of the source code in a commercial product, or offering the mod or modified versions as a paid service.

All copies and forks must retain copyright notices and this license.

On `2030-01-01`, the license changes to `GPL-3.0-or-later`.

See [LICENSE](LICENSE) for the full license text and [LICENSE.md](LICENSE.md) for a short summary.
