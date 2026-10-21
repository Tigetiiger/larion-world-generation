# Verification harness

Scripts used to prove the 26.2 port generates the same terrain as Larion 4.3.1 on 1.21.1.
Server folders, jars and worlds live outside the repo (they were in `~/Projects/larion-verify`).

Layout expected next to the scripts:
- `ref-1.21.1/` — Fabric 1.21.1 server, mods: Larion 4.3.1 (built from upstream `8180674`), Fabric API, Chunky
- `port-26.2/` — Fabric 26.2 server, mods: this port, Fabric API, Chunky
- `vanilla-26.2/` — same as `port-26.2` without Larion
- `port-c2me/` — `port-26.2` plus C2ME and FerriteCore

Scripts:
- `batch.sh` — pregenerates 4 test areas on each server and runs `compare.py` (`RADIUS`, `VANILLA=1` env vars)
- `pregen.py` — fresh world + Chunky pregen + clean stop
- `compare.py` — column-by-column terrain height, fluid level, biome and cave comparison + diff heatmap
- `diag.py` — lists structures in chunks whose terrain differs
- `biomecount.py`, `locate.py` — sulfur caves / pale garden frequency
- `floating.py`, `items.py` — kelp-lag indicators (floating sand/gravel, dropped items)

Run the helper scripts with `python3` from this folder (they import `compare.py`).
