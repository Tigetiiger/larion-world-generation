# Larion → Minecraft 26.2 port plan

Unofficial, AI-assisted port of Larion World Generation (`main`, version 4.3.1, commit `8180674`) to Minecraft 26.2.
Fork: `Tigetiiger/larion-world-generation`, branch `port-26.2`. Upstream remote: `upstream`.

## Decisions

| Topic | Decision |
|---|---|
| Audience | Personal + friends. Keep mod id `larion`. Jar shared via GitHub releases on the fork. |
| Base version | Unreleased `main` (4.3.1), not the 4.3.0 Modrinth release. |
| Terrain fidelity | 1:1 with 4.3.1 on 1.21.1, plus new vanilla worldgen (sulfur caves, pale garden, new features). |
| Loaders | Fabric only. Keep the `common` + `fabric` split so NeoForge can be added later. Forge and NeoForge modules removed from the build. |
| Side | Server-side only in multiplayer (clients don't need it). Singleplayer still needs it on the client. |
| Clouds | Unchanged (vanilla 192). Players can change them with a client mod. |
| World type | Default only. Large Biomes / Amplified are unsupported (upstream #51). |
| Sulfur caves | Port 1:1, measure how often they spawn, tune only if missing or very rare. |
| Kelp lag bug (#16/#21) | Check after the 1:1 port. `main` already removed the carver tag override (the author's proposed fix). Patch only if the bug still reproduces. |
| Verification | Automated, headless: same seeds on 1.21.1 + 4.3.1 vs 26.2 + port, compared column by column. |
| Companion mods | Must work: Distant Horizons, C2ME, FerriteCore. (Noisium, Faster Random, World Preview have no 26.2 builds.) |
| Out of scope | 26.3, Disc World add-on, NeoForge/Forge, Large Biomes, terrain retuning, other upstream issues. |

## Status (2026-10-21)

Phases 0–5 done. Phases 6–7 done except the manual in-game checks. Phase 8 (commit/release) not started.

Build notes: Loom 1.19 needs Gradle 9.8+, so Loom **1.16.3** is used with Gradle 9.5. The system Java 25 on this machine is runtime-only; builds use a portable JDK 25 in `~/.local/jdks/`.

### Verification results (radius 512 blocks, 4 areas: seeds 12345, -987654321, 42, and 12345 at 20000/-15000)

| Check | Result |
|---|---|
| Terrain height | 99.18%, 99.98%, 99.94%, 99.89% of columns identical. Every difference checked in seed 12345 is inside a trial chamber (73 of 74 chunks, the last one a mineshaft next to one), so it comes from vanilla structure changes, not Larion. |
| Fluid levels | 99.98–100% identical |
| Biomes | Identical except cells that became `sulfur_caves` |
| Caves | Underground air volume within 0.6%, per-chunk correlation ≥ 0.998 |
| Speed | Port 54–70 s vs reference 59–68 s per area: no slowdown |
| Sulfur caves | Present, *more* common than vanilla (0.4–2.8% of biome cells vs 0–0.9%). Within ~200 blocks of spawn on all 3 seeds. No tuning needed. |
| Pale garden | Found on 2 of 3 seeds via `/locate` (vanilla: 3 of 3). Rare in both. |
| Kelp lag | Floating sand/gravel ≈ 9.5/chunk vs vanilla ≈ 7.2 and 1.21.1 `main` ≈ 10. The 4.3.0 cause (carver tag override) is gone in `main`. Needs a manual in-game check. |
| C2ME + FerriteCore | Output identical to plain port, 3.5× faster pregen, no errors |

### Still to do (manual)
- Join a dedicated server running the port with a client that does **not** have Larion.
- Fly over an ocean for a few minutes, then count dropped items (`/kill @e[type=item]`).
- Distant Horizons: check that distant terrain looks right.

## Phase 0 — Setup (done)

- [x] Fork cloned to `/home/artur/Projects/larion-world-generation`, branch `port-26.2`, `upstream` remote added.
- [x] README disclaimer added.
- [x] Portable JDK 21 (for the 1.21.1 reference build; Gradle 8.10 can't run on Java 25). JDK 25 is already installed system-wide.

## Phase 1 — Reference build (1.21.1)

Goal: a jar of exactly what we're porting, to compare against.

1. Build upstream `8180674` as-is with JDK 21 (`JAVA_HOME=<jdk21> ./gradlew :fabric:build`), in a separate worktree so the port branch stays clean.
2. Keep `larion-fabric-1.21.1-4.3.1.jar` outside the repo (harness folder).

## Phase 2 — Build system (Fabric only)

Use MultiLoader-Template branch `26.2` as the reference for build files.

- Java 21 → 25. Gradle wrapper 8.10 → 9.5.0. foojay resolver 0.8.0 → 1.0.0.
- Root `build.gradle`: `fabric-loom` 1.8 → `net.fabricmc.fabric-loom` 1.19.x; `net.neoforged.moddev` 2.0.78 → 2.0.148 (still used by `common` to compile against vanilla).
- `settings.gradle`: include only `common` and `fabric`. Delete `forge/` and `neoforge/` (recoverable from git).
- `gradle.properties`: `minecraft_version=26.2`, `neo_form_version=26.2-2`, `fabric_loader_version=0.20.1`, `minecraft_version_range=[26.2, 26.3)`, Fabric range `~26.2`. Remove Parchment, Forge, NeoForge properties. `version=4.3.1+26.2`.
- `fabric/build.gradle`: drop `mappings` (26.x ships unobfuscated), `modImplementation` → `implementation`, drop the refmap config.
- `common/build.gradle`: drop Parchment; mixin compile dep → `net.fabricmc:sponge-mixin:0.17.3+mixin.0.8.7`; drop MixinExtras (unused).
- Fabric API: Larion's code doesn't use it. Remove it from `depends` so a server needs only Fabric Loader. Keep it in the dev runtime only if a test mod needs it.
- `fabric.mod.json`: java `>=25`, homepage → fork URL, drop the template's `suggests` placeholder.
- Mixin configs: `compatibilityLevel` → `JAVA_25`, remove `refmap`.
- Access widener: header `accessWidener v2 named` → `accessWidener v2 official`. Remove the `makeCodec` and `NOISE_VALUE_CODEC` lines (no longer needed, see Phase 3). Mirror the same removals in `common/.../accesstransformer-common.cfg`.
- `flake.nix`: bump `jdk21` → `jdk25` and `gradle_8` → `gradle_9` (not used on this machine; keep it consistent).

Checkpoint: `./gradlew :fabric:build` compiles once Phase 3 is done.

## Phase 3 — Java changes

All 4 mixin targets still exist in 26.2 with the same signatures, so no mixin edits are needed.

| Change | Files |
|---|---|
| `DensityFunction.HOLDER_HELPER_CODEC` → `DensityFunction.CODEC` | Division, FlatDomainWarp, Signum, Sine, Sqrt |
| Replace `mapAll(Visitor)` overrides with `mapChildren(Visitor)` returning `new X(visitor.apply(child), …)`. `mapAll` is now a default method that recurses for you. | same 5 |
| `DensityFunctions.makeCodec(c)` → `KeyDispatchDataCodec.of(c)` (identical implementation) | same 5 |
| `ConditionSource.codec()` now returns `MapCodec`: `CODEC = MapCodec.unit(INSTANCE)`, return it directly | `SomewhatSteepMaterialCondition` |
| `ResourceLocation` → `Identifier`; register `SomewhatSteepMaterialCondition.CODEC` (not `.codec()`) | fabric `LarionMod` |

## Phase 4 — Data changes

1. **`data/minecraft/worldgen/noise_settings/overworld.json`**
   - Replace `noise_router.initial_density_without_jaggedness` with an exact equivalent of the 1.21.1 search (which scanned from Y 512 down in steps of 4 for density > 0.390625):
     ```json
     "preliminary_surface_level": {
       "type": "minecraft:find_top_surface",
       "density": { "type": "minecraft:add", "argument1": -0.390625, "argument2": <old initial_density_without_jaggedness> },
       "upper_bound": 512,
       "lower_bound": -128,
       "cell_height": 4
     }
     ```
   - Surface rules: 3-way merge (base = vanilla 1.21.1, ours = Larion, theirs = vanilla 26.2). Vanilla only added `sulfur_caves` rules in 3 places. A trial merge had no conflicts.
   - Leave Larion's `final_density` alone (1:1).
2. **`data/minecraft/worldgen/density_function/overworld/caves/spaghetti_2d.json`**: uses the removed `weird_scaled_sampler` and would fail to load. Rebase on vanilla 26.2 (now `interval_select`) and re-apply Larion's 3 tweaks (`xz_scale` 0.85, Y gradient -128 → 480).
3. **`data/minecraft/dimension_type/overworld.json` and `overworld_caves.json`**: copy vanilla 26.2 (new format: `attributes`, `timelines`, `default_clock`), then set `min_y: -128`, `height: 640`, `logical_height: 640`.
4. **No change needed**: carvers, the other overridden vanilla density functions (identical in 26.2), noise files, and all internal references (all 160 resolve).

Checkpoint: dev server starts, creates a Default world, no registry/codec errors in the log, terrain reaches Y 512 and caves reach Y -128.

## Phase 5 — Verification harness

Headless, scripted, kept in a `tools/verify/` folder (or outside the repo; decide when building it).

- Two dedicated servers, same 3 seeds, same area (start with a 1024×1024-block square per seed, plus one far-away area):
  - A: 1.21.1 + Fabric Loader + Fabric API + Larion 4.3.1 reference jar + Chunky 1.4.23
  - B: 26.2 + Fabric Loader + port jar + Chunky 1.5.3 (plus Fabric API if Chunky needs it)
- Pregenerate with Chunky, stop the server, then a Python script reads the region files and compares:
  - **Terrain height**: per column, the highest natural terrain block (stone, dirt, grass, sand, gravel, terracotta, snow/ice blocks…; ignoring trees and plants, since vanilla features changed). Pass: ≥ 99.5% of columns equal and every remaining difference explained. Output a diff heatmap image.
  - **Biomes**: per 4×4×4 cell. Differences are allowed only where 26.2 has a new biome (pale garden, sulfur caves) or vanilla placement changed.
  - **Water/lava levels** (oceans, aquifers, lava sea at Y -118): must match.
  - **Caves**: underground air volume per chunk. Report only (carver randomness may differ slightly).
  - **Speed**: chunks/second for A vs B. Informational; flag if B is >20% slower.

## Phase 6 — New content and known issues

1. **Sulfur caves**: from the harness, compare how often `sulfur_caves` appears in B vs plain vanilla 26.2 (same seeds). If B is under ~25% of the vanilla rate, tune the smallest thing possible and re-run Phase 5 to prove surface terrain is unchanged.
2. **Kelp lag**: pregenerate an ocean-heavy area and count dropped item entities. Compare to plain vanilla 26.2. If still bad, find the smallest fix (keep ocean floors supported) and re-run Phase 5.

## Phase 7 — Companion mods and server-only check

- **C2ME**: run B again with C2ME. Output must match plain B exactly; no errors in the log.
- **FerriteCore**: smoke test (server starts, world generates).
- **Distant Horizons**: manual client test. Distant terrain looks like the near terrain.
- **Server-only**: manual. Join the B server with a client that doesn't have Larion. Expect: joins fine, full -128..512 height renders.

## Phase 8 — Release

- README: title → "Larion World Generation — unofficial 26.2 port". Note: server-side only, Default world type, which companion mods were tested and which aren't available on 26.2. Update the outdated 4.2 note about caves not carving through dirt/sand/gravel.
- Changelog: `changelogs/4.3.1+26.2.md` (port changes, sulfur caves, any kelp fix).
- Commit, push the branch, publish a GitHub release on the fork with the jar (ask first).

## Risks

- `main` 4.3.1 was never released; its last commit is "mystery tweaks". It may contain half-finished tuning. The port reproduces it faithfully either way.
- C2ME is alpha on 26.2 and replaces how density functions run, so it is the most likely compatibility problem.
- The reference build depends on 2024-era plugins (Loom 1.8-SNAPSHOT, ForgeGradle 6) still resolving from their Maven repos. If it fails, build only the `fabric` subproject with Forge/NeoForge removed from `settings.gradle` in the reference worktree. That doesn't change the jar's contents.
