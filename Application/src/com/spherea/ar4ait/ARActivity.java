package com.spherea.ar4ait;

import java.io.File;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.MotionEvent;
import android.widget.ImageButton;

import com.metaio.sdk.ARViewActivity;
import com.metaio.sdk.MetaioDebug;
import com.metaio.sdk.jni.EENV_MAP_FORMAT;
import com.metaio.sdk.jni.IGeometry;
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
     * 3D augmented content model
     */
    private IGeometry mAugmentedModel = null;

    /**
     * 3D augmented tool model
     */
    private IGeometry mAugmentedToolModel = null;

    /**
     * Check for close distance
     */
    private boolean mIsCloseToModel = false;

    /**
     * Check for tracking
     */
    private boolean mIsTracking = false;

    /**
     * 3D model to occlude augmented content
     */
    private IGeometry mOcclusionModel = null;
	
	/**
	 * Pose visualization model
	 */
	private IGeometry mAidModel = null;

    /**
     * Debug flag to enabled model rendering
     */
    private String[] mDebugModes = {"off", "model", "normals", "normals_and_matches", "points"};
    private int mCurrentDebugMode = 0;

    /**
     * Activity buttons
     */
    ImageButton mResetPoseButton = null;
    ImageButton mResetButton = null;
    ImageButton mModelButton = null;
    ImageButton mToolButton = null;
    ImageButton mAidButton = null;
    ImageButton mPreviousButton = null;
    ImageButton mNextButton = null;

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

        // Get buttons
        mResetPoseButton = (ImageButton)mGUIView.findViewById(R.id.resetPoseButton);
        mResetButton = (ImageButton)mGUIView.findViewById(R.id.resetButton);
        mModelButton = (ImageButton)mGUIView.findViewById(R.id.modelButton);
        mAidButton = (ImageButton)mGUIView.findViewById(R.id.aidButton);
        mToolButton = (ImageButton)mGUIView.findViewById(R.id.toolButton);
        mPreviousButton = (ImageButton)mGUIView.findViewById(R.id.previousButton);
        mNextButton = (ImageButton)mGUIView.findViewById(R.id.nextButton);

        // Default state
        setTracking(false);
		mCallbackHandler = new MetaioSDKCallbackHandler();
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
            checkDistanceToTarget();
        }
        catch (Exception e)
        {
        }
    }

    /* Close the app
     */
    public void onCloseButtonClick(View v)
	{
		finish();
	}

    /* Reset to non-tracking state
	 */
    public void onResetButtonClick(View v)
    {
        metaioSDK.sensorCommand("reset");
    }

    /* Reset pose to default state
	 */
    public void onResetPoseButtonClick(View v)
    {
        metaioSDK.sensorCommand("resetInitialPose");
    }

    /* Display the default pose or not
	 */
    public void onAidButtonClick(View v)
    {
        if ( mAidModel != null ) {
            mAidModel.setVisible( !mAidModel.isVisible() );
        }
    }

    /* Display the line model as currently tracked or not
	 */
    public void onModelButtonClick(View v)
    {
        // Switch between debug modes
        mCurrentDebugMode = (mCurrentDebugMode + 1) % mDebugModes.length;
        metaioSDK.sensorCommand("setDebugRenderMode", mDebugModes[mCurrentDebugMode]);
    }

    /* Display the tools associated with the model when in tracking state
	 */
    public void onToolButtonClick(View v)
    {
        if ( mAugmentedToolModel != null ) {
            mAugmentedToolModel.setVisible( !mAugmentedToolModel.isVisible() );
        }
    }

    /* Go back to previous state of the procedure when in tracking state
	 */
    public void onPreviousButtonClick(View v)
    {
        // Start animation
        //setAnimationEnabled(mAugmentedModel, true);
    }

    /* Switch to next state of the procedure when in tracking state
	 */
    public void onNextButtonClick(View v)
    {
        // Stop animation
        //setAnimationEnabled(mAugmentedModel, false);
    }

    /* This method setups the activity according to tracking state
	 */
    private void setTracking(Boolean enabled)
    {
        mIsTracking = enabled;

        // Hide/Show relevant buttons
        // Take care this might be called from Metaio SDK callback out of the UI thread
        runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                mResetPoseButton.setVisibility(mIsTracking ? View.GONE : View.VISIBLE);
                mResetButton.setVisibility(!mIsTracking ? View.GONE : View.VISIBLE);
                mModelButton.setVisibility(View.VISIBLE);
                mToolButton.setVisibility(!mIsTracking ? View.GONE : View.VISIBLE);
                mAidButton.setVisibility(mIsTracking ? View.GONE : View.VISIBLE);
                mPreviousButton.setVisibility(!mIsTracking ? View.GONE : View.VISIBLE);
                mNextButton.setVisibility(!mIsTracking ? View.GONE : View.VISIBLE);
            }
        });

        // if we detect any target, we bind the loaded augmented content to this target
        // actually this is done automatically by assigning the model to the right COS at creation
        /*
        if ( (mAugmentedModel != null) && enabled )
        {
            mAugmentedModel.setCoordinateSystemID(trackingValue.getCoordinateSystemID());
        }
        */
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

	@Override
	protected void loadContents() 
	{
        // If you want to occlude the augmented content
        /*
        mOcclusionModel = loadModel("cup_tracking/SurfaceModel.obj");
        mAidModel = loadModel("cup_tracking/VIS_INIT.obj");
        mAugmentedModel = loadModel("Screw.zip");
        mAugmentedModel.setScale(new Vector3d(0.05f, 0.05f, 0.05f));
        mAugmentedModel.setTranslation(new Vector3d(-11f, 58f, 44f));
        //mAugmentedModel.setRotation(new Rotation(0f, 0f, 0f));
        mAugmentedToolModel = loadModel("Screwdriver.zip");
        mAugmentedToolModel.setScale(new Vector3d(0.3f, 0.3f, 0.3f));
        mAugmentedToolModel.setTranslation(new Vector3d(-10.7f, 57.4f, 66f));
        mAugmentedToolModel.setRotation(new Rotation(0f, -(float)Math.PI / 2f, 0f));
        */
        mOcclusionModel = loadModel("mouse_tracking/SurfaceModel.obj");
        mAidModel = loadModel("mouse_tracking/VIS_INIT.obj");
        mTrackingModel = loadModel("mouse_tracking/VIS_TRACK.obj");
        mAugmentedModel = loadModel("Screw.zip");
        mAugmentedModel.setScale(new Vector3d(0.1f, 0.1f, 0.1f));
        mAugmentedModel.setTranslation(new Vector3d(-0f, -33f, 17.5f));
        mAugmentedToolModel = loadModel("Screwdriver.zip");
        mAugmentedToolModel.setScale(new Vector3d(0.2f, 0.2f, 0.2f));
        mAugmentedToolModel.setTranslation(new Vector3d(-0f, -33f, 50f));
        mAugmentedToolModel.setRotation(new Rotation(0f, -(float)Math.PI / 2f, 0f));

		//final File envmapPath = AssetsManager.getAssetPathAsFile(getApplicationContext(), "env_map.png");
		//metaioSDK.loadEnvironmentMap(envmapPath, EENV_MAP_FORMAT.EEMF_LATLONG);

		if (mTrackingModel != null)
            mTrackingModel.setCoordinateSystemID(1);
        if (mAugmentedModel != null)
            mAugmentedModel.setCoordinateSystemID(1);
        if (mAugmentedToolModel != null)
        {
            mAugmentedToolModel.setCoordinateSystemID(1);
            // Not visible by default
            mAugmentedToolModel.setVisible(false);
        }
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
		//setTrackingConfiguration("cup_tracking/Tracking.xml");
        setTrackingConfiguration("mouse_tracking/Tracking.xml");
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
                    setTracking(trackingValue.isTrackingState());
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
    /* Toggle animation on/off on a given 3D model
	 */
    private void setAnimationEnabled(IGeometry model, Boolean enabled)
    {
        if ( model == null )
            return;

        // Start animation
        if ( enabled && ( model.getAnimationNames().size() > 0 ) )
        {
            model.startAnimation(model.getAnimationNames().get(0), true);
        }
        // Stop animation
        else if ( !enabled )
        {
            model.stopAnimation();
        }
    }

    /* Load a given tracking configuration
     */
    private boolean setTrackingConfiguration(final String path)
	{
		boolean result = false;
		try
		{
			// set tracking configuration
			final File xmlPath = AssetsManager.getAssetPathAsFile(getApplicationContext(), path);			
			result = metaioSDK.setTrackingConfiguration(xmlPath);
			MetaioDebug.log("Loaded tracking configuration "+xmlPath);			
		}       
		catch (Exception e)
		{
			MetaioDebug.log(Log.ERROR, "Error loading tracking configuration: "+ path + " " +e.getMessage());
			return result;
		}		
		return result;
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

        // Start Info Activity
        if ( mAugmentedToolModel.equals(geometry) || isChild(mAugmentedToolModel, geometry) )
        {
            Intent intent = new Intent(getApplicationContext(), InformationActivity.class);
            startActivity(intent);
        }
	}
	
}
