package dev.tigetiiger.larionrim.mixin;

import dev.tigetiiger.larionrim.df.Edge;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** No structures start beyond the wall. */
@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {
    @Inject(method = "tryGenerateStructure", at = @At("HEAD"), cancellable = true)
    private void larionRim$noStructuresInVoid(net.minecraft.world.level.levelgen.structure.StructureSet.StructureSelectionEntry entry,
                                              net.minecraft.world.level.StructureManager sm, net.minecraft.core.RegistryAccess ra,
                                              net.minecraft.world.level.levelgen.RandomState rs,
                                              net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager tm,
                                              long seed, net.minecraft.world.level.chunk.ChunkAccess chunk, ChunkPos pos,
                                              net.minecraft.core.SectionPos sp, net.minecraft.resources.ResourceKey<Level> dim,
                                              CallbackInfoReturnable<Boolean> cir) {
        if (!dim.equals(Level.OVERWORLD)) return;
        if (!((Object) this instanceof NoiseBasedChunkGenerator g) || !Edge.isLarionOverworld(g.generatorSettings().value())) return;
        // Use the chunk corner closest to the centre, so structures touching the wall area are blocked too.
        int x = pos.getMinBlockX() + 8, z = pos.getMinBlockZ() + 8;
        if (Math.sqrt((double) x * x + (double) z * z) - dev.tigetiiger.larionrim.RimSettings.wallRadius() > Edge.CUT_START) {
            cir.setReturnValue(false);
        }
    }
}
