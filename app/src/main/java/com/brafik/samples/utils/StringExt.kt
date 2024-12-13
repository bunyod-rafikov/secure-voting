package com.brafik.samples.utils

fun String.toFixedLengthByteArray(length: Int): ByteArray {
    val bytes = this.encodeToByteArray()
    return if (bytes.size >= length) {
        bytes.copyOf(length) // Truncate or fit to length
    } else {
        bytes + ByteArray(length - bytes.size) { 0 } // Pad with null bytes
    }
}

fun ByteArray.toNullTerminatedString(): String {
    val nullIndex = this.indexOf(0.toByte())
    val actualData = if (nullIndex >= 0) this.copyOfRange(0, nullIndex) else this
    return actualData.toString(Charsets.UTF_8)
}