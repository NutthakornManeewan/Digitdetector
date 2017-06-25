package com.example.antenna.digitdetector;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;

import jxl.read.biff.BiffException;
import jxl.write.WriteException;


public class MainActivity extends Activity implements CvCameraViewListener2, AdapterView.OnItemSelectedListener {
    private static final String OPENCV_TAG = "OCVSample::Activity";
    private static final String NUMBER_TAG = "Result";

    private Spinner spinner_customer;
    private Spinner spinner_order;
    ArrayAdapter<String> adapter_customer;
    ArrayAdapter<String> adapter_order;

    private final int THICKNESS = 3;
    private final int LINETYPE  = 2;
    private final int SHIFT     = 0;
    public  int order_index, customer_index;
    private boolean isTouch     = true;
    private boolean sendButton  = false;
    public double NUMBER_RESULT = 0;
    public float xVal=0.0f, xMove=0.0f, yVal=0.0f, yMove=0.0f;

    private Mat mRgba;
    private Mat MatforCal;
    private ArrayList<Rect> RectforCal;
    private ArrayList<String> customer_list = new ArrayList<>();
    private ArrayList<String> order_list = new ArrayList<>();

    private CameraBridgeViewBase mOpenCvCameraView;
    private StorageDisk storageDisk;
    private ColorBlobDetector mDetector;
    private ExcelProcessing excelP;
    private CustomSelectedItem dropdown_getindex = new CustomSelectedItem();

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(OPENCV_TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    private GoogleApiClient client;
    public MainActivity() {
        Log.i(OPENCV_TAG, "Instantiated new " + this.getClass());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(OPENCV_TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        spinner_customer = (Spinner)findViewById(R.id.spinner_customer);
        spinner_order    = (Spinner)findViewById(R.id.spinner_order);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.color_blob_detection_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
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
            Log.d(OPENCV_TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(OPENCV_TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba         = new Mat(height, width, CvType.CV_8UC4);
        mDetector     = new ColorBlobDetector();
        mDetector.modelReader();
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public void acceptImage (View view) { isTouch    = false;}
    public void retakeImage (View view) {
        isTouch    = true;
    }

    public void excelProcess (View view) throws IOException, BiffException {
        excelP.readExcelSetup();
        customer_list = excelP.readExcelCustomerSheet();
        adapter_customer = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, customer_list);
        adapter_customer.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_customer.setAdapter(adapter_customer);
        spinner_customer.setOnItemSelectedListener(this);
    }

    // --- If click to send data, do this method ---
    public void acceptData  (View view) throws IOException, WriteException {
        if (NUMBER_RESULT >= 0) {
              excelP.WriteWorkbook(customer_index, dropdown_getindex.index, NUMBER_RESULT);
              storageDisk = new StorageDisk("File_order_Betagro");
              storageDisk.connectToDisk();
        }
    }
    public void checkWifi  (View view) throws IOException, BiffException {
        excelP = new ExcelProcessing("File_order_Betagro");
        excelP.downloadFileExcel();
        Log.i("Excel", "Check WiFi and Download file successfully!");
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        ArrayList<Rect> rects;
        Mat checkMat = null;
        mRgba                 = inputFrame.rgba();
        int frame_height      = mRgba.height();
        Point text_pos        = new Point(100, frame_height-50);

        if (isTouch) {
            checkMat      = mDetector.process(mRgba);
            rects         = mDetector.GetRects();
            RectforCal    = (ArrayList<Rect>) rects.clone();
            MatforCal     = checkMat;
            NUMBER_RESULT = 0;

            //--- If rect == null program ERROR!!
            if (!rects.isEmpty()) {
                for (int i = 0; i < rects.size(); i++) {
                    Imgproc.rectangle(mRgba, new Point(rects.get(i).tl().x - 7, rects.get(i).tl().y - 7),
                            new Point(rects.get(i).br().x + 7, rects.get(i).br().y + 7),
                            new Scalar(0, 255, 0, 255), THICKNESS, LINETYPE, SHIFT);
                    //Imgproc.putText(mRgba, Double.toString(rects.get(i).area()), rects.get(i).tl(), 1, 2, new Scalar(0, 255, 0, 255), 1);
                }
                Imgproc.putText(mRgba, Double.toString(NUMBER_RESULT), text_pos, 2, 3.5, new Scalar(0, 255, 0, 255), 5);
            }
        } else {
            if (!RectforCal.isEmpty()) {
                mDetector.SortElements();
                NUMBER_RESULT = mDetector.GetString();
                Imgproc.putText(mRgba, Double.toString(NUMBER_RESULT), text_pos, 2, 3.5, new Scalar(0, 255, 0, 255), 5);
                Log.i(NUMBER_TAG, "Number result : " + NUMBER_RESULT);
            }
        }
        return mRgba;
    }

    @Override
    public void onStart() {
        super.onStart();
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW,
                "Main Page",
                Uri.parse("http://host/path"),
                Uri.parse("android-app://com.example.antenna.digitdetector/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW,
                "Main Page",
                Uri.parse("http://host/path"),
                Uri.parse("android-app://com.example.antenna.digitdetector/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

//    @Override
//    public boolean onTouch(View view, MotionEvent motionEvent) {
//        int MotionCapture = motionEvent.getAction();
//        xVal = motionEvent.getRawX();
//        yVal = motionEvent.getRawY();
//        switch (MotionCapture & MotionEvent.ACTION_MASK) {
//            case MotionEvent.ACTION_DOWN:
//                xMove = motionEvent.getX();
//                yMove = motionEvent.getY();
//                break;
//            case MotionEvent.ACTION_UP:
//                break;
//            case MotionEvent.ACTION_POINTER_DOWN:
//                break;
//            case MotionEvent.ACTION_POINTER_UP:
//                break;
//            case MotionEvent.ACTION_MOVE:
//                break;
//            default:
//                return true;
//        }
//        return true;
//    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        customer_index = position;
        Log.i("Excel", "Customer index : " + customer_index);
        order_list.clear();
        order_list    = excelP.readExcelOrderReportSheet(customer_list.get(position));
        adapter_order = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, order_list);
        adapter_order.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner_order.setAdapter(adapter_order);
        spinner_order.setOnItemSelectedListener(dropdown_getindex);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }
}