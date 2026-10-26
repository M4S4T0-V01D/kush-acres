# Kush Acres — expansion plan

Living progress file. Update the checkboxes as work lands; each phase ships as its own GitHub release
so the in-game updater can pick it up.

Legend: `[x]` done · `[~]` in progress · `[ ]` not started

## Requests (from the owner)
- Expand everything; maps 2–3× bigger (starting farm too) for massive farms.
- Rotate the view 4 ways to see the farm from every side.
- GitHub repo with in-game updating from the GitHub release APK.
- Realtor from level 20: buy farms in other places, 10 new farms (11 total), each with its own layout
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

## Phase 4 — Multiple farms
- [ ] Split per-farm state (tiles, land, shack, paint, layout) from player state (cash, level, stash…)
- [ ] 10 new farm sites: name, map shape/obstacles (water, rocks, roads), land position on map, price, level
- [ ] Realtor keeper (level 20+) to buy; farm switcher; offline ticking for all owned farms
- [ ] Save format v5 with a list of sites

## Phase 5 — Per-farm towns
- [ ] Each site: town name, keepers (names/looks), back-alley dealer + stock
- [ ] Strain sets per site (site-exclusive strains, like levels)

## Phase 6 — Vehicles
- [ ] Multi-tile vehicle placement (generalise shack footprint)
- [ ] RV, food truck, tractor, delivery van, … each with an effect

## Phase 7 — Workers + activities
- [ ] More helper types / per-farm crews
- [ ] More things to do (TBD while building)

## Phase 8 — Graphics
- [ ] Redraw decor at higher detail
- [ ] Overhaul ground, plants, buildings, lighting

## Release log
- 0.2.1 — before this plan
- 0.3.0 — in-game updater from GitHub releases; land grows to 24×24 (4 new tiers); Settings tab
- 0.4.0 — map 40×40 with land tiers up to 40×40; only the visible part of the farm is drawn
- 0.5.0 — rotate the farm 4 ways (↺ ↻ in the top bar)
