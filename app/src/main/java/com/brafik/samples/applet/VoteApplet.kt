package com.brafik.samples.applet

import com.brafik.samples.model.AppletSigned
import com.brafik.samples.model.ElectionData
import com.brafik.samples.model.IssuerSigned
import com.brafik.samples.model.RegisteredVote
import com.brafik.samples.model.SignedCsr
import com.brafik.samples.model.VoteReceipt

interface VoteApplet {
    fun createCsr(pin: String): SignedCsr
    fun fulfillCsr(certificate: ByteArray): Boolean
    fun registerElectionData(electionData: IssuerSigned<ElectionData>): Boolean
    fun vote(electionId: Int, votedOption: String, pin: String): AppletSigned<VoteReceipt>
    fun storeFinalReceipt(receipt: IssuerSigned<VoteReceipt>): Boolean
    fun getRegisteredElection(electionId: Int): ElectionData
    fun getRegisteredVote(electionId: Int): RegisteredVote?
}