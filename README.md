# Advanced Economics

Advanced Economics is a Fabric economy mod for Minecraft `26.1.2`. It adds world-scoped money, shops, selling, item unlocks, daily quests, professions, investments, loans, paid services, gambling, insurance, and an in-game economy menu.

Each world keeps its own economy data. Player balances, investments, loans, unlocks, quests, professions, insurance, and history do not carry between worlds.

## Requirements

- Minecraft `26.1.2`
- Fabric Loader `0.19.2` or newer
- Fabric API `0.150.0+26.1.2`
- Java `25`

## In-Game Menu

Press `N` to open or close the Advanced Economics menu. The keybind is listed in Minecraft Controls under the Advanced Economics category and can be rebound.

The menu is grouped into three pages:

- `Market`: Shop, Sell, Wallet, Invest
- `Earnings`: Unlocks, Quests, Gamble
- `More`: Setting, About, Credits

The menu shows balances, prices, item unlock state, inventory counts, daily quests, profession status, server feature toggles, and a scrollable command guide in the About page.

## Main Features

- Wallet balance with money transfers
- Shop buying and inventory selling
- Item unlock progression
- Configurable sell prices
- Daily sell deal with 2x value
- Daily quests with money rewards
- Professions with sell bonuses and profession quests
- Investment deposits with daily growth
- Fixed-term deposits for 7, 14, or 30 days
- Emergency loans and repayments
- Money duels
- Item and money gambling
- Paid teleporting
- XP and money trading
- Item repair, disenchant, and rename services
- Player death insurance
- Pet selling and pet insurance
- Server feature toggles

## Commands

Use `/aehelp` or `/money help` in game for the full command list with examples.

Common commands:

- `/money` - Shows your balance.
- `/money send <player> <amount|all>` - Sends money with a 5% sender fee.
- `/sell <count|all>` - Sells the held supported item.
- `/sell <item> <count|all>` - Sells a supported item from your inventory.
- `/sell inventory` - Sells all supported inventory items.
- `/buy <item> <count|all>` - Buys unlocked items.
- `/unlock <item>` - Pays to unlock shop buying for an item.
- `/price` - Shows the held item price.
- `/invest <amount|all>` - Deposits money with a 1% fee.
- `/invest balance` - Shows investment balance.
- `/invest withdraw` - Withdraws investments with a 1% fee.
- `/termdeposit <7|14|30> <amount>` - Starts a fixed-term deposit.
- `/termdeposit claim` - Claims a matured fixed-term deposit.
- `/dailyquest` - Shows the daily quest.
- `/dailyquest claim` - Turns in quest items for money.
- `/profession choose <profession>` - Selects a profession.
- `/profession quest` - Shows profession quest work.
- `/profession quest claim` - Claims a completed profession quest.
- `/duel <player> <amount>` - Starts a money duel.
- `/duel accept` - Accepts a duel.
- `/duel deny` - Rejects a duel.
- `/gamble` - Gambles the held item stack.
- `/gamble money <amount>` - Gambles money.
- `/paidtp <player>` - Teleports to a player for `$1500.00`.
- `/effectbuy <effect>` - Buys a short potion effect.
- `/xpmoney buy <levels>` - Buys XP levels with money.
- `/xpmoney sell <levels|all>` - Sells XP levels for money.
- `/itemservice repair` - Repairs the held item.
- `/itemservice disenchant` - Removes held-item enchantments.
- `/itemservice rename <name>` - Renames the held item.
- `/loan take <amount>` - Takes an emergency loan.
- `/loan repay <amount|all>` - Repays a loan.
- `/pet sell` - Sells a nearby owned tamed pet.
- `/pet insure` - Insures a nearby owned tamed pet.
- `/insurance buy` - Reduces the next death fee.
- `/aesettings` - Shows feature toggles.
- `/aesettings toggle <feature>` - Toggles an economy feature.
- `/aegivemoney <amount>` - Creative-only command to give yourself money.
- `/aegivemoney <player> <amount>` - Creative-only command to give another player money.

Supported professions are `miner`, `lumberjack`, `farmer`, `fisherman`, and `hunter`.

## Configuration

Sell prices are loaded from:

```txt
config/advanced-economics-prices.txt
```

Feature toggles are loaded from:

```txt
config/advanced-economics-settings.json
```

The price config is generated on first server start. Values are dollars, so `0.01` means one cent. Buy prices are always calculated as four times the sell price.

Example:

```txt
dirt=0.01
stone=0.50
coal=0.20
iron_ingot=0.90
gold_ingot=0.90
diamond=5.00
```

Datapacks can override sell prices with:

```txt
data/advanced-economics/economy/prices.json
```

Example:

```json
{
  "diamond": "8.00"
}
```

## Saved Data

World economy data is saved in:

```txt
<world save>/advanced-economics/player-data.json
```

Keep this file when updating the mod if you want player economy progress to continue.

## Build

Build the mod with the Gradle wrapper:

```sh
./gradlew build
```

The compiled mod jar is created in:

```txt
build/libs/
```

Use the normal jar:

```txt
advanced-economics-fabric-26.1.2-4.0.jar
```

Do not install a `-sources.jar` as a Minecraft mod. Source jars are not playable mod jars.

## Development

Run the Fabric development client with:

```sh
./gradlew runClient
```

For small UI edits, an IDE debug run with Java HotSwap can avoid restarting Minecraft. Resource-only changes can often be reloaded in game with `F3 + T`.

## Optional Integrations

Mod Menu, JEI, REI, and EMI are listed as suggested integrations in `fabric.mod.json`. They are not required.

## License

Advanced Economics is authored by `xlvr`.

Advanced Economics is licensed under the Business Source License 1.1 (`BSL-1.1`). You may use, modify, compile, fork, distribute, and run this mod free of charge for personal, educational, community, server, and non-commercial purposes.

Monetized YouTube videos, Twitch streams, reviews, tutorials, gameplay content, and free public or private modpacks are permitted.

Commercial use requires prior written permission from the copyright holder. Commercial use includes selling the mod, selling modified versions, selling access to the mod, redistributing it as part of a paid product, bundling it into commercial software or services, using substantial portions of the source code in a commercial product, or offering the mod or modified versions as a paid service.

All copies and forks must retain copyright notices and this license.

On `2030-01-01`, the license changes to `GPL-3.0-or-later`.

See [LICENSE](LICENSE) for the full license text and [LICENSE.md](LICENSE.md) for a short summary.
