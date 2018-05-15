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
import org.apache.lucene.util.OpenBitSet;
import org.apache.lucene.index.TermDocs;  // for javadocs

/**
 * A {@link Filter} that only accepts documents whose single
 * term value in the specified field is contained in the
 * provided set of allowed terms.
 * 
 * <p/>
 * 
 * This is the same functionality as TermsFilter (from
 * contrib/queries), except this filter requires that the
 * field contains only a single term for all documents.
 * Because of drastically different implementations, they
 * also have different performance characteristics, as
 * described below.
 * 
 * <p/>
 * 
 * The first invocation of this filter on a given field will
 * be slower, since a {@link FieldCache.StringIndex} must be
 * created.  Subsequent invocations using the same field
 * will re-use this cache.  However, as with all
 * functionality based on {@link FieldCache}, persistent RAM
 * is consumed to hold the cache, and is not freed until the
 * {@link IndexReader} is closed.  In contrast, TermsFilter
 * has no persistent RAM consumption.
 * 
 * 
 * <p/>
 * 
 * With each search, this filter translates the specified
 * set of Terms into a private {@link OpenBitSet} keyed by
 * term number per unique {@link IndexReader} (normally one
 * reader per segment).  Then, during matching, the term
 * number for each docID is retrieved from the cache and
 * then checked for inclusion using the {@link OpenBitSet}.
 * Since all testing is done using RAM resident data
 * structures, performance should be very fast, most likely
 * fast enough to not require further caching of the
 * DocIdSet for each possible combination of terms.
 * However, because docIDs are simply scanned linearly, an
 * index with a great many small documents may find this
 * linear scan too costly.
 * 
 * <p/>
 * 
 * In contrast, TermsFilter builds up an {@link OpenBitSet},
 * keyed by docID, every time it's created, by enumerating
 * through all matching docs using {@link TermDocs} to seek
 * and scan through each term's docID list.  While there is
 * no linear scan of all docIDs, besides the allocation of
 * the underlying array in the {@link OpenBitSet}, this
 * approach requires a number of "disk seeks" in proportion
 * to the number of terms, which can be exceptionally costly
 * when there are cache misses in the OS's IO cache.
 * 
 * <p/>
 * 
 * Generally, this filter will be slower on the first
 * invocation for a given field, but subsequent invocations,
 * even if you change the allowed set of Terms, should be
 * faster than TermsFilter, especially as the number of
 * Terms being matched increases.  If you are matching only
 * a very small number of terms, and those terms in turn
 * match a very small number of documents, TermsFilter may
 * perform faster.
 *
 * <p/>
 *
 * Which filter is best is very application dependent.
 */

public class FieldCacheTermsFilter extends Filter {
  private String field;
  private String[] terms;

  public FieldCacheTermsFilter(String field, String[] terms) {
    this.field = field;
    this.terms = terms;
  }

  public FieldCache getFieldCache() {
    return FieldCache.DEFAULT;
  }

  public DocIdSet getDocIdSet(IndexReader reader) throws IOException {
    return new FieldCacheTermsFilterDocIdSet(getFieldCache().getStringIndex(reader, field));
  }

  protected class FieldCacheTermsFilterDocIdSet extends DocIdSet {
    private FieldCache.StringIndex fcsi;

    private OpenBitSet openBitSet;

    public FieldCacheTermsFilterDocIdSet(FieldCache.StringIndex fcsi) {
      this.fcsi = fcsi;
      openBitSet = new OpenBitSet(this.fcsi.lookup.length);
      for (int i=0;i<terms.length;i++) {
        int termNumber = this.fcsi.binarySearchLookup(terms[i]);
        if (termNumber > 0) {
          openBitSet.fastSet(termNumber);
        }
      }
    }

    public DocIdSetIterator iterator() {
      return new FieldCacheTermsFilterDocIdSetIterator();
    }

    /** This DocIdSet implementation is cacheable. */
    public boolean isCacheable() {
      return true;
    }

    protected class FieldCacheTermsFilterDocIdSetIterator extends DocIdSetIterator {
      private int doc = -1;

      /** @deprecated use {@link #docID()} instead. */
      public int doc() {
        return doc;
      }
      
      public int docID() {
        return doc;
      }

      /** @deprecated use {@link #nextDoc()} instead. */
      public boolean next() {
        return nextDoc() != NO_MORE_DOCS;
      }
      
      public int nextDoc() {
        try {
          while (!openBitSet.fastGet(fcsi.order[++doc])) {}
        } catch (ArrayIndexOutOfBoundsException e) {
          doc = NO_MORE_DOCS;
        }
        return doc;
      }

      /** @deprecated use {@link #advance(int)} instead. */
      public boolean skipTo(int target) {
        return advance(target) != NO_MORE_DOCS;
      }
      
      public int advance(int target) {
        try {
          doc = target;
          while (!openBitSet.fastGet(fcsi.order[doc])) {
            doc++;
          }
        } catch (ArrayIndexOutOfBoundsException e) {
          doc = NO_MORE_DOCS;
        }
        return doc;
      }
    }
  }
}
