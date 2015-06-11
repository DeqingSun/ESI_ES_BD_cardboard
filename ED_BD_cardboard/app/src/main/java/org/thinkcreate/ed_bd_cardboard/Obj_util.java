package org.thinkcreate.ed_bd_cardboard;

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

import java.io.InputStreamReader;

/**
 * Created by sundeqing on 6/1/15.
 */
class ObjUtil {
    private static final String TAG = "ObjUtil";

    public float[][] objVert, objNorm, objText;
    public int[] objTextFile = new int[0];
    String[] objNames = new String[0];
    int[] objVertices = new int[0];
    float[][] objPosition = new float[0][];//x y distance: right, up, forward

    public int loadSettingsFromFile(String filename) {
        BufferedReader br = null;
        int line_counting=0;
        int obj_counting=0;
        String[] obj_names = new String[0];
        int[] obj_vertices = new int[0];

        try {
            String fpath = Environment.getExternalStorageDirectory() +"/"+ filename;

            //Log.i(TAG, fpath);

            br = new BufferedReader(new FileReader(fpath));

            String line = "";
            while ((line = br.readLine()) != null) {
                //Log.i(TAG, line);
                if (line_counting==0){
                    obj_counting=Integer.parseInt(line);
                    obj_names=new String[obj_counting];
                    obj_vertices=new int[obj_counting];
                    objPosition =new float[obj_counting][3];
                }else if (line_counting<1+obj_counting){
                    int index=line_counting-1;
                    String[] parts = line.trim().split(" ");
                    obj_names[index]=parts[0];
                    obj_vertices[index]=Integer.parseInt(parts[1]);
                    objPosition[index][0]=Float.parseFloat(parts[2]);
                    objPosition[index][1]=Float.parseFloat(parts[3]);
                    objPosition[index][2]=Float.parseFloat(parts[4]);
                }
                line_counting++;
            }
            objNames=obj_names;
            objVertices=obj_vertices;
            objTextFile = new int[obj_counting];
            objVert = new float[obj_counting][];
            objNorm = new float[obj_counting][];
            objText = new float[obj_counting][];
            loadVert();
        }catch (FileNotFoundException e){
            // do stuff here..
            return -1;
        }catch (IOException e){
            // do stuff here..
            return -1;
        }
        return obj_counting;
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

    public void loadVert() {
        for (int i=0;i<objNames.length;i++) {
            objVert[i] = loadArrayFromFile("Cardboard/obj_info/"+objNames[i]+".hVerts", objVertices[i] * 3);
            objNorm[i] = loadArrayFromFile("Cardboard/obj_info/"+objNames[i]+".hNorms", objVertices[i] * 3);
            objText[i] = loadArrayFromFile("Cardboard/obj_info/"+objNames[i]+".hTexts", objVertices[i] * 2);
        }
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

            //Log.i("loadGLTexture", "Bitmap:{w:" + width + " h:" + height + "}");


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

    public void loadAllTextures() {
        for (int i=0;i<objNames.length;i++){
            objTextFile[i]=loadTexture("Cardboard/obj_info/"+objNames[i]+".jpg");
        }
    }
}
