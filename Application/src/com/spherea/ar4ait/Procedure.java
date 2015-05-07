package com.spherea.ar4ait;

import java.util.ArrayList;

public class Procedure
{
    /**
     * The list of steps
     */
    ArrayList<ProcedureStep> mProcedureSteps = new ArrayList<ProcedureStep>();

    /**
     * Name of the procedure
     */
    private CharSequence mName = "";


    /**
     * Constructs a new procedure with the give name
     * @param name specifies the name of the procedure
     */
    public Procedure(CharSequence name) {
        mName = name;
    }

    CharSequence getName() {
        return mName;
    }

    ProcedureStep getStep(int i) {
        return mProcedureSteps.get(i);
    }

    int getNumberOfSteps() {
        return mProcedureSteps.size();
    }

}
