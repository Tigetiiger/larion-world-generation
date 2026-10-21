package dev.tigetiiger.larionrim.mixin;

import dev.tigetiiger.larionrim.RimSurfaceRule;
import dev.tigetiiger.larionrim.df.Edge;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.SurfaceRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Puts the Rimworld materials in front of Larion's surface rules (overworld only). */
@Mixin(NoiseGeneratorSettings.class)
public abstract class NoiseGeneratorSettingsMixin {
    @Unique private SurfaceRules.RuleSource larionRim$wrapped;

    @Inject(method = "surfaceRule", at = @At("RETURN"), cancellable = true)
    private void larionRim$wrap(CallbackInfoReturnable<SurfaceRules.RuleSource> cir) {
        if (!Edge.isLarionOverworld((NoiseGeneratorSettings) (Object) this)) return;
        if (larionRim$wrapped == null) larionRim$wrapped = SurfaceRules.sequence(RimSurfaceRule.INSTANCE, cir.getReturnValue());
        cir.setReturnValue(larionRim$wrapped);
    }
}
