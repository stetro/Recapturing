package de.stetro.recapturing;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
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
import de.stetro.recapturing.main.MainActivity;
import de.stetro.recapturing.pojo.FramePackage;

/**
 * Image registration process with time measurement of each operation
 * 
 * @author Steffen Troester
 */
public class RecapturingProcessor {

	private static final String TAG = "Recapturing Processor";
	private boolean registrationMethod = true;
	private Mat grayPicture;
	private Mat templateDescriptors;
	private Mat templateGrayPicture;
	private MatOfKeyPoint templateMatOfKeyPoint;
	private MatOfDMatch matOfDMatchesOfSceneAndObject;
	private Mat blitPicture;

	private static final RecapturingMode MODE = RecapturingMode.FEAUTURE_BASED;

	private static int DISTANCE_LIMIT = 20;
	private static final int DESCRIPTOR_MATCHING_METHOD = DescriptorMatcher.BRUTEFORCE_HAMMING;
	private static final int DESCRIPTOR_EXTRATOR_METHOD = DescriptorExtractor.ORB;
	private static final int FEATURE_DETECTOR_METHOD = FeatureDetector.ORB;
	private Mat templateMat;
	private final Scalar whitecolor = new Scalar(0xFF, 0xFF, 0xFF, 0xFF);
	private static String filenameDetection;
	private static String filename;;

	/**
	 * Prepares and allocate the gray scale {@link Mat} images
	 * 
	 * @param width
	 * @param height
	 */
	public synchronized void prepareViewSize(int width, int height) {
		grayPicture = new Mat(height, width, CvType.CV_8UC1);
		blitPicture = new Mat(height, width, CvType.CV_8UC1);
		setUpConfigurationFiles();
	}

	/**
	 * Generates configuration files for each {@link FeatureDetector} or
	 * {@link DescriptorExtractor}
	 */
	private void setUpConfigurationFiles() {
		filename = MainActivity.getTempFileName("yml");
		filenameDetection = MainActivity.getTempFileName("xml");
		switch (FEATURE_DETECTOR_METHOD) {
		case FeatureDetector.FAST:
			writeFile(filename, "<?xml version=\"1.0\"?>\n<opencv_storage>\n<threshold>130</threshold>\n<nonmaxSuppression>1</nonmaxSuppression>\n</opencv_storage>\n");
			break;
		case FeatureDetector.ORB:
			writeFile(filename, "<?xml version=\"1.0\"?>\n<opencv_storage>\n<threshold>130</threshold>\n<nonmaxSuppression>1</nonmaxSuppression>\n</opencv_storage>\n");
			break;
		case FeatureDetector.BRISK:
			writeFile(filename, "%YAML:1.0\n" + "radiusList: 3.0\n" + "numberList: 3.0\n" + "dMax: 5.85\n" + "dMin: 8.2\n" + "indexChanges: 30\n" + "threshold: 10\n" + "octaves: 1\n");
			break;
		}
		switch (DESCRIPTOR_EXTRATOR_METHOD) {
		case DescriptorExtractor.FREAK:
			writeFile(filenameDetection, "%YAML:1.0\n" + "radiusList: 3.0\n" + "numberList: 3.0\n" + "dMax: 5.85\n" + "dMin: 8.2\n" + "indexChanges: 30\n" + "threshold: 10\n" + "nOctaves: 1\n");
			break;
		}
	}

	/**
	 * Writes content to file path (Configuration files)
	 * 
	 * @param path
	 * @param content
	 */
	protected static void writeFile(String path, String content) {
		FileOutputStream stream = null;
		try {
			stream = new FileOutputStream(new File(path));
			FileChannel fc = stream.getChannel();
			fc.write(Charset.defaultCharset().encode(content));
		} catch (IOException e) {

		} finally {
			if (stream != null)
				try {
					stream.close();
				} catch (IOException e) {

				}
		}
	}

	/**
	 * Main process routine with input Image from {@link MainActivity} (Camera
	 * frame)
	 * 
	 * @param inputPicture
	 * @return
	 */
	public synchronized FramePackage process(Mat inputPicture) {
		FramePackage fp = new FramePackage();
		long start = System.currentTimeMillis();
		convertToGrayScaleImage(inputPicture, grayPicture);
		if (MODE == RecapturingMode.FEAUTURE_BASED) {
			MatOfKeyPoint matOfKeyPoint = detectFeatures(grayPicture, fp);
			if (templateDescriptors != null) {
				Mat descriptors = computeDescriptors(grayPicture, matOfKeyPoint, fp);
				matOfDMatchesOfSceneAndObject = findDescriptorMatches(descriptors, templateDescriptors, fp);

				filterBestMatches(matOfDMatchesOfSceneAndObject, matOfKeyPoint, templateMatOfKeyPoint, grayPicture, fp);
			} else {
				Features2d.drawKeypoints(grayPicture, matOfKeyPoint, grayPicture);
			}
		} else {
			if (templateGrayPicture != null) {
				Point phaseCorrelate = Imgproc.phaseCorrelate(inputPicture, templateMat);
				Core.line(grayPicture, phaseCorrelate, phaseCorrelate, whitecolor);
			}
		}
		fp.setDisplayRate(1000 / (System.currentTimeMillis() - start));
		fp.setFrame(grayPicture);
		return fp;
	}

	private Mat computeDescriptors(Mat grayPicture2, MatOfKeyPoint matOfKeyPoint, FramePackage fp) {
		Long begin = System.currentTimeMillis();
		Mat computeDescriptors = computeDescriptors(grayPicture2, matOfKeyPoint);
		fp.setDescriptionTime(System.currentTimeMillis() - begin);
		return computeDescriptors;
	}

	private MatOfDMatch findDescriptorMatches(Mat descriptors, Mat templateDescriptors2, FramePackage fp) {
		Long begin = System.currentTimeMillis();
		MatOfDMatch findDescriptorMatches = findDescriptorMatches(descriptors, templateDescriptors2);
		fp.setMatchingTime(System.currentTimeMillis() - begin);
		return findDescriptorMatches;
	}

	private MatOfKeyPoint detectFeatures(Mat grayPicture2, FramePackage fp) {
		Long begin = System.currentTimeMillis();
		MatOfKeyPoint detectFeatures = detectFeatures(grayPicture2);
		fp.setDetectionTime(System.currentTimeMillis() - begin);
		return detectFeatures;
	}

	private void filterBestMatches(MatOfDMatch matchesOfBoth, MatOfKeyPoint matOfSceneKeyPoint, MatOfKeyPoint matOfObjectKeyPoint, Mat grayPicture, FramePackage fp) {
		Long begin = System.currentTimeMillis();
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
		fp.setFilterTime(System.currentTimeMillis() - begin);

		calculateHomography(grayPicture, fp, matchedPointsInScene, matchedPointsInObject);

	}

	private void calculateHomography(Mat grayPicture, FramePackage fp, List<Point> matchedPointsInScene, List<Point> matchedPointsInObject) {
		Long begin;
		fp.setMatches(matchedPointsInScene.size());
		if (matchedPointsInScene.size() >= 4) {
			begin = System.currentTimeMillis();
			MatOfPoint2f bestSceneKeyPoint = new MatOfPoint2f();
			bestSceneKeyPoint.fromList(matchedPointsInScene);
			MatOfPoint2f bestObjectKeyPoint = new MatOfPoint2f();
			bestObjectKeyPoint.fromList(matchedPointsInObject);
			Mat homography = Calib3d.findHomography(bestObjectKeyPoint, bestSceneKeyPoint, Calib3d.RANSAC, 10);
			// Mat homography = Calib3d.findHomography(bestObjectKeyPoint,
			// bestSceneKeyPoint, Calib3d.LMEDS,10);
			Imgproc.warpPerspective(templateGrayPicture, blitPicture, homography, blitPicture.size());
			Core.addWeighted(grayPicture, 0.5, blitPicture, 0.5, 0.0, grayPicture);
			fp.setDltTime(System.currentTimeMillis() - begin);
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
		// descriptorExtractor.read(filenameDetection);
		descriptorExtractor.compute(inputPicture, matOfKeyPoint, descriptors);
		return descriptors;
	}

	private static MatOfKeyPoint detectFeatures(Mat inputPicture) {

		FeatureDetector featureDetector = FeatureDetector.create(FEATURE_DETECTOR_METHOD);
		featureDetector.read(filename);
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
