package chiefarug.mods.monitorial.early_startup;

import chiefarug.mods.monitorial.config.MonitorData;
import chiefarug.mods.monitorial.config.MonitorialStartupConfig;
import chiefarug.mods.monitorial.config.Position;
import chiefarug.mods.monitorial.mixin.ScreenManagerAccessor;
import chiefarug.mods.monitorial.mixin.WindowAccessor;
import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.ScreenManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
import net.minecraft.client.Minecraft;
import net.neoforged.fml.loading.FMLConfig;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static chiefarug.mods.monitorial.config.ForceMoveState.NEVER;
import static org.lwjgl.glfw.GLFW.GLFW_FEATURE_UNAVAILABLE;
import static org.lwjgl.glfw.GLFW.GLFW_NO_ERROR;
import static org.lwjgl.glfw.GLFW.glfwGetError;

public class Helpers {
    public static final Logger LOGGER = LogUtils.getLogger();


    public static Monitor getBestGLFWMonitorCode(Supplier<Monitor> defaultPrimary, ObjectCollection<Monitor> monitors) {
        if (monitors.size() == 1) {
            SharedData.hasMultipleMonitors = false;
            return defaultPrimary.get(); // silently return if there is only one monitor. we don't need to do anything.
        }

        if (FMLConfig.getBoolConfigValue(FMLConfig.ConfigValue.EARLY_WINDOW_CONTROL) && MonitorialStartupConfig.getInstance().forceMove() == NEVER) {
            LOGGER.warn("earlyWindowControl in config/fml.toml is enabled and forceMove in config/monitorial-startup.json is set to NEVER so Monitorial has been effectively disabled!");
            return defaultPrimary.get(); // in theory running the code after this is fine, but the logging messages will be confusing
        }

        Optional<MonitorData> configured = MonitorialStartupConfig.getInstance().defaultMonitor();
        if (configured.isEmpty()) {
            LOGGER.info("Monitorial is not configured, using default behaviour (primary monitor)");
            LOGGER.info("To configure use the in game configuration screen, through the mods button! In case you want to configure manually, here are your monitors:");
            for (Monitor monitor : monitors)
                LOGGER.info("\t {\"name\": {}, \"x\": {}, \"y\": {}},", GLFW.glfwGetMonitorName(monitor.getMonitor()), monitor.getX(), monitor.getY());
            return defaultPrimary.get();
        }
        MonitorData preferred = configured.get();


        int len = monitors.size();
        record NamedDistancedMonitor(int distSqrd, Monitor monitor, String name) {}
        List<NamedDistancedMonitor> distancedMonitors = new ArrayList<>(len);
        List<NamedDistancedMonitor> matches = new ArrayList<>(Math.min(3, len)); // 3 is typically the maximum amount of monitors people have of the same type

        for (Monitor monitor : monitors) {
            int x = monitor.getX(),
                y = monitor.getY();
            String name = GLFW.glfwGetMonitorName(monitor.getMonitor());

//            LOGGER.info("Monitorial found a monitor named '{}' at x: {}, y: {}", name, x, y);

            NamedDistancedMonitor distanced = new NamedDistancedMonitor(preferred.getDistanceSqrdTo(x, y), monitor, name);
            distancedMonitors.add(distanced);
            if (preferred.name().equals(name))
                matches.add(distanced);

        }
        if (matches.size() == 1) {
            LOGGER.debug("Monitorial found exactly one monitor that matched the configured name '{}'", preferred.name());
            NamedDistancedMonitor monitor = matches.getFirst();
            return monitor.monitor;
        }
        if (matches.isEmpty()) {
            LOGGER.debug("Monitorial couldn't find any monitors that match the configured name of {}. Searching by position instead.", preferred.name());
            matches = distancedMonitors;
        }

        return matches.stream()
                .min(Comparator.comparingInt(NamedDistancedMonitor::distSqrd))
                .map(d -> d.monitor)
                .orElseGet(defaultPrimary);
    }

    /**
     * @return If the move operation succeeded.
     */
    public static boolean forceUpdatePos(Monitor monitor, long window, int relativeX, int relativeY) {
        int x = monitor.getX() + relativeX;
        int y = monitor.getY() + relativeY;

        String monitorName = GLFW.glfwGetMonitorName(monitor.getMonitor());
        LOGGER.info("Attempting to force move window to relative position {}, {} on monitor {} (located at x: {}, y: {})", x, y, monitorName, monitor.getX(), monitor.getY());
        GLFW.glfwSetWindowPos(window, x, y);
        try (MemoryStack memorystack = MemoryStack.stackPush()) {
            PointerBuffer pointerbuffer = memorystack.mallocPointer(1);
            int error = glfwGetError(pointerbuffer);
            switch (error) {
                case GLFW_NO_ERROR -> {
                    LOGGER.info("Monitorial successfully moved the game window to monitor '{}'!", monitorName);
                    return true;
                }
                case GLFW_FEATURE_UNAVAILABLE -> LOGGER.error("Monitorial failed to move the window, your window manager does not support moving windows!");
                default -> {
                    long pDescription = pointerbuffer.get();
                    String description = pDescription == 0L ? "" : MemoryUtil.memUTF8(pDescription);
                    LOGGER.error("Monitorial failed to move the window for an unknown reason. Suppressing GLFW error [{}]: {}", error, description);
                }
            }
            return false;
        }
    }

    /**
     * @return If the resize operation succeeded. If both the new width and new height are invalid will noop and return true.
     */
    public static boolean forceUpdateSize(long window, int newWidth, int newHeight) {
        if (newWidth == -1 && newHeight == -1) return true;
        int w, h;
        if (newWidth <= 0 || newHeight <= 0) {
            int[] wC = new int[1],
                  hC = new int[1];
            GLFW.glfwGetWindowSize(window, wC, hC);
            w = wC[0];
            h= hC[0];
        } else {
            w = newWidth;
            h = newHeight;
        }
        LOGGER.info("Attempting to force resize window to size {}x{}", w, h);
        GLFW.glfwSetWindowSize(window, w, h);
        try (MemoryStack memorystack = MemoryStack.stackPush()) {
            PointerBuffer pointerbuffer = memorystack.mallocPointer(1);
            int error = glfwGetError(pointerbuffer);
            switch (error) {
                case GLFW_NO_ERROR -> {
                    LOGGER.info("Monitorial successfully resized the game window!");
                    return true;
                }
                case GLFW_FEATURE_UNAVAILABLE -> LOGGER.error("Monitorial failed to resize the window, your window manager does not support resizing windows!");
                default -> {
                    long pDescription = pointerbuffer.get();
                    String description = pDescription == 0L ? "" : MemoryUtil.memUTF8(pDescription);
                    LOGGER.error("Monitorial failed to resize the window for an unknown reason. Suppressing GLFW error [{}]: {}", error, description);
                }
            }
            return false;
        }
    }

    public static Position getMonitorSize(long monitor) {
        return getMonitorSize(getMonitor(monitor));
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static Position getMonitorSize(Optional<MonitorData> data) {
        return getMonitorSize(data.map(MonitorData::id).orElseGet(GLFW::glfwGetPrimaryMonitor));
    }

    public static Position getMonitorSize(Monitor monitor) {
        var vidMode = monitor.getCurrentMode();
        int x = vidMode.getWidth();
        int y = vidMode.getHeight();
        return new Position(x, y);
    }


    public static Long2ObjectMap<Monitor> getMonitors() {
        return getMonitors(Minecraft.getInstance().getWindow());
    }

    public static Long2ObjectMap<Monitor> getMonitors(Window window) {
        return getMonitors(((WindowAccessor) ((Object) window)).monitorial$getScreenManager());
    }

    public static Long2ObjectMap<Monitor> getMonitors(ScreenManager manager) {
        return ((ScreenManagerAccessor) manager).monitorial$getMonitors();
    }

    public static Monitor getMonitor(long id) {
        return getMonitor(id, Minecraft.getInstance().getWindow());
    }

    public static Monitor getMonitor(long id, Window window) {
        return getMonitor(id, ((WindowAccessor) ((Object) window)).monitorial$getScreenManager());
    }

    public static Monitor getMonitor(long id, ScreenManager manager) {
        return manager.getMonitor(id);
    }

    public static Monitor getPrimaryMonitor() {
        return getMonitor(GLFW.glfwGetPrimaryMonitor());
    }

    // If the middle of the monitor is on a monitor then use that
    // Otherwise we use the one the monitor is most on based on overlap area
    public static Monitor getCurrentMonitor(Window window) {
        Position windowTopLeft = new Position(window.getX(), window.getY());
        Position middle = new Position(window.getX() + window.getWidth() / 2, window.getY() + window.getHeight() / 2);
        Position windowBottomRight = new Position(window.getX() + window.getWidth(), window.getY() + window.getWidth());

        int maxArea = Integer.MIN_VALUE;
        Monitor bestMatch = null;
        for (Monitor monitor : getMonitors(window).values()) {
            Position monitorTopLeft = new Position(monitor.getX(), monitor.getY());
            Position monitorBottomRight = new Position(monitor.getX() + monitor.getCurrentMode().getWidth(), monitor.getY() + monitor.getCurrentMode().getHeight());
            if (isInsideArea(middle, monitorTopLeft, monitorBottomRight))
                return monitor;
            int overlap = getOverlap(windowTopLeft, windowBottomRight, monitorTopLeft, monitorBottomRight);
            if (overlap > maxArea)
                bestMatch = monitor;
        }
        if (bestMatch == null) throw new IllegalStateException("Somehow you don't have any monitors connected?");
        return bestMatch;
    }

    private static boolean isInsideArea(Position target, Position r1, Position r2) {
        return target.x() >= r1.x() &&
                target.y() >= r1.y() &&
                target.x() < r2.x() &&
                target.y() < r2.y();
    }

    private static int getOverlap(Position r1, Position r2, Position l1, Position l2) {
        int x = Math.min(r1.x(), r2.x()) - Math.max(l1.x(), l2.x());
        int y = Math.min(r1.y(), r2.y()) - Math.max(l1.y(), l2.y());
        if (x < 0 || y < 0) return 0;
        return x * y;
    }

    public static Monitor getCurrentMonitor() {
        return getCurrentMonitor(Minecraft.getInstance().getWindow());
    }
}
