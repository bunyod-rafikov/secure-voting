package com.brafik.samples.applet;

import javacard.framework.Util;

/**
 * DER Parser for JavaCard courtesy of ChatGPT - happy case validated.
 */
public class DER {

    /**
     * Get the tag at the current position.
     *
     * @param data   DER encoded data
     * @param offset Offset to start reading
     * @return The tag value
     */
    public static short getTag(byte[] data, short offset) {
        return (short) (data[offset] & 0xFF);
    }

    /**
     * Get the length at the current position.
     *
     * @param data   DER encoded data
     * @param offset Offset to start reading
     * @return Length of the tag's value
     */
    public static short getLength(byte[] data, short offset) {
        short lengthByte = (short) (data[(short) (offset + 1)] & 0xFF);
        if (lengthByte < 0x80) {
            return lengthByte; // Short form
        }
        short lengthSize = (short) (lengthByte & 0x7F); // Number of subsequent bytes indicating length
        short length = 0;
        for (short i = 0; i < lengthSize; i++) {
            length = (short) ((length << 8) | (data[(short) (offset + 2 + i)] & 0xFF));
        }
        return length;
    }

    /**
     * Get the offset to the value of a tag.
     *
     * @param data   DER encoded data
     * @param offset Offset to the tag
     * @return Offset to the value of the tag
     */
    public static short getValueOffset(byte[] data, short offset) {
        short lengthByte = (short) (data[(short) (offset + 1)] & 0xFF);
        if (lengthByte < 0x80) {
            return (short) (offset + 2); // Short form
        }
        return (short) (offset + 2 + (lengthByte & 0x7F)); // Long form
    }

    /**
     * Get the offset of the next TLV (tag-length-value).
     *
     * @param data   DER encoded data
     * @param offset Offset to the current tag
     * @return Offset to the next TLV
     */
    public static short getNextTLV(byte[] data, short offset) {
        short length = getLength(data, offset);
        short valueOffset = getValueOffset(data, offset);
        return (short) (valueOffset + length);
    }

    /**
     * Search for a specific tag in the DER structure, starting from a given offset.
     * Searches all depths recursively.
     *
     * @param data      DER encoded data
     * @param offset    Starting offset
     * @param length    Total length to search
     * @param tagToFind Tag to search for
     * @return Offset of the found tag, or -1 if not found
     */
    public static short findTag(byte[] data, short offset, short length, short tagToFind) {
        short end = (short) (offset + length);
        while (offset < end) {
            short tag = getTag(data, offset);
            short tagLength = getLength(data, offset);
            short valueOffset = getValueOffset(data, offset);

            if (tag == tagToFind) {
                return offset;
            }

            if ((tag & 0x20) != 0) { // Constructed tag
                // Recurse into constructed tag
                short foundOffset = findTag(data, valueOffset, tagLength, tagToFind);
                if (foundOffset != -1) {
                    return foundOffset;
                }
            }
            offset = getNextTLV(data, offset);
        }
        return -1; // Not found
    }

    /**
     * Get the next occurrence of a specific tag starting from the current offset.
     * Continues searching from the provided offset.
     *
     * @param data      DER encoded data
     * @param offset    Starting offset
     * @param length    Total length to search
     * @param tagToFind Tag to search for
     * @return Offset of the found tag, or -1 if not found
     */
    public static short getNextTag(byte[] data, short offset, short length, short tagToFind) {
        return findTag(data, offset, length, tagToFind);
    }

    /**
     * Extract the length and value of a tag.
     *
     * @param data         DER encoded data
     * @param offset       Offset to the tag
     * @param output       Output buffer to store the tag's value
     * @param outputOffset Offset in the output buffer
     * @return Length of the value copied
     */
    public static short getTagValue(byte[] data, short offset, byte[] output, short outputOffset) {
        short length = getLength(data, offset);
        short valueOffset = getValueOffset(data, offset);
        Util.arrayCopyNonAtomic(data, valueOffset, output, outputOffset, length);
        return length;
    }

    public static short writeLength(byte[] buffer, short offset, short length) {
        if (length < 0x80) {
            buffer[offset++] = (byte) length;
        } else if (length <= 0xFF) {
            buffer[offset++] = (byte) 0x81;
            buffer[offset++] = (byte) length;
        } else {
            buffer[offset++] = (byte) 0x82;
            buffer[offset++] = (byte) (length >> 8);
            buffer[offset++] = (byte) length;
        }
        return offset;
    }
}
