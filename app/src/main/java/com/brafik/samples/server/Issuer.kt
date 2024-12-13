package com.brafik.samples.server

import com.brafik.samples.model.AppletSigned
import com.brafik.samples.model.ElectionData
import com.brafik.samples.model.IssuerSigned
import com.brafik.samples.model.VoteReceipt
import java.security.cert.X509Certificate

/**
 * This interface is to implement a class or an object for Backend component.
 *
 * As a part of the current project it is implemented within the app, while in the real world
 * it should be implemented within a server instance
 * */
interface Issuer {
    fun issueCertificate(csr: ByteArray, signature: ByteArray): X509Certificate
    fun generateElection(): IssuerSigned<ElectionData>
    fun verifyVote(voteReceipt: AppletSigned<VoteReceipt>): IssuerSigned<VoteReceipt>
}