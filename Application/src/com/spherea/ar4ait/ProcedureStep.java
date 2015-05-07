package com.spherea.ar4ait;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.metaio.sdk.jni.IGeometry;
import com.metaio.tools.io.AssetsManager;

import java.io.File;
import java.net.MalformedURLException;

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

    /**
     * Description for the step
     */
    private CharSequence mDescription = "";

    /**
     * Warning (if any) for the step
     */
    private CharSequence mWarning = "";

    /* Constructor
      */
    public ProcedureStep(IGeometry augmentedModel, IGeometry augmentedToolModel) {
        mAugmentedModel = augmentedModel;
        mAugmentedToolModel = augmentedToolModel;

        if (mAugmentedModel != null) {
            mAugmentedModel.setCoordinateSystemID(1);
            // Not visible by default
            mAugmentedModel.setVisible(false);
        }
        if (mAugmentedToolModel != null)
        {
            mAugmentedToolModel.setCoordinateSystemID(1);
            // Not visible by default
            mAugmentedToolModel.setVisible(false);
        }
    }

    /* Get any description message
	 */
    public CharSequence getDescription()
    {
        return mDescription;
    }

    /* Set description message
	 */
    public void setDescription(CharSequence description)
    {
        mDescription = description;
    }

    /* Get any warning message
	 */
    public CharSequence getWarning()
    {
        return mWarning;
    }

    /* Set warning message
	 */
    public void setWarning(CharSequence warning)
    {
        mWarning = warning;
    }

    /* Display the model associated with the step
      */
    public void show()
    {
        if ( mAugmentedModel != null ) {
            mAugmentedModel.setVisible( true );
            // Start animation if any
            startAnimation();
        }
    }

    /* Hide the model associated with the step
      */
    public void hide()
    {
        if ( mAugmentedModel != null ) {
            mAugmentedModel.setVisible( false );
            // Stop animation if any
            stopAnimation();
        }
    }

    /* Check if the tools associated with the step is visible
	 */
    public Boolean isToolVisible() {
        return ( ( mAugmentedToolModel != null ) && mAugmentedToolModel.isVisible() );
    }

    /* Display the tools associated with the step
	 */
    public void setToolVisible(Boolean visible) {
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

    public Boolean onGeometryTouched(IGeometry geometry, Activity context)
    {
        // Start Info Activity
        if ( mAugmentedToolModel.equals(geometry) || isChild(mAugmentedToolModel, geometry) )
        {
            final File filePath = AssetsManager.getAssetPathAsFile(context.getApplicationContext(), "16776-MU-KB-051-B-manuel-utilisateur-CLAPET.pdf");

            Intent intent = new Intent(context.getApplicationContext(), InformationActivity.class);
            try {
                intent.putExtra("URL", filePath.toURI().toURL().toString());
                //intent.putExtra("URL", filePath.toURI().toURL().toString() + "#page=35");
                context.startActivity(intent);
                return true;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        return false;
    }
}
