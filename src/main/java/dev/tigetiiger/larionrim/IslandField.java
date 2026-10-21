package dev.tigetiiger.larionrim;

/**
 * Sky island placement. Pure function of (world salt, grid cell), so terrain shaping and
 * the decoration pass agree on where every island is.
 */
public final class IslandField {
    /** Set from the world's seeded noise when worldgen is wired up (see Edge.mapChildren). */
    public static volatile long salt = 0;

    public record Island(double x, double z, double radius, double topY, double depth,
                         double phase1, double phase2, double springAngle, double rndSpring) {
        /** Outline radius in a given direction (deterministic wobble). */
        public double edgeRadius(double angle) {
            return radius * (1 + 0.14 * Math.sin(3 * angle + phase1) + 0.07 * Math.sin(5 * angle + phase2));
        }

        /** Climate: z / wall radius (negative = north = cold). */
        public double latitude() { return z / RimSettings.wallRadius(); }
    }

    /** The island whose grid cell contains (x, z), or null. */
    public static Island at(int x, int z) {
        int s = RimSettings.active().islandSpacing;
        return inCell(Math.floorDiv(x, s), Math.floorDiv(z, s));
    }

    public static Island inCell(int cx, int cz) {
        RimSettings.Values v = RimSettings.active();
        int s = v.islandSpacing;
        double margin = 160;
        double ix = cx * (double) s + margin + rnd(cx, cz, 1) * (s - 2 * margin);
        double iz = cz * (double) s + margin + rnd(cx, cz, 2) * (s - 2 * margin);
        double radius = 25 + rnd(cx, cz, 3) * 50;
        double dist = Math.sqrt(ix * ix + iz * iz) - RimSettings.wallRadius();
        if (dist < v.islandBandMin + radius || dist > v.islandBandMax) return null;
        double topY = 80 + rnd(cx, cz, 4) * 120;
        double depth = radius * (1.0 + rnd(cx, cz, 5) * 0.9);
        return new Island(ix, iz, radius, topY, depth, rnd(cx, cz, 6) * 6.283, rnd(cx, cz, 7) * 6.283,
                rnd(cx, cz, 8) * 6.283, rnd(cx, cz, 9));
    }

    /** Deterministic random in [0,1) for a cell and key. */
    public static double rnd(int cx, int cz, int k) {
        return (mix(salt ^ (cx * 0x9E3779B97F4A7C15L) ^ (cz * 0xC2B2AE3D27D4EB4FL) ^ (k * 0x165667B19E3779F9L)) >>> 11) * 0x1.0p-53;
    }

    /** Deterministic random in [0,1) for a block position. */
    public static double rnd3(int x, int y, int z, int k) {
        return (mix(salt ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xD6E8FEB86659FD93L) ^ (z * 0xC2B2AE3D27D4EB4FL) ^ (k * 0x165667B19E3779F9L)) >>> 11) * 0x1.0p-53;
    }

    /** Smooth value noise in [0,1] on a grid of the given cell size (deterministic, same everywhere). */
    public static double vnoise(double x, double z, double cell, int k) {
        double gx = x / cell, gz = z / cell;
        int ix = (int) Math.floor(gx), iz = (int) Math.floor(gz);
        double fx = gx - ix, fz = gz - iz;
        fx = fx * fx * (3 - 2 * fx); fz = fz * fz * (3 - 2 * fz);
        double a = rnd(ix, iz, k), b = rnd(ix + 1, iz, k), c = rnd(ix, iz + 1, k), d = rnd(ix + 1, iz + 1, k);
        return (a + (b - a) * fx) + ((c + (d - c) * fx) - (a + (b - a) * fx)) * fz;
    }

    /** 0..1 strength of a rocky outcrop on island tops at (x, z). */
    public static double rockiness(int x, int z) {
        double n = 0.65 * vnoise(x, z, 14, 31) + 0.35 * vnoise(x, z, 6, 32);
        return Math.max(0, (n - 0.62) / 0.38);
    }

    /** 0..1 strength of a rock crag on an island's underside (3D, so patches wrap around the cone). */
    public static double underRock(int x, int y, int z) {
        double n = 0.6 * vnoise(x + 0.7 * y, z - 0.5 * y, 11, 33) + 0.4 * vnoise(x - 0.4 * y, z + 0.8 * y, 5, 34);
        return Math.max(0, (n - 0.5) / 0.5);
    }

    public static long mix(long b) {
        b ^= b >>> 33; b *= 0xff51afd7ed558ccdL; b ^= b >>> 33; b *= 0xc4ceb9fe1a85ec53L; b ^= b >>> 33;
        return b;
    }

    private IslandField() {}
}
