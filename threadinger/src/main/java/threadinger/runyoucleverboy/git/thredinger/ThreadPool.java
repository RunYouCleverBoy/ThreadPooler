package threadinger.runyoucleverboy.git.thredinger;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
public class ThreadPool {
    public final int POOL_SIZE = 10;
    private final ExecutorService threadPool;
    private final AtomicInteger inventorySize;

    public ThreadPool() {
        threadPool = Executors.newFixedThreadPool(POOL_SIZE);
        inventorySize = new AtomicInteger(POOL_SIZE);
    }

    public void shutdown() {
        threadPool.shutdown();
    }

    public <T> Future<T> submitCallable(final Callable<T> command, final String name) {
        return threadPool.submit(new CommandWrapper<>(command, name));
    }

    public void submit(final Runnable command, final String name, final Runnable whenDone) {
        threadPool.submit(new Runnable() {
            @Override
            public void run() {
                setCurrentThreadName(name);
                inventorySize.decrementAndGet();
                command.run();
                if (whenDone != null) {
                    whenDone.run();
                }
                inventorySize.incrementAndGet();
            }
        });
    }

    public void submit(final Runnable command) {
        threadPool.submit(new Runnable() {
            @Override
            public void run() {
                setCurrentThreadName("ThreadPoolRunnable" + System.nanoTime());
                inventorySize.decrementAndGet();
                command.run();
                inventorySize.incrementAndGet();
            }
        });
    }

    private void setCurrentThreadName(String name) {
        if (name != null && !name.isEmpty()) {
            Thread.currentThread().setName(name);
        }
    }

    private class CommandWrapper<T> implements Callable<T> {
        private final Callable<T> command;
        private final String name;

        public CommandWrapper(Callable<T> command, String name) {
            this.command = command;
            this.name = name;
        }

        @Override
        public T call() throws Exception {
            setCurrentThreadName(name);
            inventorySize.decrementAndGet();
            T result = command.call();
            inventorySize.incrementAndGet();
            return result;
        }
    }
}
