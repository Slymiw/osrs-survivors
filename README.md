# OSRS Survivors

A Vampire Survivors-style arena game set in Old School RuneScape, played in the browser. You
fight in real places rebuilt from the game's map. You start with bare fists and level up into
real OSRS gear, prayers and spells. Champions drop unique loot on slot-machine reels, and every
run earns coins for permanent upgrades.

Open the page and play. There's nothing to install and no account to make. Progress is kept in
your browser's local storage; a save code carries it to another device or browser.

## Playing

The start menu offers:

- **New run**: pick a location. Lumbridge is open from the start; each
  harder place opens once you survive 5:00 somewhere one difficulty below.
- **Continue**: the saved run, in the place it was saved.
- **Upgrades**: spend coins on permanent upgrades.
- **Pets**: the pets you've won from champions.
- **Options**: master, combat and interface volume, and screen flashes, the level-up and boss
  drop animations and screen shake (turn any off for a calmer screen), and full screen where
  the browser allows it. Also on the pause screen. Kept in your browser. **Report a bug** is
  there too: say what happened and send it (no account needed); the game adds its version,
  your device and screen, recent errors and, if you leave it ticked, your save code.
- **Save code (other devices)**: *Copy my code* puts your progress (coins, upgrades, pets, best
  times and saved run) on the clipboard as one line of text; on another device, *Load a code*
  and paste it. Loading asks first, then replaces that browser's progress. Options stay per device.
- **What's new**: the changelog, newest version first, in one list you scroll (mouse wheel,
  the scrollbar, or the up and down keys). After an update the button glows until you've looked.
  The version number sits beside the game's name.

| Key | Action |
| --- | --- |
| WASD / arrow keys | Move |
| P / Esc | Pause / resume (the pause screen shows your equipment and stats) |
| Click an item in the inventory | Wear it, drink a dose or eat it (works while paused too) |
| Shift-click or right-click an item | Drop it |
| Drag an item | Move it to another slot |
| I / Tab or click the backpack tab | Show or hide the inventory |
| M | Mute or unmute the sound |
| 1 / 2 / 3 or click a card | Pick a level-up reward |
| R or click *Reroll* | Re-roll the offers (with the Reroll upgrade) |
| 1 or click | Stop the boss drop's reels, then claim the drop |
| R or click *Play again* | Restart after death |
| Q or *Save & exit* | Save the run and go back to the start menu |

### Phones and tablets

The page plays on a touch screen, held upright or on its side. It lays itself out for fingers
when you touch it (and back for the mouse when you click): bigger buttons, menus that fit the
screen, and no keyboard hints.

| Touch | Action |
| --- | --- |
| Drag anywhere on open ground | Move, like a stick that appears under your finger; let go to stand still |
| Pause button (top right) | Pause; the pause screen has Resume, Options and Save & exit |
| Tap a card or button | Choose it (it happens when your finger comes up on it, so slide off to change your mind) |
| Tap the backpack tab | Show or hide the inventory |
| Tap an item | Wear it, drink a dose or eat it |
| Hold an item | See what it is (letting go then leaves it be) |
| Drag an item | Move it to another slot, or out of the inventory to drop it |
| Tap during a boss drop | Stop the reels, then claim |
| Swipe *What's new* | Scroll it; a flick keeps it gliding |
| Back button | Pause the run, or close the page you're on |

A second finger can tap the interface while the first one steers. **Full screen** is on the
options page where the browser has it (Android, iPad); on an iPhone, *Share > Add to Home Screen*
opens the game full screen like an app. Held upright, the inventory starts tucked away, and it
tucks away again on the pause screen when there's no room beside it.

The game pauses when the tab is hidden or loses focus, and saves every few seconds. Dying
clears the saved run. `index.html?dev` adds developer buttons (end run, +5 levels, champion
now, max all, reset all, +1000 coins, unlock maps).

### Gear and upgrades

- You attack by punching at first. Your first level offers a starter weapon for each style: a
  bronze scimitar, a shortbow or a staff of air.
- Each level-up offers three rewards. When possible, one is gear, one is a passive and one
  is either.
  - **Gear** follows upgrade chains for your weapon's style:
    - Melee: bronze → rune → dragon scimitar → whip → rapier → soulreaper axe (dragon claws
      strike twice).
    - Ranged: shortbow → magic shortbow → crossbows → blowpipe → Ascension crossbows.
    - Magic: staves → tridents → sang → Zorya's tome.
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
  - **Inventory**: 28 slots in the bottom right, drawn as the game's own side panel (resizable
    classic: tab stones, pillars and stone backing from the game's sprites); clicking the backpack
    tab collapses it to the stones, as in the game. Now and then a weapon for
    another style is offered for the bag; once one is there, armour and accessories for that
    style come too (one bag item a screen). Click the weapon to switch: the style's gear from the
    bag goes on with it and what it replaces goes in the bag, so one click swaps style and back.
    Prayers and potions for every style you carry are offered. Nothing is offered that the bag
    can't hold.
  - **Food** goes in the bag: sharks are sometimes offered when you're hurt, and manta rays,
    anglerfish and karambwans come as fillers. Eating takes three ticks before the next bite;
    a karambwan can be eaten straight after other food.
  - **Potions** go in the bag with four doses, once upgrades run out: divine potions for your
    styles (+15% damage), smelling salts (+10% any style), liquid adrenaline (attack 10% more
    often), Saradomin brews (+20% hitpoints), stamina (+20% speed), super restore (2 hitpoints/s),
    super antifire (15% less damage) and super defence. A dose works for a minute; drinking
    again starts the minute over. With food and coin pouches.
  - **Rarity**: every item shows how rare it is: Common, Uncommon, Rare, Epic (by its place in
    its chain), Legendary (boss drops and combinations) and Megarare.
  - **Abilities** attack on their own: cannon, thralls, Ice and Blood Barrage, chinchompas,
    god spells and a dragonfire shield. A run holds at most four.
  - More passives: Overload, Slayer (damage to champions), Thieving (extra gems), Retribution.
- **Attack speed** follows the game's ticks (0.6 s each): a scimitar swings every 4 ticks
  (2.4 s), a blowpipe every 2. Every level makes you attack a little faster, down to about half
  the starting time by level 30. Melee hits stagger monsters and melee takes 14% less damage,
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
  edge of the screen when off-screen. They're real bosses, from the Giant Mole up through the
  God Wars, the Chambers of Xeric, the Tombs of Amascut and the Fractured Archive. Their casket
  spins three reels for a unique drop: coins, an XP lamp, a boss-only item (Infernal max cape,
  Elysian spirit shield, Rondache...) or, as the jackpot, a **pet** that stays with you in every
  run (21 to collect). Rarer still, about one casket in 250 holds a **megarare** for your
  style: the Scythe of vitur or the Obligator, the Twisted bow, or Tumeken's shadow, better
  than anything else. Then it offers gear as before.
- **Healing from damage** (lifesteal, Blood Barrage) is limited: it refills by 3% of your
  hitpoints a second and holds at most 15%, so it helps a lot but can't out-heal a crowd.
- **Monsters** come in fewer but tougher, so the screen stays readable. Each place has its own
  roster arriving over time. After 15 minutes overtime begins: fresh monsters keep arriving
  ever tougher, faster and harder-hitting, and going long without a kill makes them hit harder
  still, so every run ends. No build can stand off forever.

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
./gradlew exportAssets  # re-export the artwork (and sounds) from a local OSRS cache (see below)
./gradlew exportSounds  # re-export only the sound effects; -PsoundWavs=<dir> also writes WAVs
./gradlew dist -Preadable  # game.js with its names kept, for reading stack traces
```

`build/dist` is the whole game: `index.html`, `game.js` (~0.9 MB, compiled from Java by
[TeaVM](https://teavm.org)), `assets/data.js` (sprites, icons, fonts, models and animations) and
one `assets/loc/<LOCATION>.js` per map location (192 × 192 tiles, about 10 MB each), loaded
when a run starts there, plus `assets/sounds.js` (the sound effects, about 85 KB), and `manifest.webmanifest` with
`assets/app-icon-*.png` for adding the page to a phone's home screen. About 85 MB in all. Open `index.html` directly (it works from `file://`) or serve the folder from any static
host.

The artwork is committed in `src/main/webapp/assets`, so building the page doesn't need the game.
`exportAssets` regenerates it from an OSRS cache. The client downloads one to
`~/.runelite/jagexcache/oldschool/LIVE`; pass another with `-PcacheDir=<dir>`. Re-export after a
game update, or after changing the exporter or its tables (new gear, NPCs, sprites or
locations), and commit the result.

Pushing to `main` publishes `build/dist` to GitHub Pages
(`.github/workflows/pages.yml`).

## Sound

The sound effects are the game's own. OSRS keeps them in its cache (index 4) not as recordings
but as settings for the client's synthesiser: up to ten instruments each, with pitch and volume
envelopes, vibrato, tremolo, gating, square/sine/saw/noise oscillators, an echo and a filter.
`audio.SynthSound` turns them into samples the way the client does (checked against recordings
of the same sounds on the OSRS Wiki: the timing and spectra match), in the browser, a few each
frame once the player has pressed something (browsers allow sound only after that).

Which sound plays for what is in the exporter's `SoundTable`. The meanings come from Jagex's own
names for the sound ids (listed on the OSRS Wiki's "List of sound IDs"), RuneLite's
`SoundEffectID`, and the cache's animations, some of which name their sounds (the player's
sword slash, crush, stab and spear attacks; a bow shot; a human death). Weapons sound by their
attack animation or their spell, monsters by their own hit and death sounds, and the interface
uses the game's own clicks, coins and fanfares. `exportSounds` writes the table and the sounds'
cache files (180 sounds, about 47 KB) to `assets/sounds.js`; without that file the game plays
silently.

In a run, `GameSession` records cues (attacks, hits landing, kills, pickups, level-ups...) that
`audio.SoundCues` turns into sounds, panned by where they happen. `audio.Mixer` keeps a swarm
listenable: each sound waits a moment before playing again, plays a few copies at most, and no
more than 18 sounds play at once, the important ones (level-ups, champions) taking the place of
lesser ones. There's no music: OSRS music is MIDI played through the client's own soundbank
(index 15, with Vorbis samples in index 14), which this page doesn't play yet.

## How it works

| Part | Where | Notes |
| --- | --- | --- |
| Simulation | `src/main/java/.../game` | Pure Java: phases, fixed 60 Hz steps, gear, offers, spawning, saves. Records `Cue`s for sound and effects. |
| Renderer | `src/main/java/.../render` | Draws a session through `GameAssets` with `java.awt.Graphics2D`; shapes stand in for missing art. `UiLayout` is shared by drawing and click handling. `Fx` turns cues into sparks, shockwaves, shake and notices. |
| Sound | `src/main/java/.../audio` | The OSRS synthesiser, the cue-to-sound table, the mixer's limits. Plain Java, tested on the JVM; the page plays the result with Web Audio. |
| Browser glue | `src/main/java/.../web` | `WebMain` (loop, input, menus, saves), `WebAssets` (exported art → sprites), `AnimModel`/`Sequence` (plays classic animation frames like the client's `Model.transform`), `Json` (Gson needs reflection). |
| AWT shim | `src/shim/java/org/teavm/classlib/java/awt/...` | `Graphics2D`, `Color`, `Font`, `BufferedImage`... drawn with Canvas2D. TeaVM uses `org.teavm.classlib.java.awt.TColor` in place of `java.awt.Color`. |
| Page | `src/main/webapp/index.html` | Decodes the bundled data (fonts, images, gzipped models), then calls the compiled `main`. |
| Exporter | `src/export/java` | Runs on the JVM with the RuneLite `cache` library. `GearArt` and `ExportTables` say which items, NPCs, animations and sprites to use; `map` decodes map squares and builds each location. |

When the renderer starts using an AWT method the shim lacks, `generateJavaScript` fails with
"Method java.awt.... was not found"; add it to the matching `T*` class.

## Limits

- The newest bosses stand still: Phantom Muspah, Duke Sucellus, Vardorvis, the Leviathan, the
  Whisperer, Yama, the four Tombs of Amascut bosses and most of the Fractured Archive's (Bork,
  the Smorks, Moia, Ezekiel, the Archive Guardian, Ocellus). Their animations are skeletal,
  which the browser can't play. Weapons whose
  attack is skeletal (rapier, scythe, Tumeken's shadow...) use another weapon's attack of the
  same style.
- The ground ignores height, and scenery is drawn at a fixed angle; upper floors and roofs are
  left out, and animated scenery is still.
- The player wears the default male body.
- Only worn items show their textures (the fire capes' lava flows); monsters and scenery use
  each texture's average colour.
- Balance is tuned against a scripted bot (`BalanceBot`), not yet from real play.
- No music yet (see Sound). A few sounds stand in where the game has none of its own: the
  level-up uses the "found a gem" chime (the game's level-up is a music jingle), thralls use a
  skeleton's attack, and monsters without sounds of their own use a person's.
- On very short windows (under about 400 pixels) the pause screen's stats run into its buttons.

## Credits

Old School RuneScape and its artwork, models, maps, fonts and sounds are the property of Jagex Ltd.
This is an unofficial fan project, not affiliated with or endorsed by Jagex. The code is under
the BSD-2 licence in `LICENSE`.
