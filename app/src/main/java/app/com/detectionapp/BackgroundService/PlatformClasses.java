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

package app.com.detectionapp.BackgroundService;

import android.content.Context;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import app.com.detectionapp.BackgroundService.ahat.model.JavaClass;
/**
 * This class is a helper that determines if a class is a "platform" class or not.  It's a platform class if its name starts with one of the
 * prefixes to be found in /com/sun/tools/hat/resources/platform_names_big.txt.
 *
 * @author Bill Foote
 */
public class PlatformClasses {

    static String[] names = null;

    final static String PLATFORM_CLASS_SMALL = "platform_names_small.txt";
    final static String PLATFORM_CLASS_BIG = "platform_names_big.txt";

    public static synchronized String[] getNames(Context context, Boolean Size) {
        if (names == null) {
            LinkedList<String> list = new LinkedList<>();

            try {
                InputStream str = null;
                if (Size) {
                    str = context.getResources().getAssets().open(PLATFORM_CLASS_SMALL);
                } else {
                    str = context.getResources().getAssets().open(PLATFORM_CLASS_BIG);
                }
                if (str != null) {
                    try {
                        BufferedReader rdr = new BufferedReader(new InputStreamReader(str));
                        for (; ; ) {
                            String s = rdr.readLine();
                            if (s == null) {
                                break;
                            } else if (s.length() > 0) {
                                list.add(s);
                            }
                        }
                        rdr.close();
                        str.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        // Shouldn't happen, and if it does, continuing
                        // is the right thing to do anyway.
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            names = list.toArray(new String[0]);
        }
        return names;
    }

    public static boolean isPlatformClass(JavaClass clazz, Context contex, Boolean Size) {
        // all classes loaded by bootstrap loader are considered
        //        // platform classes. In addition, the older name based filtering
        //        // is also done for compatibility.
        if (clazz.isBootstrap()) {
            return true;
        }

        String name = clazz.getName();
        // skip even the array classes of the skipped classes.
        if (name.startsWith("[")) {
            int index = name.lastIndexOf('[');
            if (index != -1) {
                if (name.charAt(index + 1) != 'L') {
                    // some primitive array.
                    return true;
                }
                // skip upto 'L' after the last '['.
                name = name.substring(index + 2);
            }
        }
        String[] nms = getNames(contex, Size);
        for (String nm : nms) {
            if (name.startsWith(nm)) {
                return true;
            }
        }
        return false;
    }
}
