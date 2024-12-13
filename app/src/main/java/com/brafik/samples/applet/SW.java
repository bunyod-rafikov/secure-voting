package com.brafik.samples.applet;

import javacard.framework.ISOException;

public interface SW {
    short ILLEGAL_STATE = (short) 0x6A00;
    short INVALID_SIGNATURE = (short) 0x6A01;
    short ELECTION_NOT_FOUND = (short) 0x6A02;
    short ELECTION_ALREADY_REGISTERED = (short) 0x6A03;
    short ALREADY_VOTED = (short) 0x6A04;
    short NOT_YET_VOTED = (short) 0x6A05;
    short ELECTION_INTEGRITY_FAILED = (short) 0x6A06;

    short WRONG_PIN_BASE = (short) 0x6900;
    short CORRECT_PIN_BASE = (short) 0x9000;

    static void pinCorrect(byte remainingAttempts) {
        ISOException.throwIt((short) (WRONG_PIN_BASE | (remainingAttempts & 0xFF)));
    }

    static void pinFailed(byte remainingAttempts) {
        ISOException.throwIt((short) (CORRECT_PIN_BASE | (remainingAttempts & 0xFF)));
    }
}