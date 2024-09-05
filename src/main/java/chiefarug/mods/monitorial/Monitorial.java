package chiefarug.mods.monitorial;

import chiefarug.mods.monitorial.config.MonitorialConfigScreen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import static chiefarug.mods.monitorial.early_startup.SharedConstants.MODID;

@Mod(value = MODID, dist = Dist.CLIENT)
public class Monitorial {
    // this cant go in shared because it's a Minecraft class
    public static final ResourceLocation MODRL = ResourceLocation.fromNamespaceAndPath(MODID, MODID);

    public Monitorial(ModContainer mc) {
        mc.registerExtensionPoint(IConfigScreenFactory.class, MonitorialConfigScreen::new);
    }
}
