package de.stetro.recapturing.main.util;

import de.stetro.recapturing.main.MainActivity;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;

/**
 * Image picking action listener
 * 
 * @author stetro
 * 
 */
public class PickImageOnClickListener implements OnClickListener {
	private MainActivity mainActivity;

	public PickImageOnClickListener(MainActivity ma) {
		this.mainActivity = ma;
	}

	@Override
	public void onClick(View v) {
		Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		mainActivity.startActivityForResult(pickPhoto, 1);
	}
}