package dev.tigetiiger.larionrim;

/**
 * The disc's outline. The wall is round overall, but its distance from 0,0 drifts in and out with the angle
 * (long swells plus small kinks); the mainland coast (inner edge of the ocean ring) has its own, larger drift.
 * Both are seeded per world and scale with the radius. Worlds created before this existed keep the exact circle.
 */
public final class RimShape {
    /** Newest edge shape; worlds saved without one use {@link #CIRCLE}. */
    public static final int CIRCLE = 1, WOBBLY = 2;

    private record Table(long salt, double radius, float[] wall, float[] coast, double maxWall, double maxCoast) {}

    private static volatile Table table;

    /** Max wall drift for the current world, in blocks. */
    public static double maxWallOffset() { return wobbly() ? table().maxWall : 0; }

    /** Max coast-line drift for the current world, in blocks. */
    public static double maxCoastOffset() { return wobbly() ? table().maxCoast : 0; }

    public static boolean wobbly() { return RimSettings.active().edgeShape >= WOBBLY; }

    /**
     * Signed distance to the wall line (negative inside). Far from the edge the drift can't matter, so the
     * angle lookup is skipped there.
     */
    public static double rel(double x, double z) {
        double R = RimSettings.wallRadius();
        double d = Math.sqrt(x * x + z * z) - R;
        if (!wobbly()) return d;
        Table t = table();
        if (Math.abs(d) > t.maxWall + 400) return d;
        // Worldgen asks for the same column many times in a row (every Y, several edge pieces): cache it.
        double[] last = LAST.get();
        if (last[0] != x || last[1] != z || last[3] != t.salt) {
            last[0] = x; last[1] = z; last[2] = lookup(t.wall, x, z); last[3] = t.salt;
        }
        return d - last[2];
    }

    private static final ThreadLocal<double[]> LAST = ThreadLocal.withInitial(() -> new double[]{Double.NaN, Double.NaN, 0, 0});

    /** Wall drift at this angle. */
    public static double wallOffset(double x, double z) {
        return wobbly() ? lookup(table().wall, x, z) : 0;
    }

    /** Coast-line drift at this angle (inner edge of the ocean ring). */
    public static double coastOffset(double x, double z) {
        return wobbly() ? lookup(table().coast, x, z) : 0;
    }

    private static double lookup(float[] a, double x, double z) {
        int n = a.length;
        double u = (Math.atan2(z, x) + Math.PI) / (2 * Math.PI) * n;
        int i = (int) u;
        double f = u - i;
        i %= n;
        return a[i] + (a[(i + 1) % n] - a[i]) * f;
    }

    private static Table table() {
        Table t = table;
        long salt = IslandField.salt;
        double R = RimSettings.wallRadius();
        if (t != null && t.salt == salt && t.radius == R) return t;
        synchronized (RimShape.class) {
            t = table;
            if (t != null && t.salt == salt && t.radius == R) return t;
            int n = (int) Math.max(8192, Math.ceil(2 * Math.PI * R / 4));
            double aWall = Math.max(30, Math.min(300, 0.015 * R));
            double aCoast = Math.max(80, Math.min(800, 0.04 * R));
            // Swells: harmonics k around the circle (wavelength = 2*pi*R / k, so they scale with the disc).
            // Kinks: irregular value noise along the circumference, cell sizes in blocks scaled by R / 5,000.
            double sc = R / 5000.0;
            // Wall: swells ~600-1,000 blocks (k 31-52) + kinks on ~110 and ~40 block cells.
            float[] wall = sum(n, salt ^ 0x5EEDL, 31, 52, 0.8 * aWall, 2 * Math.PI * R, 110 * sc, 40 * sc, 0.2 * aWall);
            // Coast: broader bends ~800-2,500 blocks (k 12-40) + smaller ones on ~300 and ~120 block cells.
            float[] coast = sum(n, salt ^ 0xC0A57L, 12, 40, 0.85 * aCoast, 2 * Math.PI * R, 300 * sc, 120 * sc, 0.15 * aCoast);
            t = new Table(salt, R, wall, coast, aWall, aCoast);
            table = t;
            return t;
        }
    }

    /** Harmonics kLo..kHi (random weights) for the swells, plus two octaves of wrapping value noise for the kinks. */
    private static float[] sum(int n, long seed, int kLo, int kHi, double ampSwell, double circumference,
                               double cell1, double cell2, double ampKink) {
        double[] swell = new double[n], kink = new double[n];
        long s = seed;
        for (int k = kLo; k <= kHi; k++) {
            s = IslandField.mix(s + k);
            double w = 0.3 + 0.7 * ((s >>> 11) * 0x1.0p-53);
            s = IslandField.mix(s);
            double ph = ((s >>> 11) * 0x1.0p-53) * 2 * Math.PI;
            add(swell, k, w, ph);
        }
        valueNoise(kink, IslandField.mix(seed + 77), Math.max(3, (int) Math.round(circumference / cell1)), 0.7);
        valueNoise(kink, IslandField.mix(seed + 78), Math.max(3, (int) Math.round(circumference / cell2)), 0.3);
        scale(swell, ampSwell);
        scale(kink, ampKink);
        float[] out = new float[n];
        for (int i = 0; i < n; i++) out[i] = (float) (swell[i] + kink[i]);
        return out;
    }

    /** Smooth random values on `knots` points around the circle (wraps), added with weight w. */
    private static void valueNoise(double[] a, long seed, int knots, double w) {
        double[] v = new double[knots];
        long s = seed;
        for (int i = 0; i < knots; i++) { s = IslandField.mix(s + i); v[i] = ((s >>> 11) * 0x1.0p-53) * 2 - 1; }
        int n = a.length;
        for (int i = 0; i < n; i++) {
            double u = (double) i * knots / n;
            int k = (int) u;
            double f = u - k;
            f = f * f * (3 - 2 * f);
            a[i] += w * (v[k % knots] + (v[(k + 1) % knots] - v[k % knots]) * f);
        }
    }

    private static void add(double[] a, int k, double w, double ph) {
        int n = a.length;
        for (int i = 0; i < n; i++) a[i] += w * Math.sin(k * (2 * Math.PI * i / n) + ph);
    }

    private static void scale(double[] a, double peak) {
        double m = 1e-9;
        for (double v : a) m = Math.max(m, Math.abs(v));
        for (int i = 0; i < a.length; i++) a[i] *= peak / m;
    }

    private RimShape() {}
}
