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
package avrora.test.probes;

import avrora.sim.Simulator;
import avrora.sim.State;
import avrora.sim.clock.DeltaQueue;
import avrora.sim.output.SimPrinter;
import cck.text.StringUtil;
import java.util.*;

/**
 * @author Ben L. Titzer
 */
public class ProbeTest {

    final HashMap entities;

    Simulator simulator;
    SimPrinter printer;
    DeltaQueue eventqueue;

    List mainCode;
    final List expectedEvents;
    List recordedEvents;

    ProbeTest() {
        entities = new HashMap();
        expectedEvents = new LinkedList();
    }

    class Event {
        final long time;
        final String name;

        Event(long t, String n) {
            time = t;
            name = n;
        }
    }

    abstract class TestEntity {
        final String name;

        TestEntity(String n) {
            name = n;
        }

        abstract void insert(int value);
        abstract void remove(int value);
    }

    public class TestProbe extends TestEntity implements Simulator.Probe {

        List beforeStmts;
        List afterStmts;

        TestProbe(String name, List b, List a) {
            super(name);
            beforeStmts = b;
            afterStmts = a;
        }

        public void fireBefore(State s, int addr) {
            recordEvent(name+".before");
            execute(beforeStmts);
        }

        public void fireAfter(State s, int addr) {
            recordEvent(name+".after");
            execute(afterStmts);
        }

        void insert(int value) {
            simulator.insertProbe(this, value);
        }

        void remove(int value) {
            simulator.removeProbe(this, value);
        }
    }

    public class TestWatch extends TestEntity implements Simulator.Watch {
        List beforeReadStmts;
        List afterReadStmts;
        List beforeWriteStmts;
        List afterWriteStmts;

        TestWatch(String name, List b1, List a1, List b2, List a2) {
            super(name);
            beforeReadStmts = b1;
            afterReadStmts = a1;
            beforeWriteStmts = b2;
            afterWriteStmts = a2;
        }

        public void fireBeforeRead(State state, int data_addr) {
            recordEvent(name+".beforeRead");
            execute(beforeReadStmts);
        }

        public void fireBeforeWrite(State state, int data_addr, byte value) {
            recordEvent(name+".beforeWrite");
            execute(beforeWriteStmts);
        }

        public void fireAfterRead(State state, int data_addr, byte value) {
            recordEvent(name+".afterRead");
            execute(afterReadStmts);
        }

        public void fireAfterWrite(State state, int data_addr, byte value) {
            recordEvent(name+".afterWrite");
            execute(afterWriteStmts);
        }

        void insert(int value) {
            simulator.insertWatch(this, value);
        }

        void remove(int value) {
            simulator.removeWatch(this, value);
        }
    }

    public class TestEvent extends TestEntity implements Simulator.Event {
        List fireStmts;

        TestEvent(String name, List b) {
            super(name);
            fireStmts = b;
        }


        public void fire() {
            recordEvent(name);
            execute(fireStmts);
        }

        void insert(int value) {
            if ( simulator != null ) simulator.insertEvent(this, value);
            else eventqueue.insertEvent(this, value);
        }

        void remove(int value) {
            if ( simulator != null ) simulator.removeEvent(this);
            else eventqueue.removeEvent(this);
        }
    }

    abstract class Stmt {
        abstract void execute();
    }

    class InsertStmt extends Stmt {
        final String name;
        final int value;

        InsertStmt(String n, int v) {
            name = n;
            value = v;
        }
        void execute() {
            TestEntity en = (TestEntity)entities.get(name);
            en.insert(value);
        }
    }

    class RemoveStmt extends Stmt {
        final String name;
        final int value;

        RemoveStmt(String n, int v) {
            name = n;
            value = v;
        }

        void execute() {
            TestEntity en = (TestEntity)entities.get(name);
            en.remove(value);
        }
    }

    class AdvanceStmt extends Stmt {
        final int value;

        AdvanceStmt(int v) {
            value = v;
        }

        void execute() {
            eventqueue.advance(value);
        }
    }

    class RunStmt extends Stmt {
        void execute() {
            simulator.start();
        }
    }

    protected void execute(List l) {
        Iterator i = l.iterator();
        while ( i.hasNext() ) {
            Stmt s = (Stmt)i.next();
            s.execute();
        }
    }

    protected void recordEvent(String e) {
        if (printer != null) {
            printer.println(e);
        }
        long time = simulator == null ? eventqueue.getCount() : simulator.getState().getCycles();
        recordedEvents.add(new Event(time, e));
    }

    //-- interface for building the test case

    public void newProbe(Token name, List b, List a) {
        TestEntity e = new TestProbe(name.image, b, a);
        entities.put(name.image, e);
    }

    public void newWatch(Token name, List b1, List a1, List b2, List a2) {
        TestEntity e = new TestWatch(name.image, b1, a1, b2, a2);
        entities.put(name.image, e);
    }

    public void newEvent(Token name, List b) {
        TestEntity e = new TestEvent(name.image, b);
        entities.put(name.image, e);
    }

    public void addInsert(List l, Token n, Token v) {
        l.add(new InsertStmt(n.image, StringUtil.evaluateIntegerLiteral(v.image)));
    }

    public void addRemove(List l, Token n, Token v) {
        l.add(new RemoveStmt(n.image, StringUtil.evaluateIntegerLiteral(v.image)));
    }

    public void addAdvance(List l, Token v) {
        l.add(new AdvanceStmt(StringUtil.evaluateIntegerLiteral(v.image)));
    }

    public void addRun(List l) {
        l.add(new RunStmt());
    }

    public void addMainCode(List l) {
        mainCode = l;
    }

    public void addResultEvent(Token time, Token name) {
        expectedEvents.add(new Event(StringUtil.evaluateIntegerLiteral(time.image), name.image));
    }

    public void run(Simulator s) throws Exception {
        simulator = s;
        printer = s.getPrinter("test.probes");
        eventqueue = null;
        recordedEvents = new LinkedList();
        execute(mainCode);
        s.start();
        match();
    }

    public void run(DeltaQueue q) throws Exception {
        eventqueue = q;
        simulator = null;
        recordedEvents = new LinkedList();
        execute(mainCode);
        match();
    }

    public void match() throws Exception {
        Iterator e = expectedEvents.iterator();
        Iterator r = recordedEvents.iterator();
        int cntr = 1;

        while ( e.hasNext() ) {
            if ( !r.hasNext() ) {
                throw new Failure("too few events recorded");
            }

            Event expect = (Event)e.next();
            Event recorded = (Event)r.next();

            if (printer != null) {
                printer.println(" --> checking "+recorded.time+" "+recorded.name+" = "
                        +expect.time+" "+expect.name);
            }

            if ( !expect.name.equals(recorded.name) ) {
                throw new Failure("incorrect event #"+cntr+": "+recorded.name+" should be "+expect.name);
            }

            if ( expect.time != recorded.time ) {
                throw new Failure("timing incorrect for event #"+cntr+": "+recorded.time+" should be "+expect.time);
            }

            cntr++;
        }

        if ( r.hasNext() ) {
            throw new Failure("too many events recorded");
        }

    }

    public class Failure extends Exception {
        public final String reason;

        Failure(String s) {
            reason = s;
        }
    }

}
