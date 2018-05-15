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
import java.io.IOException;
import java.io.Serializable;

/**
 * Expert: returns a comparator for sorting ScoreDocs.
 * 
 * <p>
 * Created: Apr 21, 2004 3:49:28 PM
 * 
 * This class will be used as part of a key to a FieldCache value. You must
 * implement hashCode and equals to avoid an explosion in RAM usage if you use
 * instances that are not the same instance. If you are searching using the
 * Remote contrib, the same instance of this class on the client will be a new
 * instance on every call to the server, so hashCode/equals is very important in
 * that situation.
 * 
 * @version $Id: SortComparatorSource.java 747019 2009-02-23 13:59:50Z
 *          mikemccand $
 * @since 1.4
 * @deprecated Please use {@link FieldComparatorSource} instead.
 */
public interface SortComparatorSource
extends Serializable {

  /**
   * Creates a comparator for the field in the given index.
   * @param reader Index to create comparator for.
   * @param fieldname  Name of the field to create comparator for.
   * @return Comparator of ScoreDoc objects.
   * @throws IOException If an error occurs reading the index.
   */
  ScoreDocComparator newComparator (IndexReader reader, String fieldname)
  throws IOException;
}
