/*
 * Copyright 2014 Google Inc. All Rights Reserved.

 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.thinkcreate.ed_bd_cardboard;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.Vibrator;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;

/**
 * A Cardboard sample application.
 */
public class MainActivity extends CardboardActivity implements CardboardView.StereoRenderer, SensorEventListener {

    private static final String TAG = "MainActivity";

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100.0f;

    private static final float CAMERA_Z = 0.01f;
    private static final float TIME_DELTA = 0.3f;

    private static final float YAW_LIMIT = 0.12f;
    private static final float PITCH_LIMIT = 0.12f;

    private static final int COORDS_PER_VERTEX = 3;

    private int my3dObjCount = 0;

    // We keep the light always position just above the user.
    private static final float[] LIGHT_POS_IN_WORLD_SPACE = new float[] { 0.0f, 2.0f, 0.0f, 1.0f };

    private final float[] lightPosInEyeSpace = new float[4];

    private FloatBuffer floorVertices;
    private FloatBuffer floorColors;
    private FloatBuffer floorNormals;

    //private FloatBuffer cubeVertices;
    //private FloatBuffer cubeColors;
    //private FloatBuffer cubeFoundColors;
    //private FloatBuffer cubeNormals;

    private FloatBuffer my3dObjVertices[] = new FloatBuffer[my3dObjCount];
    private FloatBuffer my3dObjTextures[] = new FloatBuffer[my3dObjCount];
    //private FloatBuffer my3dObjNormals[] = new FloatBuffer[my3dObjCount];

    //private int cubeProgram;
    private int floorProgram;
    private int my3dObjProgram[] = new int[my3dObjCount];

    //private int cubePositionParam;
    //private int cubeNormalParam;
    //private int cubeColorParam;
    //private int cubeModelParam;
    //private int cubeModelViewParam;
    //private int cubeModelViewProjectionParam;
    //private int cubeLightPosParam;

    private int floorPositionParam;
    private int floorNormalParam;
    private int floorColorParam;
    private int floorModelParam;
    private int floorModelViewParam;
    private int floorModelViewProjectionParam;
    private int floorLightPosParam;

    private int my3dObjPositionParam[] = new int[my3dObjCount];
    private int my3dObjNormalParam[] = new int[my3dObjCount];
    private int my3dObjTextureParam[] = new int[my3dObjCount];
    private int my3dObjModelParam[] = new int[my3dObjCount];
    private int my3dObjModelViewParam[] = new int[my3dObjCount];
    private int my3dObjModelViewProjectionParam[] = new int[my3dObjCount];
    private int my3dObjLightPosParam[] = new int[my3dObjCount];
    private int my3dObjTextureUniformHandlerParam[] = new int[my3dObjCount];

    //private float[] modelCube;
    private float[] camera;
    private float[] view;
    private float[] headView;
    private float[] modelViewProjection;
    private float[] modelView;
    private float[] modelFloor;
    private float[][] modelMy3dObj = new float[my3dObjCount][];

    //private int score = 0;
    private float objectDistance = 12f;
    private float floorDepth = 20f;

    private Vibrator vibrator;
    private CardboardOverlayView overlayView;

    private ObjUtil objUtil;

    static final int RESULT_ENABLE = 1;
    private DevicePolicyManager mDeviceManager;
    //private ActivityManager activityManager;
    private ComponentName mCompNameAdmin;
    private SensorManager mSensorManager;
    private long mLastMovementTime = System.currentTimeMillis();
    private int mWakeUpTime = 60*1000;
    private boolean mAwake = true;
    PowerManager.WakeLock mScreenLock;
    private long mFrameStartTime = System.currentTimeMillis();

    private int displayIndex=0;


    /**
     * Converts a raw text file, saved as a resource, into an OpenGL ES shader.
     *
     * @param type The type of shader we will be creating.
     * @param resId The resource ID of the raw text file about to be turned into a shader.
     * @return The shader object handler.
     */
    private int loadGLShader(int type, int resId) {
        String code = readRawTextFile(resId);
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, code);
        GLES20.glCompileShader(shader);

        // Get the compilation status.
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0) {
            Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            shader = 0;
        }

        if (shader == 0) {
            throw new RuntimeException("Error creating shader.");
        }

        return shader;
    }

    /**
     * Checks if we've had an error inside of OpenGL ES, and if so what that error is.
     *
     * @param label Label to report in case of error.
     */
    private static void checkGLError(String label) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, label + ": glError " + error);
            throw new RuntimeException(label + ": glError " + error);
        }
    }

    /**
     * Sets the view to our CardboardView and initializes the transformation matrices we will use
     * to render our scene.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.common_ui);
        CardboardView cardboardView = (CardboardView) findViewById(R.id.cardboard_view);
        cardboardView.setRenderer(this);
        setCardboardView(cardboardView);

        objUtil=new ObjUtil();
        int objCount=objUtil.loadSettingsFromFile("Cardboard/obj_info/setting.txt");
        if (objCount>0){
            my3dObjCount=objCount;

            my3dObjVertices = new FloatBuffer[my3dObjCount];
            my3dObjTextures = new FloatBuffer[my3dObjCount];
            //my3dObjNormals = new FloatBuffer[my3dObjCount];
            my3dObjProgram = new int[my3dObjCount];
            my3dObjPositionParam = new int[my3dObjCount];
            my3dObjNormalParam = new int[my3dObjCount];
            my3dObjTextureParam = new int[my3dObjCount];
            my3dObjModelParam = new int[my3dObjCount];
            my3dObjModelViewParam = new int[my3dObjCount];
            my3dObjModelViewProjectionParam = new int[my3dObjCount];
            my3dObjLightPosParam = new int[my3dObjCount];
            my3dObjTextureUniformHandlerParam = new int[my3dObjCount];
            modelMy3dObj = new float[my3dObjCount][];
        }

        //modelCube = new float[16];
        camera = new float[16];
        view = new float[16];
        modelViewProjection = new float[16];
        modelView = new float[16];
        modelFloor = new float[16];
        headView = new float[16];
        for (int i=0;i<my3dObjCount;i++) {
            modelMy3dObj[i] = new float[16];
        }
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        mDeviceManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        //activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        mCompNameAdmin = new ComponentName(this, MyAdmin.class);
        if (!mDeviceManager.isAdminActive(mCompNameAdmin)) {

            Intent intent = new Intent(DevicePolicyManager
                    .ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                    mCompNameAdmin);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    "Grant this app for saving power by lock screen");
            startActivityForResult(intent, RESULT_ENABLE);
        }
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), 40000);
        mScreenLock = ((PowerManager)getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP
                | PowerManager.ON_AFTER_RELEASE, "ScreenOnLock");

        overlayView = (CardboardOverlayView) findViewById(R.id.overlay);
        overlayView.show3DToast("Pull the magnet to switch objects");
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case RESULT_ENABLE:
                if (resultCode == Activity.RESULT_OK) {
                    Log.i("DeviceAdminSample", "Admin enabled!");
                } else {
                    Log.i("DeviceAdminSample", "Admin enable FAILED!");
                }
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onRendererShutdown() {
        Log.i(TAG, "onRendererShutdown");
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        Log.i(TAG, "onSurfaceChanged");
    }

    /**
     * Creates the buffers we use to store information about the 3D world.
     *
     * <p>OpenGL doesn't use Java arrays, but rather needs data in a format it can understand.
     * Hence we use ByteBuffers.
     *
     * @param config The EGL configuration used when creating the surface.
     */
    @Override
    public void onSurfaceCreated(EGLConfig config) {
        Log.i(TAG, "onSurfaceCreated");

        objUtil.loadAllTextures();

        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 0.5f); // Dark background so text shows up well.

        /*ByteBuffer bbVertices = ByteBuffer.allocateDirect(WorldLayoutData.CUBE_COORDS.length * 4);
        bbVertices.order(ByteOrder.nativeOrder());
        cubeVertices = bbVertices.asFloatBuffer();
        cubeVertices.put(WorldLayoutData.CUBE_COORDS);
        cubeVertices.position(0);

        ByteBuffer bbColors = ByteBuffer.allocateDirect(WorldLayoutData.CUBE_COLORS.length * 4);
        bbColors.order(ByteOrder.nativeOrder());
        cubeColors = bbColors.asFloatBuffer();
        cubeColors.put(WorldLayoutData.CUBE_COLORS);
        cubeColors.position(0);

        ByteBuffer bbFoundColors = ByteBuffer.allocateDirect(
                WorldLayoutData.CUBE_FOUND_COLORS.length * 4);
        bbFoundColors.order(ByteOrder.nativeOrder());
        cubeFoundColors = bbFoundColors.asFloatBuffer();
        cubeFoundColors.put(WorldLayoutData.CUBE_FOUND_COLORS);
        cubeFoundColors.position(0);

        ByteBuffer bbNormals = ByteBuffer.allocateDirect(WorldLayoutData.CUBE_NORMALS.length * 4);
        bbNormals.order(ByteOrder.nativeOrder());
        cubeNormals = bbNormals.asFloatBuffer();
        cubeNormals.put(WorldLayoutData.CUBE_NORMALS);
        cubeNormals.position(0);*/

        // make a floor
        ByteBuffer bbFloorVertices = ByteBuffer.allocateDirect(WorldLayoutData.FLOOR_COORDS.length * 4);
        bbFloorVertices.order(ByteOrder.nativeOrder());
        floorVertices = bbFloorVertices.asFloatBuffer();
        floorVertices.put(WorldLayoutData.FLOOR_COORDS);
        floorVertices.position(0);

        ByteBuffer bbFloorNormals = ByteBuffer.allocateDirect(WorldLayoutData.FLOOR_NORMALS.length * 4);
        bbFloorNormals.order(ByteOrder.nativeOrder());
        floorNormals = bbFloorNormals.asFloatBuffer();
        floorNormals.put(WorldLayoutData.FLOOR_NORMALS);
        floorNormals.position(0);

        ByteBuffer bbFloorColors = ByteBuffer.allocateDirect(WorldLayoutData.FLOOR_COLORS.length * 4);
        bbFloorColors.order(ByteOrder.nativeOrder());
        floorColors = bbFloorColors.asFloatBuffer();
        floorColors.put(WorldLayoutData.FLOOR_COLORS);
        floorColors.position(0);

        //make my own 3d objs
        for (int i=0;i<my3dObjCount;i++) {
            ByteBuffer bbMy3dObjVertices = ByteBuffer.allocateDirect(objUtil.objVert[i].length * 4);
            bbMy3dObjVertices.order(ByteOrder.nativeOrder());
            my3dObjVertices[i] = bbMy3dObjVertices.asFloatBuffer();
            my3dObjVertices[i].put(objUtil.objVert[i]);
            my3dObjVertices[i].position(0);

            ByteBuffer bbMy3dObjTextures = ByteBuffer.allocateDirect(objUtil.objText[i].length * 4);
            bbMy3dObjTextures.order(ByteOrder.nativeOrder());
            my3dObjTextures[i] = bbMy3dObjTextures.asFloatBuffer();
            my3dObjTextures[i].put(objUtil.objText[i]);
            my3dObjTextures[i].position(0);

            /*ByteBuffer bbMy3dObjNormals = ByteBuffer.allocateDirect(objUtil.bananaNorm.length * 4);
            bbMy3dObjNormals.order(ByteOrder.nativeOrder());
            my3dObjNormals[i] = bbMy3dObjNormals.asFloatBuffer();
            my3dObjNormals[i].put(objUtil.bananaNorm);
            my3dObjNormals[i].position(0);*/
        }

        int vertexShader = loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.light_vertex);
        int vertexTextureShader = loadGLShader(GLES20.GL_VERTEX_SHADER, R.raw.light_vertex_texture);
        int gridShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.grid_fragment);
        int passthroughShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.passthrough_fragment);
        int passthroughTextureShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, R.raw.passthrough_fragment_texture);

        /*cubeProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(cubeProgram, vertexShader);
        GLES20.glAttachShader(cubeProgram, passthroughShader);
        GLES20.glLinkProgram(cubeProgram);
        GLES20.glUseProgram(cubeProgram);

        checkGLError("Cube program");

        cubePositionParam = GLES20.glGetAttribLocation(cubeProgram, "a_Position");
        cubeNormalParam = GLES20.glGetAttribLocation(cubeProgram, "a_Normal");
        cubeColorParam = GLES20.glGetAttribLocation(cubeProgram, "a_Color");

        cubeModelParam = GLES20.glGetUniformLocation(cubeProgram, "u_Model");
        cubeModelViewParam = GLES20.glGetUniformLocation(cubeProgram, "u_MVMatrix");
        cubeModelViewProjectionParam = GLES20.glGetUniformLocation(cubeProgram, "u_MVP");
        cubeLightPosParam = GLES20.glGetUniformLocation(cubeProgram, "u_LightPos");

        GLES20.glEnableVertexAttribArray(cubePositionParam);
        GLES20.glEnableVertexAttribArray(cubeNormalParam);
        GLES20.glEnableVertexAttribArray(cubeColorParam);

        checkGLError("Cube program params");*/

        floorProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(floorProgram, vertexShader);
        GLES20.glAttachShader(floorProgram, gridShader);
        GLES20.glLinkProgram(floorProgram);
        GLES20.glUseProgram(floorProgram);

        checkGLError("Floor program");

        floorModelParam = GLES20.glGetUniformLocation(floorProgram, "u_Model");
        floorModelViewParam = GLES20.glGetUniformLocation(floorProgram, "u_MVMatrix");
        floorModelViewProjectionParam = GLES20.glGetUniformLocation(floorProgram, "u_MVP");
        floorLightPosParam = GLES20.glGetUniformLocation(floorProgram, "u_LightPos");

        floorPositionParam = GLES20.glGetAttribLocation(floorProgram, "a_Position");
        floorNormalParam = GLES20.glGetAttribLocation(floorProgram, "a_Normal");
        floorColorParam = GLES20.glGetAttribLocation(floorProgram, "a_Color");

        GLES20.glEnableVertexAttribArray(floorPositionParam);
        GLES20.glEnableVertexAttribArray(floorNormalParam);
        GLES20.glEnableVertexAttribArray(floorColorParam);

        checkGLError("Floor program params");


        for (int i=0;i<my3dObjCount;i++) {
            my3dObjProgram[i] = GLES20.glCreateProgram();
            GLES20.glAttachShader(my3dObjProgram[i], vertexTextureShader);
            GLES20.glAttachShader(my3dObjProgram[i], passthroughTextureShader);
            GLES20.glLinkProgram(my3dObjProgram[i]);
            GLES20.glUseProgram(my3dObjProgram[i]);

            checkGLError("my3dObj "+i+" program");

            my3dObjPositionParam[i] = GLES20.glGetAttribLocation(my3dObjProgram[i], "a_Position");
            my3dObjNormalParam[i] = GLES20.glGetAttribLocation(my3dObjProgram[i], "a_Normal");
            my3dObjTextureParam[i] = GLES20.glGetAttribLocation(my3dObjProgram[i], "a_TexCoordinate");

            my3dObjModelParam[i] = GLES20.glGetUniformLocation(my3dObjProgram[i], "u_Model");
            my3dObjModelViewParam[i] = GLES20.glGetUniformLocation(my3dObjProgram[i], "u_MVMatrix");
            my3dObjModelViewProjectionParam[i] = GLES20.glGetUniformLocation(my3dObjProgram[i], "u_MVP");
            my3dObjLightPosParam[i] = GLES20.glGetUniformLocation(my3dObjProgram[i], "u_LightPos");
            my3dObjTextureUniformHandlerParam[i] = GLES20.glGetUniformLocation(my3dObjProgram[i], "u_Texture");

            GLES20.glEnableVertexAttribArray(my3dObjPositionParam[i]);
            //GLES20.glEnableVertexAttribArray(my3dObjNormalParam[i]);
            GLES20.glEnableVertexAttribArray(my3dObjTextureParam[i]);

            checkGLError("my3dObj "+i+" program params");
        }

        // Object first appears directly in front of user.
        //Matrix.setIdentityM(modelCube, 0);
        //Matrix.translateM(modelCube, 0, 0, 0, -objectDistance);

        Matrix.setIdentityM(modelFloor, 0);
        Matrix.translateM(modelFloor, 0, 0, -floorDepth, 0); // Floor appears below user.

        for (int i=0;i<my3dObjCount;i++) {
            // Object my objects appears directly in front of user.
            Matrix.setIdentityM(modelMy3dObj[i], 0);
            //Matrix.translateM(modelMy3dObj[i], 0, (float)(4*Math.cos(i*2*Math.PI/my3dObjCount)), (float)(4*Math.sin(i*2*Math.PI/my3dObjCount)), -objectDistance);
            Matrix.translateM(modelMy3dObj[i], 0, objUtil.objPosition[i][0], objUtil.objPosition[i][1], -objUtil.objPosition[i][2]);
        }

        checkGLError("onSurfaceCreated");
    }

    /**
     * Converts a raw text file into a string.
     *
     * @param resId The resource ID of the raw text file about to be turned into a shader.
     * @return The context of the text file, or null in case of error.
     */
    private String readRawTextFile(int resId) {
        InputStream inputStream = getResources().openRawResource(resId);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Prepares OpenGL ES before we draw a frame.
     *
     * @param headTransform The head transformation in the new frame.
     */
    @Override
    public void onNewFrame(HeadTransform headTransform) {
        long frameEndTime = System.currentTimeMillis();
        int deltaTime = (int)(frameEndTime - mFrameStartTime);
        if (deltaTime < 500 && !mAwake) {
            try {
                Thread.sleep(500 - deltaTime);
            } catch (InterruptedException e) {
                //whatever
            }
        }
        mFrameStartTime = frameEndTime;

        // Build the Model part of the ModelView matrix.
        //Matrix.rotateM(modelCube, 0, TIME_DELTA, 0.5f, 0.5f, 1.0f);

        //!!!!!!
        //for (int i=0;i<my3dObjCount;i++) Matrix.rotateM(modelMy3dObj[i], 0, TIME_DELTA, 0.5f, 0.5f, 1.0f);

        // Build the camera matrix and apply it to the ModelView.
        Matrix.setLookAtM(camera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);

        headTransform.getHeadView(headView, 0);

        checkGLError("onReadyToDraw");
    }

    /**
     * Draws a frame for an eye.
     *
     * @param eye The eye to render. Includes all required transformations.
     */
    @Override
    public void onDrawEye(Eye eye) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        checkGLError("colorParam");

        // Apply the eye transformation to the camera.
        Matrix.multiplyMM(view, 0, eye.getEyeView(), 0, camera, 0);

        // Set the position of the light
        Matrix.multiplyMV(lightPosInEyeSpace, 0, view, 0, LIGHT_POS_IN_WORLD_SPACE, 0);

        // Build the ModelView and ModelViewProjection matrices
        // for calculating cube position and light.
        float[] perspective = eye.getPerspective(Z_NEAR, Z_FAR);
        //Matrix.multiplyMM(modelView, 0, view, 0, modelCube, 0);
        //Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
        //drawCube();

        // Set modelView for the floor, so we draw floor in the correct location
        Matrix.multiplyMM(modelView, 0, view, 0, modelFloor, 0);
        Matrix.multiplyMM(modelViewProjection, 0, perspective, 0,
                modelView, 0);
        drawFloor();

        for (int i=0;i<my3dObjCount;i++) {
            Matrix.multiplyMM(modelView, 0, view, 0, modelMy3dObj[i], 0);
            Matrix.multiplyMM(modelViewProjection, 0, perspective, 0, modelView, 0);
            if (i==displayIndex){
                drawMy3dObj(i);
            }
        }
        //Log.i(TAG,"Draweye");//looks still drawing~~~~
        //need to cap it when screen is down
        //http://stackoverflow.com/questions/4772693/how-to-limit-framerate-when-using-androids-glsurfaceview-rendermode-continuousl
    }

    @Override
    public void onFinishFrame(Viewport viewport) {
    }

    /**
     * Draw the cube.
     *
     * <p>We've set all of our transformation matrices. Now we simply pass them into the shader.
     */
    /*public void drawCube() {
        GLES20.glUseProgram(cubeProgram);

        GLES20.glUniform3fv(cubeLightPosParam, 1, lightPosInEyeSpace, 0);

        // Set the Model in the shader, used to calculate lighting
        GLES20.glUniformMatrix4fv(cubeModelParam, 1, false, modelCube, 0);

        // Set the ModelView in the shader, used to calculate lighting
        GLES20.glUniformMatrix4fv(cubeModelViewParam, 1, false, modelView, 0);

        // Set the position of the cube
        GLES20.glVertexAttribPointer(cubePositionParam, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                false, 0, cubeVertices);

        // Set the ModelViewProjection matrix in the shader.
        GLES20.glUniformMatrix4fv(cubeModelViewProjectionParam, 1, false, modelViewProjection, 0);

        // Set the normal positions of the cube, again for shading
        GLES20.glVertexAttribPointer(cubeNormalParam, 3, GLES20.GL_FLOAT, false, 0, cubeNormals);
        GLES20.glVertexAttribPointer(cubeColorParam, 4, GLES20.GL_FLOAT, false, 0,
                isLookingAtObject() ? cubeFoundColors : cubeColors);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 36);
        checkGLError("Drawing cube");
    }*/

    /**
     * Draw the floor.
     *
     * <p>This feeds in data for the floor into the shader. Note that this doesn't feed in data about
     * position of the light, so if we rewrite our code to draw the floor first, the lighting might
     * look strange.
     */
    public void drawFloor() {
        GLES20.glUseProgram(floorProgram);

        // Set ModelView, MVP, position, normals, and color.
        GLES20.glUniform3fv(floorLightPosParam, 1, lightPosInEyeSpace, 0);
        GLES20.glUniformMatrix4fv(floorModelParam, 1, false, modelFloor, 0);
        GLES20.glUniformMatrix4fv(floorModelViewParam, 1, false, modelView, 0);
        GLES20.glUniformMatrix4fv(floorModelViewProjectionParam, 1, false,
                modelViewProjection, 0);
        GLES20.glVertexAttribPointer(floorPositionParam, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                false, 0, floorVertices);
        GLES20.glVertexAttribPointer(floorNormalParam, 3, GLES20.GL_FLOAT, false, 0,
                floorNormals);
        GLES20.glVertexAttribPointer(floorColorParam, 4, GLES20.GL_FLOAT, false, 0, floorColors);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, 6);

        checkGLError("drawing floor");
    }

    //modified from drawCube
    public void drawMy3dObj(int index) {
        GLES20.glUseProgram(my3dObjProgram[index]);

        GLES20.glUniform3fv(my3dObjLightPosParam[index], 1, lightPosInEyeSpace, 0);

        // Set the Model in the shader, used to calculate lighting
        GLES20.glUniformMatrix4fv(my3dObjModelParam[index], 1, false, modelMy3dObj[index], 0);

        // Set the ModelView in the shader, used to calculate lighting
        GLES20.glUniformMatrix4fv(my3dObjModelViewParam[index], 1, false, modelView, 0);

        // Set the position of the cube
        GLES20.glVertexAttribPointer(my3dObjPositionParam[index], COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, 0, my3dObjVertices[index]);

        // Set the ModelViewProjection matrix in the shader.
        GLES20.glUniformMatrix4fv(my3dObjModelViewProjectionParam[index], 1, false, modelViewProjection, 0);



        // Set the normal positions of the cube, again for shading
        //GLES20.glVertexAttribPointer(my3dObjNormalParam[index], 3, GLES20.GL_FLOAT, false, 0, my3dObjNormals[index]);
        GLES20.glVertexAttribPointer(my3dObjTextureParam[index], 2, GLES20.GL_FLOAT, false, 0, my3dObjTextures[index]);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, objUtil.objTextFile[index]);
        GLES20.glUniform1i(my3dObjTextureUniformHandlerParam[index], 0);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, objUtil.objVertices[index]);
        checkGLError("Drawing "+index+" cube");
    }

    /**
     * Called when the Cardboard trigger is pulled.
     */
    @Override
    public void onCardboardTrigger() {
        Log.i(TAG, "onCardboardTrigger");

        /*if (isLookingAtObject()) {
            score++;
            overlayView.show3DToast("Found it! Look around for another one.\nScore = " + score);
            hideObject();
        } else {
            overlayView.show3DToast("Look around to find the object!");
        }*/
        displayIndex++;
        if (displayIndex>=my3dObjCount) displayIndex=0;

        // Always give user feedback.
        vibrator.vibrate(50);

        wakeUpFunction();
    }

    /**
     * Find a new random position for the object.
     *
     * <p>We'll rotate it around the Y-axis so it's out of sight, and then up or down by a little bit.
     */
    /*private void hideObject() {
        float[] rotationMatrix = new float[16];
        float[] posVec = new float[4];

        // First rotate in XZ plane, between 90 and 270 deg away, and scale so that we vary
        // the object's distance from the user.
        float angleXZ = (float) Math.random() * 180 + 90;
        Matrix.setRotateM(rotationMatrix, 0, angleXZ, 0f, 1f, 0f);
        float oldObjectDistance = objectDistance;
        objectDistance = (float) Math.random() * 15 + 5;
        float objectScalingFactor = objectDistance / oldObjectDistance;
        Matrix.scaleM(rotationMatrix, 0, objectScalingFactor, objectScalingFactor,
                objectScalingFactor);
        Matrix.multiplyMV(posVec, 0, rotationMatrix, 0, modelCube, 12);

        // Now get the up or down angle, between -20 and 20 degrees.
        float angleY = (float) Math.random() * 80 - 40; // Angle in Y plane, between -40 and 40.
        angleY = (float) Math.toRadians(angleY);
        float newY = (float) Math.tan(angleY) * objectDistance;

        Matrix.setIdentityM(modelCube, 0);
        Matrix.translateM(modelCube, 0, posVec[0], newY, posVec[2]);
    }*

    /**
     * Check if user is looking at object by calculating where the object is in eye-space.
     *
     * @return true if the user is looking at the object.
     */
    /*private boolean isLookingAtObject() {
        float[] initVec = { 0, 0, 0, 1.0f };
        float[] objPositionVec = new float[4];

        // Convert object space to camera space. Use the headView from onNewFrame.
        Matrix.multiplyMM(modelView, 0, headView, 0, modelCube, 0);
        Matrix.multiplyMV(objPositionVec, 0, modelView, 0, initVec, 0);

        float pitch = (float) Math.atan2(objPositionVec[1], -objPositionVec[2]);
        float yaw = (float) Math.atan2(objPositionVec[0], -objPositionVec[2]);

        return Math.abs(pitch) < PITCH_LIMIT && Math.abs(yaw) < YAW_LIMIT;
    }*/


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Do nothing
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        //Log.i("TAG",String.format("%2.2f   %2.2f   %2.2f",event.values[0],event.values[1],event.values[2]));

        float threshold = 0.1f;
        long currentTime = System.currentTimeMillis();
        if (Math.abs(event.values[0])<threshold && Math.abs(event.values[1])<threshold && Math.abs(event.values[2])<threshold){
            if (mAwake){
                if ((currentTime-mLastMovementTime)>mWakeUpTime){
                    mAwake=false;   //sleep
                    Log.i(TAG,"Sleep no movement");
                    //lock device
                    boolean active = mDeviceManager.isAdminActive(mCompNameAdmin);

                    if (active) {
                        mDeviceManager.lockNow();
                    }else{
                        Log.i("TAG","No Admin access to lock screen");
                    }
                }
            }else{
                //do nothing, Keep sleeping
            }
        }else{
            if (mAwake){
                //do nothing
            }else{
                Log.i(TAG,"Wake UP due to movement!");
                wakeUpFunction();
            }
        }
    }

    private void wakeUpFunction(){
        mAwake=true;    //wake up!!
        mLastMovementTime=System.currentTimeMillis();;
        mScreenLock.acquire();
        mScreenLock.release();
    }

}
