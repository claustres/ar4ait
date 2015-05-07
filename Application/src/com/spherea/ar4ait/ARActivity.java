package com.spherea.ar4ait;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.view.MotionEvent;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.metaio.sdk.ARViewActivity;
import com.metaio.sdk.MetaioDebug;
import com.metaio.sdk.jni.EENV_MAP_FORMAT;
import com.metaio.sdk.jni.ELIGHT_TYPE;
import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.ILight;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.Rotation;
import com.metaio.sdk.jni.TrackingValues;
import com.metaio.sdk.jni.TrackingValuesVector;
import com.metaio.sdk.jni.Vector3d;
import com.metaio.tools.io.AssetsManager;

public class ARActivity extends ARViewActivity
{
	/**
	 * Tracking state visualization model
	 */
	private IGeometry mTrackingModel = null;

    /**
     * Check for close distance
     */
    private boolean mIsCloseToModel = false;

    /**
     * Check for tracking state
     */
    private boolean mIsTracking = false;

    /**
     * Check for tracking paused or running
     */
    private boolean mIsTrackingPaused = true;

    /**
     * 3D model to occlude augmented content
     */
    private IGeometry mOcclusionModel = null;
	
	/**
	 * Pose visualization model
	 */
	private IGeometry mAidModel = null;

    /**
     * Scene head light
     */
    private ILight mHeadLight = null;

    /**
     * Debug flag to enabled model rendering
     */
    private String[] mDebugModes = {"off", "model", "normals", "normals_and_matches", "points"};
    private int mCurrentDebugMode = 0;


    /**
     * The procedure to be executed
     */
    Procedure mProcedure = new Procedure("Platine");

    /**
     * The different procedure steps
     */
    private List<ProcedureStep> mProcedureSteps = new ArrayList<ProcedureStep>();
    private int mCurrentStep = 0;

    /**
     * Activity widgets
     */
    ImageButton mTrackingButton = null;
    ImageButton mFreezeButton = null;
    ImageButton mResetPoseButton = null;
    ImageButton mResetButton = null;
    ImageButton mDebugButton = null;
    ImageButton mToolButton = null;
    ImageButton mAidButton = null;
    ImageButton mPreviousButton = null;
    ImageButton mNextButton = null;
    ImageButton mSettingsButton = null;
    LinearLayout mSettingsPanel = null;
    SettingsView mControlPanel = null;

    TextView    mDescriptionBox = null;
    TextView    mWarningBox = null;

    /**
     * Activity touch events
     */
    float mXDown;
    float mYDown;
    float mXMove;
    float mYMove;
    float mXUp;
    float mYUp;

    /**
	 * Metaio SDK callback handler
	 */
	private MetaioSDKCallbackHandler mCallbackHandler;	

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);

        // Get the widgets
        mTrackingButton = (ImageButton)mGUIView.findViewById(R.id.trackingButton);
        mFreezeButton = (ImageButton)mGUIView.findViewById(R.id.freezeButton);
        mResetPoseButton = (ImageButton)mGUIView.findViewById(R.id.resetPoseButton);
        mResetButton = (ImageButton)mGUIView.findViewById(R.id.resetButton);
        mDebugButton = (ImageButton)mGUIView.findViewById(R.id.debugButton);
        mAidButton = (ImageButton)mGUIView.findViewById(R.id.aidButton);
        mToolButton = (ImageButton)mGUIView.findViewById(R.id.toolButton);
        mPreviousButton = (ImageButton)mGUIView.findViewById(R.id.previousButton);
        mNextButton = (ImageButton)mGUIView.findViewById(R.id.nextButton);
        mSettingsButton = (ImageButton)mGUIView.findViewById(R.id.settingsButton);
        mSettingsPanel = (LinearLayout)mGUIView.findViewById(R.id.settingsView);
        mSettingsPanel.setVisibility(View.GONE);

        mDescriptionBox = (TextView)mGUIView.findViewById(R.id.descriptionBox);
        mWarningBox = (TextView)mGUIView.findViewById(R.id.warningBox);

        mControlPanel = (SettingsView)getFragmentManager().findFragmentById(R.id.settingsView);

        mCallbackHandler = new MetaioSDKCallbackHandler();

        // Reflect default states in UI
        updateGUI();
	}

	@Override
	protected void onDestroy() 
	{
		super.onDestroy();
		mCallbackHandler.delete();
		mCallbackHandler = null;
	}

    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        // This manages geometry picking
        super.onTouch(v, event);

        // Nothing to do while tracking
        if ( mIsTracking ) return true;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                mXDown = event.getX();
                mYDown = event.getY();
                mXMove = mYMove = -1;
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                final float ROTATION_SENSITIVITY = 10;
                // Compute motion delta (initialize on first move position with down position)
                float dX = ( event.getX() - (mXMove >= 0 ? mXMove : mXDown) ) / ROTATION_SENSITIVITY;
                float dY = ( event.getY() - (mYMove >= 0 ? mYMove : mYDown) ) / ROTATION_SENSITIVITY;
                metaioSDK.sensorCommand("rotatePose", Float.toString(dX) + " " + Float.toString(dY) + " 0");
                // Update last moved position
                mXMove = event.getX();
                mYMove = event.getY();
                break;
            }
            case MotionEvent.ACTION_UP: {
                mXUp = event.getX();
                mYUp = event.getY();
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                break;
            }
        }

        return true;
    }

	@Override
	protected IMetaioSDKCallback getMetaioSDKCallbackHandler()
	{
		return mCallbackHandler;
	}

    @Override
    public void onDrawFrame()
    {
        super.onDrawFrame();

        try
        {
            // Not used yet
            //checkDistanceToTarget();
            // Update headlight
            if ( mHeadLight != null ) {
                /* BUG : computing camera position gives incoherent results (the camera is positioned by default in -Z instead of -X)
                TrackingValues trackingValues = metaioSDK.getTrackingValues(1, false);

                // 3D point on the coordinate system
                Vector3d point = new Vector3d(0.0f, 0.0f, 0.0f);
                // camera position w.r.t. to the 3D point
                Vector3d cameraPosition = trackingValues.getTranslation();
                cameraPosition.add(trackingValues.getRotation().rotatePoint(point));

                // calculate the lighting direction for headlight
                Vector3d direction = cameraPosition.multiply(-1.0f);
                direction.normalize();
                mHeadLight.setDirection(direction);
                */
            }
        }
        catch (Exception e)
        {
        }
    }

    /**
     *  Control the CAD Model tracking
     */
    public String command(String command)
    {
        MetaioDebug.log("SensorCommand:" + command);
        return metaioSDK.sensorCommand(command);
    }

    /**
     *  Control the CAD Model tracking
     */
    public String command(String command, String parameters)
    {
        MetaioDebug.log("SensorCommand:" + command + " " + parameters);
        return metaioSDK.sensorCommand(command, parameters);
    }

    /**
     *  Close the app
     */
    public void onCloseButtonClick(View v)
	{
		finish();
	}

    /* Start/Stop tracking
	 */
    public void onTrackingButtonClick(View v)
    {
        setTrackingEnabled( mIsTrackingPaused );
    }

    /* Freeze/Resume tracking
	 */
    public void onFreezeButtonClick(View v)
    {
        metaioSDK.setFreezeTracking(!metaioSDK.getFreezeTracking());
        // Reflect change in GUI
        updateGUI();
    }

    /* Reset to non-tracking state
	 */
    public void onResetButtonClick(View v)
    {
        command("reset");
    }

    /**
     * Reset pose to default state
	 */
    public void onResetPoseButtonClick(View v)
    {
        command("resetInitialPose");
    }

    /**
     *  Display the default pose or not
	 */
    public void onAidButtonClick(View v)
    {
        if ( !mIsTracking && (mAidModel != null) ) {
            mAidModel.setVisible( !mAidModel.isVisible() );
        } else if ( mIsTracking && (mTrackingModel != null) ) {
            mTrackingModel.setVisible( !mTrackingModel.isVisible() );
        }
    }

    /**
     *  Display the control panel
     */
    public void onSettingsButtonClick(View v)
    {
        mSettingsPanel.setVisibility(View.VISIBLE);
    }

    /**
     * Close the settings panel
     */
    public void onCloseSettingsButtonClick(View v)
    {
        mSettingsPanel.setVisibility(View.GONE);
        mSettingsButton.setVisibility(View.VISIBLE);
    }

    /**
     * Close the settings panel
     */
    public void onSaveSettingsButtonClick(View v)
    {
        String xmlFileContent = metaioSDK.sensorCommand("exportConfig");
        try {
            ProcedureStorage procedureStorage = new ProcedureStorage(mProcedure,getApplicationContext());
            if (procedureStorage.createProcedureDirectory()) {
                String trackingParametersFilePath = procedureStorage.getTrackingParametersFilePath();
                File trackingParametersFile = new File(trackingParametersFilePath);
                if (trackingParametersFile.exists()) {
                    trackingParametersFile.delete();
                }
                FileOutputStream fileOutputStream = new FileOutputStream(trackingParametersFilePath);
                OutputStreamWriter outputStreamWriter=new OutputStreamWriter(fileOutputStream);
                outputStreamWriter.write(xmlFileContent);
                outputStreamWriter.close();
                fileOutputStream.close();
                Toast.makeText(getApplicationContext(), "Tracking parameters saved", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(getApplicationContext(), "Cannot create procedure directory", Toast.LENGTH_SHORT).show();
            }

        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    /**
     *  Display the line model as currently tracked or not
	 */
    public void onDebugButtonClick(View v)
    {
        // Switch between debug modes
        mCurrentDebugMode = (mCurrentDebugMode + 1) % mDebugModes.length;
        command("setDebugRenderMode", mDebugModes[mCurrentDebugMode]);
    }

    /* Display the tools associated with the model when in tracking state
	 */
    public void onToolButtonClick(View v)
    {
        mProcedureSteps.get(mCurrentStep).setToolVisible(!mProcedureSteps.get(mCurrentStep).isToolVisible());
    }

    /* Show the current procedure step
	 */
    private void updateBox(TextView view, String text) {
        if ( text.isEmpty() ) {
            view.setVisibility(View.GONE);
        } else {
            view.setVisibility(View.VISIBLE);
            view.setText(text);
        }
    }

    /* Show the current procedure step
	 */
    private void showCurrentStep() {
        // Show model
        mProcedureSteps.get(mCurrentStep).show();
        // And generic information
        // Take care this might be called from Metaio SDK callback out of the UI thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateBox(mDescriptionBox, mProcedureSteps.get(mCurrentStep).getDescription().toString());
                updateBox(mWarningBox, mProcedureSteps.get(mCurrentStep).getWarning().toString());
            }
        });

    }

    /* Hide the current procedure step
	 */
    private void hideCurrentStep() {
        // Hide model
        mProcedureSteps.get(mCurrentStep).hide();
        // Take care this might be called from Metaio SDK callback out of the UI thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mDescriptionBox.setVisibility(View.GONE);
                mWarningBox.setVisibility(View.GONE);
            }
        });
    }

    /* Change the current procedure step
	 */
    private void setCurrentStep(int step) {
        // Hide previous step
        hideCurrentStep();
        // Switch to new step
        mCurrentStep = step;
        // Show new step
        showCurrentStep();
        // Reflect change in GUI
        updateGUI();
    }

    /* Go back to previous state of the procedure when in tracking state
	 */
    public void onPreviousButtonClick(View v)
    {
        setCurrentStep((mCurrentStep - 1) % mProcedureSteps.size());
    }

    /* Switch to next state of the procedure when in tracking state
	 */
    public void onNextButtonClick(View v)
    {
        setCurrentStep( (mCurrentStep + 1) % mProcedureSteps.size() );
    }

    /* This method setups the activity GUI according to tracking state
	 */
    private void updateGUI() {
        final Boolean frozen = metaioSDK.getFreezeTracking();

        // Hide/Show relevant buttons
        // Take care this might be called from Metaio SDK callback out of the UI thread
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                if ( mIsTrackingPaused ) {
                    mTrackingButton.setImageResource(R.drawable.play);
                } else {
                    mTrackingButton.setImageResource(R.drawable.stop);
                }
                mTrackingButton.setVisibility(View.VISIBLE);
                mFreezeButton.setVisibility(!mIsTracking ? View.GONE : View.VISIBLE);
                mResetPoseButton.setVisibility(mIsTracking ? View.GONE : View.VISIBLE);
                mAidButton.setVisibility(!mIsTrackingPaused ? View.VISIBLE : View.GONE);
                mResetButton.setVisibility(!mIsTrackingPaused && !frozen && mIsTracking ? View.VISIBLE : View.GONE);
                mDebugButton.setVisibility(!mIsTrackingPaused && !frozen ? View.VISIBLE : View.GONE);
                mToolButton.setVisibility(!mIsTracking ? View.GONE : View.VISIBLE);
                mPreviousButton.setVisibility(!mIsTracking || (mCurrentStep == 0) ? View.GONE : View.VISIBLE);
                mNextButton.setVisibility(!mIsTracking || (mCurrentStep == mProcedureSteps.size() - 1) ? View.GONE : View.VISIBLE);
            }
        });
    }

    /* This method update the current tracking state (tracking or not)
	 */
    private void setTrackingState(Boolean tracking)
    {
        if ( mIsTracking != tracking ) {
            // Keep track of state
            mIsTracking = tracking;
            // Reflect change in GUI
            updateGUI();
            if ( mIsTracking ) {
                showCurrentStep();
            } else {
                hideCurrentStep();
            }
        }
    }

    /* This method pause or resume the tracking
	 */
    private void setTrackingEnabled(Boolean enabled)
    {
        // Pause or resume tracking depending on current state
        if ( !enabled ) {
            // Reset everything first
            if ( metaioSDK.getFreezeTracking() ) {
                metaioSDK.setFreezeTracking(false);
            }
            hideCurrentStep();
            command("resetInitialPose");
            command("reset");
            mIsTracking = false;
            metaioSDK.pauseTracking(true);
        } else {
            metaioSDK.resumeTracking();
        }
        // Keep track of state
        mIsTrackingPaused = !enabled;
        // Reflect change in GUI
        updateGUI();
    }

    /* This method is regularly called, calculates the distance between phone and target
	 * and performs actions based on the distance
	 */
    private void checkDistanceToTarget()
    {
        // check if the current state is tracking
        // Note, you can use this mechanism also to detect if something is tracking or not.
        // (e.g. for triggering an action as soon as some target is visible on screen)
        if ( mIsTracking )
        {
            // get the translation part of the pose
            final Vector3d translation = metaioSDK.getTrackingValues(1).getTranslation();
            // calculate the distance as sqrt( x^2 + y^2 + z^2 )
            final float distanceToTarget = translation.norm();
            // define a threshold distance
            final float threshold = 800;

            // we're not close yet, let's check if we are now
            if (distanceToTarget < threshold)
            {
                // flip the variable
                if ( !mIsCloseToModel )
                {
                    mIsCloseToModel = true;
                    MetaioDebug.log("Close to model");
                    // and stop an animation
                    //setAnimationEnabled(mAugmentedModel, true);
                }
            }
            else if ( mIsCloseToModel )
            {
                MetaioDebug.log("Far from model");
                // we flip this variable again
                mIsCloseToModel = false;
                // and start the close_up animation
                //setAnimationEnabled(mAugmentedModel, false);
            }
        }
    }

    private void createHeadlight() {
        metaioSDK.setAmbientLight(new Vector3d(0.0f));

        mHeadLight = metaioSDK.createLight();
        mHeadLight.setType(ELIGHT_TYPE.ELIGHT_TYPE_DIRECTIONAL);
        mHeadLight.setAmbientColor(new Vector3d(0, 0, 0));
        mHeadLight.setDiffuseColor(new Vector3d(1, 1, 1));
        mHeadLight.setCoordinateSystemID(1);
        mHeadLight.setDirection(new Vector3d(0, 0, -1)); // Default -Z
    }

	@Override
	protected void loadContents() 
	{
        // Initialize lighting
        createHeadlight();

        mOcclusionModel = loadModel("platine_joint_tracking/SurfaceModel.obj");
        mAidModel = loadModel("platine_joint_tracking/VIS_INIT.obj");
        mTrackingModel = loadModel("platine_joint_tracking/VIS_TRACK.obj");

        // Load the procedure steps
        IGeometry augmentedModel = loadModel("platine_steps/platine_step_1.obj");
        IGeometry augmentedToolModel = loadModel("platine_steps/platine_step_1_tools.obj");
        augmentedToolModel.setScale(new Vector3d(3f, 3f, 3f));
        augmentedToolModel.setTranslation(new Vector3d(-0f, -340f, 100f));
        ProcedureStep step1 = new ProcedureStep(augmentedModel, augmentedToolModel);
        step1.setDescription("Fixation du raidisseur");
        step1.setWarning("Monter le raidisseur du côté non fraisé des Sub-D 9 points");
        mProcedureSteps.add( step1 );

        augmentedModel = loadModel("platine_steps/platine_step_2.obj");
        ProcedureStep step2 = new ProcedureStep(augmentedModel, augmentedToolModel);
        step2.setDescription("Fixation des deux cornières (éclaté)");
        mProcedureSteps.add( step2 );

        augmentedModel = loadModel("platine_steps/platine_step_3.obj");
        ProcedureStep step3 = new ProcedureStep(augmentedModel, augmentedToolModel);
        step3.setDescription("Fixation des deux cornières (fini)");
        mProcedureSteps.add( step3 );

		final File envmapPath = AssetsManager.getAssetPathAsFile(getApplicationContext(), "env_map.png");
		metaioSDK.loadEnvironmentMap(envmapPath, EENV_MAP_FORMAT.EEMF_LATLONG);

		if (mTrackingModel != null)
            mTrackingModel.setCoordinateSystemID(1);
        if (mOcclusionModel != null)
        {
            mOcclusionModel.setCoordinateSystemID(1);
            mOcclusionModel.setOcclusionMode(true);
        }
		if (mAidModel != null)
			mAidModel.setCoordinateSystemID(2);

        // Load camera calibration
        final File cameraPath = AssetsManager.getAssetPathAsFile(getApplicationContext(), "camera.xml");
        metaioSDK.setCameraParameters(cameraPath);

        // Then tracking settings
        File trackingParametersFile = AssetsManager.getAssetPathAsFile(getApplicationContext(),"platine_joint_tracking/Tracking.xml");
        //ProcedureStorage procedureStorage = new ProcedureStorage(mProcedure, getApplicationContext());
        //File trackingParametersFile = new File(procedureStorage.getTrackingParametersFilePath());
        try {
            // set tracking configuration
            if (metaioSDK.setTrackingConfiguration(trackingParametersFile)) {
                mControlPanel.refresh();
            }


        } catch (Exception e) {
            MetaioDebug.log(Log.ERROR, "Error loading tracking configuration: " + trackingParametersFile.getAbsolutePath() + " " + e.getMessage());
        }
   	}

    /* Manage Metaio SDK callbacks
     */
    final class MetaioSDKCallbackHandler extends IMetaioSDKCallback
	{
		@Override
		public void onSDKReady() 
		{
			// show GUI
			runOnUiThread(new Runnable() 
			{
				@Override
				public void run() 
				{
					mGUIView.setVisibility(View.VISIBLE);
				}
			});
            // default state
            if ( mIsTrackingPaused ) {
                metaioSDK.pauseTracking(true);
            }
		}

        @Override
        public void onAnimationEnd(IGeometry geometry, String animationName)
        {
            MetaioDebug.log("Animation ended: "+animationName);
            if (animationName.equalsIgnoreCase("Hand anim"))
            {
                // Whatever
            }
        }

        @Override
        public void onTrackingEvent(TrackingValuesVector trackingValues)
        {
            for (int i=0; i<trackingValues.size(); i++)
            {
                final TrackingValues trackingValue = trackingValues.get(i);
                MetaioDebug.log("Tracking state for COS " + trackingValue.getCoordinateSystemID() + " is " + trackingValue.getState());
                // Update tracking state
                if ( trackingValue.getCoordinateSystemID() == 1 )
                {
                    setTrackingState(trackingValue.isTrackingState());
                }
            }
        }
	}

	/* Load a given 3D model
	 */
    private IGeometry loadModel(final String path)
	{
		IGeometry geometry = null;
		try
		{
			// Load model
			final File modelPath = AssetsManager.getAssetPathAsFile(getApplicationContext(), path);			
			geometry = metaioSDK.createGeometry(modelPath);
			
			MetaioDebug.log("Loaded geometry "+modelPath);			
		}       
		catch (Exception e)
		{
			MetaioDebug.log(Log.ERROR, "Error loading geometry: "+e.getMessage());
			return geometry;
		}		
		return geometry;
	}

    /* Check if a given 3D model is child of another
      */
    private Boolean isChild(IGeometry model, IGeometry geometry)
    {
        IGeometry parent = geometry.getParentGeometry();

        while ( ( parent != null ) && !parent.equals(model) )
        {
            parent = parent.getParentGeometry();
        }

        return ( parent != null ) && parent.equals(model);
    }

	@Override
	protected int getGUILayout()
	{
		return R.layout.arview;
	}

	@Override
	protected void onGeometryTouched(IGeometry geometry) 
	{
        MetaioDebug.log("Touched Geometry: "+geometry.getName());

        mProcedureSteps.get(mCurrentStep).onGeometryTouched(geometry, this);
	}
	
}
