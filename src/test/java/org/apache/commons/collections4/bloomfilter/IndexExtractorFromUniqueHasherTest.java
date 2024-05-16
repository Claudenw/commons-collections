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

public class IndexExtractorFromUniqueHasherTest extends AbstractIndexExtractorTest {

    @Override
    protected IndexExtractor createEmptyExtractor() {
        return NullHasher.INSTANCE.indices(Shape.fromKM(17, 72));
    }

    @Override
    protected IndexExtractor createExtractor() {
        // hasher has collisions and wraps
        return new IncrementingHasher(4, 8).indices(Shape.fromKM(17, 72)).uniqueIndices();
    }

    @Override
    protected int getAsIndexArrayBehaviour() {
        return DISTINCT;
    }

    @Override
    protected int[] getExpectedIndices() {
        return new int[] {4, 12, 20, 28, 36, 44, 52, 60, 68};
    }
}
