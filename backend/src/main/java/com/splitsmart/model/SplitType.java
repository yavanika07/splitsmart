package com.splitsmart.model;

public enum SplitType {
    EQUAL,       // amount divided equally among participants
    EXACT,       // each participant's exact share is given
    PERCENTAGE   // each participant's percentage is given (must total 100)
}
