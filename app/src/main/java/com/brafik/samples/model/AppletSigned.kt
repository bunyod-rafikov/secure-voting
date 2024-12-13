package com.brafik.samples.model

import com.brafik.samples.utils.getInt
import com.brafik.samples.utils.getLong
import com.brafik.samples.utils.toNullTerminatedString

data class AppletSigned<T>(
    val data: T,
    val voterCertificate: ByteArray,
    val signature: ByteArray
) {
    companion object {
        fun fromByteArray(value: ByteArray, signature: ByteArray): AppletSigned<VoteReceipt> {
            var offset = 0
            val resElectId = value.getInt(offset);
            offset += 4
            val resVoteOption = value.copyOfRange(offset, offset + 20)
            offset += 20
            val resTimestamp = value.getLong(offset)
            offset += 8
            val cerLen = value.size - offset
            val resCert = value.copyOfRange(offset, offset + cerLen)

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
    }
}