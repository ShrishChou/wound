/*===============================================================================
Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of QUALCOMM Incorporated, registered in the United States 
and other countries. Trademarks of QUALCOMM Incorporated are used with permission.
===============================================================================*/

/*
 * Main class that deals with the rendering of the object.
 */

package com.mobiletechnologylab.wound_imager.image;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.mobiletechnologylab.wound_imager.ApplicationSession;
import com.mobiletechnologylab.wound_imager.utils.Arrow;
import com.mobiletechnologylab.wound_imager.utils.CubeShaders;
import com.mobiletechnologylab.wound_imager.utils.LineShaders;
import com.mobiletechnologylab.wound_imager.utils.LoadingDialogHandler;
import com.mobiletechnologylab.wound_imager.utils.SampleUtils;
import com.mobiletechnologylab.wound_imager.utils.Texture;
import com.qualcomm.vuforia.CameraCalibration;
import com.qualcomm.vuforia.CameraDevice;
import com.qualcomm.vuforia.Frame;
import com.qualcomm.vuforia.Image;
import com.qualcomm.vuforia.Matrix34F;
import com.qualcomm.vuforia.Matrix44F;
import com.qualcomm.vuforia.PIXEL_FORMAT;
import com.qualcomm.vuforia.Rectangle;
import com.qualcomm.vuforia.Renderer;
import com.qualcomm.vuforia.State;
import com.qualcomm.vuforia.Tool;
import com.qualcomm.vuforia.VIDEO_BACKGROUND_REFLECTION;
import com.qualcomm.vuforia.Vec3F;
import com.qualcomm.vuforia.VideoBackgroundConfig;
import com.qualcomm.vuforia.Vuforia;

import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Vector;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
//import org.apache.commons.math3.stat.regression.SimpleRegression;

// The renderer class for the ImageTargets sample. 
public class ImageTargetRenderer implements GLSurfaceView.Renderer {

    // Enable calibration
    private static boolean CALIBRATE_FLAG = false;

    // Position variable right
    private static float SEARCH_LIMIT_RIGHT = 2.6f;
    private static float SEARCH_LIMIT_LEFT = -1.34f;

    // General variables
    private static final String LOGTAG = "ImageTargetRenderer";
    private ApplicationSession vuforiaAppSession;
    private ImageTargets mActivity;
    private Vector<Texture> mTextures;
    private int numVertices = 180;

    private int shaderProgramID;
    private int vertexHandle;
    private int normalHandle;
    private int textureCoordHandle;
    private int mvpMatrixHandle;
    private int texSampler2DHandle;

    // Model variables
    private Arrow mArrow;
    private Renderer mRenderer;
    boolean mIsActive = false;

    private Square indicatorSearchSpace;
    // Signal processing variables
    Handler activityHandler = new Handler(Looper.getMainLooper());
    CameraCalibration cameraCalib;
    private Vec3F[][] pixelLevelLocations;
    private double[] colorSpectrum;
    private double colorCount;
    private int[][] colorPixels;
    private Bitmap cameraBitmap;
    private Vec3F measureLoc;

    private long unmatched = 0;

    // Open GL magic
    private int vbShaderProgramID = 0;
    private int vbVertexHandle = 0;
    private int colorVertexHandle = 0;
    private int lineOpacityHandle = 0;
    private int lineColorHandle = 0;
    private int mvpMatrixButtonsHandle = 0;
    private LinkedList<Double> last10Obs;


    float[] tempvec = new float[]{0f, 0f, 0f};
    float chi = 0f;
    float phi = 0f;
    float maxZ = 0f;

    private float RedRight = 11f;
    private float GreenLeft = 21f;
    private boolean found = false;

    private Rectangle[] targetRectangle;
    private Rectangle[] woundImageRectangle;

    private Rectangle[] colorChartTopLeftRectangle;
    private Rectangle[] colorChartTopRightRectangle;
    private Rectangle[] colorChartBottomLeftRectangle;
    private Rectangle[] colorChartBottomRightRectangle;

    float[][] corners = new float[][]{
            new float[2],
            new float[2]
    };
    float skewx, skewy;

    int calibrationOutputCount;

    public ImageTargetRenderer(ImageTargets activity, ApplicationSession session) {
        mActivity = activity;
        vuforiaAppSession = session;

        updateOffsets();

        calibrationOutputCount = 0;
    }

    // rawOffsets values were set using positionValues_2016-11-04.xlsx to reflect the new target
    // If these offsets need to be set again, enable calibration mode using the boolean flag at the
    // top of this file. Then, set the marker to 60 and aim the phone at the device. It will take 10
    // measurements over 10 seconds and write them to a csv file. You will know it is done when it
    // says Measurement = -999. Click Continue, set the marker to 100, click Retest and repeat. Do
    // this for each of the values defined in positionValues_2016-11-04.xlsx. Once you have done it
    // for all the values, exit the app and find the file positionValues.csv stored in the main
    // directory on the phone. Copy and paste these values into positonValues_2016-11-04.xlsx and
    // use the Average column to update the rawOffsets.

//	private static final float[] rawOffsets = new float[]{
//			0.012398617f, 0.100845608f, 0.191947059f, 0.271009746f, 0.346482197f, 0.411437149f,
//			0.477823572f, 0.54402727f, 0.600059798f, 0.653406896f, 0.709095296f, 0.754436074f,
//			0.79720104f, 0.844411631f, 0.915036778f, 0.954574747f
//	};

    private static final float[] rawOffsets = new float[]{
            0.015364804f, 0.102554857f, 0.180413978f, 0.271232971f, 0.342453904f, 0.410560047f,
            0.471284033f,
            0.539975316f, 0.583380092f, 0.644392855f, 0.689587093f, 0.748611874f, 0.787475188f,
            0.841653522f,
            0.911462314f, 0.937526268f
    };


    // offsets will be defined as rawOffsets + linearOffset
    private float[] offsets;

    // Value defining horizontal offset of sticker from original calibration
    // Can be modified later with a menu to allow calibration from the app
    private static final float linearOffset = 0.0f;

    // Peak flow meter readings corresponding to each of the rawOffset values
    private static final int[] values = new int[]{
            60, 100, 150, 200, 250, 300, 350, 400,
            450, 500, 550, 600, 650, 700, 800, 840
    };

    private static final int[] positions = new int[]{
            -14, 90, 190, 298, 385, 470, 550, 624,
            700, 773, 850, 915, 975, 1030, 1145, 1190
    };

    private float history[] = new float[]{
            0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f
    };
    private int historyPos = 0;
    private float lastMeasurement = 0;

    /**
     * Initialize rendering - set up and initialize variables that will be displayed.
     */
    private void initRendering() {
        mArrow = new Arrow();
        mRenderer = Renderer.getInstance();
        indicatorSearchSpace = null;

        chi = -20;

//		CameraDevice.getInstance().setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_MACRO);
        cameraCalib = CameraDevice.getInstance().getCameraCalibration();
        float[] resolution = cameraCalib.getSize().getData();
        cameraBitmap = Bitmap
                .createBitmap((int) resolution[0], (int) resolution[1], Bitmap.Config.RGB_565);

        colorSpectrum = new double[]{4, 6, 8, 10, 12, 14};
        last10Obs = new LinkedList<Double>();

        // NOTE: (0,0) is the center of the target
        // multiple sampling inside
        // Rectangles defining the target and search line. These values were set through trial and error by looking at app results.
        targetRectangle = new Rectangle[1];
        targetRectangle[0] = new Rectangle(-1.42f, 1.05f, 1.42f, -1.05f); // draws the bounding box around target border

        woundImageRectangle = new Rectangle[1];
        woundImageRectangle[0] = new Rectangle(-1.22f, 0.32f, 1.22f, -0.32f); // draws the bounding box for wound image

        colorChartTopLeftRectangle = new Rectangle[1];
        colorChartTopLeftRectangle[0] = new Rectangle(-0.58f, 0.65f, -0.02f, 0.35f); // draws the bounding box for color chart

        colorChartTopRightRectangle = new Rectangle[1];
        colorChartTopRightRectangle[0] = new Rectangle(0.02f, 0.65f, 0.59f, 0.35f); // draws the bounding box for color chart

        colorChartBottomLeftRectangle = new Rectangle[1];
        colorChartBottomLeftRectangle[0] = new Rectangle(-0.58f, -0.39f, -0.02f, -0.7f); // draws the bounding box for color chart

        colorChartBottomRightRectangle = new Rectangle[1];
        colorChartBottomRightRectangle[0] = new Rectangle(0.01f, -0.39f, 0.58f, -0.7f); // draws the bounding box for color chart


        // Initialize Arrow rendering
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f : 1.0f);
        for (Texture t : mTextures) {
            GLES20.glGenTextures(1, t.mTextureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                    t.mWidth, t.mHeight, 0, GLES20.GL_RGBA,
                    GLES20.GL_UNSIGNED_BYTE, t.mData);
        }
        shaderProgramID = SampleUtils.createProgramFromShaderSrc(
                CubeShaders.CUBE_MESH_VERTEX_SHADER,
                CubeShaders.CUBE_MESH_FRAGMENT_SHADER);

        vertexHandle = GLES20.glGetAttribLocation(shaderProgramID, "vertexPosition");
        normalHandle = GLES20.glGetAttribLocation(shaderProgramID, "vertexNormal");
        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID, "vertexTexCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID, "modelViewProjectionMatrix");
        texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID, "texSampler2D");

        vbShaderProgramID = SampleUtils.createProgramFromShaderSrc(
                LineShaders.LINE_VERTEX_SHADER,
                LineShaders.LINE_FRAGMENT_SHADER);
        vbVertexHandle = GLES20.glGetAttribLocation(vbShaderProgramID, "vertexPosition");
        mvpMatrixButtonsHandle = GLES20
                .glGetUniformLocation(vbShaderProgramID, "modelViewProjectionMatrix");
        lineOpacityHandle = GLES20.glGetUniformLocation(vbShaderProgramID, "opacity");
        lineColorHandle = GLES20.glGetUniformLocation(vbShaderProgramID, "color");

        mActivity.loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);
    }

    // The render function.
    private void renderFrame() {

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        State state = mRenderer.begin();
        mRenderer.drawVideoBackground();

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        if (Renderer.getInstance().getVideoBackgroundConfig().getReflection()
                == VIDEO_BACKGROUND_REFLECTION.VIDEO_BACKGROUND_REFLECTION_ON) {
            GLES20.glFrontFace(GLES20.GL_CW); // Front camera
        } else {
            GLES20.glFrontFace(GLES20.GL_CCW); // Back camera
        }

        if (state.getNumTrackableResults() == 0) {
            activityHandler.post(new Runnable() {
                public void run() {
                    mActivity.setPhi(100);
                }
            });
            activityHandler.post(new Runnable() {
                public void run() {
                    mActivity.hideCapture();
                }
            });
        }

        //if(state.getNumTrackableResults() > 0) android.util.Log.v(LOGTAG, "Found "+state.getNumTrackableResults()+" tracking points");
        for (int tIdx = 0; tIdx < state.getNumTrackableResults(); tIdx++) {

            Matrix34F pose = state.getTrackableResult(0).getPose();

            /**
             * Calculate error angle phi between current pose and orthonormal & display
             */
            float[] poseRotation = getRotationMatrix(pose);
            float[] targetRotation = {1.0f, 0f, 0f, 0f,   0f, -1.0f, 0f, 0f,   0f, 0f, -1.0f, 0f,    0f, 0f, 0f, 0f};
            float[] resultMatrix = new float[16];
            Matrix.multiplyMM(resultMatrix, 0, poseRotation, 0, targetRotation, 0);
            float[] d = new float[3];
            d[0] = resultMatrix[6] - resultMatrix[9];
            d[1] = resultMatrix[8] - resultMatrix[2];
            d[2] = resultMatrix[1] - resultMatrix[4];
            double dmag = Math.sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2]);
            double phi = Math.asin(dmag/2);

            activityHandler.post(new Runnable() {
                public void run() {
                    mActivity.setPhi(phi);
                }
            });


            Matrix44F modelViewMatrix_Vuforia = Tool.convertPose2GLMatrix(pose);

            float[] modelViewMatrixTarget = modelViewMatrix_Vuforia.getData();
            float[] modelViewMatrixWound = modelViewMatrix_Vuforia.getData();
            float[] modelViewMatrixColorChartTopLeft = modelViewMatrix_Vuforia.getData();
            float[] modelViewMatrixColorChartTopRight = modelViewMatrix_Vuforia.getData();
            float[] modelViewMatrixColorChartBottomLeft = modelViewMatrix_Vuforia.getData();
            float[] modelViewMatrixColorChartBottomRight = modelViewMatrix_Vuforia.getData();


            float[] targetVertices = targetGLVertices(); // Green box around target
            float[] woundImageVertices = woundGLVertices(); // Blue box around wound image
            float[] colorChartTopLeftVertices = colorChartTopLeftGLVertices(); // Blue box around TL color chart
            float[] colorChartTopRightVertices = colorChartTopRightGLVertices(); // Blue box around TR color chart
            float[] colorChartBottomLeftVertices = colorChartBottomLeftGLVertices(); // Blue box around BL color chart
            float[] colorChartBottomRightVertices = colorChartBottomRightGLVertices(); // Blue box around BR color chart


            float[] modelViewProjectionTarget = new float[16];
            float[] modelViewProjectionWound = new float[16];
            float[] modelViewProjectionColorChartTopLeft = new float[16];
            float[] modelViewProjectionColorChartTopRight = new float[16];
            float[] modelViewProjectionColorChartBottomLeft = new float[16];
            float[] modelViewProjectionColorChartBottomRight = new float[16];


            /**
             * AR rendering
             */
            //Draws red, yellow, or green box around target (depending on error angle phi)
            Matrix.scaleM(modelViewMatrixTarget, 0, 35, 35, 35);
            Matrix.multiplyMM(modelViewProjectionTarget, 0,
                    vuforiaAppSession.getProjectionMatrix().getData(), 0, modelViewMatrixTarget, 0);
            GLES20.glUseProgram(vbShaderProgramID);
            GLES20.glVertexAttribPointer(vbVertexHandle, 3, GLES20.GL_FLOAT, false, 0,
                    fillBuffer(targetVertices));
            GLES20.glEnableVertexAttribArray(vbVertexHandle);
            GLES20.glUniform1f(lineOpacityHandle, 1.0f);
            if (phi > 0.5d)
                GLES20.glUniform3f(lineColorHandle, 1.0f, 0.0f, 0.0f);
//            else if (phi > 0.1d)
//                GLES20.glUniform3f(lineColorHandle, 1.0f, 1.0f, 0.0f);
            else
                GLES20.glUniform3f(lineColorHandle, 0.0f, 1.0f, 0.0f);
            GLES20.glUniformMatrix4fv(mvpMatrixButtonsHandle, 1, false, modelViewProjectionTarget,
                    0);
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, 8 * targetRectangle.length);

            //Draws blue box around wound image
            Matrix.scaleM(modelViewMatrixWound, 0, 35, 35, 35);
            Matrix.multiplyMM(modelViewProjectionWound, 0,
                    vuforiaAppSession.getProjectionMatrix().getData(), 0, modelViewMatrixWound, 0);
            GLES20.glUseProgram(vbShaderProgramID);
            GLES20.glVertexAttribPointer(vbVertexHandle, 3, GLES20.GL_FLOAT, false, 0,
                    fillBuffer(woundImageVertices));
            GLES20.glEnableVertexAttribArray(vbVertexHandle);
            GLES20.glUniform1f(lineOpacityHandle, 1.0f);
            GLES20.glUniform3f(lineColorHandle, 0.0f, 0.0f, 1.0f);
            GLES20.glUniformMatrix4fv(mvpMatrixButtonsHandle, 1, false, modelViewProjectionWound,
                    0);
            GLES20.glDrawArrays(GLES20.GL_LINES, 0, 8 * woundImageRectangle.length);

//            //Draws blue box around top left color chart
//            Matrix.scaleM(modelViewMatrixColorChartTopLeft, 0, 35, 35, 35);
//            Matrix.multiplyMM(modelViewProjectionColorChartTopLeft, 0,
//                    vuforiaAppSession.getProjectionMatrix().getData(), 0, modelViewMatrixColorChartTopLeft, 0);
//            GLES20.glUseProgram(vbShaderProgramID);
//            GLES20.glVertexAttribPointer(vbVertexHandle, 3, GLES20.GL_FLOAT, false, 0,
//                    fillBuffer(colorChartTopLeftVertices));
//            GLES20.glEnableVertexAttribArray(vbVertexHandle);
//            GLES20.glUniform1f(lineOpacityHandle, 1.0f);
//            GLES20.glUniform3f(lineColorHandle, 0.0f, 0.0f, 1.0f);
//            GLES20.glUniformMatrix4fv(mvpMatrixButtonsHandle, 1, false, modelViewProjectionColorChartTopLeft,
//                    0);
//            GLES20.glDrawArrays(GLES20.GL_LINES, 0, 8 * colorChartTopLeftRectangle.length);
//
//            //Draws blue box around top right color chart
//            Matrix.scaleM(modelViewMatrixColorChartTopRight, 0, 35, 35, 35);
//            Matrix.multiplyMM(modelViewProjectionColorChartTopRight, 0,
//                    vuforiaAppSession.getProjectionMatrix().getData(), 0, modelViewMatrixColorChartTopRight, 0);
//            GLES20.glUseProgram(vbShaderProgramID);
//            GLES20.glVertexAttribPointer(vbVertexHandle, 3, GLES20.GL_FLOAT, false, 0,
//                    fillBuffer(colorChartTopRightVertices));
//            GLES20.glEnableVertexAttribArray(vbVertexHandle);
//            GLES20.glUniform1f(lineOpacityHandle, 1.0f);
//            GLES20.glUniform3f(lineColorHandle, 0.0f, 0.0f, 1.0f);
//            GLES20.glUniformMatrix4fv(mvpMatrixButtonsHandle, 1, false, modelViewProjectionColorChartTopRight,
//                    0);
//            GLES20.glDrawArrays(GLES20.GL_LINES, 0, 8 * colorChartTopRightRectangle.length);
//
//            //Draws blue box around bottom left color chart
//            Matrix.scaleM(modelViewMatrixColorChartBottomLeft, 0, 35, 35, 35);
//            Matrix.multiplyMM(modelViewProjectionColorChartBottomLeft, 0,
//                    vuforiaAppSession.getProjectionMatrix().getData(), 0, modelViewMatrixColorChartBottomLeft, 0);
//            GLES20.glUseProgram(vbShaderProgramID);
//            GLES20.glVertexAttribPointer(vbVertexHandle, 3, GLES20.GL_FLOAT, false, 0,
//                    fillBuffer(colorChartBottomLeftVertices));
//            GLES20.glEnableVertexAttribArray(vbVertexHandle);
//            GLES20.glUniform1f(lineOpacityHandle, 1.0f);
//            GLES20.glUniform3f(lineColorHandle, 0.0f, 0.0f, 1.0f);
//            GLES20.glUniformMatrix4fv(mvpMatrixButtonsHandle, 1, false, modelViewProjectionColorChartBottomLeft,
//                    0);
//            GLES20.glDrawArrays(GLES20.GL_LINES, 0, 8 * colorChartBottomLeftRectangle.length);
//
//            //Draws blue box around bottom right color chart
//            Matrix.scaleM(modelViewMatrixColorChartBottomRight, 0, 35, 35, 35);
//            Matrix.multiplyMM(modelViewProjectionColorChartBottomRight, 0,
//                    vuforiaAppSession.getProjectionMatrix().getData(), 0, modelViewMatrixColorChartBottomRight, 0);
//            GLES20.glUseProgram(vbShaderProgramID);
//            GLES20.glVertexAttribPointer(vbVertexHandle, 3, GLES20.GL_FLOAT, false, 0,
//                    fillBuffer(colorChartBottomRightVertices));
//            GLES20.glEnableVertexAttribArray(vbVertexHandle);
//            GLES20.glUniform1f(lineOpacityHandle, 1.0f);
//            GLES20.glUniform3f(lineColorHandle, 0.0f, 0.0f, 1.0f);
//            GLES20.glUniformMatrix4fv(mvpMatrixButtonsHandle, 1, false, modelViewProjectionColorChartBottomRight,
//                    0);
//            GLES20.glDrawArrays(GLES20.GL_LINES, 0, 8 * colorChartBottomRightRectangle.length);


            /**
             * Grab image slice
             */
            if (phi < 0.5) {
                // Grab camera image and convert into source Mat
                cameraBitmap = getCameraBitmap(state);
                Bitmap srcBitmap = cameraBitmap.copy(Bitmap.Config.ARGB_8888, true);
                Mat srcImageMat = new Mat(srcBitmap.getHeight(), srcBitmap.getWidth(), CvType.CV_8UC4);
                Utils.bitmapToMat(srcBitmap, srcImageMat);

                Bitmap rectifiedWoundBitmap = rectifyImageSlice(woundImageRectangle[0], pose, srcImageMat,
                        930, 260);

                Bitmap rectifiedColorChartTopLeftBitmap = rectifyImageSlice(colorChartTopLeftRectangle[0],
                        pose, srcImageMat, 200, 130);
                Bitmap rectifiedColorChartTopRightBitmap = rectifyImageSlice(colorChartTopRightRectangle[0],
                        pose, srcImageMat, 200, 130);
                Bitmap rectifiedColorChartBottomLeftBitmap = rectifyImageSlice(colorChartBottomLeftRectangle[0],
                        pose, srcImageMat, 200, 130);
                Bitmap rectifiedColorChartBottomRightBitmap = rectifyImageSlice(colorChartBottomRightRectangle[0],
                        pose, srcImageMat, 200, 130);

                Bitmap colorChartImage = mergeBitmaps(rectifiedColorChartTopLeftBitmap,
                        rectifiedColorChartTopRightBitmap,
                        rectifiedColorChartBottomLeftBitmap,
                        rectifiedColorChartBottomRightBitmap);

                // Return rectified bitmap to ImageTargets
                activityHandler.post(new Runnable() {
                    public void run() {
                        mActivity.updateCapture(rectifiedWoundBitmap, colorChartImage);
//                        mActivity.updateCapture(colorChartImage);
                    }
                });
            }
            else {
                activityHandler.post(new Runnable() {
                    public void run() {
                        mActivity.hideCapture();
                    }
                });
            }
            SampleUtils.checkGLError("FrameMarkers render frame");
        }
        GLES20.glDisable(GLES20.GL_DEPTH_TEST);
        mRenderer.end();
    }

    // Following code is mostly derived from "Mastering OpenCV Android Application Programming"
    // Chapter 9, pages 188-189.
    private Bitmap rectifyImageSlice(Rectangle imageSliceRect, Matrix34F pose, Mat srcImageMat,
                              int rectifiedWidth, int rectifiedHeight) {
        // Grab corners of slice in target space
        Vec3F topLeftImageSliceCapture = new Vec3F(imageSliceRect.getLeftTopX() * 35.0f,
                imageSliceRect.getLeftTopY() * 35.0f, 0f);
        Vec3F topRightImageSliceCapture = new Vec3F(imageSliceRect.getRightBottomX() * 35.0f,
                imageSliceRect.getLeftTopY() * 35.0f, 0f);
        Vec3F bottomLeftImageSliceCapture = new Vec3F(imageSliceRect.getLeftTopX() * 35.0f,
                imageSliceRect.getRightBottomY() * 35.0f, 0f);
        Vec3F bottomRightImageSliceCapture = new Vec3F(imageSliceRect.getRightBottomX() * 35.0f,
                imageSliceRect.getRightBottomY() * 35.0f, 0f);

        // Find projection of slice corners in camera space
        float[] topLeftImageSliceCaptureProjection = Tool.projectPoint(cameraCalib, pose,
                topLeftImageSliceCapture).getData();
        float[] topRightImageSliceCaptureProjection = Tool.projectPoint(cameraCalib, pose,
                topRightImageSliceCapture).getData();
        float[] bottomLeftImageSliceCaptureProjection = Tool.projectPoint(cameraCalib, pose,
                bottomLeftImageSliceCapture).getData();
        float[] bottomRightImageSliceCaptureProjection = Tool.projectPoint(cameraCalib, pose,
                bottomRightImageSliceCapture).getData();


        // Generate an arraylist of the x,y coordinates of the four corners of the skewed image
        Point topLeftSkewed = new Point(Math.round(topLeftImageSliceCaptureProjection[0]),
                Math.round(topLeftImageSliceCaptureProjection[1]));
        Point topRightSkewed = new Point(Math.round(topRightImageSliceCaptureProjection[0]),
                Math.round(topRightImageSliceCaptureProjection[1]));
        Point bottomRightSkewed = new Point(Math.round(bottomRightImageSliceCaptureProjection[0]),
                Math.round(bottomRightImageSliceCaptureProjection[1]));
        Point bottomLeftSkewed = new Point(Math.round(bottomLeftImageSliceCaptureProjection[0]),
                Math.round(bottomLeftImageSliceCaptureProjection[1]));

        ArrayList<Point> skewedCorners = new ArrayList<Point>();
        skewedCorners.add(topLeftSkewed);
        skewedCorners.add(topRightSkewed);
        skewedCorners.add(bottomRightSkewed);
        skewedCorners.add(bottomLeftSkewed);

        // Generate an arraylist of the x,y coordinates of the four corners of the final, rectified image
        // Note the width and height of "rectifiedMat" should be adjusted if the slice image's dimensions change
//        Mat rectifiedMat = Mat.zeros(new Size(930,    260), CvType.CV_8UC3);
        Mat rectifiedMat = Mat.zeros(new Size(rectifiedWidth,    rectifiedHeight), CvType.CV_8UC3);
        ArrayList<Point> rectifiedCorners = new ArrayList<Point>();
        rectifiedCorners.add(new Point(0, 0));
        rectifiedCorners.add(new Point(rectifiedMat.cols(), 0));
        rectifiedCorners.add(new Point(rectifiedMat.cols(), rectifiedMat.rows()));
        rectifiedCorners.add(new Point(0, rectifiedMat.rows()));

        // Calculate transformation matrix
        Mat origPts = Converters.vector_Point2f_to_Mat(skewedCorners);
        Mat resultPts = Converters.vector_Point2f_to_Mat(rectifiedCorners);
        Mat transformation = Imgproc.getPerspectiveTransform(origPts,   resultPts);

        // Rectify the image matrix
        Imgproc.warpPerspective(srcImageMat, rectifiedMat, transformation,  rectifiedMat.size());

        // Convert Matrix back into bitmap
        Bitmap resultBitmap = Bitmap.createBitmap(rectifiedMat.cols(), rectifiedMat.rows(),  Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(rectifiedMat, resultBitmap);

        return resultBitmap;
    }

    public Bitmap mergeBitmaps(Bitmap topLeft, Bitmap topRight, Bitmap bottomLeft, Bitmap bottomRight)
    {

        Bitmap comboBitmap;

        int width, height;

        width = topLeft.getWidth() + topRight.getWidth();
        height = topLeft.getHeight() + bottomLeft.getHeight();

        comboBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Canvas comboImage = new Canvas(comboBitmap);

        comboImage.drawBitmap(topLeft, 0f, 0f, null);
        comboImage.drawBitmap(topRight, topLeft.getWidth(), 0f , null);
        comboImage.drawBitmap(bottomLeft, 0, topLeft.getHeight(), null);
        comboImage.drawBitmap(bottomRight, topLeft.getWidth(), topLeft.getHeight(), null);
        return comboBitmap;

    }

    private int getDimension(int x, int camera, int slice) {
        if (slice > 0) {
            return Math.min(slice, x - camera);
        }
        return x - camera;
    }

    //REturn if a vector is smaller than a unit vector
    static boolean isSubUnit(float[] coords) {
        for (int i = 0; i < coords.length; ++i) {
            float f = coords[i];
            if (f < -1.0 || f > 1) {
                return false;
            }
        }
        return true;
    }

    /**
     * Sampling utils
     */
    private Vec3F[] getColorLevelArea(float cx, float cy, float cz) {
        Vec3F[] samples = new Vec3F[9];
        int r = 3, c = 3;
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                samples[i * r + j] = new Vec3F(cx + (i - 1) * 0.1f, cy
                        + (j - 1) * 0.1f, cz);
            }
        }
        return samples;
    }

    private void updateOffsets() {
        offsets = new float[rawOffsets.length];
        for (int i = 0; i < rawOffsets.length; i++) {
            offsets[i] = rawOffsets[i] + linearOffset;
        }
    }

    public double getColorCount() {
        return colorCount;
    }

    public String[] getColorPixels() {
        String[] result = new String[colorSpectrum.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = String.format("%d#%d#%d#%d", (int) colorSpectrum[i],
                    colorPixels[i][0], colorPixels[i][1], colorPixels[i][2]);
        }
        return result;
    }

    private int[] averagePixels(int[] pixels) {
        int redSum = 0;
        int blueSum = 0;
        int greenSum = 0;
        for (int i = 0; i < pixels.length; i++) {
            redSum += Color.red(pixels[i]);
            blueSum += Color.blue(pixels[i]);
            greenSum += Color.green(pixels[i]);
        }
        return new int[]{redSum / pixels.length, greenSum / pixels.length,
                blueSum / pixels.length};
    }

//    private double byteSamplingModel(int[] pixels, int measurement) {
//        SimpleRegression model = new SimpleRegression(true);
//        double[][] data = new double[colorSpectrum.length][2];
//        for (int i = 0; i < colorSpectrum.length; i++) {
//            data[i][0] = (double) pixels[i];
//            data[i][1] = (double) colorSpectrum[i];
//        }
//        model.addData(data);
//        return model.predict(measurement);
//    }

    private int[] getPixelsOnBitmap(Vec3F[] vectors, Matrix34F pose) {
        int[] pixels = new int[vectors.length];
        for (int i = 0; i < vectors.length; i++) {
            float[] point = Tool.projectPoint(cameraCalib, pose, vectors[i]).getData();

            int x = Math.round(point[0]);
            int y = Math.round(point[1]);

            if (y >= cameraBitmap.getHeight()) {
                y = cameraBitmap.getHeight() - 1;
            }

            if (x >= cameraBitmap.getWidth()) {
                x = cameraBitmap.getWidth() - 1;
            }

            if (x >= 0 && y >= 0) {
                pixels[i] = cameraBitmap.getPixel(x, y);
            }
        }

        return pixels;
    }

    private Bitmap getCameraBitmap(State state) {
        Image image = null;
        Frame frame = state.getFrame();
        for (int i = 0; i < frame.getNumImages(); i++) {
            image = frame.getImage(i);
            if (image.getFormat() == PIXEL_FORMAT.RGB565) {
                break;
            }
        }

        if (image != null) {
            ByteBuffer buffer = image.getPixels();
            cameraBitmap.copyPixelsFromBuffer(buffer);
            return cameraBitmap;
        } else {
            Log.e(LOGTAG, "image not found.");
        }
        return null;
    }

    private float[] getRotationMatrix(Matrix34F pose) {
        float poseValues[] = pose.getData();
        float rot[] = new float[16];
        rot[0] = poseValues[0];
        rot[1] = poseValues[1];
        rot[2] = poseValues[2];
        rot[3] = 0f;

        rot[4] = poseValues[4];
        rot[5] = poseValues[5];
        rot[6] = poseValues[6];
        rot[7] = 0f;

        rot[8] = poseValues[8];
        rot[9] = poseValues[9];
        rot[10] = poseValues[10];
        rot[11] = 0f;

        rot[12] = 0f;
        rot[13] = 0f;
        rot[14] = 0f;
        rot[15] = 0f;

        return rot;
    }


    private static final String TAG = "ImageTargetRenderer";

    private Bitmap segment(Bitmap source, int x, int y, int width, int height) {
        if ((x < 0) || (y < 0) || (width < 1) || (height < 1)) {
            return null;
        }
        else {
            Bitmap target = Bitmap.createBitmap(cameraBitmap.getWidth(), cameraBitmap.getHeight(),
                    Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(target);
            Paint p = new Paint();
            p.setColor(Color.BLACK);
            canvas.drawBitmap(source, 0, 0, new Paint());
            // and then cut it down
            //        Log.v(TAG, "width: " + width + " height: " + height);
            return Bitmap.createBitmap(target, x, y, width, height);
        }
    }

    public void setTextures(Vector<Texture> textures) {
        mTextures = textures;
    }

    /**
     * Render utils
     */
    private float[] targetGLVertices() {
        float[] vertices = new float[targetRectangle.length * 24];
        int vInd = 0;

        for (Rectangle rect : targetRectangle) {
            vertices[vInd] = rect.getLeftTopX();
            vertices[vInd + 1] = rect.getLeftTopY();
            vertices[vInd + 2] = 0.0f;
            vertices[vInd + 3] = rect.getRightBottomX();
            vertices[vInd + 4] = rect.getLeftTopY();
            vertices[vInd + 5] = 0.0f;
            vertices[vInd + 6] = rect.getRightBottomX();
            vertices[vInd + 7] = rect.getLeftTopY();
            vertices[vInd + 8] = 0.0f;
            vertices[vInd + 9] = rect.getRightBottomX();
            vertices[vInd + 10] = rect.getRightBottomY();
            vertices[vInd + 11] = 0.0f;
            vertices[vInd + 12] = rect.getRightBottomX();
            vertices[vInd + 13] = rect.getRightBottomY();
            vertices[vInd + 14] = 0.0f;
            vertices[vInd + 15] = rect.getLeftTopX();
            vertices[vInd + 16] = rect.getRightBottomY();
            vertices[vInd + 17] = 0.0f;
            vertices[vInd + 18] = rect.getLeftTopX();
            vertices[vInd + 19] = rect.getRightBottomY();
            vertices[vInd + 20] = 0.0f;
            vertices[vInd + 21] = rect.getLeftTopX();
            vertices[vInd + 22] = rect.getLeftTopY();
            vertices[vInd + 23] = 0.0f;
            vInd += 24;
        }
        return vertices;
    }

    private float[] woundGLVertices() {
        float[] vertices = new float[woundImageRectangle.length * 24];
        int vInd = 0;

        for (Rectangle rect : woundImageRectangle) {
            vertices[vInd] = rect.getLeftTopX();
            vertices[vInd + 1] = rect.getLeftTopY();
            vertices[vInd + 2] = 0.0f;
            vertices[vInd + 3] = rect.getRightBottomX();
            vertices[vInd + 4] = rect.getLeftTopY();
            vertices[vInd + 5] = 0.0f;
            vertices[vInd + 6] = rect.getRightBottomX();
            vertices[vInd + 7] = rect.getLeftTopY();
            vertices[vInd + 8] = 0.0f;
            vertices[vInd + 9] = rect.getRightBottomX();
            vertices[vInd + 10] = rect.getRightBottomY();
            vertices[vInd + 11] = 0.0f;
            vertices[vInd + 12] = rect.getRightBottomX();
            vertices[vInd + 13] = rect.getRightBottomY();
            vertices[vInd + 14] = 0.0f;
            vertices[vInd + 15] = rect.getLeftTopX();
            vertices[vInd + 16] = rect.getRightBottomY();
            vertices[vInd + 17] = 0.0f;
            vertices[vInd + 18] = rect.getLeftTopX();
            vertices[vInd + 19] = rect.getRightBottomY();
            vertices[vInd + 20] = 0.0f;
            vertices[vInd + 21] = rect.getLeftTopX();
            vertices[vInd + 22] = rect.getLeftTopY();
            vertices[vInd + 23] = 0.0f;
            vInd += 24;
        }
        return vertices;
    }

    private float[] colorChartTopLeftGLVertices() {
        float[] vertices = new float[colorChartTopLeftRectangle.length * 24];
        int vInd = 0;

        for (Rectangle rect : colorChartTopLeftRectangle) {
            vertices[vInd] = rect.getLeftTopX();
            vertices[vInd + 1] = rect.getLeftTopY();
            vertices[vInd + 2] = 0.0f;
            vertices[vInd + 3] = rect.getRightBottomX();
            vertices[vInd + 4] = rect.getLeftTopY();
            vertices[vInd + 5] = 0.0f;
            vertices[vInd + 6] = rect.getRightBottomX();
            vertices[vInd + 7] = rect.getLeftTopY();
            vertices[vInd + 8] = 0.0f;
            vertices[vInd + 9] = rect.getRightBottomX();
            vertices[vInd + 10] = rect.getRightBottomY();
            vertices[vInd + 11] = 0.0f;
            vertices[vInd + 12] = rect.getRightBottomX();
            vertices[vInd + 13] = rect.getRightBottomY();
            vertices[vInd + 14] = 0.0f;
            vertices[vInd + 15] = rect.getLeftTopX();
            vertices[vInd + 16] = rect.getRightBottomY();
            vertices[vInd + 17] = 0.0f;
            vertices[vInd + 18] = rect.getLeftTopX();
            vertices[vInd + 19] = rect.getRightBottomY();
            vertices[vInd + 20] = 0.0f;
            vertices[vInd + 21] = rect.getLeftTopX();
            vertices[vInd + 22] = rect.getLeftTopY();
            vertices[vInd + 23] = 0.0f;
            vInd += 24;
        }
        return vertices;
    }

    private float[] colorChartTopRightGLVertices() {
        float[] vertices = new float[colorChartTopRightRectangle.length * 24];
        int vInd = 0;

        for (Rectangle rect : colorChartTopRightRectangle) {
            vertices[vInd] = rect.getLeftTopX();
            vertices[vInd + 1] = rect.getLeftTopY();
            vertices[vInd + 2] = 0.0f;
            vertices[vInd + 3] = rect.getRightBottomX();
            vertices[vInd + 4] = rect.getLeftTopY();
            vertices[vInd + 5] = 0.0f;
            vertices[vInd + 6] = rect.getRightBottomX();
            vertices[vInd + 7] = rect.getLeftTopY();
            vertices[vInd + 8] = 0.0f;
            vertices[vInd + 9] = rect.getRightBottomX();
            vertices[vInd + 10] = rect.getRightBottomY();
            vertices[vInd + 11] = 0.0f;
            vertices[vInd + 12] = rect.getRightBottomX();
            vertices[vInd + 13] = rect.getRightBottomY();
            vertices[vInd + 14] = 0.0f;
            vertices[vInd + 15] = rect.getLeftTopX();
            vertices[vInd + 16] = rect.getRightBottomY();
            vertices[vInd + 17] = 0.0f;
            vertices[vInd + 18] = rect.getLeftTopX();
            vertices[vInd + 19] = rect.getRightBottomY();
            vertices[vInd + 20] = 0.0f;
            vertices[vInd + 21] = rect.getLeftTopX();
            vertices[vInd + 22] = rect.getLeftTopY();
            vertices[vInd + 23] = 0.0f;
            vInd += 24;
        }
        return vertices;
    }

    private float[] colorChartBottomLeftGLVertices() {
        float[] vertices = new float[colorChartBottomLeftRectangle.length * 24];
        int vInd = 0;

        for (Rectangle rect : colorChartBottomLeftRectangle) {
            vertices[vInd] = rect.getLeftTopX();
            vertices[vInd + 1] = rect.getLeftTopY();
            vertices[vInd + 2] = 0.0f;
            vertices[vInd + 3] = rect.getRightBottomX();
            vertices[vInd + 4] = rect.getLeftTopY();
            vertices[vInd + 5] = 0.0f;
            vertices[vInd + 6] = rect.getRightBottomX();
            vertices[vInd + 7] = rect.getLeftTopY();
            vertices[vInd + 8] = 0.0f;
            vertices[vInd + 9] = rect.getRightBottomX();
            vertices[vInd + 10] = rect.getRightBottomY();
            vertices[vInd + 11] = 0.0f;
            vertices[vInd + 12] = rect.getRightBottomX();
            vertices[vInd + 13] = rect.getRightBottomY();
            vertices[vInd + 14] = 0.0f;
            vertices[vInd + 15] = rect.getLeftTopX();
            vertices[vInd + 16] = rect.getRightBottomY();
            vertices[vInd + 17] = 0.0f;
            vertices[vInd + 18] = rect.getLeftTopX();
            vertices[vInd + 19] = rect.getRightBottomY();
            vertices[vInd + 20] = 0.0f;
            vertices[vInd + 21] = rect.getLeftTopX();
            vertices[vInd + 22] = rect.getLeftTopY();
            vertices[vInd + 23] = 0.0f;
            vInd += 24;
        }
        return vertices;
    }

    private float[] colorChartBottomRightGLVertices() {
        float[] vertices = new float[colorChartBottomRightRectangle.length * 24];
        int vInd = 0;

        for (Rectangle rect : colorChartBottomRightRectangle) {
            vertices[vInd] = rect.getLeftTopX();
            vertices[vInd + 1] = rect.getLeftTopY();
            vertices[vInd + 2] = 0.0f;
            vertices[vInd + 3] = rect.getRightBottomX();
            vertices[vInd + 4] = rect.getLeftTopY();
            vertices[vInd + 5] = 0.0f;
            vertices[vInd + 6] = rect.getRightBottomX();
            vertices[vInd + 7] = rect.getLeftTopY();
            vertices[vInd + 8] = 0.0f;
            vertices[vInd + 9] = rect.getRightBottomX();
            vertices[vInd + 10] = rect.getRightBottomY();
            vertices[vInd + 11] = 0.0f;
            vertices[vInd + 12] = rect.getRightBottomX();
            vertices[vInd + 13] = rect.getRightBottomY();
            vertices[vInd + 14] = 0.0f;
            vertices[vInd + 15] = rect.getLeftTopX();
            vertices[vInd + 16] = rect.getRightBottomY();
            vertices[vInd + 17] = 0.0f;
            vertices[vInd + 18] = rect.getLeftTopX();
            vertices[vInd + 19] = rect.getRightBottomY();
            vertices[vInd + 20] = 0.0f;
            vertices[vInd + 21] = rect.getLeftTopX();
            vertices[vInd + 22] = rect.getLeftTopY();
            vertices[vInd + 23] = 0.0f;
            vInd += 24;
        }
        return vertices;
    }

    private Buffer fillBuffer(float[] array) {
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * array.length);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (float d : array) {
            bb.putFloat(d);
        }
        bb.rewind();
        return bb;
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        if (!mIsActive) {
            return;
        }
        renderFrame();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.d(LOGTAG, "GLRenderer.onSurfaceCreated");
        initRendering();
        vuforiaAppSession.onSurfaceCreated();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.d(LOGTAG, "GLRenderer.onSurfaceChanged");
        vuforiaAppSession.onSurfaceChanged(width, height);
    }

    public int[] getCameraDetails() {
        VideoBackgroundConfig config = mRenderer.getInstance().getVideoBackgroundConfig();
        int[] details = new int[2];

        details[0] = config.getSize().getData()[0];
        details[1] = config.getSize().getData()[1];

        return details;
    }

    class Square {

        // This class is used to create the indicator search space and adjust it for the target location
        private FloatBuffer vertexBuffer;
        private ShortBuffer drawListBuffer;

        // number of coordinates per vertex in this array
        final int COORDS_PER_VERTEX = 3;


        float squareCoords[] = {SEARCH_LIMIT_LEFT - 0.1f, -0.55f, 0.0f,   // top left
                SEARCH_LIMIT_LEFT - 0.1f, -0.65f, 0.0f,   // bottom left
                SEARCH_LIMIT_RIGHT + 0.1f, -0.65f, 0.0f,   // bottom right
                SEARCH_LIMIT_RIGHT + 0.1f, -0.55f, 0.0f}; // top right
        private short drawOrder[] = {0, 1, 2, 0, 2, 3}; // order to draw vertices
        float color[] = {1.0f, 0f, 0f, 1.0f};

        private final String vertexShaderCode =
                // This matrix member variable provides a hook to manipulate
                // the coordinates of the objects that use this vertex shader
                "uniform mat4 uMVPMatrix;" +
                        "attribute vec4 vPosition;" +
                        "void main() {" +
                        // The matrix must be included as a modifier of gl_Position.
                        // Note that the uMVPMatrix factor *must be first* in order
                        // for the matrix multiplication product to be correct.
                        "  gl_Position = uMVPMatrix * vPosition;" +
                        "}";

        private final String fragmentShaderCode =
                "precision mediump float;" +
                        "uniform vec4 vColor;" +
                        "void main() {" +
                        "  gl_FragColor = vColor;" +
                        "}";

        int mProgram, vertexShader, fragmentShader;

        final int vertexStride = COORDS_PER_VERTEX * 4;
        final int vertexCount = 4;

        public Square() {
            // initialize vertex byte buffer for shape coordinates
            ByteBuffer bb = ByteBuffer.allocateDirect(
                    squareCoords.length * 4); // (# of coordinate values * 4 bytes per float)
            bb.order(ByteOrder.nativeOrder());
            vertexBuffer = bb.asFloatBuffer();
            vertexBuffer.put(squareCoords);
            vertexBuffer.position(0);

            // initialize byte buffer for the draw list
            ByteBuffer dlb = ByteBuffer.allocateDirect(
                    drawOrder.length * 2); // (# of coordinate values * 2 bytes per short)
            dlb.order(ByteOrder.nativeOrder());
            drawListBuffer = dlb.asShortBuffer();
            drawListBuffer.put(drawOrder);
            drawListBuffer.position(0);

            vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
            fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

            mProgram = GLES20.glCreateProgram();             // create empty OpenGL ES Program
            GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
            GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
            GLES20.glLinkProgram(
                    mProgram);                  // creates OpenGL ES program executables
        }

        int loadShader(int type, String shaderCode) {
            // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
            // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
            int shader = GLES20.glCreateShader(type);

            // add the source code to the shader and compile it
            GLES20.glShaderSource(shader, shaderCode);
            GLES20.glCompileShader(shader);

            return shader;
        }

        public void draw(float[] projection) {
            // Add program to OpenGL ES environment
            GLES20.glUseProgram(mProgram);

            // get handle to vertex shader's vPosition member
            int mPositionHandle = GLES20.glGetAttribLocation(mProgram, "vPosition");

            // Enable a handle to the triangle vertices
            GLES20.glEnableVertexAttribArray(mPositionHandle);

            // Prepare the triangle coordinate data
            GLES20.glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false,
                    vertexStride, vertexBuffer);

            // get handle to fragment shader's vColor member and color for drawing the triangle
            int mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
            GLES20.glUniform4fv(mColorHandle, 1, color, 0);

            // get handle to shape's transformation matrix
            int mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
            GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, projection, 0);

            // Draw the indicatorSearchSpace
            GLES20.glDrawElements(
                    GLES20.GL_TRIANGLES, drawOrder.length,
                    GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

            // Disable vertex array
            GLES20.glDisableVertexAttribArray(mPositionHandle);

            // Disable vertex array
            GLES20.glDisableVertexAttribArray(mPositionHandle);
        }

        public void update(float[] projection) {
            int[] offsets = new int[]{0, 3, 6, 9};
            float[][] rawCorners = new float[][]{
                    new float[2],
                    new float[2],
                    new float[2],
                    new float[2]
            };
            int which = 0;
            for (int offt : offsets) {
                float[] inVec = pluckVec3(offt);
                float[] outVec = new float[4];
                Matrix.multiplyMV(outVec, 0, projection, 0, inVec, 0);
                divideByW(outVec);
                //Copy the x and y into the array
                System.arraycopy(outVec, 0, rawCorners[which], 0, 2);
                ++which;
            }
            //Cleanup the corners  order is TL,BL,BR,TR
            corners[0][0] = Math.min(rawCorners[0][0], rawCorners[1][0]); //Left of TL and BL
            corners[0][1] = Math.max(rawCorners[1][1], rawCorners[3][1]);//Top of TL,TR
            corners[1][0] = Math.max(rawCorners[2][0], rawCorners[3][0]);//Right of TR,BR
            corners[1][1] = Math.min(rawCorners[1][1], rawCorners[2][1]); //Bottom of BL,BR
            skewx = Math.abs(rawCorners[0][0] - rawCorners[1][0]);
            skewy = Math.abs(rawCorners[1][1] - rawCorners[2][1]);
            //android.util.Log.v(LOGTAG, "Corners  are " + Arrays.toString(corners[0]) + "->" + Arrays.toString(corners[1]));
        }

        public float[] pluckVec3(int offset) {
            float[] out = new float[4];
            out[3] = 1;
            for (int i = 0; i < 3; ++i) {
                out[i] = squareCoords[i + offset];
            }
            return out;
        }

        public void divideByW(float[] vec4) {
            for (int i = 0; i < 3; ++i) {
                vec4[i] = vec4[i] / vec4[3];
            }
        }

        public float[] envelope() {
            return squareCoords;
        }
    }

    int locateMarker(Bitmap slice) {
        int i, r, g, b, color, w = slice.getWidth(), h, height = slice.getHeight();
        for (h = height - 1; h >= 0; h--) {
            for (i = 0; i < w; i++) {
                color = slice.getPixel(i, h);
                r = Color.red(color);
                g = Color.green(color);
                b = Color.blue(color);

                //android.util.Log.v("COLOR", "("+i+","+h+") "+Integer.toHexString(r)+" "+Integer.toHexString(g)+" "+Integer.toHexString(b));
                if (r > 60 && r > 2 * g && r > 2 * b) {
                    return i;
                }
            }
        }
        return -1;
    }

    float measure(int pos, int width) {
        float ratio = (float) pos / (float) width;
        float value = 840.0f; // Set the value to 840 to return as a default
        int i;
        if (ratio < offsets[0]) {
            return 60.0f;
        } else {
            for (i = 1; i < offsets.length; i++) {
                if (ratio >= offsets[i - 1] && ratio <= offsets[i]) {
                    value = values[i - 1]
                            + (ratio - offsets[i - 1]) * (values[i] - values[i - 1]) / (offsets[i]
                            - offsets[i - 1]);

                    // set the arrow offset
                    chi = positions[i - 1]
                            + (ratio - offsets[i - 1]) * (positions[i] - positions[i - 1]) / (
                            offsets[i] - offsets[i - 1]);
                    break;
                }
            }
        }
        return value;
    }

    void incrementChi() {
        chi = chi + 1;
        if (chi > 1200) {
            chi = -20;
        }
        android.util.Log.v("CHI", String.valueOf(chi));
    }

    float peakFlowUnitsToImageUnits(int flowValue, float imageUnitsMin, float imageUnitsMax) {
        float imageUnits = -999.0f;
        if (flowValue <= 60) {
            imageUnits = imageUnitsMin;
        } else if (flowValue >= 840) {
            imageUnits = imageUnitsMax;
        } else {
            for (int i = 0; i < values.length; i++) {
                if (flowValue <= values[i]) {
                    float percentOfImage =
                            (flowValue - values[i - 1]) * (offsets[i] - offsets[i - 1]) / (values[i]
                                    - values[i - 1]) + offsets[i - 1];
                    imageUnits = percentOfImage * (imageUnitsMax - imageUnitsMin) + imageUnitsMin;
                    break;
                }
            }
        }
        return imageUnits;
    }
}