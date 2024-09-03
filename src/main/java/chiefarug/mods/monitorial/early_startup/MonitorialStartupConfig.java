package chiefarug.mods.monitorial.early_startup;

import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.fml.loading.FMLConfig;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

import static chiefarug.mods.monitorial.MonitorialConstants.MODID;
import static chiefarug.mods.monitorial.early_startup.MonitorialStartupConfig.Anchor.Snap.*;

public record MonitorialStartupConfig(Optional<MonitorData> defaultMonitor, Anchor anchorPosition, ForceMoveState forceMove) {
    public static final Path location = FMLPaths.CONFIGDIR.get().resolve(MODID + "-startup.json");
    private static final Codec<MonitorialStartupConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MonitorData.CODEC.optionalFieldOf("defaultMonitor").forGetter(MonitorialStartupConfig::defaultMonitor),
            Anchor.CODEC.fieldOf("anchorPosition").forGetter(MonitorialStartupConfig::anchorPosition),
            ForceMoveState.CODEC.fieldOf("forceMove").forGetter(MonitorialStartupConfig::forceMove)
    ).apply(instance, MonitorialStartupConfig::new));
    private static final Logger LGGR = LogUtils.getLogger();
    public static MonitorialStartupConfig INSTANCE = load();


    public enum Anchor {
        TOP_LEFT(MIN, MIN),         TOP_MIDDLE(MIN, MIDDLE),    TOP_RIGHT(MIN, MAX),
        MIDDLE_LEFT(MIDDLE, MIN),   CENTRE(MIDDLE, MIDDLE),     MIDDLE_RIGHT(MIDDLE, MAX),
        BOTTOM_LEFT(MAX, MIN),      BOTTOM_MIDDLE(MAX, MIDDLE), BOTTOM_RIGHT(MAX, MAX),
        DEFAULT(null, null);
        enum Snap {
            MIN { int get(int max) {
                return 0;
            }},
            MIDDLE { int get(int max) {
                return max / 2;
            }},
            MAX { int get(int max) {
                return max;
            }};
            abstract int get(int max);
        }
        private final Snap x, y;
        Anchor(Snap x, Snap y) {
            this.x = x;
            this.y = y;
        }
        public int getAbsX(int minX, int maxX) {
            return minX + x.get(maxX - minX);
        }
        public int getAbsY(int minY, int maxY) {
            return minY + y.get(maxY - minY);
        }

        public static final Codec<Anchor> CODEC = Codec.STRING.xmap(s -> Anchor.valueOf(s.toUpperCase(Locale.ROOT)), Enum::name);
    }

    public enum ForceMoveState {
        NEVER { public boolean shouldAttemptMove() {
            return false;
        }},
        ELS_ONLY { public boolean shouldAttemptMove() {
            return FMLConfig.getBoolConfigValue(FMLConfig.ConfigValue.EARLY_WINDOW_CONTROL);
        }},
        ALWAYS { public boolean shouldAttemptMove() {
            return true;
        }};
        public static final Codec<ForceMoveState> CODEC = Codec.STRING.xmap(s -> ForceMoveState.valueOf(s.toUpperCase(Locale.ROOT)), Enum::name);
        public abstract boolean shouldAttemptMove();
    }

    // default constructor, for use when the config was not found or failed to load.
    private MonitorialStartupConfig() {
        this(Optional.empty(), Anchor.DEFAULT, ForceMoveState.ELS_ONLY);
        this.save();
    }

    private static MonitorialStartupConfig load() {
        try {
            return CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(Files.readString(location, StandardCharsets.UTF_8)).getAsJsonObject())
                    .resultOrPartial(error -> LGGR.error("Failed to parse {} config from {}! Overwriting with default. Error message: {}", MODID, location, error))
                    .orElseGet(MonitorialStartupConfig::new);
        } catch (NoSuchFileException ignored) { // the file was deleted or never made in the first place, silently generate a new one.
        } catch (IOException e) {
            LGGR.error("Failed to load {} config file from {}! Overwriting with default. Error message: {}", MODID, location, e);
        } catch (JsonSyntaxException e) {
            LGGR.error("Failed to parse {} config file from {}! Overwriting with default. Error message: {}", MODID, location, e);
        }
        return new MonitorialStartupConfig();
    }

    private void save() {
        try {
            var result = CODEC.encodeStart(JsonOps.INSTANCE, this)
                    .resultOrPartial(error -> LGGR.error("Failed to write {} config to {}! Error message: {}", MODID, location, error));
            if (result.isPresent()) {
                JsonWriter writer = new JsonWriter(new BufferedWriter(new FileWriter(location.toFile())));
                writer.setIndent("\t");
                writer.setSerializeNulls(true);
                Streams.write(result.get().getAsJsonObject(), writer);
                writer.close();
            }
        } catch (IOException e) {
            LGGR.error("Failed to save {} config file to {}! Error message: {}", MODID, location, e);
        }
    }
}
