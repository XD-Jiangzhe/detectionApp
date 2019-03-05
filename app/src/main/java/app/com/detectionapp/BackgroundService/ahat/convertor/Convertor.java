package app.com.detectionapp.BackgroundService.ahat.convertor;

import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

/**
 * @ Author      ：Huanran Wang
 * @ E-mail      : wanghuanran@hit.edu.cn
 * @ Date        ：Created in 13:11 2018.8.1
 * @ Description ：转换类，将delvik中导出的堆二进制文件，转换为可以被Ahat读取和解析的格式.
 */

public class Convertor {
    public static final String SUFFIX = "_conv.hprof";

    private final int kIdentSize = 4;
    private final int kRecHdrLen = 9;

    final int HPROF_BASIC_OBJECT = 2, HPROF_BASIC_BOOLEAN = 4, HPROF_BASIC_CHAR = 5, HPROF_BASIC_FLOAT = 6, HPROF_BASIC_DOUBLE = 7,
            HPROF_BASIC_BYTE = 8, HPROF_BASIC_SHORT = 9, HPROF_BASIC_INT = 10, HPROF_BASIC_LONG = 11;

    final int HPROF_TAG_HEAP_DUMP = 0x0c, HPROF_TAG_HEAP_DUMP_SEGMENT = 0x1c;

    final int HPROF_ROOT_UNKNOWN = 0xff,/* 1.0.2 tags */
            HPROF_ROOT_JNI_GLOBAL = 0x01, HPROF_ROOT_JNI_LOCAL = 0x02, HPROF_ROOT_JAVA_FRAME = 0x03, HPROF_ROOT_NATIVE_STACK = 0x04,
            HPROF_ROOT_STICKY_CLASS = 0x05, HPROF_ROOT_THREAD_BLOCK = 0x06, HPROF_ROOT_MONITOR_USED = 0x07, HPROF_ROOT_THREAD_OBJECT = 0x08,
            HPROF_CLASS_DUMP = 0x20, HPROF_INSTANCE_DUMP = 0x21, HPROF_OBJECT_ARRAY_DUMP = 0x22, HPROF_PRIMITIVE_ARRAY_DUMP = 0x23,

    HPROF_HEAP_DUMP_INFO = 0xfe,/* Android 1.0.3 tags */
            HPROF_ROOT_INTERNED_STRING = 0x89, HPROF_ROOT_FINALIZING = 0x8a, HPROF_ROOT_DEBUGGER = 0x8b, HPROF_ROOT_REFERENCE_CLEANUP =
            0x8c, HPROF_ROOT_VM_INTERNAL = 0x8d, HPROF_ROOT_JNI_MONITOR = 0x8e, HPROF_UNREACHABLE = 0x90, /* deprecated */
            HPROF_PRIMITIVE_ARRAY_NODATA_DUMP = 0xc3;

    private int get2BE(short[] buf, int pos) {
        int val;

        val = ((buf[pos] & 0x0ff) << 8) | ((buf[pos + 1]) & 0x0ff);

        return val;
    }

    private long get4BE(short[] buf, int pos) {
        long val;

        val = ((buf[pos] & 0x0ff) << 24) | ((buf[pos + 1] & 0x0ff) << 16) | ((buf[pos + 2] & 0x0ff) << 8) | (buf[pos + 3] & 0x0ff);

        return val;
    }

    private void set4BE(short[] buf, int pos, long val) {
        buf[pos] = (short) ((val >> 24) & 0x0ff);
        buf[pos + 1] = (short) ((val >> 16) & 0x0ff);
        buf[pos + 2] = (short) ((val >> 8) & 0x0ff);
        buf[pos + 3] = (short) (val & 0x0ff);
    }

    private int computeBasicLen(final int basicType) {
        final int[] sizes = {-1, -1, 4, -1, 1, 2, 4, 8, 1, 2, 4, 8};
        final long maxSize = sizes.length;

        if (basicType >= maxSize) return -1;

        return sizes[basicType];
    }

    private int computeClassDumpLen(short[] origBuf, int _pos, int len) {
        int pos = _pos;
        int blockLen = 0;
        int i, count;

        blockLen += kIdentSize * 7 + 8;
        pos += blockLen;
        len -= blockLen;

        if (len < 0) return -1;

        count = get2BE(origBuf, pos);
        pos += 2;
        len -= 2;

        for (i = 0; i < count; i++) {
            int basicType;
            int basicLen;

            basicType = origBuf[pos + 2];
            basicLen = computeBasicLen(basicType);
            if (basicLen < 0) {
                System.err.println("ERROR: invalid basicType");
                return -1;
            }

            pos += 2 + 1 + basicLen;
            len -= 2 + 1 + basicLen;
            if (len < 0) return -1;
        }

        count = get2BE(origBuf, pos);
        pos += 2;
        len -= 2;

        for (i = 0; i < count; i++) {
            int basicType;
            int basicLen;

            basicType = origBuf[pos + kIdentSize];
            basicLen = computeBasicLen(basicType);
            if (basicLen < 0) {
                System.err.println("ERROR: invalid basicType");
                return -1;
            }

            pos += kIdentSize + 1 + basicLen;
            len -= kIdentSize + 1 + basicLen;
            if (len < 0) return -1;
        }

        count = get2BE(origBuf, pos);
        pos += 2;
        len -= 2;

        for (i = 0; i < count; i++) {
            pos += kIdentSize + 1;
            len -= kIdentSize + 1;
            if (len < 0) return -1;
        }

        return pos - _pos;
    }

    private long computeInstanceDumpLen(short[] origBuf, int _pos, int len) {
        long extraCount = get4BE(origBuf, _pos + kIdentSize * 2 + 4);
        return kIdentSize * 2 + 8 + extraCount;
    }

    private long computeObjectArrayDumpLen(short[] origBuf, int _pos, int len) {
        long arrayCount = get4BE(origBuf, _pos + kIdentSize + 4);
        return kIdentSize * 2 + 8 + arrayCount * kIdentSize;
    }

    private long computePrimitiveArrayDumpLen(short[] origBuf, int _pos, int len) {
        long arrayCount = get4BE(origBuf, _pos + kIdentSize + 4);
        int basicType;
        basicType = origBuf[_pos + kIdentSize + 8];
        int basicLen = computeBasicLen(basicType);

        return kIdentSize + 9 + arrayCount * basicLen;
    }

    private int processHeapDump(ExpandBuf pBuf, DataOutputStream dos) {
        ExpandBuf pOutBuf = new ExpandBuf();

        short[] originBuf = pBuf.storage;
        int pos = 0;
        int len = pBuf.getLength();
        int result = -1;

        pBuf = null;
        if (!pOutBuf.addData(originBuf, pos, kRecHdrLen)) {
            return result;
        }

        pos += kRecHdrLen;
        len -= kRecHdrLen;

        while (len > 0) {
            int subType = originBuf[pos];
            boolean justCopy = true;
            int subLen;

            switch (subType) {
                /* 1.0.2 types */
                case HPROF_ROOT_UNKNOWN:
                    subLen = kIdentSize;
                    break;
                case HPROF_ROOT_JNI_GLOBAL:
                    subLen = kIdentSize * 2;
                    break;
                case HPROF_ROOT_JNI_LOCAL:
                    subLen = kIdentSize + 8;
                    break;
                case HPROF_ROOT_JAVA_FRAME:
                    subLen = kIdentSize + 8;
                    break;
                case HPROF_ROOT_NATIVE_STACK:
                    subLen = kIdentSize + 4;
                    break;
                case HPROF_ROOT_STICKY_CLASS:
                    subLen = kIdentSize;
                    break;
                case HPROF_ROOT_THREAD_BLOCK:
                    subLen = kIdentSize + 4;
                    break;
                case HPROF_ROOT_MONITOR_USED:
                    subLen = kIdentSize;
                    break;
                case HPROF_ROOT_THREAD_OBJECT:
                    subLen = kIdentSize + 8;
                    break;
                case HPROF_CLASS_DUMP:
                    subLen = computeClassDumpLen(originBuf, pos + 1, len - 1);
                    break;
                case HPROF_INSTANCE_DUMP:
                    subLen = (int) computeInstanceDumpLen(originBuf, pos + 1, len - 1);
                    break;
                case HPROF_OBJECT_ARRAY_DUMP:
                    subLen = (int) computeObjectArrayDumpLen(originBuf, pos + 1, len - 1);
                    break;
                case HPROF_PRIMITIVE_ARRAY_DUMP:
                    subLen = (int) computePrimitiveArrayDumpLen(originBuf, pos + 1, len - 1);
                    break;

                /* these were added for Android in 1.0.3 */
                case HPROF_HEAP_DUMP_INFO:
                    justCopy = false;
                    subLen = kIdentSize + 4;
                    // no 1.0.2 equivalent for this
                    break;
                case HPROF_ROOT_INTERNED_STRING:
                    originBuf[pos] = HPROF_ROOT_UNKNOWN;
                    subLen = kIdentSize;
                    break;
                case HPROF_ROOT_FINALIZING:
                    originBuf[pos] = HPROF_ROOT_UNKNOWN;
                    subLen = kIdentSize;
                    break;
                case HPROF_ROOT_DEBUGGER:
                    originBuf[pos] = HPROF_ROOT_UNKNOWN;
                    subLen = kIdentSize;
                    break;
                case HPROF_ROOT_REFERENCE_CLEANUP:
                    originBuf[pos] = HPROF_ROOT_UNKNOWN;
                    subLen = kIdentSize;
                    break;
                case HPROF_ROOT_VM_INTERNAL:
                    originBuf[pos] = HPROF_ROOT_UNKNOWN;
                    subLen = kIdentSize;
                    break;
                case HPROF_ROOT_JNI_MONITOR:
                    /* keep the ident, drop the next 8 bytes */
                    originBuf[pos] = HPROF_ROOT_UNKNOWN;
                    justCopy = false;
                    pOutBuf.addData(originBuf, pos, 1 + kIdentSize);
                    subLen = kIdentSize + 8;
                    break;
                case HPROF_UNREACHABLE:
                    originBuf[pos] = HPROF_ROOT_UNKNOWN;
                    subLen = kIdentSize;
                    break;
                case HPROF_PRIMITIVE_ARRAY_NODATA_DUMP:
                    originBuf[pos] = HPROF_PRIMITIVE_ARRAY_DUMP;
                    originBuf[pos + 5] = originBuf[pos + 6] = originBuf[pos + 7] = originBuf[pos + 8] = 0;
                    /* set array len to 0*/
                    subLen = kIdentSize + 9;
                    break;

                /* shouldn't get here */
                default:
                    System.err.println("ERROR: unexpected subtype");
                    return result;
            }

            if (justCopy) {
                /* copy source data */
                pOutBuf.addData(originBuf, pos, 1 + subLen);
            }

            /* advance to next entry */
            pos += 1 + subLen;
            len -= 1 + subLen;
        }

        /*
         * Update the record length.
         */
        set4BE(pOutBuf.storage, 5, pOutBuf.getLength() - kRecHdrLen);

        if (!pOutBuf.writeData(dos)) return result;

        result = 0;

        return result;
    }

    private static final String TAG = "Convertor";
    private boolean filterData(DataInputStream dis, DataOutputStream dos) {
        byte[] magicString;
        ExpandBuf pBuf = new ExpandBuf();
        boolean result = false;

        if (!pBuf.readString(dis)) {
            return result;
        }

        /*
         * Start with the header.
         */
        magicString = new byte[pBuf.getLength() - 1];
        for (int i = 0; i < pBuf.getLength() - 1; i++) {
            magicString[i] = (byte) pBuf.storage[i];
        }

        String versionStr = new String(magicString);
        System.out.println(versionStr);

        if (!versionStr.equals("JAVA PROFILE 1.0.3")) {
            if (versionStr.equals("JAVA PROFILE 1.0.2")) {
                System.err.println("ERROR: HPROF file already in 1.0.2 format.");
            } else {
                System.err.println("ERROR: expecting HPROF file format 1.0.3");
            }
            return result;
        }

        /* downgrade to 1.0.2 */
        pBuf.storage[17] = '2';
        if (!pBuf.writeData(dos)) {
            return result;
        }

        /*
         * Copy: (4b) identifier size, always 4 (8b) file creation date
         */
        if (!pBuf.readData(dis, 12, false)) return result;
        if (!pBuf.writeData(dos)) return result;

        /*
         * Read records until we hit EOF. Each record begins with:
         * (1b) type
         * (4b) timestamp
         * (4b) length of data that follows
         */
        while (true) {
            /* read type char */
            if (!pBuf.readData(dis, 1, true)) {
                return result;
            }
            try {
                if (dis.available() == 0) {
                    break;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return result;
            }

            /* read the rest of the header */
            if (!pBuf.readData(dis, kRecHdrLen - 1, false)) {
                return result;
            }

            short[] buf = pBuf.storage;
            short type;
            long length;

            type = buf[0];
            // timestamp=get4BE(buf, 1);
            length = get4BE(buf, 5);
            buf = null;/* ptr invalid after next read op */

            /* read the record data */
            if (length != 0) {
                if (!pBuf.readData(dis, (int) length, false)) return result;
            }

            if (type == HPROF_TAG_HEAP_DUMP || type == HPROF_TAG_HEAP_DUMP_SEGMENT) {
                if (processHeapDump(pBuf, dos) != 0) return result;
                pBuf.clear();
            } else {
                /* keep */
                if (!pBuf.writeData(dos)) return result;
            }
        }

        result = true;
        return result;
    }

    public boolean startConvert(String inFileName) {
        try {
            DataInputStream dis = new DataInputStream(new FileInputStream(inFileName));

            String outFileName = inFileName + SUFFIX;
            File outFile = new File(outFileName);

            if (outFile.exists()) {
                outFile.delete();
            }
            outFile.createNewFile();

            DataOutputStream dos = new DataOutputStream(new FileOutputStream(outFileName));

            boolean res = filterData(dis, dos);
            dis.close();

            dos.flush();
            dos.close();

            return res;
        } catch (IOException e) {
            Log.d(TAG, "startConvert: jiangzhe " + e.getMessage());
            return false;
        }
    }
}

class ExpandBuf {

    short[] storage;
    int curLen;
    int maxLen;

    public ExpandBuf() {
        /** Writen by  : Huanran Wang
         *  Created on : 2018.8.1 at 13:17.
         *  Description: 一块内存。。。完全按照C文件的思想. */

        final int kInitialSize = 64;
        storage = new short[kInitialSize];
        curLen = 0;
        maxLen = kInitialSize;
    }

    public int getLength() {
        return curLen;
    }

    public void clear() {
        curLen = 0;
    }

    public void reAlloc(int size) {
        storage = Arrays.copyOf(storage, size);
    }

    public boolean ensureCapacity(int size) {
        if (curLen + size > maxLen) {
            int newSize = curLen + size + 128;
            reAlloc(newSize);
            if (storage == null) {
                System.err.println("ERROR: realloc failed on size=" + newSize);
                return false;
            }
            maxLen = newSize;
        }

        return true;
    }

    public boolean addData(short[] data, int _pos, int count) {
        if (!ensureCapacity(count)) {
            return false;
        }

        for (int i = 0; i < count; i++) {
            storage[curLen++] = data[_pos + i];
        }

        return true;
    }

    public boolean readString(DataInputStream dis) {
        try {
            int ic;

            do {
                if (!ensureCapacity(1)) {
                    return false;
                }
                ic = dis.readUnsignedByte();
                storage[curLen++] = (short) ic;
            } while (ic != 0);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("ERROR: failed reading input");
            return false;
        }

        return true;
    }

    public boolean readData(DataInputStream dis, int count, boolean eofExpected) {
        int actual = 0;

        ensureCapacity(count);

        byte[] temp = new byte[count];

        try {
            actual = dis.read(temp);

            if (actual != count) {
                if (!eofExpected) {
                    System.err.println("ERROR: read data failed");
                    return false;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("ERROR: read data failed");
            return false;
        }

        for (int i = 0; i < actual; i++) {
            storage[curLen + i] = (short) (temp[i] & 0xFF);
        }
        curLen += count;
        return true;
    }

    public boolean writeData(DataOutputStream dos) {
        try {
            byte[] temp = new byte[curLen];
            for (int i = 0; i < curLen; i++) {
                temp[i] = (byte) storage[i];
            }
            dos.write(temp);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("ERROR: write data failed");
            return false;
        }

        curLen = 0;
        return true;
    }
}