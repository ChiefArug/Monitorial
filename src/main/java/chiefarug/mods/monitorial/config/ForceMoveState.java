package chiefarug.mods.monitorial.config;

import com.mojang.serialization.Codec;
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

    public abstract boolean shouldAttemptMove();
}
