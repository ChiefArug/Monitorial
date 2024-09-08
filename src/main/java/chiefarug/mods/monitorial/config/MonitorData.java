package chiefarug.mods.monitorial.config;

import chiefarug.mods.monitorial.early_startup.Helpers;
import com.mojang.blaze3d.platform.Monitor;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    public static Map<MonitorData, String> DISPLAY_NAMES = new HashMap<>();

    public static List<Optional<MonitorData>> getMonitors() {
        DISPLAY_NAMES.clear();
        ArrayList<Optional<MonitorData>> optionals = new ArrayList<>(List.of(Optional.empty()));
        // this is sorted so that numbered names apply properly.
        // sort by y first then x, so it goes from top left to top right to bottom left to bottom right
        Map<String, Integer> names = new HashMap<>();
        Helpers.getMonitors().values().stream().sorted(Comparator.comparingInt(Monitor::getY).thenComparingInt(Monitor::getX)).forEach(monitor -> {
            String orignalName = GLFW.glfwGetMonitorName(monitor.getMonitor());
            orignalName = orignalName == null ? "Unknown" : orignalName;
            String displayName = orignalName;
            int number = names.compute(orignalName, (k, v) -> v == null ? 1 : ++v);
            if (number > 1)
                displayName = String.format("%s (%s)", orignalName, number);

            MonitorData data = new MonitorData(orignalName, monitor.getX(), monitor.getY(), monitor.getMonitor());
            DISPLAY_NAMES.put(data, displayName);
            optionals.add(Optional.of(data));
        });
        return optionals;
    }

    // transient fields are ignored for equality checks
    private final String name;
    private final int x;
    private final int y;
    private final transient LongSupplier id;

    private MonitorData(String name, int x, int y) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.id = () -> Helpers.getMonitors().values().stream()
                .filter(monitor -> this.name.equals(GLFW.glfwGetMonitorName(monitor.getMonitor())))
                .min(Comparator.comparingInt(m1 -> this.getDistanceSqrdTo(m1.getX(), m1.getY())))
                .map(Monitor::getMonitor)
                .orElse(0L);
    }

    MonitorData(String name, int x, int y, long id) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.id = () -> id;
    }

    public static MonitorData from(Monitor mon) {
        String name = GLFW.glfwGetMonitorName(mon.getMonitor());
        return new MonitorData(name, mon.getX(), mon.getY(), mon.getMonitor());
    }

    public int getDistanceSqrdTo(int x2, int y2) {
        return (x - x2) * (x - x2) + (y - y2) * (y - y2);
    }

    public String name() {return name;}

    public int x() {return x;}

    public int y() {return y;}

    public long id() {return id.getAsLong();}

    public String displayName() {return DISPLAY_NAMES.get(this);}

    public Component toComponent() {
        return Component.literal(displayName());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof MonitorData that)) return false;
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
