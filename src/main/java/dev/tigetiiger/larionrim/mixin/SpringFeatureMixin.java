package dev.tigetiiger.larionrim.mixin;

import dev.tigetiiger.larionrim.RimSettings;
import dev.tigetiiger.larionrim.df.Edge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.SpringFeature;
import net.minecraft.world.level.levelgen.feature.configurations.SpringConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** No natural water/lava springs near the wall or on islands: they would pour into the void forever. */
@Mixin(SpringFeature.class)
public abstract class SpringFeatureMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void larionRim$noSpringsAtEdge(FeaturePlaceContext<SpringConfiguration> ctx, CallbackInfoReturnable<Boolean> cir) {
        if (!(ctx.chunkGenerator() instanceof NoiseBasedChunkGenerator g) || !Edge.isLarionOverworld(g.generatorSettings().value())) return;
        BlockPos o = ctx.origin();
        if (dev.tigetiiger.larionrim.RimShape.rel(o.getX(), o.getZ()) > Edge.CUT_START - 60) {
            cir.setReturnValue(false);
        }
    }
}
