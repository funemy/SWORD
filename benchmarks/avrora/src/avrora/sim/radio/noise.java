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
 */
package avrora.sim.radio;

import cck.util.Util;
import java.io.*;
import java.util.*;

/**
 *
 * @author Rodolfo de Paz
 * it handles noise file
 */
public class noise {

    private static List noise = new ArrayList();

    /** Creates a new instance of Noise from file */
    public noise(String fileName) throws IOException {
        parse(new BufferedReader(new FileReader(fileName)));
    }

    /** Creates a new instance of Noise when there isn't noise file */
    public noise() {
        noise.add(new Integer(-95));
    }

    private void parse(BufferedReader f) throws IOException {
        String line;
        while ((line = f.readLine()) != null) {
            parseLine(line);
        }
        f.close();
    }

   /**
     * parse one line of the file
     *
     * @param line
     */
    private void parseLine(String line) {
        int noise_value = 0;
        //check for comment
        if (!line.startsWith("#")) {
            StringTokenizer tokenizer = new StringTokenizer(line, " ");
            int count = 0;
            while (tokenizer.hasMoreTokens()) {
                try {
                    if (count == 0){
                        noise_value = Integer.parseInt(tokenizer.nextToken());
                    }
                    count++;
                } catch (NoSuchElementException e) {
                    throw Util.failure("Error reading Noise file");
                }
            }
            if (count == 1) {
                //parsing of this line went well -> found 1 token
                noise.add(new Integer(noise_value));
            }
        }
    }

    public static int getNoise(int index) {
        return ((Integer)noise.get(index)).intValue();
    }

    public static int sizeNoise() {
        return noise.size();
    }
}

