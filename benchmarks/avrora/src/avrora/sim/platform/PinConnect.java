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

package avrora.sim.platform;

import avrora.sim.Simulator;
import avrora.sim.SimulatorThread;
import avrora.sim.clock.StepSynchronizer;
import avrora.sim.clock.Synchronizer;
import avrora.sim.mcu.Microcontroller;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Very simple implementation of pin interconnect between microcontrollers
 *
 * @author Jacob Everist
 */

public class PinConnect {

    public static final PinConnect pinConnect;

    static {
        pinConnect = new PinConnect();
    }

    private final PinEvent pinEvent;
    public final Synchronizer synchronizer;

    // List of all the pin relationships
    protected LinkedList pinNodes;
    protected LinkedList pinConnections;
    
    // number of nodes
    protected int numNodes;

    // SERES link directions
    public static final int NONE = -1;
    public static final int NORTH = 0;
    public static final int EAST = 1;
    public static final int SOUTH = 2;
    public static final int WEST = 3;
    
    // Superbot link directions
    public static final int LED1 = 0;
    public static final int LED2 = 1;
    public static final int LED3 = 2;
    public static final int LED4 = 3;
    public static final int LED5 = 4;
    public static final int LED6 = 5;
    
    public PinConnect() {
        // period = 1
        pinNodes = new LinkedList();
        pinConnections = new LinkedList();
        pinEvent = new PinEvent();
        synchronizer = new StepSynchronizer(pinEvent);
        numNodes = 0;
    }

    public void addSeresNode(Microcontroller mcu, PinWire northTx, PinWire eastTx,
                             PinWire southTx, PinWire westTx, PinWire northRx, PinWire eastRx,
                             PinWire southRx, PinWire westRx, PinWire northInt, PinWire eastInt,
                             PinWire southInt, PinWire westInt) {

        pinNodes.add(new PinNode(mcu, northTx, eastTx, southTx, westTx, northRx, eastRx, southRx, westRx,
                northInt, eastInt, southInt, westInt, numNodes));
        numNodes++;
    }
    
    public void addSuperbotNode(Microcontroller mcu, PinWire LED1Tx, PinWire LED2Tx, PinWire LED3Tx, PinWire LED4Tx,
    		PinWire LED5Tx, PinWire LED6Tx, PinWire LED1Rx, PinWire LED2Rx, PinWire LED3Rx, PinWire LED4Rx,
			PinWire LED5Rx, PinWire LED6Rx, PinWire LED1Int, PinWire LED2Int, PinWire LED3Int,
			PinWire LED4Int, PinWire LED5Int, PinWire LED6Int ) {

    	pinNodes.add(new PinNode(mcu, LED1Tx, LED2Tx, LED3Tx, LED4Tx, LED5Tx, LED6Tx, LED1Rx, LED2Rx, LED3Rx,
    			LED4Rx, LED5Rx, LED6Rx, LED1Int, LED2Int, LED3Int, LED4Int, LED5Int, LED6Int, numNodes));
    	numNodes++;
    }
    
    public void addSimulatorThread(SimulatorThread simThread) {
        Simulator sim = simThread.getSimulator();
        Microcontroller currMCU = sim.getMicrocontroller();

        // iterator over PinNodes
        Iterator i = pinNodes.iterator();

        // go through the complete list of PinNodes
        while (i.hasNext()) {

            // get the next PinNode on the list
            PinNode p = (PinNode) i.next();

            // does this node have the equivalent Microcontroller?
            if (currMCU == p.mcu) {

                // register the simulator thread with the appropriate PinNode
                //p.addSimulatorThread(simThread);

                // add simulator thread to PinClock and PinMeet
                //synchronizer.addNode(simThread.getNode());
            }
        }
    }

    /**
     * Initialize the connections with a default topology of
     * a chain with connections on the north and south ports
     */
    public void initializeConnections() {

        // iterator over PinNodes
        Iterator i = pinNodes.iterator();

        if (!i.hasNext()) {
            return;
        }

        // the previous PinNode
        PinNode prevNode = (PinNode) i.next();

        // connect the nodes from North to South to create a long chain
        while (i.hasNext()) {
            // get next node on the list
            PinNode currNode = (PinNode) i.next();
            // two-way communication links between neighboring modules
            
            // two-way communication links between neighboring modules
            if ("SERES".equalsIgnoreCase(prevNode.platform)) {
            	prevNode.connectNodes(currNode, SOUTH, SOUTH);
            	prevNode.connectNodes(currNode, NORTH, NORTH);
            	prevNode.connectNodes(currNode, EAST, EAST);
            	prevNode.connectNodes(currNode, WEST, WEST);
            	//prevNode.connectNodes(prevNode, SOUTH, SOUTH);
            	//prevNode.connectNodes(prevNode, NORTH, NORTH);
            	//prevNode.connectNodes(prevNode, EAST, EAST);
            	//prevNode.connectNodes(prevNode, WEST, WEST);
            }
            else if ("Superbot".equalsIgnoreCase(prevNode.platform) ) {
            	prevNode.connectNodes(currNode, LED1, LED1);
            	prevNode.connectNodes(currNode, LED2, LED2);
            	prevNode.connectNodes(currNode, LED3, LED3);
            	prevNode.connectNodes(currNode, LED4, LED4);
            	prevNode.connectNodes(currNode, LED5, LED5);
            	prevNode.connectNodes(currNode, LED6, LED6);
            }
            else {
            	System.out.println("Unrecognized platform " + prevNode.platform );
            }
            // set this node as previous node
            prevNode = currNode;

        }

    }

    /**
     * This class stores all the information for a single controller node
     * and its PinWires.
     *
     * @author Jacob Everist
     */
    protected class PinNode {

        // node microcontroller
        public Microcontroller mcu;

        // transmit pins
        protected PinWire[] TxPins;

        // receive pins
        protected PinWire[] RxPins;

        // interrupt pins
        protected PinWire[] IntPins;

        // simulator thread
        //public SimulatorThread simThread;

        // node id and neighbors
    	private int localNode;
    	public PinNode[] neighborNodes;
    	
    	// side we are connected to neighbors
    	public int[] neighborSides;
    	
    	// platform ID
    	public final String platform;

    	public PinNode(Microcontroller mcu, PinWire northTx, PinWire eastTx,
                           PinWire southTx, PinWire westTx, PinWire northRx, PinWire eastRx,
                           PinWire southRx, PinWire westRx, PinWire northInt, PinWire eastInt,
                           PinWire southInt, PinWire westInt, int node) {

            this.mcu = mcu;
            
            TxPins = new PinWire[]{northTx,eastTx,southTx,westTx};
            RxPins = new PinWire[]{northRx,eastRx,southRx,westRx};
            IntPins = new PinWire[]{northInt,eastInt,southInt,westInt};
            
            
            neighborNodes = new PinNode[]{null,null,null,null};
            neighborSides = new int[]{NONE,NONE,NONE,NONE};
            localNode = node;

            platform = "SERES";

        }
        
        public PinNode(Microcontroller mcu, PinWire LED1Tx, PinWire LED2Tx, PinWire LED3Tx, PinWire LED4Tx,
        		PinWire LED5Tx, PinWire LED6Tx, PinWire LED1Rx, PinWire LED2Rx, PinWire LED3Rx, PinWire LED4Rx,
				PinWire LED5Rx, PinWire LED6Rx, PinWire LED1Int, PinWire LED2Int, PinWire LED3Int,
				PinWire LED4Int, PinWire LED5Int, PinWire LED6Int, int node) {

        	this.mcu = mcu;
     
        	TxPins = new PinWire[]{LED1Tx, LED2Tx, LED3Tx, LED4Tx, LED5Tx, LED6Tx};
        	RxPins = new PinWire[]{LED1Rx, LED2Rx, LED3Rx, LED4Rx, LED5Rx, LED6Rx};
        	IntPins = new PinWire[]{LED1Int, LED2Int, LED3Int, LED4Int, LED5Int, LED6Int};
     
     
        	neighborNodes = new PinNode[]{null,null,null,null,null,null};
        	neighborSides = new int[]{NONE,NONE,NONE,NONE,NONE,NONE};
        	localNode = node;

        	platform = "Superbot";
        }
        
        public void connectNodes(PinNode neighbor, int localSide, int neighborSide) {
        	
        	// check to see that either side has not previously been connected
        	if ( neighborNodes[localSide] != null || neighbor.neighborNodes[neighborSide] != null )
        		return;
        	
        	// set the nodes as neighbors
        	neighborNodes[localSide] = neighbor;
        	neighborSides[localSide] = neighborSide;
        	neighbor.neighborNodes[neighborSide] = this;
        	neighbor.neighborSides[neighborSide] = localSide;
        	
        	// connect the nodes on the appropriate sides
        	
            // output pin for tne local module
            PinLink localToNeighbor = new PinLink(TxPins[localSide]);
            localToNeighbor.outputNode = this;
            localToNeighbor.outputSide = localSide;
            localToNeighbor.inputNode = neighbor;
            localToNeighbor.inputSide = neighborSide;

            // input pins for the neighbor module
            localToNeighbor.addInputPin(neighbor.RxPins[neighborSide]);
            localToNeighbor.addInputPin(neighbor.IntPins[neighborSide]);

            // output pin for the neighbor module
            PinLink neighborToLocal = new PinLink(neighbor.TxPins[neighborSide]);
            neighborToLocal.outputNode = neighbor;
            neighborToLocal.outputSide = neighborSide;
            neighborToLocal.inputNode = this;
            neighborToLocal.inputSide = localSide;

            // input pins for the local module
            neighborToLocal.addInputPin(RxPins[localSide]);
            neighborToLocal.addInputPin(IntPins[localSide]);

            // add connections to the list
            pinConnections.add(localToNeighbor);
            pinConnections.add(neighborToLocal);
        }

        public void disconnectNodes(PinNode neighbor, int localSide, int neighborSide) {
        	
        	// set the nodes as neighbors
        	neighborNodes[localSide] = null;
        	neighborSides[localSide] = NONE;
        	neighbor.neighborNodes[neighborSide] = null;
        	neighbor.neighborSides[neighborSide] = NONE;
        	
        	// disconnect the nodes on the appropriate sides
        	Iterator i = pinConnections.iterator();
        	
        	// find the local to neighbor connection
        	while ( i.hasNext() ) {
        	
        		PinLink curr = (PinLink) i.next();
        	
        		// if this is the correct link, delete it
        		if ( curr.outputNode == this && curr.outputSide == localSide 
        				&& curr.inputNode == neighbor && curr.inputSide == neighborSide ) {
        			pinConnections.remove(curr);
        		}
        	}
        	
        	// reset iterator
        	i = pinConnections.iterator();
        	
        	// find the neighbor to local connection
        	while ( i.hasNext() ) {
            	
            		PinLink curr = (PinLink) i.next();
            	
            		// if this is the correct link, delete it
            		if ( curr.outputNode == neighbor && curr.outputSide == neighborSide 
            				&& curr.inputNode == this && curr.inputSide == localSide ) {
            			pinConnections.remove(curr);
            		}
            	}
        }
        /*
        public void addSimulatorThread(SimulatorThread simThread) {
            this.simThread = simThread;
        }
        */
    }

    /**
     * This class connects two PinNode devices together in two-way communication
     *
     * @author Jacob Everist
     */
    protected class PinLink {

        protected LinkedList pinWires;
        protected int currentDelay;
        
        public PinNode outputNode;
        public int outputSide;
        
        public PinNode inputNode;
        public int inputSide;

        // must start PinLink with an output pin
        public PinLink(PinWire outputPin) {

            pinWires = new LinkedList();

            // make sure it is set as output
            outputPin.wireOutput.enableOutput();

            // add to list of pins on this connection
            pinWires.add(outputPin);

        }

        // add an input pin on this connection
        public void addInputPin(PinWire inputPin) {

            // make sure it is set as input
            inputPin.wireInput.enableInput();

            // add to list of pins on this connection
            pinWires.add(inputPin);

        }

        // transmit the signals on this connection
        public void propagateSignals() {

            // iterator over PinWires
            Iterator i = pinWires.iterator();

            PinWire currOutput = null;

            // go through the complete list of PinWires to find the output wire
            while (i.hasNext()) {

                PinWire curr = (PinWire) i.next();

                // if this wire accepts output
                if (curr.outputReady()) {

                    // check that we haven't already found an output wire
                    if (currOutput != null) {
                        String s = "ERROR: More than one output wire on this PinLink";
                        System.out.println(s);
                        return;
                    } else {
                        // set this pin as the output wire
                        currOutput = curr;
                    }

                }
            }

            // check if we have an output wire
            if (currOutput == null) {
                // there is no output wire, so do nothing
            }
            // if we have an output wire, propagate its signal
            else {

                // reset the iterator
                i = pinWires.iterator();

                // go through all wires
                while (i.hasNext()) {

                    PinWire curr = (PinWire) i.next();

                    // if this is not the output, propagate the signal
                    if (curr != currOutput) {
                        // write the value of output pin to the input pins
                        curr.wireOutput.write(currOutput.wireInput.read());
                        //System.out.println("Writing " + currOutput.wireInput.read()
                        //		+ " from " + currOutput.readName() + " to "
                        //		+ curr.readName());
                    }
                }
            }
        }
    }

    protected class PinEvent implements Simulator.Event {
        public void fire() {
            // iterator over PinLinks
            Iterator i = pinConnections.iterator();
            
            PinLink currLink = (PinConnect.PinLink) i.next();
            currLink.propagateSignals();
            
            while (i.hasNext()) {
                currLink = (PinConnect.PinLink) i.next();
                currLink.propagateSignals();
            }
        }
    }
    

	/**
	 * @return Returns the pinNodes.
	 */
	public LinkedList getPinNodes() {
		return pinNodes;
	}

}
