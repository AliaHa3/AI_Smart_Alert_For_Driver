package com.example.alia.testopencv;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.HOGDescriptor;
import org.opencv.video.KalmanFilter;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;

public class FdActivity extends Activity  implements CvCameraViewListener2 {

    private static final String    TAG                 = "OCVSample::Activity";
    private static final Scalar   OBJECT_RECT_COLOR     = new Scalar(0, 255, 0, 255);
    private static final Scalar   ALERT_RECT_COLOR     = new Scalar(255, 0, 0, 255);

    public static final int        JAVA_DETECTOR       = 0;

    private MenuItem               mItemCar50;
    private MenuItem               mItemCar40;
    private MenuItem               mItemCar30;
    private MenuItem               mItemCar20;
    private MenuItem               mItemType;

    private Mat                    mRgba;
    private Mat                    mGray;
    private Mat                    intermediatemGray;

    private File                   mCascadeFile;
    private CascadeClassifier      mJavaDetector,mJavaDetector2;

    private int                    mDetectorType       = JAVA_DETECTOR;
    private String[]               mDetectorName;

    private float                  mRelativeCarSize   = 0.2f;
    private int                    mAbsoluteCarSize   = 0;

    private int                    firstFlag   = 0;
    public static int image_scale = 2;
    private CameraBridgeViewBase   mOpenCvCameraView;

    HOGDescriptor hog;
    MatOfRect faces;
    MatOfDouble weights;





    private Point p1,p2;


    private BaseLoaderCallback  mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");


                    try {

                        hog = new HOGDescriptor();
                        hog.setSVMDetector(HOGDescriptor.getDefaultPeopleDetector());
                        faces = new MatOfRect();
                        weights = new MatOfDouble();


                        // load cascade file from application resources
                        InputStream is = getResources().openRawResource(R.raw.cars);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "haarcascade_car.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

                        // load cascade file from application resources
                        is = getResources().openRawResource(R.raw.haarcascade_car_1);
                        cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "haarcascade_car1.xml");
                        os = new FileOutputStream(mCascadeFile);

                        buffer = new byte[4096];
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector2 = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector2.empty()) {
                            Log.e(TAG, "Failed to load cascade classifier");
                            mJavaDetector2 = null;
                        } else
                            Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());



                        cascadeDir.delete();

                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);

                    }



                    p1 = new Point();
                    p2 = new Point();


                    //  mOpenCvCameraView.setMaxFrameSize(800,600);
                    mOpenCvCameraView.enableFpsMeter();
                    mOpenCvCameraView.enableView();

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    public FdActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";

        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.detect_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setVisibility(CameraBridgeViewBase.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume()
    {
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
        mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        Log.d(TAG, "mgray before" + mGray.width() + " " + mGray.height());
        Imgproc.resize(mGray,mGray,new Size((Math.round(mGray.width()/image_scale)),Math.round(mGray.height()/image_scale)));
        Log.d(TAG, "mgray after" + mGray.width() + " " + mGray.height());
        Imgproc.equalizeHist(mGray,mGray);

        // detect Car
        mRgba = detectObject(mGray,mRgba);



        return mRgba;
    }



    public Mat detectObject(Mat input,Mat output){
        MatOfRect cars = new MatOfRect();

        // detect car
        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null)
                mJavaDetector.detectMultiScale(input, cars, 1.05, 3, 2,
                        new Size(50, 50), new Size(input.cols(),input.rows()));


        }

        // detect pedstrain here with mgray image
        //hog.detectMultiScale(input, faces, weights,0,new Size(),new Size(),1.2,2,false);
        hog.detectMultiScale(input, faces, weights);
        //Draw faces on the image
         Rect[] facesArray = faces.toArray();

        Rect[] carsArray = cars.toArray();

        if (mJavaDetector2 != null)
            mJavaDetector2.detectMultiScale(input, cars, 1.2, 3, 2, // TODO: objdetect.CV_HAAR_SCALE_IMAGE
                    new Size(50, 50), new Size(input.cols(),input.rows()));

        Log.d(TAG, "cars.size = " + carsArray.length);



        for (int i = 0; i < carsArray.length; i++){
            if ((carsArray[i].tl().y > mGray.height() * 0.3) ) {
                p1.x = (int) (carsArray[i].tl().x * image_scale);
                p1.y = (int) (carsArray[i].tl().y * image_scale);
                p2.x = (int) (carsArray[i].br().x * image_scale) + 15;
                p2.y = (int) (carsArray[i].br().y * image_scale) + 15;
                float f = (170 * 360) / carsArray[i].width;
                f = f/100f;

                if (p1.x + 30 < output.width()) {
                    Imgproc.putText(output, "" +f, p1, 3, 1, new Scalar(255, 0, 0, 255), 2);
                } else if (p2.x + 30 < output.width()) {
                    Imgproc.putText(output, "" +f, p2, 3, 1, new Scalar(255, 0, 0, 255), 2);
                } else if (p1.y - 30 > 0) {
                    p1.x += 50;
                    Imgproc.putText(output, "" + f, p1, 3, 1, new Scalar(255, 0, 0, 255), 2);
                } else if (p2.y - 30 > 0) {
                    p2.x += 50;
                    Imgproc.putText(output, "" + f, p2, 3, 1, new Scalar(255, 0, 0, 255), 2);
                }

                Imgproc.rectangle(output, p1, p2, OBJECT_RECT_COLOR, 3);
            }
        }
        carsArray = cars.toArray();
        for (int i = 0; i < carsArray.length; i++){
            if ((carsArray[i].tl().y > mGray.height() * 0.3) ) {
                p1.x = (int) (carsArray[i].tl().x * image_scale);
                p1.y = (int) (carsArray[i].tl().y * image_scale);
                p2.x = (int) (carsArray[i].br().x * image_scale+15);
                p2.y = (int) (carsArray[i].br().y * image_scale+15);
                float f = (170 * 360) / carsArray[i].width;
                f = f/100f;
                if (p1.x + 30 < output.width()) {
                    Imgproc.putText(output, "" +f, p1, 3, 1, new Scalar(255, 0, 0, 255), 2);
                } else if (p2.x + 30 < output.width()) {
                    Imgproc.putText(output, "" +f, p2, 3, 1, new Scalar(255, 0, 0, 255), 2);
                } else if (p1.y - 30 > 0) {
                    p1.x += 50;
                    Imgproc.putText(output, "" + f, p1, 3, 1, new Scalar(255, 0, 0, 255), 2);
                } else if (p2.y - 30 > 0) {
                    p2.x += 50;
                    Imgproc.putText(output, "" + f, p2, 3, 1, new Scalar(255, 0, 0, 255), 2);
                }

                Imgproc.rectangle(output, p1, p2, OBJECT_RECT_COLOR, 3);
            }
        }


        for (int i = 0; i < facesArray.length; i++) {
            p1.x = (int) (facesArray[i].tl().x * image_scale);
            p1.y = (int) (facesArray[i].tl().y * image_scale);
            p2.x = (int) (facesArray[i].br().x * image_scale);
            p2.y = (int) (facesArray[i].br().y * image_scale);
            float f = (50 * 360) / facesArray[i].width;
            f = f/100f;

            if (p1.x + 30 < output.width()) {
                Imgproc.putText(output, "" + f, p1, 3, 1, new Scalar(255, 0, 0, 255), 2);
            } else if (p2.x + 30 < output.width()) {
                Imgproc.putText(output, "" + f, p2, 3, 1, new Scalar(255, 0, 0, 255), 2);
            } else if (p1.y - 30 > 0) {
                p1.x += 50;
                Imgproc.putText(output, "" + f, p1, 3, 1, new Scalar(255, 0, 0, 255), 2);
            } else if (p2.y - 30 > 0) {
                p2.x += 50;
                Imgproc.putText(output, "" + f, p2, 3, 1, new Scalar(255, 0, 0, 255), 2);
            }

            Imgproc.rectangle(output, p1, p2,OBJECT_RECT_COLOR , 3);

        }






        return output;

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        Log.i(TAG, "called onCreateOptionsMenu");
        mItemCar50 = menu.add("Car size 50%");
        mItemCar40 = menu.add("Car size 40%");
        mItemCar30 = menu.add("Car size 30%");
        mItemCar20 = menu.add("Car size 20%");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.i(TAG, "called onOptionsItemSelected; selected item: " + item);
        if (item == mItemCar50)
            setMinFaceSize(0.5f);
        else if (item == mItemCar40)
            setMinFaceSize(0.4f);
        else if (item == mItemCar30)
            setMinFaceSize(0.3f);
        else if (item == mItemCar20)
            setMinFaceSize(0.2f);

        return true;
    }

    private void setMinFaceSize(float faceSize) {
        mRelativeCarSize = 0;
        mAbsoluteCarSize = 0;
    }


}
