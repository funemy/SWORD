/*
 * "Copyright (c) 2009 Cork Institute of Technology, Ireland
 * All rights reserved."
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose, without fee, and without written agreement is
 * hereby granted, provided that the above copyright notice, the following
 * two paragraphs and the author appear in all copies of this software.
 *
 * IN NO EVENT SHALL THE CORK INSTITUTE OF TECHNOLOGY BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE CORK INSTITUTE
 * OF TECHNOLOGY HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * THE CORK INSTITUTE OF TECHNOLOGY SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS."
 *
 * @author Rodolfo de Paz Alberola
 */

package avrora.sim.state;

public class Complex{

        private double real = 0.0D;         // Real part of a complex number
        private double imag = 0.0D;         // Imaginary part of a complex number
        
        // constructor - initialises both real and imag
        public Complex(double real, double imag)
        {
                this.real = real;
                this.imag = imag;
        }       
       // Absolute value (modulus) of a complex number 
        public static double abs(Complex a){
                double rmod = Math.abs(a.real);
                double imod = Math.abs(a.imag);
                double ratio = 0.0D;
                double res = 0.0D;

                if(rmod==0.0D){
                res=imod;
                }
                else{
                if(imod==0.0D){
                        res=rmod;
                }
                        if(rmod>=imod){
                                ratio=a.imag/a.real;
                                res=rmod*Math.sqrt(1.0D + ratio*ratio);
                        }
                        else{
                                ratio=a.real/a.imag;
                                res=imod*Math.sqrt(1.0D + ratio*ratio);
                        }
                }
                return res;
        }
}
