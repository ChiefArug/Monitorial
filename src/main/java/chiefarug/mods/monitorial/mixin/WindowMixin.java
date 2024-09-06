package chiefarug.mods.monitorial.mixin;

import chiefarug.mods.monitorial.config.MonitorialConfigScreen;
import chiefarug.mods.monitorial.config.MonitorialStartupConfig;
import chiefarug.mods.monitorial.config.Position;
import chiefarug.mods.monitorial.early_startup.Helpers;
import chiefarug.mods.monitorial.early_startup.SharedData;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.ScreenManager;
import com.mojang.blaze3d.platform.VideoMode;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(Window.class)
public class WindowMixin {
    @Shadow @Final private long window;
    @Shadow private int windowedHeight;
    @Shadow private int windowedWidth;

    @Shadow private int width;

    @Shadow private int height;

    @Shadow private boolean fullscreen;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Shadow private Optional<VideoMode> preferredFullscreenVideoMode;

    @Inject(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Ljava/lang/Object;<init>()V", shift = At.Shift.AFTER)
    )
    public void monitorial$captureInitialDisplayData(WindowEventHandler eventHandler, ScreenManager screenManager, DisplayData displayData, String preferredFullscreenVideoMode, String title, CallbackInfo ci) {

    }


    @WrapOperation(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/platform/ScreenManager;getMonitor(J)Lcom/mojang/blaze3d/platform/Monitor;"
            )
    )
    public Monitor monitorial$wrapGetWindow(ScreenManager instance, long monitorID, Operation<Monitor> original, WindowEventHandler _eh, ScreenManager _sm, DisplayData displayData) {
        // All fields access here are set before we access them. MAKE SURE TO DOUBLE CHECK THAT BEFORE EDITING
        SharedData.originalSize = new Position(displayData.width, displayData.height);
        Monitor m = original.call(instance ,monitorID);
        VideoMode mode = m.getPreferredVidMode(this.fullscreen ? this.preferredFullscreenVideoMode : Optional.empty());
        SharedData.orignialPosition = new Position(m.getX() + mode.getWidth() / 2 - this.width / 2, m.getY() + mode.getHeight() / 2 - this.height / 2);
        return Helpers.getBestGLFWMonitorCode(() -> m, ((ScreenManagerAccessor) instance).monitorial$getMonitors().values());
    }

    @Inject(
            method = "<init>",
            at = @At(value = "INVOKE", target = "Lorg/lwjgl/glfw/GLFW;glfwDefaultWindowHints()V")
    )
    public void monitorial$setSize(WindowEventHandler eventHandler, ScreenManager screenManager, DisplayData displayData, String preferredFullscreenVideoMode, String title, CallbackInfo ci) {
        MonitorialStartupConfig config = MonitorialStartupConfig.getInstance();
        Position size = config.windowSize();
        if (size.x() > 0)
            this.windowedWidth = this.width = size.x();
        if (size.y() > 0)
            this.windowedHeight = this.height = size.y();
    }

    @Inject(
            method = "onResize",
            at = @At("TAIL")
    )
    public void monitorial$onWindowResize(long window, int width, int height, CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof MonitorialConfigScreen configScreen)
            configScreen.onResize();
    }

    @Inject(
            method = "onMove",
            at = @At("TAIL")
    )
    public void monitorial$onWindowMove(long window, int x, int y, CallbackInfo ci) {
        if (Minecraft.getInstance().screen instanceof MonitorialConfigScreen configScreen)
            configScreen.onMove();
    }

    @Inject(
            method = "close",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;assertOnRenderThread()V")
    )
    public void monitorial$onWindowClose(CallbackInfo ci) {
        MonitorialStartupConfig.getInstance().onWindowClose(((Window) ((Object) this)));
    }
}
