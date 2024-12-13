package com.brafik.samples.model

data class IssuerSigned<T>(
    val data: T,
    val signature: ByteArray
)