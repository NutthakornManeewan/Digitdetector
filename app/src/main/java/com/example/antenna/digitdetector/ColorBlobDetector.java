package com.example.antenna.digitdetector;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;

import com.google.android.gms.appdatasearch.Feature;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.HOGDescriptor;

public class ColorBlobDetector {

    private static final String TAG  = "Result";
    private static final String TAG3 = "Result";

    public double NUMBER_RESULT  = 0;
    public static List<float[]> desList1 = new ArrayList<float[]>();
    public ArrayList<Rect> RECTS = new ArrayList<Rect>();
    public double widthMat=0.0, heightMat=0.0;

    // ----- Cache -----
    Mat mGray                = new Mat();
    Mat mSharp            = new Mat();
    Mat mBlur                = new Mat();
    Mat mThreshold           = new Mat();
    Mat mReducedNoise        = new Mat();
    Mat mReducedNoise_t      = new Mat();
    Mat hierarchy            = new Mat();
    MatOfPoint2f ApproxCurve = new MatOfPoint2f();

    // ----- Ellipse Kernel -----
    Mat RectKernel = Imgproc.getStructuringElement (Imgproc.MORPH_RECT, new Size(2, 6));

    public void modelReader() {
        if (desList1.isEmpty()) {
            readModel("Model", desList1);
            Log.e(TAG, "Read model successfully!");
        }
    }

    public Mat process (Mat rgbaImage) {
        RECTS.clear();
        ArrayList<MatOfPoint> mContours = new ArrayList<MatOfPoint>();

        // ***** Preprocess - 01 *****
        Imgproc.cvtColor    (rgbaImage, mGray, Imgproc.COLOR_RGB2GRAY);
        Imgproc.GaussianBlur(mGray, mBlur, new Size(7,7), 0.5, 0.5);
        Core.addWeighted    (mGray, 1.5, mBlur, -0.5, 0, mSharp);
        Imgproc.medianBlur  (mSharp, mBlur, 3);
        Imgproc.GaussianBlur(mBlur, mBlur, new Size(7,7), 0.5, 0.5);

        // ***** Thresholding *****
        Imgproc.threshold   (mBlur, mThreshold, 0, 255, Imgproc.THRESH_OTSU);
        Imgproc.threshold   (mThreshold, mThreshold, 0, 255, Imgproc.THRESH_BINARY_INV);
        Imgproc.GaussianBlur(mThreshold, mThreshold, new Size(7,7), 0.2, 0.2);

        // ***** Preprocess - 02 *****
        widthMat  = Math.floor(mThreshold.size().width * 0.005);
        heightMat = Math.floor(mThreshold.size().height * 0.010);

        Mat RectKernel = Imgproc.getStructuringElement (Imgproc.MORPH_RECT, new Size(widthMat, heightMat));
        Imgproc.morphologyEx(mThreshold, mReducedNoise, Imgproc.MORPH_CLOSE, RectKernel);
        Imgproc.morphologyEx(mThreshold, mReducedNoise_t, Imgproc.MORPH_CLOSE, RectKernel);
        Imgproc.findContours(mReducedNoise, mContours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

        for (int i=0; i < mContours.size(); i++) {
            MatOfPoint2f contour2f = new MatOfPoint2f (mContours.get(i).toArray());
            double ApproxDistance  = Imgproc.arcLength (contour2f, true) * 0.0005 ;
            double orientation     = 0.0;

            if (contour2f.size().height >= 5)
                orientation = Imgproc.fitEllipse(contour2f).angle;

            Imgproc.approxPolyDP   (contour2f, ApproxCurve, ApproxDistance, true); // find line around polygon.
            MatOfPoint points      = new MatOfPoint (ApproxCurve.toArray());
            Rect rect              = Imgproc.boundingRect (points);
            double tmpX = rect.br().x - rect.tl().x;
            double tmpY = rect.br().y - rect.tl().y;
            double ratio_XY = tmpX / tmpY;

            if (rect.br().x <= mThreshold.size().width && rect.br().y <=mThreshold.size().height && rect.tl().x >= 0 && rect.tl().y >= 0) {
                RECTS.add(rect);
                if (rect.area() >= 10000 && rect.area() < 100000) {
                    if (orientation >= 65 && orientation <= 90) {

                    }
                }
            }
        }
        return mReducedNoise_t;
    }

    public void SortElements () {
        Rect tmp     = null;
        boolean flag = true;
        while (flag) {
            flag = false;
            for (int j = 0; j < RECTS.size() - 1; j++) {
                if (RECTS.get(j + 1).tl().x < RECTS.get(j).tl().x) {
                    tmp = RECTS.get(j);
                    RECTS.set(j, RECTS.get(j + 1));
                    RECTS.set(j + 1, tmp);
                    flag = true;
                }
            }
        }

        Log.e(TAG, "NIGHTMARE IS STARTEDDDDDDDDDD !!!!!!!");
        // ---- Feature Extraction -----
        int stringCode[] = new int[RECTS.size()];
        NUMBER_RESULT    = 0;

        for (int i = 0; i < RECTS.size(); i++) {
            if (RECTS.get(i).tl().y - 7 >= 0 && RECTS.get(i).tl().x - 7 >= 0 && RECTS.get(i).br().x+7 <= 1280 && RECTS.get(i).br().y+7 <= 720) {

                Mat mDigit    = mBlur.submat((int) RECTS.get(i).tl().y - 7, (int) RECTS.get(i).br().y + 7, (int) RECTS.get(i).tl().x - 7, (int) RECTS.get(i).br().x + 7);
                int front     = 0;
                double area   = (RECTS.get(i).br().x - RECTS.get(i).tl().x) * (RECTS.get(i).br().y - RECTS.get(i).tl().y);
                double Multip = Math.pow(10, RECTS.size() - 1 - i);

                if (area >= 22000) {
                    Log.e(TAG, "Area greater than 22000");
                    stringCode[i] = classiflyDigit(mDigit, desList1);
                    front         = (int) (stringCode[i] / 10);
                    NUMBER_RESULT = NUMBER_RESULT + (front * Multip);
                } else {
                    stringCode[i] = 1;
                    NUMBER_RESULT = NUMBER_RESULT + (1 * Multip);
                }
            }
        }
         if (RECTS.size() >= 4)
            NUMBER_RESULT = NUMBER_RESULT / 1000;
        Log.i(TAG3, "Number result : " + NUMBER_RESULT);
    }

    public static float[] extractHOG (Mat digit){
        Mat rDigit              = new Mat();
        // -- Default size = (64,128);
        Imgproc.resize          (digit, rDigit, new Size(64, 128));
        final HOGDescriptor hog = new HOGDescriptor();
        MatOfFloat descriptors  = new MatOfFloat();
        MatOfPoint locations    = new MatOfPoint();
        final Size winStride    = new Size(rDigit.height(), rDigit.width());
        final Size padding      = new Size(0, 0);

        Log.e(TAG, "Hello GG");
        // --- Compute 'Histogram of Gradients' for sub image. ---
        hog.compute(rDigit, descriptors, winStride, padding, locations);
        Log.e(TAG, "Here this is a trap! : " + descriptors.width() + " | " + descriptors.height());

        float[] des = new float[descriptors.height()];
        for(int m = 0; m < descriptors.height(); m++) {
            des[m] = (float)descriptors.get(m,0)[0];
        }
        Log.e(TAG, "Hey this is a pass value eiei!");
        return des;
    }

    public static int classiflyDigit(Mat digit, List<float[]> model) {
        float[] mDescriptors1 = extractHOG(digit);
        int index_min         = 0;
        int special_min       = 0;
        double min            = 100000;
        int index_mins[]      = new int[model.size()];    // --- 220 size
        double mins[]         = new double[model.size()]; // --- 220 size
        Log.i(TAG, "Model size : " + model.size());
        for(int i=0 ; i < model.size(); i++) {
            float[] mDescriptors2 = model.get(i);
            double dist           = disHOG( mDescriptors1 ,  mDescriptors2);

            index_mins[i] = i;
            mins[i]       = dist;
            if(dist < min) {
                min       = dist;
                index_min = i;
            }
        }
        KNNClassifier knnClassifier = new KNNClassifier(index_mins, mins);
        Log.i(TAG, "Obj. Created!");
        knnClassifier.SortDistance();
        Log.i(TAG, "Obj. Sorted!");
        special_min = knnClassifier.Candidate();
        Log.i(TAG, "Obj. Candidate!");
        if (special_min != 0)
            return special_min;
        else
            return index_min;
    }

    public static double disHOG(float[] mDescriptors1 , float[] mDescriptors2) {
        double distance=0;
        for (int i=0; i< mDescriptors1.length ;i++) {
            distance += ((mDescriptors1[i]-mDescriptors2[i])*(mDescriptors1[i]-mDescriptors2[i]) );
        }
        return distance;
    }

    public static Boolean readModel (String fname, List<float[]> desList) {
        try {
            File sdcard     = Environment.getExternalStorageDirectory();
            String fileName = fname + ".txt";
            File file       = new File(sdcard, fileName);
            // If file does not exists, then create it
            if (!file.exists())
                Log.i(TAG3, "File is not exists");

            FileReader fileReader = new FileReader(file.getAbsoluteFile());
            BufferedReader br     = new BufferedReader(fileReader);
            String line           = null;
            int count_line        = 0;

            // if no more lines the readLine() returns null
            while ((line = br.readLine()) != null) {
                String [] s = line.split(" ");
                float[] des = new float[3780] ;

                for(int i=0 ;i< s.length ;i++) {
                    des[i]  = Float.parseFloat(s[i]);
                }
                desList.add(des);
                count_line++;
            }
            return true;
        }
        catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Error when reading model.");
            return false;
        }
    }

    public ArrayList<Rect> GetRects() {
        return RECTS;
    }
    public double GetString() {
        return NUMBER_RESULT;
    }
}
