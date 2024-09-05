package chiefarug.mods.monitorial.mixin;

import net.minecraft.client.gui.components.CycleButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(CycleButton.class)
public interface UpdatableTooltip$AlsoCycleButtonAccessor {
    @Invoker("updateTooltip")
    void monitorial$updateTooltip();
}
