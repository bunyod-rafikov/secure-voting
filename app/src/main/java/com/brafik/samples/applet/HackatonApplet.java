package com.brafik.samples.applet;

import javacard.framework.*;
import javacard.security.ECPrivateKey;
import javacard.security.ECPublicKey;
import javacard.security.KeyBuilder;
import javacard.security.KeyPair;
import javacard.security.Signature;

/**
 * Hackaton applet -  never use this in any kind of production project
 */
public class HackatonApplet extends Applet {

    // CLA
    private final static byte CLA_HACKATHON = 0x00;

    // INS ISO7816
    private final static byte INS_GET_RESPONSE = (byte) 0xC0; // ISO 7816
    // INS Proprietary
    // Generate a CSR for onboarding
    private final static byte INS_CSR_CREATE = 0x10;
    // Verifies the X509 and finalizes the onboarding
    private final static byte INS_CSR_FULFILL = 0x20;
    // Updates the user PIN
    private final static byte INS_UPDATE_PIN = 0x60;
    // Verifies the user PIN
    private final static byte INS_VERIFY_PIN = 0x61;
    // Get the applet status
    private final static byte INS_GET_STATUS = 0x70;
    // Get the applet user X509
    private final static byte INS_GET_CERTIFICATE = 0x71;
    // Get election data
    private final static byte INS_GET_ELECTION = 0x72;
    // Get registered elections
    private final static byte INS_GET_ELECTIONS = 0x73;
    // Register a new election
    private final static byte INS_REGISTER_ELECTION = 0x76;
    // Vote on an election
    private final static byte INS_VOTE = 0x77;
    // Get the vote receipt from a previous vote
    private final static byte INS_GET_VOTE_RECEIPT = 0x78;
    // Removes the vote from the applet when the vote is registered at the voting authority
    private final static byte INS_REGISTER_SUBMISSION = 0x79;

    // P1 & P2
    private final static byte P1_INIT = 1;
    private final static byte P1_UPDATE = 2;
    private final static byte P1_FINAL = 3;

    // Constants
    private static final byte MAX_PIN_TRIES = 10;
    private static final byte MAX_PIN_SIZE = 8;
    private static final byte MIN_PIN_SIZE = 4;

    private static final byte MAX_NUMBER_OF_ELECTIONS = 5;

    private final short APDU_CHUNK_SIZE = 240;

    // The voting authority / issuer public key
    private final static byte[] ISSUER_PUBLIC_KEY = {
            (byte) 0x04, (byte) 0x70, (byte) 0x9d, (byte) 0xb8, (byte) 0xd6, (byte) 0x08,
            (byte) 0x27, (byte) 0x80, (byte) 0x40, (byte) 0xe6, (byte) 0x60, (byte) 0x96,
            (byte) 0xa9, (byte) 0x57, (byte) 0x59, (byte) 0xb9, (byte) 0x4e, (byte) 0xb9,
            (byte) 0x19, (byte) 0xdd, (byte) 0x19, (byte) 0x8d, (byte) 0x7d, (byte) 0x73,
            (byte) 0xe5, (byte) 0x7e, (byte) 0x5e, (byte) 0x00, (byte) 0x5d, (byte) 0xdc,
            (byte) 0x47, (byte) 0xb5, (byte) 0x6c, (byte) 0xab, (byte) 0x79, (byte) 0x8b,
            (byte) 0xb2, (byte) 0x1e, (byte) 0x15, (byte) 0xf2, (byte) 0x1e, (byte) 0x7b,
            (byte) 0xb8, (byte) 0x10, (byte) 0x15, (byte) 0x4b, (byte) 0x35, (byte) 0x70,
            (byte) 0x70, (byte) 0x67, (byte) 0x6c, (byte) 0xbc, (byte) 0x72, (byte) 0x3c,
            (byte) 0x67, (byte) 0x7a, (byte) 0xeb, (byte) 0x68, (byte) 0xf2, (byte) 0xcc,
            (byte) 0x6e, (byte) 0x67, (byte) 0x6e, (byte) 0x49, (byte) 0xfb};

    // A common key used to double sign the CSR to ensure that the CSR comes from a trusted applet
    private final static byte[] COMMON_KEY_PRIVATE_RAW = {
            (byte) 0xfe, (byte) 0x40, (byte) 0xb5, (byte) 0x3f, (byte) 0xa4, (byte) 0xf7,
            (byte) 0xda, (byte) 0xf6, (byte) 0x0b, (byte) 0x6a, (byte) 0x97, (byte) 0x03,
            (byte) 0x78, (byte) 0x88, (byte) 0x1c, (byte) 0xf1, (byte) 0x26, (byte) 0xd6,
            (byte) 0x04, (byte) 0x24, (byte) 0x8d, (byte) 0xac, (byte) 0x77, (byte) 0x85,
            (byte) 0x61, (byte) 0xa6, (byte) 0xe2, (byte) 0xd6, (byte) 0x73, (byte) 0xd6,
            (byte) 0x31, (byte) 0xec};

    // Persisted objects
    private final ECPublicKey intermediatePublicKey;
    private final ECPublicKey publicKey;
    private final ECPrivateKey privateKey;
    private final ECPrivateKey privateCommonKey;

    private final OwnerPIN pin;
    private byte[] voterCertificate = null;

    private Election[] elections;

    // Temporary transient buffers
    private final byte[] SCRATCH_BUFFER; // context specific
    private short SCRATCH_BUFFER_LEN;
    private final byte[] TMP_BUFFER; // for data that don't fit in one APDU
    private short TMP_BUFFER_LEN;
    private short TMP_BUFFER_OFFSET;

    private final short[] CANDIDATE_SIZES;

    HackatonApplet(byte[] bArray, short bOffset, byte bLength) {

        // Prepare the user keys
        publicKey = (ECPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_EC_FP_PUBLIC, KeyBuilder.LENGTH_EC_FP_256, false);
        privateKey = (ECPrivateKey) KeyBuilder.buildKey(KeyBuilder.TYPE_EC_FP_PRIVATE, KeyBuilder.LENGTH_EC_FP_256, false);
        EC.setCurveParameters(publicKey);
        EC.setCurveParameters(privateKey);

        // Create the intermediate public key
        intermediatePublicKey = (ECPublicKey) KeyBuilder.buildKey(KeyBuilder.TYPE_EC_FP_PUBLIC, KeyBuilder.LENGTH_EC_FP_256, false);
        EC.setCurveParameters(intermediatePublicKey);
        intermediatePublicKey.setW(ISSUER_PUBLIC_KEY, (short) 0, (short) ISSUER_PUBLIC_KEY.length);

        // Create the common key
        privateCommonKey = (ECPrivateKey) KeyBuilder.buildKey(KeyBuilder.TYPE_EC_FP_PRIVATE, KeyBuilder.LENGTH_EC_FP_256, false);
        EC.setCurveParameters(privateCommonKey);
        privateCommonKey.setS(COMMON_KEY_PRIVATE_RAW, (short) 0, (short) COMMON_KEY_PRIVATE_RAW.length);

        // Allocate objects for the elections
        elections = new Election[MAX_NUMBER_OF_ELECTIONS];
        for (int i = 0; i < MAX_NUMBER_OF_ELECTIONS; i++) {
            elections[i] = new Election();
        }

        // Allocate the PIN object
        pin = new OwnerPIN(MAX_PIN_TRIES, MAX_PIN_SIZE);

        // Create the large working buffer
        TMP_BUFFER = JCSystem.makeTransientByteArray((short) 1024, JCSystem.CLEAR_ON_RESET);

        // Another transient scratch buffer
        SCRATCH_BUFFER = JCSystem.makeTransientByteArray((short) 64, JCSystem.CLEAR_ON_DESELECT);
        CANDIDATE_SIZES = JCSystem.makeTransientShortArray((short) Election.MAX_OPTIONS, JCSystem.CLEAR_ON_RESET);

        register();
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) throws ISOException {
        new HackatonApplet(bArray, bOffset, bLength);
    }

    @Override
    public void process(APDU apdu) throws ISOException {
        if (!selectingApplet()) {

            byte[] buffer = apdu.getBuffer();
            short dataLen = apdu.setIncomingAndReceive();
            byte bytesRemaining = 0;

            if (buffer[ISO7816.OFFSET_CLA] != CLA_HACKATHON) {
                ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
            }

            // rudimentary state check
            checkState(buffer[ISO7816.OFFSET_INS]);

            switch (buffer[ISO7816.OFFSET_INS]) {
                case INS_CSR_CREATE:
                    short pinLen = (short) (dataLen - buffer[ISO7816.OFFSET_P1]);
                    short commonNameLen = (short) (dataLen - pinLen);
                    // PIN related
                    if (pinLen < MIN_PIN_SIZE || pinLen > MAX_PIN_SIZE) {
                        ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
                    }
                    pin.update(buffer, (short) (ISO7816.OFFSET_CDATA + commonNameLen), (byte) pinLen);
                    // CSR related
                    dataLen = createCSR(buffer, ISO7816.OFFSET_CDATA, commonNameLen);
                    apdu.setOutgoingAndSend((short) 0, dataLen);
                    if (TMP_BUFFER_LEN > 0 && TMP_BUFFER_LEN <= APDU_CHUNK_SIZE) {
                        TMP_BUFFER_OFFSET = 0;
                        ISOException.throwIt((short) (ISO7816.SW_BYTES_REMAINING_00 | (byte) TMP_BUFFER_LEN & 0xff));
                    } else {
                        ISOException.throwIt(ISO7816.SW_NO_ERROR);
                    }
                    break;
                case INS_CSR_FULFILL:
                    switch (buffer[ISO7816.OFFSET_P1]) {
                        case P1_INIT:
                            Util.arrayCopyNonAtomic(buffer, ISO7816.OFFSET_CDATA, TMP_BUFFER, (short) 0, dataLen);
                            TMP_BUFFER_LEN = dataLen;
                            break;
                        case P1_UPDATE:
                            Util.arrayCopyNonAtomic(buffer, ISO7816.OFFSET_CDATA, TMP_BUFFER, TMP_BUFFER_LEN, dataLen);
                            TMP_BUFFER_LEN += dataLen;
                            break;
                        case P1_FINAL:
                            Util.arrayCopyNonAtomic(buffer, ISO7816.OFFSET_CDATA, TMP_BUFFER, TMP_BUFFER_LEN, dataLen);
                            TMP_BUFFER_LEN += dataLen;
                            fulfillCSR(TMP_BUFFER, (short) 0, TMP_BUFFER_LEN);
                            TMP_BUFFER_LEN = 0;
                            TMP_BUFFER_OFFSET = 0;
                            SCRATCH_BUFFER_LEN = 0;
                            break;
                        default:
                            TMP_BUFFER_LEN = 0;
                            TMP_BUFFER_OFFSET = 0;
                            SCRATCH_BUFFER_LEN = 0;
                            ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
                    }
                    break;
                case INS_UPDATE_PIN:
                    updatePin(buffer);
                    break;
                case INS_VERIFY_PIN:
                    verifyPin(buffer);
                    break;
                case INS_GET_STATUS:
                    getStatus(apdu);
                    break;
                case INS_GET_CERTIFICATE:
                    // Assume that the cert is > APDU_CHUNK_SIZE...
                    // Copy start of cert to APDU buffer
                    Util.arrayCopyNonAtomic(voterCertificate, (short) 0, buffer, (short) 0, APDU_CHUNK_SIZE);
                    apdu.setOutgoingAndSend((short) 0, APDU_CHUNK_SIZE);
                    // Copy the rest to the temp buffer
                    TMP_BUFFER_LEN = (short) (voterCertificate.length - APDU_CHUNK_SIZE);
                    Util.arrayCopyNonAtomic(voterCertificate, APDU_CHUNK_SIZE, TMP_BUFFER, (short) 0, TMP_BUFFER_LEN);
                    TMP_BUFFER_OFFSET = 0;
                    short result = (short) (ISO7816.SW_BYTES_REMAINING_00 | (byte) TMP_BUFFER_LEN & 0xff);
                    ISOException.throwIt(result);
                    break;
                case INS_GET_ELECTION:
                    dataLen = getElection(buffer, ISO7816.OFFSET_CDATA);
                    apdu.setOutgoingAndSend((short) 0, dataLen);
                    break;
                case INS_GET_ELECTIONS:
                    dataLen = getElections(buffer);
                    apdu.setOutgoingAndSend((short) 0, dataLen);
                    break;
                case INS_GET_RESPONSE:
                    short bytesToSend;
                    if (TMP_BUFFER_LEN > TMP_BUFFER_OFFSET) {
                        bytesToSend = (short) Math.min((short) (TMP_BUFFER_LEN - TMP_BUFFER_OFFSET), APDU_CHUNK_SIZE);
                        Util.arrayCopyNonAtomic(TMP_BUFFER, TMP_BUFFER_OFFSET, buffer, (short) 0, bytesToSend);
                        TMP_BUFFER_OFFSET += bytesToSend;
                        apdu.setOutgoingAndSend((short) 0, bytesToSend);
                        if (TMP_BUFFER_LEN > TMP_BUFFER_OFFSET) {
                            short remaining = (short) (TMP_BUFFER_LEN - TMP_BUFFER_OFFSET);
                            ISOException.throwIt((short) (ISO7816.SW_BYTES_REMAINING_00 | (byte) remaining & 0xff));
                        }
                    }
                    ISOException.throwIt(ISO7816.SW_NO_ERROR);
                    break;

                case INS_VOTE:
                    dataLen = vote(buffer, ISO7816.OFFSET_CDATA);
                    apdu.setOutgoingAndSend((short) 0, dataLen);
                    // return signature and indicate that the result can be fetched with GET RESPONSE
                    TMP_BUFFER_OFFSET = 0;
                    if (TMP_BUFFER_LEN > APDU_CHUNK_SIZE) {
                        bytesRemaining = (byte) APDU_CHUNK_SIZE;
                    } else {
                        bytesRemaining = (byte) TMP_BUFFER_LEN;
                    }
                    ISOException.throwIt((short) (ISO7816.SW_BYTES_REMAINING_00 | (byte) bytesRemaining & 0xff));
                    break;
                case INS_GET_VOTE_RECEIPT:
                    dataLen = getVoteReceipt(buffer, ISO7816.OFFSET_CDATA);
                    apdu.setOutgoingAndSend((short) 0, dataLen);
                    // return signature and indicate that the result can be fetched with GET RESPONSE
                    TMP_BUFFER_OFFSET = 0;
                    if (TMP_BUFFER_LEN > APDU_CHUNK_SIZE) {
                        bytesRemaining = (byte) APDU_CHUNK_SIZE;
                    } else {
                        bytesRemaining = (byte) TMP_BUFFER_LEN;
                    }
                    ISOException.throwIt((short) (ISO7816.SW_BYTES_REMAINING_00 | (byte) bytesRemaining & 0xff));
                    break;
                case INS_REGISTER_ELECTION:
                    receiveLargeSignedData(buffer);
                    registerElection();
                    break;
                case INS_REGISTER_SUBMISSION:
                    receiveLargeSignedData(buffer);
                    registerSubmission();
                    break;
                default:
                    ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
            }
        }
    }

    //////// Misc

    private void checkState(short INS) {
        boolean isActivated = (voterCertificate != null);
        switch (INS) {
            case INS_CSR_CREATE:
            case INS_CSR_FULFILL:
                if (isActivated) {
                    ISOException.throwIt(SW.ILLEGAL_STATE);
                }
                break;
            case INS_UPDATE_PIN:
            case INS_VERIFY_PIN:
            case INS_GET_STATUS:
            case INS_GET_CERTIFICATE:
            case INS_GET_ELECTION:
            case INS_GET_ELECTIONS:
            case INS_REGISTER_ELECTION:
            case INS_VOTE:
            case INS_GET_VOTE_RECEIPT:
            case INS_REGISTER_SUBMISSION:
                if (!isActivated) {
                    ISOException.throwIt(SW.ILLEGAL_STATE);
                }
        }
    }

    private void getStatus(APDU apdu) {
        byte[] buffer = apdu.getBuffer();
        buffer[0] = (byte) (voterCertificate == null ? 0x00 : 0x01);
        apdu.setOutgoingAndSend((short) 0, (short) 1);
    }

    private void verifyPin(byte[] buffer) {
        byte lc = buffer[ISO7816.OFFSET_LC];
        if (!pin.check(buffer, ISO7816.OFFSET_CDATA, lc)) {
            SW.pinFailed(pin.getTriesRemaining());
        } else {
            SW.pinCorrect(pin.getTriesRemaining());
        }
    }

    private void updatePin(byte[] buffer) {
        byte lc = buffer[ISO7816.OFFSET_LC];
        if (lc < MIN_PIN_SIZE || lc > MAX_PIN_SIZE) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        if (!pin.isValidated()) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        pin.update(buffer, ISO7816.OFFSET_CDATA, lc);
    }

    // Receives data in P1_INIT and P1_UPDATE and places it in the TMP_BUFFER
    // Verifies authenticity of all received data by verifying the signature passed in P1_FINAL with the intermediate public key
    // If verification is successful the verified data can be found in TMP_BUFFER with size TMP_BUFFER_SIZE
    private void receiveLargeSignedData(byte[] buffer) {

        short dataLength = (short) (buffer[ISO7816.OFFSET_LC] & 0xFF);

        if (buffer[ISO7816.OFFSET_P1] == P1_INIT) {
            TMP_BUFFER_LEN = 0;
            TMP_BUFFER_OFFSET = 0;
            Util.arrayCopyNonAtomic(buffer, (short) ISO7816.OFFSET_CDATA, TMP_BUFFER, (short) 0, dataLength);
            TMP_BUFFER_OFFSET = dataLength;
            ISOException.throwIt(ISO7816.SW_NO_ERROR);
        } else if (buffer[ISO7816.OFFSET_P1] == P1_UPDATE) {
            if (TMP_BUFFER_OFFSET + dataLength > TMP_BUFFER.length) {
                ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
            }
            Util.arrayCopyNonAtomic(buffer, ISO7816.OFFSET_CDATA, TMP_BUFFER, TMP_BUFFER_OFFSET, dataLength);
            TMP_BUFFER_OFFSET += buffer[ISO7816.OFFSET_LC];
            ISOException.throwIt(ISO7816.SW_NO_ERROR);
        } else if (buffer[ISO7816.OFFSET_P1] == P1_FINAL) {
            Signature signatureAlg = Signature.getInstance(Signature.ALG_ECDSA_SHA_256, false);
            signatureAlg.init(intermediatePublicKey, Signature.MODE_VERIFY);
            boolean result = signatureAlg.verify(TMP_BUFFER, (short) 0, TMP_BUFFER_OFFSET, buffer, ISO7816.OFFSET_CDATA, dataLength);
            if (!result) {
                TMP_BUFFER_OFFSET = 0;
                ISOException.throwIt(SW.INVALID_SIGNATURE);
            }
            TMP_BUFFER_LEN = TMP_BUFFER_OFFSET;
            // Signature verified, continue with processing of the actual data (don't return here)
        } else {
            TMP_BUFFER_OFFSET = 0;
            TMP_BUFFER_LEN = 0;
            ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
        }
    }

    /////// CSR Related

    private short createCSR(byte[] buffer, short dataOffset, short dataLen) {
        // We allocate some transient buffers from within this, and other functions that shall
        // preferably be called only once during the applet lifecycle. This is probably not the
        // proper way to manage RAM on an actual card but for the hackaton and the vTEE we ignore
        // this for now...
        byte[] commonName = JCSystem.makeTransientByteArray(dataLen, JCSystem.CLEAR_ON_RESET);
        short commonNameLen = dataLen;
        Util.arrayCopyNonAtomic(buffer, dataOffset, commonName, (short) 0, dataLen);

        // Set the common name in the session data to match CN when we get the certificate
        Util.arrayCopyNonAtomic(commonName, (short) 0, SCRATCH_BUFFER, (short) 0, commonNameLen);
        SCRATCH_BUFFER_LEN = commonNameLen;

        // Generate new key pair
        // Note: If we keep the EC keypair as a static class member it fails to be deserialized upon
        // load (does not happen if the keypair is RSA)
        // We are ok with leaking the key pair for this hackathon
        KeyPair keyPair = new KeyPair(publicKey, privateKey);
        keyPair.genKeyPair();

        byte[] subject = JCSystem.makeTransientByteArray((short) 256, JCSystem.CLEAR_ON_RESET);
        short subjectLen = CSR.createSubject(commonName, (short) 0, commonNameLen, subject, (short) 0);

        byte[] subjectKeyInfo = JCSystem.makeTransientByteArray((short) 256, JCSystem.CLEAR_ON_RESET);
        short subjectKeyInfoLen = CSR.createSubjectPublicKeyInfo(subjectKeyInfo, (short) 0, publicKey);

        byte[] version = new byte[]{0x02, 0x01, 0x00};

        byte[] certificationRequestInfo = JCSystem.makeTransientByteArray((short) (subjectKeyInfoLen + subjectLen + version.length + 2), JCSystem.CLEAR_ON_RESET);
        short offset = 0;
        certificationRequestInfo[offset++] = 0x30;
        certificationRequestInfo[offset++] = (byte) (version.length + subjectKeyInfoLen + subjectLen);
        Util.arrayCopyNonAtomic(version, (short) 0, certificationRequestInfo, offset, (short) version.length);
        offset += (short) version.length;
        Util.arrayCopyNonAtomic(subject, (short) 0, certificationRequestInfo, offset, subjectLen);
        offset += subjectLen;
        Util.arrayCopyNonAtomic(subjectKeyInfo, (short) 0, certificationRequestInfo, offset, subjectKeyInfoLen);
        offset += subjectKeyInfoLen;
        short certificationRequestInfoLen = offset;

        // Sign TBS
        Signature sig = Signature.getInstance(Signature.ALG_ECDSA_SHA_256, false);
        sig.init(privateKey, Signature.MODE_SIGN);
        byte[] signature = JCSystem.makeTransientByteArray((short) (255), JCSystem.CLEAR_ON_RESET);
        signature[0] = 0x03; // BIT STRING
        short signatureLen = sig.sign(certificationRequestInfo, (short) 0, certificationRequestInfoLen, signature, (short) 3);
        signature[1] = (byte) (signatureLen + 1);
        signature[2] = 0x00; // Unused bits
        signatureLen += 3; // BIT STRING overhead

        // Construct everything
        byte[] signatureAlgorithm = new byte[]{0x30, 0x0A, 0x06, 0x08, 0x2A, (byte) 0x86, 0x48, (byte) 0xCE, 0x3D, 0x04, 0x03, 0x02}; // ecdsaWithSHA256
        byte[] CSR = JCSystem.makeTransientByteArray((short) (500), JCSystem.CLEAR_ON_RESET);
        offset = 0;
        CSR[offset++] = 0x30; // SEQUENCE
        short CSRLen = (short) (certificationRequestInfoLen + signatureAlgorithm.length + signatureLen);
        offset = DER.writeLength(CSR, offset, CSRLen);
        Util.arrayCopyNonAtomic(certificationRequestInfo, (short) 0, CSR, offset, certificationRequestInfoLen);
        offset += certificationRequestInfoLen;
        Util.arrayCopyNonAtomic(signatureAlgorithm, (short) 0, CSR, offset, (short) signatureAlgorithm.length);
        offset += (short) signatureAlgorithm.length;
        Util.arrayCopyNonAtomic(signature, (short) 0, CSR, offset, signatureLen);
        offset += signatureLen;

        // Double sign the CSR to ensure that it comes from a trusted applet
        sig.init(privateCommonKey, Signature.MODE_SIGN);
        // Add signature first in the response (hackish design but...)
        signatureLen = sig.sign(CSR, (short) 0, offset, buffer, (short) 0);
        // Put the CSR in the temp buffer to be fetched with GET RESPONSE
        Util.arrayCopyNonAtomic(CSR, (short) 0, TMP_BUFFER, (short) 0, offset);
        TMP_BUFFER_OFFSET = 0;
        TMP_BUFFER_LEN = offset;

        return signatureLen;
    }

    private void fulfillCSR(byte[] buffer, short dataOffset, short dataLen) {

        boolean foundName;
        boolean foundPublicKey;

        foundName = CSR.containsCN(buffer, dataOffset, dataLen, SCRATCH_BUFFER, SCRATCH_BUFFER_LEN);

        byte[] subjectPublicKey = JCSystem.makeTransientByteArray((short) 66, JCSystem.CLEAR_ON_RESET);
        short subjectPublicKeyLen = publicKey.getW(subjectPublicKey, (short) 0);

        foundPublicKey = CSR.containsSubjectPublicKey(buffer, (short) 0, dataLen, subjectPublicKey, subjectPublicKeyLen);

        if (!foundName || !foundPublicKey) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }

        boolean isValid = CSR.verifyTBS(buffer, (short) 0, dataLen, intermediatePublicKey);
        if (!isValid) {
            ISOException.throwIt(SW.INVALID_SIGNATURE);
        }
        // Cert looks good - store it and activate the instance (this should only be made once)
        voterCertificate = new byte[dataLen];
        Util.arrayCopyNonAtomic(buffer, (short) 0, voterCertificate, (short) 0, dataLen);
    }

    /////// Election and voting related

    private void registerSubmission() {

        short offset = 0;
        int electionId = Utils.getIntFromByteArray(TMP_BUFFER, offset);
        offset += 4;
        short candidateOffset = offset;
        offset += Candidate.MAX_CANDIDATE_LEN;
        long timestamp = Utils.getLongFromByteArray(TMP_BUFFER, offset);

        short index = -1;
        for (short i = 0; i < elections.length; i++) {
            if (elections[i].electionID == electionId) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            ISOException.throwIt(SW.ELECTION_NOT_FOUND);
        }

        if (elections[index].timestamp != timestamp) {
            ISOException.throwIt(SW.ELECTION_INTEGRITY_FAILED);
        }
        byte votedIndex = elections[index].voteIndex;

        for (int i = 0; i < Candidate.MAX_CANDIDATE_LEN; i++) {
            if (TMP_BUFFER[candidateOffset + i] != elections[index].candidates[votedIndex].option[i]) {
                ISOException.throwIt(ISO7816.SW_DATA_INVALID);
            }
        }

        elections[index].submitted = 1;
    }

    private short getElections(byte[] buffer) {
        short offset = 1;
        byte count = 0;

        for (int i = 0; i < elections.length; i++) {
            if (elections[i].active) {
                offset = Utils.copyIntToByteArray(elections[i].electionID, buffer, offset);
                count++;
            }
        }
        buffer[0] = count;
        return offset;
    }

    private void registerElection() {
        // First make sure we have space for the new election
        byte index = -1;
        for (byte i = 0; i < elections.length; i++) {
            if (!elections[i].active) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            ISOException.throwIt(ISO7816.SW_FILE_FULL);
        }
        elections[index].reset();

        short offset = 0;
        // election id
        elections[index].electionID = Utils.getIntFromByteArray(TMP_BUFFER, offset);
        offset += 4;

        // title
        Util.arrayCopyNonAtomic(TMP_BUFFER, offset, elections[index].title, (short) 0, (short) Election.MAX_TITLE_LENGTH);
        offset += Election.MAX_TITLE_LENGTH;

        // expiration
        elections[index].expiration = Utils.getLongFromByteArray(TMP_BUFFER, offset);
        offset += 8;

        // options lengths
        byte numOptions = TMP_BUFFER[offset];
        if (numOptions > Election.MAX_OPTIONS) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        offset += 1;
        for (int i = 0; i < numOptions; i++) {
            CANDIDATE_SIZES[i] = Utils.getShortFromByteArray(TMP_BUFFER, offset);
            offset += 2;
        }
        for (int i = 0; i < numOptions; i++) {
            Util.arrayCopyNonAtomic(TMP_BUFFER, offset, elections[index].candidates[i].option, (short) 0, CANDIDATE_SIZES[i]);
            offset += CANDIDATE_SIZES[i];
            elections[index].candidates[i].active = true;
        }

        elections[index].active = true;
    }

    private short vote(byte[] buffer, short dataOffset) {

        short offset = dataOffset;

        byte pinOffset = (byte) (buffer[ISO7816.OFFSET_P1] + dataOffset);
        byte pinSize = buffer[ISO7816.OFFSET_P2];
        if (!pin.check(buffer, pinOffset, pinSize)) {
            SW.pinFailed(pin.getTriesRemaining());
        }

        int electionId = Utils.getIntFromByteArray(buffer, offset);
        offset += 4;
        long timestamp = Utils.getLongFromByteArray(buffer, offset);
        offset += 8;
        short voteOffset = offset;

        // find the matching election...
        short index = -1;
        for (short i = 0; i < elections.length; i++) {
            if (elections[i].electionID == electionId) {
                index = i;
                break;
            }
        }
        // filed to find the election
        if (index < 0) {
            ISOException.throwIt(SW.ELECTION_NOT_FOUND);
        }
        boolean voted = false;
        // find the macthing vote and vote
        for (short i = 0; i < Election.MAX_OPTIONS; i++) {
            if (Utils.arraysAreEqual(buffer, voteOffset, elections[index].candidates[i].option, (short) 0, (short) elections[index].candidates[i].option.length)) {
                elections[index].voteIndex = (byte) i;
                elections[index].timestamp = timestamp;
                voted = true;
                break;
            }
        }
        if (!voted) {
            // failed to find the vote option
            ISOException.throwIt(SW.NOT_YET_VOTED);
        }

        // now to construct the signed response
        TMP_BUFFER_LEN = 0;
        TMP_BUFFER_OFFSET = 0;

        // [ElectionID, vote, timestamp, voterCert] signature
        TMP_BUFFER_OFFSET = Utils.copyIntToByteArray(electionId, TMP_BUFFER, TMP_BUFFER_OFFSET);
        TMP_BUFFER_OFFSET = Util.arrayCopyNonAtomic(elections[index].candidates[elections[index].voteIndex].option, (short) 0, TMP_BUFFER, TMP_BUFFER_OFFSET, Candidate.MAX_CANDIDATE_LEN);
        TMP_BUFFER_OFFSET = Utils.copyLongToByteArray(timestamp, TMP_BUFFER, TMP_BUFFER_OFFSET);
        TMP_BUFFER_OFFSET = Util.arrayCopyNonAtomic(voterCertificate, (short) 0, TMP_BUFFER, TMP_BUFFER_OFFSET, (short) voterCertificate.length);
        TMP_BUFFER_LEN = TMP_BUFFER_OFFSET;

        // return the signature in the APDU buffer and the vote in the next GET RESPONSE
        Signature signatureAlg = Signature.getInstance(Signature.ALG_ECDSA_SHA_256, false);
        signatureAlg.init(privateKey, Signature.MODE_SIGN);
        short signatureLen = signatureAlg.sign(TMP_BUFFER, (short) 0, TMP_BUFFER_LEN, buffer, (short) 0);

        // keep the signature for future reference
        Util.arrayCopyNonAtomic(buffer, (short) 0, elections[index].signature, (short) 0, signatureLen);
        elections[index].signatureSize = signatureLen;

        return signatureLen;
    }

    private short getElection(byte[] buffer, short dataOffset) {
        int electionId = Utils.getIntFromByteArray(buffer, dataOffset);
        short index = -1;
        for (short i = 0; i < elections.length; i++) {
            if (elections[i].electionID == electionId) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            ISOException.throwIt(SW.ELECTION_NOT_FOUND);
        }
        // time to reconstruct the election data
        // with the current constants we should fit within a single APDU
        short offset = 0;

        offset = Utils.copyIntToByteArray(elections[index].electionID, buffer, offset);
        offset = Util.arrayCopyNonAtomic(elections[index].title, (short) 0, buffer, offset, Election.MAX_TITLE_LENGTH);
        offset = Utils.copyLongToByteArray(elections[index].expiration, buffer, offset);

        byte numOptions = elections[index].getNumberOfOptions();
        buffer[offset] = numOptions;
        offset += 1;

        for (byte i = 0; i < numOptions; i++) {
            CANDIDATE_SIZES[i] = elections[index].candidates[i].getCandidateLen();
        }

        // encode the option size
        for (short i = 0; i < numOptions; i++) {
            buffer[offset] = (byte) (CANDIDATE_SIZES[i] >> 8);
            buffer[offset + 1] = (byte) (CANDIDATE_SIZES[i] & 0xFF);
            offset += 2;
        }

        for (byte i = 0; i < numOptions; i++) {
            offset = Util.arrayCopyNonAtomic(elections[index].candidates[i].option, (short) 0, buffer, offset, CANDIDATE_SIZES[i]);
        }

        return offset;
    }

    private short getVoteReceipt(byte[] buffer, byte dataOffset) {
        // find the election
        int electionId = Utils.getIntFromByteArray(buffer, dataOffset);
        short index = -1;
        for (short i = 0; i < elections.length; i++) {
            if (elections[i].electionID == electionId) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            ISOException.throwIt(SW.ELECTION_NOT_FOUND);
        }
        // make sure the vote is laid
        if (elections[index].voteIndex < 0) {
            ISOException.throwIt(SW.NOT_YET_VOTED);
        }

        // now to construct the signed response
        TMP_BUFFER_LEN = 0;
        TMP_BUFFER_OFFSET = 0;

        // Use same encoding as during the original vote..
        TMP_BUFFER_OFFSET = Utils.copyIntToByteArray(elections[index].electionID, TMP_BUFFER, TMP_BUFFER_OFFSET);
        TMP_BUFFER_OFFSET = Util.arrayCopyNonAtomic(elections[index].candidates[elections[index].voteIndex].option, (short) 0, TMP_BUFFER, TMP_BUFFER_OFFSET, Candidate.MAX_CANDIDATE_LEN);
        TMP_BUFFER_OFFSET = Utils.copyLongToByteArray(elections[index].timestamp, TMP_BUFFER, TMP_BUFFER_OFFSET);
        TMP_BUFFER_OFFSET = Util.arrayCopyNonAtomic(voterCertificate, (short) 0, TMP_BUFFER, TMP_BUFFER_OFFSET, (short) voterCertificate.length);
        TMP_BUFFER_LEN = TMP_BUFFER_OFFSET;

        // Return the signature in the APDU buffer and the vote in the next GET RESPONSE
        Util.arrayCopyNonAtomic(elections[index].signature, (short) 0, buffer, (short) 0, elections[index].signatureSize);
        // Also add the indicator whether the vote has been submitted or not
        buffer[elections[index].signatureSize] = elections[index].submitted;
        return (short) (elections[index].signatureSize + 1);
    }
}
