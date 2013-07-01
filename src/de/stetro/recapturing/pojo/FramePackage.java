package de.stetro.recapturing.pojo;

import org.opencv.core.Mat;

public class FramePackage {
	private Mat frame;
	private long detectionTime;
	private long descriptionTime;
	private long matchingTime;
	private long matches;
	private long displayRate;
	private long dltTime;
	private long filterTime;

	public Mat getFrame() {
		return frame;
	}

	public void setFrame(Mat frame) {
		this.frame = frame;
	}

	public long getDetectionTime() {
		return detectionTime;
	}

	public void setDetectionTime(long detectionTime) {
		this.detectionTime = detectionTime;
	}

	public long getDescriptionTime() {
		return descriptionTime;
	}

	public void setDescriptionTime(long descriptionTime) {
		this.descriptionTime = descriptionTime;
	}

	public long getMatches() {
		return matches;
	}

	public void setMatches(long matches) {
		this.matches = matches;
	}

	public long getDisplayRate() {
		return displayRate;
	}

	public void setDisplayRate(long displayTime) {
		this.displayRate = displayTime;
	}

	public long getMatchingTime() {
		return matchingTime;
	}

	public void setMatchingTime(long matchingTime) {
		this.matchingTime = matchingTime;
	}

	public long getDltTime() {
		return dltTime;
	}

	public void setDltTime(long dltTime) {
		this.dltTime = dltTime;
	}

	public long getFilterTime() {
		return filterTime;
	}

	public void setFilterTime(long filterTime) {
		this.filterTime = filterTime;
	}
}
