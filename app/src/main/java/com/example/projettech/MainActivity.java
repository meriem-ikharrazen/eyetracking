////Amelioration: Point d'améliorations a ajoutée.
/// Amelioration: elimination de plusieurs frames

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
import android.widget.GridView;
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


import static org.opencv.imgproc.Imgproc.CV_BLUR;
import static org.opencv.imgproc.Imgproc.TM_SQDIFF;
import static org.opencv.imgproc.Imgproc.TM_SQDIFF_NORMED;


import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG_OPENCV = "OpenCv";
    private static final String TAG = "MainActivity";

    CameraBridgeViewBase javaCameraView;
    Mat mRGBA;
    Mat mGray;
    Mat mRgbaF;
    Mat mRgbaT;
    Mat mGrayF;
    Mat mGrayT;

    private CascadeClassifier FaceCascadeClassifier;
    private CascadeClassifier EyeCascadeClassifier;

    File mCascadeFileEye;

    static {
        if (OpenCVLoader.initDebug()) {
            Log.d(TAG, "Opencv installed successfully");
        } else {
            Log.d(TAG, "opencv not installed");
        }
    }

    private Point irisRight;
    private Point irisLeft;

    private Rect eyearea_right;
    private Rect eyearea_left;

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
            InputStream is = getResources().openRawResource(R.raw.haarcascade_frontalface_alt2);

            // Classifier
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
            FaceCascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            Log.i(TAG_OPENCV, "Loaded cascade classifier from " +
                    mCascadeFile.getAbsolutePath());

            // Load the cascade classifier EYE
            EyeCascadeClassifier = new CascadeClassifier(mCascadeFileEye.getAbsolutePath());
            Log.i(TAG_OPENCV, "Loaded cascade classifier from " +
                    mCascadeFileEye.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG_OPENCV, "Error loading cascade", e);
        }

    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        /***** initialization of matrixs for RGB ans GRAY images ****/
        //Matrix objects initiation
        mRGBA = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC4);
        // The following variables will be needed to resize
        // the camera frame (make it portrait mode)
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);
        mGrayF = new Mat(height, width, CvType.CV_8UC4);
        mGrayT = new Mat(width, width, CvType.CV_8UC4);
    }

    @Override
    public void onCameraViewStopped() {
        mRGBA.release();
        mGray.release();
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        //convert the frame to rgba scale, then assign this value to the rgba Mat img matrix.
        mRGBA = inputFrame.rgba();
        //convert the frame to gray scale, then assign this value to the gray Mat img matrix.
        mGray = inputFrame.gray();

        /*************START: Portrait mode****************/

        // Portrait mode
        // Transpose the RGB matrix
        Core.transpose(mRGBA, mRgbaT);
        // Resize the matrix to the original size
        Imgproc.resize(mRgbaT, mRgbaF, mRGBA.size());
        // Flip the image by 90°
        Core.flip(mRgbaF, mRGBA, -1);

        // Same for Gray matrix
        Core.transpose(mGray, mGrayT);
        Imgproc.resize(mGrayT, mGrayF, mGray.size());
        Core.flip(mGrayF, mGray, -1);

        /*************END: Portrait mode****************/

        /*************START: Face detection****************/
        // Create a grayscale image
        // MatOfRect: a matrix that will contain rectangles around the face (including the faces inside the rectangles),
        // it will be filled by detectMultiScale method.
        MatOfRect faces = new MatOfRect();

        // Use the classifier to detect faces
        if (FaceCascadeClassifier != null) {
            FaceCascadeClassifier.detectMultiScale(mGray, faces, 1.1, 2, 2,
                    new Size(30, 30), new Size());
        }
        // If there are any faces found, draw a rectangle around it
        Rect[] facesArray = faces.toArray();
        for (int i = 0; i < facesArray.length; i++) {
            Imgproc.rectangle(mRGBA, facesArray[i].tl(), facesArray[i].br(), new Scalar(0, 255, 0), 3);

            /*************END: Face detection****************/

            /*************Start: Eyes detection****************/
            //  In each face, detect eyes
            Rect r = facesArray[i];
            eyearea_right = new Rect(r.x + r.width / 16,
                    (int) (r.y + (r.height / 4)),
                    (r.width - 2 * r.width / 16) / 2, (int) (r.height / 4.0));
            eyearea_left = new Rect(r.x + r.width / 16
                    + (r.width - 2 * r.width / 16) / 2,
                    (int) (r.y + (r.height / 4)),
                    (r.width - 2 * r.width / 16) / 2, (int) (r.height / 4.0));

            Imgproc.rectangle(mRGBA, eyearea_left.tl(), eyearea_left.br(),
                    new Scalar(255, 0, 0, 255), 2);
            Imgproc.rectangle(mRGBA, eyearea_right.tl(), eyearea_right.br(),
                    new Scalar(255, 0, 0, 255), 2);
            /////Log.i(TAG_OPENCV, "EYES draw");
            /*************End: Eyes detection****************/

            /*************START: iris(pupilles) detection****************/

            irisRight = get_iris(EyeCascadeClassifier, eyearea_right, 24);
            irisLeft = get_iris(EyeCascadeClassifier, eyearea_left, 24);

            /*************END: iris(pupilles) detection****************/

        }

        /*************START: TEST Harris ****************/
        /*
        if(eyearea_right !=  null && eyearea_left !=  null){
            Rect e = new Rect(eyearea_right.x + 5,
                    eyearea_right.y+5,
                    eyearea_right.width+eyearea_left.width-5, eyearea_right.height-5);
            mGray = new Mat(mGray,e);
            Imgproc.equalizeHist(mGray,mGray);

            dst = Mat.zeros(mGray.size(), CvType.CV_32F);
            int blockSize = 2;
            int apertureSize = 3;
            double k = 0.04;
            Imgproc.cornerHarris(mGray, dst, blockSize, apertureSize, k);
            Core.normalize(dst, dstNorm, 0, 255, Core.NORM_MINMAX);
            Core.convertScaleAbs(dstNorm, dstNormScaled);
            float[] dstNormData = new float[(int) (dstNorm.total() * dstNorm.channels())];
            dstNorm.get(0, 0, dstNormData);
            for (int ii = 0; ii < dstNorm.rows(); ii++) {
                for (int j = 0; j < dstNorm.cols(); j++) {
                    if ((int) dstNormData[ii * dstNorm.cols() + j] > threshold) {
                        Log.e("EyePixelVector", "right");
                        Imgproc.circle(mRGBA, new Point(j+eyearea_right.x, ii+eyearea_right.y), 3, new Scalar(0, 128, 255, 1.0), 1, 8, 0);
                    }
                }
            }
        }
        /*************START: TEST Harris ****************/

        mGray.release();
        return mRGBA;
    }

    private Point get_iris(CascadeClassifier clasificator, Rect area, int size) {

        Rect eye_template = new Rect();
        // Submat: extract the matrix area from the grey image and stock it in mROI (matrix Region of interest)
        Mat mROI = mGray.submat(area);
        //will be around eyes (including eyes), this will be filled by detectMultiScale
        MatOfRect eyes = new MatOfRect();
        //to identify iris.
        Point iris = new Point();

        /************* START: Eyes detection using the cascade classifier ****************/
        clasificator.detectMultiScale(mROI, eyes, 1.1, 2, 2, new Size(30, 30),
                new Size());
        /************* END: Eyes detection using the cascade classifier ****************/

        // Get an array for all the detected eyes
        Rect[] eyesArray = eyes.toArray();

        /************* START: Draw eye area and iris ****************/
        for (int i = 0; i < eyesArray.length; ) {
            Rect e = eyesArray[i];
            //the starting x coordinates of the rect (eye area) + the area
            e.x = area.x + e.x;
            e.y = area.y + e.y;
            Rect eye_only_rectangle = new Rect((int) e.tl().x,
                    (int) (e.tl().y + e.height * 0.4), (int) e.width,
                    (int) (e.height * 0.6));
            mROI = mGray.submat(eye_only_rectangle);
            Mat eye = mRGBA.submat(eye_only_rectangle);

            // minMaxLoc: Return the max and the min intensity in the image
            Core.MinMaxLocResult intensity = Core.minMaxLoc(mROI);
            
            // Because the ires area is Black so it's intensity is LOW (1=White, 0= Black)
            // That's why we worked by minLoc
            Imgproc.circle(eye, intensity.minLoc, 2, new Scalar(255, 255, 255, 255), 2);
            iris.x = intensity.minLoc.x + eye_only_rectangle.x;
            iris.y = intensity.minLoc.y + eye_only_rectangle.y;
            eye_template = new Rect((int) iris.x - size / 2, (int) iris.y
                    - size / 2, size, size);

            Imgproc.rectangle(mRGBA, eye_template.tl(), eye_template.br(),
                    new Scalar(255, 0, 0, 255), 2);

            return iris;
        }
        /************* END: Draw eye area and iris ****************/
        return iris;
    }

}
