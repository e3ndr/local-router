package xyz.e3ndr.localrouter.util;

import co.casterlabs.rhs.util.TaskExecutor;

public class RakuraiTaskExecutor implements TaskExecutor {
    public static final RakuraiTaskExecutor INSTANCE = new RakuraiTaskExecutor();

    private static final Thread.Builder THREAD_FACTORY = Thread.ofVirtual().name("Http Task Pool - #", 0);

    @Override
    public Task execute(Runnable toRun) {
        return new Task() {
            private final Thread thread = THREAD_FACTORY.start(toRun);

            @Override
            public void interrupt() {
                this.thread.interrupt();
            }

            @Override
            public void waitFor() throws InterruptedException {
                this.thread.join();
            }

            @Override
            public boolean isAlive() {
                return this.thread.isAlive();
            }
        };
    }

}
