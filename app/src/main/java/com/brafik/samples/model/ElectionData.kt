package com.brafik.samples.model

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

data class ElectionData(
    val electionId: Int,
    val title: String,
    val options: List<String>,
    val expiration: Long
) {
    fun toByteArray(): ByteArray {
        // Encode the title to a fixed 20-byte UTF-8 string
        val titleBytes = title.toByteArray(StandardCharsets.UTF_8).copyOf(20)

        // Encode each option to UTF-8, collect lengths and concatenated data
        val optionsBytes = options.map { it.toByteArray(StandardCharsets.UTF_8) }
        val optionsLengths = optionsBytes.map { it.size.toShort() }
        val concatenatedOptions = optionsBytes.flatMap { it.toList() }.toByteArray()

        // Calculate total size
        val totalSize = 32 + // Metadata + Expiration
                Byte.SIZE_BYTES + // Number of options
                optionsLengths.size * Short.SIZE_BYTES + // Option lengths
                concatenatedOptions.size // Option data

        // Allocate a ByteBuffer for serialization
        val buffer = ByteBuffer.allocate(totalSize)
        buffer.putInt(electionId)
        buffer.put(titleBytes)
        buffer.putLong(expiration)
        buffer.put(optionsLengths.size.toByte())
        optionsLengths.forEach { buffer.putShort(it) }
        buffer.put(concatenatedOptions)

        return buffer.array()
    }

    companion object {
        fun fromByteArray(byteArray: ByteArray): ElectionData {
            val buffer = ByteBuffer.wrap(byteArray)

            // Deserialize Metadata
            val electionId = buffer.int
            val titleBytes = ByteArray(20).also { buffer.get(it) }
            val title = String(titleBytes, StandardCharsets.UTF_8).trimEnd('\u0000')
            // Deserialize Expiration
            val expiration = buffer.long

            // Deserialize Voting Items
            val optionsCount = buffer.get().toInt()
            val optionsLengths = List(optionsCount) { buffer.short.toInt() }
            val concatenatedOptions = ByteArray(optionsLengths.sum()).also { buffer.get(it) }
            val options = optionsLengths.foldIndexed(emptyList<String>()) { _, acc, length ->
                val start = acc.sumOf { it.length }
                acc + String(concatenatedOptions, start, length, StandardCharsets.UTF_8)
            }

            return ElectionData(electionId, title, options, expiration)
        }
    }
}
