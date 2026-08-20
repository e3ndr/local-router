package xyz.e3ndr.localrouter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import xyz.e3ndr.localrouter.db.Providers;
import xyz.e3ndr.localrouter.inference.InferenceProvider;

public class LR {
    public static Connection database;

    public static void init() throws SQLException {
        database = DriverManager.getConnection("jdbc:sqlite:db.sqlite");
    }

    private static final Map<String, ReentrantLock> resourcePools = new HashMap<>();

    public static synchronized void lockLocalModels(String resourcePool, String currentProvider) {
        ReentrantLock lock = resourcePools.computeIfAbsent(resourcePool, (k) -> new ReentrantLock(true));

        lock.lock();
        for (InferenceProvider provider : Providers.providers()) {
            if (provider.id().equals(currentProvider)) {
                continue;
            }

            try {
                provider.sleep();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void unlockLocalModels(String resourcePool) {
        ReentrantLock lock = resourcePools.get(resourcePool);
        lock.unlock();
    }

}
