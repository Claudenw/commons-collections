/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.commons.collections4.bloomfilter;

import java.util.BitSet;

/**
 * A Bloom filter.
 *
 * Instances are immutable.
 *
 * @since 4.5
 */
public class StandardBloomFilter implements BloomFilter {
    /**
     * An empty BloomFilter
     */
    public static final StandardBloomFilter EMPTY = new StandardBloomFilter(new BitSet(0));



    /**
     * The BitSet that represents the Bloom filter.
     */
    private final BitSet bitSet;


    /**
     * Constructor.
     *
     * @param protoFilter the protoFilter to build this Bloom filter from.
     * @param config      the Filter configuration to use to build the Bloom filter.
     */
    public StandardBloomFilter(ProtoBloomFilter protoFilter, BloomFilterConfiguration config) {
        this.bitSet = new BitSet(config.getNumberOfBits());
        protoFilter.getUniqueHashes().forEach(hash -> hash.populate(bitSet, config));
    }

    /**
     * Constructor.
     *
     * A copy of the bitSet parameter is made so that the Bloom filter is isolated
     * from any further changes in the bitSet.
     *
     * @param bitSet The bit set that was built by the config.
     */
    public StandardBloomFilter(BitSet bitSet) {
        this.bitSet = (BitSet) bitSet.clone();
    }

    @Override
    public final boolean inverseMatches(final BloomFilter other) {
        return other.matches(this);
    }

    @Override
    public final boolean matches(final BloomFilter other) {
        BitSet temp = other.getBitSet();
        temp.and(bitSet);
        return temp.equals(bitSet);
    }


    @Override
    public final int getHammingWeight() {
        return bitSet.cardinality();
    }



    @Override
    public String toString() {
        return bitSet.toString();
    }

    @Override
    public int hashCode() {
        return bitSet.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof StandardBloomFilter && this.bitSet.equals(((StandardBloomFilter) other).bitSet);
    }

    @Override
    public StandardBloomFilter merge(BloomFilter other) {
        BitSet next = other.getBitSet();
        next.or(bitSet);
        return new StandardBloomFilter(next);
    }

    @Override
    public final BitSet getBitSet() {
        return (BitSet) bitSet.clone();
    }
}
