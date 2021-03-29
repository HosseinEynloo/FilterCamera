/*
 * Copyright 2016 nekocode
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.nekocode.camFilter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;


import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import cn.nekocode.camFilter.filter.AsciiArtFilter;
import cn.nekocode.camFilter.filter.BasicDeformFilter;
import cn.nekocode.camFilter.filter.BlackAndWhiteFilter;
import cn.nekocode.camFilter.filter.BlueorangeFilter;
import cn.nekocode.camFilter.filter.CameraFilter;
import cn.nekocode.camFilter.filter.CartoonFilter;
import cn.nekocode.camFilter.filter.CastingFilter;
import cn.nekocode.camFilter.filter.ChromaticAberrationFilter;
import cn.nekocode.camFilter.filter.ContrastFilter;
import cn.nekocode.camFilter.filter.CrackedFilter;
import cn.nekocode.camFilter.filter.CrosshatchFilter;
import cn.nekocode.camFilter.filter.EMInterferenceFilter;
import cn.nekocode.camFilter.filter.EdgeDetectionFilter;
import cn.nekocode.camFilter.filter.GrayFilter;
import cn.nekocode.camFilter.filter.HexagonMosaicFilter;
import cn.nekocode.camFilter.filter.JFAVoronoiFilter;
import cn.nekocode.camFilter.filter.LegofiedFilter;
import cn.nekocode.camFilter.filter.LichtensteinEsqueFilter;
import cn.nekocode.camFilter.filter.MappingFilter;
import cn.nekocode.camFilter.filter.MirrorFilter;
import cn.nekocode.camFilter.filter.MoneyFilter;
import cn.nekocode.camFilter.filter.NegativeFilter;
import cn.nekocode.camFilter.filter.NoiseWarpFilter;
import cn.nekocode.camFilter.filter.NostalgiaFilter;
import cn.nekocode.camFilter.filter.OriginalFilter;
import cn.nekocode.camFilter.filter.PixelizeFilter;
import cn.nekocode.camFilter.filter.PolygonizationFilter;
import cn.nekocode.camFilter.filter.RefractionFilter;
import cn.nekocode.camFilter.filter.ReliefFilter;
import cn.nekocode.camFilter.filter.SwirlFilter;
import cn.nekocode.camFilter.filter.TileMosaicFilter;
import cn.nekocode.camFilter.filter.TrianglesMosaicFilter;
import cn.nekocode.camFilter.filter.TripleFilter;
import cn.nekocode.camFilter.filter.WaterReflectionFilter;
import cn.nekocode.camFilter.mediaRecord.CameraHelper;

/**
 * @author nekocode (nekocode.cn@gmail.com)
 */
public class CameraRenderer implements Runnable, TextureView.SurfaceTextureListener {
    private static final String TAG = "CameraRenderer";
    private static final int EGL_OPENGL_ES2_BIT = 4;
    private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
    private static final int DRAW_INTERVAL = 1000 / 30;

    public static Thread renderThread;
    private Context context;
    private SurfaceTexture surfaceTexture;
    private int gwidth, gheight;

    private EGLDisplay eglDisplay;
    private EGLSurface eglSurface;
    private EGLContext eglContext;
    private EGL10 egl10;


    public static Camera camera;
    MediaRecorder rec;
    private SurfaceTexture cameraSurfaceTexture;
    private int cameraTextureId;
    private CameraFilter selectedFilter;
    private int selectedFilterId = R.id.filter2;
    private SparseArray<CameraFilter> cameraFilterMap = new SparseArray<>();


    boolean isRecording = false;
    MediaRecorder mMediaRecorder;
    private File mOutputFile;


    public CameraRenderer(Context context) {
        this.context = context;
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
//        Log.i("opengl1", "onSurfaceTextureSizeChanged: ");
        gwidth = -width;
        gheight = -height;
//        surface.setDefaultBufferSize(width, height);
    }


    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        Log.i("opengl1", "onSurfaceTextureAvailable: ");
        if (renderThread != null && renderThread.isAlive()) {
            renderThread.interrupt();
        }
        renderThread = new Thread(this);
        surfaceTexture = surface;
        gwidth = -width;
        gheight = -height;


        // Open camera
        Pair<Camera.CameraInfo, Integer> backCamera = getBackCamera();
        int backCameraId = backCamera.second;
        camera = Camera.open(backCameraId);


        // Start rendering
        renderThread.start();
    }

    protected void setDisplayOrientation(Camera camera, int angle) {
        Method downPolymorphic;
        try {
            downPolymorphic = camera.getClass().getMethod("setDisplayOrientation", new Class[]{int.class});
            if (downPolymorphic != null)
                downPolymorphic.invoke(camera, new Object[]{angle});
        } catch (Exception e1) {
        }
    }

    public void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 180;
                break;
            case Surface.ROTATION_90:
                degrees = 180;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 180;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        Log.i("opengl1", "onSurfaceTextureUpdated: ");
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.i("opengl1", "onSurfaceTextureDestroyed: ");
        if (camera != null) {
//            camera.stopPreview();
            camera.release();
        }
        if (renderThread != null && renderThread.isAlive()) {
            renderThread.interrupt();
        }
        CameraFilter.release();

        return true;
    }

    public void setSelectedFilter(int id) {
        selectedFilterId = id;
        selectedFilter = cameraFilterMap.get(id);
        if (selectedFilter != null)
            selectedFilter.onAttach();
    }

    @Override
    public void run() {
        initGL(surfaceTexture);
        // Setup camera filters map
        cameraFilterMap.append(R.id.filter0, new OriginalFilter(context));
        cameraFilterMap.append(R.id.filter1, new EdgeDetectionFilter(context));
        cameraFilterMap.append(R.id.filter2, new PixelizeFilter(context));
        cameraFilterMap.append(R.id.filter3, new EMInterferenceFilter(context));
        cameraFilterMap.append(R.id.filter4, new TrianglesMosaicFilter(context));
        cameraFilterMap.append(R.id.filter5, new LegofiedFilter(context));
        cameraFilterMap.append(R.id.filter6, new TileMosaicFilter(context));
        cameraFilterMap.append(R.id.filter7, new BlueorangeFilter(context));
        cameraFilterMap.append(R.id.filter8, new ChromaticAberrationFilter(context));
        cameraFilterMap.append(R.id.filter9, new BasicDeformFilter(context));
        cameraFilterMap.append(R.id.filter10, new ContrastFilter(context));
        cameraFilterMap.append(R.id.filter11, new NoiseWarpFilter(context));
        cameraFilterMap.append(R.id.filter12, new RefractionFilter(context));
        cameraFilterMap.append(R.id.filter13, new MappingFilter(context));
        cameraFilterMap.append(R.id.filter14, new CrosshatchFilter(context));
        cameraFilterMap.append(R.id.filter15, new LichtensteinEsqueFilter(context));
        cameraFilterMap.append(R.id.filter16, new AsciiArtFilter(context));
        cameraFilterMap.append(R.id.filter17, new MoneyFilter(context));
        cameraFilterMap.append(R.id.filter18, new CrackedFilter(context));
        cameraFilterMap.append(R.id.filter19, new PolygonizationFilter(context));
        cameraFilterMap.append(R.id.filter20, new JFAVoronoiFilter(context));
        cameraFilterMap.append(R.id.filter21, new BlackAndWhiteFilter(context));
        cameraFilterMap.append(R.id.filter22, new GrayFilter(context));
        cameraFilterMap.append(R.id.filter23, new NegativeFilter(context));
        cameraFilterMap.append(R.id.filter24, new NostalgiaFilter(context));
        cameraFilterMap.append(R.id.filter25, new CastingFilter(context));
        cameraFilterMap.append(R.id.filter26, new ReliefFilter(context));
        cameraFilterMap.append(R.id.filter27, new SwirlFilter(context));
        cameraFilterMap.append(R.id.filter28, new HexagonMosaicFilter(context));
        cameraFilterMap.append(R.id.filter29, new MirrorFilter(context));
        cameraFilterMap.append(R.id.filter30, new TripleFilter(context));
        cameraFilterMap.append(R.id.filter31, new CartoonFilter(context));
        cameraFilterMap.append(R.id.filter32, new WaterReflectionFilter(context));
        setSelectedFilter(selectedFilterId);

        // Create texture for camera preview
        cameraTextureId = MyGLUtils.genTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
        cameraSurfaceTexture = new SurfaceTexture(cameraTextureId);

        // Start camera preview
        try {
            camera.setPreviewTexture(cameraSurfaceTexture);
            camera.startPreview();
        } catch (IOException ioe) {
            // Something bad happened
        }

        // Render loop
        while (!Thread.currentThread().isInterrupted()) {
            try {
                if (gwidth < 0 && gheight < 0)
                    GLES20.glViewport(0, 0, gwidth = -gwidth, gheight = -gheight);

                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

                // Update the camera preview texture
                synchronized (this) {
                    cameraSurfaceTexture.updateTexImage();
                }

                // Draw camera preview
                if (selectedFilter != null)
                    selectedFilter.draw(cameraTextureId, gwidth, gheight);

                // Flush
                GLES20.glFlush();
                egl10.eglSwapBuffers(eglDisplay, eglSurface);

                Thread.sleep(DRAW_INTERVAL);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        cameraSurfaceTexture.release();
        GLES20.glDeleteTextures(1, new int[]{cameraTextureId}, 0);
    }

    public void filiming() {
        camera = Camera.open();
        rec = new MediaRecorder();                               // state "Initial"

        camera.lock();
        camera.unlock();

        rec.setCamera(camera);                                  // state still "Initial"
        rec.setVideoSource(MediaRecorder.VideoSource.CAMERA);    // state "Initialized"
        rec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);  // state "DataSourceConfigured"
        rec.setVideoEncoder(MediaRecorder.VideoEncoder.H264);

//        rec.setPreviewDisplay(cameraSurfaceTexture);
//        rec.setPreviewDisplay();

        rec.setOutputFile(Environment.getExternalStorageDirectory() + "/test.mp4");

        try {
            rec.prepare();                                           // state "Prepared"
        } catch (IOException e) {
            e.printStackTrace();
        }
        rec.start();                                             // state "Recording"

// ...

        rec.stop();
    }

    public void startCapture() {

        if (isRecording) {
            // BEGIN_INCLUDE(stop_release_media_recorder)
            Toast.makeText(context, "isRecording => Stop", Toast.LENGTH_LONG).show();
            // stop recording and release camera
            try {
                mMediaRecorder.stop();  // stop the recording
            } catch (RuntimeException e) {
                // RuntimeException is thrown when stop() is called immediately after start().
                // In this case the output file is not properly constructed ans should be deleted.
                Log.d(TAG, "RuntimeException: stop() is called immediately after start()");
                //noinspection ResultOfMethodCallIgnored
                mOutputFile.delete();
            }
            Log.i("papap", "releaseMediaRecorder: ");
            releaseMediaRecorder(); // release the MediaRecorder object
            camera.lock();         // take camera access back from MediaRecorder

            // inform the user that recording has stopped
//            setCaptureButtonText("Capture");
            isRecording = false;
            releaseCamera();
            // END_INCLUDE(stop_release_media_recorder)

        } else {

            // BEGIN_INCLUDE(prepare_start_media_recorder)

            new MediaPrepareTask().execute(null, null, null);

            Toast.makeText(context, "Start Record", Toast.LENGTH_LONG).show();

            // END_INCLUDE(prepare_start_media_recorder)

        }
    }

    private void releaseCamera() {
        if (camera != null) {
            // release the camera for other applications
            camera.release();
            camera = null;
        }
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            // clear recorder configuration
            mMediaRecorder.reset();
            // release the recorder object
            mMediaRecorder.release();
            mMediaRecorder = null;
            // Lock camera for later use i.e taking it back from MediaRecorder.
            // MediaRecorder doesn't need it anymore and we will release it if the activity pauses.
            camera.lock();
        }
    }


    @SuppressLint("NewApi")
    private boolean prepareVideoRecorder() {
        // BEGIN_INCLUDE (configure_preview)

        if (camera == null)
            camera = CameraHelper.getDefaultCameraInstance();

        // We need to make sure that our preview and recording video size are supported by the
        // camera. Query camera to find all the sizes and choose the optimal size given the
        // dimensions of our preview surface.


        Camera.Parameters parameters = camera.getParameters();
//        parameters.setColorEffect(Camera.Parameters.EFFECT_NEGATIVE);
        List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
        List<Camera.Size> mSupportedVideoSizes = parameters.getSupportedVideoSizes();
        Camera.Size optimalSize = CameraHelper.getOptimalVideoSize(mSupportedVideoSizes, mSupportedPreviewSizes, gwidth, gheight);

        // Use the same size for recording profile.
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        profile.videoFrameWidth = optimalSize.width;
        profile.videoFrameHeight = optimalSize.height;


        // likewise for the camera object itself.
        parameters.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight);
        camera.setParameters(parameters);
        Surface surface = new Surface(cameraSurfaceTexture);

        try {
            // Requires API level 11+, For backward compatibility use {@link setPreviewDisplay}
            // with {@link SurfaceView}
            camera.setPreviewTexture(cameraSurfaceTexture);
        } catch (IOException e) {
            Log.e("POI", "Surface texture is unavailable or unsuitable" + e.getMessage());
            return false;
        }
        // END_INCLUDE (configure_preview)

        // BEGIN_INCLUDE (configure_media_recorder)
        mMediaRecorder = new MediaRecorder();

        // Step 1: Unlock and set camera to MediaRecorder
        camera.unlock();
        mMediaRecorder.setCamera(camera);


        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(profile);
        mMediaRecorder.setPreviewDisplay(surface);

        // Step 4: Set output file
        mOutputFile = CameraHelper.getOutputMediaFile(CameraHelper.MEDIA_TYPE_VIDEO);
        if (mOutputFile == null) {
            Log.e("POI", "prepareVideoRecorder: ");

            return false;
        }
        mMediaRecorder.setOutputFile(mOutputFile.getPath());
        // END_INCLUDE (configure_media_recorder)

        // Step 5: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void initGL(SurfaceTexture texture) {
        egl10 = (EGL10) EGLContext.getEGL();

        eglDisplay = egl10.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);
        if (eglDisplay == EGL10.EGL_NO_DISPLAY) {
            throw new RuntimeException("eglGetDisplay failed " +
                    android.opengl.GLUtils.getEGLErrorString(egl10.eglGetError()));
        }

        int[] version = new int[2];
        if (!egl10.eglInitialize(eglDisplay, version)) {
            throw new RuntimeException("eglInitialize failed " +
                    android.opengl.GLUtils.getEGLErrorString(egl10.eglGetError()));
        }

        int[] configsCount = new int[1];
        EGLConfig[] configs = new EGLConfig[1];
        int[] configSpec = {
                EGL10.EGL_RENDERABLE_TYPE,
                EGL_OPENGL_ES2_BIT,
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 0,
                EGL10.EGL_STENCIL_SIZE, 0,
                EGL10.EGL_NONE
        };

        EGLConfig eglConfig = null;
        if (!egl10.eglChooseConfig(eglDisplay, configSpec, configs, 1, configsCount)) {
            throw new IllegalArgumentException("eglChooseConfig failed " +
                    android.opengl.GLUtils.getEGLErrorString(egl10.eglGetError()));
        } else if (configsCount[0] > 0) {
            eglConfig = configs[0];
        }
        if (eglConfig == null) {
            throw new RuntimeException("eglConfig not initialized");
        }

        int[] attrib_list = {EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE};
        eglContext = egl10.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list);
        eglSurface = egl10.eglCreateWindowSurface(eglDisplay, eglConfig, texture, null);

        if (eglSurface == null || eglSurface == EGL10.EGL_NO_SURFACE) {
            int error = egl10.eglGetError();
            if (error == EGL10.EGL_BAD_NATIVE_WINDOW) {
                Log.e(TAG, "eglCreateWindowSurface returned EGL10.EGL_BAD_NATIVE_WINDOW");
                return;
            }
            throw new RuntimeException("eglCreateWindowSurface failed " +
                    android.opengl.GLUtils.getEGLErrorString(error));
        }

        if (!egl10.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            throw new RuntimeException("eglMakeCurrent failed " +
                    android.opengl.GLUtils.getEGLErrorString(egl10.eglGetError()));
        }
    }

    private Pair<Camera.CameraInfo, Integer> getBackCamera() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        final int numberOfCameras = Camera.getNumberOfCameras();

        for (int i = 0; i < numberOfCameras; ++i) {
            Camera.getCameraInfo(i, cameraInfo);
            if (MainActivity.backFace) {
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    return new Pair<>(cameraInfo, i);
                }
            } else {
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    return new Pair<>(cameraInfo, i);
                }

            }
        }
        return null;
    }

    private Pair<Camera.CameraInfo, Integer> getFrontCamera() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        final int numberOfCameras = Camera.getNumberOfCameras();

        for (int i = 0; i < numberOfCameras; ++i) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                return new Pair<>(cameraInfo, i);
            }
        }
        return null;
    }

    class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            // initialize video camera
            if (prepareVideoRecorder()) {
                // Camera is available and unlocked, MediaRecorder is prepared,
                // now you can start recording
                mMediaRecorder.start();

                isRecording = true;
                Log.i("ASD", "No release: ");
            } else {
                // prepare didn't work, release the camera
                releaseMediaRecorder();
                Log.i("ASD", "release: ");
                Log.i(TAG, "doInBackground: ");
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                ((Activity) context).finish();
            }
            // inform the user that recording has started
//            setCaptureButtonText("Stop");

        }
    }
}