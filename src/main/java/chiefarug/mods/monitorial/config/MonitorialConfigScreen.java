package chiefarug.mods.monitorial.config;

import chiefarug.mods.monitorial.early_startup.Helpers;
import chiefarug.mods.monitorial.mixin.UpdatableTooltip$AlsoCycleButtonAccessor;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.widget.ExtendedSlider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

import static chiefarug.mods.monitorial.Monitorial.MODRL;
import static chiefarug.mods.monitorial.config.MonitorialStartupConfig.*;
import static chiefarug.mods.monitorial.early_startup.Helpers.getPrimaryMonitorName;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "LoggingSimilarMessage"})
public class MonitorialConfigScreen extends OptionsSubScreen { //TODO: update values of things when the window resizes/moves (see comment about mixin to the method mc has for that) if automatic mode is on.

    private static final Component GLOBAL = Component.translatable("options.monitorial.global_config");
    private static final Component LOCAL = Component.translatable("options.monitorial.local_config");
    private static final Component BUTTON_DISABLED = Component.translatable("options.monitorial.disabled_automatic");
    private static final Component COLON = Component.literal(": ");
    private static final Component PIXELS = Component.translatable("options.monitorial.pixel_postfix");
    public static final ResourceLocation DISABLED_SLIDER_HANDLE = MODRL.withPath("widget/slider_disabled");

    private CycleButton<Boolean> automaticMode;
    private List<AbstractWidget> manualWidgets = List.of();
    private CustomSlider xPos;
    private CustomSlider yPos;
    private CustomSlider xSize;
    private CustomSlider ySize;
    private StringWidget configNotActiveText;

    private CycleButton<Boolean> currentlyEditing;
    private CycleButton<Boolean> useGlobal;


    public MonitorialConfigScreen(ModContainer ignored, Screen parent) {
        super(parent, Minecraft.getInstance().options, Component.translatable("options.monitorial.config_title"));
    }

    @SuppressWarnings("OptionalIsPresent")
    private static Component getMonitorString(Optional<MonitorData> monitor) {
        return monitor.isEmpty() ?
                Component.translatable("options.monitorial.monitor_data.primary", Component.literal(getPrimaryMonitorName())) :
                Component.literal(monitor.get().name());
    }

    private static List<Optional<MonitorData>> getMonitorData() {
        return List.of(Optional.empty()); //TODO: get list of possible monitor datas. MAKE SURE TO USE THE INSTANCE FROM THE CONFIG FOR THE CURRENT ONE, unless we pass in a list that does indexOf using equals()
    }

    @Override
    @SuppressWarnings("DataFlowIssue") // list is set directly before this method is called
    protected void addOptions() {
        // adding small in a list of one is like adding a big one.
        this.list.addSmall(List.of(currentlyEditing = CycleButton.builder((Boolean value) -> value ? GLOBAL : LOCAL)
                .withValues(true, false)
                .withInitialValue(isUsingGlobalConfig())
                .withTooltip(bool -> Tooltip.create(Component.translatable("options.monitorial.currently_editing.tooltip", Component.literal(getPrettyLocation(bool)).withStyle(ChatFormatting.GRAY))))
                .create(-1, -1, 310, 20,
                        Component.translatable("options.monitorial.currently_editing"),
                        this::updateCurrentlyEditedConfig
                )));
        addUpdatableOptions();
    }

    protected void addUpdatableOptions() {
        // this is safe as list is initted just before this method is called
        this.list.addSmall(List.of(
                useGlobal = CycleButton.onOffBuilder(getCurrentlyEditedConfig().useGlobalConfig())
                        .withTooltip(bool -> Tooltip.create(Component.translatable("options.monitorial.use_global_config.tooltip")))
                        .create(-1, -1, 150, 20,
                                Component.translatable("options.monitorial.use_global_config"), this::updateUseGlobalConfig),
                automaticMode = CycleButton.onOffBuilder(getCurrentlyEditedConfig().automaticMode())
                        .withTooltip(bool -> Tooltip.create(Component.translatable("options.monitorial.automatic_mode.tooltip")))
                        .create(-1, -1, 150, 20,
                                Component.translatable("options.monitorial.automatic_mode"), this::updateAutomaticMode)
        ));
        this.list.addSmall(manualWidgets = List.of(
                this.createManualButton("default_monitor", //NOTE: the list of values for this button needs to be recreated each time the screen opened in case a monitor got added/removed
                        MonitorialConfigScreen::getMonitorString,
                        getMonitorData(),
                        getCurrentlyEditedConfig().defaultMonitor(),
                        this::updateDefaultMonitor
                ),
                this.createManualButton("force_move",
                        ForceMoveState::toComponent,
                        List.of(ForceMoveState.values()),
                        getCurrentlyEditedConfig().forceMove(),
                        this::updateForceMove
                ), // the values and ranges for these will be set at the end in updateSliders()
                xPos = new CustomSlider(Component.translatable("options.monitorial.x_pos").append(COLON), -1,1, 0, value ->
                    getCurrentlyEditedConfig().position(getCurrentlyEditedConfig().position().withX(value.intValue()))
                , Tooltip.create(Component.translatable("options.monitorial.x_pos.tooltip"))),
                yPos = new CustomSlider(Component.translatable("options.monitorial.y_pos").append(COLON), -1,1, 0, value ->
                    getCurrentlyEditedConfig().position(getCurrentlyEditedConfig().position().withY(value.intValue()))
                , Tooltip.create(Component.translatable("options.monitorial.y_pos.tooltip"))),
                xSize = new CustomSlider(Component.translatable("options.monitorial.x_size").append(COLON), -1,1, 0, value ->
                    getCurrentlyEditedConfig().windowSize().ifPresent(current -> getCurrentlyEditedConfig().windowSize(Optional.of(current.withX(value.intValue())))) //TODO: move these to private methods and maybe use map rather than ifPresent -> make new op and set
                , Tooltip.create(Component.translatable("options.monitorial.x_size.tooltip"))),
                ySize = new CustomSlider(Component.translatable("options.monitorial.y_size").append(COLON), -1,1, 0, value ->
                    getCurrentlyEditedConfig().windowSize().ifPresent(current -> getCurrentlyEditedConfig().windowSize(Optional.of(current.withY(value.intValue()))))
                ,Tooltip.create(Component.translatable("options.monitorial.y_size.tooltip")))
        ));
        updateSliders(getCurrentlyEditedConfig().defaultMonitor());
        updateWidgetsActiveState(getCurrentlyEditedConfig().automaticMode());
    }

    private void updateSliders(Optional<MonitorData> monitorData) {
        long monitor = monitorData.map(MonitorData::id).orElseGet(GLFW::glfwGetPrimaryMonitor);
        if (monitor == 0) { // in theory impossible
            Helpers.LOGGER.error("Couldn't find monitor {}, which is strange because it was present when the configuration screen opened...", monitorData);
            return;
        }
        int[] width = new int[1],
                height = new int[1];
        GLFW.glfwGetMonitorWorkarea(monitor, null,null, width, height);
        xPos.setRange(0, width[0]);
        yPos.setRange(0, height[0]);
        xSize.setRange(1, width[0]);
        ySize.setRange(1, height[0]);
        updateSliderValues();
    }

    private void updateSliderValues() { //TODO: add button to apply current size and position if auto mode is off.
        if (!automaticMode.getValue()) return;
        Window window = Minecraft.getInstance().getWindow();
        xPos.setValue(window.getX());
        yPos.setValue(window.getY());
        xSize.setValue(window.getWidth());
        ySize.setValue(window.getHeight());
    }

    private void updateWidgetsActiveState(boolean isAuto) {
        for (AbstractWidget widg : manualWidgets) {
            widg.active = !isAuto;
            if (widg instanceof UpdatableTooltip$AlsoCycleButtonAccessor cycleButton)
                cycleButton.monitorial$updateTooltip();
        }
    }

    // isGlobalUsingGlobal is a supplier so that we can edit the local file without having to load the global file
    private void updateNotActiveWarningText(boolean isEditingGlobal, BooleanSupplier isGlobalUsingGlobal, boolean isLocalUsingGlobal) {
        MutableComponent message;
        var fine = Component.empty();
        var globalGlobal = Component.translatable("options.monitorial.not_in_use.global_config.global_disabled");
        var globalLocal = Component.translatable("options.monitorial.not_in_use.global_config.local_disabled");
        var localLocal = Component.translatable("options.monitorial.not_in_use.local_config.local_disabled");
        if (isEditingGlobal)
            if (isGlobalUsingGlobal.getAsBoolean())
                if (isLocalUsingGlobal)
                    message = fine; // we are editing global and everyone agrees that global is the way to go
                else
                    message = globalLocal; // we are editing global but local says to not use global
            else
                message = globalGlobal; // we are editing global but global says not to use global
        else
            if (isLocalUsingGlobal)
                if (isGlobalUsingGlobal.getAsBoolean())
                    message = localLocal; // we are editing local but local says to use global and global agrees
                else
                    message = fine; // we are editing local but local says to use global, but global says to use local, so we are fine as global overrides
            else
                message = fine; // we are editing local and local says to use local
        configNotActiveText.setMessage(message.withStyle(ChatFormatting.RED));
    }

    private <T> CycleButton<T> createManualButton(String name, Function<T, Component> valueToComponent, Collection<T> values, T initialValue, CycleButton.OnValueChange<T> updateFunction) {
        CycleButton<T> button = CycleButton.builder(valueToComponent)
                .withValues(values)
                .withInitialValue(initialValue)
                .withTooltip(manualTooltip(Tooltip.create(Component.translatable("options.monitorial." + name + ".tooltip"))))
                .create(
                        -1, -1, 150, 20,
                        Component.translatable("options.monitorial." + name),
                        updateFunction
                );
        button.active = !getCurrentlyEditedConfig().automaticMode();
        return button;
    }

    public void onResize(int width, int height) {
        if (automaticMode.getValue()) {
            this.xSize.setValue(width);
            this.ySize.setValue(height);
        }
    }

    public void onMove(int x, int y) {
        //TODO: add monitor checks if we moved primarily outside the current monitor, and swap to that monitor. pos + wid/2 to get primary x pos to determine with.
        if (automaticMode.getValue()) {
            this.xPos.setValue(x);
            this.yPos.setValue(y);
        }
    }

    @Override //TODO: add 'apply' button to move the window now.
    protected void addFooter() {
        this.layout.addToFooter(
                Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                        .width(200)
                        .build(),
                layout -> layout.paddingBottom(10).alignVerticallyBottom()
        );

        this.layout.setFooterHeight(this.layout.getFooterHeight() * 2); // double footer height so our text fits
        this.layout.addToFooter(configNotActiveText = new StringWidget(400, 20,
                CommonComponents.EMPTY, font
        ), layout -> layout.paddingTop(10).alignVerticallyTop());
    }

    private static final class CustomSlider extends ExtendedSlider implements UpdatableTooltip$AlsoCycleButtonAccessor {

        @Nullable
        private Long applyDelay = null;
        private final Consumer<Double> onApply;
        private final Tooltip tooltip;

        private CustomSlider(Component prefix, double minValue, double maxValue, double currentValue, Consumer<Double> onApply, Tooltip tooltip) {
            super(-1, -1, 150, 20, prefix, PIXELS, minValue, maxValue, currentValue, 1, 1, true);
            this.onApply = onApply;
            this.tooltip = tooltip;
        }

        private void setRange(double min, double max) {
            this.minValue = min;
            this.maxValue = max;
            this.setValue(this.value); // Math.min(max, Math.max(value, min));
        }

        @NotNull
        @Override
        protected ResourceLocation getHandleSprite() {
            return this.active ? super.getHandleSprite() : DISABLED_SLIDER_HANDLE;
        }

        @Override
        public void monitorial$updateTooltip() {
            this.setTooltip(this.isActive() ? this.tooltip : Tooltip.create(BUTTON_DISABLED));
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

    private <T> OptionInstance.TooltipSupplier<T> manualTooltip(Tooltip tooltip) {
        return (T t) -> automaticMode.getValue() ? Tooltip.create(BUTTON_DISABLED) : tooltip;
    }

    private MonitorialStartupConfig getCurrentlyEditedConfig() {
        return isEditingGlobal() ? MonitorialStartupConfig.GLOBAL.get() : MonitorialStartupConfig.LOCAL;
    }

    private void updateCurrentlyEditedConfig(CycleButton<Boolean> _b, Boolean editingGlobal) {
        getCurrentlyEditedConfig().save();
        // clear the list of updatable options, then add them back. the easiest way to update all the values.
        // we don't need to change any 'which config are we editing' things because everything that checks that refers back to this button's value
        this.list.children().removeIf((ContainerObjectSelectionList.Entry<?> item) -> item.children().stream().noneMatch(b -> b == currentlyEditing));
        this.addUpdatableOptions();

        updateNotActiveWarningText(editingGlobal, () -> MonitorialStartupConfig.GLOBAL.get().useGlobalConfig(), MonitorialStartupConfig.LOCAL.useGlobalConfig());
    }

    private void updateUseGlobalConfig(CycleButton<?> _b, boolean useGlobal) {
        getCurrentlyEditedConfig().useGlobalConfig(useGlobal);
        if (isEditingGlobal())
            updateNotActiveWarningText(true, () -> useGlobal, MonitorialStartupConfig.LOCAL.useGlobalConfig());
        else
            updateNotActiveWarningText(false, () -> MonitorialStartupConfig.GLOBAL.get().useGlobalConfig(), useGlobal);
    }

    private void updateForceMove(CycleButton<?> _b, ForceMoveState state) {
        getCurrentlyEditedConfig().forceMove(state);
    }

    private void updateDefaultMonitor(CycleButton<?> _b, Optional<MonitorData> monitorData) {
        getCurrentlyEditedConfig().defaultMonitor(monitorData);
        updateSliders(monitorData);
    }


    private void updateAutomaticMode(CycleButton<?> _b, boolean automaticMode) {
        getCurrentlyEditedConfig().automaticMode(automaticMode); //TODO: when automatic mode is turned on move position sliders to automatic mode positions
        updateWidgetsActiveState(automaticMode);
    }

    private boolean isEditingGlobal() {
        return this.currentlyEditing.getValue();
    }

    @Override
    public void removed() {
        getCurrentlyEditedConfig().save();
    }
}
