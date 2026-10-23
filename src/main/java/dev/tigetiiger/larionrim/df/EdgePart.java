package dev.tigetiiger.larionrim.df;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.tigetiiger.larionrim.IslandField;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * One position-only piece of the edge shape. The final density combines these with Larion's terrain using
 * vanilla min / max / range_choice, so worldgen optimizers (C2ME) can still compile Larion's part.
 * Parts: "rel" (distance from the wall), "cap" (lowers Larion's land), "raise" (seabed fill, beach, ridge),
 * "cut" (overhang underside), "islands" (everything beyond the wall).
 */
public record EdgePart(String part, DensityFunction.NoiseHolder noise) implements DensityFunction.SimpleFunction {
    public static final KeyDispatchDataCodec<EdgePart> CODEC = KeyDispatchDataCodec.of(RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.fieldOf("part").forGetter(EdgePart::part),
            DensityFunction.NoiseHolder.CODEC.fieldOf("noise").forGetter(EdgePart::noise)).apply(i, EdgePart::new)));

    @Override
    public double compute(FunctionContext c) {
        return switch (part) {
            case "rel" -> Edge.rel(c);
            case "cap" -> Edge.cap(c);
            case "raise" -> Edge.raise(c, noise);
            case "cut" -> Edge.cut(c, noise);
            default -> Edge.islands(c, noise);
        };
    }

    @Override
    public DensityFunction mapChildren(Visitor visitor) {
        EdgePart e = new EdgePart(part, visitor.visitNoise(noise));
        if (e.noise.noise() != null && part.equals("raise")) {
            Edge.lodNoise = e.noise;
            IslandField.salt = IslandField.mix(Double.doubleToLongBits(e.noise.getValue(12.34, 56.78, 90.12)) * 31 + 7);
        }
        return e;
    }

    @Override public double minValue() { return -1e6; }
    @Override public double maxValue() { return 1e6; }
    @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() { return CODEC; }
}
