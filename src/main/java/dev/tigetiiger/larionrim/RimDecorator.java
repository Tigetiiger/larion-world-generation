package dev.tigetiiger.larionrim;

import dev.tigetiiger.larionrim.df.Edge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SpeleothemThickness;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Runs after surface building: hangs roots, vines, glow lichen, dripstone and ice from the rim's
 * overhang and the island cones, and adds a spring to some temperate islands.
 */
public final class RimDecorator {
    public static void decorate(net.minecraft.server.level.WorldGenRegion region, ChunkAccess chunk) {
        int x0 = chunk.getPos().getMinBlockX(), z0 = chunk.getPos().getMinBlockZ();
        double R = RimSettings.wallRadius();
        double nearest = Math.sqrt(Math.pow(Math.max(Math.abs(x0 + 8) - 12, 0), 2) + Math.pow(Math.max(Math.abs(z0 + 8) - 12, 0), 2));
        if (nearest - R < Edge.CUT_START - 30) return; // nothing to do this far inside
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16; dx++) for (int dz = 0; dz < 16; dz++) {
            int x = x0 + dx, z = z0 + dz;
            double rel = Math.sqrt((double) x * x + (double) z * z) - R;
            if (rel < Edge.CUT_START) continue;
            RimSurfaceRule.Style st = RimSurfaceRule.style(x, z);
            boolean solidAbove = false;
            for (int y = 300; y > -120; y--) {
                pos.set(x, y, z);
                BlockState s = chunk.getBlockState(pos);
                boolean solid = !s.isAir() && s.getFluidState().isEmpty();
                if (solidAbove && s.isAir()) {
                    int placed = hang(chunk, pos, x, y, z, st);
                    if (placed > 0) { y -= placed; solidAbove = false; continue; } // don't hang things off decorations
                }
                solidAbove = solid;
            }
        }
        spring(region, chunk, x0, z0);
    }

    /** (x, y, z) is air directly under a solid ceiling. */
    /** Returns how many blocks were placed going down from y. */
    private static int hang(ChunkAccess chunk, BlockPos.MutableBlockPos pos, int x, int y, int z, RimSurfaceRule.Style st) {
        double r = IslandField.rnd3(x, y, z, 21);
        int len = 1 + (int) (IslandField.rnd3(x, y, z, 22) * 4);
        switch (st) {
            case TEMPERATE -> {
                if (r < 0.06) { set(chunk, pos, x, y, z, Blocks.HANGING_ROOTS.defaultBlockState()); return 1; }
                if (r < 0.11) {
                    BlockState vine = Blocks.VINE.defaultBlockState().setValue(VineBlock.UP, true);
                    int i = 0;
                    for (; i < len && air(chunk, pos, x, y - i, z); i++) set(chunk, pos, x, y - i, z, vine);
                    return i;
                }
                if (r < 0.14) { set(chunk, pos, x, y, z, Blocks.GLOW_LICHEN.defaultBlockState().setValue(BlockStateProperties.UP, true)); return 1; }
            }
            case COLD -> {
                if (r < 0.07) return dripstone(chunk, pos, x, y, z, Math.min(len, 3));
            }
            case FROZEN -> {
                if (r < 0.05) {
                    int i = 0;
                    for (; i < Math.min(len, 3) && air(chunk, pos, x, y - i, z); i++)
                        set(chunk, pos, x, y - i, z, (i == 0 ? Blocks.PACKED_ICE : Blocks.ICE).defaultBlockState());
                    return i;
                }
            }
            case HOT -> { }
        }
        return 0;
    }

    private static int dripstone(ChunkAccess chunk, BlockPos.MutableBlockPos pos, int x, int y, int z, int len) {
        int n = 0;
        while (n < len && air(chunk, pos, x, y - n, z)) n++;
        for (int i = 0; i < n; i++) {
            SpeleothemThickness t = i == n - 1 ? SpeleothemThickness.TIP : i == 0 && n > 2 ? SpeleothemThickness.BASE : SpeleothemThickness.FRUSTUM;
            set(chunk, pos, x, y - i, z, Blocks.POINTED_DRIPSTONE.defaultBlockState()
                    .setValue(BlockStateProperties.VERTICAL_DIRECTION, Direction.DOWN)
                    .setValue(BlockStateProperties.SPELEOTHEM_THICKNESS, t));
        }
        return n;
    }

    /** About half of the temperate islands get a spring near their rim that spills over the edge. */
    private static void spring(net.minecraft.server.level.WorldGenRegion region, ChunkAccess chunk, int x0, int z0) {
        IslandField.Island is = IslandField.at(x0 + 8, z0 + 8);
        if (is == null || is.rndSpring() > 0.5) return;
        if (RimSurfaceRule.style((int) is.x(), (int) is.z()) != RimSurfaceRule.Style.TEMPERATE) return;
        double a = is.springAngle(), d = is.edgeRadius(a) - 3;
        int sx = (int) Math.floor(is.x() + Math.cos(a) * d), sz = (int) Math.floor(is.z() + Math.sin(a) * d);
        if (sx < x0 || sx >= x0 + 16 || sz < z0 || sz >= z0 + 16) return;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = (int) is.topY() + 12; y > is.topY() - 12; y--) {
            pos.set(sx, y, sz);
            BlockState s = chunk.getBlockState(pos);
            if (!s.isAir() && s.getFluidState().isEmpty() && s.isSolidRender()) {
                pos.setY(y + 1); // on top of the ground, so it spreads out and over the edge
                chunk.setBlockState(pos, Blocks.WATER.defaultBlockState());
                region.scheduleTick(pos.immutable(), net.minecraft.world.level.material.Fluids.WATER, 0); // start flowing
                return;
            }
        }
    }

    private static boolean air(ChunkAccess chunk, BlockPos.MutableBlockPos pos, int x, int y, int z) {
        return chunk.getBlockState(pos.set(x, y, z)).isAir();
    }

    private static void set(ChunkAccess chunk, BlockPos.MutableBlockPos pos, int x, int y, int z, BlockState s) {
        chunk.setBlockState(pos.set(x, y, z), s);
    }

    private RimDecorator() {}
}
