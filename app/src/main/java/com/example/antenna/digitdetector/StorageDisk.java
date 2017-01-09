package com.example.antenna.digitdetector;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

/**
 * Created by Antenna on 6/20/2016.
 */
public class StorageDisk {

    String file_write;
    String TAG = "FTP_Status";
    StorageDisk (String data_write) {
        file_write = data_write;
    }

    public void connectToDisk () {
        new AsyncTask<Integer, Void, Void>() {
            @Override
            protected Void doInBackground(Integer... integers) {
                Session session         = null;
                Channel channel         = null;
                ChannelSftp channelSftp = null;
                int SFTPPORT            = 22;
                String workDir          = "/home/root/";
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                String currentDateandTime = sdf.format(new Date());

                try {
                    JSch jSch = new JSch();
                    session   = jSch.getSession("root", "192.168.60.1", SFTPPORT);
                    session.setPassword("welc0me");
                    Log.d(TAG, "Set password successfully.");

                    Properties config = new Properties();
                    config.put("StrictHostKeyChecking", "no");
                    session.setConfig(config);
                    Log.d(TAG, "Set config successfully.");
                    session.connect();
                    Log.d(TAG, "Connect to 192.168.60.1 successfully.");

                    channel     = session.openChannel("sftp");
                    Log.d(TAG, "Open channel SFTP successfully.");
                    channelSftp = (ChannelSftp)channel;
                    channelSftp.connect();
                    String now_dir = channelSftp.pwd();
                    Log.d(TAG, "Now in this \""+ now_dir + "\"");
                    File f = new File(Environment.getExternalStorageDirectory(), file_write+".xls");
                    Log.d(TAG, "Read file to 'f' successfully. --> " + Environment.getExternalStorageDirectory());

                    channelSftp.put(new FileInputStream(f), file_write+".xls");
                    Log.d(TAG, "Put file to " + workDir + " successfully!");
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d(TAG, "Have error : " + e.toString());
                }
                return null;
            }
        }.execute(1);
    }

    public void WriteToPhone (String file_name, String result) {
        try {

            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            String currentDateandTime = sdf.format(new Date());

            String filename= "/sdcard/"+file_name+currentDateandTime+".txt";
            FileWriter fw = new FileWriter(filename,true); //the true will append the new data
            fw.write(result + "\n");
            Log.d(TAG, "Save to sdcard already!");
            fw.close();
        }
        catch(IOException ioe)
        {
            System.err.println("IOException: " + ioe.getMessage());
        }
    }
}
