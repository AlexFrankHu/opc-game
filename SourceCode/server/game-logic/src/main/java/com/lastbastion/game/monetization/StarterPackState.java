package com.lastbastion.game.monetization;

import java.io.Serializable;

public final class StarterPackState implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean eligible;
    private boolean purchased;
    private long firstEligibleMs;

    public boolean eligible() { return eligible; }
    public void setEligible(boolean v) { this.eligible = v; }

    public boolean purchased() { return purchased; }
    public void setPurchased(boolean v) { this.purchased = v; }

    public long firstEligibleMs() { return firstEligibleMs; }
    public void setFirstEligibleMs(long v) { this.firstEligibleMs = v; }
}
