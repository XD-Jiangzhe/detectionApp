package app.com.detectionapp.BackgroundService;

import android.content.Context;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import app.com.detectionapp.BackgroundService.ahat.model.AbstractJavaHeapObjectVisitor;
import app.com.detectionapp.BackgroundService.ahat.model.JavaClass;
import app.com.detectionapp.BackgroundService.ahat.model.JavaHeapObject;
import app.com.detectionapp.BackgroundService.ahat.model.Snapshot;


public class QueryClassInfo {
    private Snapshot shot = null;
    private Context context = null;
    private Set<String> platformClass = new HashSet<>();
    String fileName = null;
    final String SUFFIX = ".txt";

    boolean excludePlatform = true;

    PrintWriter out = null;
    PrintWriter outClass = null;

    public QueryClassInfo(Snapshot _shot, Context _context) {
        this.shot = _shot;
        context = _context;
    }

    public void process(String hprofFilePath, String platformClassFile) {
        fileName = hprofFilePath + SUFFIX;

        File outfile = new File(fileName);
        File outPlatformClassfile = new File(platformClassFile);
        try {
            if (outfile.exists()) {
                outfile.delete();
            }
            outfile.createNewFile();
            outPlatformClassfile.createNewFile();
            out = new PrintWriter(new FileWriter(outfile));
            outClass = new PrintWriter(new FileWriter(outPlatformClassfile));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        Iterator classes = shot.getClasses();
        out.println(shot.getClassesNum());
        String lastPackage = null;

        while (classes.hasNext()) {
            JavaClass clazz = (JavaClass) classes.next();
            if (excludePlatform && PlatformClasses.isPlatformClass(clazz, context, false)) {
                String text = clazz.getName();
                int index = text.indexOf("$");
                if (index == -1) {
                    platformClass.add(text);
                } else {
                    platformClass.add(text.substring(0, index));
                }
            }
            if (PlatformClasses.isPlatformClass(clazz, context, true)) {
                continue;
            }
            String name = clazz.getName();
            int pos = name.lastIndexOf(".");
            String pkg;
            if (name.startsWith("[")) { // Only in ancient heap dumps
                pkg = "<Arrays>";
            } else if (pos == -1) {
                pkg = "<Default Package>";
            } else {
                pkg = name.substring(0, pos);
            }

            if (!pkg.equals(lastPackage)) {
                out.println();
                out.println();
                out.print("Package ");
                out.println(pkg);
            }
            lastPackage = pkg;

            out.print("\t" + clazz.toString());
            if (clazz.getId() != -1) {
                out.print(" [" + clazz.getIdString() + "]");
            }
            out.println();

            Map<JavaClass, Long> referrersStat = new HashMap<JavaClass, Long>();
            final Map<JavaClass, Long> refereesStat = new HashMap<JavaClass, Long>();
            Enumeration instances = clazz.getInstances(false);
            while (instances.hasMoreElements()) {
                JavaHeapObject instance = (JavaHeapObject) instances.nextElement();
                if (instance.getId() == -1) {
                    continue;
                }
                Enumeration e = instance.getReferers();
                while (e.hasMoreElements()) {
                    JavaHeapObject ref = (JavaHeapObject) e.nextElement();
                    JavaClass cl = ref.getClazz();
                    if (cl == null) {
                        System.out.println("null class for " + ref);
                        continue;
                    }
                    Long count = referrersStat.get(cl);
                    if (count == null) {
                        count = Long.valueOf(1);
                    } else {
                        count = Long.valueOf(count.longValue() + 1);
                    }
                    referrersStat.put(cl, count);
                }
                instance.visitReferencedObjects(new AbstractJavaHeapObjectVisitor() {
                    public void visit(JavaHeapObject obj) {
                        JavaClass cl = obj.getClazz();
                        Long count = refereesStat.get(cl);
                        if (count == null) {
                            count = Long.valueOf(1);
                        } else {
                            count = Long.valueOf(count.longValue() + 1);
                        }
                        refereesStat.put(cl, count);
                    }
                });
            } // for each instance

            // Referers by Type
            if (referrersStat.size() != 0) {
                out.println("\t\tReferrers by Type");
                print(referrersStat);
            }

            // Referees by Type
            if (refereesStat.size() != 0) {
                out.println("\t\tReferees by Type");
                print(refereesStat);
            }
            out.println();
        }
        for (String text : platformClass) {
            outClass.println(text);
        }
        out.flush();
        out.close();
        outClass.close();
    }

    private void print(final Map<JavaClass, Long> map) {
        Set<JavaClass> keys = map.keySet();
        JavaClass[] classes = new JavaClass[keys.size()];
        keys.toArray(classes);
        Arrays.sort(classes, new Comparator<JavaClass>() {
            public int compare(JavaClass first, JavaClass second) {
                Long count1 = map.get(first);
                Long count2 = map.get(second);
                return count2.compareTo(count1);
            }
        });
        for (int i = 0; i < classes.length; i++) {
            JavaClass clazz = classes[i];
            out.print("\t\t\t" + clazz.getName() + " ");
            out.println(map.get(clazz));
        }
    }
}