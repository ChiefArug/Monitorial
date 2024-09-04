package chiefarug.mods.monitorial.config;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record Position(int x, int y){
    public static final Codec<Position> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("x").forGetter(Position::x),
            Codec.INT.fieldOf("y").forGetter(Position::y)
    ).apply(instance, Position::new));
}
