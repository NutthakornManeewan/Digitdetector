package com.example.antenna.digitdetector;
/**
 * Created by wwwNa on 12/8/2016.
 */

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Properties;

import jxl.*;
import jxl.write.*;
import jxl.read.biff.BiffException;
import jxl.write.Number;

public class ExcelProcessing {
    String TAG = "Excel";
    String db_name, product_type;
    File input_workbook;
    WritableWorkbook wa;
    Workbook w;
    WorkbookSettings wbSettings = new WorkbookSettings();
    WritableFont generalFont    = new WritableFont(WritableFont.TAHOMA, 10, WritableFont.BOLD);

    FileInputStream f;

    private String input_file;
    private WritableCellFormat generalCell;
    private ArrayList<Integer> customer_index = new ArrayList<>();

    ExcelProcessing(String file_name) throws IOException, BiffException {
        input_file           = file_name;
    }

    public void downloadFileExcel() {
        new AsyncTask<Integer, Void, Void>() {
            @Override
            protected Void doInBackground(Integer... integers) {
                Session session         = null;
                Channel channel         = null;
                ChannelSftp channelSftp = null;
                int SFTPPORT            = 22;
                String workDir          = "/home/root/";

                try {
                    JSch jsch = new JSch();
                    session   = jsch.getSession("root", "192.168.60.1", SFTPPORT);
                    session.setPassword("welc0me");
                    Log.d(TAG, "Set password successfully.");

                    Properties config = new Properties();
                    config.put("StrictHostKeyChecking", "no");
                    session.setConfig(config);
                    session.connect();

                    channel     = session.openChannel("sftp");
                    Log.d(TAG, "Open channel SFTP successfully.");

                    channelSftp = (ChannelSftp)channel;
                    channelSftp.connect();
                    OutputStream output = new FileOutputStream("/sdcard/"+input_file+".xls");
                    channelSftp.get(input_file+".xls", output);
                    output.close();
                } catch (JSchException e) {
                    e.printStackTrace();
                } catch (SftpException e) {
                    e.printStackTrace();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute(1);
    }

    public void readExcelSetup() throws IOException, BiffException {
        input_workbook = new File(Environment.getExternalStorageDirectory(), input_file+".xls");
        w                    = Workbook.getWorkbook(input_workbook);
        Sheet customer_sheet = w.getSheet(0);
        db_name              = customer_sheet.getCell(2,1).getContents().substring(0,6);
        product_type         = customer_sheet.getCell(2,2).getContents();
    }

    /*public void readExcelFile() throws IOException {
        generalCell = new WritableCellFormat(generalFont);
        File input_workbook = new File(Environment.getExternalStorageDirectory(), input_file+".xls");

        try {
            wbSettings.setLocale(new Locale("en", "EN"));
            wa = Workbook.createWorkbook(input_workbook, wbSettings);
            wa.createSheet(w.getSheet(0).getName(), 0);

            Sheet sheet          = w.getSheet(0);
            WritableSheet wSheet = wa.getSheet(0);
            Cell cell;
            WritableFont tmp_font;

            for (int i=0; i<sheet.getColumns(); i++) {
                for (int j=0; j<sheet.getRows(); j++) {
                    cell = sheet.getCell(i,j);
                    if (cell.getCellFormat().getFont().getBoldWeight() >= 700) {
                        tmp_font = new WritableFont(WritableFont.TAHOMA, 10, WritableFont.BOLD);
                        wSheet.addCell(new Label(i, j, cell.getContents(), new WritableCellFormat(tmp_font)));
                    }
                    else {
                        tmp_font = new WritableFont(WritableFont.TAHOMA, 10);
                        wSheet.addCell(new Label(i, j, cell.getContents(), new WritableCellFormat(tmp_font)));
                    }
                }
            }
            Number num = new Number(4,5, 100, generalCell);
            addNumber(wSheet, num);
            wa.write();
            wa.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (WriteException e) {
            e.printStackTrace();
        }
    }*/

    public ArrayList<String> readExcelCustomerSheet() throws IOException, BiffException {

        ArrayList<String> customer_list = new ArrayList<>();
        Sheet customer_sheet = w.getSheet("Customer_"+db_name+"_"+product_type+"_1");
        Log.i(TAG, "Customer_"+db_name+"_"+product_type+"_1");

        // === Typically list of customer name start in cell(3,5) ===
        int i = 5;
        while (!customer_sheet.getCell(3, i).getContents().isEmpty()) {
            customer_list.add(customer_sheet.getCell(3, i++).getContents());
            if (i >= customer_sheet.getRows())
                break;
        }
        return customer_list;
    }

    public ArrayList<String> readExcelOrderReportSheet (String customer_name) {
        ArrayList<String> order_list = new ArrayList<>();
        Sheet order_sheet            = w.getSheet("Total_"+db_name+"_"+product_type+"_1");
        int row_size                 = order_sheet.getRows();

        for (int i=0; i<row_size; i++) {
            if (customer_name.equals(order_sheet.getCell(0,i).getContents())) {
                customer_index.add(i);
                int j=i-1;
                while (!order_sheet.getCell(3,j).getContents().isEmpty()) {
                    order_list.add(order_sheet.getCell(3, j++).getContents());
                    if (j >= row_size)
                        break;
                }
                break;
            }
        }
        return order_list;
    }

    public void addNumber (WritableSheet sheet, Number num) throws WriteException {
        try {
            sheet.addCell(num);
        } catch (WriteException e) {
            e.printStackTrace();
        }
    }

    public void WriteWorkbook(int cusIdx, int ordIndex, double numRes) throws IOException, WriteException {
        generalCell = new WritableCellFormat(generalFont);
        File input_workbook = new File(Environment.getExternalStorageDirectory(), input_file+".xls");

        Sheet custo_sheet = w.getSheet("Customer_"+db_name+"_"+product_type+"_1");
        Sheet order_sheet = w.getSheet("Total_"+db_name+"_"+product_type+"_1");
        Sheet sumry_sheet = w.getSheet("PPLan_"+db_name+"_"+product_type+"_1");

        Log.i("Excel", "Custo_sheet : " + custo_sheet.getName());

        wbSettings.setLocale(new Locale("en", "EN"));
        wa = Workbook.createWorkbook(input_workbook, wbSettings);
        wa.createSheet(custo_sheet.getName(), 0);
        wa.createSheet(order_sheet.getName(), 1);
        wa.createSheet(sumry_sheet.getName(), 2);

        WritableSheet custoSheet = wa.getSheet(0);
        WritableSheet orderSheet = wa.getSheet(1);
        WritableSheet sumrySheet = wa.getSheet(2);

        Cell cell;
        WritableFont tmp_font;

        for (int i=0; i<order_sheet.getColumns(); i++) {
            for (int j=0; j<order_sheet.getRows(); j++) {
                cell = order_sheet.getCell(i,j);
                if (!cell.getContents().isEmpty()) {
                    if (cell.getCellFormat().getFont().getBoldWeight() >= 700) {
                        tmp_font = new WritableFont(WritableFont.TAHOMA, 10, WritableFont.BOLD);
                        orderSheet.addCell(new Label(i, j, cell.getContents(), new WritableCellFormat(tmp_font)));
                    } else {
                        tmp_font = new WritableFont(WritableFont.TAHOMA, 10);
                        orderSheet.addCell(new Label(i, j, cell.getContents(), new WritableCellFormat(tmp_font)));
                    }
                }
            }
        }

        for (int i=0; i<custo_sheet.getColumns(); i++) {
            for (int j=0; j<custo_sheet.getRows(); j++) {
                cell = custo_sheet.getCell(i,j);
                if (!cell.getContents().isEmpty()) {
                    if (cell.getCellFormat().getFont().getBoldWeight() >= 700) {
                        tmp_font = new WritableFont(WritableFont.TAHOMA, 10, WritableFont.BOLD);
                        custoSheet.addCell(new Label(i, j, cell.getContents(), new WritableCellFormat(tmp_font)));
                    } else {
                        tmp_font = new WritableFont(WritableFont.TAHOMA, 10);
                        custoSheet.addCell(new Label(i, j, cell.getContents(), new WritableCellFormat(tmp_font)));
                    }
                }
            }
        }

        for (int i=0; i<sumry_sheet.getColumns(); i++) {
            for (int j=0; j<sumry_sheet.getRows(); j++) {
                cell = sumry_sheet.getCell(i,j);
                if (!cell.getContents().isEmpty()) {
                    if (cell.getCellFormat().getFont().getBoldWeight() >= 700) {
                        tmp_font = new WritableFont(WritableFont.TAHOMA, 10, WritableFont.BOLD);
                        sumrySheet.addCell(new Label(i, j, cell.getContents(), new WritableCellFormat(tmp_font)));
                    } else {
                        tmp_font = new WritableFont(WritableFont.TAHOMA, 10);
                        sumrySheet.addCell(new Label(i, j, cell.getContents(), new WritableCellFormat(tmp_font)));
                    }
                }
            }
        }

        Number num = new Number(customer_index.get(cusIdx), ordIndex, numRes, generalCell);
        orderSheet.addCell(num);
        wa.write();
        wa.close();
    }
}
