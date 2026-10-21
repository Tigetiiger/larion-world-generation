package dev.tigetiiger.larionrim.mixin;

import dev.tigetiiger.larionrim.RimDecorator;
import dev.tigetiiger.larionrim.df.Edge;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(NoiseBasedChunkGenerator.class)
public abstract class NoiseBasedChunkGeneratorMixin {
    @Inject(method = "buildSurface(Lnet/minecraft/server/level/WorldGenRegion;Lnet/minecraft/world/level/StructureManager;Lnet/minecraft/world/level/levelgen/RandomState;Lnet/minecraft/world/level/chunk/ChunkAccess;)V", at = @At("TAIL"))
    private void larionRim$decorate(WorldGenRegion region, StructureManager sm, RandomState rs, ChunkAccess chunk, CallbackInfo ci) {
        if (Edge.isLarionOverworld(((NoiseBasedChunkGenerator) (Object) this).generatorSettings().value())) RimDecorator.decorate(region, chunk);
    }
}
