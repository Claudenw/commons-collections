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

import org.apache.commons.collections4.bloomfilter.hasher.Hasher;

/**
 * The interface that describes an updatable Bloom filter.
 * @since 4.5
 */
public interface UpdatableBloomFilter extends BloomFilter {


    // Modification Operations

    /**
     * Merges the specified Bloom filter into this Bloom filter. Specifically all bit indexes
     * that are enabled in the {@code other} filter will be enabled in this filter.
     *
     * <p>Note: This method should return {@code true} even if no additional bit indexes were
     * enabled. A {@code false} result indicates that this filter is not ensured to contain
     * the {@code other} Bloom filter.
     *
     * @param other the other Bloom filter
     * @return true if the merge was successful
     * @throws IllegalArgumentException if the shape of the other filter does not match
     * the shape of this filter
     */
    boolean merge(BloomFilter other);

    /**
     * Merges the specified decomposed Bloom filter into this Bloom filter. Specifically all
     * bit indexes that are identified by the {@code hasher} will be enabled in this filter.
     *
     * <p>Note: This method should return {@code true} even if no additional bit indexes were
     * enabled. A {@code false} result indicates that this filter is not ensured to contain
     * the specified decomposed Bloom filter.
     *
     * @param hasher the hasher to provide the indexes
     * @return true if the merge was successful
     * @throws IllegalArgumentException if the hasher cannot generate indices for the shape of
     * this filter
     */
    boolean merge(Hasher hasher);

}
