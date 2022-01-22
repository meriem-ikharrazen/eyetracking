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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG_OPENCV = "OpenCv";
    private static final String TAG = "MainActivity";
    CameraBridgeViewBase javaCameraView;
    Mat mRGBA;
    Mat mGray;
    private int absoluteFaceSize;

    private CascadeClassifier cascadeClassifier;
    File mCascadeFile;
    CascadeClassifier mJavaDetector;

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

            // Load the cascade classifier
            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            Log.i(TAG_OPENCV, "Loaded cascade classifier from " +
                    mCascadeFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG_OPENCV, "Error loading cascade", e);
        }
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRGBA = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat();

        absoluteFaceSize = (int) (height * 0.2);

    }


    @Override
    public void onCameraViewStopped() {
        mRGBA.release();
        mGray.release();
    }

        @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

            mRGBA = inputFrame.rgba();
            mGray = inputFrame.gray();

            //Core.flip(mRGBA, mGray, 1);
            // releasing what's not anymore needed

            // Create a grayscale image

            MatOfRect faces = new MatOfRect();

            // Use the classifier to detect faces
            if (cascadeClassifier != null) {
                cascadeClassifier.detectMultiScale(mGray, faces, 1.1, 2, 2,
                        new Size(absoluteFaceSize, absoluteFaceSize), new Size());
            }

            // If there are any faces found, draw a rectangle around it
            Rect[] facesArray = faces.toArray();
            for (int i = 0; i <facesArray.length; i++) {
                Imgproc.rectangle(mRGBA, facesArray[i].tl(), facesArray[i].br(), new Scalar(255, 0, 0), 5);
                Log.i(TAG_OPENCV, "draw");
            }
            mGray.release();
            return mRGBA;

        /*mRGBA = inputFrame.rgba();
        Core.flip(mRGBA, mGray, 1);
        // releasing what's not anymore needed
        mRGBA.release();
        return mGray;*/

           /*mRGBA = inputFrame.rgba();
            Mat mRgbaT = mRGBA.t();
            Core.flip(mRGBA.t(), mRgbaT, -1);
            Imgproc.resize(mRgbaT, mRgbaT, mRGBA.size());
            return mRgbaT;*/

    }

}
