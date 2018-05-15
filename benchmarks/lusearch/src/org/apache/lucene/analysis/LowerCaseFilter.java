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

import org.apache.lucene.analysis.tokenattributes.TermAttribute;

/**
 * Normalizes token text to lower case.
 *
 * @version $Id: LowerCaseFilter.java 797665 2009-07-24 21:45:48Z buschmi $
 */
public final class LowerCaseFilter extends TokenFilter {
  public LowerCaseFilter(TokenStream in) {
    super(in);
    termAtt = (TermAttribute) addAttribute(TermAttribute.class);
  }

  private TermAttribute termAtt;
  
  public final boolean incrementToken() throws IOException {
    if (input.incrementToken()) {

      final char[] buffer = termAtt.termBuffer();
      final int length = termAtt.termLength();
      for(int i=0;i<length;i++)
        buffer[i] = Character.toLowerCase(buffer[i]);

      return true;
    } else
      return false;
  }
}
