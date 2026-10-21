package dev.tigetiiger.larionrim;

import dev.tigetiiger.larionrim.df.CoordNorm;
import dev.tigetiiger.larionrim.df.EdgePart;
import dev.tigetiiger.larionrim.df.RingBlend;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LarionRim implements ModInitializer {
    public static final String MOD_ID = "larion_rim";
    public static final Logger LOG = LoggerFactory.getLogger("Larion Rim");

    @Override
    public void onInitialize() {
        register("coord_norm", CoordNorm.CODEC.codec());
        register("ring_blend", RingBlend.CODEC.codec());
        register("edge_part", EdgePart.CODEC.codec());
        Registry.register(BuiltInRegistries.MATERIAL_RULE, Identifier.fromNamespaceAndPath(MOD_ID, "rim"), RimSurfaceRule.CODEC);
        RimSettings.readConfig(); // create the config file with defaults on first start
    }

    private static void register(String name, com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.levelgen.DensityFunction> codec) {
        Registry.register(BuiltInRegistries.DENSITY_FUNCTION_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, name), codec);
    }
}
