package com.google.vrtoolkit.cardboard.samples.treasurehunt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.os.Environment;
import android.util.Log;

import java.io.InputStreamReader;

/**
 * Created by sundeqing on 6/1/15.
 */
class ObjUtil {
    private static final String TAG = "ObjUtil";

    public float[] bananaVert, bananaNorm, bananaText;
    public int bananaTextFile;
    public int bananaVertexCount;

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
        //bananaTextFile=loadTexture("Cardboard/obj_info/banana.jpg");
        bananaVertexCount=768;
        bananaVert=loadArrayFromFile("Cardboard/obj_info/banana.hVerts",bananaVertexCount*3);
        bananaNorm =loadArrayFromFile("Cardboard/obj_info/banana.hNorms",bananaVertexCount*3);
        bananaText =loadArrayFromFile("Cardboard/obj_info/banana.hTexts",bananaVertexCount*2);

        /*bananaVertexCount=36;
        bananaVert=loadArrayFromFile("Cardboard/obj_info/cube-textures.hVerts",bananaVertexCount*3);
        bananaNorm =loadArrayFromFile("Cardboard/obj_info/cube-textures.hNorms",bananaVertexCount*3);
        bananaText =loadArrayFromFile("Cardboard/obj_info/cube-textures.hTexts",bananaVertexCount*2);*/


    }

    public int loadTexture(String filename)
    {
        final int[] textureHandle = new int[1];

        GLES20.glGenTextures(1, textureHandle, 0);

        if (textureHandle[0] != 0)
        {
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inScaled = false;   // No pre-scaling

            // Read in the resource
            final Bitmap bitmap = BitmapFactory.decodeFile(Environment.getExternalStorageDirectory() +"/"+ filename);

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();

            Log.i("loadGLTexture", "Bitmap:{w:" + width + " h:" + height + "}");


            // Bind to the texture in OpenGL
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

            // Set filtering
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

            // Recycle the bitmap, since its data has been loaded into OpenGL.
            bitmap.recycle();
        }

        if (textureHandle[0] == 0)
        {
            throw new RuntimeException("Error loading texture.");
        }

        return textureHandle[0];
    }
}