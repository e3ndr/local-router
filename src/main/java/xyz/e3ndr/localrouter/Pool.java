package xyz.e3ndr.localrouter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import xyz.e3ndr.localrouter.InFlight.InFlightRequest;
import xyz.e3ndr.localrouter.inference.InferenceProvider;

/**
 * The coordinator for one resource pool.
 *
 * The VRAM invariant: at most one provider in a pool may hold in-flight
 * requests at a time. When a different provider's request has a clear path,
 * the previous provider is slept and the new one is woken. Providers are
 * kept warm otherwise: a provider is never slept just because its queue is
 * empty (load times can be penalizing), it stays active until another
 * provider takes the pool.
 *
 * Fairness: every local request joins a single FIFO queue for the pool. A
 * request only starts when it is the head of the queue and its constraints
 * allow it to run:
 *
 * - the pool is empty, or
 * - it is on the active provider, there is a free concurrency slot, and
 *   (if the provider is configured to) all in-flight requests target the
 *   same model.
 *
 * A request on the active provider with a free slot still waits behind
 * earlier requests from other providers — no skipping the line.
 */
public class Pool {
    private static final Logger log = Logger.getLogger(Pool.class.getName());

    /**
     * The fair lock: the queue order is strictly by arrival, and only the
     * head is ever considered.
     */
    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition drained = this.lock.newCondition();

    /**
     * The provider currently holding in-flight requests in this pool, or
     * null when the pool is empty.
     */
    private InferenceProvider active;

    /**
     * The in-flight requests on the active provider, in start order.
     */
    private final Map<String, Ticket> inFlight = new LinkedHashMap<>();

    /**
     * The FIFO queue of local requests waiting to start, in arrival order.
     */
    private final Map<String, Ticket> queue = new LinkedHashMap<>();

    /**
     * Join the queue, wait for this request's turn, start it, and return the
     * ticket. Blocks until the request is dispatched, or the thread is
     * interrupted/cancelled (in which case the queue slot is released and
     * an {@link InterruptedException} is thrown).
     *
     * When this request is the one that activates a new provider, the
     * switch work (sleeping the previous provider + waking this one) is
     * performed here, outside the pool lock, so a slow wake-up does not
     * hold up the queue.
     */
    public Ticket acquire(InferenceProvider provider, String modelId, InFlightRequest inFlight) throws InterruptedException {
        Ticket ticket;
        boolean needsSwitch;
        InferenceProvider switchedFrom;

        this.lock.lockInterruptibly();
        try {
            ticket = new Ticket(this, provider, modelId, inFlight);
            this.queue.put(inFlight.id, ticket);
            log.info(() -> "[pool] enqueued " + inFlight.id + " (" + provider.id() + " / " + modelId + ")");

            while (ticket.state == State.QUEUED) {
                try {
                    if (Thread.interrupted()) {
                        this.interruptQueued(ticket);
                        throw new InterruptedException();
                    }
                    this.drained.await();
                } catch (InterruptedException e) {
                    if (ticket.state == State.QUEUED) {
                        this.interruptQueued(ticket);
                    }
                    throw e;
                }
            }

            if (ticket.state == State.CANCELLED) {
                throw new InterruptedException();
            }

            needsSwitch = ticket.isSwitch;
            switchedFrom = ticket.switchedFrom;
        } finally {
            this.lock.unlock();
        }

        if (needsSwitch) {
            try {
                if (switchedFrom != null) {
                    this.sleepQuietly(switchedFrom);
                }
                provider.wakeUp();
            } catch (InterruptedException e) {
                // Cancelled mid-switch: give back the slot we just took.
                this.release(ticket);
                inFlight.cancel();
                throw e;
            } catch (IOException e) {
                throw new RuntimeException("Failed to prepare provider " + provider.id() + " for inference: " + e, e);
            }
        }

        return ticket;
    }

    /**
     * Release a started request's slot. Idempotent — safe to call from both
     * the request's cleanup path and an error path.
     */
    public void release(Ticket ticket) {
        this.lock.lock();
        try {
            if (ticket.state != State.RUNNING) {
                return;
            }

            ticket.state = State.DONE;
            this.inFlight.remove(ticket.inFlight.id);
            ticket.inFlight.markCompleted();
            log.info(() -> "[pool] released " + ticket.inFlight.id + " (" + ticket.provider.id() + " / " + ticket.modelId + ")");

            if (this.inFlight.isEmpty()) {
                this.active = null; // The pool is empty again; the provider stays warm.
            }

            this.dispatch();
        } finally {
            this.lock.unlock();
        }
    }

    /**
     * Drop a queued (not yet started) request from the line and let the
     * dispatcher re-evaluate the new head.
     */
    private void interruptQueued(Ticket ticket) {
        if (this.queue.remove(ticket.inFlight.id) != null) {
            ticket.state = State.CANCELLED;
            this.drained.signalAll();
            this.dispatch();
        }
    }

    /**
     * Start queued requests from the head of the line while the head is
     * allowed to run. Must be called with the lock held.
     */
    private void dispatch() {
        while (!this.queue.isEmpty()) {
            Ticket head = this.queue.values().iterator().next();
            if (!this.canStart(head)) {
                return;
            }

            this.queue.remove(head.inFlight.id);

            boolean isSwitch = this.active == null || this.active != head.provider;
            head.switchedFrom = isSwitch && this.active != null ? this.active : null;
            if (isSwitch) {
                this.active = head.provider;
            }

            this.inFlight.put(head.inFlight.id, head);
            head.state = State.RUNNING;
            head.isSwitch = isSwitch;
            log.info(() -> "[pool] started " + head.inFlight.id + " (" + head.provider.id() + " / " + head.modelId + (isSwitch ? ", provider switch" : "") + ")");

            this.drained.signalAll();
        }
    }

    /**
     * May the given ticket (always the head of the queue when called) start
     * now? Must be called with the lock held.
     */
    private boolean canStart(Ticket head) {
        if (this.active == null) {
            return true;
        }

        if (this.active != head.provider) {
            return false; // Another provider holds the pool; wait for a full drain.
        }

        if (this.inFlight.size() >= head.provider.concurrency()) {
            return false;
        }

        if (head.provider.concurrencyMatchesModel()) {
            for (Ticket inFlight : this.inFlight.values()) {
                if (!inFlight.modelId.equals(head.modelId)) {
                    return false; // No second model on the same resources.
                }
            }
        }

        return true;
    }

    private void sleepQuietly(InferenceProvider provider) {
        try {
            provider.sleep();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Keep the flag; the wake-up/callers react to it.
        } catch (Exception e) {
            log.warning(() -> "[pool] failed to sleep provider " + provider.id() + ": " + e);
        }
    }

    private enum State {
        QUEUED,
        RUNNING,
        CANCELLED,
        DONE
    }

    /**
     * A single request's claim on the pool. One ticket per request.
     */
    public static class Ticket {
        final Pool pool;
        final InferenceProvider provider;
        final String modelId;
        final InFlightRequest inFlight;

        volatile State state = State.QUEUED;

        /**
         * True when starting this ticket activated a new provider for the
         * pool (the switch work was performed in {@link #acquire}).
         */
        volatile boolean isSwitch;

        /**
         * The provider that was active before this ticket's switch, if any.
         */
        volatile InferenceProvider switchedFrom;

        Ticket(Pool pool, InferenceProvider provider, String modelId, InFlightRequest inFlight) {
            this.pool = pool;
            this.provider = provider;
            this.modelId = modelId;
            this.inFlight = inFlight;
        }

        /**
         * Release this ticket's pool slot. Idempotent — the request's
         * cleanup path calls it exactly once, and error paths may not.
         */
        public void release() {
            this.pool.release(this);
        }
    }
}
