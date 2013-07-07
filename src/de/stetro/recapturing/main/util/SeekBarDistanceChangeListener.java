package de.stetro.recapturing.main.util;

import de.stetro.recapturing.RecapturingProcessor;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;

/**
 * Pipes slider information to recapturing processor
 * 
 * @author Steffen Troester
 * 
 */
public class SeekBarDistanceChangeListener implements OnSeekBarChangeListener {
	private final TextView tv;
	private final SeekBar sb;
	private RecapturingProcessor recapturingProcessor;

	public SeekBarDistanceChangeListener(TextView tv, SeekBar sb, RecapturingProcessor rp) {
		this.tv = tv;
		this.sb = sb;
		this.recapturingProcessor = rp;
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