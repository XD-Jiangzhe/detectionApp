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

import java.util.Enumeration;
import java.util.Hashtable;
import app.com.detectionapp.DumpHeapService.ahat.util.ArraySorter;
import app.com.detectionapp.DumpHeapService.ahat.util.Comparer;

public class ReachableObjects {
    public ReachableObjects(JavaHeapObject root,
                            final ReachableExcludes excludes) {
        this.root = root;

        final Hashtable<JavaHeapObject, JavaHeapObject> bag = new Hashtable<>();
        final Hashtable<String, String> fieldsExcluded = new Hashtable<>(); //Bag<String>
        final Hashtable<String, String> fieldsUsed = new Hashtable<>(); // Bag<String>
        JavaHeapObjectVisitor visitor = new AbstractJavaHeapObjectVisitor() {
            public void visit(JavaHeapObject t) {
                if (t != null && t.getSize() > 0 && bag.get(t) == null) {
                    bag.put(t, t);
                    t.visitReferencedObjects(this);
                }
            }

            public boolean mightExclude() {
                return excludes != null;
            }

            public boolean exclude(JavaClass clazz, JavaField f) {
                if (excludes == null) {
                    return false;
                }
                String nm = clazz.getName() + "." + f.getName();
                if (excludes.isExcluded(nm)) {
                    fieldsExcluded.put(nm, nm);
                    return true;
                } else {
                    fieldsUsed.put(nm, nm);
                    return false;
                }
            }
        };

        /* Put the closure of root and all objects reachable from root into
         bag (depth first), but don't include root:*/
        visitor.visit(root);
        bag.remove(root);

        /* Now grab the elements into a vector, and sort it in decreasing size*/
        JavaThing[] things = new JavaThing[bag.size()];
        int i = 0;
        for (Enumeration e = bag.elements(); e.hasMoreElements(); ) {
            things[i++] = (JavaThing) e.nextElement();
        }
        ArraySorter.sort(things, new Comparer() {
            public int compare(Object lhs, Object rhs) {
                JavaThing left = (JavaThing) lhs;
                JavaThing right = (JavaThing) rhs;
                int diff = right.getSize() - left.getSize();
                if (diff != 0) {
                    return diff;
                }
                return left.compareTo(right);
            }
        });
        this.reachables = things;

        this.totalSize = root.getSize();
        for (i = 0; i < things.length; i++) {
            this.totalSize += things[i].getSize();
        }

        excludedFields = getElements(fieldsExcluded);
        usedFields = getElements(fieldsUsed);
    }

    public JavaHeapObject getRoot() {
        return root;
    }

    public JavaThing[] getReachables() {
        return reachables;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public String[] getExcludedFields() {
        return excludedFields;
    }

    public String[] getUsedFields() {
        return usedFields;
    }

    private String[] getElements(Hashtable ht) {
        Object[] keys = ht.keySet().toArray();
        int len = keys.length;
        String[] res = new String[len];
        System.arraycopy(keys, 0, res, 0, len);
        ArraySorter.sortArrayOfStrings(res);
        return res;
    }

    private JavaHeapObject root;
    private JavaThing[] reachables;
    private String[] excludedFields;
    private String[] usedFields;
    private long totalSize;
}
