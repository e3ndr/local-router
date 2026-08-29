package xyz.e3ndr.localrouter;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry of the resource pools. A pool is a shared pool of inference
 * resources (e.g. a set of GPUs) that only one local provider can use at a
 * time. Requests on providers that share a pool coordinate through a single
 * {@link Pool} instance, so requests on one provider never block or sleep
 * the providers of any other pool.
 */
public class Pools {
    private static final Map<String, Pool> pools = new HashMap<>();

    public static Pool pool(String resourcePool) {
        return pools.computeIfAbsent(resourcePool, (k) -> new Pool());
    }
}
