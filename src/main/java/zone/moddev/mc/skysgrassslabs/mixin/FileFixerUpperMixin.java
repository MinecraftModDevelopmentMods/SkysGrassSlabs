package zone.moddev.mc.skysgrassslabs.mixin;

import com.mojang.serialization.Dynamic;
import net.minecraft.util.filefix.FileFixerUpper;
import net.minecraft.util.worldupdate.UpgradeProgress;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import zone.moddev.mc.skysgrassslabs.compat.LegacyWorldDataHook;

/** Captures legacy registry IDs before Minecraft relocates and fixes world files. */
@Mixin(FileFixerUpper.class)
abstract class FileFixerUpperMixin {
    @Inject(
            method = "fix(Lnet/minecraft/world/level/storage/LevelStorageSource$LevelStorageAccess;"
                    + "Lcom/mojang/serialization/Dynamic;"
                    + "Lnet/minecraft/util/worldupdate/UpgradeProgress;)"
                    + "Lcom/mojang/serialization/Dynamic;",
            at = @At("HEAD"))
    private void skysgrassslabs$captureLegacyLevelData(
            LevelStorageSource.LevelStorageAccess access, Dynamic<?> levelData,
            UpgradeProgress progress, CallbackInfoReturnable<Dynamic<?>> callback) {
        LegacyWorldDataHook.captureLegacyLevelData(access, access.getLevelDirectory());
    }
}
