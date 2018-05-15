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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.util.AttributeSource;

/**
 * This class can be used if the token attributes of a TokenStream
 * are intended to be consumed more than once. It caches
 * all token attribute states locally in a List.
 * 
 * <P>CachingTokenFilter implements the optional method
 * {@link TokenStream#reset()}, which repositions the
 * stream to the first Token. 
 */
public class CachingTokenFilter extends TokenFilter {
  private List cache = null;
  private Iterator iterator = null; 
  private AttributeSource.State finalState;
  
  public CachingTokenFilter(TokenStream input) {
    super(input);
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
  
  public final boolean incrementToken() throws IOException {
    if (cache == null) {
      // fill cache lazily
      cache = new LinkedList();
      fillCache();
      iterator = cache.iterator();
    }
    
    if (!iterator.hasNext()) {
      // the cache is exhausted, return false
      return false;
    }
    // Since the TokenFilter can be reset, the tokens need to be preserved as immutable.
    restoreState((AttributeSource.State) iterator.next());
    return true;
  }
  
  public final void end() throws IOException {
    if (finalState != null) {
      restoreState(finalState);
    }
  }

  public void reset() throws IOException {
    if(cache != null) {
      iterator = cache.iterator();
    }
  }
  
  private void fillCache() throws IOException {
    while(input.incrementToken()) {
      cache.add(captureState());
    }
    // capture final state
    input.end();
    finalState = captureState();
  }

}
