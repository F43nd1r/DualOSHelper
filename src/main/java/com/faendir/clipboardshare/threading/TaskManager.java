package com.faendir.clipboardshare.threading;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * @author lukas
 * @since 03.11.19
 */
public class TaskManager {
    private final ExecutorService service = Executors.newCachedThreadPool();

    public Future<?> startTask(Runnable task) {
        return service.submit(task);
    }
}
