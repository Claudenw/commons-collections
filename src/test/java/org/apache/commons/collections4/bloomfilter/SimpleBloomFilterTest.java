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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link SimpleBloomFilter}.
 */
public class SimpleBloomFilterTest extends AbstractBloomFilterTest<SimpleBloomFilter> {
    @Override
    protected SimpleBloomFilter createEmptyFilter(final Shape shape) {
        return new SimpleBloomFilter(shape);
    }

    @Test
    public void testMergeShortBitMapProducer() {
        SimpleBloomFilter filter = createEmptyFilter(getTestShape());
        // create a producer that returns too few values
        // shape expects 2 longs we are sending 1.
        BitMapProducer producer = p -> {
            return p.test(2L);
        };
        assertTrue(filter.merge(producer));
        assertEquals(1, filter.cardinality());
    }

    @Test
    public void testCardinalityAndIsEmpty() {
        testCardinalityAndIsEmpty(createEmptyFilter(getTestShape()));

        // test the case where is empty after merge
        // in this case the internal cardinality == -1
        BloomFilter bf = createEmptyFilter(getTestShape());
        bf.merge(IndexProducer.fromIndexArray());
        assertTrue(bf.isEmpty());
    }

}
