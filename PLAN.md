# Larion: Rimworld — design and implementation plan

Server-side Fabric addon for the unofficial Larion 26.2 port. Replaces the Disc World datapack: limits the world to a disc that ends in an ocean ring, a climate-themed wall, the void, and sparse floating islands.
("Rimworld" is a placeholder name.)

Branch `rimworld-26.2` in `Tigetiiger/larion-world-generation`, containing only this addon. Built with AI assistance (same disclaimer as the port).

## Decisions (from the design grilling, 2026-10-21)

| Topic | Decision |
|---|---|
| Side | Server-side only. Clients need it only for singleplayer or the Mod Menu screen. |
| Config | Config file sets the default; Mod Menu screen (optional, client) edits it. The value is saved into each world when it is created and never changes for that world. |
| Diameter | Default **10,000**, measured **wall to wall**. Wall at 5,000 from spawn (0,0). |
| Layout (outward) | Larion land → ocean ring (~1,000 wide) with small natural Larion islets → wall → void, with sky islands 200–2,000 blocks beyond the wall. |
| Climate | Disc World style, scaled to the diameter: frozen at the north wall, hot at the south wall, temperate middle. |
| Wall | Mountain ridge, max 50 blocks above sea level. 4 styles by climate, blended over ~200 blocks: frozen = packed/blue ice cliffs with snow; cold = dark stone with snow caps; temperate = grassy stone with moss, some trees; hot = banded sandstone/terracotta. |
| Wall back | Thin rim: ridge sits on a shelf; below sea level the disc's outer face overhangs into the void with hanging roots/dripstone. |
| Sky islands | ~1 per 1000×1000 blocks, evenly spread through the band (≈70 at default size). 50–150 blocks wide, top surface Y 80–200. Top gets the real biome for its climate (vanilla decoration). Cone-shaped underside themed by climate: frozen = ice icicle cone; cold = stone + dripstone; temperate = dirt/roots with hanging vines and glow lichen; hot = banded terracotta spike. Wet islands get a small spring off the edge. Underside shows ores (coal/iron/copper, rare diamond near the tip). |
| Structures | None generate beyond the wall. |
| Void | True void; falling kills. |
| Nether portals | Existing portals always link normally (island portals work). Only when a **new** Overworld portal would be created beyond the wall or in the ocean ring, it is placed just inside the wall instead. |
| Mod Menu | Only the diameter. Other numbers (ring width, island band, island spacing) live in the config file. |

## How it works (technical outline)

- **Per-world radius:** saved as world data on creation, loaded before chunk generation, exposed to worldgen through a custom density function type `larion_rim:disc_distance` (distance from 0,0 divided by the wall radius). All shapes are written in terms of it, so one number controls everything.
- **Terrain inside the disc:** data overrides (like Disc World): `continents` is pushed down toward ocean across the ring, with Larion's own noise allowed to poke through as small islets; `temperature/delta` is scaled to the radius.
- **Wall + void + islands:** a wrapper around Larion's final density. A copy of Larion's `final_density` is kept under a new id, and the original id becomes: Larion terrain inside the wall, a wall ridge at the wall, and outside it the sky-island function `larion_rim:sky_islands` (jittered 1000×1000 grid, one island per cell inside the band, cone shape, height from a hash). The copy must be refreshed if the Larion port's `final_density` changes.
- **No water in the void:** mixin wrapping the chunk generator's global fluid picker so it returns air beyond the wall. This wraps whatever Larion's own mixin returns, so the Larion port stays unchanged.
- **Climate styles:** a custom surface rule condition reads the climate (temperature at the column) to pick wall and island-underside materials; cone undersides use the ceiling side of surface rules. Ores and hanging decoration on undersides via a small placed feature.
- **Biomes beyond the wall:** climate there is forced to land values so islands get land biomes, never ocean (prevents monuments and ruins).
- **Structures:** mixin on structure placement that rejects starts beyond the wall.
- **Portals:** mixin on new-portal creation in the Overworld: if the chosen spot is beyond the ring's inner edge, move it radially inward to just inside the wall. Portal *search* (linking to existing portals) is untouched.

## Phases

1. **Skeleton:** Fabric mod (Java 25, Loom 1.16.3, Gradle 9.5), depends on `larion >= 4.3.1+26.2`. Config file, world data, `disc_distance` type, Mod Menu screen (Mod Menu 20.x, optional).
2. **Disc + ring:** continents/temperature overrides; ocean ring with natural islets.
3. **Void:** final density wrapper, fluid picker wrapper, biome forcing, structure blocking.
4. **Wall:** ridge shape, 4 climate styles, thin rim overhang.
5. **Sky islands:** placement, cone shape, climate undersides, ores, springs.
6. **Portals:** clamp new portals.
7. **Verification** (reuse `tools/verify` from the port):
   - With the addon, terrain inside ~3,500 blocks matches Disc World-style output; the Larion port alone is unaffected.
   - Beyond the wall: no water, no blocks except islands; island count/size/height within spec; no structures.
   - Wall height ≤ 50 above sea level everywhere; style matches climate.
   - Changing the config does not change an existing world.
   - Portal tests: random Nether point → lands inside wall; island portal → links back to the island.
   - C2ME produces identical output; Distant Horizons renders the edge (manual).
8. **Release:** README with AI disclaimer, changelog, push the branch.

## Open risks

- Wall and islands are new terrain design; expect a couple of rounds of in-game tuning (screenshots from a test client).
- Copying Larion's `final_density` ties the addon to the port version.
- Name "Rimworld" collides with an existing game title; fine for private use, rename before any public release.
