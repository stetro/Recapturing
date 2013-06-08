package de.stetro.recapturing;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class MainActivity extends Activity implements CvCameraViewListener, View.OnTouchListener {

	private static final int PREVIEW_SIZE = 300;
	private static final int MAX_WIDTH = 680;
	private static final int MAX_HEIGHT = 460;

	private final class SeekBarDistanceChangeListener implements OnSeekBarChangeListener {
		private final TextView tv;
		private final SeekBar sb;

		private SeekBarDistanceChangeListener(TextView tv, SeekBar sb) {
			this.tv = tv;
			this.sb = sb;
		}

		@Override
		public void onStopTrackingTouch(SeekBar arg0) {
		}

		@Override
		public void onStartTrackingTouch(SeekBar arg0) {
		}

		@Override
		public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
			recapturingProcessor.setDistance((int) sb.getProgress());
			tv.setText("Feature Distance Limit: " + (int) sb.getProgress());
		}
	}

	private final class OpenCVBaseLoaderCallbackListener extends BaseLoaderCallback {
		private OpenCVBaseLoaderCallbackListener(Context AppContext) {
			super(AppContext);
		}

		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS:
				Log.i(TAG, "OpenCV loaded successfully");
				openCvCameraView.setOnTouchListener(MainActivity.this);
				openCvCameraView.enableView();
				break;
			default:
				super.onManagerConnected(status);
				break;
			}
		}
	}

	class PickImageOnClickListener implements OnClickListener {
		@Override
		public void onClick(View v) {
			Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			startActivityForResult(pickPhoto, 1);
		}
	}

	private static final String TAG = "Recapturing App";

	private CameraBridgeViewBase openCvCameraView;
	private RecapturingProcessor recapturingProcessor;

	private int viewWidth;
	private int viewHeight;

	private BaseLoaderCallback mLoaderCallback = new OpenCVBaseLoaderCallbackListener(this);

	private ImageButton imagePicker;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		prepareLayoutAndFlags();
		prepareCameraView();
		prepareRecapturingProcessor();
		prepareImagePicker();
		prepareDistanceSeekBar();
	}

	private void prepareDistanceSeekBar() {
		final SeekBar sb = (SeekBar) findViewById(R.id.distance_seeker);
		sb.setProgress(20);
		final TextView tv = (TextView) findViewById(R.id.distance_text);
		sb.setOnSeekBarChangeListener(new SeekBarDistanceChangeListener(tv, sb));
	}

	private void prepareLayoutAndFlags() {
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

		setContentView(R.layout.main_layout);
	}

	private void prepareImagePicker() {
		imagePicker = (ImageButton) findViewById(R.id.image_picker);
		imagePicker.setOnClickListener(new PickImageOnClickListener());
	}

	private void prepareRecapturingProcessor() {
		recapturingProcessor = new RecapturingProcessor();
		recapturingProcessor.prepareNewViewWithImage();
	}

	private void prepareCameraView() {
		openCvCameraView = (CameraBridgeViewBase) findViewById(R.id.recapturing_camera_preview);
		openCvCameraView.setMaxFrameSize(MAX_WIDTH, MAX_HEIGHT);
		openCvCameraView.setCvCameraViewListener(this);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (openCvCameraView != null)
			openCvCameraView.disableView();
	}

	@Override
	public void onResume() {
		super.onResume();
		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
	}

	public void onDestroy() {
		super.onDestroy();
		if (openCvCameraView != null)
			openCvCameraView.disableView();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.i(TAG, "Menu Item selected " + item);
		if (item.getItemId() == R.id.menu_load_image) {
			recapturingProcessor.prepareNewViewWithImage();
		} else if (item.getItemId() == R.id.menu_toggle_method) {
			recapturingProcessor.toggleRegistrationMethod();
		}
		return true;
	}

	public void onCameraViewStarted(int width, int height) {
		viewWidth = width;
		viewHeight = height;
		recapturingProcessor.prepareViewSize(width, height);
	}

	public void onCameraViewStopped() {
	}

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
		return recapturingProcessor.process(inputFrame);
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
					preview = resize(bitmap, PREVIEW_SIZE);
					templateBitmap = resize(bitmap, MAX_WIDTH);
					imagePicker.setImageBitmap(preview);
					recapturingProcessor.setTemplateBitmap(templateBitmap);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			break;
		}
	}

	private Bitmap resize(Bitmap bitmap, int previewSize) {
		Bitmap resized;
		if (bitmap.getHeight() > bitmap.getWidth()) {
			float factor = ((float) bitmap.getWidth() / (float) bitmap.getHeight());
			resized = Bitmap.createScaledBitmap(bitmap, (int) (previewSize * factor), previewSize, true);
		} else {
			float factor = ((float) bitmap.getHeight() / (float) bitmap.getWidth());
			resized = Bitmap.createScaledBitmap(bitmap, previewSize, (int) (previewSize * factor), true);
		}
		return resized;
	}
}
