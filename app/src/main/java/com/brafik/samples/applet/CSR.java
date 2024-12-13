package com.brafik.samples.applet;

import javacard.framework.JCSystem;
import javacard.framework.Util;
import javacard.security.ECPublicKey;
import javacard.security.Signature;

public class CSR {

    static byte[] subjectPublicKey = JCSystem.makeTransientByteArray((short) 66, JCSystem.CLEAR_ON_RESET);

    public static boolean verifyTBS(byte[] certificate, short offset, short datalen, ECPublicKey signerPublicKey) {

        // Outermost SEQUENCE
        short outerSequenceOffset = DER.findTag(certificate, offset, datalen, (short) 0x30); // SEQUENCE tag
        short outerSequenceLength = DER.getLength(certificate, outerSequenceOffset);
        short outerSequenceValueOffset = DER.getValueOffset(certificate, outerSequenceOffset);

        // TBS SEQUENCE
        short tbsOffset = DER.findTag(certificate, outerSequenceValueOffset, outerSequenceLength, (short) 0x30); // SEQUENCE tag
        short tbsNextTLVOffset = DER.getNextTLV(certificate, tbsOffset);
        short tbsTLVLength = (short) (tbsNextTLVOffset - tbsOffset);
        byte[] tbs = new byte[tbsTLVLength];
        System.arraycopy(certificate, tbsOffset, tbs, 0, tbsTLVLength);

        // AlgorithmIdentifier SEQUENCE
        short algorithmIdentifierOffset = DER.findTag(certificate, tbsNextTLVOffset,
                (short) (outerSequenceValueOffset + outerSequenceLength - tbsNextTLVOffset),
                (short) 0x30);
        short algorithmIdentifierNextTLVOffset = DER.getNextTLV(certificate, algorithmIdentifierOffset);

        // Signature BIT STRING
        short signatureOffset = DER.findTag(certificate, algorithmIdentifierNextTLVOffset,
                (short) (outerSequenceValueOffset + outerSequenceLength - algorithmIdentifierNextTLVOffset),
                (short) 0x03);
        short signatureNextTLVOffset = DER.getNextTLV(certificate, signatureOffset);
        short signatureTLVLength = (short) (signatureNextTLVOffset - signatureOffset);
        byte[] signature = new byte[signatureTLVLength];
        System.arraycopy(certificate, signatureOffset, signature, 0, signatureTLVLength);

        // Verify the signature
        Signature signatureAlg = Signature.getInstance(Signature.ALG_ECDSA_SHA_256, false);
        signatureAlg.init(signerPublicKey, Signature.MODE_VERIFY);
        // Skip BIT STRING overhead in signature and it works...
        return signatureAlg.verify(tbs, (short) 0, tbsTLVLength, signature, (short) 3, (short) ((short) signature.length - 3));
    }

    static short createSubject(byte[] src, short srcOff, short dataLen, byte[] dst, short dstOff) {
        short offset = dstOff;
        dst[offset++] = 0x30; // Name
        dst[offset++] = (byte) ((byte) dataLen + 11);
        dst[offset++] = 0x31; // RelativeDistunguishedName
        dst[offset++] = (byte) ((byte) dataLen + 9);
        dst[offset++] = 0x30; // AttributeTypeAndValue
        dst[offset++] = (byte) (dataLen + 7);
        dst[offset++] = 0x06; // AttributeType (commonName)
        dst[offset++] = 0x03;
        dst[offset++] = 0x55;
        dst[offset++] = 0x04;
        dst[offset++] = 0x03;
        dst[offset++] = 0x13; // AttributeValue
        dst[offset++] = (byte) dataLen;
        Util.arrayCopyNonAtomic(src, srcOff, dst, offset, dataLen);
        offset += dataLen;
        return (short) (offset - dstOff);
    }

    static short createSubjectPublicKeyInfo(byte[] dst, short dstOff, ECPublicKey publicKey) {
        short offset = dstOff;

        // AlgorithmIdentifier for ecPublicKey + prime256v1
        byte[] algorithmIdentifier = new byte[]{
                0x30, 0x13, 0x06, 0x07, 0x2A, (byte) 0x86, 0x48, (byte) 0xCE, 0x3D, 0x02, 0x01,
                0x06, 0x08, 0x2A, (byte) 0x86, 0x48, (byte) 0xCE, 0x3D, 0x03, 0x01, 0x07
        };

        // Get uncompressed public key
        short subjectPublicKeyLen = publicKey.getW(subjectPublicKey, (short) 0);

        dst[offset++] = (byte) 0x30;    // SubjectPublicKeyInfo SEQUENCE
        dst[offset++] = (byte) (algorithmIdentifier.length + subjectPublicKeyLen + 3); // +3 for BIT STRING overhead

        // Copy AlgorithmIdentifier
        Util.arrayCopyNonAtomic(algorithmIdentifier, (short) 0, dst, offset, (short) algorithmIdentifier.length);
        offset += (short) algorithmIdentifier.length;

        // BIT STRING
        dst[offset++] = (byte) 0x03;
        dst[offset++] = (byte) (subjectPublicKeyLen + 1); // Length includes padding byte
        dst[offset++] = 0x00; // Unused bits = 0

        // Copy SubjectPublicKey
        Util.arrayCopyNonAtomic(subjectPublicKey, (short) 0, dst, offset, subjectPublicKeyLen);
        offset += subjectPublicKeyLen;

        return (short) (offset - dstOff);
    }

    static boolean containsCN(byte[] cert, short certOffset, short certLength, byte[] cn, short cnLen) {
        byte[] cnOID = {0x55, 0x04, 0x03};
        short offset = certOffset;
        while (offset < (certOffset + certLength)) {
            short tag = DER.getTag(cert, offset);
            short length = DER.getLength(cert, offset);
            short valueOffset = DER.getValueOffset(cert, offset);
            if (tag == 0x06) { // OID tag
                if (length == 3 && // CN OID
                        cert[valueOffset] == cnOID[0] &&
                        cert[(short) (valueOffset + 1)] == cnOID[1] &&
                        cert[(short) (valueOffset + 2)] == cnOID[2]) {
                    short cnValueOffset = DER.getNextTLV(cert, offset);
                    short cnLength = DER.getLength(cert, cnValueOffset);
                    short cnValueStart = DER.getValueOffset(cert, cnValueOffset);
                    if (cnLength == cnLen && Utils.arraysAreEqual(cert, cnValueStart, cn, (short) 0, cnLen)) {
                        return true;
                    }
                }
            }
            // Recursively search constructed tags
            if ((tag & 0x20) != 0) { // Constructed tag
                if (containsCN(cert, valueOffset, length, cn, cnLen)) {
                    return true; // Match found in a nested structure
                }
            }
            offset = DER.getNextTLV(cert, offset);
        }
        return false;
    }

    static boolean containsSubjectPublicKey(byte[] cert, short certOffset, short certLength, byte[] subjectPublicKey, short subjectPublicKeyLength) {
        short offset = certOffset;
        while (offset < (certOffset + certLength)) {
            short tag = DER.getTag(cert, offset);
            short length = DER.getLength(cert, offset);
            short valueOffset = DER.getValueOffset(cert, offset);
            if (tag == 0x03) { // BIT STRING
                short bitStringLength = DER.getLength(cert, offset);
                short bitStringValueOffset = DER.getValueOffset(cert, offset);
                short publicKeyLength = (short) (bitStringLength - 1); // skip the first byte: unused bits indicator
                if (publicKeyLength == subjectPublicKeyLength &&
                        Utils.arraysAreEqual(cert, (short) (bitStringValueOffset + 1), subjectPublicKey, (short) 0, publicKeyLength)) {
                    return true;
                }
            }
            // Recursively search constructed tags
            if ((tag & 0x20) != 0) {
                if (containsSubjectPublicKey(cert, valueOffset, length, subjectPublicKey, subjectPublicKeyLength)) {
                    return true; // Found in nested structure
                }
            }
            offset = DER.getNextTLV(cert, offset);
        }
        return false;
    }
}
