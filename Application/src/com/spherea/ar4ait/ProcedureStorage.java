package com.spherea.ar4ait;

import java.io.File;
import java.io.FileInputStream;
import android.content.Context;

public class ProcedureStorage
{
    /**
     * The procedure
     */
    private Procedure mProcedure;

    /**
     * The application context
     */
    private Context mContext;


    /**
     * Constructs a new ProcedureStorage
     * @param procedure
     * @param context
     */
    public ProcedureStorage(Procedure procedure, Context context) {
        mProcedure = procedure;
        mContext = context;
    }

    /**
     * Returns the path to the procedure's directory
     * @return
     */
    String getProcedureDirectoryPath() {
        return mContext.getExternalFilesDir(null).toString() + "/" + mProcedure.getName();
    }

    /**
     * Returns the path to the procedure's file
     * @return
     */
    String getProcedureFilePath() {
        return getProcedureDirectoryPath() + "/" + mProcedure.getName() + ".xml";
    }

    /**
     * Returns the path to the procedure's tracking parameters file
     * @return
     */
    String getTrackingParametersFilePath() {
        return getProcedureDirectoryPath() + "/" + mProcedure.getName() + "_Tracking.xml";
    }

    /**
     * Creates the procedure directory if not exists
     */
    boolean createProcedureDirectory() {
        File procedureDirectory = new File(getProcedureDirectoryPath());
        if (procedureDirectory.exists()) {
            return true;
        }
        return procedureDirectory.mkdir();
    }

    /**
     * Reads the procedure content
     * @return
     */
    boolean readProcedure() {
        //TODO
        return true;
    }


}
