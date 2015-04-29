package com.spherea.ar4ait;

import java.io.File;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
	 * 3D model
	 */
	private IGeometry mModel = null;

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
    private Boolean mDebugModel = false;

    /**
     * Debug flag to enabled features rendering
     */
    private Boolean mDebugFeatures = false;

    ImageButton mResetButton = null;
    ImageButton mModelButton = null;
    ImageButton mToolButton = null;
    ImageButton mAidButton = null;
    ImageButton mPreviousButton = null;
    ImageButton mNextButton = null;

	/**
	 * Metaio SDK callback handler
	 */
	private MetaioSDKCallbackHandler mCallbackHandler;	

	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);

           // Get buttons
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

	public void onCloseButtonClick(View v)
	{
		finish();
	}

    public void onAidButtonClick(View v)
    {
        mAidModel.setVisible( !mAidModel.isVisible() );
    }

    public void onModelButtonClick(View v)
    {
        mDebugModel = !mDebugModel;
        metaioSDK.sensorCommand("setDebugRenderMode", mDebugModel ? "model" : "off");
    }

    public void onToolButtonClick(View v)
    {
        mAugmentedToolModel.setVisible( !mAugmentedToolModel.isVisible() );
    }

	public void onResetButtonClick(View v)
	{
		metaioSDK.sensorCommand("reset");
	}

    public void onPreviousButtonClick(View v)
    {
        // Start animation
        //setAnimationEnabled(mAugmentedModel, true);
    }

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
                mResetButton.setVisibility(!mIsTracking ? View.GONE : View.VISIBLE);
                mModelButton.setVisibility(View.VISIBLE);
                mToolButton.setVisibility(!mIsTracking ? View.GONE : View.VISIBLE);
                mAidButton.setVisibility(mIsTracking ? View.GONE : View.VISIBLE);
                mPreviousButton.setVisibility(!mIsTracking ? View.GONE : View.VISIBLE);
                mNextButton.setVisibility(!mIsTracking ? View.GONE : View.VISIBLE);
            }
        });

        // if we detect any target, we bind the loaded augmented content to this target
        // actually this is done automatically by assiging the model to the right COS at creation
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
		//mModel = loadModel("cup.obj");
        // If you want to occlude the augmented content
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

		//final File envmapPath = AssetsManager.getAssetPathAsFile(getApplicationContext(), "env_map.png");
		//metaioSDK.loadEnvironmentMap(envmapPath, EENV_MAP_FORMAT.EEMF_LATLONG);

		if (mModel != null)
			mModel.setCoordinateSystemID(1);
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
		setTrackingConfiguration("cup_tracking/Tracking.xml");
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

        while ( ( parent != null ) && ( parent != model ) )
        {
            parent = parent.getParentGeometry();
        }

        return parent == model;
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
        if ( isChild(mAugmentedToolModel, geometry) )
        {
            Intent intent = new Intent(getApplicationContext(), InformationActivity.class);
            startActivity(intent);
        }
	}
	
}
