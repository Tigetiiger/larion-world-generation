// Original credit: https://github.com/klinbee/More-Density-Functions (thanks)
package com.badgerson.larion.density_function_types;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;

public record Signum(DensityFunction df) implements DensityFunctions.PureTransformer {

    private static final MapCodec<Signum> MAP_CODEC = RecordCodecBuilder.mapCodec((instance) -> instance.group(DensityFunction.CODEC.fieldOf("argument").forGetter(Signum::df)).apply(instance, (Signum::new)));
    public static final KeyDispatchDataCodec<Signum> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    @Override
    public DensityFunction input() {
        return this.df;
    }

    @Override
    public double transform(double density) {
        return Math.signum(density);
    }

    @Override
    public DensityFunction mapChildren(Visitor visitor) {
        return new Signum(visitor.apply(this.df));
    }

    @Override
    public double minValue() {
        return transform(this.df.minValue());
    }

    @Override
    public double maxValue() {
        return transform(this.df.maxValue());
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}

