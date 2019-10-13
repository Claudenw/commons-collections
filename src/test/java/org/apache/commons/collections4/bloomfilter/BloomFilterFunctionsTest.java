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


    /*
     * The Jaccard Similarity is defined as: cardinality( A xor B ) / cardinality( A or B )
     *
     * @param bloomFilter1 the first bloom filter
     * @param bloomFilter2 the second bloom filter
     * @return the Jaccard Similarity.
     */

    @Test
    public void jaccardSimilarityTest() {
        BitSet bs1 = new BitSet();
        bs1.set(1);
        bs1.set(2);

        BitSet bs2 = new BitSet();
        bs2.set(2);
        bs2.set(3);

        BloomFilter bf1 = new StandardBloomFilter( bs1 );
        BloomFilter bf2 = new StandardBloomFilter( bs2 );
        // should be 2/3
        assertEquals( 0.6666666666666666, BloomFilterFunctions.getJaccardSimilarity( bf1, bf2 ), 0.00000000000000001);

    }

    @Test
    public void jaccardDistanceTest() {
        BitSet bs1 = new BitSet();
        bs1.set(1);
        bs1.set(2);

        BitSet bs2 = new BitSet();
        bs2.set(2);
        bs2.set(3);

        BloomFilter bf1 = new StandardBloomFilter( bs1 );
        BloomFilter bf2 = new StandardBloomFilter( bs2 );
        // should be 1/3
        assertEquals( 0.33333333333333337, BloomFilterFunctions.getJaccardDistance( bf1, bf2 ), 0.00000000000000001);

    }

    /*
     * The consine similarity between two filters is
     * defined as: cardinality( AandB )/ (sqrt( cardinality( a ))* sqrt( cardinality( b
     * )))
     *
     * @param bloomFilter1 the first bloom filter.
     * @param bloomFilter2 the second bloom filter.
     * @return the cosine similarity.
     */

    @Test
    public void cosineSimilarityTest() {
        BitSet bs1 = new BitSet();
        bs1.set(1);
        bs1.set(2);
        bs1.set(3);
        bs1.set(4);

        BitSet bs2 = new BitSet();
        bs2.set(11);
        bs2.set(12);
        bs2.set(13);
        bs2.set(14);

        BloomFilter bf1 = new StandardBloomFilter( bs1 );
        BloomFilter bf2 = new StandardBloomFilter( bs2 );
        // should be 0 / (sqrt(4) * sqrt(4))
        assertEquals( 0.0, BloomFilterFunctions.getCosineSimilarity( bf1, bf2 ), 0.1);

        bs2 = new BitSet();
        bs2.set(2);
        bs2.set(3);
        bs2.set(4);
        bs2.set(5);
        bf2 = new StandardBloomFilter( bs2 );
        // should be 3 / (sqrt(4) * sqrt(4))
        assertEquals( 0.75, BloomFilterFunctions.getCosineSimilarity( bf1, bf2 ), 0.1);

        bs2 = new BitSet();
        bs2.set(2);
        bs2.set(3);
        bs2.set(4);
        bs2.set(5);
        bs2.set(6);
        bs2.set(7);
        bs2.set(8);
        bs2.set(9);
        bs2.set(10);
        bf2 = new StandardBloomFilter( bs2 );
        // should be 3 / (sqrt(4) * sqrt(9)) = 3/(2*3) = .5
        assertEquals( 0.5, BloomFilterFunctions.getCosineSimilarity( bf1, bf2 ), 0.1);

    }

    @Test
    public void cosineSimilarityTest_Zeros() {
        BitSet bs1 = new BitSet();
        bs1.set(1);
        bs1.set(2);
        bs1.set(3);
        bs1.set(4);

        BloomFilter bf1 = new StandardBloomFilter( bs1 );

        assertEquals( 0.0, BloomFilterFunctions.getCosineSimilarity( bf1, StandardBloomFilter.EMPTY ), 0.0);
        assertEquals( 0.0, BloomFilterFunctions.getCosineSimilarity( StandardBloomFilter.EMPTY, bf1 ), 0.0);
        assertEquals( 0.0, BloomFilterFunctions.getCosineSimilarity( StandardBloomFilter.EMPTY,
            StandardBloomFilter.EMPTY ), 0.0);

    }

    @Test
    public void cosineDistanceTest() {
        BitSet bs1 = new BitSet();
        bs1.set(1);
        bs1.set(2);
        bs1.set(3);
        bs1.set(4);

        BloomFilter bf1 = new StandardBloomFilter( bs1 );

        assertEquals( 1.0, BloomFilterFunctions.getCosineDistance( bf1, StandardBloomFilter.EMPTY ), 0.0);
        assertEquals( 1.0, BloomFilterFunctions.getCosineDistance( StandardBloomFilter.EMPTY, bf1 ), 0.0);
        assertEquals( 1.0, BloomFilterFunctions.getCosineDistance( StandardBloomFilter.EMPTY,
            StandardBloomFilter.EMPTY ), 0.0);

        BitSet bs2 = new BitSet();
        bs2.set(11);
        bs2.set(12);
        bs2.set(13);
        bs2.set(14);

        BloomFilter bf2 = new StandardBloomFilter( bs2 );
        assertEquals( 1.0, BloomFilterFunctions.getCosineDistance( bf1, bf2 ), 0.0);

        bs2 = new BitSet();
        bs2.set(2);
        bs2.set(3);
        bs2.set(4);
        bs2.set(5);
        bf2 = new StandardBloomFilter( bs2 );
        // should be 3 / (sqrt(4) * sqrt(4))
        assertEquals( 0.25, BloomFilterFunctions.getCosineDistance( bf1, bf2 ), 0.1);
    }
}
