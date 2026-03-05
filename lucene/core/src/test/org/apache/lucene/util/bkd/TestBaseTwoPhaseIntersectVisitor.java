/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.lucene.util.bkd;

import org.apache.lucene.index.PointValues;
import org.apache.lucene.tests.util.LuceneTestCase;
import org.apache.lucene.util.bkd.BKDReader.BaseTwoPhaseIntersectVisitor;
import org.apache.lucene.util.bkd.BKDReader.TwoPhaseIntersectVisitor.PrefetchMode;

public class TestBaseTwoPhaseIntersectVisitor extends LuceneTestCase {

  private BaseTwoPhaseIntersectVisitor createVisitor(PrefetchMode mode) {
    return new BaseTwoPhaseIntersectVisitor(mode) {
      @Override
      public void visit(int docID) {}

      @Override
      public void visit(int docID, byte[] packedValue) {}

      @Override
      public PointValues.Relation compare(byte[] minPackedValue, byte[] maxPackedValue) {
        return PointValues.Relation.CELL_CROSSES_QUERY;
      }
    };
  }

  public void testDefaultPrefetchModeIsFirstMatch() {
    BaseTwoPhaseIntersectVisitor visitor = createVisitor(PrefetchMode.FIRST_MATCH);
    assertEquals(PrefetchMode.FIRST_MATCH, visitor.prefetchMode());
  }

  public void testConstructorWithAllMatch() {
    BaseTwoPhaseIntersectVisitor visitor = createVisitor(PrefetchMode.ALL_MATCH);
    assertEquals(PrefetchMode.ALL_MATCH, visitor.prefetchMode());
  }

  public void testGlobalPrefetchModeDefault() {
    assertEquals(PrefetchMode.FIRST_MATCH, BaseTwoPhaseIntersectVisitor.getGlobalPrefetchMode());
  }

  public void testSetGlobalPrefetchMode() {
    PrefetchMode original = BaseTwoPhaseIntersectVisitor.getGlobalPrefetchMode();
    try {
      BaseTwoPhaseIntersectVisitor.setGlobalPrefetchMode(PrefetchMode.ALL_MATCH);
      assertEquals(PrefetchMode.ALL_MATCH, BaseTwoPhaseIntersectVisitor.getGlobalPrefetchMode());
    } finally {
      BaseTwoPhaseIntersectVisitor.setGlobalPrefetchMode(original);
    }
  }

  public void testGlobalModeDoesNotAffectExistingVisitor() {
    BaseTwoPhaseIntersectVisitor visitor = createVisitor(PrefetchMode.FIRST_MATCH);
    BaseTwoPhaseIntersectVisitor.setGlobalPrefetchMode(PrefetchMode.ALL_MATCH);
    try {
      assertEquals(PrefetchMode.FIRST_MATCH, visitor.prefetchMode());
    } finally {
      BaseTwoPhaseIntersectVisitor.setGlobalPrefetchMode(PrefetchMode.FIRST_MATCH);
    }
  }
}
