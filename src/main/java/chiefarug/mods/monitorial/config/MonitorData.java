package chiefarug.mods.monitorial.config;

import chiefarug.mods.monitorial.mixin.ScreenManagerAccessor;
import chiefarug.mods.monitorial.mixin.WindowAccessor;
import com.mojang.blaze3d.platform.Monitor;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;

import static chiefarug.mods.monitorial.early_startup.Helpers.getDistanceSqrd;

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
    private final long id;

    public MonitorData(String name, int x, int y) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.id = getMonitors().stream()
                .filter(monitor -> this.name.equals(GLFW.glfwGetMonitorName(monitor.getMonitor())))
                .min(Comparator.comparingInt(m1 -> getDistanceSqrd(m1.getX(), x, m1.getY(), y)))
                .map(Monitor::getMonitor)
                .orElse(0L);
    }

    public MonitorData(String name, int x, int y, long id) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.id = id;
    }

    public String name() {return name;}

    public int x() {return x;}

    public int y() {return y;}

    public long id() {return id;}

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

    private Collection<Monitor> getMonitors() { ///mmmm juicy casting
        return ((ScreenManagerAccessor) ((WindowAccessor) ((Object) Minecraft.getInstance().getWindow())).monitorial$getScreenManager()).monitorial$getMonitors().values();
    }

}
