# Kush Acres — expansion plan

Living progress file. Update the checkboxes as work lands; each phase ships as its own GitHub release
so the in-game updater can pick it up.

Legend: `[x]` done · `[~]` in progress · `[ ]` not started

## Requests (from the owner)
- Expand everything; maps 2–3× bigger (starting farm too) for massive farms.
- Rotate the view 4 ways to see the farm from every side.
- GitHub repo with in-game updating from the GitHub release APK.
- Realtor from level 15: buy farms in other places, 10 new farms (11 total), each with its own layout
  and position on its map, its own cannabis types (like levels), switch back and forth freely.
- Each place has its own town, its own people running it, its own back-alley stuff.
- Vehicles (RV and others) to buy that sit on the lawn and do different things.
- More worker expansion, more things to do overall.
- Pixel upgrade to all decor and a graphics overhaul.

## Phase 0 — Tracking
- [x] PLAN.md (this file) + todo list

## Phase 1 — GitHub repo + in-game updater
- [x] `git init`, check nothing secret is tracked (keystore/local.properties are ignored)
- [x] Public repo `M4S4T0-V01D/kush-acres` (public so the game can download release APKs without a token)
- [x] `Updater.kt`: GET `releases/latest` → compare tag `vX.Y.Z` with versionName → download `KushAcres.apk`
      → install with PackageInstaller (INTERNET + REQUEST_INSTALL_PACKAGES)
- [x] Check on launch (at most every few hours) + "Check for updates" in Menu → Sound/Settings tab
- [x] `release.sh`: build, tag, `gh release create vX.Y.Z KushAcres.apk`
- [x] First release
- Note: updates must be signed with the same key (this PC's `.android-home` debug key). Keep it backed up.

## Phase 2 — Bigger maps (0.4.0)
- [x] Farm.MAX 16 → 40 (2.5×), with save migration (`stride` field, Save v4)
- [x] LAND tiers up to 40×40 (level 80); trophy follows Farm.MAX; test keeps tiers in line with MAX
- [x] Viewport rendering: World draws only the on-screen part (`World.view`), tiles culled with `seen()`,
      1 bitmap px per art px when zoomed out
- [ ] Check on a real phone: smooth panning/zooming at full zoom-out

## Phase 3 — 4-way rotation (0.5.0)
- [x] View transform in `World` (tile/centre/hit/pick through rotation)
- [x] Mirrored sprites on odd rotations; shack front/back art
- [x] Rotate buttons in the farm HUD, recentre on rotate, remember choice
- [ ] Check on a real phone: shack/decor/helpers look right from all 4 sides

## Phase 4 — Multiple farms (0.6.0)
- [x] Split per-farm state (`Site`: tiles, land, paint) from player state (cash, level, stash, shack level, helpers)
- [x] 10 new farm sites (`Sites.kt`): own grass/wild colours, trees or pines, lakes/rivers/coast/boulders/crater rim,
      land anchored at its own spot on the map (corner, edge or middle), price and level (15 → 58; lowered from 20 → 64 in 0.7.0)
- [x] Mona sells them from Lv 15 ("Your farms" list also in the farm chip under the HUD); switch any time;
      every owned farm keeps growing and helpers pick/cook on all of them
- [x] Boulders on your land can be broken up (cost scales with level); water keeps nearby plots wet
- [x] Save format v5 with a list of sites (v4 saves migrate into Home Acres)
- [x] Seed menu in shelves: Street / Mid / Top shelf / Legend (the ladder in quarters), Lab hybrids, Exotic seeds
- [x] Menu wood colours follow the farm you're on (`Theme`), so each farm is recognisable at a glance
- [ ] Check on a real phone: terrain look, farm switching, menu colours
- Note: roads were left out; per-site strains and towns come in phase 5.

## Desktop version (Linux / KDE)
- [x] `desktop/` module: Compose for Desktop compiles the shared model/art/ui sources as they are; the few Android
      classes they use are stand-ins in `desktop/src/main/kotlin/android*` (graphics on Skia, prefs as properties files)
- [x] Own sound mixer (Java sound), Esc = back, mouse wheel zooms, window minimise = app in background
- [x] Updater takes `KushAcres-linux.tar.gz` from the same GitHub releases and swaps the installed folder
- [x] `desktop/install.sh`: installs to `~/.local/lib/kush-acres`, icon + start-menu entry (Games)
- [x] `release.sh` attaches the Linux tarball too
- [x] Keyboard: arrows/WASD pan, +/− zoom, Q/E rotate, F11 full screen; window size and spot remembered (0.10.0)
- Note: the desktop farm is its own save (`~/.local/share/kush-acres`), separate from the phone's.

## Phase 5 — Per-farm towns
- [x] Each site: town name, keepers with their own names, lines and looks (0.9.0)
- [x] Back-alley stock per town (0.10.0)
- [x] Strain sets per site (site-exclusive strains, like levels) (0.10.0)

## Phase 6 — Vehicles
- [x] Multi-tile vehicle placement: 2×2 like the shack, movable, saved (0.10.0)
- [x] RV (Lv 18, +2 beds, +5% XP), Food Truck (22, sells cooked goods every 5 min, +10%), Tractor (26, replants
      empty plots every 10 min), Delivery Van (30, sends a ready order every 5 min) (0.10.0)

## Phase 7 — Workers + activities
- [x] Work buildings (Build → Work tab), up to 6 of each per farm, unlocked by level: Water Tower (Lv 9, keeps
      nearby plots wet), Grow House (11, +20% growth nearby), Bunkhouse (13, +2 worker beds), Trim Shed (16, +15%
      yield nearby), Drying Barn (20, facilities nearby 25% faster), Tool Shed (24, runner every 4 min instead of 6).
      A worker staffing a building doubles its effect (Tower: reach 3); spare workers water dry plots
- [x] Hire workers per farm (cap grows with Bunkhouses); they walk the farm, staying near the building they staff
- [x] Share card (Menu → Share card): picture card themed by the current farm with lifetime stats; saves to
      Pictures/KushAcres on the phone, ~/Pictures/KushAcres on desktop
- [x] Per-farm crews sharing the worker beds: Trimmer (Lv 14, +8% yield each, up to 3), Guard (17, clears bugs and
      mould, up to 2), Driver (20, vehicles 40% sooner) (0.10.0)
- [x] Weekly Cannabis Cup (Farm Book → Cup, Lv 10): a class each week, three rival growers, three entries paid in
      grams of bud, cash/Bucks/gem/XP prizes for better placings, Blue Ribbon and Cup Legend trophies (0.10.2)
- [ ] More things to do (TBD while building)

## Phase 8 — Graphics
- [x] Decor shaded at fine-pixel detail: banded light across each piece, lit top-left rims, shaded lower edges
- [x] Overhaul ground, plants, buildings, lighting: soft ground shadows under everything standing on the farm (0.10.0); decor shading, lit edges on buildings and vehicles (0.10.1); plants, trees, bushes, lamps and sprinklers shaded the same way (ground tiles keep their lit edges; day/night tint, lamp glow and fireflies were already in)

## Phase 9 — Holidays, farmers, Pot Bucks (0.8.0)
- [x] Right-click / long-press tile menu: move (stunts growing plants, by strain) or destroy plants and decor
- [x] Claim buttons in dailies and challenges update right away
- [x] More random events
- [x] Holiday events for 9 holidays: tokens from sales, dailies, challenges and events; a holiday farm, permanent strains, decor and a pet each (Halloween: candy corn, black cat)
- [x] Pot Bucks currency (levels, dailies, challenges, lucky harvests) with its own shop
- [x] Farmer creator (name and looks), start spot picker on a new game, restyle for cash or Pot Bucks, restart game
- [x] Move a farm to a new spot of the same size for a fee
- [x] Vivid per-farm strains and crosses; hippie festivals (big farms, 3 h, faster money)
- [x] Share card shows a real render of the farm
- [ ] Test the creator, map picker, holiday shop and Bucks shop by hand in the running app

## Phase 10 — Power, gems, specials, reggae (0.9.0)
- [x] Settings button (⚙) in the top bar
- [x] Pot Bucks cosmetics: new decor, strains, grass and dirt colours per farm (cash or Pot Bucks)
- [x] Orders: sell now, or deliver for more after a wait
- [x] Gems: a rare third currency for 15 special buildings that boost a farm
- [x] Electricity: generators and power stations; lamps, hash presses and other machines need power
- [x] Store keeper portraits redrawn at 96 px with their shops behind them
- [x] Slow reggae loops, a different tune for home, town and every other farm
- [x] Phone top bar: smaller money numbers so they don't wrap
- [x] The camera spot and zoom are remembered per farm
- [x] Holiday tokens from dailies, challenges, jobs and hand harvests (35%, 1–3); more at higher levels; dispensary sales give half of an order
- [x] Upgrades and the shack belong to each farm; the stash adds up every farm's storage
- [ ] Listen to the area tunes and test power, deliveries and specials by hand

## Phase 11 — Weather, breeding, collection (0.10.0)
- [x] Storms besides rain; damp weather can grow mould on ripe plants (clear it from the tile menu)
- [x] Strain traits (hardy, fast, heavy, …) that hybrids inherit from their parents
- [x] Each season favours some strains: bigger harvests while they are in season
- [x] "While you were away" summary: cash earned, levels gained, plants ripe, sales and replants by helpers
- [x] Collection book marks every strain you have grown and shows its traits
- [x] Per-farm goals (Trophies tab): 100 and 1,000 harvests, both local strains, 5 work buildings, full crew, fleet; a gem and XP each
- [ ] Try weather, vehicles and crews by hand in the running app

## Release log
- 0.2.1 — before this plan
- 0.3.0 — in-game updater from GitHub releases; land grows to 24×24 (4 new tiers); Settings tab
- 0.4.0 — map 40×40 with land tiers up to 40×40; only the visible part of the farm is drawn
- 0.5.0 — rotate the farm 4 ways (↺ ↻ in the top bar, moved under it in 0.6.1)
- 0.6.0 — 10 more farms to buy from Mona at Lv 20+, each with its own land, colours and terrain; tiered seed menu
- 0.6.1 — rotate buttons moved under the top bar so the cash has room
- 0.7.0 — never tagged; its work buildings, hireable workers, farms from Lv 15 and share card shipped in 0.8.0
- 0.8.0 — holiday events (Halloween now), Pot Bucks, farmer creator, start spot and farm moving, restart, right-click / hold tile menu, hippie festivals, vivid strains and crosses, share card shows your farm
- 0.8.1 — seed shelves fold into low, mid and high level drop-downs
- 0.8.2 — Pot Bucks, holiday token and festival chips get the same wood box as the money
- 0.8.3 — Pot Bucks sit next to the money in the top bar; big amounts shortened to K, M and B
- 0.9.0 — settings button, cosmetics and grass/dirt colours, deliveries, gems and 15 specials, electricity, new keeper portraits, reggae music per area, each town has its own keepers, upgrades and shack per farm, more holiday tokens, camera remembered per farm
- 0.10.0 — desktop keys and window memory, per-town back alleys and strains, vehicles, crews, storms and mould, strain traits, seasonal favourites, away summary, collection book, per-farm goals, ground shadows
- 0.10.1 — decor redrawn with light and shade, lit edges on buildings and vehicles
- 0.10.2 — weekly Cannabis Cup; plants, trees, bushes, lamps and sprinklers shaded
