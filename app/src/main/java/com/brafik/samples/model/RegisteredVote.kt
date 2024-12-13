package com.brafik.samples.model

data class RegisteredVote(
    val value: AppletSigned<VoteReceipt>,
    val isSubmitted: Boolean
)