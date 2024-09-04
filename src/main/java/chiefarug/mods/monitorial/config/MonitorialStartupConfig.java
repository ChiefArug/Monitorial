package chiefarug.mods.monitorial.config;

import com.google.common.base.Suppliers;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.Streams;
import com.google.gson.stream.JsonWriter;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.neoforged.fml.loading.FMLPaths;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static chiefarug.mods.monitorial.MonitorialShared.MODID;

// TODO: initialize this off thread so that we don't block the main thread with two file read/writes?
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class MonitorialStartupConfig {
    private static final String filename = MODID + "-startup.json";

    public static final Path localLocation = FMLPaths.CONFIGDIR.get().resolve(filename);
    public static final Path globalLocation = SystemUtils.getUserHome().toPath().resolve(".minecraft").resolve(FMLPaths.CONFIGDIR.relative()).resolve(filename);

    // TODO: add docs to this somehow. single field with url?
    private static final Codec<MonitorialStartupConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("useGlobalConfig").forGetter(MonitorialStartupConfig::useGlobalConfig),
            MonitorData.CODEC.optionalFieldOf("defaultMonitor").forGetter(MonitorialStartupConfig::defaultMonitor),
            PositionAnchor.CODEC.fieldOf("positionAnchor").forGetter(MonitorialStartupConfig::positionAnchor),
            Position.CODEC.fieldOf("positionOffset").forGetter(MonitorialStartupConfig::position),
            Position.CODEC.optionalFieldOf("windowSize").forGetter(MonitorialStartupConfig::windowSize),
            ForceMoveState.CODEC.fieldOf("forceMove").forGetter(MonitorialStartupConfig::forceMove)
    ).apply(instance, MonitorialStartupConfig::new));
    private static final Logger LGGR = LogUtils.getLogger();
    private static final MonitorialStartupConfig LOCAL = load(localLocation);
    private static final Supplier<MonitorialStartupConfig> GLOBAL = Suppliers.memoize(() -> load(globalLocation));
    private boolean useGlobalConfig;
    private Optional<MonitorData> defaultMonitor;
    private PositionAnchor positionAnchor;
    private Position position;
    private Optional<Position> windowSize;
    private ForceMoveState forceMove;

    private MonitorialStartupConfig(boolean useGlobalConfig, Optional<MonitorData> defaultMonitor, PositionAnchor positionAnchor, Position position, Optional<Position> windowSize, ForceMoveState forceMove) {
        this.useGlobalConfig = useGlobalConfig;
        this.defaultMonitor = defaultMonitor;
        this.positionAnchor = positionAnchor;
        this.position = position;
        this.windowSize = windowSize;
        this.forceMove = forceMove;
    }


    // default constructor, for use when the config was not found or failed to load.
    private MonitorialStartupConfig() {
        this(true, Optional.empty(), PositionAnchor.DEFAULT, new Position(0, 0), Optional.empty(), ForceMoveState.ELS_ONLY);
        this.save();
    }


    public static MonitorialStartupConfig getInstance() {
        if (LOCAL.useGlobalConfig()) {
            if (GLOBAL.get().useGlobalConfig())
                return GLOBAL.get();
            return LOCAL;
        }
        return LOCAL;
    }


    private static MonitorialStartupConfig load(Path location) {
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

    boolean save() {
        try {
            var result = CODEC.encodeStart(JsonOps.INSTANCE, this)
                    .resultOrPartial(error -> LGGR.error("Failed to write {} config to {}! Error message: {}", MODID, localLocation, error));
            if (result.isPresent()) {
                JsonWriter writer = new JsonWriter(new BufferedWriter(new FileWriter(localLocation.toFile())));
                writer.setIndent("\t");
                writer.setSerializeNulls(true);
                Streams.write(result.get().getAsJsonObject(), writer);
                writer.close();
            }
        } catch (IOException e) {
            LGGR.error("Failed to save {} config file to {}! Error message: {}", MODID, localLocation, e);
            return false;
        }
        return true;
    }

    public boolean useGlobalConfig() {return useGlobalConfig;}

    public Optional<MonitorData> defaultMonitor() {return defaultMonitor;}

    public PositionAnchor positionAnchor() {return positionAnchor;}

    public Position position() {return position;}

    public Optional<Position> windowSize() {return windowSize;}

    public ForceMoveState forceMove() {return forceMove;}


    boolean useGlobalConfig(boolean newValue) {return useGlobalConfig = newValue;}

    Optional<MonitorData> defaultMonitor(Optional<MonitorData> newValue) {return defaultMonitor = newValue;}

    PositionAnchor positionAnchor(PositionAnchor newValue) {return positionAnchor = newValue;}

    Position position(Position newValue) {return position = newValue;}

    Optional<Position> windowSize(Optional<Position> newValue) {return windowSize = newValue;}

    ForceMoveState forceMove(ForceMoveState newValue) {return forceMove = newValue;}

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (MonitorialStartupConfig) obj;
        return this.useGlobalConfig == that.useGlobalConfig &&
                Objects.equals(this.defaultMonitor, that.defaultMonitor) &&
                Objects.equals(this.positionAnchor, that.positionAnchor) &&
                Objects.equals(this.position, that.position) &&
                Objects.equals(this.windowSize, that.windowSize) &&
                Objects.equals(this.forceMove, that.forceMove);
    }

    @Override
    public int hashCode() {
        return Objects.hash(useGlobalConfig, defaultMonitor, positionAnchor, position, windowSize, forceMove);
    }

    @Override
    public String toString() {
        return "MonitorialStartupConfig[" +
                "useGlobalConfig=" + useGlobalConfig + ", " +
                "defaultMonitor=" + defaultMonitor + ", " +
                "positionAnchor=" + positionAnchor + ", " +
                "position=" + position + ", " +
                "windowSize=" + windowSize + ", " +
                "forceMove=" + forceMove + ']';
    }

}
