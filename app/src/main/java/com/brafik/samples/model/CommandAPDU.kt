package com.brafik.samples.model

fun commandAPDU(
    cla: Byte,
    ins: Byte,
    p1: Byte,
    p2: Byte,
    data: ByteArray,
    le: Byte = 0x00,
): ByteArray {
    val commandApdu = ByteArray(6 + data.size)
    commandApdu[0] = cla
    commandApdu[1] = ins
    commandApdu[2] = p1
    commandApdu[3] = p2
    commandApdu[4] = ((data.size) and 0x0FF).toByte() // Lc
    System.arraycopy(data, 0, commandApdu, 5, data.size) // data
    commandApdu[commandApdu.size - 1] = le // Le
    return commandApdu
}

fun commandAPDU(cla: Byte, ins: Byte, p1: Byte, p2: Byte): ByteArray {
    val commandApdu = ByteArray(4)
    commandApdu[0] = cla
    commandApdu[1] = ins
    commandApdu[2] = p1
    commandApdu[3] = p2
    return commandApdu
}