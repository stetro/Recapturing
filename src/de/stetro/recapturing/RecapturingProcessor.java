package de.stetro.recapturing;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.calib3d.Calib3d;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.features2d.DMatch;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.features2d.KeyPoint;
import org.opencv.imgproc.Imgproc;

import android.graphics.Bitmap;
import android.util.Log;

public class RecapturingProcessor {

	private static final String TAG = "Recapturing Processor";
	private boolean registrationMethod = true;
	private Mat grayPicture;
	private Mat templateDescriptors;
	private Mat templateGrayPicture;
	private MatOfKeyPoint templateMatOfKeyPoint;
	private MatOfDMatch matOfDMatchesOfSceneAndObject;
	private Mat blitPicture;

	private static final RecapturingMode MODE = RecapturingMode.AREA_BASED;

	private static int DISTANCE_LIMIT = 20;
	private static final int DESCRIPTOR_MATCHING_METHOD = DescriptorMatcher.BRUTEFORCE_HAMMINGLUT;
	private static final int DESCRIPTOR_EXTRATOR_METHOD = DescriptorExtractor.ORB;
	private static final int FEATURE_DETECTOR_METHOD = FeatureDetector.ORB;
	private Mat templateMat;
	private final Scalar whitecolor = new Scalar(0xFF, 0xFF, 0xFF, 0xFF);;

	public RecapturingProcessor() {

	}

	public synchronized void prepareNewViewWithImage() {

	}

	public synchronized void prepareViewSize(int width, int height) {
		grayPicture = new Mat(height, width, CvType.CV_8UC1);
		blitPicture = new Mat(height, width, CvType.CV_8UC1);
	}

	public synchronized Mat process(Mat inputPicture) {
		convertToGrayScaleImage(inputPicture, grayPicture);
		if (MODE == RecapturingMode.FEAUTURE_BASED) {
			MatOfKeyPoint matOfKeyPoint = detectFeatures(grayPicture);
			if (templateDescriptors != null) {
				Mat descriptors = computeDescriptors(grayPicture, matOfKeyPoint);
				matOfDMatchesOfSceneAndObject = findDescriptorMatches(descriptors, templateDescriptors);

				filterBestMatches(matOfDMatchesOfSceneAndObject, matOfKeyPoint, templateMatOfKeyPoint, grayPicture);
			} else {
				Features2d.drawKeypoints(grayPicture, matOfKeyPoint, grayPicture);
			}
		} else {
			if (templateGrayPicture != null) {
				Point phaseCorrelate = Imgproc.phaseCorrelate(inputPicture, templateMat);
				Core.line(grayPicture, phaseCorrelate, phaseCorrelate, whitecolor);
			}
		}

		return grayPicture;
	}

	private void filterBestMatches(MatOfDMatch matchesOfBoth, MatOfKeyPoint matOfSceneKeyPoint, MatOfKeyPoint matOfObjectKeyPoint, Mat grayPicture) {
		List<DMatch> matchesList = matchesOfBoth.toList();
		List<KeyPoint> sceneKeyPoints = matOfSceneKeyPoint.toList();
		List<KeyPoint> objectKeyPoint = matOfObjectKeyPoint.toList();
		List<Point> matchedPointsInScene = new ArrayList<Point>();
		List<Point> matchedPointsInObject = new ArrayList<Point>();
		for (int i = 0; i < matchesList.size(); i++) {
			if (matchesList.get(i).distance <= DISTANCE_LIMIT) {
				Point scenePoint = sceneKeyPoints.get(matchesList.get(i).trainIdx).pt;
				Point objectPoint = objectKeyPoint.get(matchesList.get(i).queryIdx).pt;
				matchedPointsInScene.add(scenePoint);
				matchedPointsInObject.add(objectPoint);
				if (grayPicture != null)
					Core.circle(grayPicture, scenePoint, 5, new Scalar(0xFF, 0xFF, 0xFF, 0xFF));
			}
		}

		if (matchedPointsInScene.size() >= 4) {
			MatOfPoint2f bestSceneKeyPoint = new MatOfPoint2f();
			bestSceneKeyPoint.fromList(matchedPointsInScene);
			MatOfPoint2f bestObjectKeyPoint = new MatOfPoint2f();
			bestObjectKeyPoint.fromList(matchedPointsInObject);
			Mat homography = Calib3d.findHomography(bestObjectKeyPoint, bestSceneKeyPoint, Calib3d.RANSAC, 10);
			Imgproc.warpPerspective(templateGrayPicture, blitPicture, homography, blitPicture.size());
			Core.addWeighted(grayPicture, 0.5, blitPicture, 0.5, 0.0, grayPicture);

		}

	}

	private static MatOfDMatch findDescriptorMatches(Mat descriptors, Mat templateDescriptors) {
		DescriptorMatcher descriptorMatcher = DescriptorMatcher.create(DESCRIPTOR_MATCHING_METHOD);
		MatOfDMatch matches = new MatOfDMatch();
		try {
			descriptorMatcher.match(templateDescriptors, descriptors, matches);
		} catch (Exception e) {
			Log.e(TAG, "Feature Matching was not successfully");
		}
		return matches;
	}

	private static Mat computeDescriptors(Mat inputPicture, MatOfKeyPoint matOfKeyPoint) {
		Mat descriptors = new Mat();
		DescriptorExtractor descriptorExtractor = DescriptorExtractor.create(DESCRIPTOR_EXTRATOR_METHOD);
		descriptorExtractor.compute(inputPicture, matOfKeyPoint, descriptors);
		return descriptors;
	}

	private static MatOfKeyPoint detectFeatures(Mat inputPicture) {
		FeatureDetector featureDetector = FeatureDetector.create(FEATURE_DETECTOR_METHOD);
		MatOfKeyPoint matOfKeyPoint = new MatOfKeyPoint();
		featureDetector.detect(inputPicture, matOfKeyPoint);
		return matOfKeyPoint;
	}

	private static void convertToGrayScaleImage(Mat inputPicture, Mat destinationPicture) {
		Imgproc.cvtColor(inputPicture, destinationPicture, Imgproc.COLOR_RGB2GRAY);
	}

	public void toggleRegistrationMethod() {
		registrationMethod = !registrationMethod;
	}

	public void deliverTouchEvent(int x, int y) {

	}

	public void setTemplateBitmap(Bitmap bitmap) {
		templateMat = new Mat();
		Utils.bitmapToMat(bitmap, templateMat);
		templateGrayPicture = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC1);
		convertToGrayScaleImage(templateMat, templateGrayPicture);
		templateMatOfKeyPoint = detectFeatures(templateGrayPicture);
		Log.i(TAG, "loaded image has " + templateMatOfKeyPoint.toList().size() + " Keypoints");
		templateDescriptors = computeDescriptors(templateGrayPicture, templateMatOfKeyPoint);
	}

	public void setDistance(int distance) {
		DISTANCE_LIMIT = distance;
	}
}
