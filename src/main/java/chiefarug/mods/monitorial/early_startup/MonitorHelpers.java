package chiefarug.mods.monitorial.early_startup;

import chiefarug.mods.monitorial.MonitorialShared;
import chiefarug.mods.monitorial.config.MonitorData;
import chiefarug.mods.monitorial.config.MonitorialStartupConfig;
import chiefarug.mods.monitorial.mixin.ScreenManagerAccessor;
import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.ScreenManager;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.ObjectCollection;
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

public class MonitorHelpers {
    public static final Logger LOGGER = LogUtils.getLogger();


    public static Monitor getBestGLFWMonitorCode(Supplier<Monitor> defaultPrimary, ObjectCollection<Monitor> monitors) {
        if (monitors.size() == 1) {
            MonitorialShared.hasMultipleMonitors = false;
            return defaultPrimary.get(); // silently return if there is only one monitor. we don't need to do anything.
        }

        if (FMLConfig.getBoolConfigValue(FMLConfig.ConfigValue.EARLY_WINDOW_CONTROL) && MonitorialStartupConfig.getInstance().forceMove() == NEVER) {
            LOGGER.warn("earlyWindowControl in config/fml.toml is enabled and forceMove in config/monitorial-startup.json is set to NEVER so Monitorial has been effectively disabled!");
            return defaultPrimary.get(); // in theory running the code after this is fine, but the logging messages will be confusing
        }

        Optional<MonitorData> configured = MonitorialStartupConfig.getInstance().defaultMonitor();
        if (configured.isEmpty()) {
            LOGGER.info("Monitorial is not configured, using default behaviour (primary monitor)");
            LOGGER.info("To configured set one of these as \"defaultMonitor\" in config/monitorial-startup.json: ");
            logMonitors();
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

            LOGGER.trace("Monitorial found a monitor named '{}' at x: {}, y: {}", name, x, y);

            NamedDistancedMonitor distanced = new NamedDistancedMonitor(getDistanceSqrd(preferred.x(), x, preferred.y(), y), monitor, name);
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

    private static int getDistanceSqrd(int x1, int x2, int y1, int y2) {
        return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2);
    }

    public static void logMonitors() {
        PointerBuffer monitors = GLFW.glfwGetMonitors();
        if (monitors == null) return;
        int len = monitors.limit();
        for (int i = 0; i < len; i++) {
            long monitor = monitors.get();
            int[] x = new int[1],
                    y = new int[1];
            String name = GLFW.glfwGetMonitorName(monitor);
            GLFW.glfwGetMonitorPos(monitor, x, y);

            LOGGER.info("""
                    {"name": {}, "x": {}, "y": {}},"
                    """, name, x[0], y[0]);
        }
    }

    public static void forceMoveToMonitor(Monitor monitor, long window, int windowHeight, int windowWidth) {
        if (!MonitorialStartupConfig.getInstance().forceMove().shouldAttemptMove())  return;

        int x = monitor.getX() + windowWidth / 2; //TODO: fix this maths being slightly wrong compared to vanilla
        int y = monitor.getY() + windowHeight / 2;

        String monitorName = GLFW.glfwGetMonitorName(monitor.getMonitor());
        LOGGER.info("Attempting to force move window to position x: {}. y: {} on monitor {} (located at x: {}, y: {})", x, y, monitorName, monitor.getX(), monitor.getY());
        GLFW.glfwSetWindowPos(window, x, y);
        try (MemoryStack memorystack = MemoryStack.stackPush()) {
            PointerBuffer pointerbuffer = memorystack.mallocPointer(1);
            int error = glfwGetError(pointerbuffer);
            switch (error) {
                case GLFW_NO_ERROR -> LOGGER.info("Monitorial successfully moved the game window to monitor '{}' after NeoForge released control!", monitorName);
                case GLFW_FEATURE_UNAVAILABLE -> LOGGER.error("Monitorial failed to move the window, your window manager does not support moving windows! Disable forceMove in config/monitorial-startup.json to stop trying and hide this message.");
                default -> {
                    long pDescription = pointerbuffer.get();
                    String description = pDescription == 0L ? "" : MemoryUtil.memUTF8(pDescription);
                    LOGGER.error("Monitorial failed to move the window for an unknown reason. Suppressing GLFW error [{}]: {}", error, description);
                }
            }
        }
    }

    public static boolean hasMultipleMonitors(ScreenManager manager) {
        return ((ScreenManagerAccessor) manager).getMonitors().size() > 1;
    }
}
