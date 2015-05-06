package com.spherea.ar4ait;


import android.os.Bundle;
import android.app.Fragment;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.View.OnClickListener;
import android.widget.RadioGroup;
import android.widget.RadioButton;


public class SettingsView extends Fragment implements OnClickListener
{

    private RadioGroup mModeRadioGroup;
    private RadioButton mInitializeOnceModeButton;
    private RadioButton mEdgeOnlyModeButton;
    private RadioButton mHybridModeButton;
    private RangeSeekBar mNumFeaturesEdgeAlignmentSeekBar;
    private RangeSeekBar mMinQualityEdgeAlignmentSeekBar;
    private RangeSeekBar mSearchRangeRelativeEdgeAlignmentSeekBar;
    private RangeSeekBar mVisibilityTestDepthBiasEdgeAlignmentSeekBar;
    private RangeSeekBar mMinQualityEdgeTrackingSeekBar;
    private RangeSeekBar mSearchRangeRelativeEdgeTrackingSeekBar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ARActivity arActivity = (ARActivity)getActivity();
        View view  = inflater.inflate(R.layout.settingsview, container, false);

        mModeRadioGroup = (RadioGroup)view.findViewById(R.id.modeRadioGroup);
        mInitializeOnceModeButton = (RadioButton)view.findViewById(R.id.initializeOnceModeButton);
        mInitializeOnceModeButton.setOnClickListener(this);
        mEdgeOnlyModeButton = (RadioButton)view.findViewById(R.id.edgeOnlyModeButton);
        mEdgeOnlyModeButton.setOnClickListener(this);
        mHybridModeButton = (RadioButton)view.findViewById(R.id.hybridModeButton);
        mHybridModeButton.setOnClickListener(this);

        mNumFeaturesEdgeAlignmentSeekBar = (RangeSeekBar)view.findViewById(R.id.numFeaturesEdgeAlignmentSeekBar);
        mNumFeaturesEdgeAlignmentSeekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar bar, Object minValue, Object maxValue) {
                ((ARActivity)getActivity()).command("setNumFeatures", bar.getSelectedMaxValue().toString());
            }
        });

        mMinQualityEdgeAlignmentSeekBar = (RangeSeekBar)view.findViewById(R.id.minQualityEdgeAlignmentSeekBar);
        mMinQualityEdgeAlignmentSeekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar bar, Object minValue, Object maxValue) {
                ((ARActivity)getActivity()).command("setMinQuality", bar.getSelectedMaxValue().toString());
            }
        });

        mSearchRangeRelativeEdgeAlignmentSeekBar = (RangeSeekBar)view.findViewById(R.id.searchRangeRelativeEdgeAlignmentSeekBar);
        mSearchRangeRelativeEdgeAlignmentSeekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar bar, Object minValue, Object maxValue) {
                ((ARActivity)getActivity()).command("setSearchRangeRelative", bar.getSelectedMaxValue().toString());
            }
        });

        mVisibilityTestDepthBiasEdgeAlignmentSeekBar = (RangeSeekBar)view.findViewById(R.id.visibilityTestDepthBiasEdgeAlignmentSeekBar);
        mVisibilityTestDepthBiasEdgeAlignmentSeekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar bar, Object minValue, Object maxValue) {
                ((ARActivity)getActivity()).command("setVisibilityTestDepthBias", bar.getSelectedMaxValue().toString());
            }
        });

        mMinQualityEdgeTrackingSeekBar = (RangeSeekBar)view.findViewById(R.id.minQualityEdgeTrackingSeekBar);
        mMinQualityEdgeTrackingSeekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar bar, Object minValue, Object maxValue) {
                ((ARActivity)getActivity()).command("setMinQualityTrack", bar.getSelectedMaxValue().toString());
            }
        });

        mSearchRangeRelativeEdgeTrackingSeekBar = (RangeSeekBar)view.findViewById(R.id.searchRangeRelativeEdgeTrackingSeekBar);
        mSearchRangeRelativeEdgeTrackingSeekBar.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar bar, Object minValue, Object maxValue) {
                ((ARActivity)getActivity()).command("setSearchRangeRelativeTrack", bar.getSelectedMaxValue().toString());
            }
        });

        return view;
     }

    @Override
    public void onClick(View view) {
        ARActivity arActivity = (ARActivity)getActivity();
        switch (view.getId()) {
            case R.id.initializeOnceModeButton:
                arActivity.command("setMode", "initialize_once");
                break;
            case R.id.edgeOnlyModeButton:
                arActivity.command("setMode", "edge_only");
                break;
            case R.id.hybridModeButton:
                arActivity.command("setMode", "hybrid");
                break;
        }
    }

    /**
     * Refresh the view
     */
    public void refresh() {
        ARActivity arActivity = (ARActivity)getActivity();

        // Refresh the tracking mode
        String trackingMode = arActivity.command("getMode");
        if (trackingMode.equals("initialize_once")) {
            mModeRadioGroup.check(mInitializeOnceModeButton.getId());
        } else if (trackingMode.equals("edge_only")) {
            mModeRadioGroup.check(mEdgeOnlyModeButton.getId());
        } else if (trackingMode.equals("hybrid")) {
            mModeRadioGroup.check(mHybridModeButton.getId());
        }

        // Refresh the Alignment parameters
        int numFeatures = Integer.parseInt(arActivity.command("getNumFeatures"));
        mNumFeaturesEdgeAlignmentSeekBar.setSelectedMaxValue(numFeatures);
        Float minQuality = Float.valueOf(arActivity.command("getMinQuality"));
        mMinQualityEdgeAlignmentSeekBar.setSelectedMaxValue(minQuality);
        Float searchRangeRelative = Float.valueOf(arActivity.command("getSearchRangeRelative"));
        mSearchRangeRelativeEdgeAlignmentSeekBar.setSelectedMaxValue(searchRangeRelative);
        Float visibilityTestDepthBias = Float.valueOf(arActivity.command("getVisibilityTestDepthBias"));
        mVisibilityTestDepthBiasEdgeAlignmentSeekBar.setSelectedMaxValue(visibilityTestDepthBias);

        // Refresh the Tracking parameters
        Float minQualityTrack = Float.valueOf(arActivity.command("getMinQualityTrack"));
        mMinQualityEdgeTrackingSeekBar.setSelectedMaxValue(minQualityTrack);
        Float searchRangeRelativeTrack = Float.valueOf(arActivity.command("getSearchRangeRelativeTrack"));
        mSearchRangeRelativeEdgeTrackingSeekBar.setSelectedMaxValue(searchRangeRelativeTrack);

    }
}
