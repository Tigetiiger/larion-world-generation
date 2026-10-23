package dev.tigetiiger.larionrim;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Disc geometry. Defaults come from config/larion_rim.json; each world copies the values into
 * its own larion_rim.json when it is created and keeps them forever after.
 * The active values are static because density functions have no other way to reach them.
 */
public final class RimSettings {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String FILE_NAME = "larion_rim.json";

    /** Serialized form, used for both the config file and the per-world file. */
    public static final class Values {
        /** Wall to wall, in blocks. */
        public int diameter = 10_000;
        /** Width of the ocean ring inside the wall. */
        public int oceanRingWidth = 1_000;
        /** Sky islands appear between these distances beyond the wall. */
        public int islandBandMin = 200;
        public int islandBandMax = 2_000;
        /** One island per square cell of this size. */
        public int islandSpacing = 1_000;
        /** Outline style; see {@link RimShape}. Worlds saved before this field existed keep the exact circle. */
        public int edgeShape = RimShape.WOBBLY;

        Values sanitized() {
            Values v = new Values();
            v.diameter = clamp(diameter, 2_000, 200_000);
            v.oceanRingWidth = clamp(oceanRingWidth, 0, v.diameter / 4);
            v.islandBandMin = clamp(islandBandMin, 100, 10_000);
            v.islandBandMax = clamp(islandBandMax, v.islandBandMin + 200, 20_000);
            v.islandSpacing = clamp(islandSpacing, 400, 10_000);
            v.edgeShape = clamp(edgeShape, RimShape.CIRCLE, RimShape.WOBBLY);
            return v;
        }
    }

    private static volatile Values active = new Values();

    public static Values active() { return active; }
    public static double wallRadius() { return active.diameter / 2.0; }

    public static Path configFile() {
        return Path.of("config", FILE_NAME);
    }

    /** Reads the global default config, creating it if missing. */
    public static Values readConfig() {
        Path f = configFile();
        Values v = read(f);
        if (v == null) {
            v = new Values();
            write(f, v);
        }
        return v.sanitized();
    }

    public static void writeConfig(Values v) {
        write(configFile(), v.sanitized());
    }

    /** Called when a server loads a world: use the world's saved values, or lock in the config. */
    public static void loadForWorld(Path worldDir) {
        Path f = worldDir.resolve(FILE_NAME);
        Values v = read(f);
        if (v == null) {
            v = readConfig();
            write(f, v);
            LarionRim.LOG.info("Larion Rim: new world, locking diameter {} (saved to {})", v.diameter, f);
        } else {
            LarionRim.LOG.info("Larion Rim: world diameter {}, edge shape {}", v.diameter, v.edgeShape);
        }
        active = v.sanitized();
    }

    private static Values read(Path f) {
        if (!Files.isRegularFile(f)) return null;
        try {
            String json = Files.readString(f);
            Values v = GSON.fromJson(json, Values.class);
            // A world file from before outlines existed: keep its exact circle so new chunks match old ones.
            if (!f.equals(configFile()) && !json.contains("\"edgeShape\"")) v.edgeShape = RimShape.CIRCLE;
            return v;
        } catch (Exception e) {
            LarionRim.LOG.error("Larion Rim: could not read {}, using defaults", f, e);
            return new Values();
        }
    }

    private static void write(Path f, Values v) {
        try {
            if (f.getParent() != null) Files.createDirectories(f.getParent());
            Files.writeString(f, GSON.toJson(v));
        } catch (IOException e) {
            LarionRim.LOG.error("Larion Rim: could not write {}", f, e);
        }
    }

    private static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    private RimSettings() {}
}
