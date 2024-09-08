package chiefarug.mods.monitorial.config;

import chiefarug.mods.monitorial.mixin.UpdatableTooltip$AlsoCycleButtonAccessor;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.gui.widget.ExtendedSlider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

final class CustomSlider extends ExtendedSlider implements UpdatableTooltip$AlsoCycleButtonAccessor {

    @Nullable
    private Long applyDelay = null;
    private final Consumer<Double> onApply;
    private final Tooltip tooltip;

    CustomSlider(Component prefix, double minValue, double maxValue, double currentValue, Consumer<Double> onApply, Tooltip tooltip) {
        super(-1, -1, 150, 20, prefix, MonitorialConfigScreen.PIXELS, minValue, maxValue, currentValue, 1, 1, true);
        this.onApply = onApply;
        this.tooltip = tooltip;
    }

    @SuppressWarnings("SameParameterValue")
    void setRange(double min, double max) {
//        double oldValue = this.value;
        this.minValue = min; // we dont actually handle the value here as it is stored in percent form, which is a good way to keep it
        this.maxValue = max;// (so if it was half your screen size on a 1080p monitor, after clicking the button it will be half the screensize on the 4k monitor)
//        this.value =; // this will handle clamping to the new range for us
    }

    @NotNull
    @Override
    protected ResourceLocation getHandleSprite() {
        return this.active ? super.getHandleSprite() : MonitorialConfigScreen.DISABLED_SLIDER_HANDLE;
    }

    @Override
    public void monitorial$updateTooltip() {
        this.setTooltip(this.isActive() ? this.tooltip : Tooltip.create(MonitorialConfigScreen.BUTTON_DISABLED));
    }

    @Override
    protected void applyValue() {
        this.applyDelay = Util.getMillis() + 600L;
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
        if (this.applyDelay != null && this.applyDelay >= Util.getMillis()) {
            this.onApply.accept(this.getValue());
        }
    }
}
