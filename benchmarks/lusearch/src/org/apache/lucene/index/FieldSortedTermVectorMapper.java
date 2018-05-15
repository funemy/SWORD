package org.apache.lucene.index;

import java.util.*;

/**
 * Copyright 2007 The Apache Software Foundation
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * For each Field, store a sorted collection of {@link TermVectorEntry}s
 * <p/>
 * This is not thread-safe.
 */
public class FieldSortedTermVectorMapper extends TermVectorMapper{
  private Map fieldToTerms = new HashMap();
  private SortedSet currentSet;
  private String currentField;
  private Comparator comparator;

  /**
   *
   * @param comparator A Comparator for sorting {@link TermVectorEntry}s
   */
  public FieldSortedTermVectorMapper(Comparator comparator) {
    this(false, false, comparator);
  }


  public FieldSortedTermVectorMapper(boolean ignoringPositions, boolean ignoringOffsets, Comparator comparator) {
    super(ignoringPositions, ignoringOffsets);
    this.comparator = comparator;
  }

  public void map(String term, int frequency, TermVectorOffsetInfo[] offsets, int[] positions) {
    TermVectorEntry entry = new TermVectorEntry(currentField, term, frequency, offsets, positions);
    currentSet.add(entry);
  }

  public void setExpectations(String field, int numTerms, boolean storeOffsets, boolean storePositions) {
    currentSet = new TreeSet(comparator);
    currentField = field;
    fieldToTerms.put(field, currentSet);
  }

  /**
   * Get the mapping between fields and terms, sorted by the comparator
   *
   * @return A map between field names and {@link java.util.SortedSet}s per field.  SortedSet entries are {@link TermVectorEntry}
   */
  public Map getFieldToTerms() {
    return fieldToTerms;
  }


  public Comparator getComparator() {
    return comparator;
  }
}
