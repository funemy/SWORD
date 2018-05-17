/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.geronimo.daytrader.javaee6.utils;


/**
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class MDBStats extends java.util.HashMap {
    
    
    //Singleton class
    private static MDBStats mdbStats = null;
    private MDBStats()
    {
    }
    
    public static synchronized MDBStats getInstance()
    {
        if (mdbStats == null)
            mdbStats = new MDBStats();
        return mdbStats;
    }
    
    public TimerStat addTiming(String type, long sendTime, long recvTime)
    {
        TimerStat stats = null;
        synchronized (type)
        {

            stats = (TimerStat) get(type);
            if (stats == null) stats = new TimerStat();

            long time =  recvTime - sendTime;                                
            if ( time > stats.getMax() ) stats.setMax(time);
            if ( time < stats.getMin() ) stats.setMin(time);
            stats.setCount(stats.getCount()+1);
            stats.setTotalTime(stats.getTotalTime() + time);
            
            put(type, stats);
        }
        return stats;
    }



    public synchronized void reset()
    {
        clear();
    }    
    


}
