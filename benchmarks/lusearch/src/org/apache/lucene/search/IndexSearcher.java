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
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.ReaderUtil;

/** Implements search over a single IndexReader.
 *
 * <p>Applications usually need only call the inherited
 * {@link #search(Query,int)}
 * or {@link #search(Query,Filter,int)} methods. For performance reasons it is 
 * recommended to open only one IndexSearcher and use it for all of your searches.
 * 
 * <p>Note that you can only access the deprecated {@link Hits} from an IndexSearcher as long as it is
 * not yet closed, otherwise an IOException will be thrown. 
 *
 * <a name="thread-safety"></a><p><b>NOTE</b>: {@link
 * <code>IndexSearcher</code>} instances are completely
 * thread safe, meaning multiple threads can call any of its
 * methods, concurrently.  If your application requires
 * external synchronization, you should <b>not</b>
 * synchronize on the <code>IndexSearcher</code> instance;
 * use your own (non-Lucene) objects instead.</p>
 */
public class IndexSearcher extends Searcher {
  IndexReader reader;
  private boolean closeReader;
  private IndexReader[] subReaders;
  private int[] docStarts;

  /** Creates a searcher searching the index in the named directory.
   * @throws CorruptIndexException if the index is corrupt
   * @throws IOException if there is a low-level IO error
   * @deprecated Use {@link #IndexSearcher(Directory, boolean)} instead
   */
  public IndexSearcher(String path) throws CorruptIndexException, IOException {
    this(IndexReader.open(path), true);
  }

  /** Creates a searcher searching the index in the named
   *  directory.  You should pass readOnly=true, since it
   *  gives much better concurrent performance, unless you
   *  intend to do write operations (delete documents or
   *  change norms) with the underlying IndexReader.
   * @param path directory where IndexReader will be opened
   * @param readOnly if true, the underlying IndexReader
   * will be opened readOnly
   * @throws CorruptIndexException if the index is corrupt
   * @throws IOException if there is a low-level IO error
   * @deprecated Use {@link #IndexSearcher(Directory, boolean)} instead
   */
  public IndexSearcher(String path, boolean readOnly) throws CorruptIndexException, IOException {
    this(IndexReader.open(path, readOnly), true);
  }

  /** Creates a searcher searching the index in the provided directory.
   * @throws CorruptIndexException if the index is corrupt
   * @throws IOException if there is a low-level IO error
   * @deprecated Use {@link #IndexSearcher(Directory, boolean)} instead
   */
  public IndexSearcher(Directory directory) throws CorruptIndexException, IOException {
    this(IndexReader.open(directory), true);
  }

  /** Creates a searcher searching the index in the named
   *  directory.  You should pass readOnly=true, since it
   *  gives much better concurrent performance, unless you
   *  intend to do write operations (delete documents or
   *  change norms) with the underlying IndexReader.
   * @throws CorruptIndexException if the index is corrupt
   * @throws IOException if there is a low-level IO error
   * @param path directory where IndexReader will be opened
   * @param readOnly if true, the underlying IndexReader
   * will be opened readOnly
   */
  public IndexSearcher(Directory path, boolean readOnly) throws CorruptIndexException, IOException {
    this(IndexReader.open(path, readOnly), true);
  }

  /** Creates a searcher searching the provided index. */
  public IndexSearcher(IndexReader r) {
    this(r, false);
  }
  
  private IndexSearcher(IndexReader r, boolean closeReader) {
    reader = r;
    this.closeReader = closeReader;

    List subReadersList = new ArrayList();
    gatherSubReaders(subReadersList, reader);
    subReaders = (IndexReader[]) subReadersList.toArray(new IndexReader[subReadersList.size()]);
    docStarts = new int[subReaders.length];
    int maxDoc = 0;
    for (int i = 0; i < subReaders.length; i++) {
      docStarts[i] = maxDoc;
      maxDoc += subReaders[i].maxDoc();
    }
  }

  protected void gatherSubReaders(List allSubReaders, IndexReader r) {
    ReaderUtil.gatherSubReaders(allSubReaders, r);
  }

  /** Return the {@link IndexReader} this searches. */
  public IndexReader getIndexReader() {
    return reader;
  }

  /**
   * Note that the underlying IndexReader is not closed, if
   * IndexSearcher was constructed with IndexSearcher(IndexReader r).
   * If the IndexReader was supplied implicitly by specifying a directory, then
   * the IndexReader gets closed.
   */
  public void close() throws IOException {
    if(closeReader)
      reader.close();
  }

  // inherit javadoc
  public int docFreq(Term term) throws IOException {
    return reader.docFreq(term);
  }

  // inherit javadoc
  public Document doc(int i) throws CorruptIndexException, IOException {
    return reader.document(i);
  }
  
  // inherit javadoc
  public Document doc(int i, FieldSelector fieldSelector) throws CorruptIndexException, IOException {
	    return reader.document(i, fieldSelector);
  }
  
  // inherit javadoc
  public int maxDoc() throws IOException {
    return reader.maxDoc();
  }

  // inherit javadoc
  public TopDocs search(Weight weight, Filter filter, final int nDocs) throws IOException {

    if (nDocs <= 0) {
      throw new IllegalArgumentException("nDocs must be > 0");
    }

    TopScoreDocCollector collector = TopScoreDocCollector.create(nDocs, !weight.scoresDocsOutOfOrder());
    search(weight, filter, collector);
    return collector.topDocs();
  }

  public TopFieldDocs search(Weight weight, Filter filter,
      final int nDocs, Sort sort) throws IOException {
    return search(weight, filter, nDocs, sort, true);
  }

  /**
   * Just like {@link #search(Weight, Filter, int, Sort)}, but you choose
   * whether or not the fields in the returned {@link FieldDoc} instances should
   * be set by specifying fillFields.<br>
   *
   * <p>NOTE: this does not compute scores by default.  If you
   * need scores, create a {@link TopFieldCollector}
   * instance by calling {@link TopFieldCollector#create} and
   * then pass that to {@link #search(Weight, Filter,
   * Collector)}.</p>
   */
  public TopFieldDocs search(Weight weight, Filter filter, final int nDocs,
                             Sort sort, boolean fillFields)
      throws IOException {
    
    SortField[] fields = sort.fields;
    boolean legacy = false;
    for(int i = 0; i < fields.length; i++) {
      SortField field = fields[i];
      String fieldname = field.getField();
      int type = field.getType();
      // Resolve AUTO into its true type
      if (type == SortField.AUTO) {
        int autotype = SortField.detectFieldType(reader, fieldname);
        if (autotype == SortField.STRING) {
          fields[i] = new SortField (fieldname, field.getLocale(), field.getReverse());
        } else {
          fields[i] = new SortField (fieldname, autotype, field.getReverse());
        }
      }

      if (field.getUseLegacySearch()) {
        legacy = true;
      }
    }
    
    if (legacy) {
      // Search the single top-level reader
      TopDocCollector collector = new TopFieldDocCollector(reader, sort, nDocs);
      HitCollectorWrapper hcw = new HitCollectorWrapper(collector);
      hcw.setNextReader(reader, 0);
      if (filter == null) {
        Scorer scorer = weight.scorer(reader, true, true);
        if (scorer != null) {
          scorer.score(hcw);
        }
      } else {
        searchWithFilter(reader, weight, filter, hcw);
      }
      return (TopFieldDocs) collector.topDocs();
    }
    
    TopFieldCollector collector = TopFieldCollector.create(sort, nDocs,
        fillFields, fieldSortDoTrackScores, fieldSortDoMaxScore, !weight.scoresDocsOutOfOrder());
    search(weight, filter, collector);
    return (TopFieldDocs) collector.topDocs();
  }

  public void search(Weight weight, Filter filter, Collector collector)
      throws IOException {
    
    if (filter == null) {
      for (int i = 0; i < subReaders.length; i++) { // search each subreader
        collector.setNextReader(subReaders[i], docStarts[i]);
        Scorer scorer = weight.scorer(subReaders[i], !collector.acceptsDocsOutOfOrder(), true);
        if (scorer != null) {
          scorer.score(collector);
        }
      }
    } else {
      for (int i = 0; i < subReaders.length; i++) { // search each subreader
        collector.setNextReader(subReaders[i], docStarts[i]);
        searchWithFilter(subReaders[i], weight, filter, collector);
      }
    }
  }

  private void searchWithFilter(IndexReader reader, Weight weight,
      final Filter filter, final Collector collector) throws IOException {

    assert filter != null;
    
    Scorer scorer = weight.scorer(reader, true, false);
    if (scorer == null) {
      return;
    }

    int docID = scorer.docID();
    assert docID == -1 || docID == DocIdSetIterator.NO_MORE_DOCS;

    // CHECKME: use ConjunctionScorer here?
    DocIdSet filterDocIdSet = filter.getDocIdSet(reader);
    if (filterDocIdSet == null) {
      // this means the filter does not accept any documents.
      return;
    }
    
    DocIdSetIterator filterIter = filterDocIdSet.iterator();
    if (filterIter == null) {
      // this means the filter does not accept any documents.
      return;
    }
    int filterDoc = filterIter.nextDoc();
    int scorerDoc = scorer.advance(filterDoc);
    
    collector.setScorer(scorer);
    while (true) {
      if (scorerDoc == filterDoc) {
        // Check if scorer has exhausted, only before collecting.
        if (scorerDoc == DocIdSetIterator.NO_MORE_DOCS) {
          break;
        }
        collector.collect(scorerDoc);
        filterDoc = filterIter.nextDoc();
        scorerDoc = scorer.advance(filterDoc);
      } else if (scorerDoc > filterDoc) {
        filterDoc = filterIter.advance(scorerDoc);
      } else {
        scorerDoc = scorer.advance(filterDoc);
      }
    }
  }

  public Query rewrite(Query original) throws IOException {
    Query query = original;
    for (Query rewrittenQuery = query.rewrite(reader); rewrittenQuery != query;
         rewrittenQuery = query.rewrite(reader)) {
      query = rewrittenQuery;
    }
    return query;
  }

  public Explanation explain(Weight weight, int doc) throws IOException {
    int n = ReaderUtil.subIndex(doc, docStarts);
    int deBasedDoc = doc - docStarts[n];
    
    return weight.explain(subReaders[n], deBasedDoc);
  }

  private boolean fieldSortDoTrackScores;
  private boolean fieldSortDoMaxScore;

  /** By default, no scores are computed when sorting by
   *  field (using {@link #search(Query,Filter,int,Sort)}).
   *  You can change that, per IndexSearcher instance, by
   *  calling this method.  Note that this will incur a CPU
   *  cost.
   * 
   *  @param doTrackScores If true, then scores are
   *  returned for every matching document in {@link
   *  TopFieldDocs}.
   *
   *  @param doMaxScore If true, then the max score for all
   *  matching docs is computed. */
  public void setDefaultFieldSortScoring(boolean doTrackScores, boolean doMaxScore) {
    fieldSortDoTrackScores = doTrackScores;
    fieldSortDoMaxScore = doMaxScore;
  }
}
