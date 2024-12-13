package com.brafik.samples.model

data class SignedCsr(
    val csr: ByteArray,
    val signature: ByteArray
)
