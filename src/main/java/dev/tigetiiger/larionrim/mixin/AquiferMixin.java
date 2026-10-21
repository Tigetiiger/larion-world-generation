package dev.tigetiiger.larionrim.mixin;

import dev.tigetiiger.larionrim.RimFluidPicker;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** In the dry zone, empty space stays empty: no aquifer "barrier" stone floating in the void. */
@Mixin(Aquifer.NoiseBasedAquifer.class)
public abstract class AquiferMixin {
    @Shadow @Final private Aquifer.FluidPicker globalFluidPicker;

    @Inject(method = "computeSubstance", at = @At("HEAD"), cancellable = true)
    private void larionRim$dryVoid(DensityFunction.FunctionContext ctx, double density, CallbackInfoReturnable<BlockState> cir) {
        if (density <= 0 && globalFluidPicker instanceof RimFluidPicker && RimFluidPicker.dry(ctx.blockX(), ctx.blockY(), ctx.blockZ())) {
            cir.setReturnValue(Blocks.AIR.defaultBlockState());
        }
    }
}
