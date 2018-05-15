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

import org.apache.lucene.store.Directory;

final class FormatPostingsFieldsWriter extends FormatPostingsFieldsConsumer {

  final Directory dir;
  final String segment;
  final TermInfosWriter termsOut;
  final FieldInfos fieldInfos;
  final FormatPostingsTermsWriter termsWriter;
  final DefaultSkipListWriter skipListWriter;
  final int totalNumDocs;

  public FormatPostingsFieldsWriter(SegmentWriteState state, FieldInfos fieldInfos) throws IOException {
    super();

    dir = state.directory;
    segment = state.segmentName;
    totalNumDocs = state.numDocs;
    this.fieldInfos = fieldInfos;
    termsOut = new TermInfosWriter(dir,
                                   segment,
                                   fieldInfos,
                                   state.termIndexInterval);

    // TODO: this is a nasty abstraction violation (that we
    // peek down to find freqOut/proxOut) -- we need a
    // better abstraction here whereby these child consumers
    // can provide skip data or not
    skipListWriter = new DefaultSkipListWriter(termsOut.skipInterval,
                                               termsOut.maxSkipLevels,
                                               totalNumDocs,
                                               null,
                                               null);

    state.flushedFiles.add(state.segmentFileName(IndexFileNames.TERMS_EXTENSION));
    state.flushedFiles.add(state.segmentFileName(IndexFileNames.TERMS_INDEX_EXTENSION));

    termsWriter = new FormatPostingsTermsWriter(state, this);
  }

  /** Add a new field */
  FormatPostingsTermsConsumer addField(FieldInfo field) {
    termsWriter.setField(field);
    return termsWriter;
  }

  /** Called when we are done adding everything. */
  void finish() throws IOException {
    termsOut.close();
    termsWriter.close();
  }
}
