package com.badgerson.larion.density_function_types;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

public record FlatDomainWarp(DensityFunction input, DensityFunction warpX, DensityFunction warpZ)
		implements DensityFunction {

	private static final MapCodec<FlatDomainWarp> MAP_CODEC = RecordCodecBuilder.mapCodec((instance) -> instance
			.group(DensityFunction.CODEC.fieldOf("input").forGetter(FlatDomainWarp::input),
					DensityFunction.CODEC.fieldOf("warp_x").forGetter(FlatDomainWarp::warpX),
					DensityFunction.CODEC.fieldOf("warp_z").forGetter(FlatDomainWarp::warpZ))
			.apply(instance, (FlatDomainWarp::new)));
	public static final KeyDispatchDataCodec<FlatDomainWarp> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

	@Override
	public double compute(FunctionContext context) {
		return input.compute(new SinglePointContext(
				context.blockX() + (int) this.warpX.compute(context),
				context.blockY(),
				context.blockZ() + (int) this.warpZ.compute(context)));
	}

	@Override
	public void fillArray(double[] densities, ContextProvider provider) {
		provider.fillAllDirectly(densities, this);
	}

	@Override
	public DensityFunction mapChildren(Visitor visitor) {
	    return new FlatDomainWarp(visitor.apply(this.input), visitor.apply(this.warpX), visitor.apply(this.warpZ));
	}

	@Override
	public double minValue() {
		return this.input.minValue();
	}

	@Override
	public double maxValue() {
		return this.input.maxValue();
	}

	@Override
	public KeyDispatchDataCodec<? extends DensityFunction> codec() {
		return CODEC;
	}
}
