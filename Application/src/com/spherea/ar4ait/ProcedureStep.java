package com.spherea.ar4ait;

import java.util.ArrayList;

import android.app.Activity;
import android.widget.Toast;

import com.metaio.sdk.jni.IGeometry;


public class ProcedureStep
{
    /**
     * 3D augmented content model for the step
     */
    private IGeometry mAugmentedModel = null;

    /**
     * 3D augmented tool models for the step
     */
    private ArrayList<IGeometry> mAugmentedToolModels = new ArrayList<IGeometry>();

    /**
     * Description for the step
     */
    private CharSequence mDescription = "";

    /**
     * Warning (if any) for the step
     */
    private CharSequence mWarning = "";

    /**
     * Flag to store whether the augmented tool models are visible or not
     */
    private boolean mAreToolsVisible = false;

    /**
     * Flag to store whether the augmented tool models are visible or not
     */
    private boolean mAreToolsVisibleWhenHidden = false;

    /* Constructor
      */
    public ProcedureStep(IGeometry augmentedModel) {
        mAugmentedModel = augmentedModel;

        if (mAugmentedModel != null) {
            mAugmentedModel.setCoordinateSystemID(1);
            mAugmentedModel.setVisible(false);
        }
    }

    /**
     *  Adds an augmented tool model
     */
    void addAugmentedToolModel(IGeometry augmentedToolModel) {
        if (augmentedToolModel!=null) {
            augmentedToolModel.setCoordinateSystemID(1);
            augmentedToolModel.setVisible(mAreToolsVisible);
            mAugmentedToolModels.add(augmentedToolModel);
        }
    }

    /**
     *  Get any description message
	 */
    public CharSequence getDescription() {

        return mDescription;
    }

    /**
     *  Set description message
	 */
    public void setDescription(CharSequence description) {

        mDescription = description;
    }

    /**
     *  Get any warning message
	 */
    public CharSequence getWarning() {

        return mWarning;
    }

    /**
     *  Set warning message
	 */
    public void setWarning(CharSequence warning) {

        mWarning = warning;
    }

    /**
     *  Display the model associated with the step
     */
    public void show() {
        if ( mAugmentedModel != null ) {
            mAugmentedModel.setVisible(true);
            // Start animation if any
            startAnimation();
            // Keeps the tools hidden
            setToolsVisible(mAreToolsVisibleWhenHidden);
        }
    }

    /**
     * Hide the model associated with the step
     */
    public void hide()
    {
        if ( mAugmentedModel != null ) {
            mAugmentedModel.setVisible(false);
            // Stop animation if any
            stopAnimation();
            // Hides the tools
            mAreToolsVisibleWhenHidden = mAreToolsVisible;
            setToolsVisible(false);
        }
    }

    /**
     *  Check if the tools associated with the step is visible
	 */
    public Boolean areToolsVisible() {
        return mAreToolsVisible;
    }

    /**
     *  Display the tools associated with the step
	 */
    public void setToolsVisible(Boolean visible) {
        for (int i = 0; i < mAugmentedToolModels.size(); i++) {
            mAugmentedToolModels.get(i).setVisible(visible);
        }
        mAreToolsVisible = visible;
    }

    /**
     * Toggle animation on/off on a given 3D model
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

    /**
     *  Launch animation on the model for this step
	 */
    public void startAnimation() {
        setAnimationEnabled(mAugmentedModel, true);
    }

    /**
     *  Stop animation on the model for this step
	 */
    public void stopAnimation() {
        setAnimationEnabled(mAugmentedModel, false);
    }

    /**
     *  Check if a given 3D model is child of another
      */
    private Boolean isChild(IGeometry model, IGeometry geometry) {
        IGeometry parent = geometry.getParentGeometry();

        while ( ( parent != null ) && !parent.equals(model) )
        {
            parent = parent.getParentGeometry();
        }

        return ( parent != null ) && parent.equals(model);
    }

    /**
     * Slot called when a geometry has been touched
     * @param geometry the picked geometry
     * @param activity the AR activity
     * @return true if the callback has been handled
     */
    public Boolean onGeometryTouched(IGeometry geometry, Activity activity)
    {
        for (int i = 0; i < mAugmentedToolModels.size() ; i++) {
            IGeometry augmentedToolModel = mAugmentedToolModels.get(i);
            if (augmentedToolModel.equals(geometry)) {
                String name = geometry.getName();
                Toast.makeText(activity.getApplicationContext(), name, Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        return false;
    }
}
