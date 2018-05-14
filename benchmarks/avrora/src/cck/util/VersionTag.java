/**
 * Copyright (c) 2004-2005, Regents of the University of California
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
 */

package cck.util;

import java.util.HashMap;

/**
 * The <code>Version</code> class represents a version number, including the major version, the commit number,
 * as well as the date and time of the last commit.
 *
 * @author Ben L. Titzer
 */
public class VersionTag {

    private static final HashMap tags = new HashMap();

    /**
     * The <code>getVersionTag()</code> method gets the version tag for the specified module.
     *
     * @param module the name of the module as a string
     * @return a reference to the version tag for the specified module if it exists; null otherwise
     */
    public static VersionTag getVersionTag(String module) {
        return (VersionTag) tags.get(module);
    }

    /**
     * The <code>module</code> field stores the name of the module as a string.
     */
    public final String module;

    /**
     * The <code>prefix</code> field stores the string that the prefix of the version (if any) for this
     * version.
     */
    public final String prefix;

    /**
     * The <code>major</code> field stores the integer that represents the major number.
     */
    public final int major;

    /**
     * The <code>minor</code> field stores the integer that represents the minor number.
     */
    public final int minor;

    /**
     * The <code>commit</code> field stores the commit number (i.e. the number of code revisions committed to
     * CVS since the last release).
     */
    public final int commit;

    /**
     * The <code>string</code> field stores a reference to this version instance represented as a string.
     */
    public final String string;

    /**
     * The <code>suffix</code> field stores a reference to this version instance represented as a string
     * that is suitable as a file suffix.
     */
    public final String suffix;

    /**
     * The constructor for the <code>Version</code> class creates a new version object that represents
     * the version of the code.
     *
     * @param mod the module name
     * @param prefix a string representing the release name, such as <code>"Beta"</code> for example
     * @param maj    the major version number
     * @param min    the minor version number
     * @param comm   the commit number
     */
    public VersionTag(String mod, String prefix, int maj, int min, int comm) {
        this.module = mod;
        this.prefix = prefix;
        this.major = maj;
        this.minor = min;
        this.commit = comm;
        this.string = prefixString(false, ' ') + major + '.' + minor + '.' + commitNumber();
        this.suffix = prefixString(true, '-') + major + '.' + minor + '.' + commitNumber();
        tags.put(mod, this);
    }

    /**
     * The <code>toString()</code> method converts this version to a string.
     *
     * @return a string representation of this version
     */
    public String toString() {
        return string;
    }

    private String prefixString(boolean lower, char suffix) {
        if ( prefix.length() == 0 ) return "";
        return (lower ? prefix.toLowerCase() : prefix) + suffix;
    }

    private String commitNumber() {
        if ( commit < 10 ) return "00"+commit;
        if ( commit < 100 ) return "0"+commit;
        return Integer.toString(commit);
    }

    /**
     * The <code>isStable()</code> method returns whether this version tag corresponds to
     * a stable release of the source code. It determines this by the <code>minor</code>
     * version number; if this number is even, then the code is considered to be stable.
     *
     * @return true if this version tag corresponds to a stable version
     */
    public boolean isStable() {
        return minor % 2 == 0;
    }
}
