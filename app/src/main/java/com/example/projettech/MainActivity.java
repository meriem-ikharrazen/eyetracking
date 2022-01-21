package com.example.projettech;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
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
import org.opencv.imgproc.Imgproc;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static String TAG = "MainActivity";
    JavaCameraView javaCameraView;
    Mat mRGBA;
    Mat mGray;
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

        javaCameraView =  findViewById(R.id.camera_frame);
        javaCameraView.setCameraPermissionGranted();
        javaCameraView.setCameraIndex(1);
        javaCameraView.setVisibility(SurfaceView.VISIBLE);
        javaCameraView.enableView();
        javaCameraView.setCvCameraViewListener(MainActivity.this);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRGBA = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat();
    }


    @Override
    public void onCameraViewStopped() {
        mRGBA.release();
        mGray.release();
    }

        @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        /*mRGBA = inputFrame.rgba();
        Core.flip(mRGBA, mGray, 1);
        // releasing what's not anymore needed
        mRGBA.release();
        return mGray;*/

           mRGBA = inputFrame.rgba();
            Mat mRgbaT = mRGBA.t();
            Core.flip(mRGBA.t(), mRgbaT, -1);
            Imgproc.resize(mRgbaT, mRgbaT, mRGBA.size());
            return mRgbaT;

    }

}
