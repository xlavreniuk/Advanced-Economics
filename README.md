# Advanced Economics

Simple Fabric server-side economy mod.

## Mod Image

Put your mod image at `src/main/resources/assets/advanced-economics/icon.png`.
Use a square PNG, for example `128x128`.

## Commands

- `/sell` sells one supported item from your main hand.
- `/sell <count>` sells that many supported items from your main hand.
- `/sell all` sells all supported items from your main hand stack.
- `/sell inventory` sells every supported item in your inventory.
- `/buy dirt`, `/buy dirst`, or `/buy minecraft:dirt` buys one dirt.
- `/buy <item> <count>` buys that many items.
- `/buy <item> all` buys as many as you can afford and fit in your inventory.
- `/buy stone` or `/buy minecraft:stone` buys one stone.
- `/buy log`, `/buy oak_log`, or `/buy minecraft:oak_log` buys one oak log.
- `/buy planks`, `/buy stick`, `/buy coal`, `/buy diamond`, and many other common item names are supported.
- `/buy iron_nugget` or `/buy minecraft:iron_nugget` buys one iron nugget.
- `/buy iron`, `/buy iron_ingot`, or `/buy minecraft:iron_ingot` buys one iron ingot.
- `/buy gold_nugget` or `/buy minecraft:gold_nugget` buys one gold nugget.
- `/buy gold`, `/buy gold_ingot`, or `/buy minecraft:gold_ingot` buys one gold ingot.
- `/money` shows your current balance.
- `/money send <player> <amount>` sends money to another player.
- `/money send <player> all` sends your whole balance to another player.
- `/money help` shows the command list.
- `/price` shows buy and sell prices for the item in your main hand.
- `/prices` shows all configured sell and buy prices.
- `/invest <amount>` moves money into an investment deposit.
- `/invest all` invests your whole money balance.
- `/invest balance` shows your investment balance.
- `/invest withdraw` withdraws the whole investment balance.
- `/ludka` has a 40% win chance. On win, the held item or stack is doubled and overflow goes to another slot or drops nearby. On loss, the held item or stack disappears. If the lost item has a configured price, you get 5% cashback from its sell value. Unpriced items give `$0.01` cashback.

Selling prices are loaded from `config/advanced-economics-prices.txt`.
The mod creates this file the first time the server starts:

```txt
# Advanced Economics sell prices.
# Edit these values, then restart the server.
# Buy prices are always 3x the sell price.
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

Only list sell prices in this file. Buy prices are always calculated as three times the sell price.
The generated file includes 70+ common entries for logs, planks, stone variants, ores, ingots, gems, food, mob drops, basic blocks, and common pickaxes.

Balances are stored in memory for now, so they reset when the server restarts.
Investment deposits are also stored in memory for now. Deposits grow by 1% at the end of each Minecraft day.

## Build

Install Gradle or open this folder as a Gradle project in IntelliJ IDEA, then run:

```sh
gradle build
```

The mod jar will be created in `build/libs/`.

This project targets Minecraft `26.1.2`, Fabric Loader `0.19.2`, and Fabric API `0.150.0+26.1.2`.
Minecraft `26.1.2` needs Java `25` for mod development.

## License

Advanced Economics is licensed under the Business Source License 1.1 (`BUSL-1.1`).
Copyright (c) Andrii Lavreniuk.

You may use, modify, compile, fork, distribute, and run this mod free of charge for personal, educational, community, server, and non-commercial purposes. Monetized YouTube videos, Twitch streams, reviews, tutorials, and gameplay content featuring the mod are permitted. Free public and private modpacks are permitted.

Commercial use requires prior written permission from the copyright holder. Commercial use includes selling the mod, selling modified versions, selling access to the mod, redistributing it as part of a paid product, bundling it into commercial software or services, using substantial portions of the source code in a commercial product, or offering the mod or modified versions as a paid service.

All copies and forks must retain copyright notices and this license.

On `2030-01-01`, the license changes to `GPL-3.0-or-later`.

See [LICENSE](LICENSE) for the full license text and [LICENSE.md](LICENSE.md) for a short summary.
