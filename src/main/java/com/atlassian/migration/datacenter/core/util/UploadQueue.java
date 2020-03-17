package com.atlassian.migration.datacenter.core.util;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Thin wrapper around a blocking queue that uses Optional.empty() to signal that the queue is closed.
 * A queue-len must be provided, which should be the maximum uploads that can be in flight concurrently.
 * This ensures the producer will pause (block) if it out-runs the consumer's capacity to upload,
 * AKA back-pressure.
 *
 * Copyright Atlassian: 13/03/2020
 */
public class UploadQueue<T>
{
    private BlockingQueue<Optional<T>> queue;

    public UploadQueue(Integer max)
    {
        this.queue = new LinkedBlockingQueue<>(max);
    }

    /**
     * Put a value on the queue to be consumed; blocks if the consumer is saturated.
     *
     * @param v
     * @throws InterruptedException
     */
    public void put(T v) throws InterruptedException
    {
        queue.put(Optional.of(v));
    }

    /**
     * Similar to BlockingQueue.take(), except returns Optional.empty() if the producer is finished.
     *
     * @return Optional of value, or Optional.empty() if the producer is finished.
     * @throws InterruptedException
     */
    public Optional<T> take() throws InterruptedException
    {
        return queue.take();
    }

    /**
     * Signal to the consumer that the producer is finished.
     *
     * @throws InterruptedException
     */
    public void finish() throws InterruptedException{
        queue.put(Optional.empty());
    }

    public boolean contains(T o)
    {
        return queue.contains(Optional.of(o));
    }

    public boolean isEmpty()
    {
        return queue.isEmpty();
    }

    public int size()
    {
        return queue.size();
    }
}
