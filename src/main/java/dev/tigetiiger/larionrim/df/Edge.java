package dev.tigetiiger.larionrim.df;

import dev.tigetiiger.larionrim.IslandField;
import dev.tigetiiger.larionrim.RimSettings;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunction.FunctionContext;

/**
 * Shape of the disc's edge, as position-only pieces (see {@link EdgePart}). The final density combines them
 * with Larion's terrain L as:
 * <pre>
 *   rel &lt; AFFECT_START : L
 *   rel &gt; OUTER_FOOT   : islands
 *   otherwise          : min(max(min(L, cap), raise), cut)
 * </pre>
 * Distances are in blocks, relative to the wall radius R ("rel").
 */
public final class Edge {
    static final int SEA = 63;
    /** Larion terrain is untouched closer than this to the wall. */
    public static final double AFFECT_START = -300;
    /** Wall ridge: centre and half width. Its outer foot is at R + 16. */
    public static final double RIDGE_CENTER = -32, RIDGE_HALF = 48;
    /** Max ridge height above sea level. */
    static final double RIDGE_MAX = 50;
    /** The rim's underside rises from the world bottom here up to just below sea level at the outer foot. */
    public static final double CUT_START = -110;
    /** Width of the beach strip inside the ridge. */
    public static final double BEACH_WIDTH = 16;
    public static final double OUTER_FOOT = RIDGE_CENTER + RIDGE_HALF;

    public static double rel(FunctionContext c) {
        return radius(c) - RimSettings.wallRadius();
    }

    /** Lowers Larion's own land toward sea level over the last ~300 blocks, so only the ridge forms the skyline. */
    static double cap(FunctionContext c) {
        double rel = rel(c);
        double lower = smooth(clamp01((rel - AFFECT_START) / 125.0));
        double capY = SEA - 4 + (1 - lower) * 300;
        return (capY - c.blockY()) / 6.0;
    }

    /** Everything that adds rock: the sloping seabed fill, the beach and the ridge. */
    static double raise(FunctionContext c, DensityFunction.NoiseHolder noise) {
        double rel = rel(c);
        int x = c.blockX(), y = c.blockY(), z = c.blockZ();
        // Solid below a sloping floor (Y ~18 rising to ~40 at the beach) so no cave can connect the ocean to the
        // cut-away underside. Larion's own seabed above it is left alone.
        double d = floorWeight(rel) * (floorY(x, z, rel, noise) - y) / 6.0;
        double frozen = frozen(z);
        if (rel <= RIDGE_CENTER - RIDGE_HALF) d = Math.max(d, (beachTop(rel, frozen) - y) / 3.0);
        double top = ridgeTop(x, z, rel, frozen, noise);
        if (!Double.isNaN(top)) d = Math.max(d, (top - y) / (frozen > 0.5 ? 2.0 : 8.0));
        return d;
    }

    static double floorWeight(double rel) { return smooth(clamp01((rel + 175) / 35.0)); }

    static double floorY(int x, int z, double rel, DensityFunction.NoiseHolder noise) {
        return 18 + 22 * smooth(clamp01((rel + 140) / 50.0)) + 3 * noise.getValue(x * 0.04, 800, z * 0.04);
    }

    /** 1 on the frozen (north) side. */
    static double frozen(int z) { return clamp01((-0.55 - z / RimSettings.wallRadius()) / 0.05); }

    /** Beach along the inner foot of the ridge, then a steep drop. No beach on the frozen side. */
    static double beachTop(double rel, double frozen) {
        double foot = RIDGE_CENTER - RIDGE_HALF;
        double beachTop = rel >= foot - BEACH_WIDTH
                ? SEA + 1 + 2.5 * Math.min(1, (rel - (foot - BEACH_WIDTH)) / BEACH_WIDTH)
                : SEA + 1 - (foot - BEACH_WIDTH - rel) * 2.5;
        return beachTop - frozen * 60;
    }

    /** Top of the wall ridge, or NaN outside it. */
    static double ridgeTop(int x, int z, double rel, double frozen, DensityFunction.NoiseHolder noise) {
        double p = 1 - Math.abs(rel - RIDGE_CENTER) / RIDGE_HALF;
        if (!(p > 0)) return Double.NaN;
        double jag = 0.5 + 0.5 * x / Math.max(1, Math.sqrt((double) x * x + (double) z * z)); // east jagged, west soft
        double hmul = Math.max(0.35, Math.min(1, 0.68 + 0.38 * noise.getValue(x * 0.003, 500, z * 0.003)));
        // Soft: rounded hill. Jagged: sharper profile with ridged spikes. Ice: flat-topped wall with a sheer face.
        double ridged = 1 - Math.abs(noise.getValue(x * 0.045, 600, z * 0.045));
        double ice = smooth(clamp01(p / 0.12)) * (0.9 + 0.1 * noise.getValue(x * 0.05, 700, z * 0.05));
        // Outer half stays mountain-high and is sliced off at the outer foot: a sheer cliff into the void.
        // Only a small, rugged dip so the cut edge isn't a flat plateau.
        double ps = rel > RIDGE_CENTER ? 1 - 0.25 * clamp01((rel - RIDGE_CENTER) / (OUTER_FOOT - RIDGE_CENTER)) : p;
        double soft = ps * ps * (3 - 2 * ps);
        double jagged = Math.pow(ps, 0.6) * (0.55 + 0.6 * ridged * ridged);
        double shape = lerp(lerp(soft, jagged, jag), ice, frozen);
        return SEA + Math.min(RIDGE_MAX, (6 + (RIDGE_MAX - 6) * shape) * hmul);
    }

    /** The edge noise, captured when the density functions are bound to a world (for Distant Horizons LODs). */
    public static volatile DensityFunction.NoiseHolder lodNoise;

    /**
     * Highest solid Y of a column inside the wall, given Larion's own surface there (for Distant Horizons LODs,
     * which only know one surface per column). Matches the real terrain's top up to rounding.
     */
    public static int lodSurface(int x, int z, int larionTop) {
        DensityFunction.NoiseHolder noise = lodNoise;
        double rel = Math.sqrt((double) x * x + (double) z * z) - RimSettings.wallRadius();
        if (rel < AFFECT_START || noise == null) return larionTop;
        double lower = smooth(clamp01((rel - AFFECT_START) / 125.0));
        double top = Math.min(larionTop, Math.floor(SEA - 4 + (1 - lower) * 300));
        if (floorWeight(rel) > 0) top = Math.max(top, Math.ceil(floorY(x, z, rel, noise)) - 1);
        double frozen = frozen(z);
        if (rel <= RIDGE_CENTER - RIDGE_HALF) top = Math.max(top, Math.ceil(beachTop(rel, frozen)) - 1);
        double ridge = ridgeTop(x, z, rel, frozen, noise);
        if (!Double.isNaN(ridge)) top = Math.max(top, Math.ceil(ridge) - 1);
        return (int) top;
    }

    /** Island beyond the wall at this column for LODs: {bottomY, topY}, or null for void. */
    public static int[] lodIsland(int x, int z) {
        IslandField.Island is = IslandField.at(x, z);
        if (is == null) return null;
        double dx = x - is.x(), dz = z - is.z();
        double q = Math.sqrt(dx * dx + dz * dz) / is.edgeRadius(Math.atan2(dz, dx));
        if (q >= 1) return null;
        DensityFunction.NoiseHolder noise = lodNoise;
        double n = noise == null ? 0 : noise.getValue(x * 0.08, 300, z * 0.08);
        int top = (int) Math.floor(is.topY() + 4 * (1 - q * q) + 2 * n);
        int bottom = (int) Math.ceil(is.topY() - 2 - is.depth() * Math.pow(1 - q, 1.6));
        return bottom < top ? new int[]{bottom, top} : null;
    }

    /** Rim underside: below this height everything is cut away, giving an overhang into the void. */
    static double cut(FunctionContext c, DensityFunction.NoiseHolder noise) {
        double rel = rel(c);
        if (rel <= CUT_START) return 1000;
        double s = Math.min(1, (rel - CUT_START) / (OUTER_FOOT - CUT_START));
        double h = -128 + (SEA - 14 + 128) * Math.pow(s, 0.45) + 6 * noise.getValue(c.blockX() * 0.05, 100, c.blockZ() * 0.05);
        return (c.blockY() - h) / 6.0;
    }

    /** Sparse floating islands in a band beyond the wall (placement from IslandField). */
    static double islands(FunctionContext c, DensityFunction.NoiseHolder noise) {
        int x = c.blockX(), z = c.blockZ(), y = c.blockY();
        IslandField.Island is = IslandField.at(x, z);
        if (is == null) return -1;
        if (y > is.topY() + 12 || y < is.topY() - is.depth() - 12) return -1;
        double dx = x - is.x(), dz = z - is.z();
        double q = Math.sqrt(dx * dx + dz * dz) / is.edgeRadius(Math.atan2(dz, dx));
        if (q >= 1) return -1;
        double top = is.topY() + 4 * (1 - q * q) + 2 * noise.getValue(x * 0.08, 300, z * 0.08);
        // Cone underneath; noise makes it craggy, rock crags jut out of it.
        double bottom = is.topY() - 2 - is.depth() * Math.pow(1 - q, 1.6)
                + 4 * noise.getValue(x * 0.1, y * 0.1 + 400, z * 0.1)
                - 6 * IslandField.underRock(x, y, z);
        return Math.max(-1, Math.min(1, Math.min(top - y, y - bottom) / 4.0));
    }

    /** Rim underside height without noise (used to keep fluids out of the void under the overhang). */
    public static double cutHeight(double rel) {
        if (rel <= CUT_START) return -128;
        double s = Math.min(1, (rel - CUT_START) / (OUTER_FOOT - CUT_START));
        return -128 + (SEA - 14 + 128) * Math.pow(s, 0.45);
    }

    /** True if nothing but void and sky islands exists at this column. */
    public static boolean beyondWall(int x, int z) {
        return Math.sqrt((double) x * x + (double) z * z) - RimSettings.wallRadius() > OUTER_FOOT;
    }

    /** The addon only touches Larion's overworld (min Y -128, height 640). */
    public static boolean isLarionOverworld(net.minecraft.world.level.levelgen.NoiseGeneratorSettings s) {
        return s.noiseSettings().minY() == -128 && s.noiseSettings().height() == 640;
    }

    static double clamp01(double v) { return v < 0 ? 0 : v > 1 ? 1 : v; }
    static double smooth(double t) { return t * t * (3 - 2 * t); }
    static double lerp(double a, double b, double t) { return a + (b - a) * t; }

    private static double radius(FunctionContext c) {
        return Math.sqrt((double) c.blockX() * c.blockX() + (double) c.blockZ() * c.blockZ());
    }

    private Edge() {}
}
