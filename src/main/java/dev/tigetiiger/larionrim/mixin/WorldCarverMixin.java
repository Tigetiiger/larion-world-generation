package dev.tigetiiger.larionrim.mixin;

import dev.tigetiiger.larionrim.RimSettings;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.carver.CarverConfiguration;
import net.minecraft.world.level.levelgen.carver.CarvingContext;
import net.minecraft.world.level.levelgen.carver.WorldCarver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** No carver caves near the wall: they would open water-filled tunnels into the void. */
@Mixin(WorldCarver.class)
public abstract class WorldCarverMixin {
    @Inject(method = "carveEllipsoid", at = @At("HEAD"), cancellable = true)
    private void larionRim$keepWallSolid(CarvingContext ctx, CarverConfiguration cfg, net.minecraft.world.level.chunk.ChunkAccess chunk,
                                         java.util.function.Function<?, ?> biomes, Aquifer aquifer,
                                         double x, double y, double z, double hRadius, double vRadius,
                                         net.minecraft.world.level.chunk.CarvingMask mask, WorldCarver.CarveSkipChecker skip,
                                         CallbackInfoReturnable<Boolean> cir) {
        if (ctx.getMinGenY() != -128 || ctx.getGenDepth() != 640) return; // Larion overworld only
        double rel = dev.tigetiiger.larionrim.RimShape.rel(x, z);
        if (rel > -170 - hRadius) cir.setReturnValue(false);
    }
}
