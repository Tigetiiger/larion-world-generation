package dev.tigetiiger.larionrim.mixin;

import dev.tigetiiger.larionrim.RimSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.LakeFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** No lava (or water) lakes near the edge or on sky islands. */
@Mixin(LakeFeature.class)
public abstract class LakeFeatureMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void larionRim$noLakesAtEdge(FeaturePlaceContext<LakeFeature.Configuration> ctx, CallbackInfoReturnable<Boolean> cir) {
        if (!(ctx.chunkGenerator() instanceof NoiseBasedChunkGenerator g) || !dev.tigetiiger.larionrim.df.Edge.isLarionOverworld(g.generatorSettings().value())) return;
        BlockPos o = ctx.origin();
        if (Math.sqrt((double) o.getX() * o.getX() + (double) o.getZ() * o.getZ()) - RimSettings.wallRadius() > -300) cir.setReturnValue(false);
    }
}
