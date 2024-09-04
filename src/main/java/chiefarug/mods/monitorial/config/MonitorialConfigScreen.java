package chiefarug.mods.monitorial.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MonitorialConfigScreen extends OptionsSubScreen {

    private final List<AbstractWidget> OPTIONS = List.of(
            Button.builder(Component.translatable("options.monitorial.use_global_config"), button -> System.out.println("hello")).build()
    );

    public MonitorialConfigScreen(ModContainer _mod, Screen parent) {
        super(parent, Minecraft.getInstance().options, Component.translatable("options.monitorial.config_title"));
    }

    @Override
    @SuppressWarnings("DataFlowIssue") // list is set directly before this method is called
    protected void addOptions() {
        this.list.addSmall(OPTIONS);
    }
}
