package org.apache.lucene.search;

/**
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

import java.io.IOException;

import org.apache.lucene.index.IndexReader;

/** A {@link HitCollector} implementation that collects the top-sorting
 * documents, returning them as a {@link TopFieldDocs}.  This is used by {@link
 * IndexSearcher} to implement {@link TopFieldDocs}-based search.
 *
 * <p>This may be extended, overriding the collect method to, e.g.,
 * conditionally invoke <code>super()</code> in order to filter which
 * documents are collected.
 *
 * @deprecated Please use {@link TopFieldCollector} instead.
 */
public class TopFieldDocCollector extends TopDocCollector {

  private FieldDoc reusableFD;

  /** Construct to collect a given number of hits.
   * @param reader the index to be searched
   * @param sort the sort criteria
   * @param numHits the maximum number of hits to collect
   */
  public TopFieldDocCollector(IndexReader reader, Sort sort, int numHits)
    throws IOException {
    super(new FieldSortedHitQueue(reader, sort.fields, numHits));
  }

  // javadoc inherited
  public void collect(int doc, float score) {
    if (score > 0.0f) {
      totalHits++;
      if (reusableFD == null)
        reusableFD = new FieldDoc(doc, score);
      else {
        // Whereas TopScoreDocCollector can skip this if the
        // score is not competitive, we cannot because the
        // comparators in the FieldSortedHitQueue.lessThan
        // aren't in general congruent with "higher score
        // wins"
        reusableFD.score = score;
        reusableFD.doc = doc;
      }
      reusableFD = (FieldDoc) hq.insertWithOverflow(reusableFD);
    }
  }

  // javadoc inherited
  public TopDocs topDocs() {
    FieldSortedHitQueue fshq = (FieldSortedHitQueue)hq;
    ScoreDoc[] scoreDocs = new ScoreDoc[fshq.size()];
    for (int i = fshq.size()-1; i >= 0; i--)      // put docs in array
      scoreDocs[i] = fshq.fillFields ((FieldDoc) fshq.pop());

    return new TopFieldDocs(totalHits, scoreDocs,
                            fshq.getFields(), fshq.getMaxScore());
  }
}
