package com.spherea.ar4ait;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;

import com.metaio.sdk.ARViewActivity;
import com.metaio.sdk.MetaioDebug;
import com.metaio.sdk.jni.IGeometry;
import com.metaio.sdk.jni.IMetaioSDKCallback;
import com.metaio.sdk.jni.Rotation;
import com.metaio.sdk.jni.TrackingValues;
import com.metaio.sdk.jni.TrackingValuesVector;
import com.metaio.sdk.jni.Vector3d;
import com.metaio.tools.io.AssetsManager;

import java.io.File;

public class ProcedureStep
{
    /**
     * 3D augmented content model for the step
     */
    private IGeometry mAugmentedModel = null;

    /**
     * 3D augmented tool model for the step
     */
    private IGeometry mAugmentedToolModel = null;

    /* Constructor
      */
    public ProcedureStep(IGeometry augmentedModel, IGeometry augmentedToolModel) {
        mAugmentedModel = augmentedModel;
        mAugmentedToolModel = augmentedToolModel;

        if (mAugmentedModel != null)
            mAugmentedModel.setCoordinateSystemID(1);
        if (mAugmentedToolModel != null)
        {
            mAugmentedToolModel.setCoordinateSystemID(1);
            // Not visible by default
            mAugmentedToolModel.setVisible(false);
        }
    }

    /* Display the model associated with the step
      */
    public void show()
    {
        if ( mAugmentedModel != null ) {
            mAugmentedModel.setVisible( true );
        }
    }

    /* Hide the model associated with the step
      */
    public void hide()
    {
        if ( mAugmentedModel != null ) {
            mAugmentedModel.setVisible( false );
        }
    }

    /* Check if the tools associated with the step is visible
	 */
    public Boolean isToolVisible()
    {
        return ( ( mAugmentedToolModel != null ) && mAugmentedToolModel.isVisible() );
    }

    /* Display the tools associated with the step
	 */
    public void setToolVisible(Boolean visible)
    {
        if ( mAugmentedToolModel != null ) {
            mAugmentedToolModel.setVisible( visible );
        }
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

    /* Launch animation on the model for this step
	 */
    public void startAnimation()
    {
        setAnimationEnabled(mAugmentedModel, true);
    }

    /* Stop animation on the model for this step
	 */
    public void stopAnimation()
    {
        setAnimationEnabled(mAugmentedModel, false);
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

	public Boolean isTool(IGeometry geometry)
	{
        return ( mAugmentedToolModel.equals(geometry) || isChild(mAugmentedToolModel, geometry) );
	}
	
}
