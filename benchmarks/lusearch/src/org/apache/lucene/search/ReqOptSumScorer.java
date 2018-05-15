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

/** A Scorer for queries with a required part and an optional part.
 * Delays skipTo() on the optional part until a score() is needed.
 * <br>
 * This <code>Scorer</code> implements {@link Scorer#skipTo(int)}.
 */
class ReqOptSumScorer extends Scorer {
  /** The scorers passed from the constructor.
   * These are set to null as soon as their next() or skipTo() returns false.
   */
  private Scorer reqScorer;
  private Scorer optScorer;

  /** Construct a <code>ReqOptScorer</code>.
   * @param reqScorer The required scorer. This must match.
   * @param optScorer The optional scorer. This is used for scoring only.
   */
  public ReqOptSumScorer(
      Scorer reqScorer,
      Scorer optScorer)
  {
    super(null); // No similarity used.
    this.reqScorer = reqScorer;
    this.optScorer = optScorer;
  }

  /** @deprecated use {@link #nextDoc()} instead. */
  public boolean next() throws IOException {
    return reqScorer.next();
  }

  public int nextDoc() throws IOException {
    return reqScorer.nextDoc();
  }
  
  /** @deprecated use {@link #advance(int)} instead. */
  public boolean skipTo(int target) throws IOException {
    return reqScorer.skipTo(target);
  }

  public int advance(int target) throws IOException {
    return reqScorer.advance(target);
  }
  
  /** @deprecated use {@link #docID()} instead. */
  public int doc() {
    return reqScorer.doc();
  }

  public int docID() {
    return reqScorer.docID();
  }
  
  /** Returns the score of the current document matching the query.
   * Initially invalid, until {@link #next()} is called the first time.
   * @return The score of the required scorer, eventually increased by the score
   * of the optional scorer when it also matches the current document.
   */
  public float score() throws IOException {
    int curDoc = reqScorer.docID();
    float reqScore = reqScorer.score();
    if (optScorer == null) {
      return reqScore;
    }
    
    int optScorerDoc = optScorer.docID();
    if (optScorerDoc < curDoc && (optScorerDoc = optScorer.advance(curDoc)) == NO_MORE_DOCS) {
      optScorer = null;
      return reqScore;
    }
    
    return optScorerDoc == curDoc ? reqScore + optScorer.score() : reqScore;
  }

  /** Explain the score of a document.
   * TODO: Also show the total score.
   * See BooleanScorer.explain() on how to do this.
   */
  public Explanation explain(int doc) throws IOException {
    Explanation res = new Explanation();
    res.setDescription("required, optional");
    res.addDetail(reqScorer.explain(doc));
    res.addDetail(optScorer.explain(doc));
    return res;
  }
}

