package com.example.projettech;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;

import android.graphics.Camera;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import org.opencv.core.MatOfRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.lang.reflect.Method;

public class OpenCvCamera extends Activity implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final String TAG = "MainActivity";
    Mat mRGBA;
    Mat mRGBAT;
    //CameraBridgeViewBase cameraBridgeViewBase;
    int activeCamera = CameraBridgeViewBase.CAMERA_ID_FRONT;
    JavaCameraView cameraBridgeViewBase;
    private CascadeClassifier haarCascade;

    BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface.SUCCESS:{
                    Log.i(TAG, "onManagerConnected: Opencv loaded");
                    cameraBridgeViewBase.enableView();
                }
                default:{
                    super.onManagerConnected(status);
                }
                break;
            }

        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCompat.requestPermissions(OpenCvCamera.this, new String[]{Manifest.permission.CAMERA},1);
        setContentView(R.layout.activity_opencv_camera);
        cameraBridgeViewBase =findViewById(R.id.camera_surface);
        cameraBridgeViewBase.setCameraIndex(activeCamera);
        cameraBridgeViewBase.setVisibility(CameraBridgeViewBase.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        if request is denied, this will return an empty array
        switch(requestCode){
            case 1:{
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    cameraBridgeViewBase.setCameraPermissionGranted();
                }
                else{
                    // permission denied
                    Log.i("permission", "CAMERA denied");
                }
                return;
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(OpenCVLoader.initDebug()){
//            if load success
            Log.d(TAG, "onResume: Opencv initialized");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else{
            Log.d(TAG, "onResume: Opencv not initialized");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, baseLoaderCallback);
        }

    }
    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {

    }

    @Override
    protected void onPause() {
        super.onPause();
        if(cameraBridgeViewBase !=null){
            cameraBridgeViewBase.disableView();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(cameraBridgeViewBase !=null){
            cameraBridgeViewBase.disableView();
        }
    }

    @Override
    public void onCameraViewStopped() {
        mRGBA.release();

    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        mRGBAT = new Mat();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRGBA = inputFrame.rgba();
        Core.flip(mRGBA, mRGBAT, 1);
        // releasing what's not anymore needed
        mRGBA.release();
        return mRGBAT;
    }

}

