package com.writesbadcode.seba.omrtest;

import android.app.Activity;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.AKAZE;
import org.opencv.features2d.AgastFeatureDetector;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.Feature2D;
import org.opencv.features2d.FeatureDetector;
import org.opencv.features2d.Features2d;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.utils.Converters;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;


public class MainActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "OCVSample::Activity";
    private int w, h;
    private CameraBridgeViewBase mOpenCvCameraView;
    TextView tvName;
    Scalar RED = new Scalar(255, 0, 0);
    Scalar GREEN = new Scalar(0, 255, 0);


    AKAZE detector;

    Mat mask;

    DescriptorExtractor descriptor;
    DescriptorMatcher matcher;
    Mat descriptors2,descriptors1;
    Mat img1;
    MatOfKeyPoint keypoints1,keypoints2;

    CascadeClassifier faceDetector;
    MatOfRect faceDetections;
    File mCascadeFile;

    Mat mRgba;
    Mat mRgbaF;
    Mat mRgbaT;


    static {
        if (!OpenCVLoader.initDebug())
            Log.d("ERROR", "Unable to load OpenCV");
        else
            Log.d("SUCCESS", "OpenCV loaded");
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    try {
                        initializeOpenCVDependencies();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    private void initializeOpenCVDependencies() throws IOException {
        mOpenCvCameraView.enableView();
        detector = AKAZE.create();
        //descriptor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);


        img1 = new Mat();
        AssetManager assetManager = getAssets();
        InputStream istr = assetManager.open("a.jpeg");
        Bitmap bitmap = BitmapFactory.decodeStream(istr);
        Utils.bitmapToMat(bitmap, img1);
        Imgproc.cvtColor(img1, img1, Imgproc.COLOR_RGB2GRAY);
        img1.convertTo(img1, 0); //converting the image to match with the type of the cameras image


        mask = new Mat(img1.size(),CvType.CV_8U);
        descriptors1 = new Mat();
        keypoints1 = new MatOfKeyPoint();
        //detector.detect(img1, keypoints1,);
        detector.detectAndCompute(img1,mask, keypoints1,descriptors1);

//
//        InputStream is = assetManager.open("lbpcascade_frontalface.xml");
//        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
//        mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
//        FileOutputStream os = new FileOutputStream(mCascadeFile);
//
//        byte[] buffer = new byte[4096];
//        int bytesRead;
//        while ((bytesRead = is.read(buffer)) != -1) {
//            os.write(buffer, 0, bytesRead);
//        }
//        is.close();
//        os.close();
//
//
//        faceDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());

    }


    public MainActivity() {

        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        tvName = (TextView) findViewById(R.id.text1);

    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        w = width;
        h = height;

        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);
    }

    public void onCameraViewStopped() {

    }

    public Mat recognize(Mat aInputFrame) {

//        Mat rot = Imgproc.getRotationMatrix2D(new Point(aInputFrame.cols()/2,aInputFrame.rows()/2), 270,1);
//
//        Imgproc.warpAffine(aInputFrame,aInputFrame,rot,aInputFrame.size());

        Imgproc.cvtColor(aInputFrame, aInputFrame, Imgproc.COLOR_RGB2GRAY);
        descriptors2 = new Mat();
        keypoints2 = new MatOfKeyPoint();
        mask = new Mat(aInputFrame.size(), CvType.CV_8U);

        detector.detectAndCompute(aInputFrame,mask, keypoints2,descriptors2);

        // Matching
        MatOfDMatch matches = new MatOfDMatch();
        if (img1.type() == aInputFrame.type()) {
            matcher.match(descriptors1, descriptors2, matches);
        } else {
            return aInputFrame;
        }
        List<DMatch> matchesList = matches.toList();

        Double max_dist = 0.0;
        Double min_dist = 100d;

        for (int i = 0; i < matchesList.size(); i++) {
            Double dist = (double) matchesList.get(i).distance;
            if (dist < min_dist)
                min_dist = dist;
            if (dist > max_dist)
                max_dist = dist;
        }

        LinkedList<DMatch> good_matches = new LinkedList<DMatch>();
        for (int i = 0; i < matchesList.size(); i++) {
            if (matchesList.get(i).distance <= (1.5 * min_dist))
                good_matches.addLast(matchesList.get(i));
        }

        MatOfDMatch goodMatches = new MatOfDMatch();
        goodMatches.fromList(good_matches);
        Mat outputImg = new Mat();
        MatOfByte drawnMatches = new MatOfByte();
        if (aInputFrame.empty() || aInputFrame.cols() < 1 || aInputFrame.rows() < 1) {
            return aInputFrame;
        }
        Features2d.drawMatches(img1, keypoints1, aInputFrame, keypoints2, goodMatches, outputImg, GREEN, RED, drawnMatches, Features2d.NOT_DRAW_SINGLE_POINTS);
        Imgproc.resize(outputImg, outputImg, aInputFrame.size());

        return outputImg;

//
//        MatOfRect faceDetections = new MatOfRect();
//        faceDetector.detectMultiScale(aInputFrame, faceDetections);
//        for(Rect rect : faceDetections.toArray()){
//            Imgproc.rectangle(aInputFrame, new Point(rect.x,rect.y), new Point(rect.x+ rect.width, rect.y+rect.height), new Scalar(0,255,0));
//        }
//
//        return aInputFrame;

    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        // TODO Auto-generated method stub
        mRgba = inputFrame.rgba();
        // Rotate mRgba 90 degrees
        Core.transpose(mRgba, mRgbaT);
        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0,0, 0);
        Core.flip(mRgbaF, mRgba, 1 );

        return recognize(mRgba);

    }

    private Mat findLargestRectangle(Mat inputImg) {


        Mat gray = new Mat();
        Mat thresh = new Mat();

        Imgproc.cvtColor(inputImg, gray, Imgproc.COLOR_BGR2GRAY);


        Imgproc.threshold(gray,thresh,0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);

        Mat temp = thresh.clone();

        Mat hiearchy = new Mat();

        Mat corners = new Mat(4,1, CvType.CV_32FC2);


//        Imgproc.GaussianBlur(inputImg,inputImg,new Size(5,5),5);

        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(temp, contours, hiearchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        hiearchy.release();

        for(MatOfPoint contour : contours){

            MatOfPoint2f contourPoints = new MatOfPoint2f(contour.toArray());
            RotatedRect minRect = Imgproc.minAreaRect(contourPoints);
            Point[] rectPoints = new Point[4];
            minRect.points(rectPoints);

            if(minRect.size.height > inputImg.width()/2){

                List<Point> srcPoints = new ArrayList<>(4);

                srcPoints.addAll(Arrays.asList(rectPoints));

                corners = Converters.vector_Point_to_Mat(srcPoints, CvType.CV_32F);
            }


        }

        Imgproc.erode(thresh, thresh, new Mat(), new Point(-1,-1), 10);
        Imgproc.dilate(thresh, thresh, new Mat(), new Point(-1,-1), 5);
        Mat results = new Mat(1000,250,CvType.CV_8UC3);
        Mat quad = new Mat(1000,250,CvType.CV_8UC1);

        List<Point> dstPoints = new ArrayList<>();
        dstPoints.add(new Point(0,0));
        dstPoints.add(new Point(1000,0));
        dstPoints.add(new Point(1000,250));
        dstPoints.add(new Point(0,250));

        Mat quadPoints = Converters.vector_Point_to_Mat(dstPoints, CvType.CV_32F);

        Mat transmtx = Imgproc.getPerspectiveTransform(corners, quadPoints);
        Imgproc.warpPerspective(inputImg, results, transmtx, new Size(1000,250));
        Imgproc.warpPerspective(thresh, quad, transmtx, new Size(1000,250));

        Imgproc.resize(quad,quad,new Size(20,5));

        return quad;

    }
}