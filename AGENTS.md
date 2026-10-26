# Project: OSRS Survivors

A Vampire Survivors-style arena game set in Old School RuneScape, played in the browser and
published on GitHub Pages. It must stay playable by just opening the page: no installs, no
accounts, no server. Player-facing docs and the build are in `README.md`.

(It began as a RuneLite plugin; that version is gone. Nothing here runs inside RuneLite any more.
The RuneLite `cache` and `client` libraries are only used by the exporter, at build time.)

## Environment

- No `java` on PATH. Before any gradle command: `export JAVA_HOME=/c/Users/Joe/.devtools/jdk17`
  (Git Bash). Gradle works with `--offline` once dependencies are cached.
- `./gradlew test` runs ~140 headless tests. `./gradlew dist` builds the page into `build/dist`.
- `./gradlew exportAssets` rewrites `src/main/webapp/assets` from the local OSRS cache
  (`~/.runelite/jagexcache/oldschool/LIVE`, or `-PcacheDir=`). The exported artwork is committed
  and published: re-export after changing `GearArt`, `ExportTables`, `WebExport`, the `map`
  code, or adding gear, NPCs, sprites or locations, and commit the result.
- Pushing to `main` deploys `build/dist` to GitHub Pages (`.github/workflows/pages.yml`). CI
  never exports; it only compiles `game.js` and copies the committed artwork.

## Layout

- `src/main/java` (compiled to JavaScript by TeaVM):
  - `com.osrssurvivors.game`: pure simulation. `GameSession` owns a run and the start menu
    state; phases `TITLE / RUNNING / PAUSED / LEVEL_UP / BOSS_DROP / GAME_OVER`; fixed 1/60 s
    steps.
  - `com.osrssurvivors.render`: `GameRenderer` (all drawing, falls back to shapes when
    `GameAssets` returns null) and `UiLayout` (every panel/button rectangle; shared by drawing
    and click handling, so add buttons there, never only in the renderer).
  - `com.osrssurvivors.web`: browser glue. `WebMain` (loop, input, menus, saves), `WebAssets`
    (exported models → cached sprites within a per-frame budget), `AnimModel`/`Sequence` (classic
    animation frames, like the client's `Model.transform`), `ActorData` (decodes the model blob),
    `MapArea`, `Saves`, `Json`, `Browser`.
- `src/shim/java`: Canvas2D implementations of `java.awt` classes, named
  `org.teavm.classlib.java.awt.T*`. New AWT calls in the renderer need a matching method here;
  the TeaVM build reports missing ones as "not found".
- `src/main/webapp/index.html`: decodes the bundled data (fonts, images, gzipped models) and
  calls the compiled `main`. `assets/data.js` and `assets/loc/*.js` are plain scripts so the page
  also works from `file://`.
- `src/export/java` (JVM only): `WebExport` (reads the cache, lights models, writes the assets),
  `GearArt` (gear/passive/perk/pet → item ids, animations, sprites), `ExportTables` (NPC looks,
  location icons, UI sprites, default body kits), `map` (map square/object/floor decoding,
  `AreaBuilder`/`TerrainPainter` building a 192-tile (3×3 map squares) arena per `Location`).
- Shared code must stay TeaVM-friendly: no reflection, threads or file IO in `game`/`render`/`web`.

## Game systems (where to change things)

- **Gear** (`Gear`): chains per slot × style by tier. Weapons are declared with OSRS attack
  speed in ticks (`Gear.TICK` = 0.6 s, ranged uses rapid speed); damage per hit =
  tier DPS curve (`Gear.Balance`) × cooldown, so slower weapons hit harder. `unique()` items
  (combination weapons, boss drops) are excluded from `Gear.at()`/chains and never offered as
  plain upgrades. Every new Gear needs an item id in `GearArt` (export source set; a test enforces it; weapons
  also need an attack animation, magic weapons a spell sprite).
- **Attack speed-up**: `Build.attackCooldown` multiplies by `levelHaste()` (0.977 per level,
  floor 0.45), Swiftness, the Haste perk and the Tzrek-jad pet. `GameSession.applyBuild` must
  run after levels change (it sets `Build.setLevel`).
- **Melee compensation**: melee hits stagger (`Build.MELEE_STAGGER`) and melee takes
  `Build.MELEE_GUARD` (14%) less damage. Style damage shares are in `Gear.Balance`.
- **Passives** (`Passive`): ranks with per-rank titles; `isAbility()` ones auto-attack
  (`Abilities`) and are capped at `GameSession.MAX_ABILITIES` = 4 per run. Append new enum
  values at the end (saves store names, but keep order stable anyway).
- **Combinations** (`Combination`): base weapon + passive at max rank → unique weapon, forced to
  the front of every offer while ready. Cards show the recipe; the passive's card says what it
  will make. A unique weapon gets no sidegrades (so never its own base back).
- **Offers**: other-style weapons only before `STYLE_SWITCH_MAX_LEVEL`; when upgrades run out,
  fillers fill the screen at random: potions (`Boost`, `Offer.Kind.BOOST`, doses kept in
  `Build`/`RunSave.boosts`, some capped), food and a coin pouch (`Offer.Kind.COINS`).
- **Rarity** (`ItemRarity`, `Gear.getRarity`): chain items by their place in their own
  slot × style chain, `unique()` items Legendary, `BossDrop.MEGARARES` Megarare. Display only.
- **Sets** (`GearSet`, bonuses implemented in `Build`): Barrows, Void, Inquisitor, Obsidian,
  Crystal, Virtus. Set pieces count only when not weapons.
- **Spawning** (`Spawner`): deliberately few monsters on screen (`MAX_ENEMIES` 60, slow interval
  ramp, `BATCH_GROWTH`), each tougher and worth more (`HEALTH_SCALE`, `XP_SCALE`), and overtime
  (`OVERTIME_START` 900 s) which makes monsters toughen exponentially so strong builds still end:
  health e^(t/200), contact damage that to the power `OVERTIME_DAMAGE_SHARE`, up to +60% speed.
  In overtime a full screen is refreshed (`retireStale` swaps the oldest far-off monster for a
  fresh one), and `GameSession.stallMultiplier` raises monster damage once 20 s pass without a
  kill. Without these a build too weak to kill and too tanky to die stood off forever (seen with
  sanguine scythe melee). `OvertimeTest` guards it. Location rosters/difficulty in `Location`.
- **Champions are real bosses** (`EnemyKind` entries with `isBoss()`, listed weakest first, raids
  included; `Spawner.boss` picks from a window of `BOSS_WINDOW` that moves `BOSS_STEP` with
  difficulty and champion count). A boss's
  health/speed/contact damage come from `location.newest(elapsed)`, so swapping bosses never
  changes balance. Models over `ExportTables.MAX_NPC_TILES` (at export) are shrunk.
- **XP curve**: `Progression.xpToNext` = 4 + 2.5n + 0.03n².
- **Boss drops** (`BossDrop`): champions drop a casket → phase `BOSS_DROP` → reels (timing in
  `BossDrop.STOPS`/`CLAIMABLE`, real-time via `advance`) → `claimDrop()` (first call during the
  spin only skips) → prize applied → the casket's normal gear offers. Odds: megarare 0.4% (its
  own roll first: the player's style's weapon from `MEGARARES`), then pet 8%, boss item 27%,
  lamp 33%, coins rest; unavailable outcomes fall down a rarity.
- **Meta progression**: `MetaProgress` (coins, `Perk` ranks, `Pet`s; stored as JSON by name,
  `getChanges()` tells `WebMain` when to store it). Runs pay `GameSession.coinsSoFar()` on
  death (constants `COINS_PER_*`). Perks/pets apply through `Build.setMeta/setPets`.
  `GameSession.setMeta` restarts the session, so call it before `load(save)`.
- **Location unlocks**: `Location.isUnlocked(bestTimes)`: difficulty 1 always open;
  others need `UNLOCK_SECONDS` (300) at difficulty − 1 on the map.
- **Persistence** (`web.Saves`, browser local storage): `savedRun` (RunSave JSON), `bestTimes`,
  `profile` (MetaProgress), stored as JSON through `web.Json` (Gson needs reflection).
- **Dev tools**: `index.html?dev` adds buttons: pause screen (end run, +5 levels with a reward
  each, champion now) and main menu (max all, reset all, +1000 coins, unlock maps; the unlock is
  kept in local storage until "Reset all").
- **Changelog** (`Changelog`): add a `Release` at the top for each update (its version becomes
  `Changelog.VERSION`, shown on the main menu). The "What's new" button glows until opened
  (`web.Saves` keeps the last version seen). Notes can carry a picture (gear, pet, place,
  monster, potion).
- **Pathing**: monsters walk straight only when their whole body can (`Arena.clearPath`), else
  follow `PathField`; one that barely moves while trying gets `DETOUR_SECONDS` of path-following,
  and slips round corners like the player. Wall boxes are capped at `Enemy.MAX_COLLISION_RADIUS`
  (just under a cell) so every open tile's centre fits any monster.
- **Options** (`Option`, `Options`, stored by `web.Saves`): screen flashes, level-up show
  (`LevelUpShow`), boss drop show (`DropShow`). New flashing or big motion in the renderer must
  check `ui.on(Option...)`. The options page is `GameSession.openOptions()` over the title or
  pause screen; Esc closes it.
- **Pause screen**: `UiLayout.loadoutSections/loadoutTiles` lay out tiles (abilities, prayers and
  skills, set bonuses, pets) with hover tooltips; main menu has a Pets page (`TitleScreen.PETS`).

## Balance workflow

- `BalanceTest` plays `BalanceBot` (scripted, picks rewards like a person: weapon > abilities >
  armour/prayers) through 48 seeds (from one generator) in Lumbridge, Varrock, Wilderness and
  requires style survival times within 25% of each other. Wilderness runs vary a lot (about one in
  five ends before 4:00): judge balance on 40+ seeds, never 8. `BalanceBot.play(..., meta)` accepts a profile.
- For absolute numbers, run the bot from a scratch main on the test runtime classpath (get it
  with a gradle init script printing `sourceSets.test.runtimeClasspath.asPath`). Current
  targets with a fresh profile: Lumbridge ~12.5 min, Varrock/Falador ~11, Wilderness ~6
  (Lumbridge ~1000 kills a run since the crowd was cut);
  fully upgraded ≈ 2.2× longer (Lumbridge 24–30 min, every run ending by ~30); 300–700 coins per
  run (~30 runs to max the shop). Megarares sit ~40% above the best chain weapon of their style
  (a volley shares one attack's damage, so `volley(2)` needs its own `power`).
- Watch for snowballing (levels exploding past ~100): it came from too many abilities and a
  flat XP curve.
## Testing gotchas

- `java.util.Random` with consecutive small seeds gives nearly identical first draws. When a
  test needs varied rolls, derive seeds from one generator (`new Random(seeds.nextLong())`).
- `Scenarios` (public, test source, game package) builds states that other test packages can't
  reach through package-private hooks, e.g. a session in `BOSS_DROP`.
- Renderer output can be checked by rendering a session to a PNG off-screen with
  `GameAssets.NONE` and viewing the image.
- The page can be checked headless: open `build/dist/index.html` (file:// works) in Edge with
  `--headless=new --remote-debugging-port` and drive it over the DevTools protocol. Headless
  screenshots briefly mark the page hidden, which pauses the game; override
  `document.visibilityState` to report visible in tests.
- The RuneLite `cache` library's `ModelLoader` calls `computeAnimationTables()`, which nulls
  `packedVertexGroups`. `WebExport.restoreLabels` rebuilds them; without that every model is
  exported unlabelled and nothing animates. If animations freeze again after a library update,
  check the labels in `data.js` first.

## Status (2026-10-26)

- Maps are 192 × 192 (3 × 3 map squares); Barbarian Village was removed for overlapping its
  neighbours and Al Kharid moved east. A probe of 2400 monster walks on the real maps found none
  stuck after the pathing fix (584 before).
- New this session, not yet seen by the user in the browser: the bigger maps, the changelog
  page and its first-launch highlight, raid bosses, raid pets, megarares and their reel tier,
  item rarity on cards, potion fillers, the overtime changes.

- Animations fixed (player idle/walk/attack confirmed in headless Edge). Skeletal bosses still
  stand still: Muspah, Sucellus, Vardorvis, Leviathan, Whisperer, Yama, all four Tombs bosses
  and six of the Fractured Archive's.
- Not yet confirmed by the user in the browser: OSRS tick timings and level speed-up,
  fewer/tougher monsters, champion ring/name/pointer, the slot-machine drop, pets following the
  player, combinations, sets/abilities, the Upgrades shop, coins, rerolls, revival, location
  locks, the pause screen, the Pets page, dev tools. Ask the user to check these before
  considering them done.
- Some NPC ids for monster models are best guesses (werewolf, feral vampyre, crocodile).
