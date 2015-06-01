package com.google.vrtoolkit.cardboard.samples.treasurehunt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import android.os.Environment;
import android.util.Log;

import java.io.InputStreamReader;

/**
 * Created by sundeqing on 6/1/15.
 */
class ObjUtil {
    private static final String TAG = "ObjUtil";

    public float[] bananaVert, bananaNorm, bananaText;

    public float[] loadSettingsFromFile(String filename) {
        BufferedReader br = null;
        try {
            String fpath = Environment.getExternalStorageDirectory() +"/"+ filename;

            //Log.i(TAG, fpath);

            br = new BufferedReader(new FileReader(fpath));

            String line = "";
            while ((line = br.readLine()) != null) {
                Log.i(TAG, line);
            }


        }catch (FileNotFoundException e){
            // do stuff here..
            return null;
        }catch (IOException e){
            // do stuff here..
            return null;
        }
        return null;
    }



    public float[] loadArrayFromFile(String filename,int length) {

        String fpath = Environment.getExternalStorageDirectory() +"/"+ filename;
        //http://maider.blog.sohu.com/281585518.html
        /*Using BufferedReader*/
        int i = 0;
        float[] dataArray = new float[length];
        try {
            BufferedReader buff = new BufferedReader(new InputStreamReader(new FileInputStream(new File(fpath))));
            while (i < length) {
                dataArray[i++] = Float.parseFloat(buff.readLine());
            }
            buff.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataArray;
    }

    public void loadBanana() {
        bananaVert=loadArrayFromFile("Cardboard/obj_info/banana.hVerts",8056*9);
        bananaNorm =loadArrayFromFile("Cardboard/obj_info/banana.hNorms",8056*9);
        bananaText =loadArrayFromFile("Cardboard/obj_info/banana.hTexts",8056*6);
    }
}