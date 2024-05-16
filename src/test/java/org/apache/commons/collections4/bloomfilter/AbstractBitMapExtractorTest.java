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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.function.LongPredicate;

import org.junit.jupiter.api.Test;

public abstract class AbstractBitMapExtractorTest {

    /**
     * A testing consumer that always returns false.
     */
    static final LongPredicate FALSE_CONSUMER = arg0 -> false;

    /**
     * A testing consumer that always returns true.
     */
    static final LongPredicate TRUE_CONSUMER = arg0 -> true;

    /**
     * Creates an producer without data.
     * @return a producer that has no data.
     */
    protected abstract BitMapExtractor createEmptyProducer();

    /**
     * Creates a producer with some data.
     * @return a producer with some data
     */
    protected abstract BitMapExtractor createProducer();

    protected boolean emptyIsZeroLength() {
        return false;
    }

    @Test
    public final void testAsBitMapArray() {
        long[] array = createEmptyProducer().asBitMapArray();
        for (int i = 0; i < array.length; i++) {
            assertEquals(0, array[i], "Wrong value at " + i);
        }

        array = createProducer().asBitMapArray();
        assertFalse(array.length == 0);
    }

    @Test
    public final void testForEachBitMap() {
        assertFalse(createProducer().processBitMaps(FALSE_CONSUMER), "non-empty should be false");
        if (emptyIsZeroLength()) {
            assertTrue(createEmptyProducer().processBitMaps(FALSE_CONSUMER), "empty should be true");
        } else {
            assertFalse(createEmptyProducer().processBitMaps(FALSE_CONSUMER), "empty should be false");
        }

        assertTrue(createProducer().processBitMaps(TRUE_CONSUMER), "non-empty should be true");
        assertTrue(createEmptyProducer().processBitMaps(TRUE_CONSUMER), "empty should be true");
    }

    @Test
    public void testForEachBitMapEarlyExit() {
        final int[] passes = new int[1];
        assertFalse(createProducer().processBitMaps(l -> {
            passes[0]++;
            return false;
        }));
        assertEquals(1, passes[0]);

        passes[0] = 0;
        if (emptyIsZeroLength()) {
            assertTrue(createEmptyProducer().processBitMaps(l -> {
                passes[0]++;
                return false;
            }));
            assertEquals(0, passes[0]);
        } else {
            assertFalse(createEmptyProducer().processBitMaps(l -> {
                passes[0]++;
                return false;
            }));
            assertEquals(1, passes[0]);
        }
    }

    @Test
    public final void testForEachBitMapPair() {
        final LongBiPredicate func = (x, y) -> x == y;
        assertTrue(createEmptyProducer().processBitMapPairs(createEmptyProducer(), func), "empty == empty failed");
        assertFalse(createEmptyProducer().processBitMapPairs(createProducer(), func), "empty == not_empty failed");
        assertFalse(createProducer().processBitMapPairs(createEmptyProducer(), func), "not_empty == empty passed");
        assertTrue(createProducer().processBitMapPairs(createProducer(), func), "not_empty == not_empty failed");

        // test BitMapProducers of different length send 0 for missing values.
        final int[] count = new int[3];
        final LongBiPredicate lbp = (x, y) -> {
            if (x == 0) {
                count[0]++;
            }
            if (y == 0) {
                count[1]++;
            }
            count[2]++;
            return true;
        };
        createEmptyProducer().processBitMapPairs(createProducer(), lbp);
        assertEquals(count[2], count[0]);

        Arrays.fill(count, 0);
        createProducer().processBitMapPairs(createEmptyProducer(), lbp);
        assertEquals(count[2], count[1]);

        // test where the created producer does not process all records because the predicate function
        // returns false before the processing is completed.
        final int[] limit = new int[1];
        final LongBiPredicate shortFunc =  (x, y) -> {
            limit[0]++;
            return limit[0] < 2;
        };
        final BitMapExtractor shortProducer = l -> true;
        assertFalse(createProducer().processBitMapPairs(shortProducer, shortFunc));
    }

    @Test
    public void testForEachBitMapPairEarlyExit() {

        // test BitMapProducers of different length send 0 for missing values.
        final int[] count = new int[1];
        final LongBiPredicate lbp = (x, y) -> {
            count[0]++;
            return false;
        };
        createProducer().processBitMapPairs(createEmptyProducer(), lbp);
        assertEquals(1, count[0]);

        Arrays.fill(count, 0);
        createEmptyProducer().processBitMapPairs(createProducer(), lbp);
        assertEquals(1, count[0]);
    }
}
