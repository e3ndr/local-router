package xyz.e3ndr.localrouter.routes;

import java.io.Closeable;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import lombok.RequiredArgsConstructor;
import xyz.e3ndr.localrouter.InFlight.InFlightRequest;
import xyz.e3ndr.localrouter.Pool;

@RequiredArgsConstructor
class _RequestCleanup implements Closeable {
    public final Thread threadToInterrupt;

    public final AtomicBoolean isCompleted = new AtomicBoolean(false);
    public final AtomicBoolean isInterrupted = new AtomicBoolean(false);

    public InFlightRequest inFlight;
    public Pool.Ticket poolTicket;

    public volatile InputStream streamToClose;

    @Override
    public void close() {
        if (!this.isCompleted.compareAndSet(false, true)) {
            return;
        }

        if (this.inFlight != null) {
            this.inFlight.markCompleted();
        }

        if (this.poolTicket != null) {
            this.poolTicket.release();
        }

        if (this.streamToClose != null) {
            try {
                this.streamToClose.close();
            } catch (Throwable ignored) {}
        }
    }

    public void interrupt() {
        if (!this.isInterrupted.compareAndSet(false, true)) {
            return;
        }

        if (this.streamToClose != null) {
            try {
                this.streamToClose.close();
            } catch (Throwable ignored) {}
        }

        this.threadToInterrupt.interrupt();
    }

}
