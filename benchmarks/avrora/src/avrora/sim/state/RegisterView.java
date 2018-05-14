/**
 * Copyright (c) 2007, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * Neither the name of the University of California, Los Angeles nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * Created Sep 2, 2007
 */
package avrora.sim.state;

/**
 * The <code>RegisterView</code> class abstracts the storage of a register's
 * actual state from its view. Since many hardware registers have complex
 * internal state, including many different subfields, this interface provides
 * a way to view a sequence of bits as contiguous though the bits may in fact
 * be spread over several registers, or mixed within a single register.
 *
 * Each register view appears as a contiguous range of bits with a fixed width.
 * Utilities in the <code>RegisterUtil</code> class allow creating views of
 * register and (and views of views) that are useful.
 *
 * Most register views do not actually store any state, but instead will retrieve
 * the actual state from wherever it is stored when requested by the <code>getValue()</code>
 * method. This simplifies the task of maintaining a consistent view of the state,
 * since there is typically only one place where the actual state is stored.
 *
 * @author Ben L. Titzer
 */
public interface RegisterView {

    /**
     * The <code>getWidth() method gets the width of this register view.
     * 
     * @return the width of this register view in bits
     */
    public int getWidth();

    /**
     * The <code>getValue()</code> method gets the value of the bits
     * corresponding to this register view. A register view may not necessarily
     * store the actual state, and therefore may call the <code>getValue()</code>
     * method on any underlying registers or register views in order to construct
     * this register view's state.
     *
     * @return the value of the bits of this register view
     */
    public int getValue();

    /**
     * The <code>setValue()</code> updates this register view's value,
     * wherever the actual state is stored. Because a register view may
     * not actually store the state of the bits, it may call the <code>setValue</code>
     * method on any underlying registers or register views in order to update
     * the actual state, wherever it happens to be stored.
     *
     * @param val the new value of the bits
     */
    public void setValue(int val);
}

