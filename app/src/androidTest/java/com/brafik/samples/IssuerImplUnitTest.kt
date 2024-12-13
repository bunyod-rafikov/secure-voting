package com.brafik.samples

import com.brafik.samples.server.IssuerImpl
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemWriter
import org.junit.Before
import org.junit.Test
import java.io.StringWriter
import java.security.cert.X509Certificate

class IssuerImplUnitTest {

    companion object {
        // Example CSR
        val csr = byteArrayOf(
            48, -127, -42, 48, 127, 2, 1, 0, 48, 31, 49, 29, 48, 27, 6, 3, 85, 4, 3, 19, 20, 67,
            114, 117, 110, 99, 104, 121, 32, 77, 99, 67, 114, 117, 110, 99, 104, 102, 97, 99, 101,
            48, 89, 48, 19, 6, 7, 42, -122, 72, -50, 61, 2, 1, 6, 8, 42, -122, 72, -50, 61, 3, 1, 7,
            3, 66, 0, 4, 43, 73, 90, -101, 65, 66, -36, 49, 118, 36, 98, 106, -47, 8, -44, -119,
            108, 18, -87, 122, -15, -95, 55, 46, -102, 123, 15, 41, -83, -54, -21, 73, -34, -29,
            119, -55, 127, -69, 23, -74, 20, -128, -24, 87, -11, -50, 114, 64, -124, -120, -30,
            118, 54, 25, 21, -99, 3, 45, 59, -32, -111, 38, 109, -42, 48, 10, 6, 8, 42, -122, 72,
            -50, 61, 4, 3, 2, 3, 71, 0, 48, 68, 2, 32, 43, 73, 90, -101, 65, 66, -36, 49, 118, 36,
            98, 106, -47, 8, -44, -119, 108, 18, -87, 122, -15, -95, 55, 46, -102, 123, 15, 41, -83,
            -54, -21, 73, 2, 32, 82, 21, 124, 12, 80, -97, -58, 116, 83, -46, -58, -99, -94, -64,
            22, 83, -5, 92, -80, -94, -108, -82, 30, -24, -3, 56, 47, -95, -86, -88, 41, 107
        )

        val csrSignature = byteArrayOf(
            48, 68, 2, 32, 43, 73, 90, -101, 65, 66, -36, 49, 118, 36, 98, 106, -47, 8, -44, -119,
            108, 18, -87, 122, -15, -95, 55, 46, -102, 123, 15, 41, -83, -54, -21, 73, 2, 32, 94,
            74, 3, 89, -103, 3, 72, 104, -42, -9, -40, -19, -94, -108, -88, -13, -22, 15, 114, 76,
            -102, 21, 38, -47, 3, 51, 69, -32, -107, 46, 125, -112
        )

        private lateinit var issuer: IssuerImpl
    }

    @Before
    fun setUp() {
        issuer = IssuerImpl()
    }

    @Test
    fun issueCertificateFromCsr() {
        val cert = issuer.issueCertificate(csr, csrSignature)
        assert(cert.subjectDN.toString().contains("CN=Crunchy McCrunchface"))
        println("Issued Certificate:\n${cert.toPemString()}")
    }

    private fun X509Certificate.toPemString(): String {
        val stringWriter = StringWriter()
        val pemWriter = PemWriter(stringWriter)

        pemWriter.writeObject(PemObject("CERTIFICATE", encoded))
        pemWriter.close()

        return stringWriter.toString()
    }
}