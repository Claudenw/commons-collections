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

import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Implementation of the methods to manage the Layers in a Layered Bloom filter.
 * <p>
 * The manager comprises a list of Bloom filters that are managed based on
 * various rules. The last filter in the list is known as the {@code target} and
 * is the filter into which merges are performed. The Layered manager utilizes
 * three methods to manage the list.
 * </p>
 * <ul>
 * <li>ExtendCheck - A Predicate that if true causes a new Bloom filter to be
 * created as the new target.</li>
 * <li>FilterSupplier - A Supplier that produces empty Bloom filters to be used
 * as a new target.</li>
 * <li>Cleanup - A Consumer of a LinkedList of BloomFilter that removes any
 * expired or out dated filters from the list.</li>
 * </ul>
 * <p>
 * When extendCheck returns {@code true} the following steps are taken:
 * </p>
 * <ol>
 * <li>If the current target is empty it is removed.</li>
 * <li>{@code Cleanup} is called</li>
 * <li>{@code FilterSuplier} is executed and the new filter added to the list as
 * the {@code target} filter.</li>
 * </ol>
 *
 * @since 4.5
 */
public class LayerManager implements BloomFilterProducer {

    /**
     * A collection of common ExtendCheck implementations to test whether to extend
     * the depth of a LayerManager.
     */
    public static class ExtendCheck {
        private ExtendCheck() {
        }

        private static final Predicate<LayerManager> ADVANCE_ON_POPULATED = lm -> {
            return !lm.filters.peekLast().isEmpty();
        };

        private static final Predicate<LayerManager> NEVER_ADVANCE = x -> false;

        /**
         * Advances the target once a merge has been performed.
         */
        public static final Predicate<LayerManager> advanceOnPopulated() {
            return ADVANCE_ON_POPULATED;
        }

        /**
         * Does not automatically advance the target. next() must be called directly to
         * perform the advance.
         */
        public static final Predicate<LayerManager> neverAdvance() {
            return NEVER_ADVANCE;
        }

        /**
         * Calculates the estimated number of Bloom filters (n) that have been merged
         * into the target and compares that with the estimated maximum expected
         * {@code n} based on the shape. If the target is saturated then a new target is
         * created.
         *
         * @param shape The shape of the filters in the LayerManager.
         * @return A Predicate suitable for the LayerManager extendCheck parameter.
         */
        public static final Predicate<LayerManager> advanceOnCalculatedSaturation(Shape shape) {
            return advanceOnSaturation(shape.estimateMaxN());
        }

        /**
         * Creates a new target after a specific number of filters have been added to
         * the current target.
         *
         * @param breakAt the number of filters to merge into each filter in the list.
         * @return A Predicate suitable for the LayerManager extendCheck parameter.
         * @throws IllegalArgumentException if breakAt is &lt;= 0
         */
        public static Predicate<LayerManager> advanceOnCount(int breakAt) {
            if (breakAt <= 0) {
                throw new IllegalArgumentException("'breakAt' must be greater than 0");
            }
            return new Predicate<LayerManager>() {
                int count = 0;

                @Override
                public boolean test(LayerManager filter) {
                    return ++count % breakAt == 0;
                }
            };
        }

        /**
         * Creates a new target after the current target is saturated. Saturation is
         * defined as the estimated N of the target Bloom filter being greater than the
         * maxN specified.
         *
         * @param maxN the maximum number of estimated items in the filter.
         * @return A Predicate suitable for the LayerManager extendCheck parameter.
         * @throws IllegalArgumentException if maxN is &lt;= 0
         */
        public static final Predicate<LayerManager> advanceOnSaturation(double maxN) {
            if (maxN <= 0) {
                throw new IllegalArgumentException("'maxN' must be greater than 0");
            }
            return manager -> {
                BloomFilter bf = manager.filters.peekLast();
                return maxN <= bf.getShape().estimateN(bf.cardinality());
            };
        }
    }

    /**
     * Static methods to create a Consumer of a LinkedList of BloomFilter perform
     * tests on whether to reduce the collecton of Bloom filters.
     *
     */
    public static class Cleanup {
        private Cleanup() {
        }

        private static final Consumer<LinkedList<BloomFilter>> NO_CLEANUP = x -> {
        };

        private static final Consumer<LinkedList<BloomFilter>> REMOVE_EMPTY_TARGET = x -> {
            if (x.getLast().cardinality() == 0) {
                x.removeLast();
            }
        };

        /**
         * A Cleanup that never removes anything.
         */
        public static final Consumer<LinkedList<BloomFilter>> noCleanup() {
            return NO_CLEANUP;
        }

        /**
         * Removes the earliest filters in the list when the the number of filters
         * exceeds maxSize.
         *
         * @param maxSize the maximum number of filters for the list. Must be greater
         *                than 0
         * @return A Consumer for the LayerManager filterCleanup constructor argument.
         * @throws IllegalArgumentException if maxSize is &lt;= 0
         */
        public static final Consumer<LinkedList<BloomFilter>> onMaxSize(int maxSize) {
            if (maxSize <= 0) {
                throw new IllegalArgumentException("'maxSize' must be greater than 0");
            }
            return ll -> {
                while (ll.size() > maxSize) {
                    ll.removeFirst();
                }
            };
        }

        /**
         * Removes the last added target if it is empty.
         *
         * @return A Consumer for the LayerManager filterCleanup constructor argument.
         */
        public static final Consumer<LinkedList<BloomFilter>> removeEmptyTarget() {
            return REMOVE_EMPTY_TARGET;
        }
    }

    private final LinkedList<BloomFilter> filters = new LinkedList<>();
    private final Consumer<LinkedList<BloomFilter>> filterCleanup;
    private final Predicate<LayerManager> extendCheck;
    private final Supplier<BloomFilter> filterSupplier;

    /**
     * Creates a new Builder with defaults of NEVER_ADVANCE and NO_CLEANUP
     *
     * @return A builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Constructor.
     *
     * @param filterSupplier the supplier of new Bloom filters to add the the list
     *                       when necessary.
     * @param extendCheck    The predicate that checks if a new filter should be
     *                       added to the list.
     * @param filterCleanup  the consumer that removes any old filters from the
     *                       list.
     * @param initialize     true if the filter list should be initialized.
     */
    private LayerManager(Supplier<BloomFilter> filterSupplier, Predicate<LayerManager> extendCheck,
            Consumer<LinkedList<BloomFilter>> filterCleanup, boolean initialize) {
        this.filterSupplier = filterSupplier;
        this.extendCheck = extendCheck;
        this.filterCleanup = filterCleanup;
        if (initialize) {
            addFilter();
        }
    }

    /**
     * Adds a new Bloom filter to the list.
     */
    private void addFilter() {
        BloomFilter bf = filterSupplier.get();
        if (bf == null) {
            throw new NullPointerException("filterSupplier returned null.");
        }
        filters.add(bf);
    }

    /**
     * Creates a deep copy of this LayerManager.
     *
     * @return a copy of this layer Manager.
     */
    public LayerManager copy() {
        LayerManager newMgr = new LayerManager(filterSupplier, extendCheck, filterCleanup, false);
        newMgr.filters.clear();
        for (BloomFilter bf : filters) {
            newMgr.filters.add(bf.copy());
        }
        return newMgr;
    }

    /**
     * Forces an advance to the next depth. This method will clean-up the current
     * layers and generate a new filter layer.
     * <p>
     * Ths method is used within the {@link #getTarget()} when the configured
     * {@code ExtendCheck} returns {@code true}.
     * </p>
     */
    void next() {
        this.filterCleanup.accept(filters);
        addFilter();
    }

    /**
     * Returns the number of filters in the LayerManager.
     *
     * @return the current depth.
     */
    public final int getDepth() {
        return filters.size();
    }

    /**
     * Gets the Bloom filter at the specified depth. The filter at depth 0 is the
     * oldest filter.
     *
     * @param depth the depth at which the desired filter is to be found.
     * @return the filter.
     * @throws NoSuchElementException if depth is not in the range
     *                                [0,filters.size())
     */
    public final BloomFilter get(int depth) {
        if (depth < 0 || depth >= filters.size()) {
            throw new NoSuchElementException(String.format("Depth must be in the range [0,%s)", filters.size()));
        }
        return filters.get(depth);
    }

    /**
     * Returns the current target filter. If a new filter should be created based on
     * {@code extendCheck} it will be created before this method returns.
     *
     * @return the current target filter.
     */
    public final BloomFilter getTarget() {
        if (extendCheck.test(this)) {
            next();
        }
        return filters.peekLast();
    }

    /**
     * Clear all the filters in the layer manager, and set up a new one as the
     * target.
     */
    public final void clear() {
        filters.clear();
        addFilter();
    }

    /**
     * Executes a Bloom filter Predicate on each Bloom filter in the manager in
     * depth order. Oldest filter first.
     *
     * @param bloomFilterPredicate the predicate to evaluate each Bloom filter with.
     * @return {@code false} when the first filter fails the predicate test. Returns
     *         {@code true} if all filters pass the test.
     */
    @Override
    public boolean forEachBloomFilter(Predicate<BloomFilter> bloomFilterPredicate) {
        for (BloomFilter bf : filters) {
            if (!bloomFilterPredicate.test(bf)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Builder to create Layer Manager
     */
    public static class Builder {
        private Predicate<LayerManager> extendCheck;
        private Supplier<BloomFilter> supplier;
        private Consumer<LinkedList<BloomFilter>> cleanup;

        private Builder() {
            extendCheck = ExtendCheck.NEVER_ADVANCE;
            cleanup = Cleanup.NO_CLEANUP;
        }

        /**
         * Builds the layer manager with the specified properties.
         *
         * @return a new LayerManager.
         */
        public LayerManager build() {
            if (supplier == null) {
                throw new NullPointerException("Supplier must not be null");
            }
            if (extendCheck == null) {
                throw new NullPointerException("ExtendCheck must not be null");
            }
            if (cleanup == null) {
                throw new NullPointerException("Cleanup must not be null");
            }
            return new LayerManager(supplier, extendCheck, cleanup, true);
        }

        /**
         * Sets the extendCheck predicate. When the predicate returns {@code true} a new
         * target will be created.
         *
         * @param extendCheck The predicate to determine if a new target should be
         *                    created.
         * @return this for chaining.
         */
        public Builder extendCheck(Predicate<LayerManager> extendCheck) {
            this.extendCheck = extendCheck;
            return this;
        }

        /**
         * Sets the supplier of Bloom filters. When extendCheck creates a new target,
         * the supplier provides the instance of the Bloom filter.
         *
         * @param supplier The supplier of new Bloom filter instances.
         * @return this for chaining.
         */
        public Builder supplier(Supplier<BloomFilter> supplier) {
            this.supplier = supplier;
            return this;
        }

        /**
         * Sest the Consumer that cleans the list of Bloom filters.
         *
         * @param cleanup the Consumer that will modify the list of filters removing out
         *                dated or stale filters.
         * @return this for chaining.
         */
        public Builder cleanup(Consumer<LinkedList<BloomFilter>> cleanup) {
            this.cleanup = cleanup;
            return this;
        }
    }
}
