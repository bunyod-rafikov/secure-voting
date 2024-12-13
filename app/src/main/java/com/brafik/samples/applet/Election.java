package com.brafik.samples.applet;

public class Election {

    public final static short MAX_TITLE_LENGTH = 20;
    public final static short MAX_SIGNATURE_SIZE = 73;
    public final static short MAX_OPTIONS = 5;

    public int electionID;
    public long expiration;
    public byte[] title;
    public boolean active;
    public byte voteIndex;
    public long timestamp;
    public byte[] signature;
    public short signatureSize;
    public byte submitted;

    public Candidate[] candidates;

    public Election() {
        this.electionID = 0;
        this.expiration = 0;
        this.timestamp = 0;
        this.submitted = 0;
        this.title = new byte[MAX_TITLE_LENGTH];
        this.candidates = new Candidate[MAX_OPTIONS];
        this.voteIndex = -1;
        for (short i = 0; i < MAX_OPTIONS; i++) {
            candidates[i] = new Candidate();
        }
        this.active = false;
        this.signature = new byte[MAX_SIGNATURE_SIZE];
        this.signatureSize = 0;
    }

    public void reset() {
        this.electionID = 0;
        this.expiration = 0;
        this.timestamp = 0;
        this.signatureSize = 0;
        this.voteIndex = -1;
        this.submitted = 0;
        this.active = false;
        for (byte i = 0; i < MAX_OPTIONS; i++) {
            candidates[i].reset();
        }
    }

    public byte getNumberOfOptions() {
        byte numberOfOptions = 0;
        for (byte i = 0; i < MAX_OPTIONS; i++) {
            if (candidates[i].active) {
                numberOfOptions++;
            }
        }
        return numberOfOptions;
    }

    public byte getAvailableIndex() {
        for (byte i = 0; i < MAX_OPTIONS; i++) {
            if (!candidates[i].active) {
                return i;
            }
        }
        return -1;
    }
}


