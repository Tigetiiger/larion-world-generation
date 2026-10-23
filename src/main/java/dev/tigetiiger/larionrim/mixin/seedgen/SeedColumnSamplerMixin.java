package dev.tigetiiger.larionrim.mixin.seedgen;

import dev.tigetiiger.larionrim.compat.SeedGenEdge;
import dev.tigetiiger.larionrim.df.Edge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** SeedGen's surface height: the wall's ridge and beach inside the edge, island tops (or nothing) beyond it. */
@Pseudo
@Mixin(targets = "mobius.seedrenderer.sample.SeedColumnSampler", remap = false)
public abstract class SeedColumnSamplerMixin {
    @Shadow @Final private int minY;
    @Shadow @Final private int maxY;

    @Inject(method = "surfaceHeight(IILmobius/seedrenderer/sample/SampleQuality;I)I", at = @At("HEAD"), cancellable = true, remap = false)
    private void larionRim$beyond4(int x, int z, @Coerce Object quality, int stride, CallbackInfoReturnable<Integer> cir) {
        larionRim$beyond(x, z, cir);
    }

    @Inject(method = "surfaceHeight(IILmobius/seedrenderer/sample/SampleQuality;)I", at = @At("HEAD"), cancellable = true, remap = false)
    private void larionRim$beyond3(int x, int z, @Coerce Object quality, CallbackInfoReturnable<Integer> cir) {
        larionRim$beyond(x, z, cir);
    }

    @Inject(method = "surfaceHeight(IILmobius/seedrenderer/sample/SampleQuality;I)I", at = @At("RETURN"), cancellable = true, remap = false)
    private void larionRim$wall4(int x, int z, @Coerce Object quality, int stride, CallbackInfoReturnable<Integer> cir) {
        larionRim$wall(x, z, cir);
    }

    @Inject(method = "surfaceHeight(IILmobius/seedrenderer/sample/SampleQuality;)I", at = @At("RETURN"), cancellable = true, remap = false)
    private void larionRim$wall3(int x, int z, @Coerce Object quality, CallbackInfoReturnable<Integer> cir) {
        larionRim$wall(x, z, cir);
    }

    /** Beyond the wall the terrain search is skipped entirely: the island layout is known directly. */
    private void larionRim$beyond(int x, int z, CallbackInfoReturnable<Integer> cir) {
        if (!SeedGenEdge.active(minY, maxY) || SeedGenEdge.rel(x, z) < Edge.OUTER_FOOT) return;
        int[] island = Edge.lodIsland(x, z);
        cir.setReturnValue(island == null ? minY : island[1]);
    }

    private void larionRim$wall(int x, int z, CallbackInfoReturnable<Integer> cir) {
        if (!SeedGenEdge.active(minY, maxY)) return;
        double rel = SeedGenEdge.rel(x, z);
        if (rel < Edge.AFFECT_START || rel >= Edge.OUTER_FOOT) return;
        cir.setReturnValue(Edge.lodSurface(x, z, cir.getReturnValue()));
    }
}
