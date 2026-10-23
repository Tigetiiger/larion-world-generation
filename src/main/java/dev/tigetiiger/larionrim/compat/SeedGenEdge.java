package dev.tigetiiger.larionrim.compat;

import dev.tigetiiger.larionrim.RimSettings;
import dev.tigetiiger.larionrim.df.Edge;

/**
 * Shared state for the Distant Horizons SeedGen compat mixins. SeedGen draws far-away terrain itself, straight
 * from the seed, with one solid surface per column; these hooks give it the disc's edge: the wall, the void
 * under the rim's overhang, empty void beyond, and the sky islands.
 */
public final class SeedGenEdge {
    /** Per-thread state of the column SeedGen is currently writing. */
    public static final class Column {
        /** Everything below this DH height is void (under the rim's overhang); MIN_VALUE when unused. */
        public int clip = Integer.MIN_VALUE;
        public boolean airWritten;
        /** SeedGen's air block wrapper for the current column. */
        public Object air;
    }

    /** -Dlarionrim.lodDebug=true logs SeedGen's columns along the +X axis through the edge. */
    public static final boolean DEBUG = Boolean.getBoolean("larionrim.lodDebug");

    public static final ThreadLocal<Column> COLUMN = ThreadLocal.withInitial(Column::new);

    /** True for Larion's overworld (Y -128..512) when the addon's terrain is bound to the running world. */
    public static boolean active(int minY, int maxY) {
        return minY == -128 && maxY == 512 && Edge.lodNoise != null;
    }

    public static double rel(int x, int z) {
        return Math.sqrt((double) x * x + (double) z * z) - RimSettings.wallRadius();
    }

    private SeedGenEdge() {}
}
