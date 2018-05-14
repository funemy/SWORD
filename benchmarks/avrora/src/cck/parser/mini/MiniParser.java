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
 * Creation date: Jan 12, 2006
 */

package cck.parser.mini;

/**
 * The <code>MiniParser</code> class implements the base functionality of a parser
 * that can build lexical tokens from a file. It does not manage the lexical state,
 * but rather deals with loading the individual characters from a file while
 * remembering their position in the file (for debugging).
 *
 * @author Ben L. Titzer
 */
public class MiniParser {

    //=========================================================================================
    //========================== B A S I C = L I B R A R Y ====================================
    //=========================================================================================

    // INT-pos: int "{ldigit:[1-9]}{ival:[0-9]*}" -> evalDecimal(lval,ival)
    // INT-neg: int "-{ival:INT-pos}"             -> -(ival)
    // INT-hex: int "0x{hval:[0-9,a-f,A-F]*}"     -> evalHexadecimal(hval)
    // INT-oct: int "0{oval:[0-7]*}"              -> evalOctal(oval);

    // BOOL-true: boolean "true" -> true
    // BOOL-false: boolean "false" -> false

    // IDENT: String "{lchar:[a-z,A-z,_]}{tchar:[a-z,A-z,_,0-9]*}" -> literal(lchar, tchar)

    // STRING: String "{DQUOTE}{ch:StrChar*}{DQUOTE}"  -> new Symbol(ch)
    // StrChar-slash: "{BSLASH}{BSLASH}"               -> '\\'
    // StrChar-nl:    "{BSLASH}n"                      -> '\n'
    // StrChar-tab:   "{BSLASH}t"                      -> '\t'
    // StrChar-tab:   "{BSLASH}{DQUOTE}"               -> '"'
    // StrChar-octal: "{BSLASH}{ch:[0-3]}{chl:[0-7]*}" -> (char)evalOctal(ch, chl)

    //=========================================================================================
    //============================== M I N I = P A R S E R ====================================
    //=========================================================================================

    // Rule: "{case:Case} : {type:Type?} {pat:Pattern} -> {expr:Expr}\n" -> new Rule(case, type, pat, expr)
    // Case: "{cid:IDENT}{scid:SubCase?}" -> new Case(cid, scid)
    // SubCase: "-{id:IDENT}"                    -> id
    // Pattern: "\"{i:Item*}\""                  -> i
    // Item-prod: "\{i:Inner\}"                  -> i
    // Item-lbesc: "\\\{"                        -> LBRACKET
    // Item-rbesc: "\\\}"                        -> RBRACKET
    // Item-qesc: "\\\""                         -> QUOTE
    // Item-char: "{ch:CHAR}"                    -> ch
    // Inner: "{name:Var?} {prod:Prod} {mod:Mod*}" -> new Production(name, prod, mod)
    // Var: "{nm:IDENT} :"                        -> nm
    // Prod-id: "{prod:IDENT}"                   -> new ProdRef(prod)
    // Prod-range: "[{s:Set list}]"              -> new Set(merge(s))

    // Set-range: "{cl:CHAR}{en:End?}" -> new Range(cl, en)
    // End: "-{ch:CHAR}"               -> ch

    // Mod-q: "?"    -> QUEST
    // Mod-s: "*"    -> STAR
    // Mod-l: "list" -> LIST
    // Mod-s: "+"    -> PLUS

    public MiniParser(String fname) {
    }
}
