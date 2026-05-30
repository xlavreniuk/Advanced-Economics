# MineSeller

Simple Fabric server-side economy mod.

## Commands

- `/sell` sells one supported item from your main hand.
- `/sell <count>` sells that many supported items from your main hand.
- `/sell all` or `/sellall` sells all supported items from your main hand stack.
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
- `/money help` shows the command list.
- `/price` shows buy and sell prices for the item in your main hand.
- `/ludka` rolls a number from 0 to 99. Under 50 loses the held item or stack, 50 or higher doubles it. Non-stackable items get one extra copy on win. If the lost item has a configured price, you get 5% cashback from its sell value. Unpriced items give `$0.01` cashback.

Selling prices are loaded from `config/mineseller-prices.txt`.
The mod creates this file the first time the server starts:

```txt
# MineSeller sell prices.
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

## Build

Install Gradle or open this folder as a Gradle project in IntelliJ IDEA, then run:

```sh
gradle build
```

The mod jar will be created in `build/libs/`.

This project targets Minecraft `26.1.2`, Fabric Loader `0.19.2`, and Fabric API `0.150.0+26.1.2`.
Minecraft `26.1.2` needs Java `25` for mod development.
