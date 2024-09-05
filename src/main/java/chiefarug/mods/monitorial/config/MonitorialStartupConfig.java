package chiefarug.mods.monitorial.config;

import chiefarug.mods.monitorial.early_startup.Helpers;
import com.google.common.base.Suppliers;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.mojang.blaze3d.platform.Window;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.fml.loading.FMLPaths;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static chiefarug.mods.monitorial.early_startup.SharedData.MODID;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class MonitorialStartupConfig {
    private static final String localFilename = MODID + "-startup.json";
    private static final String globalFilename = MODID + "-global.json";

    public static final Path localLocation = FMLPaths.CONFIGDIR.get().resolve(localFilename);
    public static final Path globalLocation = SystemUtils.getUserHome().toPath().resolve(".minecraft").resolve(FMLPaths.CONFIGDIR.relative()).resolve(globalFilename);

    // TODO: add docs to this somehow. single field with url?
    private static final Codec<MonitorialStartupConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("useGlobalConfig").forGetter(MonitorialStartupConfig::useGlobalConfig),
            Codec.BOOL.fieldOf("automaticMode").forGetter(MonitorialStartupConfig::automaticMode),
            MonitorData.CODEC.optionalFieldOf("defaultMonitor").forGetter(MonitorialStartupConfig::defaultMonitor),
            ForceMoveState.CODEC.fieldOf("forceMove").forGetter(MonitorialStartupConfig::forceMove),
            Position.CODEC.fieldOf("positionOffset").forGetter(MonitorialStartupConfig::position),
            Position.CODEC.fieldOf("windowSize").forGetter(MonitorialStartupConfig::windowSize)
    ).apply(instance, MonitorialStartupConfig::new));
    private static final Logger LGGR = LogUtils.getLogger();
    static final MonitorialStartupConfig LOCAL = load(false);
    static final Supplier<MonitorialStartupConfig> GLOBAL = Suppliers.memoize(() -> load(true));

    private boolean useGlobalConfig;
    private boolean automaticMode;
    private Optional<MonitorData> defaultMonitor;
    private ForceMoveState forceMove;
    private Position position;
    private Position windowSize;

    private MonitorialStartupConfig(boolean useGlobalConfig, boolean automaticMode, Optional<MonitorData> defaultMonitor, ForceMoveState forceMove, Position position, Position windowSize) {
        this.useGlobalConfig = useGlobalConfig;
        this.automaticMode = automaticMode;
        this.defaultMonitor = defaultMonitor;
        this.forceMove = forceMove;
        this.position = position;
        this.windowSize = windowSize;
    }


    // default constructor, for use when the config was not found or failed to load.
    private MonitorialStartupConfig() {
        this(true, true, MonitorData.from(Helpers.getCurrentMonitor()), ForceMoveState.ALWAYS, new Position(0, 0), new Position(-1, -1));
        this.save();
    }

    public static boolean isUsingGlobalConfig() {
        if (LOCAL.useGlobalConfig()) {
            return GLOBAL.get().useGlobalConfig();
        }
        return false;
    }

    public static MonitorialStartupConfig getInstance() {
        if (isUsingGlobalConfig()) {
            return GLOBAL.get();
        }
        return LOCAL;
    }


    private static MonitorialStartupConfig load(boolean isGlobal) {
        Path location = getLocation(isGlobal);

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

    void save() {
        Path location = getLocation(isGlobal());
        File folder = location.toFile().getParentFile();
        if (!folder.exists() && !folder.mkdirs()) {
            LGGR.error("Failed to write {} config to {}! Couldn't create one or more folders to put the file in!", MODID, location);
            return;
        }
        try {
            var result = CODEC.encodeStart(JsonOps.INSTANCE, this)
                    .resultOrPartial(error -> LGGR.error("Failed to serialize {} config! Error message: {}", MODID, error));
            if (result.isPresent()) {
                JsonWriter writer = new JsonWriter(new BufferedWriter(new FileWriter(location.toFile())));
                writer.setIndent("\t");
                writer.setSerializeNulls(true);
                Streams.write(result.get().getAsJsonObject(), writer);
                writer.close();
                LGGR.info("Saved {} config file to {}", MODID , location);
            }
        } catch (IOException e) {
            LGGR.error("Failed to save {} config file to {}! Error message: {}", MODID, location, e);
        }
    }

    private boolean isGlobal() {
        return this != LOCAL;
    }

    static Path getLocation(boolean isGlobal) {
        return isGlobal ? globalLocation : localLocation;
    }

    public static String getPrettyLocation(boolean isGlobal) {
        if (isGlobal) return globalLocation.toString();
        return localLocation.toString();
    }

    public boolean useGlobalConfig() {return useGlobalConfig;}

    public boolean automaticMode() {return automaticMode;}

    public Optional<MonitorData> defaultMonitor() {return defaultMonitor;}

    public ForceMoveState forceMove() {return forceMove;}

    public Position position() {return position;}

    public Position windowSize() {return windowSize;}

    void useGlobalConfig(boolean newValue) {useGlobalConfig = newValue;}

    void automaticMode(boolean newValue) {automaticMode = newValue;}

    void defaultMonitor(Optional<MonitorData> newValue) {defaultMonitor = newValue;}

    void forceMove(ForceMoveState newValue) {forceMove = newValue;}

    void position(Position newValue) {position = newValue;}

    void windowSize(Position newValue) {windowSize = newValue;}

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (MonitorialStartupConfig) obj;
        return this.useGlobalConfig == that.useGlobalConfig &&
                Objects.equals(this.defaultMonitor, that.defaultMonitor) &&
                Objects.equals(this.position, that.position) &&
                Objects.equals(this.windowSize, that.windowSize) &&
                Objects.equals(this.forceMove, that.forceMove);
    }

    @Override
    public int hashCode() {
        return Objects.hash(useGlobalConfig, defaultMonitor, position, windowSize, forceMove);
    }

    @Override
    public String toString() {
        return "MonitorialStartupConfig[" +
                "useGlobalConfig=" + useGlobalConfig + ", " +
                "defaultMonitor=" + defaultMonitor + ", " +
                "position=" + position + ", " +
                "windowSize=" + windowSize + ", " +
                "forceMove=" + forceMove + ']';
    }

    public void onWindowClose(Window window) {
        defaultMonitor(MonitorData.from(Helpers.getCurrentMonitor(window)));
        windowSize(new Position(window.getWidth(), window.getHeight()));
        position(new Position(window.getX(), window.getY()));
        save();
    }
}
