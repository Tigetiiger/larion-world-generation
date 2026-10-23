package dev.tigetiiger.larionrim.df;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tigetiiger.larionrim.RimSettings;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Smooth 0 → 1 ramp by distance from 0,0. {@code from}/{@code to} are block offsets relative
 * to the wall; the keywords "ring" in {@code relative_to} shift them to the ocean ring's inner edge.
 */
public record RingBlend(double from, double to, String relativeTo) implements DensityFunction.SimpleFunction {
    public static final KeyDispatchDataCodec<RingBlend> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.DOUBLE.fieldOf("from").forGetter(RingBlend::from),
            Codec.DOUBLE.fieldOf("to").forGetter(RingBlend::to),
            Codec.STRING.optionalFieldOf("relative_to", "wall").forGetter(RingBlend::relativeTo)).apply(i, RingBlend::new)));

    @Override
    public double compute(FunctionContext c) {
        double x = c.blockX(), z = c.blockZ();
        double d;
        if (relativeTo.equals("ring")) {
            // distance to the mainland coast line: ring's inner edge plus its own drift (only looked up near it)
            d = Math.sqrt(x * x + z * z) - (RimSettings.wallRadius() - RimSettings.active().oceanRingWidth);
            if (Math.abs(d) < dev.tigetiiger.larionrim.RimShape.maxCoastOffset() + 400) d -= dev.tigetiiger.larionrim.RimShape.coastOffset(x, z);
        } else {
            d = dev.tigetiiger.larionrim.RimShape.rel(x, z);
        }
        double f = from, e = to;
        if (relativeTo.equals("ring") && !dev.tigetiiger.larionrim.RimShape.wobbly()) { f = -300; e = 0; } // circle worlds: original coast push
        double t = (d - f) / (e - f);
        if (t <= 0) return 0;
        if (t >= 1) return 1;
        return t * t * (3 - 2 * t);
    }

    @Override public double minValue() { return 0; }
    @Override public double maxValue() { return 1; }
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }
}
