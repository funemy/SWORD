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
import java.util.LinkedList;

import org.apache.lucene.analysis.NumericTokenStream; // for javadocs
import org.apache.lucene.document.NumericField; // for javadocs
import org.apache.lucene.util.NumericUtils;
import org.apache.lucene.util.ToStringUtils;
import org.apache.lucene.util.StringHelper;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;

/**
 * <p>A {@link Query} that matches numeric values within a
 * specified range.  To use this, you must first index the
 * numeric values using {@link NumericField} (expert: {@link
 * NumericTokenStream}).  If your terms are instead textual,
 * you should use {@link TermRangeQuery}.  {@link
 * NumericRangeFilter} is the filter equivalent of this
 * query.</p>
 *
 * <p>You create a new NumericRangeQuery with the static
 * factory methods, eg:
 *
 * <pre>
 * Query q = NumericRangeQuery.newFloatRange("weight",
 *                                           new Float(0.3f), new Float(0.10f),
 *                                           true, true);
 * </pre>
 *
 * matches all documents whose float valued "weight" field
 * ranges from 0.3 to 0.10, inclusive.
 *
 * <p>The performance of NumericRangeQuery is much better
 * than the corresponding {@link TermRangeQuery} because the
 * number of terms that must be searched is usually far
 * fewer, thanks to trie indexing, described below.</p>
 *
 * <p>You can optionally specify a <a
 * href="#precisionStepDesc"><code>precisionStep</code></a>
 * when creating this query.  This is necessary if you've
 * changed this configuration from its default (4) during
 * indexing.  Lower values consume more disk space but speed
 * up searching.  Suitable values are between <b>1</b> and
 * <b>8</b>. A good starting point to test is <b>4</b>,
 * which is the default value for all <code>Numeric*</code>
 * classes.  See <a href="#precisionStepDesc">below</a> for
 * details.
 *
 * <p>This query defaults to {@linkplain
 * MultiTermQuery#CONSTANT_SCORE_AUTO_REWRITE_DEFAULT} for
 * 32 bit (int/float) ranges with precisionStep &le;8 and 64
 * bit (long/double) ranges with precisionStep &le;6.
 * Otherwise it uses {@linkplain
 * MultiTermQuery#CONSTANT_SCORE_FILTER_REWRITE} as the
 * number of terms is likely to be high.  With precision
 * steps of &le;4, this query can be run with one of the
 * BooleanQuery rewrite methods without changing
 * BooleanQuery's default max clause count.
 *
 * <p><font color="red"><b>NOTE:</b> This API is experimental and
 * might change in incompatible ways in the next release.</font>
 *
 * <br><h3>How it works</h3>
 *
 * <p>See the publication about <a target="_blank" href="http://www.panfmp.org">panFMP</a>,
 * where this algorithm was described (referred to as <code>TrieRangeQuery</code>):
 *
 * <blockquote><strong>Schindler, U, Diepenbroek, M</strong>, 2008.
 * <em>Generic XML-based Framework for Metadata Portals.</em>
 * Computers &amp; Geosciences 34 (12), 1947-1955.
 * <a href="http://dx.doi.org/10.1016/j.cageo.2008.02.023"
 * target="_blank">doi:10.1016/j.cageo.2008.02.023</a></blockquote>
 *
 * <p><em>A quote from this paper:</em> Because Apache Lucene is a full-text
 * search engine and not a conventional database, it cannot handle numerical ranges
 * (e.g., field value is inside user defined bounds, even dates are numerical values).
 * We have developed an extension to Apache Lucene that stores
 * the numerical values in a special string-encoded format with variable precision
 * (all numerical values like doubles, longs, floats, and ints are converted to
 * lexicographic sortable string representations and stored with different precisions
 * (for a more detailed description of how the values are stored,
 * see {@link NumericUtils}). A range is then divided recursively into multiple intervals for searching:
 * The center of the range is searched only with the lowest possible precision in the <em>trie</em>,
 * while the boundaries are matched more exactly. This reduces the number of terms dramatically.</p>
 *
 * <p>For the variant that stores long values in 8 different precisions (each reduced by 8 bits) that
 * uses a lowest precision of 1 byte, the index contains only a maximum of 256 distinct values in the
 * lowest precision. Overall, a range could consist of a theoretical maximum of
 * <code>7*255*2 + 255 = 3825</code> distinct terms (when there is a term for every distinct value of an
 * 8-byte-number in the index and the range covers almost all of them; a maximum of 255 distinct values is used
 * because it would always be possible to reduce the full 256 values to one term with degraded precision).
 * In practice, we have seen up to 300 terms in most cases (index with 500,000 metadata records
 * and a uniform value distribution).</p>
 *
 * <a name="precisionStepDesc"><h3>Precision Step</h3>
 * <p>You can choose any <code>precisionStep</code> when encoding values.
 * Lower step values mean more precisions and so more terms in index (and index gets larger).
 * On the other hand, the maximum number of terms to match reduces, which optimized query speed.
 * The formula to calculate the maximum term count is:
 * <pre>
 *  n = [ (bitsPerValue/precisionStep - 1) * (2^precisionStep - 1 ) * 2 ] + (2^precisionStep - 1 )
 * </pre>
 * <p><em>(this formula is only correct, when <code>bitsPerValue/precisionStep</code> is an integer;
 * in other cases, the value must be rounded up and the last summand must contain the modulo of the division as
 * precision step)</em>.
 * For longs stored using a precision step of 4, <code>n = 15*15*2 + 15 = 465</code>, and for a precision
 * step of 2, <code>n = 31*3*2 + 3 = 189</code>. But the faster search speed is reduced by more seeking
 * in the term enum of the index. Because of this, the ideal <code>precisionStep</code> value can only
 * be found out by testing. <b>Important:</b> You can index with a lower precision step value and test search speed
 * using a multiple of the original step value.</p>
 *
 * <p>Good values for <code>precisionStep</code> are depending on usage and data type:
 * <ul>
 *  <li>The default for all data types is <b>4</b>, which is used, when no <code>precisionStep</code> is given.
 *  <li>Ideal value in most cases for <em>64 bit</em> data types <em>(long, double)</em> is <b>6</b> or <b>8</b>.
 *  <li>Ideal value in most cases for <em>32 bit</em> data types <em>(int, float)</em> is <b>4</b>.
 *  <li>For low cardinality fields larger precision steps are good. If the cardinality is &lt; 100, it is
 *  fair to use {@link Integer#MAX_VALUE} (see below).
 *  <li>Steps <b>&ge;64</b> for <em>long/double</em> and <b>&ge;32</b> for <em>int/float</em> produces one token
 *  per value in the index and querying is as slow as a conventional {@link TermRangeQuery}. But it can be used
 *  to produce fields, that are solely used for sorting (in this case simply use {@link Integer#MAX_VALUE} as
 *  <code>precisionStep</code>). Using {@link NumericField NumericFields} for sorting
 *  is ideal, because building the field cache is much faster than with text-only numbers.
 *  These fields have one term per value and therefore also work with term enumeration for building distinct lists
 *  (e.g. facets / preselected values to search for).
 *  Sorting is also possible with range query optimized fields using one of the above <code>precisionSteps</code>.
 * </ul>
 *
 * <p>Comparisons of the different types of RangeQueries on an index with about 500,000 docs showed
 * that {@link TermRangeQuery} in boolean rewrite mode (with raised {@link BooleanQuery} clause count)
 * took about 30-40 secs to complete, {@link TermRangeQuery} in constant score filter rewrite mode took 5 secs
 * and executing this class took &lt;100ms to complete (on an Opteron64 machine, Java 1.5, 8 bit
 * precision step). This query type was developed for a geographic portal, where the performance for
 * e.g. bounding boxes or exact date/time stamps is important.</p>
 *
 * @since 2.9
 **/
public final class NumericRangeQuery extends MultiTermQuery {

  private NumericRangeQuery(final String field, final int precisionStep, final int valSize,
    Number min, Number max, final boolean minInclusive, final boolean maxInclusive
  ) {
    assert (valSize == 32 || valSize == 64);
    if (precisionStep < 1)
      throw new IllegalArgumentException("precisionStep must be >=1");
    this.field = StringHelper.intern(field);
    this.precisionStep = precisionStep;
    this.valSize = valSize;
    this.min = min;
    this.max = max;
    this.minInclusive = minInclusive;
    this.maxInclusive = maxInclusive;

    // For bigger precisionSteps this query likely
    // hits too many terms, so set to CONSTANT_SCORE_FILTER right off
    // (especially as the FilteredTermEnum is costly if wasted only for AUTO tests because it
    // creates new enums from IndexReader for each sub-range)
    switch (valSize) {
      case 64:
        setRewriteMethod( (precisionStep > 6) ?
          CONSTANT_SCORE_FILTER_REWRITE : 
          CONSTANT_SCORE_AUTO_REWRITE_DEFAULT
        );
        break;
      case 32:
        setRewriteMethod( (precisionStep > 8) ?
          CONSTANT_SCORE_FILTER_REWRITE : 
          CONSTANT_SCORE_AUTO_REWRITE_DEFAULT
        );
        break;
      default:
        // should never happen
        throw new IllegalArgumentException("valSize must be 32 or 64");
    }
    
    // shortcut if upper bound == lower bound
    if (min != null && min.equals(max)) {
      setRewriteMethod(CONSTANT_SCORE_BOOLEAN_QUERY_REWRITE);
    }
  }
  
  /**
   * Factory that creates a <code>NumericRangeQuery</code>, that queries a <code>long</code>
   * range using the given <a href="#precisionStepDesc"><code>precisionStep</code></a>.
   * You can have half-open ranges (which are in fact &lt;/&le; or &gt;/&ge; queries)
   * by setting the min or max value to <code>null</code>. By setting inclusive to false, it will
   * match all documents excluding the bounds, with inclusive on, the boundaries are hits, too.
   */
  public static NumericRangeQuery newLongRange(final String field, final int precisionStep,
    Long min, Long max, final boolean minInclusive, final boolean maxInclusive
  ) {
    return new NumericRangeQuery(field, precisionStep, 64, min, max, minInclusive, maxInclusive);
  }
  
  /**
   * Factory that creates a <code>NumericRangeQuery</code>, that queries a <code>long</code>
   * range using the default <code>precisionStep</code> {@link NumericUtils#PRECISION_STEP_DEFAULT} (4).
   * You can have half-open ranges (which are in fact &lt;/&le; or &gt;/&ge; queries)
   * by setting the min or max value to <code>null</code>. By setting inclusive to false, it will
   * match all documents excluding the bounds, with inclusive on, the boundaries are hits, too.
   */
  public static NumericRangeQuery newLongRange(final String field,
    Long min, Long max, final boolean minInclusive, final boolean maxInclusive
  ) {
    return new NumericRangeQuery(field, NumericUtils.PRECISION_STEP_DEFAULT, 64, min, max, minInclusive, maxInclusive);
  }
  
  /**
   * Factory that creates a <code>NumericRangeQuery</code>, that queries a <code>int</code>
   * range using the given <a href="#precisionStepDesc"><code>precisionStep</code></a>.
   * You can have half-open ranges (which are in fact &lt;/&le; or &gt;/&ge; queries)
   * by setting the min or max value to <code>null</code>. By setting inclusive to false, it will
   * match all documents excluding the bounds, with inclusive on, the boundaries are hits, too.
   */
  public static NumericRangeQuery newIntRange(final String field, final int precisionStep,
    Integer min, Integer max, final boolean minInclusive, final boolean maxInclusive
  ) {
    return new NumericRangeQuery(field, precisionStep, 32, min, max, minInclusive, maxInclusive);
  }
  
  /**
   * Factory that creates a <code>NumericRangeQuery</code>, that queries a <code>int</code>
   * range using the default <code>precisionStep</code> {@link NumericUtils#PRECISION_STEP_DEFAULT} (4).
   * You can have half-open ranges (which are in fact &lt;/&le; or &gt;/&ge; queries)
   * by setting the min or max value to <code>null</code>. By setting inclusive to false, it will
   * match all documents excluding the bounds, with inclusive on, the boundaries are hits, too.
   */
  public static NumericRangeQuery newIntRange(final String field,
    Integer min, Integer max, final boolean minInclusive, final boolean maxInclusive
  ) {
    return new NumericRangeQuery(field, NumericUtils.PRECISION_STEP_DEFAULT, 32, min, max, minInclusive, maxInclusive);
  }
  
  /**
   * Factory that creates a <code>NumericRangeQuery</code>, that queries a <code>double</code>
   * range using the given <a href="#precisionStepDesc"><code>precisionStep</code></a>.
   * You can have half-open ranges (which are in fact &lt;/&le; or &gt;/&ge; queries)
   * by setting the min or max value to <code>null</code>. By setting inclusive to false, it will
   * match all documents excluding the bounds, with inclusive on, the boundaries are hits, too.
   */
  public static NumericRangeQuery newDoubleRange(final String field, final int precisionStep,
    Double min, Double max, final boolean minInclusive, final boolean maxInclusive
  ) {
    return new NumericRangeQuery(field, precisionStep, 64, min, max, minInclusive, maxInclusive);
  }
  
  /**
   * Factory that creates a <code>NumericRangeQuery</code>, that queries a <code>double</code>
   * range using the default <code>precisionStep</code> {@link NumericUtils#PRECISION_STEP_DEFAULT} (4).
   * You can have half-open ranges (which are in fact &lt;/&le; or &gt;/&ge; queries)
   * by setting the min or max value to <code>null</code>. By setting inclusive to false, it will
   * match all documents excluding the bounds, with inclusive on, the boundaries are hits, too.
   */
  public static NumericRangeQuery newDoubleRange(final String field,
    Double min, Double max, final boolean minInclusive, final boolean maxInclusive
  ) {
    return new NumericRangeQuery(field, NumericUtils.PRECISION_STEP_DEFAULT, 64, min, max, minInclusive, maxInclusive);
  }
  
  /**
   * Factory that creates a <code>NumericRangeQuery</code>, that queries a <code>float</code>
   * range using the given <a href="#precisionStepDesc"><code>precisionStep</code></a>.
   * You can have half-open ranges (which are in fact &lt;/&le; or &gt;/&ge; queries)
   * by setting the min or max value to <code>null</code>. By setting inclusive to false, it will
   * match all documents excluding the bounds, with inclusive on, the boundaries are hits, too.
   */
  public static NumericRangeQuery newFloatRange(final String field, final int precisionStep,
    Float min, Float max, final boolean minInclusive, final boolean maxInclusive
  ) {
    return new NumericRangeQuery(field, precisionStep, 32, min, max, minInclusive, maxInclusive);
  }
  
  /**
   * Factory that creates a <code>NumericRangeQuery</code>, that queries a <code>float</code>
   * range using the default <code>precisionStep</code> {@link NumericUtils#PRECISION_STEP_DEFAULT} (4).
   * You can have half-open ranges (which are in fact &lt;/&le; or &gt;/&ge; queries)
   * by setting the min or max value to <code>null</code>. By setting inclusive to false, it will
   * match all documents excluding the bounds, with inclusive on, the boundaries are hits, too.
   */
  public static NumericRangeQuery newFloatRange(final String field,
    Float min, Float max, final boolean minInclusive, final boolean maxInclusive
  ) {
    return new NumericRangeQuery(field, NumericUtils.PRECISION_STEP_DEFAULT, 32, min, max, minInclusive, maxInclusive);
  }
  
  //@Override
  protected FilteredTermEnum getEnum(final IndexReader reader) throws IOException {
    return new NumericRangeTermEnum(reader);
  }

  /** Returns the field name for this query */
  public String getField() { return field; }

  /** Returns <code>true</code> if the lower endpoint is inclusive */
  public boolean includesMin() { return minInclusive; }
  
  /** Returns <code>true</code> if the upper endpoint is inclusive */
  public boolean includesMax() { return maxInclusive; }

  /** Returns the lower value of this range query */
  public Number getMin() { return min; }

  /** Returns the upper value of this range query */
  public Number getMax() { return max; }
  
  //@Override
  public String toString(final String field) {
    final StringBuffer sb = new StringBuffer();
    if (!this.field.equals(field)) sb.append(this.field).append(':');
    return sb.append(minInclusive ? '[' : '{')
      .append((min == null) ? "*" : min.toString())
      .append(" TO ")
      .append((max == null) ? "*" : max.toString())
      .append(maxInclusive ? ']' : '}')
      .append(ToStringUtils.boost(getBoost()))
      .toString();
  }

  //@Override
  public final boolean equals(final Object o) {
    if (o==this) return true;
    if (!super.equals(o))
      return false;
    if (o instanceof NumericRangeQuery) {
      final NumericRangeQuery q=(NumericRangeQuery)o;
      return (
        field==q.field &&
        (q.min == null ? min == null : q.min.equals(min)) &&
        (q.max == null ? max == null : q.max.equals(max)) &&
        minInclusive == q.minInclusive &&
        maxInclusive == q.maxInclusive &&
        precisionStep == q.precisionStep
      );
    }
    return false;
  }

  //@Override
  public final int hashCode() {
    int hash = super.hashCode();
    hash += field.hashCode()^0x4565fd66 + precisionStep^0x64365465;
    if (min != null) hash += min.hashCode()^0x14fa55fb;
    if (max != null) hash += max.hashCode()^0x733fa5fe;
    return hash +
      (Boolean.valueOf(minInclusive).hashCode()^0x14fa55fb)+
      (Boolean.valueOf(maxInclusive).hashCode()^0x733fa5fe);
  }
  
  // field must be interned after reading from stream
  private void readObject(java.io.ObjectInputStream in) throws java.io.IOException, ClassNotFoundException {
    in.defaultReadObject();
    field = StringHelper.intern(field);
  }

  // members (package private, to be also fast accessible by NumericRangeTermEnum)
  String field;
  final int precisionStep, valSize;
  final Number min, max;
  final boolean minInclusive,maxInclusive;

  /**
   * Subclass of FilteredTermEnum for enumerating all terms that match the
   * sub-ranges for trie range queries.
   * <p>
   * WARNING: This term enumeration is not guaranteed to be always ordered by
   * {@link Term#compareTo}.
   * The ordering depends on how {@link NumericUtils#splitLongRange} and
   * {@link NumericUtils#splitIntRange} generates the sub-ranges. For
   * {@link MultiTermQuery} ordering is not relevant.
   */
  private final class NumericRangeTermEnum extends FilteredTermEnum {

    private final IndexReader reader;
    private final LinkedList/*<String>*/ rangeBounds = new LinkedList/*<String>*/();
    private String currentUpperBound = null;

    NumericRangeTermEnum(final IndexReader reader) throws IOException {
      this.reader = reader;
      
      switch (valSize) {
        case 64: {
          // lower
          long minBound = Long.MIN_VALUE;
          if (min instanceof Long) {
            minBound = min.longValue();
          } else if (min instanceof Double) {
            minBound = NumericUtils.doubleToSortableLong(min.doubleValue());
          }
          if (!minInclusive && min != null) {
            if (minBound == Long.MAX_VALUE) break;
            minBound++;
          }
          
          // upper
          long maxBound = Long.MAX_VALUE;
          if (max instanceof Long) {
            maxBound = max.longValue();
          } else if (max instanceof Double) {
            maxBound = NumericUtils.doubleToSortableLong(max.doubleValue());
          }
          if (!maxInclusive && max != null) {
            if (maxBound == Long.MIN_VALUE) break;
            maxBound--;
          }
          
          NumericUtils.splitLongRange(new NumericUtils.LongRangeBuilder() {
            //@Override
            public final void addRange(String minPrefixCoded, String maxPrefixCoded) {
              rangeBounds.add(minPrefixCoded);
              rangeBounds.add(maxPrefixCoded);
            }
          }, precisionStep, minBound, maxBound);
          break;
        }
          
        case 32: {
          // lower
          int minBound = Integer.MIN_VALUE;
          if (min instanceof Integer) {
            minBound = min.intValue();
          } else if (min instanceof Float) {
            minBound = NumericUtils.floatToSortableInt(min.floatValue());
          }
          if (!minInclusive && min != null) {
            if (minBound == Integer.MAX_VALUE) break;
            minBound++;
          }
          
          // upper
          int maxBound = Integer.MAX_VALUE;
          if (max instanceof Integer) {
            maxBound = max.intValue();
          } else if (max instanceof Float) {
            maxBound = NumericUtils.floatToSortableInt(max.floatValue());
          }
          if (!maxInclusive && max != null) {
            if (maxBound == Integer.MIN_VALUE) break;
            maxBound--;
          }
          
          NumericUtils.splitIntRange(new NumericUtils.IntRangeBuilder() {
            //@Override
            public final void addRange(String minPrefixCoded, String maxPrefixCoded) {
              rangeBounds.add(minPrefixCoded);
              rangeBounds.add(maxPrefixCoded);
            }
          }, precisionStep, minBound, maxBound);
          break;
        }
          
        default:
          // should never happen
          throw new IllegalArgumentException("valSize must be 32 or 64");
      }
      
      // seek to first term
      next();
    }

    //@Override
    public float difference() {
      return 1.0f;
    }
    
    /** this is a dummy, it is not used by this class. */
    //@Override
    protected boolean endEnum() {
      assert false; // should never be called
      return (currentTerm != null);
    }

    /**
     * Compares if current upper bound is reached,
     * this also updates the term count for statistics.
     * In contrast to {@link FilteredTermEnum}, a return value
     * of <code>false</code> ends iterating the current enum
     * and forwards to the next sub-range.
     */
    //@Override
    protected boolean termCompare(Term term) {
      return (term.field() == field && term.text().compareTo(currentUpperBound) <= 0);
    }
    
    /** Increments the enumeration to the next element.  True if one exists. */
    //@Override
    public boolean next() throws IOException {
      // if a current term exists, the actual enum is initialized:
      // try change to next term, if no such term exists, fall-through
      if (currentTerm != null) {
        assert actualEnum!=null;
        if (actualEnum.next()) {
          currentTerm = actualEnum.term();
          if (termCompare(currentTerm)) return true;
        }
      }
      // if all above fails, we go forward to the next enum,
      // if one is available
      currentTerm = null;
      if (rangeBounds.size() < 2) return false;
      // close the current enum and read next bounds
      if (actualEnum != null) {
        actualEnum.close();
        actualEnum = null;
      }
      final String lowerBound = (String)rangeBounds.removeFirst();
      this.currentUpperBound = (String)rangeBounds.removeFirst();
      // this call recursively uses next(), if no valid term in
      // next enum found.
      // if this behavior is changed/modified in the superclass,
      // this enum will not work anymore!
      setEnum(reader.terms(new Term(field, lowerBound)));
      return (currentTerm != null);
    }

    /** Closes the enumeration to further activity, freeing resources.  */
    //@Override
    public void close() throws IOException {
      rangeBounds.clear();
      currentUpperBound = null;
      super.close();
    }

  }
  
}
