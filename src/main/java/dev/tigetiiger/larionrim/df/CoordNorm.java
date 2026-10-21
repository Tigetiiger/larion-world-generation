package dev.tigetiiger.larionrim.df;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tigetiiger.larionrim.RimSettings;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/** A coordinate divided by the wall radius: "x", "z", or "r" (distance from 0,0). */
public record CoordNorm(String axis) implements DensityFunction.SimpleFunction {
    public static final KeyDispatchDataCodec<CoordNorm> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("axis").forGetter(CoordNorm::axis)).apply(i, CoordNorm::new)));

    @Override
    public double compute(FunctionContext c) {
        double r = RimSettings.wallRadius();
        return switch (axis) {
            case "x" -> c.blockX() / r;
            case "z" -> c.blockZ() / r;
            default -> Math.sqrt((double) c.blockX() * c.blockX() + (double) c.blockZ() * c.blockZ()) / r;
        };
    }

    @Override public double minValue() { return axis.equals("r") ? 0 : -1000; }
    @Override public double maxValue() { return 1000; }
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }
}
