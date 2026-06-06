package com.janboerman.invsee.folia;

import com.janboerman.invsee.spigot.InvseePlusPlus;
import com.janboerman.invsee.spigot.api.Scheduler;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.EntityScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.RegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler implementation based on {@link GlobalRegionScheduler}, {@link AsyncScheduler},
 * {@link RegionScheduler} and {@link EntityScheduler}.
 */
public class FoliaScheduler implements Scheduler {

    private static final long MILLIS_PER_TICK = 50L;

    private final InvseePlusPlus plugin;

    public FoliaScheduler(InvseePlusPlus plugin) {
        this.plugin = plugin;
    }


    /**
     * Legacy adapter: older callers often passed {@code null} as retired callback because
     * the old implementation silently used the global scheduler when the player was offline.
     * Keep those callers completing while the codebase migrates to explicit offline flows;
     * new code should call {@link #runEntity(UUID, Runnable, Runnable)} with a real retired
     * callback instead.
     */
    @Override
    @Deprecated
    public void executeSyncPlayer(UUID playerId, Runnable task, Runnable retired) {
        runEntity(playerId, task, retired != null ? retired : task);
    }

    @Override
    public TaskHandle runEntity(UUID playerId, Runnable task, Runnable retired) {
        if (plugin.isShuttingDown()) return runInline(task);
        Server server = plugin.getServer();
        Player player = server.getPlayer(playerId);
        if (player == null) {
            if (retired != null) {
                retired.run();
            }
            return Scheduler.unscheduledTask();
        }

        return runEntity(player, task, retired);
    }

    @Override
    public TaskHandle runEntity(HumanEntity entity, Runnable task, Runnable retired) {
        if (plugin.isShuttingDown()) return runInline(task);
        ScheduledTask scheduledTask = entity.getScheduler().run(plugin, ignored -> task.run(), retired);
        return foliaTask(scheduledTask);
    }

    @Override
    public TaskHandle runEntityDelayed(HumanEntity entity, Runnable task, Runnable retired, long delayTicks) {
        if (plugin.isShuttingDown()) return Scheduler.unscheduledTask();
        ScheduledTask scheduledTask = entity.getScheduler().runDelayed(plugin, ignored -> task.run(), retired, positiveTicks(delayTicks));
        return foliaTask(scheduledTask);
    }

    @Override
    public TaskHandle runEntityRepeatedly(HumanEntity entity, Runnable task, Runnable retired, long initialDelayTicks, long periodTicks) {
        if (plugin.isShuttingDown()) return Scheduler.unscheduledTask();
        ScheduledTask scheduledTask = entity.getScheduler().runAtFixedRate(plugin, ignored -> task.run(), retired, positiveTicks(initialDelayTicks), positiveTicks(periodTicks));
        return foliaTask(scheduledTask);
    }

    @Override
    public TaskHandle runGlobal(Runnable task) {
        if (plugin.isShuttingDown()) return runInline(task);
        return foliaTask(plugin.getServer().getGlobalRegionScheduler().run(plugin, ignored -> task.run()));
    }

    @Override
    public TaskHandle runGlobalRepeatedly(Runnable task, long ticksInitialDelay, long ticksPeriod) {
        if (plugin.isShuttingDown()) return Scheduler.unscheduledTask();
        return foliaTask(plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, ignored -> task.run(), positiveTicks(ticksInitialDelay), positiveTicks(ticksPeriod)));
    }

    @Override
    public TaskHandle runAsync(Runnable task) {
        if (plugin.isShuttingDown()) return runInline(task);
        return foliaTask(plugin.getServer().getAsyncScheduler().runNow(plugin, ignored -> task.run()));
    }

    @Override
    public TaskHandle runAsyncRepeatedly(Runnable task, long initialDelayTicks, long periodTicks) {
        if (plugin.isShuttingDown()) return Scheduler.unscheduledTask();
        return foliaTask(plugin.getServer().getAsyncScheduler().runAtFixedRate(plugin, ignored -> task.run(), ticksToMillis(positiveTicks(initialDelayTicks)), ticksToMillis(positiveTicks(periodTicks)), TimeUnit.MILLISECONDS));
    }

    @Override
    public TaskHandle runGlobalDelayed(Runnable task, long delayTicks) {
        if (plugin.isShuttingDown()) return Scheduler.unscheduledTask();
        return foliaTask(plugin.getServer().getGlobalRegionScheduler().runDelayed(plugin, ignored -> task.run(), positiveTicks(delayTicks)));
    }

    @Override
    public TaskHandle runAsyncDelayed(Runnable task, long delayTicks) {
        if (plugin.isShuttingDown()) return Scheduler.unscheduledTask();
        return foliaTask(plugin.getServer().getAsyncScheduler().runDelayed(plugin, ignored -> task.run(), ticksToMillis(positiveTicks(delayTicks)), TimeUnit.MILLISECONDS));
    }

    @Override
    public TaskHandle runRegion(Location location, Runnable task) {
        if (plugin.isShuttingDown()) return runInline(task);
        return foliaTask(plugin.getServer().getRegionScheduler().run(plugin, location, ignored -> task.run()));
    }

    @Override
    public TaskHandle runRegion(World world, int chunkX, int chunkZ, Runnable task) {
        if (plugin.isShuttingDown()) return runInline(task);
        return foliaTask(plugin.getServer().getRegionScheduler().run(plugin, world, chunkX, chunkZ, ignored -> task.run()));
    }

    @Override
    public TaskHandle runRegionDelayed(Location location, Runnable task, long delayTicks) {
        if (plugin.isShuttingDown()) return Scheduler.unscheduledTask();
        return foliaTask(plugin.getServer().getRegionScheduler().runDelayed(plugin, location, ignored -> task.run(), positiveTicks(delayTicks)));
    }

    @Override
    public TaskHandle runRegionRepeatedly(Location location, Runnable task, long initialDelayTicks, long periodTicks) {
        if (plugin.isShuttingDown()) return Scheduler.unscheduledTask();
        return foliaTask(plugin.getServer().getRegionScheduler().runAtFixedRate(plugin, location, ignored -> task.run(), positiveTicks(initialDelayTicks), positiveTicks(periodTicks)));
    }

    private static TaskHandle runInline(Runnable task) {
        task.run();
        return Scheduler.completedTask();
    }

    private static long positiveTicks(long ticks) {
        return Math.max(1L, ticks);
    }

    private static long ticksToMillis(long ticks) {
        return ticks * MILLIS_PER_TICK;
    }

    private static TaskHandle foliaTask(ScheduledTask task) {
        return task == null ? Scheduler.unscheduledTask() : new FoliaTaskHandle(task);
    }

    private static final class FoliaTaskHandle implements TaskHandle {

        private final ScheduledTask task;

        private FoliaTaskHandle(ScheduledTask task) {
            this.task = task;
        }

        @Override
        public boolean cancel() {
            return task.cancel() != ScheduledTask.CancelledState.CANCELLED_ALREADY;
        }

        @Override
        public boolean isCancelled() {
            return task.isCancelled();
        }
    }
}
