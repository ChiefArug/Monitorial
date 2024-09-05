package chiefarug.mods.monitorial.config;

import chiefarug.mods.monitorial.early_startup.Helpers;
import com.mojang.blaze3d.platform.Monitor;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.lwjgl.glfw.GLFW;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongSupplier;

public final class MonitorData {
    public static final Codec<MonitorData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("name").forGetter(MonitorData::name),
                    Codec.INT.fieldOf("x").forGetter(MonitorData::x),
                    Codec.INT.fieldOf("y").forGetter(MonitorData::y)
            ).apply(instance, MonitorData::new));
    private final String name;
    private final int x;
    private final int y;
    private final LongSupplier id;

    public MonitorData(String name, int x, int y) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.id = () -> Helpers.getMonitors().values().stream()
                .filter(monitor -> this.name.equals(GLFW.glfwGetMonitorName(monitor.getMonitor())))
                .min(Comparator.comparingInt(m1 -> this.getDistanceSqrdTo(m1.getX(), m1.getY())))
                .map(Monitor::getMonitor)
                .orElse(0L);
    }

    public MonitorData(String name, int x, int y, long id) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.id = () -> id;
    }

    public static Optional<MonitorData> from(Monitor mon) {
        return Optional.of(new MonitorData(GLFW.glfwGetMonitorName(mon.getMonitor()), mon.getX(), mon.getY(), mon.getMonitor()));
    }

    public int getDistanceSqrdTo(int x2, int y2) {
        return (x - x2) * (x - x2) + (y - y2) * (y - y2);
    }

    public String name() {return name;}

    public int x() {return x;}

    public int y() {return y;}

    public long id() {return id.getAsLong();}

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (MonitorData) obj;
        return Objects.equals(this.name, that.name) &&
                this.x == that.x &&
                this.y == that.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, x, y);
    }

    @Override
    public String toString() {
        return "Monitor[" +
                "name=" + name + ", " +
                "x=" + x + ", " +
                "y=" + y + ']';
    }

}
