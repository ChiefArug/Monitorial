package chiefarug.mods.monitorial.mixin;

import chiefarug.mods.monitorial.config.MonitorialStartupConfig;
import chiefarug.mods.monitorial.config.Position;
import chiefarug.mods.monitorial.early_startup.Helpers;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow @Final private Window window;

    @Inject(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setWindowActive(Z)V")
    )
    public void monitorial$forceUpdateSizeAndPos$WindowsSucks(GameConfig gameConfig, CallbackInfo ci) {
        MonitorialStartupConfig config = MonitorialStartupConfig.getInstance();
        if (!config.forceMove().shouldAttemptMove()) return;
        Position position = config.position();
        Helpers.forceUpdatePos(Helpers.getCurrentMonitor(window), window.getWindow(), position.x(), position.y());
        Position size = config.windowSize();
        Helpers.forceUpdateSize(window.getWindow(), size.x(), size.y());
    }
}
