package chiefarug.mods.monitorial;

import chiefarug.mods.monitorial.config.MonitorialConfigScreen;
import chiefarug.mods.monitorial.mixin.ScreenAccessor;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

import static chiefarug.mods.monitorial.MonitorialShared.MODID;

@Mod(value = MODID, dist = Dist.CLIENT)
public class Monitorial {
    // this cant go in shared because it's a Minecraft class
    public static final ResourceLocation MODRL = ResourceLocation.fromNamespaceAndPath(MODID, MODID);

    public Monitorial(ModContainer mc){
        mc.registerExtensionPoint(IConfigScreenFactory.class, MonitorialConfigScreen::new);
    }
}
