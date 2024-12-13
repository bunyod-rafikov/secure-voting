package com.brafik.samples.model

import com.brafik.samples.utils.toByteArray
import com.brafik.samples.utils.toFixedLengthByteArray

data class VoteReceipt(
    val electionId: Int,
    val votedOption: String,
    val timestamp: Long
) {
    fun toByteArray(): ByteArray {
        val electIdBytes = electionId.toByteArray()
        val voteOptionBytes = votedOption.toFixedLengthByteArray(20)
        val timestampBytes = timestamp.toByteArray()

        return electIdBytes + voteOptionBytes + timestampBytes
    }
}
