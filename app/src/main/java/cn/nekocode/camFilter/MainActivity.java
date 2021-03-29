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

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import cn.nekocode.camFilter.filter.CameraFilter;

/**
 * @author nekocode (nekocode.cn@gmail.com)
 */
public class MainActivity extends AppCompatActivity implements GestureDetector.OnGestureListener {
    private static final int REQUEST_CAMERA_PERMISSION = 101;
    public static boolean isAccessCamera = false;
    public static boolean isAccessExternalStorage = false;
    private static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION = 200;
    private FrameLayout container;
    ImageView imageView_takePhoto;
    ImageView imageView_ChangeFacing;
    ImageView imageView5;
    private CameraRenderer renderer;
    private TextureView textureView;
    private int filterId = R.id.filter0;
    private int mCurrentFilterId = 0;
    Typeface typeface_iranSans;
    TextView textViewFilterName;

    String[] TITLES = {"Original", "EdgeDectection", "Pixelize",
            "EMInterference", "TrianglesMosaic", "Legofied",
            "TileMosaic", "Blueorange", "ChromaticAberration",
            "BasicDeform", "Contrast", "NoiseWarp", "Refraction",
            "Mapping", "Crosshatch", "LichtensteinEsque",
            "AsciiArt", "MoneyFilter", "Cracked", "Polygonization",
            "JFAVoronoi", "BlackAndWhite", "Gray", "Negative",
            "Nostalgia", "Casting", "Relief", "Swirl", "HexagonMosaic",
            "Mirror", "Triple", "Cartoon", "WaterReflection"
    };

    Integer[] FILTER_RES_IDS = {R.id.filter0, R.id.filter1, R.id.filter2, R.id.filter3, R.id.filter4,
            R.id.filter5, R.id.filter6, R.id.filter7, R.id.filter8, R.id.filter9, R.id.filter10,
            R.id.filter11, R.id.filter12, R.id.filter13, R.id.filter14, R.id.filter15, R.id.filter16,
            R.id.filter17, R.id.filter18, R.id.filter19, R.id.filter20,
            R.id.filter21, R.id.filter22, R.id.filter23, R.id.filter24,
            R.id.filter25, R.id.filter26, R.id.filter27, R.id.filter28,
            R.id.filter29, R.id.filter30, R.id.filter31, R.id.filter32};

    ArrayList<Integer> mFilterArray = new ArrayList<>(Arrays.asList(FILTER_RES_IDS));

    GestureDetector mGestureDetector;

    public static boolean backFace = true;

    static  float TEXTURE_COORDS[] = {
            1.0f, 0.0f,
            0.0f, 0.0f,
            1.0f, 1.0f,
            0.0f, 1.0f,
    };


    static  float TEXTURE_COORDS_front[] = {
            1.0f, 1.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            0.0f, 0.0f,
    };

    CameraFilter cameraFilter;


    boolean isRecording=false;
    MediaRecorder mMediaRecorder;

    Toolbar toolbar;
    DrawerLayout drawerLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();

        setuptoolbarMainActivity();

//        setTitle(TITLES[mCurrentFilterId]);
        setTitle("CamFilter");
        textViewFilterName.setText(TITLES[mCurrentFilterId]);

        getCameraPermission();

        mGestureDetector = new GestureDetector(this, this);

        imageView_takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                capture();
                Toast.makeText(MainActivity.this, "save", Toast.LENGTH_SHORT).show();
            }
        });

        imageView_ChangeFacing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (backFace) {
                    backFace = false;
                    if (CameraRenderer.camera != null) {
                        CameraRenderer.camera.stopPreview();
                        CameraRenderer.camera.release();
                    }
                    if (CameraRenderer.renderThread != null && CameraRenderer.renderThread.isAlive()) {
                        CameraRenderer.renderThread.interrupt();
                    }
                    CameraFilter.release();

                    CameraFilter.TEXTURE_COORDS=TEXTURE_COORDS;
                    setupCameraPreviewView();


                } else {
                    backFace = true;
                    if (CameraRenderer.camera != null) {
                        CameraRenderer.camera.stopPreview();
                        CameraRenderer.camera.release();
                    }
                    if (CameraRenderer.renderThread != null && CameraRenderer.renderThread.isAlive()) {
                        CameraRenderer.renderThread.interrupt();
                    }
                    CameraFilter.release();

                    CameraFilter.TEXTURE_COORDS=TEXTURE_COORDS_front;
                    setupCameraPreviewView();


                }
            }
        });

//        imageView5.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//
//                renderer.startCapture();
//
//
//            }
//        });

    }


    private void init() {
        container = findViewById(R.id.frameLayoutRootElementMainActivity);
        imageView_takePhoto = findViewById(R.id.imageView5);
        imageView_ChangeFacing = findViewById(R.id.imageView_changeFacing);
        toolbar=findViewById(R.id.toolbarMainActivity);
        drawerLayout=findViewById(R.id.drawerLayoutMainActivity);
//        imageView5=findViewById(R.id.imageView5);
        typeface_iranSans = Typeface.createFromAsset(getAssets(), "fonts/iran_sans.ttf");
        textViewFilterName=findViewById(R.id.textViewFilterName);
    }

    private void setuptoolbarMainActivity() {

        setSupportActionBar(toolbar);
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        ActionBarDrawerToggle drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, 0, 0);
        drawerLayout.addDrawerListener(drawerToggle);
        drawerToggle.syncState();


        for (int i = 0; i < toolbar.getChildCount(); i++) {
            if (toolbar.getChildAt(i) instanceof TextView) {
                ((TextView) toolbar.getChildAt(i)).setTypeface(typeface_iranSans);
                ((TextView) toolbar.getChildAt(i)).setTextSize(18);
            }

        }

//        relativeLayoutHome = navigationView.findViewById(R.id.relativeLayoutBasicPage);
//        relativeLayoutEnter = navigationView.findViewById(R.id.relativeLayoutEnter);
//        relativeLayoutSubjects = navigationView.findViewById(R.id.relativeLayoutSubjects);
//        relativeLayoutMarked = navigationView.findViewById(R.id.relativeLayoutMarked);
//        relativeLayoutAboutUs = navigationView.findViewById(R.id.relativeLayoutAboutUs);
//        relativeLayoutContactUs = navigationView.findViewById(R.id.relativeLayoutContactUs);
//
//        relativeLayoutHome.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                drawerLayout.isDrawerOpen(GravityCompat.START);
//                drawerLayout.closeDrawer(GravityCompat.START);
//            }
//        });
//
//        relativeLayoutEnter.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//
//            }
//        });
//
//        relativeLayoutSubjects.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                Toast.makeText(ActivityMain.this, "موضوعات", Toast.LENGTH_SHORT).show();
//
//            }
//        });
//
//        relativeLayoutMarked.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                Intent intent = new Intent(ActivityMain.this, ActivityMarker.class);
//                startActivity(intent);
//
//            }
//        });
//
//        relativeLayoutAboutUs.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//
//            }
//        });
//
//        relativeLayoutContactUs.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                Intent intentContactUs = new Intent(ActivityMain.this, ActivityContactUs.class);
//                startActivity(intentContactUs);
//
//            }
//        });



    }

    public void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                REQUEST_CAMERA_PERMISSION);
    }


    public void setupCameraPreviewView() {
        renderer = new CameraRenderer(this);
        textureView = new TextureView(this);
        container.addView(textureView);
        textureView.setSurfaceTextureListener(renderer);

//        textureView.setOnTouchListener(this);
        textureView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mGestureDetector.onTouchEvent(motionEvent);
                return true;
            }
        });

        textureView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                renderer.onSurfaceTextureSizeChanged(null, v.getWidth(), v.getHeight());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.filter, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        filterId = item.getItemId();

//        // TODO: need tidy up
//        if (filterId == R.id.capture) {
//            Toast.makeText(this,
//                    capture() ? "The capture has been saved to your sdcard root path." :
//                            "Save failed!",
//                    Toast.LENGTH_SHORT).show();
//            return true;
//        }

//        setTitle(item.getTitle());
//
//        if (renderer != null)
//            renderer.setSelectedFilter(filterId);
//        mCurrentFilterId = mFilterArray.indexOf(filterId);
        return true;
    }

    private boolean capture() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    Toast.makeText(this, "Camera access is required.", Toast.LENGTH_LONG).show();
                    requestCameraPermission();
                } else {
                    requestCameraPermission();
                }

            } else {

                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "require to access media!!", Toast.LENGTH_SHORT).show();
                    getMediaAccessPermission();
                } else {
                    String mPath = genSaveFileName(getTitle().toString() + "_", ".png");
                    File imageFile = new File(mPath);
                    if (imageFile.exists()) {
                        imageFile.delete();
                    }

                    // create bitmap screen capture
                    Bitmap bitmap = textureView.getBitmap();
                    OutputStream outputStream = null;

                    try {
                        outputStream = new FileOutputStream(imageFile);
                        bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream);
                        outputStream.flush();
                        outputStream.close();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        return false;
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }

                }

            }
        } else {

            String mPath = genSaveFileName(getTitle().toString() + "_", ".png");
            File imageFile = new File(mPath);
            if (imageFile.exists()) {
                imageFile.delete();
            }

            // create bitmap screen capture
            Bitmap bitmap = textureView.getBitmap();
            OutputStream outputStream = null;

            try {
                outputStream = new FileOutputStream(imageFile);
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream);
                outputStream.flush();
                outputStream.close();

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return false;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

        }

//        if (ContextCompat.checkSelfPermission(this,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                != PackageManager.PERMISSION_GRANTED) {
//            Toast.makeText(this, "require to access media!!", Toast.LENGTH_SHORT).show();
//        } else if (ContextCompat.checkSelfPermission(this,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                == PackageManager.PERMISSION_GRANTED) {
//
//            String mPath = genSaveFileName(getTitle().toString() + "_", ".png");
//            File imageFile = new File(mPath);
//            if (imageFile.exists()) {
//                imageFile.delete();
//            }
//
//            // create bitmap screen capture
//            Bitmap bitmap = textureView.getBitmap();
//            OutputStream outputStream = null;
//
//            try {
//                outputStream = new FileOutputStream(imageFile);
//                bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream);
//                outputStream.flush();
//                outputStream.close();
//
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//                return false;
//            } catch (IOException e) {
//                e.printStackTrace();
//                return false;
//            }
//
//        }
//        return true;
        return true;

    }



    private String genSaveFileName(String prefix, String suffix) {
        Date date = new Date();
        SimpleDateFormat dateformat1 = new SimpleDateFormat("yyyyMMdd_hhmmss");
        String timeString = dateformat1.format(date);
        String externalPath = Environment.getExternalStorageDirectory().toString();
        return externalPath + "/" + prefix + timeString + suffix;
    }

    @Override
    public boolean onDown(MotionEvent motionEvent) {

        return false;
    }

    @Override
    public void onShowPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent motionEvent) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent motionEvent) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        float velocity = Math.abs(velocityX) > Math.abs(velocityY) ? velocityX : velocityY;
        int step = velocity > 0 ? -1 : 1;
        mCurrentFilterId = circleLoop(TITLES.length, mCurrentFilterId, step);
        textViewFilterName.setText(TITLES[mCurrentFilterId]);
//        setTitle(TITLES[mCurrentFilterId]);
        if (renderer != null) {
            renderer.setSelectedFilter(FILTER_RES_IDS[mCurrentFilterId]);
        }
        return true;
    }

    private int circleLoop(int size, int currentPos, int step) {
        if (step == 0) {
            return currentPos;
        }

        if (step > 0) {
            if (currentPos + step >= size) {
                return (currentPos + step) % size;
            } else {
                return currentPos + step;
            }
        } else {
            if (currentPos + step < 0) {
                return currentPos + step + size;
            } else {
                return currentPos + step;
            }
        }
    }


    public void getCameraPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    Toast.makeText(this, "Camera access is required.", Toast.LENGTH_LONG).show();
                    requestCameraPermission();
                } else {
                    requestCameraPermission();
                }

            } else {
                 setupCameraPreviewView();
            }
        } else {
            setupCameraPreviewView();
        }

    }

    public void getMediaAccessPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // tanks for Access
            } else {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION);
            }
        } else {
            // tanks for Access
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    isAccessCamera = true;
                    setupCameraPreviewView();
                } else {
                    isAccessCamera = false;
                    Toast.makeText(this, "Dont camera permission", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    isAccessExternalStorage = true;
                    Toast.makeText(this, "Acsses to external storage", Toast.LENGTH_SHORT).show();
                } else {
                    isAccessExternalStorage = false;
                    Toast.makeText(this, "Media access is required for storage", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        renderer = null;
        textureView = null;
    }
}
