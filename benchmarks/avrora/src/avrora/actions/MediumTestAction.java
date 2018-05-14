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
 * Created Oct 17, 2007
 */
package avrora.actions;

import cck.util.Option;
import cck.text.StringUtil;
import cck.text.Terminal;
import avrora.sim.radio.Medium;
import avrora.sim.clock.Clock;
import avrora.sim.clock.MainClock;
import avrora.sim.Simulator;

/**
 * The <code>MediumTestAction</code> definition.
 *
 * @author Ben L. Titzer
 */
public class MediumTestAction extends Action {
    public static final String HELP = "The \"test\" action invokes the internal automated testing framework " +
            "that runs test cases supplied at the command line. The test cases are " +
            "used in regressions for diagnosing bugs.";

    public final Option.Bool DETAIL = newOption("detail", false, "This option selects whether " +
            "the testing framework will report detailed information for failed test cases.");

    public MediumTestAction() {
        super(HELP);
    }

    public void run(String[] args) throws Exception {
        // create a medium and test it.
        Medium m = new Medium(null, null, 300, 102, 8, 128);
        MainClock c = new MainClock("main", 3000);
        final Medium.Transmitter t = new TestTransmitter(m, c);
        final Medium.Receiver r = new TestReceiver(m, c);

        Simulator.Event send = new Simulator.Event() {
            public void fire() {
				//power and frequency are doubles now
                t.beginTransmit(0.0,2.4);
            }
        };

        Simulator.Event recv = new Simulator.Event() {
            public void fire() {
                r.beginReceive(2.4);
            }
        };

        c.insertEvent(send, 1000);
        c.insertEvent(send, 45500);
        c.insertEvent(send, 65500);
        c.insertEvent(send, 106000);

        c.insertEvent(recv, 100);

        for ( int i = 0; i < 1000000; i++ ) c.advance(1);
    }

    class TestTransmitter extends Medium.Transmitter {
        int counter;

        TestTransmitter(Medium m, Clock c) {
            super(m, c);
        }
        public byte nextByte() {
            byte val = (byte) (counter++);
            if ( counter % 5 == 0 ) endTransmit();
            return val;
        }
    }

    class TestReceiver extends Medium.Receiver {
        TestReceiver(Medium m, Clock c) {
            super(m, c);
        }
        public byte nextByte(boolean lock, byte b) {
            if (lock)
                Terminal.println(clock.getCount()+" "+StringUtil.toMultirepString(b, 8));
            else
                Terminal.println(clock.getCount()+" lock end");
            return b;
        }
                 public void setRssiValid (boolean v){
             //do nothing
         }
          public boolean getRssiValid (){
              //do nothing
              return false;
         }
          public void setRSSI (double PRec){
              //do nothing              
          }
          public void setBER (double BER){             
            //do nothing
        }
        
        
    }
}

