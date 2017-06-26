package com.example.antenna.digitdetector;

import android.os.Environment;
import android.util.Log;

import org.opencv.core.Core;
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

public class ColorBlobDetector {

    private static final String ResearchTag  = "Result";
    public double NUMBER_RESULT           = 0;
    public static List<float[]> desList1  = new ArrayList<float[]>();
    public ArrayList<Double> rect_density = new ArrayList<Double>();
    public ArrayList<Rect> RECTS          = new ArrayList<Rect>();

    // ----- Cache -----
    Mat mGray                = new Mat();
    Mat mSharp               = new Mat();
    Mat mBlur                = new Mat();
    Mat mThreshold           = new Mat();
    Mat mReducedNoise        = new Mat();
    Mat mReducedNoise_t      = new Mat();
    Mat hierarchy            = new Mat();
    MatOfPoint2f ApproxCurve = new MatOfPoint2f();
    int FrameWidth=0, FrameHeight=0;

    // ----- Ellipse Kernel -----
    Mat RectKernel = Imgproc.getStructuringElement (Imgproc.MORPH_RECT, new Size(2, 6));

    public void modelReader() {
        if (desList1.isEmpty()) {
            readModel("Model", desList1);
            Log.e(ResearchTag, "Read model successfully!");
        }
    }

    public Mat process (Mat rgbaImage) {
        RECTS.clear();
        FrameWidth = rgbaImage.width();
        FrameHeight= rgbaImage.height();
        ArrayList<MatOfPoint> mContours = new ArrayList<>();
        Mat RectKernelErode = Imgproc.getStructuringElement (Imgproc.MORPH_RECT, new Size(5,10));
        Mat RectKernelClose = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5,5));
        double MAXIMUM_AREA = 100000, MINIMUM_AREA = 1500, MIN_RATIO=0.1, MAX_RATIO=0.9, HEIGHT_TH=150;

        // ***** Preprocess - 01 *****
        Imgproc.cvtColor    (rgbaImage, mGray, Imgproc.COLOR_RGB2GRAY);
        Imgproc.GaussianBlur(mGray, mBlur, new Size(7,7), 1.5, 1.5);

        // ***** Thresholding *****
        Imgproc.adaptiveThreshold(mBlur, mThreshold, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 75, 7.0);
        Imgproc.morphologyEx(mThreshold, mThreshold, Imgproc.MORPH_ERODE, RectKernelErode);
        Imgproc.threshold (mThreshold, mThreshold, 0, 255, Imgproc.THRESH_BINARY_INV);
        Imgproc.morphologyEx(mThreshold, mThreshold, Imgproc.MORPH_ERODE, RectKernelClose);

        // ***** Find contour and filtering process *****
        Imgproc.findContours(mThreshold, mContours, hierarchy, Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_SIMPLE);

        for (int i=0; i < mContours.size(); i++) {
            MatOfPoint2f contour2f = new MatOfPoint2f (mContours.get(i).toArray());
            double ApproxDistance  = Imgproc.arcLength (contour2f, true) * 0.0005 ;
            double contourArea     = Imgproc.contourArea(mContours.get(i));

            Imgproc.approxPolyDP (contour2f, ApproxCurve, ApproxDistance, true); // find line around polygon.
            MatOfPoint points    = new MatOfPoint (ApproxCurve.toArray());
            Rect rect            = Imgproc.boundingRect (points);
            double tempRectWidth = rect.br().x - rect.tl().x;
            double tempRectHeight= rect.br().y - rect.tl().y;
            double aspectRatio   = tempRectWidth / tempRectHeight;
            /* ***** Area size *****
                Only my ASUS-Zenfone 2
                Horizontal : 884736
                Vertical   : 622080
             **********************/
            if (rect.br().x <= mThreshold.size().width && rect.br().y <= mThreshold.size().height && rect.tl().x >= 0 && rect.tl().y >= 0) {
                    if (tempRectWidth - tempRectHeight < 0) {
                        if (rect.area() > MINIMUM_AREA && rect.area() < MAXIMUM_AREA) {
                            if (aspectRatio > MIN_RATIO && aspectRatio < MAX_RATIO) {
                                if (Math.abs(rect.tl().y - rect.br().y) >= HEIGHT_TH) {
                                    RECTS.add(rect);
                                    rect_density.add(contourArea / rect.area());
                                }
                            }
                        }
                    }
                }
            }
        return mReducedNoise_t;
    }

    public void SortElements () {
        Rect tmp        = null;
        double tmp_area = 0.0;
        boolean flag    = true;
        while (flag) {
            flag = false;
            for (int j = 0; j < RECTS.size() - 1; j++) {
                if (RECTS.get(j + 1).tl().x < RECTS.get(j).tl().x) {
                    tmp      = RECTS.get(j);
                    tmp_area = rect_density.get(j);

                    rect_density.add(j, rect_density.get(j+1));
                    rect_density.add(j+1, tmp_area);
                    RECTS.set(j, RECTS.get(j + 1));
                    RECTS.set(j + 1, tmp);
                    flag = true;
                }
            }
        }

        if (RECTS.size()>1) {
            ArrayList<Integer> tmpIndexToRemove = new ArrayList<>();
            for (int i=1; i<RECTS.size(); i++) {
                if (isInside(RECTS.get(i-1), RECTS.get(i))) {
                    tmpIndexToRemove.add(i);
                }
            }
            for (int j=0; j<tmpIndexToRemove.size(); j++) {
                RECTS.remove((int) tmpIndexToRemove.get(j));
            }
        }

        // ---- Feature Extraction -----
        int stringCode[] = new int[RECTS.size()];
        NUMBER_RESULT    = 0;

        for (int i = 0; i < RECTS.size(); i++) {
            int OffsetPlus = 5, offsetCheck = 7;
            if (RECTS.get(i).tl().y - offsetCheck >= 0 && RECTS.get(i).tl().x - offsetCheck >= 0 && RECTS.get(i).br().x+offsetCheck <= FrameWidth && RECTS.get(i).br().y+offsetCheck <= FrameHeight) {
                Mat mDigit    = mBlur.submat((int) RECTS.get(i).tl().y - OffsetPlus, (int) RECTS.get(i).br().y + OffsetPlus, (int) RECTS.get(i).tl().x - OffsetPlus, (int) RECTS.get(i).br().x + OffsetPlus);
                int front     = 0;
                double Multip = Math.pow(10, RECTS.size() - 1 - i);
                double widthOfRect = RECTS.get(i).br().x - RECTS.get(i).tl().x;
                double heightOfRect= RECTS.get(i).br().y - RECTS.get(i).tl().y;
                double aspectRatio = widthOfRect/heightOfRect;
                //if (area >= 22000) {
                if (aspectRatio > 0.3) {
                    stringCode[i] = classiflyDigit(mDigit, desList1);
                    front = (int) (stringCode[i] / 10);
                    NUMBER_RESULT = NUMBER_RESULT + (front * Multip);
                } else {
                    stringCode[i] = 1;
                    NUMBER_RESULT = NUMBER_RESULT + (1 * Multip);
                }
            }
        }
        if (NUMBER_RESULT >= 1000)
            NUMBER_RESULT = NUMBER_RESULT / 1000;
        Log.i(ResearchTag, "Number result: " + NUMBER_RESULT);
    }

    public static float[] extractHOG (Mat digit){
        Mat rDigit                  = new Mat();
        Size standardSizeOfSubImage = new Size(64, 128);
        // -- Default size = (64,128);
        Imgproc.resize          (digit, rDigit, standardSizeOfSubImage);
        final HOGDescriptor hog = new HOGDescriptor();
        MatOfFloat descriptors  = new MatOfFloat();
        MatOfPoint locations    = new MatOfPoint();
        final Size winStride    = new Size(rDigit.height(), rDigit.width());
        final Size padding      = new Size(0, 0);
        // --- Compute 'Histogram of Gradients' for sub image. ---
        hog.compute(rDigit, descriptors, winStride, padding, locations);

        float[] des = new float[descriptors.height()];
        for(int m = 0; m < descriptors.height(); m++) {
            des[m] = (float)descriptors.get(m,0)[0];
        }
        return des;
    }

    public static int classiflyDigit(Mat digit, List<float[]> model) {
        float[] mDescriptors1 = extractHOG(digit);
        int special_min;
        int index_min         = 0;
        double min            = 100000;
        int index_mins[]      = new int[model.size()];    // --- 220 size
        double mins[]         = new double[model.size()]; // --- 220 size

        for(int i=0 ; i < model.size(); i++) {
            float[] mDescriptors2 = model.get(i);
            double dist   = disHOG( mDescriptors1 ,  mDescriptors2);
            index_mins[i] = i;
            mins[i]       = dist;
            if(dist < min) {
                min       = dist;
                index_min = i;
            }
        }
        KNNClassifier knnClassifier = new KNNClassifier(index_mins, mins);
        knnClassifier.SortDistance();
        special_min = knnClassifier.Candidate();
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
                Log.i(ResearchTag, "File is not exists");

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
            Log.e(ResearchTag, "Error when reading model.");
            return false;
        }
    }

    public boolean isInside(Rect beforeRect, Rect currentRect) {
        boolean insideValue = false;
        double[] currentXValue={currentRect.tl().x, currentRect.br().x},
                currentYValue={currentRect.tl().y, currentRect.br().y},
                beforeXValue ={beforeRect.tl().x, beforeRect.br().x},
                beforeYValue ={beforeRect.tl().y, beforeRect.br().y};
        if (currentXValue[0] > beforeXValue[0] && currentXValue[1] < beforeXValue[1]) {
            if (currentYValue[0] > beforeYValue[0] && currentYValue[1] < beforeYValue[1]) {
                insideValue = true;
            }
        }
        return insideValue;
    }

    public ArrayList<Rect> GetRects() {
        return RECTS;
    }
    public double GetString() {
        return NUMBER_RESULT;
    }
    public ArrayList GetDensity() {
        return rect_density;
    }
}
