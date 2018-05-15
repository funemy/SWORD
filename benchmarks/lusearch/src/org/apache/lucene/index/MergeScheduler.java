package org.apache.lucene.index;

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

/** <p>Expert: {@link IndexWriter} uses an instance
 *  implementing this interface to execute the merges
 *  selected by a {@link MergePolicy}.  The default
 *  MergeScheduler is {@link ConcurrentMergeScheduler}.</p>
 *
 * <p><b>NOTE:</b> This API is new and still experimental
 * (subject to change suddenly in the next release)</p>
 *
 * <p><b>NOTE</b>: This class typically requires access to
 * package-private APIs (eg, SegmentInfos) to do its job;
 * if you implement your own MergePolicy, you'll need to put
 * it in package org.apache.lucene.index in order to use
 * these APIs.
*/

public abstract class MergeScheduler {

  /** Run the merges provided by {@link IndexWriter#getNextMerge()}. */
  abstract void merge(IndexWriter writer)
    throws CorruptIndexException, IOException;

  /** Close this MergeScheduler. */
  abstract void close()
    throws CorruptIndexException, IOException;
}
