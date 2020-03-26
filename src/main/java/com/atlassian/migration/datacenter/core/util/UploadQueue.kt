/*
 * Copyright 2020 Atlassian
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.atlassian.migration.datacenter.core.util

import java.util.Optional
import java.util.concurrent.BlockingQueue
import java.util.concurrent.LinkedBlockingQueue

class UploadQueue<T>(max: Int?) {
    private val queue: BlockingQueue<Optional<T>>

    /**
     * Put a value on the queue to be consumed; blocks if the consumer is saturated.
     *
     * @param v
     * @throws InterruptedException
     */
    @Throws(InterruptedException::class)
    fun put(v: T) {
        queue.put(Optional.of(v))
    }

    /**
     * Similar to BlockingQueue.take(), except returns Optional.empty() if the producer is finished.
     *
     * @return Optional of value, or Optional.empty() if the producer is finished.
     * @throws InterruptedException
     */
    @Throws(InterruptedException::class)
    fun take(): Optional<T> {
        return queue.take()
    }

    /**
     * Signal to the consumer that the producer is finished.
     *
     * @throws InterruptedException
     */
    @Throws(InterruptedException::class)
    fun finish() {
        queue.put(Optional.empty())
    }

    operator fun contains(o: T): Boolean {
        return queue.contains(Optional.of(o))
    }

    fun isEmpty(): Boolean {
        return queue.isEmpty()
    }

    fun size(): Int {
        return queue.size
    }

    init {
        queue = LinkedBlockingQueue(max!!)
    }
}