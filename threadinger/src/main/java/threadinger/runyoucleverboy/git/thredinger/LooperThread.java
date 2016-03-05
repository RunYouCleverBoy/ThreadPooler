package threadinger.runyoucleverboy.git.thredinger;

import android.os.Handler;
import android.os.Looper;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Description:
 * Created by RunYouCleverBoy on 5.3.16.
 */
@SuppressWarnings("unused")
public class LooperThread {
    public static final int INVALID_DELAY = -1;
    private Thread thread;
    private Handler handler;
    private LinkedList<EarlyRunnable> earlyRunnables = new LinkedList<>();
    private AtomicBoolean available = new AtomicBoolean(true);

    public LooperThread() {
        thread = new Thread(new LooperRunnable());
    }

    public synchronized LooperThread shutdown() {
        available.set(false);
        try {
            post(new PoisonPill());
            thread.join();
            thread = null;
        } catch (InterruptedException ignored) {
        }
        return this;
    }

    public synchronized LooperThread post(Runnable runnable) {
        return post(runnable, INVALID_DELAY);
    }

    public synchronized LooperThread post(Runnable runnable, int delay) {
        validateRuntime();
        if (handler != null) {
            if (delay > 0) {
                handler.postDelayed(runnable, delay);
            } else {
                handler.post(runnable);
            }
        } else {
            earlyRunnables.add(new EarlyRunnable(runnable, delay));
        }

        return this;
    }

    private synchronized void handleEarlyRunnables() {
        handler = new Handler();
        for (EarlyRunnable runnable : earlyRunnables) {
            post(runnable.runnable, runnable.delay);
        }
    }

    private void validateRuntime() {
        if (!available.get()) {
            throw new NotReadyException();
        }
    }

    private class LooperRunnable implements Runnable {
        @Override
        public void run() {
            Looper.prepare();
            handleEarlyRunnables();
            Looper.loop();
        }
    }

    private class PoisonPill implements Runnable {
        @Override
        public void run() {
            Looper looper = Looper.myLooper();
            if (looper != null) {
                looper.quit();
            }
        }
    }

    private class EarlyRunnable {
        private Runnable runnable;
        private int delay;

        public EarlyRunnable(Runnable runnable, int delay) {
            this.runnable = runnable;
            this.delay = delay;
        }
    }

    private class NotReadyException extends RuntimeException {
        public NotReadyException() {
            super("Looper thread is not active anymore");
        }
    }
}
