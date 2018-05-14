/**
 * Copyright (c) 2005, Regents of the University of California
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
 * Created Sep 21, 2005
 */
package jintgen.isdl;

import jintgen.jigir.Decl;
import jintgen.types.Type;
import java.util.HashMap;

/**
 * @author Ben L. Titzer
 */
public class Environment {

    protected final Environment parent;
    protected final HashMap<String, Decl> varMap;
    protected final HashMap<String, SubroutineDecl> methodMap;

    public Environment(Environment p) {
        parent = p;
        varMap = new HashMap<String, Decl>();
        methodMap = new HashMap<String, SubroutineDecl>();
    }

    public Decl resolveDecl(String name) {
        Decl decl = varMap.get(name);
        if ( decl != null ) return decl;
        return parent != null ? parent.resolveDecl(name) : null;
    }

    public SubroutineDecl resolveSubroutine(String name) {
        SubroutineDecl type = methodMap.get(name);
        if ( type != null ) return type;
        return parent != null ? parent.resolveSubroutine(name) : null;
    }

    public void addDecl(Decl d) {
        varMap.put(d.getName(), d);
    }

    public void addVariable(String name, Type t) {
        varMap.put(name, new VarDecl(name, t));
    }

    public void addSubroutine(SubroutineDecl d) {
        methodMap.put(d.name.image, d);
    }

    public boolean isDefinedLocally(String name) {
        return varMap.containsKey(name);
    }

    public Iterable<Decl> getDecls() {
        return varMap.values();
    }

    protected class VarDecl implements Decl {
        protected final String name;
        protected final Type type;

        protected VarDecl(String n, Type t) {
            name = n;
            type = t;
        }

        public String getName() {
            return name;
        }

        public Type getType() {
            return type;
        }
    }
}
