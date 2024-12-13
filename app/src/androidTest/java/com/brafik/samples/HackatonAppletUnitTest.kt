package com.brafik.samples

import com.licel.jcardsim.utils.AIDUtil
import com.brafik.samples.model.AppletSigned
import com.brafik.samples.applet.HackatonApplet
import com.brafik.samples.model.ElectionData
import com.brafik.samples.model.SignedCsr
import com.brafik.samples.model.VoteReceipt
import com.brafik.samples.model.commandAPDU
import com.brafik.samples.smartcardio.CardSimulator
import com.brafik.samples.smartcardio.ResponseAPDU
import com.brafik.samples.utils.getInt
import com.brafik.samples.utils.getLong
import com.brafik.samples.utils.toByteArray
import com.brafik.samples.utils.toNullTerminatedString
import javacard.framework.AID
import javacard.framework.ISO7816
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.security.Security
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate


class HackatonAppletUnitTest {

    companion object {
        private const val CLA_HACKATHON: Byte = 0x00;
        private const val INS_CREATE_CSR: Byte = 0x10;
        private const val INS_FULFILL_CSR: Byte = 0x20;
        private const val INS_UPDATE_PIN: Byte = 0x60;
        private const val INS_VERIFY_PIN: Byte = 0x61;
        private const val INS_GET_CERTIFICATE: Byte = 0x71
        private const val INS_GET_ELECTION: Byte = 0x72
        private const val INS_GET_ELECTIONS: Byte = 0x73
        private const val INS_REGISTER_ELECTION: Byte = 0x76
        private const val INS_VOTE: Byte = 0x77
        private const val INS_GET_VOTE_RECEIPT: Byte = 0x78
        private const val INS_GET_RESPONSE: Byte = 0xC0.toByte();

        private const val P1_INIT: Byte = 1;
        private const val P1_UPDATE: Byte = 2;
        private const val P1_FINAL: Byte = 3;

        private const val APDU_CHUNK_SIZE = 240
        private const val COMMON_NAME = "Crunchy McCrunchface"
        private const val PIN = "123456"
        private const val UPD_PIN = "abcdefgh"

        private const val ELECTION_ID = 1234578
        private const val ELECTION_TITLE = "Sweden 2024"
        private const val ELECTION_OPTION = "Option 2X"
        private val ELECTION_OPTIONS = listOf("Option 1", "Opt 2", ELECTION_OPTION, "Option the third")
        private val ELECTION_MOCK_SIGNATURE = ByteArray(70)


        // Note: cert card sim generates the same keys so the hard coded cert can be used
        // to fulfill the CSR. Just don't change the common name in the call above...
        val X509CERT = listOf(
            0x30, 0x82, 0x01, 0xA5, 0x30, 0x82, 0x01, 0x4B,
            0xA0, 0x03, 0x02, 0x01, 0x02, 0x02, 0x08, 0x5D, 0x3C, 0xA3, 0x79, 0x7C,
            0x44, 0xC0, 0x45, 0x30, 0x0A, 0x06, 0x08, 0x2A, 0x86, 0x48, 0xCE, 0x3D,
            0x04, 0x03, 0x02, 0x30, 0x6E, 0x31, 0x0B, 0x30, 0x09, 0x06, 0x03, 0x55,
            0x04, 0x06, 0x13, 0x02, 0x53, 0x45, 0x31, 0x0E, 0x30, 0x0C, 0x06, 0x03,
            0x55, 0x04, 0x08, 0x13, 0x05, 0x53, 0x6B, 0x61, 0x6E, 0x65, 0x31, 0x0E,
            0x30, 0x0C, 0x06, 0x03, 0x55, 0x04, 0x07, 0x13, 0x05, 0x4D, 0x61, 0x6C,
            0x6D, 0x6F, 0x31, 0x23, 0x30, 0x21, 0x06, 0x03, 0x55, 0x04, 0x0A, 0x13,
            0x1A, 0x43, 0x72, 0x75, 0x6E, 0x63, 0x68, 0x66, 0x69, 0x73, 0x68, 0x20,
            0x44, 0x69, 0x67, 0x69, 0x74, 0x61, 0x6C, 0x20, 0x43, 0x61, 0x73, 0x68,
            0x20, 0x41, 0x42, 0x31, 0x1A, 0x30, 0x18, 0x06, 0x03, 0x55, 0x04, 0x03,
            0x13, 0x11, 0x69, 0x6E, 0x74, 0x65, 0x72, 0x6D, 0x65, 0x64, 0x69, 0x61,
            0x74, 0x65, 0x2D, 0x63, 0x65, 0x72, 0x74, 0x30, 0x1E, 0x17, 0x0D, 0x32,
            0x34, 0x31, 0x32, 0x30, 0x35, 0x31, 0x33, 0x31, 0x39, 0x30, 0x30, 0x5A,
            0x17, 0x0D, 0x32, 0x35, 0x31, 0x32, 0x30, 0x35, 0x31, 0x33, 0x31, 0x39,
            0x30, 0x30, 0x5A, 0x30, 0x1F, 0x31, 0x1D, 0x30, 0x1B, 0x06, 0x03, 0x55,
            0x04, 0x03, 0x13, 0x14, 0x43, 0x72, 0x75, 0x6E, 0x63, 0x68, 0x79, 0x20,
            0x4D, 0x63, 0x43, 0x72, 0x75, 0x6E, 0x63, 0x68, 0x66, 0x61, 0x63, 0x65,
            0x30, 0x59, 0x30, 0x13, 0x06, 0x07, 0x2A, 0x86, 0x48, 0xCE, 0x3D, 0x02,
            0x01, 0x06, 0x08, 0x2A, 0x86, 0x48, 0xCE, 0x3D, 0x03, 0x01, 0x07, 0x03,
            0x42, 0x00, 0x04, 0x2B, 0x49, 0x5A, 0x9B, 0x41, 0x42, 0xDC, 0x31, 0x76,
            0x24, 0x62, 0x6A, 0xD1, 0x08, 0xD4, 0x89, 0x6C, 0x12, 0xA9, 0x7A, 0xF1,
            0xA1, 0x37, 0x2E, 0x9A, 0x7B, 0x0F, 0x29, 0xAD, 0xCA, 0xEB, 0x49, 0xDE,
            0xE3, 0x77, 0xC9, 0x7F, 0xBB, 0x17, 0xB6, 0x14, 0x80, 0xE8, 0x57, 0xF5,
            0xCE, 0x72, 0x40, 0x84, 0x88, 0xE2, 0x76, 0x36, 0x19, 0x15, 0x9D, 0x03,
            0x2D, 0x3B, 0xE0, 0x91, 0x26, 0x6D, 0xD6, 0xA3, 0x22, 0x30, 0x20, 0x30,
            0x1E, 0x06, 0x09, 0x60, 0x86, 0x48, 0x01, 0x86, 0xF8, 0x42, 0x01, 0x0D,
            0x04, 0x11, 0x16, 0x0F, 0x78, 0x63, 0x61, 0x20, 0x63, 0x65, 0x72, 0x74,
            0x69, 0x66, 0x69, 0x63, 0x61, 0x74, 0x65, 0x30, 0x0A, 0x06, 0x08, 0x2A,
            0x86, 0x48, 0xCE, 0x3D, 0x04, 0x03, 0x02, 0x03, 0x48, 0x00, 0x30, 0x45,
            0x02, 0x20, 0x01, 0xEB, 0x18, 0x46, 0xEA, 0x82, 0xC0, 0x6F, 0x9A, 0x10,
            0x4B, 0xCB, 0x17, 0x5C, 0xA5, 0x67, 0x33, 0x4B, 0x87, 0x5B, 0xFA, 0x9A,
            0x05, 0xB0, 0xCF, 0x03, 0x96, 0x6D, 0x59, 0xAE, 0xEC, 0x02, 0x02, 0x21,
            0x00, 0x82, 0x15, 0x78, 0x83, 0xB3, 0x13, 0x22, 0x98, 0xD5, 0x50, 0x30,
            0xCB, 0x1B, 0x94, 0x8C, 0x89, 0x0D, 0x10, 0x2D, 0x8F, 0xF0, 0x2F, 0x65,
            0x86, 0x72, 0xDA, 0xE5, 0x66, 0xB3, 0x7C, 0xE9, 0x5A
        ).map { it.toByte() }.toByteArray()
        private lateinit var aid: AID
        private lateinit var simulator: CardSimulator

        @BeforeClass
        @JvmStatic
        fun setup() {
            aid = AIDUtil.create("F000000001")
        }
    }

    @Before
    fun setUp() {
        simulator = CardSimulator()
        simulator.installApplet(aid, HackatonApplet::class.java)
        simulator.selectApplet(aid)
    }

    @Test
    fun createCSR() {
        createSignedCSR(COMMON_NAME, PIN)

        fulfillCSR(X509CERT)

        registerElection(
            ELECTION_ID,
            System.currentTimeMillis(),
            ELECTION_TITLE,
            ELECTION_OPTIONS,
            ELECTION_MOCK_SIGNATURE
        )

        registerVote(ELECTION_ID, ELECTION_OPTION, PIN)

        getElectionFromCard(ELECTION_ID)

        getElectionsFromCard();

        getVoteFromCard(ELECTION_ID)

        getVoterCert()

        assertEquals(false, verifyUserPIN(UPD_PIN))
        assertEquals(true, verifyUserPIN(PIN))
        updateUserPIN(UPD_PIN)
        assertEquals(false, verifyUserPIN(PIN))
        assertEquals(true, verifyUserPIN(UPD_PIN))
        updateUserPIN(PIN)
    }

    private fun createSignedCSR(commonName: String, pin: String): SignedCsr {
        // CLA      0x00
        // INS      0x20
        // P1       Offset where PIN data begins
        // P2       0
        // Lc
        // Data     Common Name (no null terminator) + PIN (at offset P1)

        val commonNameBytes = commonName.toByteArray()
        val cdata = commonNameBytes + pin.toByteArray()
        val p1 = commonNameBytes.size.toByte() // set PIN offset
        val cmdCreateCSR = commandAPDU(CLA_HACKATHON, INS_CREATE_CSR, p1, 0, cdata)

        var result = simulator.transmitCommand(cmdCreateCSR)
        var responseApdu = ResponseAPDU(result)

        // Signature is returned in the first chunk, the CSR in the second
        assertEquals(0x61.toByte(), responseApdu.sW1.toByte())

        val signature = responseApdu.data
        var csr = ByteArray(0)

        // CSR is returned in the first chunk, the signature in the second
        while (responseApdu.sW1 == 0x61) {
            // How to create a Case 2 cAPDU?
            val cmdGetResponse = commandAPDU(CLA_HACKATHON, INS_GET_RESPONSE, 0, 0)
            result = simulator.transmitCommand(cmdGetResponse)
            responseApdu = ResponseAPDU(result)
            csr = responseApdu.data
        }
        assertEquals(ISO7816.SW_NO_ERROR, responseApdu.sW.toShort())
        return SignedCsr(csr, signature)
    }

    private fun fulfillCSR(x509cert: ByteArray) {
        var offset = 0
        val totalLength = x509cert.size

        // Send the first chunk with P1_INIT
        var cmd = commandAPDU(
            CLA_HACKATHON,
            INS_FULFILL_CSR,
            P1_INIT,
            0,
            x509cert.copyOfRange(offset, offset + APDU_CHUNK_SIZE)
        )
        var result = simulator.transmitCommand(cmd)
        var responseApdu = ResponseAPDU(result)
        assertEquals(ISO7816.SW_NO_ERROR, responseApdu.sW.toShort())
        offset += APDU_CHUNK_SIZE

        // Send subsequent chunks with P1_UPDATE
        while (offset + APDU_CHUNK_SIZE < totalLength) {
            cmd = commandAPDU(
                CLA_HACKATHON,
                INS_FULFILL_CSR,
                P1_UPDATE,
                0,
                x509cert.copyOfRange(offset, offset + APDU_CHUNK_SIZE)
            )
            result = simulator.transmitCommand(cmd)
            responseApdu = ResponseAPDU(result)
            assertEquals(ISO7816.SW_NO_ERROR, responseApdu.sW.toShort())
            offset += APDU_CHUNK_SIZE
        }

        // Send the final chunk with P1_FINAL
        cmd = commandAPDU(
            CLA_HACKATHON,
            INS_FULFILL_CSR,
            P1_FINAL,
            0,
            x509cert.copyOfRange(offset, totalLength)
        )
        result = simulator.transmitCommand(cmd)
        responseApdu = ResponseAPDU(result)
        assertEquals(ISO7816.SW_NO_ERROR, responseApdu.sW.toShort())
    }

    private fun verifyUserPIN(pin: String): Boolean {
        val cmdVerifyPIN = commandAPDU(CLA_HACKATHON, INS_VERIFY_PIN, 0, 0, pin.toByteArray())
        val result = simulator.transmitCommand(cmdVerifyPIN)
        val responseApdu = ResponseAPDU(result)
        // val remainingAttempts = responseApdu.sW2

        assertTrue(responseApdu.sW1 == 0x90 || responseApdu.sW1 == 0x69)
        return responseApdu.sW1 == 0x90;
    }

    private fun updateUserPIN(pin: String) {
        val cmdChangePIN = commandAPDU(CLA_HACKATHON, INS_UPDATE_PIN, 0, 0, pin.toByteArray())
        val result = simulator.transmitCommand(cmdChangePIN)
        val responseApdu = ResponseAPDU(result)
        assertEquals(ISO7816.SW_NO_ERROR, responseApdu.sW.toShort())
    }

    private fun registerElection(
        electionId: Int,
        expiration: Long,
        title: String,
        options: List<String>,
        signature: ByteArray
    ) {
        val electionData = ElectionData(
            electionId,
            title,
            options,
            expiration
        )

        val buffer = electionData.toByteArray()

        // Loop with P2_UPDATE if the election is a big one...
        var cmdRegisterElection =
            commandAPDU(CLA_HACKATHON, INS_REGISTER_ELECTION, P1_INIT, 0, buffer)
        var result = simulator.transmitCommand(cmdRegisterElection)
        var responseApdu = ResponseAPDU(result)
        assertEquals(ISO7816.SW_NO_ERROR, responseApdu.sW.toShort())

        cmdRegisterElection =
            commandAPDU(CLA_HACKATHON, INS_REGISTER_ELECTION, P1_FINAL, 0, signature)
        result = simulator.transmitCommand(cmdRegisterElection)
        responseApdu = ResponseAPDU(result)
        assertEquals(ISO7816.SW_NO_ERROR, responseApdu.sW.toShort())
    }

    private fun getVoteFromCard(electionId: Int): AppletSigned<VoteReceipt> {
        val buffer = electionId.toByteArray()
        val cmdGetVote = commandAPDU(CLA_HACKATHON, INS_GET_VOTE_RECEIPT, 0, 0, buffer)
        var result = simulator.transmitCommand(cmdGetVote)
        var responseApdu = ResponseAPDU(result)

        assertEquals(0x61.toByte(), responseApdu.sW1.toByte())

        val signature = responseApdu.data
        var voteData = ByteArray(0)

        while (responseApdu.sW1 == 0x61) {
            val cmdGetResponse = commandAPDU(CLA_HACKATHON, INS_GET_RESPONSE, 0, 0)
            result = simulator.transmitCommand(cmdGetResponse)
            responseApdu = ResponseAPDU(result)
            voteData += responseApdu.data
        }
        assertEquals(ISO7816.SW_NO_ERROR, responseApdu.sW.toShort())

        var offset = 0
        val resElectId = voteData.getInt(offset);
        offset += 4
        val resVoteOption = voteData.copyOfRange(offset, offset + 20)
        offset += 20
        val resTimestamp = voteData.getLong(offset)
        offset += 8
        val cerLen = voteData.size - offset
        val resCert = voteData.copyOfRange(offset, offset + cerLen)

        return AppletSigned(
            data = VoteReceipt(
                resElectId,
                resVoteOption.toNullTerminatedString(),
                resTimestamp
            ),
            resCert,
            signature
        )
    }

    private fun getElectionFromCard(electionId: Int): ElectionData {
        val buffer = electionId.toByteArray()
        val cmdGetElection = commandAPDU(CLA_HACKATHON, INS_GET_ELECTION, 0, 0, buffer)
        val result = simulator.transmitCommand(cmdGetElection)
        val responseApdu = ResponseAPDU(result)

        assertEquals(ISO7816.SW_NO_ERROR, responseApdu.sW.toShort())

        return ElectionData.fromByteArray(responseApdu.data)
    }

    private fun getElectionsFromCard(): List<Int> {
        val cmdGetElections = commandAPDU(CLA_HACKATHON, INS_GET_ELECTIONS, 0, 0)
        val result = simulator.transmitCommand(cmdGetElections)
        val responseApdu = ResponseAPDU(result)

        assertEquals(ISO7816.SW_NO_ERROR, responseApdu.sW.toShort())
        var offset = 0;
        val nofElections = responseApdu.data[0];
        offset++;

        val elections = mutableListOf<Int>()
        for (i in 0 until nofElections) {
            val electionId = responseApdu.data.getInt(offset)
            elections.add(electionId)
            offset += 4
        }

        return elections
    }

    private fun getVoterCert(): X509Certificate {
        val cmdGetCert = commandAPDU(CLA_HACKATHON, INS_GET_CERTIFICATE, 0, 0)
        var result = simulator.transmitCommand(cmdGetCert)
        var responseApdu = ResponseAPDU(result)

        assertEquals(responseApdu.sW1, 0x61)

        var certificateBytes = responseApdu.data
        while (responseApdu.sW1 == 0x61) {
            val cmdGetResponse = commandAPDU(CLA_HACKATHON, INS_GET_RESPONSE, 0, 0)
            result = simulator.transmitCommand(cmdGetResponse)
            responseApdu = ResponseAPDU(result)
            certificateBytes += responseApdu.data
        }
        assertEquals(ISO7816.SW_NO_ERROR, responseApdu.sW.toShort())

        Security.addProvider(BouncyCastleProvider())
        val factory = CertificateFactory.getInstance("X.509")
        val certificate: X509Certificate = factory.generateCertificate(
            ByteArrayInputStream(certificateBytes)
        ) as X509Certificate

        return certificate
    }

    private fun registerVote(electionId: Int, option: String, pin: String): Pair<ByteArray, ByteArray> {
        var data = ByteArray(0)
        data += electionId.toByteArray()
        data += System.currentTimeMillis().toByteArray()

        // MAX_OPTION_LENGTH
        val paddedOption = option.toByteArray(StandardCharsets.UTF_8).copyOf(20)
        data += paddedOption
        val pinOffset: Byte = data.size.toByte()
        val pinSize: Byte = pin.toByteArray().size.toByte()
        data += pin.toByteArray()

        // Signature is returned in the first chunk, the Vote receipt in the second
        val cmdVote = commandAPDU(CLA_HACKATHON, INS_VOTE, pinOffset, pinSize, data)
        var result = simulator.transmitCommand(cmdVote)
        var responseApdu = ResponseAPDU(result)
        assertEquals(0x61.toByte(), responseApdu.sW1.toByte())

        val signature = responseApdu.data
        var voteReceipt = ByteArray(0)

        // Get the receipt
        while (responseApdu.sW1 == 0x61) {
            val cmdGetResponse = commandAPDU(CLA_HACKATHON, INS_GET_RESPONSE, 0, 0)
            result = simulator.transmitCommand(cmdGetResponse)
            responseApdu = ResponseAPDU(result)
            voteReceipt += responseApdu.data
        }
        assertEquals(ISO7816.SW_NO_ERROR, responseApdu.sW.toShort())
        return Pair(voteReceipt, signature)
    }


    @After
    fun tearDown() {
        simulator.deleteApplet(aid)
    }
}