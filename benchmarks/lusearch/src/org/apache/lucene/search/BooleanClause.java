package org.apache.lucene.search;

import org.apache.lucene.util.Parameter;

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

/** A clause in a BooleanQuery. */
public class BooleanClause implements java.io.Serializable {
  
  /** Specifies how clauses are to occur in matching documents. */
  public static final class Occur extends Parameter implements java.io.Serializable {
    
    private Occur(String name) {
      // typesafe enum pattern, no public constructor
      super(name);
    }

    public String toString() {
      if (this == MUST) return "+";
      if (this == MUST_NOT) return "-";
      return "";
    }

    /** Use this operator for clauses that <i>must</i> appear in the matching documents. */
    public static final Occur MUST = new Occur("MUST");
    /** Use this operator for clauses that <i>should</i> appear in the 
     * matching documents. For a BooleanQuery with no <code>MUST</code> 
     * clauses one or more <code>SHOULD</code> clauses must match a document 
     * for the BooleanQuery to match.
     * @see BooleanQuery#setMinimumNumberShouldMatch
     */
    public static final Occur SHOULD = new Occur("SHOULD");
    /** Use this operator for clauses that <i>must not</i> appear in the matching documents.
     * Note that it is not possible to search for queries that only consist
     * of a <code>MUST_NOT</code> clause. */
    public static final Occur MUST_NOT = new Occur("MUST_NOT");
    
  }

  /** The query whose matching documents are combined by the boolean query.
   */
  private Query query;

  private Occur occur;


  /** Constructs a BooleanClause.
  */ 
  public BooleanClause(Query query, Occur occur) {
    this.query = query;
    this.occur = occur;
    
  }

  public Occur getOccur() {
    return occur;
  }

  public void setOccur(Occur occur) {
    this.occur = occur;

  }

  public Query getQuery() {
    return query;
  }

  public void setQuery(Query query) {
    this.query = query;
  }
  
  public boolean isProhibited() {
    return Occur.MUST_NOT.equals(occur);
  }

  public boolean isRequired() {
    return Occur.MUST.equals(occur);
  }



  /** Returns true if <code>o</code> is equal to this. */
  public boolean equals(Object o) {
    if (o == null || !(o instanceof BooleanClause))
      return false;
    BooleanClause other = (BooleanClause)o;
    return this.query.equals(other.query)
      && this.occur.equals(other.occur);
  }

  /** Returns a hash code value for this object.*/
  public int hashCode() {
    return query.hashCode() ^ (Occur.MUST.equals(occur)?1:0) ^ (Occur.MUST_NOT.equals(occur)?2:0);
  }


  public String toString() {
    return occur.toString() + query.toString();
  }
}
