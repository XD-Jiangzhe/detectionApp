/*
 * Copyright 1997-2005 Sun Microsystems, Inc.  All Rights Reserved.
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

package app.com.detectionapp.DumpHeapService.ahat.model;

/**
 * Represents a stack trace, that is, an ordered collection of stack frames.
 */

public class StackTrace {

    private StackFrame[] frames;

    public StackTrace(StackFrame[] frames) {
        this.frames = frames;
    }

    public StackTrace traceForDepth(int depth) {
        if (depth >= frames.length) {
            return this;
        } else {
            StackFrame[] f = new StackFrame[depth];
            System.arraycopy(frames, 0, f, 0, depth);
            return new StackTrace(f);
        }
    }

    public void resolve(Snapshot snapshot) {
        for (StackFrame frame : frames) {
            frame.resolve(snapshot);
        }
    }

    StackFrame[] getFrames() {
        return frames;
    }
}
