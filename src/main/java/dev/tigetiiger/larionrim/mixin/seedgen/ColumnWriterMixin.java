package dev.tigetiiger.larionrim.mixin.seedgen;

import com.seibel.distanthorizons.api.interfaces.block.IDhApiBiomeWrapper;
import com.seibel.distanthorizons.api.interfaces.block.IDhApiBlockStateWrapper;
import dev.tigetiiger.larionrim.compat.SeedGenEdge;
import mobius.seedrenderer.dh.ColumnWriter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Under the rim's overhang: the column's lowest part becomes air (void) instead of solid ground. */
@Pseudo
@Mixin(targets = "mobius.seedrenderer.dh.ColumnWriter", remap = false)
public abstract class ColumnWriterMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = "<init>", at = @At("TAIL"), remap = false)
    private void larionRim$record(CallbackInfo ci) {
        if (SeedGenEdge.DEBUG) ColumnWriter.record(true);
    }

    @Inject(method = "add(IILcom/seibel/distanthorizons/api/interfaces/block/IDhApiBlockStateWrapper;Lcom/seibel/distanthorizons/api/interfaces/block/IDhApiBiomeWrapper;II)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void larionRim$clip(int bottomY, int topY, IDhApiBlockStateWrapper block, IDhApiBiomeWrapper biome, int blockLight, int skyLight, CallbackInfo ci) {
        SeedGenEdge.Column col = SeedGenEdge.COLUMN.get();
        int clip = col.clip;
        if (clip == Integer.MIN_VALUE || bottomY >= clip) return;
        ci.cancel();
        col.clip = Integer.MIN_VALUE; // the calls below must not be clipped again
        try {
            ColumnWriter self = (ColumnWriter) (Object) this;
            if (!col.airWritten) {
                col.airWritten = true;
                if (clip > 0) self.add(0, clip, (IDhApiBlockStateWrapper) col.air, biome, 0, 15);
            }
            if (topY > clip) self.add(clip, topY, block, biome, blockLight, skyLight);
        } finally {
            col.clip = clip;
        }
    }
}
