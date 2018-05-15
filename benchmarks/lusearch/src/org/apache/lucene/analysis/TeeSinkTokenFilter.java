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
import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.util.AttributeImpl;
import org.apache.lucene.util.AttributeSource;

/**
 * This TokenFilter provides the ability to set aside attribute states
 * that have already been analyzed.  This is useful in situations where multiple fields share
 * many common analysis steps and then go their separate ways.
 * <p/>
 * It is also useful for doing things like entity extraction or proper noun analysis as
 * part of the analysis workflow and saving off those tokens for use in another field.
 *
 * <pre>
TeeSinkTokenFilter source1 = new TeeSinkTokenFilter(new WhitespaceTokenizer(reader1));
TeeSinkTokenFilter.SinkTokenStream sink1 = source1.newSinkTokenStream();
TeeSinkTokenFilter.SinkTokenStream sink2 = source1.newSinkTokenStream();

TeeSinkTokenFilter source2 = new TeeSinkTokenFilter(new WhitespaceTokenizer(reader2));
source2.addSinkTokenStream(sink1);
source2.addSinkTokenStream(sink2);

TokenStream final1 = new LowerCaseFilter(source1);
TokenStream final2 = source2;
TokenStream final3 = new EntityDetect(sink1);
TokenStream final4 = new URLDetect(sink2);

d.add(new Field("f1", final1));
d.add(new Field("f2", final2));
d.add(new Field("f3", final3));
d.add(new Field("f4", final4));
 * </pre>
 * In this example, <code>sink1</code> and <code>sink2</code> will both get tokens from both
 * <code>reader1</code> and <code>reader2</code> after whitespace tokenizer
 * and now we can further wrap any of these in extra analysis, and more "sources" can be inserted if desired.
 * It is important, that tees are consumed before sinks (in the above example, the field names must be
 * less the sink's field names). If you are not sure, which stream is consumed first, you can simply
 * add another sink and then pass all tokens to the sinks at once using {@link #consumeAllTokens}.
 * This TokenFilter is exhausted after this. In the above example, change
 * the example above to:
 * <pre>
...
TokenStream final1 = new LowerCaseFilter(source1.newSinkTokenStream());
TokenStream final2 = source2.newSinkTokenStream();
sink1.consumeAllTokens();
sink2.consumeAllTokens();
...
 * </pre>
 * In this case, the fields can be added in any order, because the sources are not used anymore and all sinks are ready.
 * <p>Note, the EntityDetect and URLDetect TokenStreams are for the example and do not currently exist in Lucene.
 */
public final class TeeSinkTokenFilter extends TokenFilter {
  private final List sinks = new LinkedList();
  
  /**
   * Instantiates a new TeeSinkTokenFilter.
   */
  public TeeSinkTokenFilter(TokenStream input) {
    super(input);
  }

  /**
   * Returns a new {@link SinkTokenStream} that receives all tokens consumed by this stream.
   */
  public SinkTokenStream newSinkTokenStream() {
    return newSinkTokenStream(ACCEPT_ALL_FILTER);
  }
  
  /**
   * Returns a new {@link SinkTokenStream} that receives all tokens consumed by this stream
   * that pass the supplied filter.
   * @see SinkFilter
   */
  public SinkTokenStream newSinkTokenStream(SinkFilter filter) {
    SinkTokenStream sink = new SinkTokenStream(this.cloneAttributes(), filter);
    this.sinks.add(new WeakReference(sink));
    return sink;
  }
  
  /**
   * Adds a {@link SinkTokenStream} created by another <code>TeeSinkTokenFilter</code>
   * to this one. The supplied stream will also receive all consumed tokens.
   * This method can be used to pass tokens from two different tees to one sink.
   */
  public void addSinkTokenStream(final SinkTokenStream sink) {
    // check that sink has correct factory
    if (!this.getAttributeFactory().equals(sink.getAttributeFactory())) {
      throw new IllegalArgumentException("The supplied sink is not compatible to this tee");
    }
    // add eventually missing attribute impls to the existing sink
    for (Iterator it = this.cloneAttributes().getAttributeImplsIterator(); it.hasNext(); ) {
      sink.addAttributeImpl((AttributeImpl) it.next());
    }
    this.sinks.add(new WeakReference(sink));
  }
  
  /**
   * <code>TeeSinkTokenFilter</code> passes all tokens to the added sinks
   * when itself is consumed. To be sure, that all tokens from the input
   * stream are passed to the sinks, you can call this methods.
   * This instance is exhausted after this, but all sinks are instant available.
   */
  public void consumeAllTokens() throws IOException {
    while (incrementToken());
  }
  
  public boolean incrementToken() throws IOException {
    if (input.incrementToken()) {
      // capture state lazily - maybe no SinkFilter accepts this state
      AttributeSource.State state = null;
      for (Iterator it = sinks.iterator(); it.hasNext(); ) {
        final SinkTokenStream sink = (SinkTokenStream) ((WeakReference) it.next()).get();
        if (sink != null) {
          if (sink.accept(this)) {
            if (state == null) {
              state = this.captureState();
            }
            sink.addState(state);
          }
        }
      }
      return true;
    }
    
    return false;
  }
  
  public final void end() throws IOException {
    super.end();
    AttributeSource.State finalState = captureState();
    for (Iterator it = sinks.iterator(); it.hasNext(); ) {
      final SinkTokenStream sink = (SinkTokenStream) ((WeakReference) it.next()).get();
      if (sink != null) {
        sink.setFinalState(finalState);
      }
    }
  }
  
  /**
   * A filter that decides which {@link AttributeSource} states to store in the sink.
   */
  public static abstract class SinkFilter {
    /**
     * Returns true, iff the current state of the passed-in {@link AttributeSource} shall be stored
     * in the sink. 
     */
    public abstract boolean accept(AttributeSource source);
    
    /**
     * Called by {@link SinkTokenStream#reset()}. This method does nothing by default
     * and can optionally be overridden.
     */
    public void reset() throws IOException {
      // nothing to do; can be overridden
    }
  }
  
  public static final class SinkTokenStream extends TokenStream {
    private final List cachedStates = new LinkedList();
    private AttributeSource.State finalState;
    private Iterator it = null;
    private SinkFilter filter;
    
    private SinkTokenStream(AttributeSource source, SinkFilter filter) {
      super(source);
      this.filter = filter;
    }
    
    private boolean accept(AttributeSource source) {
      return filter.accept(source);
    }
    
    private void addState(AttributeSource.State state) {
      if (it != null) {
        throw new IllegalStateException("The tee must be consumed before sinks are consumed.");
      }
      cachedStates.add(state);
    }
    
    private void setFinalState(AttributeSource.State finalState) {
      this.finalState = finalState;
    }
    
    public final boolean incrementToken() throws IOException {
      // lazy init the iterator
      if (it == null) {
        it = cachedStates.iterator();
      }
    
      if (!it.hasNext()) {
        return false;
      }
      
      AttributeSource.State state = (State) it.next();
      restoreState(state);
      return true;
    }
  
    public final void end() throws IOException {
      if (finalState != null) {
        restoreState(finalState);
      }
    }
    
    public final void reset() {
      it = cachedStates.iterator();
    }
  }
    
  private static final SinkFilter ACCEPT_ALL_FILTER = new SinkFilter() {
    public boolean accept(AttributeSource source) {
      return true;
    }
  };
  
}
