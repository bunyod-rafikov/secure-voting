package com.brafik.samples.utils

fun Long.toByteArray(): ByteArray {
    return byteArrayOf(
        (this shr 56 and 0xFF).toByte(),
        (this shr 48 and 0xFF).toByte(),
        (this shr 40 and 0xFF).toByte(),
        (this shr 32 and 0xFF).toByte(),
        (this shr 24 and 0xFF).toByte(),
        (this shr 16 and 0xFF).toByte(),
        (this shr 8 and 0xFF).toByte(),
        (this and 0xFF).toByte()
    )
}

fun ByteArray.getLong(offset: Int): Long {
    return ((this[offset].toLong() and 0xFF) shl 56) or
            ((this[offset + 1].toLong() and 0xFF) shl 48) or
            ((this[offset + 2].toLong() and 0xFF) shl 40) or
            ((this[offset + 3].toLong() and 0xFF) shl 32) or
            ((this[offset + 4].toLong() and 0xFF) shl 24) or
            ((this[offset + 5].toLong() and 0xFF) shl 16) or
            ((this[offset + 6].toLong() and 0xFF) shl 8) or
            (this[offset + 7].toLong() and 0xFF)
}