/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.collections4.bloomfilter;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.IntPredicate;
import java.util.function.LongPredicate;

/**
 * A bloom filter using an array of bit maps to track enabled bits. This is a
 * standard implementation and should work well for most Bloom filters.
 *
 * @since 4.5
 */
public final class ThreadSafeBloomFilter implements BloomFilter {

    /**
     * The array of bit map longs that defines this Bloom filter. Will be null if
     * the filter is empty.
     */
    private volatile long[] bitMap;

    /**
     * The Shape of this Bloom filter.
     */
    private final Shape shape;

    /**
     * The cardinality of this Bloom filter.
     */
    private volatile int cardinality;

    private final ReentrantLock writeLock;

    /**
     * Creates an empty instance.
     *
     * @param shape The shape for the filter.
     */
    public ThreadSafeBloomFilter(final Shape shape) {
        Objects.requireNonNull(shape, "shape");
        this.shape = shape;
        this.bitMap = new long[BitMap.numberOfBitMaps(shape.getNumberOfBits())];
        this.cardinality = 0;
        this.writeLock = new ReentrantLock();
    }

    /**
     * Copy constructor for {@code copy()} use.
     *
     * @param source
     */
    private ThreadSafeBloomFilter(final ThreadSafeBloomFilter source) {
        this.shape = source.shape;
        this.bitMap = source.bitMap.clone();
        this.cardinality = source.cardinality;
        this.writeLock = new ReentrantLock();
    }

    @Override
    public void clear() {
        writeLock.lock();
        try {
            Arrays.fill(bitMap, 0L);
            cardinality = 0;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public long[] asBitMapArray() {
        return Arrays.copyOf(bitMap, bitMap.length);
    }

    @Override
    public boolean forEachBitMapPair(final BitMapProducer other, final LongBiPredicate func) {
        final CountingLongPredicate p = new CountingLongPredicate(bitMap, func);
        return other.forEachBitMap(p) && p.forEachRemaining();
    }

    @Override
    public ThreadSafeBloomFilter copy() {
        return new ThreadSafeBloomFilter(this);
    }

    @Override
    public boolean merge(final IndexProducer indexProducer) {
        Objects.requireNonNull(indexProducer, "indexProducer");
        writeLock.lock();
        try {
            long[] newMap = asBitMapArray();
            indexProducer.forEachIndex(idx -> {
                if (idx < 0 || idx >= shape.getNumberOfBits()) {
                    throw new IllegalArgumentException(String.format(
                            "IndexProducer should only send values in the range[0,%s)", shape.getNumberOfBits()));
                }
                BitMap.set(newMap, idx);
                return true;
            });
            cardinality = -1;
            bitMap = newMap;
        } finally {
            writeLock.unlock();
        }
        return true;
    }

    @Override
    public boolean merge(final BitMapProducer bitMapProducer) {
        Objects.requireNonNull(bitMapProducer, "bitMapProducer");
        writeLock.lock();
        try {
            long[] newMap = asBitMapArray();
            final int[] idx = new int[1];
            bitMapProducer.forEachBitMap(value -> {
                newMap[idx[0]++] |= value;
                return true;
            });
            // idx[0] will be limit+1 so decrement it
            idx[0]--;
            final int idxLimit = BitMap.getLongIndex(shape.getNumberOfBits());
            if (idxLimit == idx[0]) {
                final long excess = newMap[idxLimit] >> shape.getNumberOfBits();
                if (excess != 0) {
                    throw new IllegalArgumentException(
                            String.format("BitMapProducer set a bit higher than the limit for the shape: %s",
                                    shape.getNumberOfBits()));
                }
            }
            bitMap = newMap;
            cardinality = -1;
        } catch (final IndexOutOfBoundsException e) {
            throw new IllegalArgumentException(
                    String.format("BitMapProducer should send at most %s maps", bitMap.length), e);
        } finally {
            writeLock.unlock();
        }
        return true;
    }

    @Override
    public boolean merge(final Hasher hasher) {
        Objects.requireNonNull(hasher, "hasher");
        return merge(hasher.indices(shape));
    }

    @Override
    public boolean merge(final BloomFilter other) {
        Objects.requireNonNull(other, "other");
        return merge((BitMapProducer) other);
    }

    @Override
    public Shape getShape() {
        return shape;
    }

    @Override
    public int characteristics() {
        return 0;
    }

    @Override
    public int cardinality() {
        // Lazy evaluation with caching
        int c = cardinality;
        if (c < 0) {
            cardinality = c = SetOperations.cardinality(this);
        }
        return c;
    }

    @Override
    public boolean forEachIndex(final IntPredicate consumer) {
        Objects.requireNonNull(consumer, "consumer");
        return IndexProducer.fromBitMapProducer(this).forEachIndex(consumer);
    }

    @Override
    public boolean forEachBitMap(final LongPredicate consumer) {
        Objects.requireNonNull(consumer, "consumer");
        long[] myBitMap = bitMap;
        for (final long l : myBitMap) {
            if (!consumer.test(l)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean contains(final IndexProducer indexProducer) {
        long[] myBitMap = bitMap;
        return indexProducer.forEachIndex(idx -> BitMap.contains(myBitMap, idx));
    }
}
