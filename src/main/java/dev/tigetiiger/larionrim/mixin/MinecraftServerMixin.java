package dev.tigetiiger.larionrim.mixin;

import dev.tigetiiger.larionrim.RimSettings;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Loads (or locks in) the world's disc settings before any chunk is generated. */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Shadow @Final protected LevelStorageSource.LevelStorageAccess storageSource;

    @Inject(method = "loadLevel", at = @At("HEAD"))
    private void larionRim$loadSettings(CallbackInfo ci) {
        RimSettings.loadForWorld(storageSource.getLevelPath(LevelResource.ROOT));
    }
}
