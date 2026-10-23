package dev.tigetiiger.larionrim.mixin.seedgen;

import com.seibel.distanthorizons.api.interfaces.block.IDhApiBiomeWrapper;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBlockStateWrapper;
import dev.tigetiiger.larionrim.RimSurfaceRule;
import dev.tigetiiger.larionrim.compat.SeedGenEdge;
import dev.tigetiiger.larionrim.df.Edge;
import mobius.seedrenderer.dh.BiomeSurfaces;
import mobius.seedrenderer.dh.ColumnWriter;
import mobius.seedrenderer.sample.SeedColumnSampler;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * SeedGen's column builder: beyond the wall, writes the void or a sky island directly (no terrain search);
 * under the rim, marks the overhang's void; on the wall, uses the addon's wall materials.
 */
@Pseudo
@Mixin(targets = "mobius.seedrenderer.dh.SeedWorldGenerator", remap = false)
public abstract class SeedWorldGeneratorMixin {
    @Shadow @Final private int minY;
    @Shadow @Final private int maxY;

    @Shadow private IDhApiBlockStateWrapper blockWrapper(BlockState state) { throw new AssertionError(); }
    @Shadow private IDhApiBiomeWrapper biomeWrapper(Holder<Biome> biome) { throw new AssertionError(); }

    @Inject(method = "buildColumn", at = @At("HEAD"), cancellable = true, remap = false)
    private void larionRim$edgeColumn(ColumnWriter column, SeedColumnSampler sampler, @Coerce Object palette, @Coerce Object canopy,
                                      @Coerce Object ground, @Coerce Object structures, int[] structureBase, int x, int z, int seaLevel,
                                      int stride, IDhApiBlockStateWrapper air, IDhApiBlockStateWrapper water, IDhApiBlockStateWrapper stone,
                                      @Coerce Object settings, @Coerce Object placedSites, CallbackInfo ci) {
        SeedGenEdge.Column col = SeedGenEdge.COLUMN.get();
        col.clip = Integer.MIN_VALUE;
        if (!SeedGenEdge.active(minY, maxY)) return;
        double rel = SeedGenEdge.rel(x, z);
        if (rel >= Edge.OUTER_FOOT) { // same boundary as the real terrain's range_choice
            int height = maxY - minY;
            int[] island = Edge.lodIsland(x, z);
            Holder<Biome> biome = sampler.biome(x, island == null ? seaLevel : island[1], z);
            IDhApiBiomeWrapper b = biomeWrapper(biome);
            if (island == null) {
                column.add(0, height, air, b);
            } else {
                int bottom = island[0] - minY, top = island[1] - minY;
                String name = biome.getRegisteredName();
                boolean sandy = name.contains("desert") || name.contains("badlands") || name.contains("beach");
                BlockState body = RimSurfaceRule.lodIslandBody(x, island[1] - 6, z);
                BlockState filler = body.is(Blocks.STONE) ? Blocks.DIRT.defaultBlockState() : body;
                int fillerBottom = Math.max(bottom, top - 3);
                column.add(0, bottom, air, b);
                column.add(bottom, fillerBottom, blockWrapper(body), b, 0);
                column.add(fillerBottom, top, blockWrapper(filler), b, 0);
                column.add(top, top + 1, blockWrapper(RimSurfaceRule.lodIslandTop(x, z, sandy)), b);
                column.add(top + 1, height, air, b);
            }
            larionRim$debug(column, x, z, stride, rel);
            ci.cancel();
        } else if (rel > Edge.CUT_START) {
            col.clip = (int) Math.floor(Edge.cutHeight(rel)) - minY;
            col.airWritten = false;
            col.air = air;
        }
    }

    @Inject(method = "buildColumn", at = @At("RETURN"), remap = false)
    private void larionRim$endColumn(ColumnWriter column, SeedColumnSampler sampler, @Coerce Object palette, @Coerce Object canopy,
                                     @Coerce Object ground, @Coerce Object structures, int[] structureBase, int x, int z, int seaLevel,
                                     int stride, IDhApiBlockStateWrapper air, IDhApiBlockStateWrapper water, IDhApiBlockStateWrapper stone,
                                     @Coerce Object settings, @Coerce Object placedSites, CallbackInfo ci) {
        SeedGenEdge.COLUMN.get().clip = Integer.MIN_VALUE;
        if (SeedGenEdge.DEBUG && SeedGenEdge.active(minY, maxY)) larionRim$debug(column, x, z, stride, SeedGenEdge.rel(x, z));
    }

    private static void larionRim$debug(ColumnWriter column, int x, int z, int stride, double rel) {
        if (!SeedGenEdge.DEBUG) return;
        if (Math.abs(z) < stride && x > 0 && rel > -320 && rel < 2200)
            org.slf4j.LoggerFactory.getLogger("larion_rim").info("[lodDebug] x={} rel={} stride={} :: {}", x, (int) rel, stride, column.describe());
    }

    @Inject(method = "coverFor", at = @At("RETURN"), remap = false)
    private void larionRim$wallCover(@Coerce Object blocks, Holder<Biome> biome, int x, int surfaceY, int z, int seaLevel,
                                     CallbackInfoReturnable<BiomeSurfaces.Cover> cir) {
        if (!SeedGenEdge.active(minY, maxY)) return;
        BlockState top = RimSurfaceRule.lodMaterial(x, surfaceY, z, 0);
        if (top == null) return;
        BiomeSurfaces.Cover cover = cir.getReturnValue();
        cover.top = top;
        BlockState filler = RimSurfaceRule.lodMaterial(x, surfaceY - 2, z, 2);
        cover.filler = filler != null ? filler : top;
        cover.banded = false;
    }
}
