package dev.tigetiiger.larionrim.mixin;

import dev.tigetiiger.larionrim.LarionRim;
import dev.tigetiiger.larionrim.RimSettings;
import dev.tigetiiger.larionrim.df.Edge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.portal.PortalForcer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * When a NEW Overworld portal would be created in the ocean ring or beyond the wall, build it on the
 * wall's beach instead. Linking to existing portals (e.g. one built on a sky island) is untouched,
 * because that happens before a new portal is ever created.
 */
@Mixin(PortalForcer.class)
public abstract class PortalForcerMixin {
    @Shadow @Final private ServerLevel level;

    @ModifyVariable(method = "createPortal", at = @At("HEAD"), argsOnly = true)
    private BlockPos larionRim$clamp(BlockPos pos, BlockPos same, Direction.Axis axis) {
        if (!level.dimension().equals(Level.OVERWORLD)) return pos;
        if (!(level.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator g) || !Edge.isLarionOverworld(g.generatorSettings().value())) return pos;
        double r = Math.sqrt((double) pos.getX() * pos.getX() + (double) pos.getZ() * pos.getZ());
        double R = RimSettings.wallRadius() + dev.tigetiiger.larionrim.RimShape.wallOffset(pos.getX(), pos.getZ());
        if (r - R < -RimSettings.active().oceanRingWidth) return pos;
        double target = R + Edge.RIDGE_CENTER - Edge.RIDGE_HALF - Edge.BEACH_WIDTH / 2; // middle of the beach
        BlockPos moved = new BlockPos((int) Math.round(pos.getX() * target / r), 70, (int) Math.round(pos.getZ() * target / r));
        LarionRim.LOG.info("Larion Rim: new portal at {} is outside the disc, moving it to {}", pos, moved);
        return moved;
    }
}
