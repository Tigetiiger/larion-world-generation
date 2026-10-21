package dev.tigetiiger.larionrim;

import dev.tigetiiger.larionrim.df.Edge;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.Aquifer;

/** Wraps Larion's fluid picker: no water or lava in the void or under the rim's overhang. */
public record RimFluidPicker(Aquifer.FluidPicker original) implements Aquifer.FluidPicker {
    public static final Aquifer.FluidStatus AIR = new Aquifer.FluidStatus(DimensionType.MIN_Y * 2, Blocks.AIR.defaultBlockState());

    /** True where no fluid may exist: the void, and a margin under the rim's overhang (aquifers sample
     *  fluid levels up to ~24 blocks away). Everything else near the wall is solid rock below Edge's floor. */
    public static boolean dry(int x, int y, int z) {
        double rel = Math.sqrt((double) x * x + (double) z * z) - RimSettings.wallRadius();
        if (rel > Edge.RIDGE_CENTER) return true; // outer half of the ridge and beyond: never ocean
        return rel > Edge.CUT_START && y < Edge.cutHeight(rel + 32) + 8;
    }

    @Override
    public Aquifer.FluidStatus computeFluid(int x, int y, int z) {
        return dry(x, y, z) ? AIR : original.computeFluid(x, y, z);
    }
}
