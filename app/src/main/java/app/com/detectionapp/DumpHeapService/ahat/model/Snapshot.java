/*

 */

package app.com.detectionapp.DumpHeapService.ahat.model;

import java.lang.ref.SoftReference;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;
import app.com.detectionapp.DumpHeapService.ahat.parser.ReadBuffer;
import app.com.detectionapp.DumpHeapService.ahat.util.Misc;


/**
 * @ Author      ：Huanran Wang
 * @ E-mail      : wanghuanran@hit.edu.cn
 * @ Date        ：Created in 15:30 2018.8.9
 * @ Description ：Represents a snapshot of the Java objects in the VM at one instant. This is the top-level "model" object read out of a
 * single .hprof or .bod file. The Original Code is HAT. The Initial Developer of the Original Code is Bill
 * Foote, with contributions from others at JavaSoft/Sun.
 */

public class Snapshot {

    public static long SMALL_ID_MASK = 0x0FFFFFFFFL; // ID掩码

    static final byte[] EMPTY_BYTE_ARRAY = new byte[0]; // 空字节数组

    private static final JavaField[] EMPTY_FIELD_ARRAY = new JavaField[0]; // 空域数组

    private static final JavaStatic[] EMPTY_STATIC_ARRAY = new JavaStatic[0]; // 空静态数组

    private Hashtable<Number, JavaHeapObject> heapObjects = new Hashtable<>(); // all heap objects

    private Hashtable<Number, JavaClass> fakeClasses = new Hashtable<>();

    private Vector<Root> roots = new Vector<>();    // all Roots in this Snapshot

    private Map<String, JavaClass> classes = new TreeMap<>();    // name-to-class map

    /* new objects relative to a baseline - lazily initialized */
    private volatile Map<JavaHeapObject, Boolean> newObjects;

    /* allocation site traces for all objects - lazily initialized*/
    private volatile Map<JavaHeapObject, StackTrace> siteTraces;

    /* object-to-Root map for all objects*/
    private Map<JavaHeapObject, Root> rootsMap = new HashMap<>();

    /* soft cache of finalizeable objects - lazily initialized*/
    private SoftReference<Vector> finalizablesCache;

    /* represents null reference*/
    private JavaThing nullThing;

    private JavaClass weakReferenceClass; // java.lang.ref.Reference class

    private int referentFieldIndex; // index of 'referent' field in java.lang.ref.Reference class

    private JavaClass javaLangClass; // java.lang.Class class

    private JavaClass javaLangString; // java.lang.String class

    private JavaClass javaLangClassLoader; // java.lang.ClassLoader class

    private volatile JavaClass otherArrayType; // unknown "other" array class

    private ReachableExcludes reachableExcludes; // Stuff to exclude from reachable query

    private ReadBuffer readBuf; // the underlying heap dump buffer

    /* True iff some heap objects have isNew set*/
    private boolean hasNewSet;
    private boolean unresolvedObjectsOK;

    /* whether object array instances have new style class or
    old style (element) class.*/
    private boolean newStyleArrayClass;

    /* object id size in the heap dump*/
    private int identifierSize = 4;

    /* minimum object size - accounts for object header in
     most Java virtual machines - we assume 2 identifierSize
     (which is true for Sun's hotspot JVM).*/
    private int minimumObjectSize;

    public Snapshot(ReadBuffer buf) {
        nullThing = new HackJavaValue("<null>", 0);
        readBuf = buf;
    }

    public void setSiteTrace(JavaHeapObject obj, StackTrace trace) {
        if (trace != null && trace.getFrames().length != 0) {
            initSiteTraces();
            siteTraces.put(obj, trace);
        }
    }

    StackTrace getSiteTrace(JavaHeapObject obj) {
        if (siteTraces != null) {
            return siteTraces.get(obj);
        } else {
            return null;
        }
    }

    public void setNewStyleArrayClass(boolean value) {
        newStyleArrayClass = value;
    }

    boolean isNewStyleArrayClass() {
        return newStyleArrayClass;
    }

    public void setIdentifierSize(int size) {
        identifierSize = size;
        minimumObjectSize = 2 * size;
    }

    int getIdentifierSize() {
        return identifierSize;
    }

    int getMinimumObjectSize() {
        return minimumObjectSize;
    }

    public void addHeapObject(long id, JavaHeapObject ho) {
        heapObjects.put(makeId(id), ho);
    }

    public void addRoot(Root r) {
        r.setIndex(roots.size());
        roots.addElement(r);
    }

    public void addClass(long id, JavaClass c) {
        addHeapObject(id, c);
        putInClassesMap(c);
    }

    JavaClass addFakeInstanceClass(long classID, int instSize) {
        // Create a fake class name based on ID.
        String name = "unknown-class<@" + Misc.toHex(classID) + ">";

        // Create fake fields convering the given instance size.
        // Create as many as int type fields and for the left over
        // size create byte type fields.
        int numInts = instSize / 4;
        int numBytes = instSize % 4;
        JavaField[] fields = new JavaField[numInts + numBytes];
        int i;
        for (i = 0; i < numInts; i++) {
            fields[i] = new JavaField("unknown-field-" + i, "I");
        }
        for (i = 0; i < numBytes; i++) {
            fields[i + numInts] = new JavaField("unknown-field-" + i + numInts, "B");
        }

        // Create fake instance class
        JavaClass c = new JavaClass(name, 0, 0, 0, 0, fields, EMPTY_STATIC_ARRAY, instSize);
        // Add the class
        addFakeClass(makeId(classID), c);
        return c;
    }

    public boolean getHasNewSet() {
        return hasNewSet;
    }

    //
    // Used in the body of resolve()
    //
    private static class MyVisitor extends AbstractJavaHeapObjectVisitor {
        JavaHeapObject t;

        public void visit(JavaHeapObject other) {
            other.addReferenceFrom(t);
        }
    }

    // To show heap parsing progress, we print a '.' after this limit
    private static final int DOT_LIMIT = 5000;

    /**
     * Called after reading complete, to initialize the structure
     */
    public void resolve(boolean calculateRefs) {
        System.out.println("Resolving " + heapObjects.size() + " objects...");

        // First, resolve the classes.  All classes must be resolved before
        // we try any objects, because the objects use classes in their
        // resolution.
        javaLangClass = findClass("java.lang.Class");
        if (javaLangClass == null) {
            System.out.println("WARNING:  hprof file does not include java.lang.Class!");
            javaLangClass = new JavaClass("java.lang.Class", 0, 0, 0, 0, EMPTY_FIELD_ARRAY, EMPTY_STATIC_ARRAY, 0);
            addFakeClass(javaLangClass);
        }
        javaLangString = findClass("java.lang.String");
        if (javaLangString == null) {
            System.out.println("WARNING:  hprof file does not include java.lang.String!");
            javaLangString = new JavaClass("java.lang.String", 0, 0, 0, 0, EMPTY_FIELD_ARRAY, EMPTY_STATIC_ARRAY, 0);
            addFakeClass(javaLangString);
        }
        javaLangClassLoader = findClass("java.lang.ClassLoader");
        if (javaLangClassLoader == null) {
            System.out.println("WARNING:  hprof file does not include java.lang.ClassLoader!");
            javaLangClassLoader = new JavaClass("java.lang.ClassLoader", 0, 0, 0, 0, EMPTY_FIELD_ARRAY, EMPTY_STATIC_ARRAY, 0);
            addFakeClass(javaLangClassLoader);
        }

        for (JavaHeapObject t : heapObjects.values()) {
            if (t instanceof JavaClass) {
                t.resolve(this);
            }
        }

        // Now, resolve everything else.
        for (JavaHeapObject t : heapObjects.values()) {
            if (!(t instanceof JavaClass)) {
                t.resolve(this);
            }
        }

        heapObjects.putAll(fakeClasses);
        fakeClasses.clear();

        weakReferenceClass = findClass("java.lang.ref.Reference");
        if (weakReferenceClass == null) {      // JDK 1.1.x
            weakReferenceClass = findClass("sun.misc.Ref");
            referentFieldIndex = 0;
        } else {
            JavaField[] fields = weakReferenceClass.getFieldsForInstance();
            for (int i = 0; i < fields.length; i++) {
                if ("referent".equals(fields[i].getName())) {
                    referentFieldIndex = i;
                    break;
                }
            }
        }

        if (calculateRefs) {
            calculateReferencesToObjects();
            System.out.print("Eliminating duplicate references");
            System.out.flush();
            // This println refers to the *next* step
        }
        int count = 0;
        for (JavaHeapObject t : heapObjects.values()) {
            t.setupReferers();
            ++count;
            if (calculateRefs && count % DOT_LIMIT == 0) {
                System.out.print(".");
                System.out.flush();
            }
        }
        if (calculateRefs) {
            System.out.println();
        }

        // to ensure that Iterator.remove() on getClasses()
        // result will throw exception..
        classes = Collections.unmodifiableMap(classes);
    }

    private void calculateReferencesToObjects() {
        System.out.print("Chasing references, expect " + (heapObjects.size() / DOT_LIMIT) + " dots");
        System.out.flush();
        int count = 0;
        MyVisitor visitor = new MyVisitor();
        for (JavaHeapObject t : heapObjects.values()) {
            visitor.t = t;
            // call addReferenceFrom(t) on all objects t references:
            t.visitReferencedObjects(visitor);
            ++count;
            if (count % DOT_LIMIT == 0) {
                System.out.print(".");
                System.out.flush();
            }
        }
        System.out.println();
        for (Root r : roots) {
            r.resolve(this);
            JavaHeapObject t = findThing(r.getId());
            if (t != null) {
                t.addReferenceFromRoot(r);
            }
        }
    }

    public void markNewRelativeTo(Snapshot baseline) {
        hasNewSet = true;
        for (JavaHeapObject t : heapObjects.values()) {
            boolean isNew;
            long thingID = t.getId();
            if (thingID == 0L || thingID == -1L) {
                isNew = false;
            } else {
                JavaThing other = baseline.findThing(t.getId());
                if (other == null) {
                    isNew = true;
                } else {
                    isNew = !t.isSameTypeAs(other);
                }
            }
            t.setNew(isNew);
        }
    }

    public Enumeration<JavaHeapObject> getThings() {
        return heapObjects.elements();
    }

    JavaHeapObject findThing(long id) {
        Number idObj = makeId(id);
        JavaHeapObject jho = heapObjects.get(idObj);
        return jho != null ? jho : fakeClasses.get(idObj);
    }

    private JavaHeapObject findThing(String id) {
        return findThing(Misc.parseHex(id));
    }

    JavaClass findClass(String name) {
        if (name.startsWith("0x")) {
            return (JavaClass) findThing(name);
        } else {
            return classes.get(name);
        }
    }

    /**
     * Return an Iterator of all of the classes in this snapshot.
     **/
    public Iterator getClasses() {
        // note that because classes is a TreeMap
        // classes are already sorted by name
        return classes.values().iterator();
    }

    public JavaClass[] getClassesArray() {
        JavaClass[] res = new JavaClass[classes.size()];
        classes.values().toArray(res);
        return res;
    }

    /**
     * 获取所有类的个数
     */
    public int getClassesNum() {
        return classes.size();
    }

    public synchronized Enumeration getFinalizerObjects() {
        Vector obj;
        if (finalizablesCache != null && (obj = finalizablesCache.get()) != null) {
            return obj.elements();
        }

        JavaClass clazz = findClass("java.lang.ref.Finalizer");
        JavaObject queue = (JavaObject) clazz.getStaticField("queue");
        JavaThing tmp = queue.getField("head");
        Vector<JavaHeapObject> finalizables = new Vector<>();
        if (tmp != getNullThing()) {
            JavaObject head = (JavaObject) tmp;
            while (true) {
                JavaHeapObject referent = (JavaHeapObject) head.getField("referent");
                JavaThing next = head.getField("next");
                if (next == getNullThing() || next.equals(head)) {
                    break;
                }
                head = (JavaObject) next;
                finalizables.add(referent);
            }
        }
        finalizablesCache = new SoftReference<Vector>(finalizables);
        return finalizables.elements();
    }

    public Enumeration<Root> getRoots() {
        return roots.elements();
    }

    public Root[] getRootsArray() {
        Root[] res = new Root[roots.size()];
        roots.toArray(res);
        return res;
    }

    public Root getRootAt(int i) {
        return roots.elementAt(i);
    }

    public ReferenceChain[] rootsetReferencesTo(JavaHeapObject target, boolean includeWeak) {
        Vector<ReferenceChain> fifo = new Vector<>();  // This is slow... A real fifo would help
        // Must be a fifo to go breadth-first
        Hashtable<JavaHeapObject, JavaHeapObject> visited = new Hashtable<>();
        // Objects are added here right after being added to fifo.
        Vector<ReferenceChain> result = new Vector<>();
        visited.put(target, target);
        fifo.addElement(new ReferenceChain(target, null));

        while (fifo.size() > 0) {
            ReferenceChain chain = fifo.elementAt(0);
            fifo.removeElementAt(0);
            JavaHeapObject curr = chain.getObj();
            if (curr.getRoot() != null) {
                result.addElement(chain);
                // Even though curr is in the rootset, we want to explore its
                // referers, because they might be more interesting.
            }
            Enumeration referers = curr.getReferers();
            while (referers.hasMoreElements()) {
                JavaHeapObject t = (JavaHeapObject) referers.nextElement();
                if (t != null && !visited.containsKey(t)) {
                    if (includeWeak || !t.refersOnlyWeaklyTo(this, curr)) {
                        visited.put(t, t);
                        fifo.addElement(new ReferenceChain(t, chain));
                    }
                }
            }
        }

        ReferenceChain[] realResult = new ReferenceChain[result.size()];
        for (int i = 0; i < result.size(); i++) {
            realResult[i] = result.elementAt(i);
        }
        return realResult;
    }

    boolean getUnresolvedObjectsOK() {
        return unresolvedObjectsOK;
    }

    public void setUnresolvedObjectsOK(boolean v) {
        unresolvedObjectsOK = v;
    }

    JavaClass getWeakReferenceClass() {
        return weakReferenceClass;
    }

    int getReferentFieldIndex() {
        return referentFieldIndex;
    }

    JavaThing getNullThing() {
        return nullThing;
    }

    public void setReachableExcludes(ReachableExcludes e) {
        reachableExcludes = e;
    }

    public ReachableExcludes getReachableExcludes() {
        return reachableExcludes;
    }

    // package privates
    void addReferenceFromRoot(Root r, JavaHeapObject obj) {
        Root root = rootsMap.get(obj);
        if (root == null) {
            rootsMap.put(obj, r);
        } else {
            rootsMap.put(obj, root.mostInteresting(r));
        }
    }

    Root getRoot(JavaHeapObject obj) {
        return rootsMap.get(obj);
    }

    JavaClass getJavaLangClass() {
        return javaLangClass;
    }

    JavaClass getJavaLangString() {
        return javaLangString;
    }

    JavaClass getJavaLangClassLoader() {
        return javaLangClassLoader;
    }

    JavaClass getOtherArrayType() {
        if (otherArrayType == null) {
            synchronized (this) {
                if (otherArrayType == null) {
                    addFakeClass(new JavaClass("[<other>", 0, 0, 0, 0, EMPTY_FIELD_ARRAY, EMPTY_STATIC_ARRAY, 0));
                    otherArrayType = findClass("[<other>");
                }
            }
        }
        return otherArrayType;
    }

    JavaClass getArrayClass(String elementSignature) {
        JavaClass clazz;
        synchronized (classes) {
            clazz = findClass("[" + elementSignature);
            if (clazz == null) {
                clazz = new JavaClass("[" + elementSignature, 0, 0, 0, 0, EMPTY_FIELD_ARRAY, EMPTY_STATIC_ARRAY, 0);
                addFakeClass(clazz);
                // This is needed because the JDK only creates Class structures
                // for array element types, not the arrays themselves.  For
                // analysis, though, we need to pretend that there's a
                // JavaClass for the array type, too.
            }
        }
        return clazz;
    }

    ReadBuffer getReadBuffer() {
        return readBuf;
    }

    void setNew(JavaHeapObject obj, boolean isNew) {
        initNewObjects();
        if (isNew) {
            newObjects.put(obj, Boolean.TRUE);
        }
    }

    boolean isNew(JavaHeapObject obj) {
        if (newObjects != null) {
            return newObjects.get(obj) != null;
        } else {
            return false;
        }
    }

    // Internals only below this point
    private Number makeId(long id) {
        if (identifierSize == 4) {
            return (int) id;
        } else {
            return id;
        }
    }

    private void putInClassesMap(JavaClass c) {
        String name = c.getName();
        if (classes.containsKey(name)) {
            // more than one class can have the same name
            // if so, create a unique name by appending
            // - and id string to it.
            name += "-" + c.getIdString();
        }
        classes.put(c.getName(), c);
    }

    private void addFakeClass(JavaClass c) {
        putInClassesMap(c);
        c.resolve(this);
    }

    private void addFakeClass(Number id, JavaClass c) {
        fakeClasses.put(id, c);
        addFakeClass(c);
    }

    private synchronized void initNewObjects() {
        if (newObjects == null) {
            synchronized (this) {
                if (newObjects == null) {
                    newObjects = new HashMap<>();
                }
            }
        }
    }

    private synchronized void initSiteTraces() {
        if (siteTraces == null) {
            synchronized (this) {
                if (siteTraces == null) {
                    siteTraces = new HashMap<>();
                }
            }
        }
    }
}
