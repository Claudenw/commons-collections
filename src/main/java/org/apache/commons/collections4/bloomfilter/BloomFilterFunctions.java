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
 * A static class to calculate well known functions for BloomFilters.
 *
 * @since 4.5
 */
public final class BloomFilterFunctions {

    /**
     * The maximum log depth at which the log calculation makes no difference to the
     * result.
     */
    private final static int MAX_LOG_DEPTH = 25;

    /**
     * Gets the approximate log for the specified filter. If the Bloom filter is
     * considered as an unsigned number what is the approximate base 2 log of that value.
     *
     * @param bloomFilter the number filter to calcualte from.
     * @return the approximate log.
     */
    public static double getApproximateLog(BloomFilter bloomFilter) {
        return getApproximateLog(bloomFilter.getBitSet());
    }

    /**
     * Gets the approximate log for this filter. If the Bloom filter is considered as an
     * unsigned number what is the approximate base 2 log of that value.
     *
     * @param bitset The bitset to estimate from.
     * @return the approximate log.
     */
    private static double getApproximateLog(BitSet bitSet) {
        return getApproximateLog(bitSet, Integer.min(bitSet.length(), MAX_LOG_DEPTH));
    }

    /**
     * Gets the approximate log for this filter. If the Bloom filter is considered as an
     * unsigned number what is the approximate base 2 log of that value. The depth
     * argument indicates how many extra bits are to be considered in the log calculation.
     * At least one bit must be considered. If there are no bits on then the log value is
     * 0.
     * <p> this approximation is calculated using a derivation of the binary logarithm
     * algorithm as noted on wikipedia</p>
     *
     * @see <a href="http://en.wikipedia.org/wiki/Binary_logarithm#Algorithm">Binary Logarithm [Wikipedia]</a>
     * @param depth the number of bits to consider.
     * @return the approximate log.
     */
    private static double getApproximateLog(BitSet bitset, int depth) {
        if (depth == 0) {
            return 0;
        }
        int[] exp = getApproximateLogExponents(bitset, depth);
        // the mantissa is the highest bit that is turned on.
        if (exp[0] < 0) {
            // there are no bits so return 0
            return 0;
        }
        double result = exp[0];
        /*
         * now we move backwards from the highest bit until the requested depth is
         * achieved.
         */
        double exp2;
        for (int i = 1; i < exp.length; i++) {
            if (exp[i] == -1) {
                return result;
            }
            exp2 = exp[i] - exp[0]; // should be negative
            result += Math.pow(2.0, exp2);
        }
        return result;
    }

    /**
     * Gets the mantissa and characteristic powers of the log. The mantissa is in position
     * position 0. The remainder are characteristic powers.
     *
     * The depth is the depth to probe for characteristics. The effective limit is 25 as
     * beyond that the value of the calculated double does not change.
     *
     * @param depth the depth to probe.
     * @return An array of depth integers that are the exponents.
     */
    private static int[] getApproximateLogExponents(BitSet bitSet, int depth) {
        if (depth < 1) {
            return new int[] {-1};
        }
        int[] exp = new int[depth];

        exp[0] = bitSet.length() - 1;
        if (exp[0] < 0) {
            return exp;
        }

        for (int i = 1; i < depth; i++) {
            exp[i] = bitSet.previousSetBit(exp[i - 1] - 1);
            if (exp[i] - exp[0] < -MAX_LOG_DEPTH) {
                exp[i] = -1;
            }
            if (exp[i] == -1) {
                return exp;
            }
        }
        return exp;
    }

    /**
     * Gets the Hamming distance between two BloomFilters. The Hamming distance is defined
     * as the number of bits that need to be "flipped" to convert one filter to the other.
     * This is the same as the xor between their bit vector representations.
     *
     * @param bloomFilter1 the first BloomFilter.
     * @param bloomFilter2 the second BloomFilter.
     * @return the Hamming distance between the filters.
     */
    public static int getHammingDistance(BloomFilter bloomFilter1, BloomFilter bloomFilter2) {
        BitSet bs1 = bloomFilter1.getBitSet();
        BitSet bs2 = bloomFilter2.getBitSet();
        bs1.xor(bs2);
        return bs1.cardinality();
    }

    /**
     * Calculates the Jaccard Similarity between the two filters. The Jaccard Similarity
     * defined as: cardinality( A xor B ) / cardinality( A or B )
     *
     * @param bloomFilter1 the first bloom filter
     * @param bloomFilter2 the second bloom filter
     * @return the Jaccard Similarity.
     */
    public static double getJaccardSimilarity(BloomFilter bloomFilter1, BloomFilter bloomFilter2) {
        BitSet denom = bloomFilter1.getBitSet();
        denom.or(bloomFilter2.getBitSet());
        return getHammingDistance(bloomFilter1, bloomFilter2) * 1.0 / denom.cardinality();
    }

    /**
     * Calculates the Jaccard Distance between two filters. The Jaccard Distance is
     * defined as: 1.0 - jaccardDiscance( A, B );
     *
     * @param bloomFilter1 the first bloom filter.
     * @param bloomFilter2 the second bloom filter.
     * @return the jaccard distance.
     */
    public static double getJaccardDistance(BloomFilter bloomFilter1, BloomFilter bloomFilter2) {
        return 1.0 - getJaccardSimilarity(bloomFilter1, bloomFilter2);
    }

    /**
     * Calculates the consine similarity between two filters. The cosine similarity is
     * defined as: cardinality( AandB )/ (sqrt( cardinality( a ))* sqrt( cardinality( b
     * )))
     *
     * @param bloomFilter1 the first bloom filter.
     * @param bloomFilter2 the second bloom filter.
     * @return the cosine similarity.
     */
    public static double getCosineSimilarity(BloomFilter bloomFilter1, BloomFilter bloomFilter2) {
        int cardA = bloomFilter1.getHammingWeight();
        int cardB = bloomFilter2.getHammingWeight();
        if (cardA == 0 || cardB == 0) {
            return 0.0;
        }
        double denom = Math.sqrt(cardA) * Math.sqrt(cardB);
        if (denom == 0) {
            return 0.0;
        }
        BitSet ab = bloomFilter1.getBitSet();
        ab.and(bloomFilter2.getBitSet());
        return ab.cardinality() / denom;
    }

    /**
     * Calculates the cosine distance between two filters. The cosine distance is defined
     * as: 1.0 - cosineSimilarity( A, B )
     *
     * @param bloomFilter1 the first bloom filter.
     * @param bloomFilter2 the second bloom filter.
     * @return the cosine distance.
     */
    public static Double getCosineDistance( BloomFilter bloomFilter1, BloomFilter bloomFilter2) {
        return 1.0 - getCosineSimilarity(bloomFilter1, bloomFilter2);
    }

}
