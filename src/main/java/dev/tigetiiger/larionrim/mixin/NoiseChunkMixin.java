package dev.tigetiiger.larionrim.mixin;

import dev.tigetiiger.larionrim.RimSettings;
import dev.tigetiiger.larionrim.df.Edge;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps oceans and the lava sea out of the void, and tells surface rules that the
 * "surface" beyond the wall is at the bottom of the world (so island tops get grass, sand, snow...).
 */
@Mixin(NoiseChunk.class)
public abstract class NoiseChunkMixin {
    @Unique private boolean larionRim$active;

    @ModifyVariable(method = "forChunk", at = @At("HEAD"), argsOnly = true)
    private static Aquifer.FluidPicker larionRim$wrapFluids(Aquifer.FluidPicker original, net.minecraft.world.level.chunk.ChunkAccess chunk,
                                                           net.minecraft.world.level.levelgen.RandomState random,
                                                           net.minecraft.world.level.levelgen.DensityFunctions.BeardifierOrMarker b,
                                                           NoiseGeneratorSettings settings) {
        return Edge.isLarionOverworld(settings) ? new dev.tigetiiger.larionrim.RimFluidPicker(original) : original;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void larionRim$remember(int cellCountXZ, net.minecraft.world.level.levelgen.RandomState random, int firstX, int firstZ,
                                    net.minecraft.world.level.levelgen.NoiseSettings ns, net.minecraft.world.level.levelgen.DensityFunctions.BeardifierOrMarker b,
                                    NoiseGeneratorSettings settings, Aquifer.FluidPicker picker, net.minecraft.world.level.levelgen.blending.Blender blender, CallbackInfo ci) {
        larionRim$active = Edge.isLarionOverworld(settings);
    }

    @Inject(method = "preliminarySurfaceLevel", at = @At("HEAD"), cancellable = true)
    private void larionRim$voidSurface(int x, int z, CallbackInfoReturnable<Integer> cir) {
        // Beyond the wall: island tops count as "surface". On the ridge: lets the surface pass style the whole
        // wall down to the overhang (it normally only works near the expected surface).
        if (larionRim$active && Math.sqrt((double) x * x + (double) z * z) - dev.tigetiiger.larionrim.RimSettings.wallRadius() > Edge.RIDGE_CENTER - Edge.RIDGE_HALF) cir.setReturnValue(-128);
    }
}
