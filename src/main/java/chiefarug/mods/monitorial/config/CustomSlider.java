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
        this.minValue = min;
        this.maxValue = max;
        this.setValue(this.value); // Math.min(max, Math.max(value, min));
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
