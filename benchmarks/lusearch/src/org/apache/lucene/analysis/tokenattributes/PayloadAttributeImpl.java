package org.apache.lucene.analysis.tokenattributes;

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

import java.io.Serializable;

import org.apache.lucene.index.Payload;
import org.apache.lucene.util.AttributeImpl;

/**
 * The payload of a Token. See also {@link Payload}.
 */
public class PayloadAttributeImpl extends AttributeImpl implements PayloadAttribute, Cloneable, Serializable {
  private Payload payload;  
  
  /**
   * Initialize this attribute with no payload.
   */
  public PayloadAttributeImpl() {}
  
  /**
   * Initialize this attribute with the given payload. 
   */
  public PayloadAttributeImpl(Payload payload) {
    this.payload = payload;
  }
  
  /**
   * Returns this Token's payload.
   */ 
  public Payload getPayload() {
    return this.payload;
  }

  /** 
   * Sets this Token's payload.
   */
  public void setPayload(Payload payload) {
    this.payload = payload;
  }
  
  public void clear() {
    payload = null;
  }

  public Object clone()  {
    PayloadAttributeImpl clone = (PayloadAttributeImpl) super.clone();
    if (payload != null) {
      clone.payload = (Payload) payload.clone();
    }
    return clone;
  }

  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    
    if (other instanceof PayloadAttribute) {
      PayloadAttributeImpl o = (PayloadAttributeImpl) other;
      if (o.payload == null || payload == null) {
        return o.payload == null && payload == null;
      }
      
      return o.payload.equals(payload);
    }
    
    return false;
  }

  public int hashCode() {
    return (payload == null) ? 0 : payload.hashCode();
  }

  public void copyTo(AttributeImpl target) {
    PayloadAttribute t = (PayloadAttribute) target;
    t.setPayload((payload == null) ? null : (Payload) payload.clone());
  }  

  
}
