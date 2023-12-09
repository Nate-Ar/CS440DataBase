package anti.sus.async;

import anti.sus.Main;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class Worker {
    private static final Duration MAX_SHUTDOWN_TIME = Duration.ofSeconds(1500);
    private final ExecutorService workExecutor;

    public Worker(final int numThreads) {
        this.workExecutor = Executors.newFixedThreadPool(numThreads);
    }

    public <T> CompletableFuture<T> submitWork(final Supplier<T> workDefinition) {
        return CompletableFuture.supplyAsync(workDefinition, this.workExecutor).exceptionally(throwable -> {
            Main.runSync(() -> {
                throw exception(throwable);
            });
            throw exception(throwable);
        });
    }

    private static AsyncException exception(final Throwable throwable) {
        return new AsyncException("Async job threw exception!", throwable);
    }

    /**
     * Shutdown the Thread group running work, waiting some amount of time
     * to give tasks already assigned a chance to complete.
     */
    public void shutdown() {
        this.workExecutor.shutdown();

        try {
            // Wait for all tasks to finish or until the specified timeout
            if (!this.workExecutor.awaitTermination(MAX_SHUTDOWN_TIME.getSeconds(), TimeUnit.SECONDS)) {
                System.out.println("Termination timed out after " + MAX_SHUTDOWN_TIME);
            }
        } catch (final InterruptedException ex) {
            System.out.println("Shutdown task interrupted!" + ex);
        }
    }

    public static final class AsyncException extends RuntimeException {
        public AsyncException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
