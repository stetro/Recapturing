package de.stetro.recapturing.main.util;

import android.graphics.Bitmap;

/**
 * Static image util container
 * 
 * @author Steffen Troester
 * 
 */
public class ImageUtil {
	/**
	 * Resize a Bitmap
	 * 
	 * @param bitmap
	 * @param previewSize
	 *            maximum width or height
	 * @return resized image
	 */
	public static Bitmap resize(Bitmap bitmap, int previewSize) {
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
