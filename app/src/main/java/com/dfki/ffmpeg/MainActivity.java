package com.dfki.ffmpeg;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
//import android.support.v4.view.GravityCompat;
//import android.support.v4.widget.DrawerLayout;
//import android.support.v7.app.ActionBarDrawerToggle;
//import android.support.v7.app.AppCompatActivity;
//import android.support.v7.widget.Toolbar;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;


import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.ContentValues;
import android.os.Build;
import android.util.Log;

import com.arthenica.mobileffmpeg.Config;
import com.arthenica.mobileffmpeg.ExecuteCallback;
import com.arthenica.mobileffmpeg.FFmpeg;

import org.jcodec.api.android.AndroidSequenceEncoder;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Rational;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_CANCEL;
import static com.arthenica.mobileffmpeg.Config.RETURN_CODE_SUCCESS;

public class MainActivity extends AppCompatActivity {
    public static final String VIDEO_URL = "video";
    private ImageButton process,slow,keypoints,play,stop;
    private Button select_video;
    private TextView tvLeft,tvRight;
    private ProgressDialog progressDialog;
    private String video_url;
    private VideoView videoView;
    private Runnable r;
    private ProgressBar progressBar;
    private ImageView left_menu_icon;
    private TextView pose_detection;
    private TextView matrices;
    private static final String root= Environment.getExternalStorageDirectory().toString();
    private static final String app_folder=root+"/DSV/";
    private static String framespath = null;
    private static int DIM_FLAG =2;
    private static boolean VIDEO_RATIO_FLAG=true; //flag used to change the video view only once
    CountDownTimer mCountDownTimer = null;


    // load 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
        //System.loadLibrary("opencv-utils");
    }

    /**
     * A native method that is implemented by the 'native-lib' native library.
     */
    public native void  drawKpsAndBbox(Bitmap src, Bitmap bitmapOut, double[] kps_x, double[] kps_y, double kps_min_x,
                                       double kps_min_y, double kps_max_x, double kps_max_y, double[] comxlist, double[] comylist);
    //public native void  drawKpsAndBbox(Bitmap src, Bitmap bitmapOut, double[] kps_x, double[] kps_y);
    //public native String stringFromJNI();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //stringFromJNI();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        progressBar =  findViewById(R.id.progressBar);
        //tvLeft = (TextView) findViewById(R.id.textleft);
        tvRight = (TextView) findViewById(R.id.textright);
        slow = (ImageButton) findViewById(R.id.slow);
        process = (ImageButton) findViewById(R.id.process);
        keypoints = (ImageButton) findViewById(R.id.keypoints);
        select_video = (Button) findViewById(R.id.select_video);
        left_menu_icon= findViewById(R.id.left_menu_icon);
        pose_detection = findViewById(R.id.pose_detection);
        pose_detection.setEnabled(true);
        matrices = findViewById(R.id.matrices);

        videoView=(VideoView) findViewById(R.id.layout_movie_wrapper);
        play = findViewById(R.id.play);
        stop = findViewById(R.id.stop);
        play.setEnabled(false);
        stop.setEnabled(false);
        keypoints.setEnabled(false);

        //creating the progress dialog
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Please wait..");
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);

        DisplayMetrics dm;
        dm=new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int dmheight = dm.heightPixels;
        int dmwidth = dm.widthPixels;

        //set up the onClickListeners

        left_menu_icon.setOnClickListener(new View.OnClickListener() {
            final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            @Override
            public void onClick(View v) {
                if (drawer.isDrawerOpen(GravityCompat.START)) {
                    drawer.closeDrawer(GravityCompat.START);
                } else {
                    drawer.openDrawer(GravityCompat.START);
                }
            }
        });
        pose_detection.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                videoView.stopPlayback();
                if(mCountDownTimer!=null) {
                    mCountDownTimer.cancel();
                }
                callPoseDetectionOnVideoActivity();
                return false;
            }
        });

        matrices.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                videoView.stopPlayback();
                if(mCountDownTimer!=null) {
                    mCountDownTimer.cancel();
                }
                callMyTableActivity();
                return false;
            }
        });

        select_video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCountDownTimer!=null) {
                    mCountDownTimer.cancel();
                }
                //create an intent to retrieve the video file from the device storage
                Intent intent = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                intent.setType("video/*");
                startActivityForResult(intent, 123);
                play.setEnabled(true);
                stop.setEnabled(false);
            }
        });

        play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                videoView.setVideoURI(Uri.parse(video_url));
                videoView.start();
                //startActivityForResult(intent, 123);
            }
        });

        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                videoView.setVideoURI(Uri.parse(video_url));
                videoView.stopPlayback();
                //startActivityForResult(intent, 123);
            }
        });

        slow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                    check if the user has selected any video or not
                    In case a user hasen't selected any video and press the button,
                    we will show an warning, stating "Please upload the video"
                 */
                if (video_url != null) {
                    //a try-catch block to handle all necessary exceptions like File not found, IOException
                    try {
                        //slowmotion(rangeSeekBar.getSelectedMinValue().intValue() * 1000, rangeSeekBar.getSelectedMaxValue().intValue() * 1000);
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                } else
                    Toast.makeText(MainActivity.this, "Please upload video", Toast.LENGTH_SHORT).show();
            }
        });

        process.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (video_url != null) {
                    try {
                        //reverse(rangeSeekBar.getSelectedMinValue().intValue() * 1000, rangeSeekBar.getSelectedMaxValue().intValue() * 1000);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                    }
                } else
                    Toast.makeText(MainActivity.this, "Please select video", Toast.LENGTH_SHORT).show();
            }
        });




        /*
            set up the VideoView.
            We will be using VideoView to view our video.
         */
        videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {

            @Override
            public void onPrepared(MediaPlayer mp) {
                play.setEnabled(false);
                stop.setEnabled(true);
                keypoints.setEnabled(true);

                //get the duartion of the video
                int duration = mp.getDuration() / 1000;
                mp.setLooping(false);
                mp.setVolume(0,0);


                /**
                 * TO CHANGE VIDEO VIEW DIMS
                 */
                if(VIDEO_RATIO_FLAG) {
                    RelativeLayout.LayoutParams videoViewParam;
                    int height = (int) ((videoView.getHeight() / videoView.getWidth()) * videoView.getWidth());
                    videoViewParam = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, height);
                    videoViewParam.addRule(RelativeLayout.CENTER_IN_PARENT);
                    videoView.setLayoutParams(videoViewParam);
                    VIDEO_RATIO_FLAG=false;
                }
                ProgressBar mProgressBar;

                final int[] i = {0};
                final String[] time = new String[1];
                final String[] newTime = new String[1];

                mProgressBar=findViewById(R.id.progressBar);
                mProgressBar.setProgress(i[0]);
                final int[] milli = new int[1];

                mCountDownTimer=new CountDownTimer(mp.getDuration(),100) {
                    //long millisUntilFinished = 1000;
                    @Override
                    public void onTick(long millisUntilFinished) {
                        if(i[0]==0){
                            milli[0] = (int) millisUntilFinished;
                        }
                        Log.v("Log_tag", "Tick of Progress "+ i[0] + " "+millisUntilFinished);
                        i[0]++;
                        //time[0] = String.valueOf(mp.getCurrentPosition());
                        time[0]=String.valueOf(mp.getDuration()-millisUntilFinished);
                        System.out.println("time : "+ time[0]);
                        if (Integer.parseInt(time[0])==0 || Integer.parseInt(time[0])==1){
                            newTime[0] = "00:00";
                        }//if(time[0].length()<2 && Integer.parseInt(time[0])!=0){
                            //newTime[0] = String.valueOf(Integer.parseInt(String.valueOf(time[0].charAt(0))));
                        //}
                        if(time[0].length()==2 && Integer.parseInt(time[0])!=0){
                            newTime[0] = "00:"+String.valueOf(Integer.parseInt(String.valueOf(time[0].charAt(0))))
                                    + String.valueOf(Integer.parseInt(String.valueOf(time[0].charAt(1))));
                        }
                        if(time[0].length()==3 && Integer.parseInt(time[0])!=0){
                            newTime[0] = String.valueOf(Integer.parseInt(String.valueOf(time[0].charAt(0))))+
                                     String.valueOf(Integer.parseInt(String.valueOf(time[0].charAt(1))))+":"+
                                    String.valueOf(Integer.parseInt(String.valueOf(time[0].charAt(2))));
                        }
                        if(time[0].length()==4 && Integer.parseInt(time[0])!=0){
                            newTime[0] = String.valueOf(Integer.parseInt(String.valueOf(time[0].charAt(0))))+":"
                                    + String.valueOf(Integer.parseInt(String.valueOf(time[0].charAt(1))))+
                                    String.valueOf(Integer.parseInt(String.valueOf(time[0].charAt(2)))) + ":"+
                                    String.valueOf(Integer.parseInt(String.valueOf(time[0].charAt(3))));
                        }if(time[0].length()==5){
                             newTime[0] = String.valueOf(Integer.parseInt(String.valueOf(time[0].charAt(0))))
                                     + String.valueOf(Integer.parseInt(String.valueOf(time[0].charAt(1))))+
                                     ":"+
                                     String.valueOf(Integer.parseInt(String.valueOf(time[0].charAt(2)))) +
                                     String.valueOf(Integer.parseInt(String.valueOf(time[0].charAt(3)))) +
                                     ":"+
                                    String.valueOf(Integer.parseInt(String.valueOf(time[0].charAt(4))));
                        }if(time[0].length()==6){
                            newTime[0] = String.valueOf(Integer.parseInt(String.valueOf(time[0].charAt(0))))
                                    + String.valueOf(Integer.parseInt(String.valueOf(time[0].charAt(1))))+
                                    ":"+
                                    String.valueOf(Integer.parseInt(String.valueOf(time[0].charAt(2)))) +
                                    String.valueOf(Integer.parseInt(String.valueOf(time[0].charAt(3)))) +
                                    ":"+
                                    String.valueOf(Integer.parseInt(String.valueOf(time[0].charAt(4)))) +
                                    String.valueOf(Integer.parseInt(String.valueOf(time[0].charAt(5))));
                        }
                        mProgressBar.setProgress(((int) i[0]*100/(mp.getDuration()/100))+5);

                        tvRight.setText(newTime[0]);
                        stop.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                //videoView.setVideoURI(Uri.parse(video_url));
                                videoView.stopPlayback();
                                cancel();
                                play.setEnabled(true);
                            }
                        });

                        keypoints.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                if (video_url != null) {
                                    ArrayList<Bitmap> bm = null;

                                    try {
                                        videoView.stopPlayback();
                                        cancel();
                                        bm =plotBboxKps();

                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                                    }
                                } else
                                    Toast.makeText(MainActivity.this, "Please select video", Toast.LENGTH_SHORT).show();
                            }
                        });


                    }

                    @Override
                    public void onFinish() {
                        //Do what you want
                        i[0]++;
                        mp.pause();
                        //tvRight.setText(":00");
                        mProgressBar.setProgress(100);
                        play.setEnabled(true);
                        stop.setEnabled(true);
                    }
                };
                mCountDownTimer.start();
                //this method changes the right TextView every 1 second as the video is being played
                //It works same as a time counter we see in any Video Player
                final Handler handler = new Handler();
                handler.postDelayed(r = new Runnable() {
                    @Override
                    public void run() {
                        handler.postDelayed(r, 1000);
                    }
                }, 1000);

            }
        });
    }


    /**
     * Method for creating fast motion video
     */
    private void fastforward(int startMs, int endMs) throws Exception {
          /* startMs is the starting time, from where we have to apply the effect.
  	         endMs is the ending time, till where we have to apply effect.
   	         For example, we have a video of 5min and we only want to fast forward a part of video
  	         say, from 1:00 min to 2:00min, then our startMs will be 1000ms and endMs will be 2000ms.
		 */

        //create a progress dialog and show it until this method executes.
        progressDialog.show();

        //creating a new file in storage
        final String filePath;
        String filePrefix = "fastforward";
        String fileExtn = ".mp4";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            /*
            With introduction of scoped storage in Android Q the primitive method gives error
            So, it is recommended to use the below method to create a video file in storage.
             */
            ContentValues valuesvideos = new ContentValues();
            valuesvideos.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + "Folder");
            valuesvideos.put(MediaStore.Video.Media.TITLE, filePrefix+System.currentTimeMillis());
            valuesvideos.put(MediaStore.Video.Media.DISPLAY_NAME, filePrefix+System.currentTimeMillis()+fileExtn);
            valuesvideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            valuesvideos.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
            valuesvideos.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());
            Uri uri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, valuesvideos);

            //get the path of the video file created in the storage.
            File file=FileUtils.getFileFromUri(this,uri);
            filePath=file.getAbsolutePath();

        }else {
            //This else statement will work for devices with Android version lower than 10
            //Here, "app_folder" is the path to your app's root directory in device storage
            File dest = new File(new File(app_folder), filePrefix + fileExtn);
            int fileNo = 0;
            //check if the file name previously exist. Since we don't want to oerwrite the video files
            while (dest.exists()) {
                fileNo++;
                dest = new File(new File(app_folder), filePrefix + fileNo + fileExtn);
            }
            //Get the filePath once the file is successfully created.
            filePath = dest.getAbsolutePath();
        }
        String exe;
        //the "exe" string contains the command to process video.The details of command are discussed later in this post.
        // "video_url" is the url of video which you want to edit. You can get this url from intent by selecting any video from gallery.
        exe="-y -i " +video_url+" -filter_complex [0:v]trim=0:"+startMs/1000+",setpts=PTS-STARTPTS[v1];[0:v]trim="+startMs/1000+":"+endMs/1000+",setpts=0.5*(PTS-STARTPTS)[v2];[0:v]trim="+(endMs/1000)+",setpts=PTS-STARTPTS[v3];[0:a]atrim=0:"+(startMs/1000)+",asetpts=PTS-STARTPTS[a1];[0:a]atrim="+(startMs/1000)+":"+(endMs/1000)+",asetpts=PTS-STARTPTS,atempo=2[a2];[0:a]atrim="+(endMs/1000)+",asetpts=PTS-STARTPTS[a3];[v1][a1][v2][a2][v3][a3]concat=n=3:v=1:a=1 "+"-b:v 2097k -vcodec mpeg4 -crf 0 -preset superfast "+filePath;

        /*
            Here, we have used he Async task to execute our query because if we use the regular method the progress dialog
            won't be visible. This happens because the regular method and progress dialog uses the same thread to execute
            and as a result only one is a allowed to work at a time.
            By using we Async task we create a different thread which resolves the issue.
         */
        long executionId = FFmpeg.executeAsync(exe, new ExecuteCallback() {

            @Override
            public void apply(final long executionId, final int returnCode) {
                if (returnCode == RETURN_CODE_SUCCESS) {
                    //after successful execution of ffmpeg command,
                    //again set up the video Uri in VideoView
                    videoView.setVideoURI(Uri.parse(filePath));
                    //change the video_url to filePath, so that we could do more manipulations in the
                    //resultant video. By this we can apply as many effects as we want in a single video.
                    //Actually there are multiple videos being formed in storage but while using app it
                    //feels like we are doing manipulations in only one video
                    video_url = filePath;
                    //play the result video in VideoView
                    videoView.start();
                    //remove the progress dialog
                    progressDialog.dismiss();
                } else if (returnCode == RETURN_CODE_CANCEL) {
                    Log.i(Config.TAG, "Async command execution cancelled by user.");
                } else {
                    Log.i(Config.TAG, String.format("Async command execution failed with returnCode=%d.", returnCode));
                }
            }
        });
    }

    /**
     Method for creating slow motion video for specific part of the video
     The below code is same as above only the command in string "exe" is changed.
     */
    private void slowmotion(int startMs, int endMs) throws Exception {

        progressDialog.show();

        final String filePath;
        String filePrefix = "slowmotion";
        String fileExtn = ".mp4";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            ContentValues valuesvideos = new ContentValues();
            valuesvideos.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + "Folder");
            valuesvideos.put(MediaStore.Video.Media.TITLE, filePrefix+System.currentTimeMillis());
            valuesvideos.put(MediaStore.Video.Media.DISPLAY_NAME, filePrefix+System.currentTimeMillis()+fileExtn);
            valuesvideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            valuesvideos.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
            valuesvideos.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());
            Uri uri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, valuesvideos);
            File file=FileUtils.getFileFromUri(this,uri);
            filePath=file.getAbsolutePath();

        }else {

            File dest = new File(new File(app_folder), filePrefix + fileExtn);
            int fileNo = 0;
            while (dest.exists()) {
                fileNo++;
                dest = new File(new File(app_folder), filePrefix + fileNo + fileExtn);
            }
            filePath = dest.getAbsolutePath();
        }
        String exe;
        exe="-y -i " +video_url+" -filter_complex [0:v]trim=0:"+startMs/1000+",setpts=PTS-STARTPTS[v1];[0:v]trim="+startMs/1000+":"+endMs/1000+",setpts=2*(PTS-STARTPTS)[v2];[0:v]trim="+(endMs/1000)+",setpts=PTS-STARTPTS[v3];[0:a]atrim=0:"+(startMs/1000)+",asetpts=PTS-STARTPTS[a1];[0:a]atrim="+(startMs/1000)+":"+(endMs/1000)+",asetpts=PTS-STARTPTS,atempo=0.5[a2];[0:a]atrim="+(endMs/1000)+",asetpts=PTS-STARTPTS[a3];[v1][a1][v2][a2][v3][a3]concat=n=3:v=1:a=1 "+"-b:v 2097k -vcodec mpeg4 -crf 0 -preset superfast "+filePath;

        long executionId = FFmpeg.executeAsync(exe, new ExecuteCallback() {

            @Override
            public void apply(final long executionId, final int returnCode) {
                if (returnCode == RETURN_CODE_SUCCESS) {

                    videoView.setVideoURI(Uri.parse(filePath));
                    video_url = filePath;
                    videoView.start();
                    progressDialog.dismiss();

                } else if (returnCode == RETURN_CODE_CANCEL) {
                    Log.i(Config.TAG, "Async command execution cancelled by user.");
                } else {
                    Log.i(Config.TAG, String.format("Async command execution failed with returnCode=%d.", returnCode));
                }
            }
        });
    }

    /**
     * Method for reversing the video
     */
    /*
	The below code is same as above only the command is changed.
*/
    private void reverse(int startMs, int endMs) throws Exception {

        progressDialog.show();
        String filePrefix = "reverse";
        String fileExtn = ".mp4";

        final String filePath;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            ContentValues valuesvideos = new ContentValues();
            valuesvideos.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + "Folder");
            valuesvideos.put(MediaStore.Video.Media.TITLE, filePrefix+System.currentTimeMillis());
            valuesvideos.put(MediaStore.Video.Media.DISPLAY_NAME, filePrefix+System.currentTimeMillis()+fileExtn);
            valuesvideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            valuesvideos.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
            valuesvideos.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());
            Uri uri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, valuesvideos);
            File file=FileUtils.getFileFromUri(this,uri);
            filePath=file.getAbsolutePath();

        }else{
            filePrefix = "reverse";
            fileExtn = ".mp4";
            File dest = new File(new File(app_folder), filePrefix + fileExtn);
            int fileNo = 0;
            while (dest.exists()) {
                fileNo++;
                dest = new File(new File(app_folder), filePrefix + fileNo + fileExtn);
            }
            filePath = dest.getAbsolutePath();
        }

        long executionId = FFmpeg.executeAsync("-y -i " + video_url + " -filter_complex [0:v]trim=0:" + endMs / 1000 + ",setpts=PTS-STARTPTS[v1];[0:v]trim=" + startMs / 1000 + ":" + endMs / 1000 + ",reverse,setpts=PTS-STARTPTS[v2];[0:v]trim=" + (startMs / 1000) + ",setpts=PTS-STARTPTS[v3];[v1][v2][v3]concat=n=3:v=1 " + "-b:v 2097k -vcodec mpeg4 -crf 0 -preset superfast " + filePath, new ExecuteCallback() {

            @Override
            public void apply(final long executionId, final int returnCode) {
                if (returnCode == RETURN_CODE_SUCCESS) {
                    videoView.setVideoURI(Uri.parse(filePath));
                    video_url = filePath;
                    videoView.start();
                    progressDialog.dismiss();
                } else if (returnCode == RETURN_CODE_CANCEL) {
                    Log.i(Config.TAG, "Async command execution cancelled by user.");
                } else {
                    Log.i(Config.TAG, String.format("Async command execution failed with returnCode=%d.", returnCode));
                }
            }
        });
    }



    //Overriding the method onActivityResult() to get the video Uri form intent.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            if (requestCode == 123) {

                if (data != null) {
                    //get the video Uri
                    Uri uri = data.getData();
                    try {
                        //get the file from the Uri using getFileFromUri() methid present in FileUils.java
                        File video_file = FileUtils.getFileFromUri(this, uri);
                        //now set the video uri in the VideoView
                        videoView.setVideoURI(uri);
                        //after successful retrieval of the video and properly setting up the retried video uri in
                        //VideoView, Start the VideoView to play that video
                        videoView.start();
                        //get the absolute path of the video file. We will require this as an input argument in
                        //the ffmpeg command.
                        video_url=video_file.getAbsolutePath();
                    } catch (Exception e) {
                        Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }


                }
            }
        }
    }

    //This method returns the seconds in hh:mm:ss time format
    private String getTime(int seconds) {
        int hr = seconds / 3600;
        int rem = seconds % 3600;
        int mn = rem / 60;
        int sec = rem % 60;
        return String.format("%02d", hr) + ":" + String.format("%02d", mn) + ":" + String.format("%02d", sec);
    }

    @Override
    public void onResume(){
        super.onResume();
        //videoView.setVideoURI(Uri.parse(video_url));
        videoView.start();

    }

    /**
     * Method for plotting bounding box and keypoints on the video
     * @return
     */
    private ArrayList<Bitmap> plotBboxKps() throws Exception {

        progressDialog.show();
        String filePrefix = "bboxkps";
        String fileExtn = ".mp4";
        //String frames_fol= MediaStore.Video.Media.RELATIVE_PATH+"Movies/" + "Frames";

        final String filePath;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            ContentValues valuesvideos = new ContentValues();
            valuesvideos.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/" + "Folder/"+filePrefix);
            valuesvideos.put(MediaStore.Video.Media.TITLE, filePrefix+System.currentTimeMillis());
            valuesvideos.put(MediaStore.Video.Media.DISPLAY_NAME, filePrefix+System.currentTimeMillis()+fileExtn);
            valuesvideos.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            valuesvideos.put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
            valuesvideos.put(MediaStore.Video.Media.DATE_TAKEN, System.currentTimeMillis());
            Uri uri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, valuesvideos);
            File file=FileUtils.getFileFromUri(this,uri);
            filePath=file.getAbsolutePath();

        }else{
            filePrefix = "bboxkps";

            fileExtn = ".mp4";
            File dest = new File(new File(app_folder), filePrefix + fileExtn);
            int fileNo = 0;
            while (dest.exists()) {
                fileNo++;
                dest = new File(new File(app_folder), filePrefix + fileNo + fileExtn);
            }
            filePath = dest.getAbsolutePath();

        }
        String newFilepath = filePath.split(filePrefix)[0];
        framespath = "-y -i "+video_url+" "+"-vf scale=\"iw/"+DIM_FLAG+":ih/"+DIM_FLAG+"\" "+newFilepath+filePrefix;
        final ArrayList<Bitmap>[] bitmaplist = new ArrayList[]{new ArrayList<Bitmap>()};
        String finalFilePrefix = filePrefix;
        long exeId = FFmpeg.executeAsync(framespath+"/pair_1_dslr_%05d.jpg "+"-preset superfast -threads 8 ", new ExecuteCallback() {

            @Override
            public void apply(final long exeId, final int returnCode) {
                if (returnCode == RETURN_CODE_SUCCESS) {
                    System.out.println("########################### Executed xtraction ########################## ");
                    try {
                        getPlottedBitmaps(newFilepath,finalFilePrefix);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //videoView.setVideoURI(Uri.parse(filePath));
                    //video_url = filePath;
                    //videoView.start();
                    progressDialog.dismiss();

                } else if (returnCode == RETURN_CODE_CANCEL) {
                    Log.i(Config.TAG, "Async command execution cancelled by user.");
                } else {
                    Log.i(Config.TAG, String.format("Async command execution failed with returnCode=%d.", returnCode));
                }
            }
        });
        return bitmaplist[0];

    }

    /**
     *
     * @param path : path of extracted frames folder
     * @throws IOException
     */
    private void getPlottedBitmaps(String path, String prefix) throws IOException {
        String jsonFileString= Utils.getJsonFromAssets(getApplicationContext(), "pair_1_dslr.json");
        // initialise kps and bbox arrays
        double [] kps_int_x = new double[16];
        double[] kpsx_array = new double[16];
        double[] kpsy_array = new double[16];
        final double [] kps_int_y = new double[16];
        final double[] bbox_int = new double[4];
        final JSONArray[] bbox = new JSONArray[1];
        final JSONArray[] kps = new JSONArray[1];

        File f=new File(path+prefix);
        File[] list = f.listFiles();

        List<File> newList = Arrays.stream(list).sorted().collect(Collectors.toList());
        ArrayList<Bitmap> bitmapimages = new ArrayList<Bitmap>();
        double[] comxlist= new double[116];
        double[] comylist= new double[116];

        int comitr=0;
        for(int i=1120;i<1350;i+=2){
            Bitmap bitmapIn = BitmapFactory.decodeStream(new FileInputStream(newList.get(i)));
            Bitmap bitmapOut = Bitmap.createBitmap(bitmapIn.getWidth(), bitmapIn.getHeight(),bitmapIn.getConfig());

            JSONObject data = Utils.getDataAtFrame(jsonFileString, i);
            JSONObject pose = Utils.getposeDataAtFrame(jsonFileString, i);
            try {
                int frame_num = (int) data.get("frame-num");
                bbox[0] = (JSONArray) data.get("bbox");
                kps[0] = (JSONArray) pose.get("kps");
                JSONArray com = (JSONArray) pose.get("com");
                bbox_int[0]=(int) bbox[0].get(0);
                bbox_int[1]=(int) bbox[0].get(1);
                bbox_int[2]=(int) bbox[0].get(2);
                bbox_int[3]=(int) bbox[0].get(3);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            for (int kpsitr = 0; kpsitr< kps[0].length();kpsitr++){
                try {
                    JSONArray kps_json = (JSONArray) kps[0].get(kpsitr);
                    kps_int_x[kpsitr] = (double) kps_json.get(0)/DIM_FLAG;
                    kps_int_y[kpsitr] = (double) kps_json.get(1)/DIM_FLAG;
                    kpsx_array[kpsitr] = (double) kps_json.get(0)/DIM_FLAG;
                    kpsy_array[kpsitr] = (double) kps_json.get(1)/DIM_FLAG;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            Arrays.sort(kpsx_array);
            Arrays.sort(kpsy_array);
            double kps_min_x = kpsx_array[0];
            double kps_min_y = kpsy_array[0];
            double kps_max_x = kpsx_array[kpsx_array.length-1];
            double kps_max_y = kpsy_array[kpsy_array.length-1];


            //############ drawKpsAndBbox #########
            comxlist[comitr]=Arrays.stream(kpsx_array).sum()/kpsx_array.length;
            comylist[comitr]=Arrays.stream(kpsy_array).sum()/kpsy_array.length;
            comitr++;
            drawKpsAndBbox(bitmapIn,bitmapOut, kps_int_x, kps_int_y, kps_min_x, kps_min_y, kps_max_x,kps_max_y,comxlist,comylist);
            bitmapimages.add(bitmapOut);
        }
        SeekableByteChannel out = null;
        try {
            out = NIOUtils.writableFileChannel(path+"/Processed.mp4");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        AndroidSequenceEncoder se = new AndroidSequenceEncoder(out, Rational.R1(25));
        // creating video with sequence encoder
        File x = new File(path+"/Processed.mp4");
        try {
            se.createSequenceEncoder(x,25);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for(int vd=0; vd<bitmapimages.size(); vd++){
            try {
                se.encodeImage(bitmapimages.get(vd));
            } catch (IOException e) {
                e.printStackTrace();
            }
            //vd+=2;
        }
        try {
            se.finish();
        } catch (IOException e) {
            e.printStackTrace();
        }
        videoView.setVideoURI(Uri.parse(path+"/Processed.mp4"));
        videoView.start();
        video_url= path+"/Processed.mp4";
    }

    private void setVideoSize(MediaPlayer mediaPlayer, SurfaceView surfaceView) {

        // // Get the dimensions of the video
        int videoWidth = mediaPlayer.getVideoWidth();
        int videoHeight = mediaPlayer.getVideoHeight();
        float videoProportion = (float) videoWidth / (float) videoHeight;

        // Get the width of the screen
        int screenWidth = getWindowManager().getDefaultDisplay().getWidth();
        int screenHeight = getWindowManager().getDefaultDisplay().getHeight();
        float screenProportion = (float) screenWidth / (float) screenHeight;

        // Get the SurfaceView layout parameters
        android.view.ViewGroup.LayoutParams lp = surfaceView.getLayoutParams();
        if (videoProportion > screenProportion) {
            lp.width = screenWidth;
            lp.height = (int) ((float) screenWidth / videoProportion);
        } else {
            lp.width = (int) (videoProportion * (float) screenHeight);
            lp.height = screenHeight;
        }
        // Commit the layout parameters
        surfaceView.setLayoutParams(lp);
    }
    private void callPoseDetectionOnVideoActivity(){
        Intent intent = new Intent(this, PoseDetectionOnVideoActivity.class);
        if(video_url==null){
            Context context = getApplicationContext();
            CharSequence text = "Please Select Video";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
        }
        else {
            intent.putExtra(VIDEO_URL, video_url);
            startActivity(intent);
        }
    }
private void callMyTableActivity(){
        Intent intent = new Intent(this, MyTableActivity.class);
        startActivity(intent);
    }
}