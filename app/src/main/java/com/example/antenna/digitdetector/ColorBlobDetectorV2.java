package com.example.antenna.digitdetector;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.HOGDescriptor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Antenna on 7/11/2016.
 */
public class ColorBlobDetectorV2 {

    public static final String TAG = "Result";
    public double NUMBER_RESULT  = 0;
    public ArrayList<Rect> RECTS = new ArrayList<Rect>();

    public static List<float[]> weightList1 = new ArrayList<float[]>();
    public static List<float[]> weightList2 = new ArrayList<float[]>();
    public static List<float[]> weightList3 = new ArrayList<float[]>();

    // ----- Cache -----
    Mat mGray                = new Mat();
    Mat mBlur                = new Mat();
    Mat mThreshold           = new Mat();
    Mat mReducedNoise        = new Mat();
    Mat hierarchy            = new Mat();
    MatOfPoint2f ApproxCurve = new MatOfPoint2f();

    // ----- Ellipse Kernel -----
    Mat EllipeKernel5x5 = Imgproc.getStructuringElement (Imgproc.MORPH_ELLIPSE, new Size(5, 5));
    Mat EllipeKernel7x7 = Imgproc.getStructuringElement (Imgproc.MORPH_ELLIPSE, new Size(20, 20));

    public Mat process (Mat rgbaImage) {
        ArrayList<MatOfPoint> mContours = new ArrayList<MatOfPoint>();
        RECTS.clear();
        Imgproc.cvtColor    (rgbaImage, mGray, Imgproc.COLOR_RGB2GRAY);
        Imgproc.GaussianBlur(mGray, mBlur, new Size(7,7), 0.5, 0.5);
        Imgproc.threshold   (mBlur, mThreshold, 200, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C);

        // ----- Reduce noise processes -----
        Imgproc.morphologyEx(mThreshold, mReducedNoise, Imgproc.MORPH_OPEN, EllipeKernel5x5);
        Imgproc.morphologyEx(mReducedNoise, mReducedNoise, Imgproc.MORPH_CLOSE, EllipeKernel7x7);

        // --- After doing a Morphological process then find contours around masked image.
        Imgproc.findContours(mReducedNoise, mContours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

        // --- Maximum suppression object to reduced not interested rectangle object -----
        for (int i=0; i < mContours.size(); i++) {
            MatOfPoint2f contour2f = new MatOfPoint2f (mContours.get(i).toArray());

            // --- Imgproc.arcLength : Compute perimeter for contours. ---
            double ApproxDistance  = Imgproc.arcLength (contour2f, true) * 0.0005 ;
            // --- After compute perimeter then create line around these polygon. ---
            Imgproc.approxPolyDP   (contour2f, ApproxCurve, ApproxDistance, true); // find line around polygon.
            MatOfPoint points      = new MatOfPoint (ApproxCurve.toArray());
            Rect rect              = Imgproc.boundingRect (points);
            // ----- If width > 20 and height > 200 then remember them to interest rectangle -----
            if(rect.br().x - rect.tl().x > 20 && rect.br().y - rect.tl().y > 200) {
                if (RECTS.size()>=1
                        && (rect.br().y < RECTS.get(RECTS.size()-1).br().y)
                        && (rect.br().x < RECTS.get(RECTS.size()-1).br().x)
                        && (rect.tl().x > RECTS.get(RECTS.size()-1).tl().x)
                        && (rect.tl().y > RECTS.get(RECTS.size()-1).tl().y)) {
                }
                else if (RECTS.size()<1) {
                    RECTS.add(rect);
                }
                else {
                    RECTS.add(rect);
                }
            }
        }
        return mReducedNoise;
    }

    public void SortElements (ArrayList<Rect> RectForCal, Mat MatForCal) {
        Rect tmp     = null;
        boolean flag = true;

        while (flag) {
            flag = false;
            for (int j = 0; j < RectForCal.size() - 1; j++) {
                if (RectForCal.get(j + 1).tl().x < RectForCal.get(j).tl().x) {
                    tmp = RectForCal.get(j);
                    RectForCal.set(j, RectForCal.get(j + 1));
                    RectForCal.set(j + 1, tmp);
                    flag = true;
                }
            }
        }

        Log.e(TAG, "NIGHTMARE IS STARTEDDDDDDDDDD!");
        // ---- Feature Extraction -----
        int stringCode[]       = new int[RectForCal.size()];

        if (weightList1.isEmpty() && weightList2.isEmpty() && weightList3.isEmpty()) {
            readWeight("weight_1f", weightList1);
            readWeight("weight_2f", weightList2);
            readWeight("weight_3f", weightList3);
            Log.i(TAG, "Read weight successfully! : " + weightList1.get(0).length + " " + weightList2.get(0).length+" " + weightList3.get(0).length);
        }
        NUMBER_RESULT = 0;

        for (int i = 0; i < RectForCal.size(); i++) {
            Mat mDigit    = MatForCal.submat((int) RectForCal.get(i).tl().y - 7, (int) RectForCal.get(i).br().y + 7, (int) RectForCal.get(i).tl().x - 7, (int) RectForCal.get(i).br().x + 7);
            int front     = 0;
            double area   = (RectForCal.get(i).br().x - RectForCal.get(i).tl().x) * (RectForCal.get(i).br().y - RectForCal.get(i).tl().y);
            double Multip = Math.pow(10, RectForCal.size() - 1 - i);

            if (area >= 22000) {
                Log.i(TAG, "Area larger than 22,000");
                stringCode[i] = classiflyDigit(mDigit);
                front = (int) (stringCode[i] / 10);
                NUMBER_RESULT = NUMBER_RESULT + (front * Multip);
            }
            else {
                stringCode[i] = 1;
                NUMBER_RESULT = NUMBER_RESULT + (1 * Multip);
            }
        }

        if (RectForCal.size() >= 4)
            NUMBER_RESULT = NUMBER_RESULT / 1000;
    }

    public static double[][] extractHOG (Mat digit) {
        Mat rDigit = new Mat();
        // -- Default size = (64,128) and hog descriptor is 3780 dimensions!!!;
        // -- Now ! change to size (40x55) and wait for test.
        Imgproc.resize (digit, rDigit, new Size(64, 128));
//        final HOGDescriptor hog = new HOGDescriptor();
        final HOGDescriptor hog = new HOGDescriptor();
        MatOfFloat descriptors  = new MatOfFloat();
        MatOfPoint locations    = new MatOfPoint();

        hog.compute(rDigit, descriptors);
        
        double[][] des = new double[1][descriptors.height()+1];
        for(int m = 1; m<descriptors.height() ; m++) {
            des[0][m] = descriptors.get(m, 0)[0];
        }
        des[0][0] = 1.0;
        return des;
    }

    public static int classiflyDigit(Mat digit){
        int idx_max = 0;
        double maxVal  = -99999;
        double[][] mDescriptors1 = extractHOG(digit);
        Log.i(TAG, "ExtractHOG! with size: ["+mDescriptors1.length+"]["+mDescriptors1[0].length+"]");
        MatricCalculator w1   = new MatricCalculator(mDescriptors1, weightList1);
        double[][] resW1      = w1.multiplicar();
        resW1                 = w1.hyperbolicTangent(resW1);
        MatricCalculator w2   = new MatricCalculator(resW1, weightList2);
        double[][] resW2      = w2.multiplicar();
        resW2                 = w1.hyperbolicTangent(resW2);
        MatricCalculator w3   = new MatricCalculator(resW2, weightList3);
        double[][] resW3      = w3.multiplicar();
        resW3                 = w1.hyperbolicTangent(resW3);

        for (int i=0; i<10; i++) {
            if(resW3[0][i] > maxVal) {
                maxVal = resW3[0][i];
                idx_max = i;
            }
        }
        Log.i(TAG, "Index max : "+idx_max+" | value max : " + maxVal);
        return 0;
    }

    public static boolean readWeight(String fname, List<float[]> desList) {
        try {
            File sdcard     = Environment.getExternalStorageDirectory();
            String fileName = fname + ".txt";
            File file       = new File(sdcard, fileName);
            // If file does not exists, then create it
            if (!file.exists())
                Log.i(TAG, "File is not exists");

            FileReader fileReader = new FileReader(file.getAbsoluteFile());
            BufferedReader br     = new BufferedReader(fileReader);
            String line           = null;

            // if no more lines the readLine() returns null
            while ((line = br.readLine()) != null) {
                String [] s = line.split(" ");
                float[] des = new float[11] ;
                for(int i=0 ;i< s.length ;i++)
                    des[i]  = Float.parseFloat(s[i]);
                desList.add(des);
            }
            return true;
        }
        catch (IOException e) {
            e.printStackTrace();
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
