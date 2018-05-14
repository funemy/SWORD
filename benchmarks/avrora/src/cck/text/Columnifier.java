/**
 * Copyright (c) 2006, Regents of the University of California
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
 * Created Aug 24, 2006
 */
package cck.text;

import java.util.Arrays;

/**
 * The <code>Columnifier</code> class implements a text utility that can format
 * a sequence of strings into a columnar format. Each column can have a width, a
 * justification (i.e. left or right), and a color.
 *
 * @author Ben L. Titzer
 */
public class Columnifier {

    protected int margin;
    protected int[] color;
    protected int[] width;
    protected boolean[] right;

    public Columnifier() {
        width = new int[0];
        color = new int[0];
        right = new boolean[0];
    }

    public Columnifier(int[] w) {
        width = w;
        right = new boolean[w.length];
        color = new int[w.length];
        Arrays.fill(color, -1);
    }

    public Columnifier(int[] c, int[] w) {
        assert c.length == w.length;
        width = w;
        right = new boolean[w.length];
        color = c;
    }

    public Columnifier(int[] c, int[] w, boolean[] r) {
        // allow the user to specify the default colors
        if ( c == null ) {
            c = new int[w.length];
            Arrays.fill(c, -1);
        }
        // all of the arrays must be the same length
        assert c.length == w.length && w.length == r.length;

        width = w;
        right = r;
        color = c;
    }

    public void setLeftMargin(int l) {
        margin = l;
    }

    public void addColumn(int c, int w, boolean r) {
        int pl = color.length;
        System.arraycopy(color, 0, color = new int[pl + 1], 0, pl);
        System.arraycopy(width, 0, width = new int[pl + 1], 0, pl);
        System.arraycopy(right, 0, right = new boolean[pl + 1], 0, pl);
        color[pl] = c;
        width[pl] = w;
        right[pl] = r;
    }

    public void println(String[] str) {
        assert str.length == width.length;

        // print the left margin
        Terminal.print(StringUtil.space(margin));

        // print each column
        for ( int cntr = 0; cntr < str.length; cntr++ ) {
            String s = str[cntr];
            int sw = s.length();
            int w = width[cntr];
            String j = sw < w ? StringUtil.space(w - sw) : "";
            if (right[cntr]) {
                Terminal.print(j);
                Terminal.print(color[cntr], s);
            } else {
                Terminal.print(color[cntr], s);
                Terminal.print(j);
            }
        }

        // go to the next line
        Terminal.nextln();
    }

    public void printTitle(int color, String[] str) {
        assert str.length == width.length;

        // print the left margin
        Terminal.print(StringUtil.space(margin));

        // print each column
        for ( int cntr = 0; cntr < str.length; cntr++ ) {
            String s = str[cntr];
            int sw = s.length();
            int w = width[cntr];
            String j = sw < w ? StringUtil.space(w - sw) : "";
            if (right[cntr]) {
                Terminal.print(j);
                Terminal.print(color, s);
            } else {
                Terminal.print(color, s);
                Terminal.print(j);
            }
        }

        // go to the next line
        Terminal.nextln();
        TermUtil.printThinSeparator();
    }
}
