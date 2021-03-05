package org.bondlib;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import stormpot.*;

import java.util.concurrent.TimeUnit;

public class ByteArrayOutputStreamBroker {

    private final static LifecycledResizablePool<ByteArrayOutputStream> poolSmallObject;
    private final static LifecycledResizablePool<ByteArrayOutputStream> poolLargeObject;
    private static Timeout timeout;

    static {

        Config<ByteArrayOutputStream> config = new Config<>();
        config.setAllocator(new ByteArrayOutputStreamAllocator());

        config.setThreadFactory(new ThreadFactoryBuilder()
                .setDaemon(true)
                .setNameFormat("Stormpot-%d")
                .build());

        // Pool1 for Bond type in function temp buffer.
        poolSmallObject = new BlazePool<>(config);
        poolLargeObject = new BlazePool<>(config);

        poolSmallObject.setTargetSize(10 * 1024);
        poolLargeObject.setTargetSize(Runtime.getRuntime().availableProcessors() * 2);
        timeout = new Timeout(50, TimeUnit.MICROSECONDS);
    }

    public static ByteArrayOutputStream allocate() {
        return allocateImpl(poolSmallObject);
    }

    public static ByteArrayOutputStream allocateLargeObject() {
        return allocateImpl(poolLargeObject);
    }

    private static ByteArrayOutputStream allocateImpl(LifecycledResizablePool<ByteArrayOutputStream> pool) {
        try {
            ByteArrayOutputStream stream = pool.claim(timeout);
            if (stream == null) {
                stream = new ByteArrayOutputStream();
            }
            return stream;
        } catch (InterruptedException ex) {
            return new ByteArrayOutputStream();
        }
    }

    public static void deallocate(ByteArrayOutputStream poolable) {
        poolable.release();
    }

    static class ByteArrayOutputStreamAllocator implements Allocator<ByteArrayOutputStream> {

        @Override
        public ByteArrayOutputStream allocate(Slot slot) throws Exception {
            return new ByteArrayOutputStream(4 * 1024, slot);
        }

        @Override
        public void deallocate(ByteArrayOutputStream poolable) throws Exception {
            poolable.destory();
        }
    }
}