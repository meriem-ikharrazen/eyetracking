package com.example.projettech;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.Objdetect;


import static org.opencv.imgproc.Imgproc.TM_SQDIFF;
import static org.opencv.imgproc.Imgproc.TM_SQDIFF_NORMED;


import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG_OPENCV = "OpenCv";
    private static final String TAG = "MainActivity";

    // Variables Match


    private static final int TM_SQDIFF = 0;
    private static final int TM_SQDIFF_NORMED = 1;
    private static final int TM_CCOEFF = 2;
    private static final int TM_CCOEFF_NORMED = 3;
    private static final int TM_CCORR = 4;
    private static final int TM_CCORR_NORMED = 5;

    CameraBridgeViewBase javaCameraView;
    Mat mRGBA;
    Mat mGray;
    Mat mRgbaF ;
    Mat mRgbaT ;
    Mat mGrayF ;
    Mat mGrayT ;


    private int absoluteFaceSize;

    private CascadeClassifier cascadeClassifier;
    private CascadeClassifier mJavaDetectorEye;

    File mCascadeFileEye;
    File mCascadeFile;



    static
   {
     if(OpenCVLoader.initDebug())
      {
           Log.d(TAG, "Opencv installed successfully");
       }
     else{
          Log.d(TAG, "opencv not installed");
      }
   }
    private Button open_camera;
    //private android.R.attr r;
    private int learn_frames=0;
    private Mat teplateR;
    private Mat teplateL;

    int method = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        javaCameraView = (CameraBridgeViewBase) findViewById(R.id.camera_frame);
        javaCameraView.setCameraPermissionGranted();
        javaCameraView.setCameraIndex(1);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.enableView();
        javaCameraView.setCvCameraViewListener(MainActivity.this);



        // Train the algorithm
        try {
            // Copy the resource into a temp file so OpenCV can load it
            InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);
            //Classifieur face
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            File mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            

            // load cascade file from application resources
            InputStream ise = getResources().openRawResource(R.raw.haarcascade_lefteye_2splits);
            File cascadeDirEye = getDir("cascade", Context.MODE_PRIVATE);
            mCascadeFileEye = new File(cascadeDirEye, "haarcascade_lefteye_2splits.xml");
            FileOutputStream ose = new FileOutputStream(mCascadeFileEye);

            while ((bytesRead = ise.read(buffer)) != -1) {
                ose.write(buffer, 0, bytesRead);
            }
            ise.close();
            ose.close();


            // Load the cascade classifier
            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            Log.i(TAG_OPENCV, "Loaded cascade classifier from " +
                    mCascadeFile.getAbsolutePath());

            // Load the cascade classifier EYE
            mJavaDetectorEye = new CascadeClassifier(mCascadeFileEye.getAbsolutePath());
            Log.i(TAG_OPENCV, "Loaded cascade classifier from " +
                    mCascadeFileEye.getAbsolutePath());

          /*
            mJavaDetectorEye.load( mCascadeFile.getAbsolutePath() );

            if (mJavaDetectorEye.empty()) {
                Log.e(TAG, "Failed to load cascade classifier for eye");
                mJavaDetectorEye = null;
            } else
                Log.i(TAG, "Loaded cascade classifier from " + mCascadeFileEye.getAbsolutePath());
*/



        } catch (Exception e) {
            Log.e(TAG_OPENCV, "Error loading cascade", e);
        }


// cam
 //       javaCameraView.enableFpsMeter();
 //       javaCameraView.setCameraIndex(1);
  //      javaCameraView.enableView();


    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRGBA = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat();
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);
        mGrayF = new Mat(height, width, CvType.CV_8UC4);
        mGrayT = new Mat(width, width, CvType.CV_8UC4);

        absoluteFaceSize = (int) (height * 0.2);

    }

    @Override
    public void onCameraViewStopped() {
        mRGBA.release();
        mGray.release();
    }

        @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

            mRGBA = inputFrame.rgba();//convert the frame to rgba scale, then assign this value to the rgba Mat img matrix.
            mGray = inputFrame.gray();//convert the frame to gray scale, then assign this value to the gray Mat img matrix.

            //Matrice transposee
            Mat mRgbaT = mRGBA.t();



            // Create a grayscale image

            MatOfRect faces = new MatOfRect();//a matrix that will contain rectangles around the face (including the faces inside the rectangles), it will be filled by detectMultiScale method.

            // Use the classifier to detect faces
            if (cascadeClassifier != null) {
                cascadeClassifier.detectMultiScale(mGray, faces, 1.1, 2, 2,
                        new Size(absoluteFaceSize, absoluteFaceSize), new Size());
            }

            // If there are any faces found, draw a rectangle around it
            Rect[] facesArray = faces.toArray();
            for (int i = 0; i <facesArray.length; i++) {
                Imgproc.rectangle(mRGBA, facesArray[i].tl(), facesArray[i].br(), new Scalar(255, 0, 0), 5);
                Log.i(TAG_OPENCV, " FACE draw");

                /// elimination de plusieurs frames



                //  In each face, detect eyes
                Rect r = facesArray[i];
                // compute the eye area

                Rect eyearea = new Rect(r.x + r.width / 8,
                        (int) (r.y + (r.height / 4.5)), r.width - 2 * r.width / 8,
                        (int) (r.height / 3.0));
                // split it
                Rect eyearea_right = new Rect(r.x + r.width / 16,
                        (int) (r.y + (r.height / 4.5)),
                        (r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));
                Rect eyearea_left = new Rect(r.x + r.width / 16
                        + (r.width - 2 * r.width / 16) / 2,
                        (int) (r.y + (r.height / 4.5)),
                        (r.width - 2 * r.width / 16) / 2, (int) (r.height / 3.0));
                // draw the area - mGray is working grayscale mat, if you want to
                // see area in rgb preview, change mGray to mRgba
                Imgproc.rectangle(mRGBA, eyearea_left.tl(), eyearea_left.br(),
                        new Scalar(255, 0, 0, 255), 2);
                Imgproc.rectangle(mRGBA, eyearea_right.tl(), eyearea_right.br(),
                        new Scalar(255, 0, 0, 255), 2);
                Log.i(TAG_OPENCV, "EYES draw");

                // we will take 5 input frames

             //   if (learn_frames < 5) {
                // no learned frames -> Learn templates from at least 5 frames..
                   // Log.i(TAG_OPENCV, "Pupill draw");
                    teplateR = get_template(mJavaDetectorEye, eyearea_right, 24);
                    teplateL = get_template(mJavaDetectorEye, eyearea_left, 24);

                    learn_frames++;
              //  }else{
                // Learning finished, use the new templates for template matching

                    match_eye(eyearea_right, teplateR, method);
                    match_eye(eyearea_left, teplateL, method);
             //   }



                Core.transpose(mRGBA, mRgbaT);
                Imgproc.resize(mRgbaT, mRgbaF, mRGBA.size(), 0, 0, 0);
                Core.flip(mRgbaF, mRGBA, -1);
                Core.transpose(mGray, mGrayT);
                Imgproc.resize(mGrayT, mGrayF, mGray.size(), 0, 0, 0);
                Core.flip(mGrayF, mGray, -1);



            }

           mGray.release();
            return mRGBA;



        /*mRGBA = inputFrame.rgba();
      //  Core.flip(mRGBA, mGray, 1);
        // releasing what's not anymore needed
        mRGBA.release();
        return mGray;*/

           /*mRGBA = inputFrame.rgba();
            Mat mRgbaT = mRGBA.t();
           // Core.flip(mRGBA.t(), mRgbaT, -1);
            Imgproc.resize(mRgbaT, mRgbaT, mRGBA.size());
            return mRgbaT;*/

    }




    // an algorithm to detect circle object from each of the images

    private void match_eye(Rect area, Mat mTemplate, int type) {

        Point matchLoc;
        Mat mROI = mGray.submat(area);
        int result_cols = mROI.cols() - mTemplate.cols() + 1;
        int result_rows = mROI.rows() - mTemplate.rows() + 1;
        // Check for bad template size


        if (mTemplate.cols() == 0 || mTemplate.rows() == 0) {
            return ;
        }

        Mat mResult = new Mat(result_cols,result_rows,CvType.CV_8UC4);
     //   Log.i(TAG_OPENCV, "Pupill draw");

        switch (type) {
            case TM_SQDIFF:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_SQDIFF);
                break;
            case TM_SQDIFF_NORMED:
                Imgproc.matchTemplate(mROI, mTemplate, mResult,
                        Imgproc.TM_SQDIFF_NORMED);
                break;
            case TM_CCOEFF:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCOEFF);
                break;
            case TM_CCOEFF_NORMED:
                Imgproc.matchTemplate(mROI, mTemplate, mResult,
                        Imgproc.TM_CCOEFF_NORMED);
                break;
            case TM_CCORR:
                Imgproc.matchTemplate(mROI, mTemplate, mResult, Imgproc.TM_CCORR);
                break;
            case TM_CCORR_NORMED:
                Imgproc.matchTemplate(mROI, mTemplate, mResult,
                        Imgproc.TM_CCORR_NORMED);
                break;
        }

        Core.MinMaxLocResult mmres = Core.minMaxLoc(mResult);
        // there is difference in matching methods - best match is max/min value
        if (type == TM_SQDIFF || type == TM_SQDIFF_NORMED) {
            matchLoc = mmres.minLoc;
        } else {
            matchLoc = mmres.maxLoc;
        }

        Point matchLoc_tx = new Point(matchLoc.x + area.x, matchLoc.y + area.y);
        Point matchLoc_ty = new Point(matchLoc.x + mTemplate.cols() + area.x,
                matchLoc.y + mTemplate.rows() + area.y);
        Imgproc.rectangle(mRGBA, matchLoc_tx, matchLoc_ty, new Scalar(255, 255, 0,
                255));

        Rect rec = new Rect(matchLoc_tx,matchLoc_ty);
    }

    //finally constructing the detection eye-pupill based on some decisions.

    private Mat get_template(CascadeClassifier clasificator, Rect area, int size) {


        Mat template = new Mat(); //prepare a Mat which will serve as a template for eyes.
        Mat mROI = mGray.submat(area); //detect only region of interest which is represented by the area. So, from the total Mat get only the submat that represent roi.

        MatOfRect eyes = new MatOfRect(); //will be around eyes (including eyes), this will be filled by detectMultiScale
        Point iris = new Point(); //to identify iris.

        Rect eye_template = new Rect();


        clasificator.detectMultiScale(mROI, eyes, 1.15, 2,
                Objdetect.CASCADE_FIND_BIGGEST_OBJECT
                        | Objdetect.CASCADE_SCALE_IMAGE, new Size(30, 30),
                new Size());

        Rect[] eyesArray = eyes.toArray(); //get the detected eyes

        for (int i = 0; i < eyesArray.length;) {
            Log.i(TAG_OPENCV, "Problem !!!!!!Pupill draw");
            Rect e = eyesArray[i];
            e.x = area.x + e.x; //the starting x coordinates of the rect (area) around the eye + the area
            e.y = area.y + e.y;
            Rect eye_only_rectangle = new Rect((int) e.tl().x,
                    (int) (e.tl().y + e.height * 0.4), (int) e.width,
                    (int) (e.height * 0.6));
            mROI = mGray.submat(eye_only_rectangle);
            Mat vyrez = mRGBA.submat(eye_only_rectangle);


            Core.MinMaxLocResult mmG = Core.minMaxLoc(mROI);

            Imgproc.circle(vyrez, mmG.minLoc, 2, new Scalar(255, 255, 255, 255), 2);
            iris.x = mmG.minLoc.x + eye_only_rectangle.x;
            iris.y = mmG.minLoc.y + eye_only_rectangle.y;
            eye_template = new Rect((int) iris.x - size / 2, (int) iris.y
                    - size / 2, size, size);

            Imgproc.rectangle(mRGBA, eye_template.tl(), eye_template.br(),
                    new Scalar(255, 0, 0, 255), 2);
            template = (mGray.submat(eye_template)).clone();
            return template;
        }
        return template;
    }

    public void onRecreateClick(View v)
    {
        learn_frames = 0;
    }

}
