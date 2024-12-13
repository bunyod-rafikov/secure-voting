package com.brafik.samples.applet;

public class Candidate {

    public final static short MAX_CANDIDATE_LEN = 20;
    public boolean active;
    public final byte[] option;

    public Candidate() {
        this.active = false;
        this.option = new byte[MAX_CANDIDATE_LEN];
    }

    public void reset() {
        this.active = false;
        for (int i = 0; i < MAX_CANDIDATE_LEN; i++) {
            this.option[i] = 0;
        }
    }

    public short getCandidateLen() {
        byte sz = 0;
        for (int i = 0; i < MAX_CANDIDATE_LEN; i++) {
            if (option[i] == 0x00) {
                return sz;
            }
            sz++;
        }
        return sz;
    }
}
