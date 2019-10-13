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

import static org.junit.Assert.assertEquals;

import java.util.BitSet;

import org.junit.Test;

/**
 * Tests for the bloom filter functions.
 */
public class BloomFilterFunctionsTest {

    @Test
    public void hammingDistanceTest_Not0() {
        BitSet bs = new BitSet();
        bs.set(2);
        StandardBloomFilter bf1 = new StandardBloomFilter(bs);
        bs.set(4);
        StandardBloomFilter bf2 = new StandardBloomFilter(bs);

        assertEquals(1, BloomFilterFunctions.getHammingDistance(bf1, bf2));
        assertEquals(1, BloomFilterFunctions.getHammingDistance(bf2, bf1));

        // show now shared bits
        bs = new BitSet();
        bs.set(4);
        bf2 = new StandardBloomFilter(bs);
        assertEquals(2,  BloomFilterFunctions.getHammingDistance(bf1, bf2));
        assertEquals(2,  BloomFilterFunctions.getHammingDistance(bf2, bf1));

    }

    @Test
    public void hammingDistanceTest_0() {
        BitSet bs = new BitSet();
        bs.set(2);
        StandardBloomFilter bf1 = new StandardBloomFilter(bs);
        StandardBloomFilter bf2 = new StandardBloomFilter(bs);

        assertEquals(0, BloomFilterFunctions.getHammingDistance(bf1, bf2));
        assertEquals(0, BloomFilterFunctions.getHammingDistance(bf2, bf1));
    }

    @Test
    public final void hammingDistanceTest_FromEmpty() {
        BitSet bs = new BitSet();
        for (int i=1;i<5;i++)
        {
            bs.set(i);
            BloomFilter bf = new StandardBloomFilter( bs );
            assertEquals( bf.getHammingWeight(), BloomFilterFunctions.getHammingDistance( bf,  StandardBloomFilter.EMPTY ) );
        }
    }
    @Test
    public void getLogTest_NoValues() {
        BitSet bs = new BitSet();
        BloomFilter bf = new StandardBloomFilter(bs);
        assertEquals(0.0, BloomFilterFunctions.getApproximateLog(bf), 0.0000001);
    }

    @Test
    public void getLogTest() {
        BitSet bs = new BitSet();
        bs.set(2);
        BloomFilter bf = new StandardBloomFilter(bs);
        assertEquals(2.0, BloomFilterFunctions.getApproximateLog(bf), 0.0000001);

        bs.set(20);
        bf = new StandardBloomFilter(bs);
        assertEquals(20.000003814697266, BloomFilterFunctions.getApproximateLog(bf), 0.000000000000001);
    }

    @Test
    public void getLogTest_largeBitSpacing() {
        BitSet bs = new BitSet();
        bs.set(2);
        bs.set(40);
        // show it is approximate bit 40 and bit 2 yeilds same as 40 alone
        BloomFilter bf = new StandardBloomFilter(bs);

        assertEquals(40.0, BloomFilterFunctions.getApproximateLog(bf), 0.000000000000001);
    }

    @Test
    public void getLogTest_FullBuffer() {
        BitSet bs = new BitSet();
        for (int i = 0; i < 28; i++) {
            bs.set(i);
        }

        BloomFilter bf = new StandardBloomFilter(bs);

        assertEquals(27.9999999, BloomFilterFunctions.getApproximateLog(bf), 0.0000001);
    }



}
