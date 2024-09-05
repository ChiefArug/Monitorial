package chiefarug.mods.monitorial.config;

import chiefarug.mods.monitorial.early_startup.Helpers;
import chiefarug.mods.monitorial.mixin.UpdatableTooltip$AlsoCycleButtonAccessor;
import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
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
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static chiefarug.mods.monitorial.Monitorial.MODRL;
import static chiefarug.mods.monitorial.config.MonitorialStartupConfig.*;

@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "LoggingSimilarMessage"})
public class MonitorialConfigScreen extends OptionsSubScreen {

    static final Component GLOBAL = Component.translatable("options.monitorial.global_config");
    static final Component LOCAL = Component.translatable("options.monitorial.local_config");
    static final Component BUTTON_DISABLED = Component.translatable("options.monitorial.disabled_automatic");
    static final Component COLON = Component.literal(": ");
    static final Component PIXELS = Component.translatable("options.monitorial.pixel_postfix");
    public static final ResourceLocation DISABLED_SLIDER_HANDLE = MODRL.withPath("widget/slider_disabled");
    private List<AbstractWidget> manualWidgets = List.of();
    private CycleButton<Boolean> automaticMode;
    private CycleButton<Optional<MonitorData>> defaultMonitor;
    private CustomSlider xPos;
    private CustomSlider yPos;
    private CustomSlider xSize;
    private CustomSlider ySize;
    private StringWidget configNotActiveText;

    private CycleButton<Boolean> currentlyEditing;

    private Button stealConfiguration;


    public MonitorialConfigScreen(ModContainer ignored, Screen parent) {
        super(parent, Minecraft.getInstance().options, Component.translatable("options.monitorial.config_title"));
    }

    @SuppressWarnings("OptionalIsPresent")
    private static Component getMonitorString(Optional<MonitorData> monitor) {
        return monitor.isEmpty() ?
                Component.translatable("options.monitorial.monitor_data.primary", GLFW.glfwGetMonitorName(GLFW.glfwGetPrimaryMonitor())) :
                Component.literal(monitor.get().name());
    }


    private static final Collector<Optional<MonitorData>, ?, ArrayList<Optional<MonitorData>>> monitorCollector = Collectors.toCollection(() -> new ArrayList<>(List.of(Optional.empty())));
    private static List<Optional<MonitorData>> getMonitorData() {
        return Helpers.getMonitors().values().stream()
                        .map(MonitorData::from)
                        .collect(monitorCollector);
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

    @SuppressWarnings("DataFlowIssue") // list is set directly before this method is called
    protected void addUpdatableOptions() {
        MonitorialStartupConfig config = getCurrentlyEditedConfig();
        // this is safe as list is initted just before this method is called
        this.list.addSmall(List.of(
                CycleButton.onOffBuilder(config.useGlobalConfig())
                        .withTooltip(bool -> Tooltip.create(Component.translatable("options.monitorial.use_global_config.tooltip")))
                        .create(-1, -1, 150, 20,
                                Component.translatable("options.monitorial.use_global_config"), this::updateUseGlobalConfig),
                automaticMode = CycleButton.onOffBuilder(getCurrentlyEditedConfig().automaticMode())
                        .withTooltip(bool -> Tooltip.create(Component.translatable("options.monitorial.automatic_mode.tooltip")))
                        .create(-1, -1, 150, 20,
                                Component.translatable("options.monitorial.automatic_mode"), this::updateAutomaticMode)
        ));
        Position monitorSize = Helpers.getMonitorSize(config.defaultMonitor());
        int sliderMaxX = monitorSize.x();
        int sliderMaxY = monitorSize.y();
        this.list.addSmall(manualWidgets = List.of(
                defaultMonitor = this.createButton("default_monitor", //NOTE: the list of values for this button needs to be recreated each time the screen opened in case a monitor got added/removed
                        MonitorialConfigScreen::getMonitorString,
                        getMonitorData(),
                        config.defaultMonitor(),
                        this::updateDefaultMonitor
                ),
                this.createButton("force_move",
                        ForceMoveState::toComponent,
                        List.of(ForceMoveState.values()),
                        config.forceMove(),
                        this::updateForceMove, true
                ), // the values and ranges for these will be set at the end in updateSliders()
                xPos = new CustomSlider(Component.translatable("options.monitorial.x_pos").append(COLON), 0, sliderMaxX, config.position().x(),
                        this::updateXPos, Tooltip.create(Component.translatable("options.monitorial.x_pos.tooltip"))),
                yPos = new CustomSlider(Component.translatable("options.monitorial.y_pos").append(COLON), 0, sliderMaxY, config.position().y(),
                        this::updateYPos, Tooltip.create(Component.translatable("options.monitorial.y_pos.tooltip"))),
                xSize = new CustomSlider(Component.translatable("options.monitorial.x_size").append(COLON), 0, sliderMaxX, config.windowSize().x(),
                        this::updateWidth, Tooltip.create(Component.translatable("options.monitorial.x_size.tooltip"))),
                ySize = new CustomSlider(Component.translatable("options.monitorial.y_size").append(COLON), 0, sliderMaxY, config.windowSize().y(),
                        this::updateHeight, Tooltip.create(Component.translatable("options.monitorial.y_size.tooltip")))
        ));

//        Window window = Minecraft.getInstance().getWindow();
//        Monitor monitor1 = Helpers.getCurrentMonitor();
//        setPosition(window.getX() - monitor1.getX(), window.getY() - monitor1.getY());
//        setSize(monitor1.getCurrentMode().getWidth(), monitor1.getCurrentMode().getHeight());
//        setSize(window.getWidth(), window.getHeight());
    }

    @Override
    protected void addFooter() {
        this.layout.setFooterHeight(78); // triple footer height so our text and buttons fit

        this.layout.addToFooter(
                Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                        .width(230)
                        .build(),
                layout -> layout.paddingBottom(5).alignVerticallyBottom()
        );
        this.layout.addToFooter(configNotActiveText = new StringWidget(400, 20,
                CommonComponents.EMPTY, font
        ), layout -> layout.paddingTop(7).alignVerticallyTop());
        this.layout.addToFooter(stealConfiguration = Button.builder(Component.translatable("options.monitorial.set_current_size_and_position"), b -> stealConfiguration())
                        .size(110, 20)
                        .build(DisablableButton::new),
                layout -> layout.alignHorizontallyCenter().paddingRight(120).alignVerticallyMiddle()
        );
        this.layout.addToFooter(Button.builder(Component.translatable("options.monitorial.apply_changes_to_window"), this::applyChangesToWindow)
                        .size(110,20)
                        .build(),
                layout -> layout.alignHorizontallyCenter().paddingRight(-120).alignVerticallyMiddle()
        );
    }

    @Override
    protected void init() {
        super.init();
        this.manualWidgets = List.of(defaultMonitor, xPos, yPos, xSize, ySize, stealConfiguration);
        updateWidgetsActiveState(getCurrentlyEditedConfig().automaticMode());
        if (automaticMode.getValue())
            stealConfiguration();
    }

    private void updateSliders(Optional<MonitorData> monitorData) {
        long monitor = monitorData.map(MonitorData::id).orElseGet(GLFW::glfwGetPrimaryMonitor);
        if (monitor == 0) { // in theory impossible
            Helpers.LOGGER.error("Couldn't find monitor {}, which is strange because it was present when the configuration screen opened...", monitorData);
            return;
        }
        Position monitorSize = Helpers.getMonitorSize(monitor);
        xPos.setRange(0, monitorSize.x());
        yPos.setRange(0, monitorSize.y());
        xSize.setRange(0, monitorSize.x());
        ySize.setRange(0, monitorSize.y());
//        if (!automaticMode.getValue()) return;

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

    private <T> OptionInstance.TooltipSupplier<T> manualTooltip(Tooltip tooltip) {
        return (T t) -> automaticMode.getValue() ? Tooltip.create(BUTTON_DISABLED) : tooltip;
    }

    private MonitorialStartupConfig getCurrentlyEditedConfig() {
        return isEditingGlobal() ? MonitorialStartupConfig.GLOBAL.get() : MonitorialStartupConfig.LOCAL;
    }

    @SuppressWarnings("DataFlowIssue") // list is always set when buttons exist, and this is only called from buttons
    private void updateCurrentlyEditedConfig(CycleButton<Boolean> _b, Boolean editingGlobal) {
        MonitorialStartupConfig config = editingGlobal ? MonitorialStartupConfig.GLOBAL.get() : MonitorialStartupConfig.LOCAL;
        config.save(editingGlobal);
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
        getCurrentlyEditedConfig().automaticMode(automaticMode);
        if (automaticMode)
            setDefaultMonitor(Helpers.getCurrentMonitor());
        if (this.automaticMode.getValue()) {
            Window window = Minecraft.getInstance().getWindow();
            Monitor monitor = Helpers.getCurrentMonitor();
            setPosition(window.getX() - monitor.getX(), window.getY() - monitor.getY());
            setSize(monitor.getCurrentMode().getWidth(), monitor.getCurrentMode().getHeight());
            setSize(window.getWidth(), window.getHeight());
        }
        updateWidgetsActiveState(automaticMode);
    }

    private void updateXPos(Double value) {getCurrentlyEditedConfig().position(getCurrentlyEditedConfig().position().withX(value.intValue()));}

    private void updateYPos(Double value) {getCurrentlyEditedConfig().position(getCurrentlyEditedConfig().position().withY(value.intValue()));}

    private void updateWidth(Double value) {getCurrentlyEditedConfig().windowSize(getCurrentlyEditedConfig().windowSize().withX(value.intValue()));}

    private void updateHeight(Double value) {getCurrentlyEditedConfig().windowSize(getCurrentlyEditedConfig().windowSize().withY(value.intValue()));}

    private void applyChangesToWindow(Button b) {
        MonitorialStartupConfig config = getCurrentlyEditedConfig();
        config.save(isEditingGlobal());

        Monitor monitor = config.defaultMonitor()
                .map(d -> Helpers.getMonitor(d.id()))
                .orElseGet(Helpers::getPrimaryMonitor);
        long window = Minecraft.getInstance().getWindow().getWindow();
        Position position = config.position();
        Position size = config.windowSize();
        boolean moveSucceeded  = Helpers.forceUpdatePos(monitor, window, position.x(), position.y());
        boolean resizeSucceeded = Helpers.forceUpdateSize(window, size.x(), size.y());
        if (!moveSucceeded) //TODO: custom button class to reset message after 5~10 seconds?
            if (!resizeSucceeded)
                b.setMessage(Component.translatable("options.monitorial.apply_changes_to_window.move_and_resize_failed"));
            else
                b.setMessage(Component.translatable("options.monitorial.apply_changes_to_window.move_failed"));
        else if (!resizeSucceeded)
            b.setMessage(Component.translatable("options.monitorial.apply_changes_to_window.resize_failed"));
    }

    private boolean isEditingGlobal() {
        return this.currentlyEditing.getValue();
    }

    @SuppressWarnings("SameParameterValue")
    private <T> CycleButton<T> createButton(String name, Function<T, Component> valueToComponent, Collection<T> values, T initialValue, CycleButton.OnValueChange<T> updateFunction) {
        return createButton(name, valueToComponent, values, initialValue, updateFunction, false);
    }

    private <T> CycleButton<T> createButton(String name, Function<T, Component> valueToComponent, Collection<T> values, T initialValue, CycleButton.OnValueChange<T> updateFunction, boolean alwaysActive) {
        Tooltip tp = Tooltip.create(Component.translatable("options.monitorial." + name + ".tooltip"));

        CycleButton<T> button = CycleButton.builder(valueToComponent)
                .withValues(values)
                .withInitialValue(initialValue)
                .withTooltip(alwaysActive ? (_i) -> tp : manualTooltip(tp))
                .create(
                        -1, -1, 150, 20,
                        Component.translatable("options.monitorial." + name),
                        updateFunction
                );
        button.active = alwaysActive || !getCurrentlyEditedConfig().automaticMode();
        return button;
    }

    public void onResize() {
        if (automaticMode.getValue()) {
            stealConfiguration();
//            Monitor monitor = Helpers.getCurrentMonitor();
//            setDefaultMonitor(monitor);
//            setSize(monitor.getCurrentMode().getWidth(), monitor.getCurrentMode().getHeight());
        }
    }

    public void onMove() {
        if (automaticMode.getValue()) {
            stealConfiguration();
//            Window window = Minecraft.getInstance().getWindow();
//            Monitor monitor = Helpers.getCurrentMonitor();
//            setDefaultMonitor(monitor);
//            setPosition(window.getX() - monitor.getX(), window.getY() - monitor.getY());
        }
    }

    private void stealConfiguration() {
        Window window = Minecraft.getInstance().getWindow();
        Monitor monitor = Helpers.getCurrentMonitor();
        setDefaultMonitor(monitor);
        setPosition(window.getX() - monitor.getX(), window.getY() - monitor.getY());
        setSize(window.getWidth(), window.getHeight());
    }

    private void setDefaultMonitor(Monitor monitor) {
        Optional<MonitorData> monitorData = MonitorData.from(monitor);
        defaultMonitor.setValue(monitorData);
        updateDefaultMonitor(defaultMonitor, monitorData);
    }

    private void setPosition(double x, double y) {
        xPos.setValue(x);
        updateXPos(x);
        yPos.setValue(y);
        updateYPos(y);
    }

    private void setSize(int width, int height) {
        this.xSize.setValue(width);
        updateWidth((double) width);
        this.ySize.setValue(height);
        updateHeight((double) height);
    }


    @Override
    public void removed() {
        getCurrentlyEditedConfig().save(isEditingGlobal());
    }

    private static class DisablableButton extends Button implements UpdatableTooltip$AlsoCycleButtonAccessor {
        private final Tooltip originalTooltip;
        public DisablableButton(Builder builder) {
            super(builder);
            this.originalTooltip = getTooltip();
        }

        @Override
        public void monitorial$updateTooltip() {
            setTooltip(this.active ? originalTooltip : Tooltip.create(BUTTON_DISABLED));
        }
    }

}
