package chiefarug.mods.monitorial.config;

import com.mojang.serialization.Codec;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.loading.FMLConfig;

import java.util.Locale;

@SuppressWarnings("unused")
public enum ForceMoveState {
    NEVER {
        public boolean shouldAttemptMove() {
            return false;
        }
    },
    ELS_ONLY {
        public boolean shouldAttemptMove() {
            return FMLConfig.getBoolConfigValue(FMLConfig.ConfigValue.EARLY_WINDOW_CONTROL);
        }
    },
    ALWAYS {
        public boolean shouldAttemptMove() {
            return true;
        }
    };
    public static final Codec<ForceMoveState> CODEC = Codec.STRING.xmap(s -> ForceMoveState.valueOf(s.toUpperCase(Locale.ROOT)), Enum::name);

    private final Component component;

    ForceMoveState() {
        this.component = Component.translatable("options.monitorial.force_move." + this.name().toLowerCase(Locale.ROOT));
    }

    public Component toComponent() {
        return component;
    }

    public abstract boolean shouldAttemptMove();
}
