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
        double R = RimSettings.wallRadius();
        // Solid below a sloping floor (Y ~18 rising to ~40 at the beach) so no cave can connect the ocean to the
        // cut-away underside. Larion's own seabed above it is left alone.
        double w = smooth(clamp01((rel + 175) / 35.0));
        double floorY = 18 + 22 * smooth(clamp01((rel + 140) / 50.0)) + 3 * noise.getValue(x * 0.04, 800, z * 0.04);
        double d = w * (floorY - y) / 6.0;

        double frozen = clamp01((-0.55 - z / R) / 0.05);                          // 1 on the frozen (north) side
        double jag = 0.5 + 0.5 * x / Math.max(1, Math.sqrt((double) x * x + (double) z * z)); // east jagged, west soft
        double hmul = Math.max(0.35, Math.min(1, 0.68 + 0.38 * noise.getValue(x * 0.003, 500, z * 0.003)));

        // Beach along the inner foot of the ridge, then a steep drop. No beach on the frozen side.
        double foot = RIDGE_CENTER - RIDGE_HALF;
        if (rel <= foot) {
            double beachTop = rel >= foot - BEACH_WIDTH
                    ? SEA + 1 + 2.5 * Math.min(1, (rel - (foot - BEACH_WIDTH)) / BEACH_WIDTH)
                    : SEA + 1 - (foot - BEACH_WIDTH - rel) * 2.5;
            beachTop -= frozen * 60;
            d = Math.max(d, (beachTop - y) / 3.0);
        }
        double p = 1 - Math.abs(rel - RIDGE_CENTER) / RIDGE_HALF;
        if (p > 0) {
            // Soft: rounded hill. Jagged: sharper profile with ridged spikes. Ice: flat-topped wall with a sheer face.
            double soft = p * p * (3 - 2 * p);
            double ridged = 1 - Math.abs(noise.getValue(x * 0.045, 600, z * 0.045));
            double jagged = Math.pow(p, 0.6) * (0.55 + 0.6 * ridged * ridged);
            double ice = smooth(clamp01(p / 0.12)) * (0.9 + 0.1 * noise.getValue(x * 0.05, 700, z * 0.05));
            double shape = lerp(lerp(soft, jagged, jag), ice, frozen);
            double top = SEA + Math.min(RIDGE_MAX, (6 + (RIDGE_MAX - 6) * shape) * hmul);
            if (rel > RIDGE_CENTER) {
                // Outer half stays mountain-high and is sliced off at the outer foot: a sheer cliff into the void.
                // Only a small, rugged dip so the cut edge isn't a flat plateau.
                double o = clamp01((rel - RIDGE_CENTER) / (OUTER_FOOT - RIDGE_CENTER));
                double ps = 1 - 0.25 * o;
                double softO = ps * ps * (3 - 2 * ps);
                double jagO = Math.pow(ps, 0.6) * (0.55 + 0.6 * ridged * ridged);
                double shapeO = lerp(lerp(softO, jagO, jag), ice, frozen);
                top = SEA + Math.min(RIDGE_MAX, (6 + (RIDGE_MAX - 6) * shapeO) * hmul);
            }
            d = Math.max(d, (top - y) / (frozen > 0.5 ? 2.0 : 8.0));
        }
        return d;
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
