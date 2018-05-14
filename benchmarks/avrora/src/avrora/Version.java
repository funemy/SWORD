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

package avrora;

import cck.util.VersionTag;

/**
 * The <code>Version</code> class records the version information for this module.
 * It has a single static method called <code>getVersion()</code> to retrieve the
 * version for this module in a <code>VersionTag</code> object.
 *
 * </p>
 * This file is automatically updated by CVS commit scripts that increment the
 * commit number each time code is committed to CVS. This guarantees that the
 * version number uniquely determines the version of the software.
 *
 * @author Ben L. Titzer
 */
public class Version {

    /**
     * The <code>commit</code> field stores the commit number (i.e. the number of code revisions committed to
     * CVS since the last release).
     */
    public static final int commit = 110;

    /**
     * The <code>TAG</code> field stores a reference to the version tag for the current
     * release and commit number.
     */
    public static final VersionTag TAG = new VersionTag("avrora", "Beta", 1, 7, commit);

    /**
     * The <code>main()</code> method implements an entrypoint to the version record
     * that prints out the version as a path suffix, in order to allow external tools
     * (such as the jar archiver) to create files tied to this particular version.
     * @param args the arguments given at the command line
     */
    public static void main(String[] args) {
        System.out.println(TAG.suffix);
    }
}
