package com.janboerman.invsee.spigot;

import com.janboerman.invsee.spigot.api.Scheduler;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.HumanEntity;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;

/**
 * Scheduler implementation based on the {@link org.bukkit.scheduler.BukkitScheduler}.
 */
public class DefaultScheduler implements Scheduler {

    private final InvseePlusPlus plugin;

    public DefaultScheduler(InvseePlusPlus plugin) {
        this.plugin = plugin;
    }

    @Override
    public TaskHandle runEntity(UUID playerId, Runnable task, Runnable retired) {
        if (plugin.isShuttingDown()) return runInline(task);
        return runGlobal(task);
    }

    @Override
    public TaskHandle runEntity(HumanEntity entity, Runnable task, Runnable retired) {
        if (plugin.isShuttingDown()) return runInline(task);
        return runGlobal(task);
    }

    @Override
    public TaskHandle runEntityDelayed(HumanEntity entity, Runnable task, Runnable retired, long delayTicks) {
        if (plugin.isShuttingDown()) return Scheduler.unscheduledTask();
        return runGlobalDelayed(task, delayTicks);
    }

    @Override
    public TaskHandle runEntityRepeatedly(HumanEntity entity, Runnable task, Runnable retired, long initialDelayTicks, long periodTicks) {
        if (plugin.isShuttingDown()) return Scheduler.unscheduledTask();
        return runGlobalRepeatedly(task, initialDelayTicks, periodTicks);
    }

    @Override
    public TaskHandle runGlobal(Runnable task) {
        if (plugin.isShuttingDown()) return runInline(task);
        if (plugin.getServer().isPrimaryThread()) {
            task.run();
            return Scheduler.completedTask();
        } else {
            return bukkitTask(plugin.getServer().getScheduler().runTask(plugin, task));
        }
    }

    @Override
    public TaskHandle runGlobalRepeatedly(Runnable task, long ticksInitialDelay, long ticksPeriod) {
        if (plugin.isShuttingDown()) return Scheduler.unscheduledTask();
        return bukkitTask(plugin.getServer().getScheduler().runTaskTimer(plugin, task, ticksInitialDelay, ticksPeriod));
    }

    @Override
    public TaskHandle runAsync(Runnable task) {
        if (plugin.isShuttingDown()) return runInline(task);
        if (!plugin.getServer().isPrimaryThread()) {
            task.run();
            return Scheduler.completedTask();
        } else {
            return bukkitTask(plugin.getServer().getScheduler().runTaskAsynchronously(plugin, task));
        }
    }

    @Override
    public TaskHandle runAsyncRepeatedly(Runnable task, long initialDelayTicks, long periodTicks) {
        if (plugin.isShuttingDown()) return Scheduler.unscheduledTask();
        return bukkitTask(plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, task, initialDelayTicks, periodTicks));
    }

    @Override
    public TaskHandle runGlobalDelayed(Runnable task, long delayTicks) {
        if (plugin.isShuttingDown()) return Scheduler.unscheduledTask();
        return bukkitTask(plugin.getServer().getScheduler().runTaskLater(plugin, task, delayTicks));
    }

    @Override
    public TaskHandle runAsyncDelayed(Runnable task, long delayTicks) {
        if (plugin.isShuttingDown()) return Scheduler.unscheduledTask();
        return bukkitTask(plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, task, delayTicks));
    }

    @Override
    public TaskHandle runRegion(Location location, Runnable task) {
        return runGlobal(task);
    }

    @Override
    public TaskHandle runRegion(World world, int chunkX, int chunkZ, Runnable task) {
        return runGlobal(task);
    }

    @Override
    public TaskHandle runRegionDelayed(Location location, Runnable task, long delayTicks) {
        return runGlobalDelayed(task, delayTicks);
    }

    @Override
    public TaskHandle runRegionRepeatedly(Location location, Runnable task, long initialDelayTicks, long periodTicks) {
        return runGlobalRepeatedly(task, initialDelayTicks, periodTicks);
    }

    private static TaskHandle runInline(Runnable task) {
        task.run();
        return Scheduler.completedTask();
    }

    private static TaskHandle bukkitTask(BukkitTask task) {
        return new BukkitTaskHandle(task);
    }

    private static final class BukkitTaskHandle implements TaskHandle {

        private final BukkitTask task;

        private BukkitTaskHandle(BukkitTask task) {
            this.task = task;
        }

        @Override
        public boolean cancel() {
            task.cancel();
            return true;
        }

        @Override
        public boolean isCancelled() {
            return task.isCancelled();
        }
    }
}
