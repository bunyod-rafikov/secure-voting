package com.brafik.samples.applet;

public class Utils {

    public static short copyShortToByteArray(short value, byte[] buffer, short offset) {
        buffer[offset] = (byte) (value >> 8);
        buffer[(short) (offset + 1)] = (byte) value;
        return (short) (offset + 2);
    }

    public static short copyIntToByteArray(int value, byte[] buffer, short offset) {
        buffer[offset] = (byte) (value >> 24);
        buffer[(short) (offset + 1)] = (byte) (value >> 16);
        buffer[(short) (offset + 2)] = (byte) (value >> 8);
        buffer[(short) (offset + 3)] = (byte) value;
        return (short) (offset + 4);
    }

    public static short copyLongToByteArray(long value, byte[] buffer, short offset) {
        buffer[offset] = (byte) (value >> 56);
        buffer[(short) (offset + 1)] = (byte) (value >> 48);
        buffer[(short) (offset + 2)] = (byte) (value >> 40);
        buffer[(short) (offset + 3)] = (byte) (value >> 32);
        buffer[(short) (offset + 4)] = (byte) (value >> 24);
        buffer[(short) (offset + 5)] = (byte) (value >> 16);
        buffer[(short) (offset + 6)] = (byte) (value >> 8);
        buffer[(short) (offset + 7)] = (byte) value;
        return (short) (offset + 8);
    }

    public static short getShortFromByteArray(byte[] buffer, short offset) {
        return (short) (((buffer[offset] & 0xFF) << 8) | (buffer[(short) (offset + 1)] & 0xFF));
    }

    public static int getIntFromByteArray(byte[] buffer, short offset) {
        return ((buffer[offset] & 0xFF) << 24) |
                ((buffer[(short) (offset + 1)] & 0xFF) << 16) |
                ((buffer[(short) (offset + 2)] & 0xFF) << 8) |
                (buffer[(short) (offset + 3)] & 0xFF);
    }

    public static long getLongFromByteArray(byte[] buffer, short offset) {
        return ((long) (buffer[offset] & 0xFF) << 56) |
                ((long) (buffer[(short) (offset + 1)] & 0xFF) << 48) |
                ((long) (buffer[(short) (offset + 2)] & 0xFF) << 40) |
                ((long) (buffer[(short) (offset + 3)] & 0xFF) << 32) |
                ((long) (buffer[(short) (offset + 4)] & 0xFF) << 24) |
                ((long) (buffer[(short) (offset + 5)] & 0xFF) << 16) |
                ((long) (buffer[(short) (offset + 6)] & 0xFF) << 8) |
                ((long) (buffer[(short) (offset + 7)] & 0xFF));
    }

    public static boolean arraysAreEqual(byte[] array1, short offset1, byte[] array2, short offset2, short length) {
        for (short i = 0; i < length; i++) {
            if (array1[(short) (offset1 + i)] != array2[(short) (offset2 + i)]) {
                return false;
            }
        }
        return true;
    }
}
