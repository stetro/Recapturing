package de.stetro.recapturing.main.util;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;

import de.stetro.recapturing.main.MainActivity;

import android.util.Log;

/**
 * Load OpenCV Manager library with this callback
 * 
 * @author Steffen Troester
 * 
 */
public class OpenCVBaseLoaderCallbackListener extends BaseLoaderCallback {
	private MainActivity mainActivity;

	public OpenCVBaseLoaderCallbackListener(MainActivity AppContext) {
		super(AppContext);
		this.mainActivity = AppContext;
	}

	@Override
	public void onManagerConnected(int status) {
		switch (status) {
		case LoaderCallbackInterface.SUCCESS:
			Log.i(MainActivity.TAG, "OpenCV loaded successfully");
			mainActivity.getOpenCvCameraView().setOnTouchListener(mainActivity);
			mainActivity.getOpenCvCameraView().enableView();
			break;
		default:
			super.onManagerConnected(status);
			break;
		}
	}
}
