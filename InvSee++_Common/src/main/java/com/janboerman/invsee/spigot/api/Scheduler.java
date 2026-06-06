package com.janboerman.invsee.spigot.api;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.HumanEntity;

import java.util.UUID;

/**
 * Scheduler abstraction for scheduling tasks.
 *
 * <p>The {@code run*} methods are the modular scheduler API. They expose the ownership model
 * needed by Folia (global region, async, entity-owned and region-owned work) while keeping
 * callers independent from the concrete server implementation. The legacy {@code execute*}
 * methods remain as a compatibility adapter for existing API consumers.</p>
 *
 * @see <a href="https://github.com/Jannyboy11/InvSee-plus-plus/wiki/Folia-support">Folia support</a>
 */
public interface Scheduler {

    /**
     * Cancellable handle returned by scheduler implementations.
     */
    public interface TaskHandle {

        /**
         * Try to cancel this task.
         * @return true when cancellation was requested for a scheduled task, otherwise false.
         */
        public boolean cancel();

        /**
         * Get whether this task is already cancelled.
         * @return true when cancelled, otherwise false.
         */
        public default boolean isCancelled() {
            return false;
        }
    }

    /**
     * No-op handle used when a task ran immediately or could not be scheduled.
     * @return a handle that cannot cancel anything.
     */
    public static TaskHandle completedTask() {
        return NoopTaskHandle.INSTANCE;
    }

    /**
     * No-op handle used when a task could not be scheduled.
     * @return a handle that cannot cancel anything.
     */
    public static TaskHandle unscheduledTask() {
        return NoopTaskHandle.INSTANCE;
    }

    enum NoopTaskHandle implements TaskHandle {
        INSTANCE;

        @Override
        public boolean cancel() {
            return false;
        }
    }

    /**
     * Schedule global server state work.
     * @param task the task to run.
     * @return a cancellable handle when the implementation schedules the task.
     */
    public default TaskHandle runGlobal(Runnable task) {
        executeSyncGlobal(task);
        return completedTask();
    }

    /**
     * Schedule delayed global server state work.
     * @param task the task to run.
     * @param delayTicks delay in ticks.
     * @return a cancellable handle.
     */
    public default TaskHandle runGlobalDelayed(Runnable task, long delayTicks) {
        executeLaterGlobal(task, delayTicks);
        return completedTask();
    }

    /**
     * Schedule repeating global server state work.
     * @param task the task to run.
     * @param initialDelayTicks initial delay in ticks.
     * @param periodTicks period in ticks.
     * @return a cancellable handle.
     */
    public default TaskHandle runGlobalRepeatedly(Runnable task, long initialDelayTicks, long periodTicks) {
        executeSyncGlobalRepeatedly(task, initialDelayTicks, periodTicks);
        return completedTask();
    }

    /**
     * Schedule async work. The task must not touch unsafe Bukkit state.
     * @param task the task to run.
     * @return a cancellable handle when the implementation schedules the task.
     */
    public default TaskHandle runAsync(Runnable task) {
        executeAsync(task);
        return completedTask();
    }

    /**
     * Schedule delayed async work. The task must not touch unsafe Bukkit state.
     * @param task the task to run.
     * @param delayTicks delay in ticks.
     * @return a cancellable handle.
     */
    public default TaskHandle runAsyncDelayed(Runnable task, long delayTicks) {
        executeLaterAsync(task, delayTicks);
        return completedTask();
    }

    /**
     * Schedule repeating async work. The task must not touch unsafe Bukkit state.
     * @param task the task to run.
     * @param initialDelayTicks initial delay in ticks.
     * @param periodTicks period in ticks.
     * @return a cancellable handle.
     */
    public default TaskHandle runAsyncRepeatedly(Runnable task, long initialDelayTicks, long periodTicks) {
        throw new UnsupportedOperationException("Repeating async tasks are not supported by this scheduler implementation.");
    }

    /**
     * Schedule work owned by a player/entity identified by UUID.
     * @param playerId the player UUID.
     * @param task the task to run while the entity is alive/owned.
     * @param retired fallback invoked if the entity scheduler retires before execution; may be null.
     * @return a cancellable handle or {@link #unscheduledTask()} if the entity cannot be scheduled.
     */
    public default TaskHandle runEntity(UUID playerId, Runnable task, Runnable retired) {
        executeSyncPlayer(playerId, task, retired);
        return completedTask();
    }

    /**
     * Schedule work owned by a live entity.
     * @param entity the entity that owns the task.
     * @param task the task to run while the entity is alive/owned.
     * @param retired fallback invoked if the entity scheduler retires before execution; may be null.
     * @return a cancellable handle or {@link #unscheduledTask()} if the entity cannot be scheduled.
     */
    public default TaskHandle runEntity(HumanEntity entity, Runnable task, Runnable retired) {
        runEntity(entity.getUniqueId(), task, retired);
        return completedTask();
    }

    /**
     * Schedule delayed work owned by a live entity.
     * @param entity the entity that owns the task.
     * @param task the task to run while the entity is alive/owned.
     * @param retired fallback invoked if the entity scheduler retires before execution; may be null.
     * @param delayTicks delay in ticks.
     * @return a cancellable handle or {@link #unscheduledTask()} if the entity cannot be scheduled.
     */
    public default TaskHandle runEntityDelayed(HumanEntity entity, Runnable task, Runnable retired, long delayTicks) {
        return runEntity(entity, task, retired);
    }

    /**
     * Schedule repeating work owned by a live entity.
     * @param entity the entity that owns the task.
     * @param task the task to run while the entity is alive/owned.
     * @param retired fallback invoked if the entity scheduler retires before execution; may be null.
     * @param initialDelayTicks initial delay in ticks.
     * @param periodTicks period in ticks.
     * @return a cancellable handle or {@link #unscheduledTask()} if the entity cannot be scheduled.
     */
    public default TaskHandle runEntityRepeatedly(HumanEntity entity, Runnable task, Runnable retired, long initialDelayTicks, long periodTicks) {
        throw new UnsupportedOperationException("Repeating entity tasks are not supported by this scheduler implementation.");
    }

    /**
     * Schedule work owned by the region containing a location.
     * @param location the location whose region owns the task.
     * @param task the task to run.
     * @return a cancellable handle.
     */
    public default TaskHandle runRegion(Location location, Runnable task) {
        return runGlobal(task);
    }

    /**
     * Schedule work owned by a chunk region.
     * @param world the world.
     * @param chunkX chunk X coordinate.
     * @param chunkZ chunk Z coordinate.
     * @param task the task to run.
     * @return a cancellable handle.
     */
    public default TaskHandle runRegion(World world, int chunkX, int chunkZ, Runnable task) {
        return runGlobal(task);
    }

    /**
     * Schedule delayed work owned by the region containing a location.
     * @param location the location whose region owns the task.
     * @param task the task to run.
     * @param delayTicks delay in ticks.
     * @return a cancellable handle.
     */
    public default TaskHandle runRegionDelayed(Location location, Runnable task, long delayTicks) {
        return runGlobalDelayed(task, delayTicks);
    }

    /**
     * Schedule repeating work owned by the region containing a location.
     * @param location the location whose region owns the task.
     * @param task the task to run.
     * @param initialDelayTicks initial delay in ticks.
     * @param periodTicks period in ticks.
     * @return a cancellable handle.
     */
    public default TaskHandle runRegionRepeatedly(Location location, Runnable task, long initialDelayTicks, long periodTicks) {
        return runGlobalRepeatedly(task, initialDelayTicks, periodTicks);
    }

    /**
     * @deprecated use {@link #runEntity(UUID, Runnable, Runnable)} instead.
     */
    @Deprecated
    public default void executeSyncPlayer(UUID playerId, Runnable task, Runnable retired) {
        runEntity(playerId, task, retired);
    }


    /**
     * @deprecated use {@link #runEntity(HumanEntity, Runnable, Runnable)} instead.
     */
    @Deprecated
    public default void executeSyncPlayer(HumanEntity player, Runnable task, Runnable retired) {
        runEntity(player, task, retired);
    }

    /**
     * @deprecated use {@link #runGlobal(Runnable)} instead.
     */
    @Deprecated
    public default void executeSyncGlobal(Runnable task) {
        runGlobal(task);
    }

    /**
     * @deprecated use {@link #runGlobalRepeatedly(Runnable, long, long)} instead.
     */
    @Deprecated
    public default void executeSyncGlobalRepeatedly(Runnable task, long ticksInitialDelay, long ticksPeriod) {
        runGlobalRepeatedly(task, ticksInitialDelay, ticksPeriod);
    }

    /**
     * @deprecated use {@link #runAsync(Runnable)} instead.
     */
    @Deprecated
    public default void executeAsync(Runnable task) {
        runAsync(task);
    }

    /**
     * @deprecated use {@link #runGlobalDelayed(Runnable, long)} instead.
     */
    @Deprecated
    public default void executeLaterGlobal(Runnable task, long delayTicks) {
        runGlobalDelayed(task, delayTicks);
    }

    /**
     * @deprecated use {@link #runAsyncDelayed(Runnable, long)} instead.
     */
    @Deprecated
    public default void executeLaterAsync(Runnable task, long delayTicks) {
        runAsyncDelayed(task, delayTicks);
    }
}
