package com.brafik.samples.applet

import com.brafik.samples.model.AppletSigned
import com.brafik.samples.model.ElectionData
import com.brafik.samples.model.IssuerSigned
import com.brafik.samples.model.RegisteredVote
import com.brafik.samples.model.SignedCsr
import com.brafik.samples.model.VoteReceipt
import com.brafik.samples.model.commandAPDU
import com.brafik.samples.smartcardio.CardSimulator
import com.brafik.samples.smartcardio.ResponseAPDU
import com.brafik.samples.utils.toByteArray
import com.thedeanda.lorem.LoremIpsum
import javacard.framework.ISO7816
import java.nio.charset.StandardCharsets
import javax.inject.Inject

class VoteAppletImpl @Inject constructor(private val simulator: CardSimulator) : VoteApplet {
    companion object {
        private const val CLA_HACKATHON: Byte = 0x00
        private const val INS_CREATE_CSR: Byte = 0x10
        private const val INS_FULFILL_CSR: Byte = 0x20
        private const val INS_GET_STATUS: Byte = 0x70
        private const val INS_GET_RESPONSE: Byte = 0xC0.toByte()
        private const val INS_REGISTER_ELECTION: Byte = 0x76
        private const val INS_REGISTER_SUBMISSION: Byte = 0x79
        private const val INS_GET_ELECTION: Byte = 0x72
        private const val INS_VOTE: Byte = 0x77
        private const val INS_GET_VOTE_RECEIPT: Byte = 0x78
        private const val APDU_COMMAND_CHUNK_SIZE = 250

        private const val ACTIVATED_STATUS: Byte = 1

        private const val P1_INIT: Byte = 1
        private const val P1_UPDATE: Byte = 2
        private const val P1_FINAL: Byte = 3
    }

    override fun createCsr(pin: String): SignedCsr {
        val commonName = LoremIpsum().name.toByteArray()
        val data = commonName + pin.toByteArray()
        val p1 = commonName.size.toByte() // use P1 as offset
        val cmdCreateCSR = commandAPDU(CLA_HACKATHON, INS_CREATE_CSR, p1, 0, data)
        val result = simulator.transmitCommand(cmdCreateCSR)
        val responseApdu = ResponseAPDU(result)
        val signature = responseApdu.data
        var csr = ByteArray(0)

        if (responseApdu.sW1.toByte() == 0x61.toByte()) {
            val cmdGetResponse = commandAPDU(CLA_HACKATHON, INS_GET_RESPONSE, 0, 0)
            val result2 = simulator.transmitCommand(cmdGetResponse)
            val responseApdu2 = ResponseAPDU(result2)
            if (ISO7816.SW_NO_ERROR != responseApdu2.sW.toShort())
                throw Exception("Create CSR failed: ${responseApdu2.sW}")
            csr = responseApdu2.data
        }
        return SignedCsr(csr, signature)
    }

    override fun fulfillCsr(certificate: ByteArray): Boolean {
        var offset = 0
        val totalLength = certificate.size

        // Max APDU size is ~255 bytes - send all data in chunks
        // Send the first chunk with P1_INIT
        var cmd = commandAPDU(
            CLA_HACKATHON,
            INS_FULFILL_CSR,
            P1_INIT,
            0,
            certificate.copyOfRange(offset, offset + APDU_COMMAND_CHUNK_SIZE)
        )
        var result = simulator.transmitCommand(cmd)
        var responseApdu = ResponseAPDU(result)
        if (ISO7816.SW_NO_ERROR != responseApdu.sW.toShort()) throw Exception("Fulfill CSR failed")
        offset += APDU_COMMAND_CHUNK_SIZE

        // Send subsequent chunks with P1_UPDATE
        while (offset + APDU_COMMAND_CHUNK_SIZE < totalLength) {
            cmd = commandAPDU(
                CLA_HACKATHON,
                INS_FULFILL_CSR,
                P1_UPDATE,
                0,
                certificate.copyOfRange(offset, offset + APDU_COMMAND_CHUNK_SIZE)
            )
            result = simulator.transmitCommand(cmd)
            responseApdu = ResponseAPDU(result)
            if (ISO7816.SW_NO_ERROR != responseApdu.sW.toShort())
                throw Exception("Fulfill CSR failed")
            offset += APDU_COMMAND_CHUNK_SIZE
        }

        // Send the final chunk with P1_FINAL
        cmd = commandAPDU(
            CLA_HACKATHON,
            INS_FULFILL_CSR,
            P1_FINAL,
            0,
            certificate.copyOfRange(offset, totalLength)
        )
        result = simulator.transmitCommand(cmd)
        responseApdu = ResponseAPDU(result)
        if (ISO7816.SW_NO_ERROR != responseApdu.sW.toShort())
            throw Exception("Fulfill CSR failed")

        cmd = commandAPDU(CLA_HACKATHON, INS_GET_STATUS, 0, 0)
        result = simulator.transmitCommand(cmd)
        responseApdu = ResponseAPDU(result)
        if (ISO7816.SW_NO_ERROR != responseApdu.sW.toShort())
            throw Exception("Failed to fetch the status")

        val currentStatus = responseApdu.data[0]
        return currentStatus == ACTIVATED_STATUS
    }

    override fun vote(
        electionId: Int,
        votedOption: String,
        pin: String
    ): AppletSigned<VoteReceipt> {
        var data = ByteArray(0)
        data += electionId.toByteArray()
        data += System.currentTimeMillis().toByteArray()

        // MAX_OPTION_LENGTH
        val paddedOption = votedOption.toByteArray(StandardCharsets.UTF_8).copyOf(20)
        data += paddedOption
        val pinOffset: Byte = data.size.toByte()
        val pinSize: Byte = pin.toByteArray().size.toByte()
        data += pin.toByteArray()

        // Signature is returned in the first chunk, the Vote receipt in the second
        val cmdVote = commandAPDU(CLA_HACKATHON, INS_VOTE, pinOffset, pinSize, data)
        var result = simulator.transmitCommand(cmdVote)
        var responseApdu = ResponseAPDU(result)
        if (0x61.toByte() != responseApdu.sW1.toByte())
            throw Exception("Failed to vote. Incomplete response from the applet")

        val signature = responseApdu.data
        var voteReceipt = ByteArray(0)

        // Get the receipt
        while (responseApdu.sW1 == 0x61) {
            val cmdGetResponse = commandAPDU(CLA_HACKATHON, INS_GET_RESPONSE, 0, 0)
            result = simulator.transmitCommand(cmdGetResponse)
            responseApdu = ResponseAPDU(result)
            voteReceipt += responseApdu.data
        }
        if (ISO7816.SW_NO_ERROR != responseApdu.sW.toShort())
            throw Exception("Failed to vote. Incomplete response from the applet")

        return AppletSigned.fromByteArray(voteReceipt, signature)
    }

    override fun storeFinalReceipt(receipt: IssuerSigned<VoteReceipt>): Boolean {
        val signature = receipt.signature
        val buffer = receipt.data.toByteArray()

        val cmdRegisterSubmissionSig = commandAPDU(
            CLA_HACKATHON,
            INS_REGISTER_SUBMISSION,
            P1_INIT,
            0,
            buffer
        )
        var result = simulator.transmitCommand(cmdRegisterSubmissionSig)
        var responseApdu = ResponseAPDU(result)
        if (ISO7816.SW_NO_ERROR != responseApdu.sW.toShort())
            throw Exception("Submission registration failed: ${responseApdu.sW}")

        val cmdRegisterSubmissionData = commandAPDU(
            CLA_HACKATHON,
            INS_REGISTER_SUBMISSION,
            P1_FINAL,
            0,
            signature
        )
        result = simulator.transmitCommand(cmdRegisterSubmissionData)
        responseApdu = ResponseAPDU(result)
        if (ISO7816.SW_NO_ERROR != responseApdu.sW.toShort())
            throw Exception("Submission registration failed: ${responseApdu.sW}")

        return true
    }

    override fun registerElectionData(electionData: IssuerSigned<ElectionData>): Boolean {
        val buffer = electionData.data.toByteArray()

        var cmdRegisterElection =
            commandAPDU(CLA_HACKATHON, INS_REGISTER_ELECTION, P1_INIT, 0, buffer)
        var result = simulator.transmitCommand(cmdRegisterElection)
        var responseApdu = ResponseAPDU(result)
        if (ISO7816.SW_NO_ERROR != responseApdu.sW.toShort())
            throw Exception("Election registration failed: ${responseApdu.sW}")

        cmdRegisterElection = commandAPDU(
            CLA_HACKATHON,
            INS_REGISTER_ELECTION,
            P1_FINAL,
            0,
            electionData.signature
        )
        result = simulator.transmitCommand(cmdRegisterElection)
        responseApdu = ResponseAPDU(result)

        return ISO7816.SW_NO_ERROR != responseApdu.sW.toShort()
    }

    override fun getRegisteredElection(electionId: Int): ElectionData {
        val buffer = electionId.toByteArray()
        val cmdGetElection = commandAPDU(CLA_HACKATHON, INS_GET_ELECTION, 0, 0, buffer)
        val result = simulator.transmitCommand(cmdGetElection)
        val responseApdu = ResponseAPDU(result)

        if (ISO7816.SW_NO_ERROR != responseApdu.sW.toShort())
            throw Exception("Failed to get an election: ${responseApdu.sW}")

        return ElectionData.fromByteArray(responseApdu.data)
    }

    override fun getRegisteredVote(electionId: Int): RegisteredVote? {
        val data = electionId.toByteArray()

        // Signature is returned in the first chunk, the Vote receipt in the second
        val cmdVote = commandAPDU(CLA_HACKATHON, INS_GET_VOTE_RECEIPT, 0, 0, data)
        var result = simulator.transmitCommand(cmdVote)
        var responseApdu = ResponseAPDU(result)
        if (0x61.toByte() != responseApdu.sW1.toByte())
            return null

        val signature = responseApdu.data.copyOfRange(0, responseApdu.data.size - 1)
        val submissionStatus: Boolean = responseApdu.data.last() != 0.toByte()
        var voteReceipt = ByteArray(0)

        // Get the receipt
        while (responseApdu.sW1 == 0x61) {
            val cmdGetResponse = commandAPDU(CLA_HACKATHON, INS_GET_RESPONSE, 0, 0)
            result = simulator.transmitCommand(cmdGetResponse)
            responseApdu = ResponseAPDU(result)
            voteReceipt += responseApdu.data
        }
        if (ISO7816.SW_NO_ERROR != responseApdu.sW.toShort())
            throw Exception("Failed to get vote. Incomplete response from the applet")

        return RegisteredVote(
            AppletSigned.fromByteArray(voteReceipt, signature),
            submissionStatus
        )
    }
}