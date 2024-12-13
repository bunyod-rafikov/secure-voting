package com.brafik.samples.server

import android.annotation.SuppressLint
import android.util.Log
import com.brafik.samples.model.AppletSigned
import com.brafik.samples.model.ElectionData
import com.brafik.samples.model.IssuerSigned
import com.brafik.samples.model.VoteReceipt
import com.thedeanda.lorem.LoremIpsum
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.bouncycastle.pkcs.PKCS10CertificationRequest
import org.bouncycastle.util.io.pem.PemReader
import java.io.StringReader
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.PublicKey
import java.security.Signature
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.X509EncodedKeySpec
import java.util.Date

/* This class is simulating the Backend component and should not be used in a real world scenario */
class IssuerImpl : Issuer {
    companion object {
        private const val ISSUER_DN = "CN=intermediate-cert"
        private const val ISSUER_KEYPAIR = """-----BEGIN EC PRIVATE KEY-----
        MHcCAQEEIBy+6NPZTuopr2bcZ827SpqzooGJPbx0GEI74NWe76G3oAoGCCqGSM49
        AwEHoUQDQgAEcJ241ggngEDmYJapV1m5TrkZ3RmNfXPlfl4AXdxHtWyreYuyHhXy
        Hnu4EBVLNXBwZ2y8cjxneuto8sxuZ25J+w==
        -----END EC PRIVATE KEY-----"""
        private const val COMMON_PUBLIC_KEY = """-----BEGIN PUBLIC KEY-----
        MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEZvbwsaYzR/K+J8ejF7YjTQenBUW6
        QBpaDK9yByZm96QRfqGzatH3IgtPfGJvuWJpaOuuSIaPi3pMoey9mhjvmg==
        -----END PUBLIC KEY-----"""
    }

    override fun issueCertificate(csr: ByteArray, signature: ByteArray): X509Certificate {
        val commonPublicKey = COMMON_PUBLIC_KEY.reader().use { reader ->
            PEMParser(reader).use { parser ->
                val pemObject = parser.readObject() as SubjectPublicKeyInfo
                val keySpec = X509EncodedKeySpec(pemObject.encoded)
                KeyFactory.getInstance("EC").generatePublic(keySpec)
            }
        }


        if (!verifySignature(csr, signature, commonPublicKey)) {
            throw IllegalArgumentException("Signature verification failed. CSR is not from a trusted applet")
        }

        val parsedCsr = PKCS10CertificationRequest(csr)
        val keySpec = X509EncodedKeySpec(parsedCsr.subjectPublicKeyInfo.encoded)
        val publicKey = KeyFactory.getInstance("EC").generatePublic(keySpec)

        val notBefore = Date()
        val notAfter = Date(notBefore.time + 365 * 24L * 60L * 60L * 1000L)

        val certBuilder = JcaX509v3CertificateBuilder(
            /* issuer = */ X500Name(ISSUER_DN),
            /* serial = */ BigInteger.valueOf(System.currentTimeMillis()),
            /* notBefore = */ notBefore,
            /* notAfter = */ notAfter,
            /* subject = */ parsedCsr.subject,
            /* publicKey = */ publicKey
        )

        val issuerKeyPair = getIssuerKeys()
        val signer = JcaContentSignerBuilder("SHA256WithECDSA").build(issuerKeyPair.private)
        val holder = certBuilder.build(signer)

        return JcaX509CertificateConverter().getCertificate(holder)
    }

    override fun generateElection(): IssuerSigned<ElectionData> {
        // Generate a unique 4-byte election ID
        val electionId = 1 // Random.nextInt(0, Int.MAX_VALUE)

        val lorem = LoremIpsum()

        // Random title selection
        val title = lorem.getTitle(1).plus(" Election")

        // Randomly generate option names
        val options = List(4) {
            lorem.name
        }

        // Set expiration to two weeks from now
        val expiration = System.currentTimeMillis() + 14 * 24 * 60 * 60 * 1000L

        // Construct the ElectionData object
        val electionData = ElectionData(
            electionId = electionId,
            title = title,
            options = options,
            expiration = expiration,
        )
        return IssuerSigned(electionData, electionData.toByteArray().sign())
    }

    override fun verifyVote(voteReceipt: AppletSigned<VoteReceipt>): IssuerSigned<VoteReceipt> {
        val dataAsByteArray = voteReceipt.data.toByteArray()
        val signedValue = dataAsByteArray + voteReceipt.voterCertificate
        val verified = verifySignature(
            signedValue,
            voteReceipt.signature,
            voteReceipt.voterCertificate.toX509Certificate().publicKey
        )

        if (verified) {
            Log.d("ISSUER", "Storing the vote receipt: ${voteReceipt.data}")
        } else {
            throw IllegalArgumentException("Signature verification failed. Vote is not from a trusted applet")
        }

        return IssuerSigned(voteReceipt.data, dataAsByteArray.sign())
    }

    private fun verifySignature(
        value: ByteArray,
        signature: ByteArray,
        publicKey: PublicKey
    ): Boolean {
        return Signature.getInstance("SHA256withECDSA").run {
            initVerify(publicKey)
            update(value)
            verify(signature)
        }
    }

    private fun getIssuerKeys(): KeyPair {
        val pemReader = PemReader(StringReader(ISSUER_KEYPAIR))
        val pemObject = PEMParser(pemReader).readObject()
        pemReader.close()

        if (pemObject is PEMKeyPair)
            return JcaPEMKeyConverter().getKeyPair(pemObject)
        else
            throw IllegalArgumentException("Wrong key pair type")
    }

    private fun ByteArray.sign(): ByteArray {
        val signature = Signature.getInstance("SHA256WithECDSA")
        signature.initSign(getIssuerKeys().private)
        signature.update(this)

        return signature.sign()
    }

    private fun ByteArray.toX509Certificate(): X509Certificate {
        // Create X509Certificate from certificate byte array
        val certificateFactory = CertificateFactory.getInstance("X.509")
        return certificateFactory.generateCertificate(this.inputStream()) as X509Certificate
    }
}