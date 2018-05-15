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

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.TermDocs;
import org.apache.lucene.util.ToStringUtils;

import java.util.Set;
import java.io.IOException;

/**
 * A query that matches all documents.
 *
 */
public class MatchAllDocsQuery extends Query {

  public MatchAllDocsQuery() {
    this(null);
  }

  private final String normsField;

  /**
   * @param normsField Field used for normalization factor (document boost). Null if nothing.
   */
  public MatchAllDocsQuery(String normsField) {
    this.normsField = normsField;
  }

  private class MatchAllScorer extends Scorer {
    final TermDocs termDocs;
    final float score;
    final byte[] norms;
    private int doc = -1;
    
    MatchAllScorer(IndexReader reader, Similarity similarity, Weight w,
        byte[] norms) throws IOException {
      super(similarity);
      this.termDocs = reader.termDocs(null);
      score = w.getValue();
      this.norms = norms;
    }

    public Explanation explain(int doc) {
      return null; // not called... see MatchAllDocsWeight.explain()
    }

    /** @deprecated use {@link #docID()} instead. */
    public int doc() {
      return termDocs.doc();
    }
    
    public int docID() {
      return doc;
    }

    /** @deprecated use {@link #nextDoc()} instead. */
    public boolean next() throws IOException {
      return nextDoc() != NO_MORE_DOCS;
    }

    public int nextDoc() throws IOException {
      return doc = termDocs.next() ? termDocs.doc() : NO_MORE_DOCS;
    }
    
    public float score() {
      return norms == null ? score : score * Similarity.decodeNorm(norms[docID()]);
    }

    /** @deprecated use {@link #advance(int)} instead. */
    public boolean skipTo(int target) throws IOException {
      return advance(target) != NO_MORE_DOCS;
    }

    public int advance(int target) throws IOException {
      return doc = termDocs.skipTo(target) ? termDocs.doc() : NO_MORE_DOCS;
    }
  }

  private class MatchAllDocsWeight extends Weight {
    private Similarity similarity;
    private float queryWeight;
    private float queryNorm;

    public MatchAllDocsWeight(Searcher searcher) {
      this.similarity = searcher.getSimilarity();
    }

    public String toString() {
      return "weight(" + MatchAllDocsQuery.this + ")";
    }

    public Query getQuery() {
      return MatchAllDocsQuery.this;
    }

    public float getValue() {
      return queryWeight;
    }

    public float sumOfSquaredWeights() {
      queryWeight = getBoost();
      return queryWeight * queryWeight;
    }

    public void normalize(float queryNorm) {
      this.queryNorm = queryNorm;
      queryWeight *= this.queryNorm;
    }

    public Scorer scorer(IndexReader reader, boolean scoreDocsInOrder, boolean topScorer) throws IOException {
      return new MatchAllScorer(reader, similarity, this,
          normsField != null ? reader.norms(normsField) : null);
    }

    public Explanation explain(IndexReader reader, int doc) {
      // explain query weight
      Explanation queryExpl = new ComplexExplanation
        (true, getValue(), "MatchAllDocsQuery, product of:");
      if (getBoost() != 1.0f) {
        queryExpl.addDetail(new Explanation(getBoost(),"boost"));
      }
      queryExpl.addDetail(new Explanation(queryNorm,"queryNorm"));

      return queryExpl;
    }
  }

  public Weight createWeight(Searcher searcher) {
    return new MatchAllDocsWeight(searcher);
  }

  public void extractTerms(Set terms) {
  }

  public String toString(String field) {
    StringBuffer buffer = new StringBuffer();
    buffer.append("*:*");
    buffer.append(ToStringUtils.boost(getBoost()));
    return buffer.toString();
  }

  public boolean equals(Object o) {
    if (!(o instanceof MatchAllDocsQuery))
      return false;
    MatchAllDocsQuery other = (MatchAllDocsQuery) o;
    return this.getBoost() == other.getBoost();
  }

  public int hashCode() {
    return Float.floatToIntBits(getBoost()) ^ 0x1AA71190;
  }
}
