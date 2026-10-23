package dev.tigetiiger.larionrim;

import com.mojang.serialization.MapCodec;
import dev.tigetiiger.larionrim.df.Edge;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SurfaceRules;

/**
 * Materials for the wall, its beach, the rim underside and the sky island cones, by climate band.
 * Runs before Larion's surface rules; returns null (falls through) everywhere else, and for island tops,
 * which keep their biome's normal surface.
 */
public enum RimSurfaceRule implements SurfaceRules.RuleSource {
    INSTANCE;

    public static final MapCodec<RimSurfaceRule> CODEC = MapCodec.unit(INSTANCE);

    public enum Style { FROZEN, COLD, TEMPERATE, HOT }

    /** Climate band from latitude (z / wall radius), with a ~200-block dithered blend between bands. */
    public static Style style(int x, int z) {
        double t = z / RimSettings.wallRadius() + (IslandField.vnoise(x, z, 48, 77) - 0.5) * 0.09;
        if (t < -0.6) return Style.FROZEN;
        if (t < -0.25) return Style.COLD;
        if (t < 0.45) return Style.TEMPERATE;
        return Style.HOT;
    }

    private static final int SEA = 63;
    private static final Block[] BANDS = {Blocks.DYED_TERRACOTTA.orange(), Blocks.TERRACOTTA, Blocks.DYED_TERRACOTTA.yellow(), Blocks.DYED_TERRACOTTA.white(),
            Blocks.DYED_TERRACOTTA.brown(), Blocks.DYED_TERRACOTTA.red(), Blocks.TERRACOTTA, Blocks.DYED_TERRACOTTA.lightGray(), Blocks.DYED_TERRACOTTA.orange()};

    @Override
    public SurfaceRules.SurfaceRule apply(SurfaceRules.Context ctx) {
        return (x, y, z) -> {
            double rel = RimShape.rel(x, z);
            if (rel < Edge.CUT_START - 20) return null;
            // Vanilla counts the surface block itself as depth 1; shift so 0 means the exposed block.
            int above = ctx.stoneDepthAbove - 1, below = ctx.stoneDepthBelow - 1;
            Style st = style(x, z);
            if (rel > Edge.OUTER_FOOT) return island(st, x, y, z, above, below);
            if (y < 40) return underside(st, x, y, z, below);
            if (rel < Edge.RIDGE_CENTER - Edge.RIDGE_HALF + 2 && y <= SEA + 4) return beach(st, y, above);
            return wall(st, x, y, z, above, below);
        };
    }

    /**
     * Wall/beach material for Distant Horizons LODs at depth {@code above} below the surface (0 = exposed top),
     * or null where the normal biome surface applies.
     */
    public static BlockState lodMaterial(int x, int y, int z, int above) {
        double rel = RimShape.rel(x, z);
        if (rel < Edge.CUT_START - 20 || rel > Edge.OUTER_FOOT) return null;
        Style st = style(x, z);
        if (rel < Edge.RIDGE_CENTER - Edge.RIDGE_HALF + 2 && y <= SEA + 4) return beach(st, y, above);
        if (y < 40) return null;
        return wall(st, x, y, z, above, 99);
    }

    /** Top / body materials of a sky island for LODs (the real island top keeps its biome's surface). */
    public static BlockState lodIslandTop(int x, int z, boolean desertBiome) {
        return switch (style(x, z)) {
            case FROZEN -> Blocks.SNOW_BLOCK.defaultBlockState();
            case HOT -> (desertBiome ? Blocks.SAND : Blocks.GRASS_BLOCK).defaultBlockState();
            default -> Blocks.GRASS_BLOCK.defaultBlockState();
        };
    }

    public static BlockState lodIslandBody(int x, int y, int z) {
        return switch (style(x, z)) {
            case FROZEN -> Blocks.PACKED_ICE.defaultBlockState();
            case COLD -> Blocks.STONE.defaultBlockState();
            case TEMPERATE -> Blocks.DIRT.defaultBlockState();
            case HOT -> band(y);
        };
    }

    private static BlockState beach(Style st, int y, int above) {
        if (above > 4) return null;
        boolean deep = above >= 2;
        return switch (st) {
            case FROZEN -> (above == 0 && y >= SEA ? Blocks.SNOW_BLOCK : Blocks.GRAVEL).defaultBlockState();
            case COLD -> Blocks.GRAVEL.defaultBlockState();
            case TEMPERATE -> (deep ? Blocks.SANDSTONE : Blocks.SAND).defaultBlockState();
            case HOT -> (deep ? Blocks.RED_SANDSTONE : Blocks.RED_SAND).defaultBlockState();
        };
    }

    private static BlockState wall(Style st, int x, int y, int z, int above, int below) {
        double r = IslandField.rnd3(x, y, z, 11);
        return switch (st) {
            case FROZEN -> (above == 0 ? Blocks.SNOW_BLOCK : r < 0.18 ? Blocks.BLUE_ICE : Blocks.PACKED_ICE).defaultBlockState();
            case COLD -> cold(x, y, z, above, r);
            case TEMPERATE -> {
                if (above == 0) yield (r < 0.3 ? Blocks.MOSS_BLOCK : Blocks.GRASS_BLOCK).defaultBlockState();
                if (above <= 3) yield Blocks.DIRT.defaultBlockState();
                yield (r < 0.08 ? Blocks.MOSSY_COBBLESTONE : Blocks.STONE).defaultBlockState();
            }
            case HOT -> above == 0 && y > SEA + 30 ? Blocks.RED_SAND.defaultBlockState() : band(y);
        };
    }

    /** 0 at the southern (temperate) edge of the cold band, 1 at its northern (frozen) edge. */
    static double frost(int z) {
        double t = z / RimSettings.wallRadius();
        return Math.max(0, Math.min(1, (-0.25 - t) / 0.35));
    }

    /** Cold: mossy cobble, stone and coarse dirt in the south, turning frosty (snow, packed ice, powder snow) to the north. */
    private static BlockState cold(int x, int y, int z, int above, double r) {
        double f = frost(z);
        double r2 = IslandField.rnd3(x, y, z, 15);
        if (above == 0) {
            if (r2 < f * 0.75 + (y > SEA + 25 ? 0.2 : 0)) return Blocks.SNOW_BLOCK.defaultBlockState();
            if (IslandField.vnoise(x, z, 9, 54) < 0.4 * (1 - f)) return Blocks.COARSE_DIRT.defaultBlockState();
        }
        if (above <= 2 && r < 0.06 * f) return Blocks.POWDER_SNOW.defaultBlockState();
        // Patches rather than speckle: moss in the south, ice in the north, cobble and stone in between.
        double patch = IslandField.vnoise(x + 0.6 * y, z - 0.4 * y, 7, 52);
        if (patch > 1 - 0.3 * f) return Blocks.PACKED_ICE.defaultBlockState();
        if (patch < 0.45 * (1 - f)) return (r2 < 0.8 ? Blocks.MOSSY_COBBLESTONE : Blocks.MOSSY_STONE_BRICKS).defaultBlockState();
        if (IslandField.vnoise(x - 0.5 * y, z + 0.7 * y, 5, 53) < 0.45) return Blocks.COBBLESTONE.defaultBlockState();
        return Blocks.STONE.defaultBlockState();
    }

    /** Ceiling of the rim's overhang (only the surface layer; the inside stays Larion stone). */
    private static BlockState underside(Style st, int x, int y, int z, int below) {
        if (below > 2) return null;
        return switch (st) {
            case FROZEN -> Blocks.PACKED_ICE.defaultBlockState();
            case COLD -> (IslandField.rnd3(x, y, z, 12) < 0.5 ? Blocks.DRIPSTONE_BLOCK : Blocks.COBBLESTONE).defaultBlockState();
            case TEMPERATE -> (below == 0 ? Blocks.ROOTED_DIRT : Blocks.DIRT).defaultBlockState();
            case HOT -> band(y);
        };
    }

    /** Island cones. Tops (within 4 blocks of the top surface) keep their biome's surface. */
    private static BlockState island(Style st, int x, int y, int z, int above, int below) {
        if (above <= 4) return null;
        double r = IslandField.rnd3(x, y, z, 13);
        if (r < 0.014) {
            // Exposed ores; the lower (deeper into the cone) the rarer.
            IslandField.Island is = IslandField.at(x, z);
            double tip = is == null ? 0 : Math.max(0, Math.min(1, (is.topY() - y) / is.depth()));
            double o = IslandField.rnd3(x, y, z, 14);
            if (tip > 0.7 && o < 0.12) return Blocks.DIAMOND_ORE.defaultBlockState();
            if (o < 0.35) return Blocks.IRON_ORE.defaultBlockState();
            if (o < 0.6) return Blocks.COPPER_ORE.defaultBlockState();
            return Blocks.COAL_ORE.defaultBlockState();
        }
        // Exposed rock patches on the underside (the craggy bits Edge pushes out of the cone).
        if (below <= 3 && IslandField.underRock(x, y, z) > 0.15) {
            double q = IslandField.rnd3(x, y, z, 16);
            return switch (st) {
                case FROZEN -> (q < 0.6 ? Blocks.STONE : Blocks.PACKED_ICE).defaultBlockState();
                case COLD -> (q < 0.5 ? Blocks.STONE : q < 0.8 ? Blocks.COBBLESTONE : Blocks.MOSSY_COBBLESTONE).defaultBlockState();
                case TEMPERATE -> (q < 0.55 ? Blocks.STONE : q < 0.85 ? Blocks.ANDESITE : Blocks.MOSSY_COBBLESTONE).defaultBlockState();
                case HOT -> (q < 0.6 ? Blocks.SANDSTONE : Blocks.SMOOTH_SANDSTONE).defaultBlockState();
            };
        }
        return switch (st) {
            case FROZEN -> (r < 0.2 ? Blocks.BLUE_ICE : Blocks.PACKED_ICE).defaultBlockState();
            case COLD -> (below <= 2 ? (r < 0.5 ? Blocks.DRIPSTONE_BLOCK : Blocks.COBBLESTONE) : Blocks.STONE).defaultBlockState();
            case TEMPERATE -> (below <= 1 ? Blocks.ROOTED_DIRT : below <= 4 ? Blocks.DIRT : Blocks.STONE).defaultBlockState();
            case HOT -> band(y);
        };
    }

    private static BlockState band(int y) {
        return BANDS[Math.floorMod(y / 3, BANDS.length)].defaultBlockState();
    }

    @Override
    public MapCodec<? extends SurfaceRules.RuleSource> codec() { return CODEC; }
}
