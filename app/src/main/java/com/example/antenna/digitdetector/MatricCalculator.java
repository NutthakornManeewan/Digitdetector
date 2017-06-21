package com.example.antenna.digitdetector;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by wwwNa on 8/29/2016.
 */
public class MatricCalculator {

    public String TAG = "Result";

    public static double[][] matA;
    public static List<float[]> matB = new ArrayList<float[]>();
    public static double[][] matResult;

    MatricCalculator(double[][] A, List<float[]> B) {
        matA      = A;
        matB      = B;
        matResult = new double[matA.length][matB.get(0).length];
    }

    public static double[][] multiplicar() {
    int aRows = matA.length;
        int aColumns = matA[0].length;
        int bRows = matB.size();
        int bColumns = matB.get(0).length;
        int cRows = matResult.length;
        int cColumns = matResult[0].length;

        if (aColumns != bRows) {
            throw new IllegalArgumentException("A:Rows: " + aColumns + " did not match B:Columns " + bRows + ".");
        }

        for (int i = 0; i < cRows; i++) {
            for (int j = 0; j < cColumns; j++) {
                matResult[i][j] = 0.00000;
            }
        }

        for (int i = 0; i < aRows; i++) { // aRow
            for (int j = 0; j < bColumns; j++) { // bColumn
                for (int k = 0; k < aColumns; k++) { // aColumn
                    matResult[i][j] += matA[i][k] * matB.get(k)[j];
                }
            }
        }
        return matResult;
    }

    public static double[][] hyperbolicTangent(double[][] inputMat) {
        double[][] resultFunction = new double[inputMat.length][inputMat[0].length];
        for (int i=0; i<inputMat[0].length; i++) {
            resultFunction[0][i] = 1 / (1 + Math.exp(-inputMat[0][i]));
        }
        return resultFunction;
    }
}
