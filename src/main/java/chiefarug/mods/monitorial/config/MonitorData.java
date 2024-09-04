package chiefarug.mods.monitorial.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record MonitorData(String name, int x, int y) {
    public static final Codec<MonitorData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("name").forGetter(MonitorData::name),
                    Codec.INT.fieldOf("x").forGetter(MonitorData::x),
                    Codec.INT.fieldOf("y").forGetter(MonitorData::y)
            ).apply(instance, MonitorData::new));
}
