/*
 * Copyright 1997-2006 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */


/*
 * The Original Code is HAT. The Initial Developer of the
 * Original Code is Bill Foote, with contributions from others
 * at JavaSoft/Sun.
 */

package app.com.detectionapp.BackgroundService.ahat.parser;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import app.com.detectionapp.BackgroundService.ahat.model.Snapshot;

/**
 * Abstract base class for reading object dump files.  A reader need not be thread-safe.
 *
 * @author Bill Foote
 */

public abstract class Reader {
    protected PositionDataInputStream in;

    protected Reader(PositionDataInputStream in) {
        this.in = in;
    }

    /**
     * Read a snapshot from a data input stream.  It is assumed that the magic number has already been read.
     */
    abstract public Snapshot read() throws IOException;

    /**
     * Read a snapshot from a file.
     *
     * @param heapFile  The name of a file containing a heap dump
     * @param callStack If true, read the call stack of allocation sites
     */
    public static Snapshot readFile(String heapFile, boolean callStack,
                                    int debugLevel)
            throws IOException {

        //Ĭ�������callStack=true, debugLevel=0

        int dumpNumber = 1;
        int pos = heapFile.lastIndexOf('#');
        if (pos > -1) {
            String num = heapFile.substring(pos + 1, heapFile.length());
            try {
                dumpNumber = Integer.parseInt(num, 10);
            } catch (NumberFormatException ex) {
                String msg = "In file name \"" + heapFile
                        + "\", a dump number was "
                        + "expected after the :, but \""
                        + num + "\" was found instead.";
                System.err.println(msg);
                throw new IOException(msg);
            }
            heapFile = heapFile.substring(0, pos);
        }
        PositionDataInputStream in = new PositionDataInputStream(
                new BufferedInputStream(new FileInputStream(heapFile)));
        try {
            int i = in.readInt();
            if (i == HprofReader.MAGIC_NUMBER) {

                Reader r
                        = new HprofReader(heapFile, in, dumpNumber,
                        callStack, debugLevel);
                Snapshot result = r.read();
                r = null;
                return result;
            } else {
                throw new IOException("Unrecognized magic number: " + i);
            }
        } finally {
            in.close();
        }
    }
}
