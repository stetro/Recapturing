package de.stetro.recapturing.main;

import java.io.File;
import java.io.IOException;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import android.app.Activity;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import de.stetro.recapturing.R;
import de.stetro.recapturing.RecapturingProcessor;
import de.stetro.recapturing.main.util.ImageUtil;
import de.stetro.recapturing.main.util.OpenCVBaseLoaderCallbackListener;
import de.stetro.recapturing.main.util.PickImageOnClickListener;
import de.stetro.recapturing.main.util.SeekBarDistanceChangeListener;
import de.stetro.recapturing.pojo.FramePackage;

/**
 * Main view component of this application. Loads images and delegate them to
 * the {@link RecapturingProcessor}.
 * 
 * @author Steffen Troester
 */
public class MainActivity extends Activity implements CvCameraViewListener, View.OnTouchListener {

	/**
	 * Maximum image size (width or height)
	 */
	private static final int PREVIEW_SIZE = 300;
	/**
	 * Maximum image width of camera preview
	 */
	private static final int MAX_WIDTH = 680;
	/**
	 * Maximum image height of camera preview
	 */
	private static final int MAX_HEIGHT = 460;

	public static final String TAG = "Recapturing App";
	private static ContextWrapper context;

	private CameraBridgeViewBase openCvCameraView;
	private RecapturingProcessor recapturingProcessor;

	private int viewWidth;
	private int viewHeight;

	private BaseLoaderCallback mLoaderCallback = new OpenCVBaseLoaderCallbackListener(this);

	private ImageButton imagePicker;
	private TextView fpsTextView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		prepareLayoutAndFlags();
		prepareCameraView();
		prepareRecapturingProcessor();
		prepareImagePicker();
		prepareDistanceSeekBar();
		fpsTextView = (TextView) findViewById(R.id.fps_textview);
		MainActivity.context = this;
	}

	/**
	 * Prepares the the slide of main_layout.xml
	 */
	private void prepareDistanceSeekBar() {
		final SeekBar sb = (SeekBar) findViewById(R.id.distance_seeker);
		sb.setProgress(20);
		final TextView tv = (TextView) findViewById(R.id.distance_text);
		sb.setOnSeekBarChangeListener(new SeekBarDistanceChangeListener(tv, sb, recapturingProcessor));
	}

	/**
	 * Prepares the layout and fullscreen settings
	 */
	private void prepareLayoutAndFlags() {
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.main_layout);
	}

	/**
	 * prepares the image picker dialogue
	 */
	private void prepareImagePicker() {
		imagePicker = (ImageButton) findViewById(R.id.image_picker);
		imagePicker.setOnClickListener(new PickImageOnClickListener(this));
	}

	private void prepareRecapturingProcessor() {
		recapturingProcessor = new RecapturingProcessor();
	}

	/**
	 * prepares the camera view element of main_layout.xml and camera capturing
	 * size
	 */
	private void prepareCameraView() {
		setOpenCvCameraView((CameraBridgeViewBase) findViewById(R.id.recapturing_camera_preview));
		getOpenCvCameraView().setMaxFrameSize(MAX_WIDTH, MAX_HEIGHT);
		getOpenCvCameraView().setCvCameraViewListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (getOpenCvCameraView() != null)
			getOpenCvCameraView().disableView();
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
	}

	public void onDestroy() {
		super.onDestroy();
		if (getOpenCvCameraView() != null)
			getOpenCvCameraView().disableView();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

	/**
	 * Forward the prepared imagesize to {@link RecapturingProcessor}
	 */
	public void onCameraViewStarted(int width, int height) {
		viewWidth = width;
		viewHeight = height;
		recapturingProcessor.prepareViewSize(width, height);
	}

	public void onCameraViewStopped() {
	}

	/**
	 * Pipes touch events to {@link RecapturingProcessor}
	 */
	public boolean onTouch(View view, MotionEvent event) {
		int xpos, ypos;
		xpos = (view.getWidth() - viewWidth) / 2;
		xpos = (int) event.getX() - xpos;
		ypos = (view.getHeight() - viewHeight) / 2;
		ypos = (int) event.getY() - ypos;
		if (xpos >= 0 && xpos <= viewWidth && ypos >= 0 && ypos <= viewHeight) {
			recapturingProcessor.deliverTouchEvent(xpos, ypos);
		}
		return false;
	}

	public Mat onCameraFrame(Mat inputFrame) {
		FramePackage fp = recapturingProcessor.process(inputFrame);
		displayFPS(fp);
		fp.getFrame();
		return fp.getFrame();
	}

	private void displayFPS(final FramePackage fp) {
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				fpsTextView.setText("FPS: " + fp.getDisplayRate() + "\n" + "Detection:" + fp.getDetectionTime() + "ms\n" + "Description:" + fp.getDescriptionTime() + "ms\n" + "Matching:"
						+ fp.getMatchingTime() + "ms\n" + "Good Matches:" + fp.getMatches() + "\n" + "Filter:" + fp.getFilterTime() + "ms\n" + "Homography:" + fp.getDltTime() + "ms\n");

			}
		});
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
		super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
		switch (requestCode) {
		case 0:
			if (resultCode == RESULT_OK) {
				Uri selectedImage = imageReturnedIntent.getData();
				imagePicker.setImageURI(selectedImage);
			}
			break;
		case 1:
			if (resultCode == RESULT_OK) {
				Log.i(TAG, "Load template image ...");
				Uri selectedImage = imageReturnedIntent.getData();
				Bitmap bitmap, preview, templateBitmap;
				try {
					bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
					preview = ImageUtil.resize(bitmap, PREVIEW_SIZE);
					templateBitmap = ImageUtil.resize(bitmap, MAX_WIDTH);
					imagePicker.setImageBitmap(preview);
					recapturingProcessor.setTemplateBitmap(templateBitmap);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			break;
		}
	}

	/**
	 * Get temporary file path of application context with a specific file
	 * extension.
	 * 
	 * @param extension
	 * @return String file path
	 */
	public static String getTempFileName(String extension) {
		File cache = context.getCacheDir();
		if (!extension.startsWith("."))
			extension = "." + extension;
		try {
			File tmp = File.createTempFile("OpenCV", extension, cache);
			String path = tmp.getAbsolutePath();
			tmp.delete();
			return path;
		} catch (IOException e) {
			Log.d("ERROR", "Failed to get temp file name. Exception is thrown: " + e);
		}
		return null;
	}

	public CameraBridgeViewBase getOpenCvCameraView() {
		return openCvCameraView;
	}

	public void setOpenCvCameraView(CameraBridgeViewBase openCvCameraView) {
		this.openCvCameraView = openCvCameraView;
	}
}
