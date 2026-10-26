# OSRS Survivors

A Vampire Survivors-style arena game set in Old School RuneScape, played in the browser. You
fight in real places rebuilt from the game's map. You start with bare fists and level up into
real OSRS gear, prayers and spells. Champions drop unique loot on slot-machine reels, and every
run earns coins for permanent upgrades.

Open the page and play. There's nothing to install and no account to make. Progress is kept in
your browser's local storage.

## Playing

The start menu offers:

- **New run**: pick a location. Lumbridge and the Open field are open from the start; each
  harder place opens once you survive 5:00 somewhere one difficulty below.
- **Continue**: the saved run, in the place it was saved.
- **Upgrades**: spend coins on permanent upgrades.
- **Pets**: the pets you've won from champions.

| Key | Action |
| --- | --- |
| WASD / arrow keys | Move |
| P / Esc | Pause / resume (the pause screen shows your equipment and stats) |
| 1 / 2 / 3 or click a card | Pick a level-up reward |
| R or click *Reroll* | Re-roll the offers (with the Reroll upgrade) |
| 1 or click | Stop the boss drop's reels, then claim the drop |
| R or click *Play again* | Restart after death |
| Q or *Save & exit* | Save the run and go back to the start menu |

The game pauses when the tab is hidden or loses focus, and saves every few seconds. Dying
clears the saved run. `index.html?dev` adds developer buttons (end run, +5 levels, champion
now, max all, reset all, +1000 coins).

### Gear and upgrades

- You attack by punching at first. Your first level offers a starter weapon for each style: a
  bronze scimitar, a shortbow or a staff of air.
- Each level-up offers three rewards. When possible, one is gear, one is a passive and one
  is either.
  - **Gear** follows upgrade chains for your weapon's style:
    - Melee: bronze → rune → dragon scimitar → whip → rapier → scythe.
    - Ranged: shortbow → magic shortbow → crossbows → blowpipe → twisted bow.
    - Magic: staves → tridents → sang → shadow.
    - Matching armour for each style (plate / d'hide / robes).
    - Shields for one-handed weapons.
    - Capes, amulets, gloves, boots and rings for any style.
  - Each piece appears on your character, and you attack with the weapon's real animation.
  - **Passives**:
    - Prayer chains: Burst of Strength → Piety, Sharp Eye → Rigour, Mystic Will → Augury,
      Thick → Steel Skin.
    - Other prayers: Rapid Heal and Redemption.
    - Skills: Hitpoints, Agility, Swiftness, Longrange, Multicombat.
    - Spells and items: Telekinetic Grab, Lamp of Knowledge, Vengeance, Special Attack.
  - **Food** (a shark) is sometimes offered when you're hurt.
  - **Abilities** attack on their own: cannon, thralls, Ice and Blood Barrage, chinchompas,
    god spells and a dragonfire shield. A run holds at most four.
  - More passives: Overload, Slayer (damage to champions), Thieving (extra gems), Retribution.
- **Attack speed** follows the game's ticks (0.6 s each): a scimitar swings every 4 ticks
  (2.4 s), a blowpipe every 2. Every level makes you attack a little faster, down to about half
  the starting time by level 30. Melee hits stagger monsters and melee takes 10% less damage,
  making up for fighting up close.
- **Combinations**: hold a weapon and max a matching passive, and its combined version leads
  every offer until you take it. Abyssal whip + Swiftness → Abyssal tentacle; AGS + Special
  Attack → AGS (or); Scythe + Bonecrusher → Sanguine scythe; Magic shortbow + Longrange →
  Magic shortbow (i); Dragon hunter crossbow + Rigour → DHCB (t); Ancient staff + Ice Barrage →
  Ice ancient sceptre; Trident of the swamp + Multicombat → Trident (e); Sanguinesti staff +
  Rapid Heal → Holy sanguinesti staff.
- **Sets**: Barrows, Void, Inquisitor's, plus Obsidian (stronger with the Tzhaar-ket-om),
  Crystal (stronger with the Bow of faerdhinen) and Virtus (spells freeze).
- **Champions** arrive every 3 minutes, marked by a gold ring, a name tag and an arrow at the
  edge of the screen when off-screen. Their casket spins three reels for a unique drop: coins,
  an XP lamp, a boss-only item (Infernal max cape, Amulet of rancour, Elysian spirit shield...)
  or, as the jackpot, a **pet** that stays with you in every run. Then it offers gear as before.
- **Monsters** come in fewer but tougher, so the screen stays readable. Each place has its own
  roster arriving over time. After 15 minutes they toughen quickly, so every run ends
  eventually.

### Coins and upgrades

Every run pays coins when it ends: more for time survived, kills and champions, and more in
harder places. Coins buy permanent upgrades on the main menu: Might, Armour, Vitality, Recovery,
Haste, Run energy, Magnet, Wisdom, Greed, Reroll and Revival. Each rank costs more than the last.
Pets won from champions add small bonuses too. A fully upgraded profile survives roughly two and
a half times as long as a new one.

## Build

Needs JDK 17 (this machine: `export JAVA_HOME=/c/Users/Joe/.devtools/jdk17`).

```sh
./gradlew dist          # the playable page in build/dist
./gradlew test          # headless unit tests; report in build/reports/tests/test/index.html
./gradlew exportAssets  # re-export the artwork from a local OSRS cache (see below)
```

`build/dist` is the whole game: `index.html`, `game.js` (~0.9 MB, compiled from Java by
[TeaVM](https://teavm.org)), `assets/data.js` (sprites, icons, fonts, models and animations) and
one `assets/loc/<LOCATION>.js` per map location, loaded when a run starts there. About 28 MB in
all. Open `index.html` directly (it works from `file://`) or serve the folder from any static
host.

The artwork is committed in `src/main/webapp/assets`, so building the page doesn't need the game.
`exportAssets` regenerates it from an OSRS cache. The client downloads one to
`~/.runelite/jagexcache/oldschool/LIVE`; pass another with `-PcacheDir=<dir>`. Re-export after a
game update, or after changing the exporter or its tables (new gear, NPCs, sprites or
locations), and commit the result.

Pushing to `main` publishes `build/dist` to GitHub Pages
(`.github/workflows/pages.yml`).

## How it works

| Part | Where | Notes |
| --- | --- | --- |
| Simulation | `src/main/java/.../game` | Pure Java: phases, fixed 60 Hz steps, gear, offers, spawning, saves. |
| Renderer | `src/main/java/.../render` | Draws a session through `GameAssets` with `java.awt.Graphics2D`; shapes stand in for missing art. `UiLayout` is shared by drawing and click handling. |
| Browser glue | `src/main/java/.../web` | `WebMain` (loop, input, menus, saves), `WebAssets` (exported art → sprites), `AnimModel`/`Sequence` (plays classic animation frames like the client's `Model.transform`), `Json` (Gson needs reflection). |
| AWT shim | `src/shim/java/org/teavm/classlib/java/awt/...` | `Graphics2D`, `Color`, `Font`, `BufferedImage`... drawn with Canvas2D. TeaVM uses `org.teavm.classlib.java.awt.TColor` in place of `java.awt.Color`. |
| Page | `src/main/webapp/index.html` | Decodes the bundled data (fonts, images, gzipped models), then calls the compiled `main`. |
| Exporter | `src/export/java` | Runs on the JVM with the RuneLite `cache` library. `GearArt` and `ExportTables` say which items, NPCs, animations and sprites to use; `map` decodes map squares and builds each location. |

When the renderer starts using an AWT method the shim lacks, `generateJavaScript` fails with
"Method java.awt.... was not found"; add it to the matching `T*` class.

## Limits

- Six newest bosses (Phantom Muspah, Duke Sucellus, Vardorvis, the Leviathan, the Whisperer,
  Yama) stand still: their animations are skeletal, which the browser can't play. Weapons whose
  attack is skeletal (rapier, scythe, Tumeken's shadow...) use another weapon's attack of the
  same style.
- The ground ignores height, and scenery is drawn at a fixed angle; upper floors and roofs are
  left out, and animated scenery is still.
- The player wears the default male body.
- Balance is tuned against a scripted bot (`BalanceBot`), not yet from real play.

## Credits

Old School RuneScape and its artwork, models, maps and fonts are the property of Jagex Ltd.
This is an unofficial fan project, not affiliated with or endorsed by Jagex. The code is under
the BSD-2 licence in `LICENSE`.
