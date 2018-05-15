package org.apache.lucene.analysis;

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
import java.io.Reader;

import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.util.AttributeSource;

/**
 * Emits the entire input as a single token.
 */
public class KeywordTokenizer extends Tokenizer {
  
  private static final int DEFAULT_BUFFER_SIZE = 256;

  private boolean done;
  private int finalOffset;
  private TermAttribute termAtt;
  private OffsetAttribute offsetAtt;
  
  public KeywordTokenizer(Reader input) {
    this(input, DEFAULT_BUFFER_SIZE);
  }

  public KeywordTokenizer(Reader input, int bufferSize) {
    super(input);
    init(bufferSize);
  }

  public KeywordTokenizer(AttributeSource source, Reader input, int bufferSize) {
    super(source, input);
    init(bufferSize);
  }

  public KeywordTokenizer(AttributeFactory factory, Reader input, int bufferSize) {
    super(factory, input);
    init(bufferSize);
  }
  
  private void init(int bufferSize) {
    this.done = false;
    termAtt = (TermAttribute) addAttribute(TermAttribute.class);
    offsetAtt = (OffsetAttribute) addAttribute(OffsetAttribute.class);
    termAtt.resizeTermBuffer(bufferSize);    
  }
  
  public final boolean incrementToken() throws IOException {
    if (!done) {
      clearAttributes();
      done = true;
      int upto = 0;
      char[] buffer = termAtt.termBuffer();
      while (true) {
        final int length = input.read(buffer, upto, buffer.length-upto);
        if (length == -1) break;
        upto += length;
        if (upto == buffer.length)
          buffer = termAtt.resizeTermBuffer(1+buffer.length);
      }
      termAtt.setTermLength(upto);
      finalOffset = correctOffset(upto);
      offsetAtt.setOffset(correctOffset(0), finalOffset);
      return true;
    }
    return false;
  }
  
  public final void end() {
    // set final offset 
    offsetAtt.setOffset(finalOffset, finalOffset);
  }

  /** @deprecated Will be removed in Lucene 3.0. This method is final, as it should
   * not be overridden. Delegates to the backwards compatibility layer. */
  public final Token next(final Token reusableToken) throws IOException {
    return super.next(reusableToken);
  }

  /** @deprecated Will be removed in Lucene 3.0. This method is final, as it should
   * not be overridden. Delegates to the backwards compatibility layer. */
  public final Token next() throws IOException {
    return super.next();
  }

  public void reset(Reader input) throws IOException {
    super.reset(input);
    this.done = false;
  }
}
