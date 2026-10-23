package dev.tigetiiger.larionrim.mixin;

import dev.tigetiiger.larionrim.RimSettings;
import dev.tigetiiger.larionrim.df.Edge;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.BlockColumn;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Icebergs and badlands spires fill downward to the surface floor; near the wall that would mean into the void. */
@Mixin(SurfaceSystem.class)
public abstract class SurfaceSystemMixin {
    @org.spongepowered.asm.mixin.Unique
    private static boolean larionRim$nearEdge(int x, int z) {
        return dev.tigetiiger.larionrim.RimShape.rel(x, z) > Edge.CUT_START - 20;
    }

    @Inject(method = "frozenOceanExtension", at = @At("HEAD"), cancellable = true)
    private void larionRim$noIcebergs(int minSurface, Biome biome, BlockColumn col, BlockPos.MutableBlockPos pos, int x, int z, int height, CallbackInfo ci) {
        if (larionRim$nearEdge(x, z)) ci.cancel();
    }

    @Inject(method = "erodedBadlandsExtension", at = @At("HEAD"), cancellable = true)
    private void larionRim$noHoodoos(BlockColumn col, int x, int z, int height, LevelHeightAccessor level, CallbackInfo ci) {
        if (larionRim$nearEdge(x, z)) ci.cancel();
    }
}
