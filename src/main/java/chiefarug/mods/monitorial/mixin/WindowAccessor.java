package chiefarug.mods.monitorial.mixin;

import com.mojang.blaze3d.platform.ScreenManager;
import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Window.class)
public interface WindowAccessor {
    @Accessor("screenManager")
    ScreenManager monitorial$getScreenManager();
}
