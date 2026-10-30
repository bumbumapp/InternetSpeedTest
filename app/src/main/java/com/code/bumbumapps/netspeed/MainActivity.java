package com.code.bumbumapps.netspeed;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import com.code.bumbumapps.netspeed.CustomAdapter.MyDatabaseHelper;
import com.code.bumbumapps.netspeed.Speed.DevilDownloadTest;
import com.code.bumbumapps.netspeed.Speed.HttpUploadTest;
import com.code.bumbumapps.netspeed.Speed.PingTest;
import com.code.bumbumapps.netspeed.schreenshot.ScreenshotUtil;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.jtv7.rippleswitchlib.RippleSwitch;

import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.model.XYSeries;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements RippleSwitch.OnCheckedChangeListener {
    static int position = 0;
    static int lastPosition = 0;
    GetSpeedTestHostsHandler getSpeedTestHostsHandler = null;
    HashSet<String> tempBlackList;
    private TextView hostname, locationname, type_of_network;
    private TextView fping, fdownload, fupload, mhead, mdatametartype;
    private Typeface type;
    private TextView pingTextView, downloadTextView, uploadTextView;
    private Button startButton,ratingme;
    private RippleSwitch rs;
    private ImageView speedface;
    private Bitmap bitmap;
    private static final int APP_PERMISSION_REQUEST = 102;
    private DecimalFormat dec;
    private DecimalFormat decmbs;
    boolean doubleBackToExitPressedOnce = false;
    int timer = 0;
    ServersPreference serversPreference;
    HashMap<Integer, List<String>> mapValue ;
    Handler handler=new Handler();
    Runnable runnable;
    int findServerIndex = 0;

    Handler handlerd=new Handler();
    @Override
    public void onResume() {
        super.onResume();
        getSpeedTestHostsHandler = new GetSpeedTestHostsHandler();
        getSpeedTestHostsHandler.start();
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_speed_layout);

        serversPreference=new ServersPreference(this);
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true);
        /** UMP consent first; ads are initialised and requested only after canRequestAds(). */
        final android.view.ViewGroup adContainer = findViewById(R.id.adView);
        AdsHelper.gatherConsent(this, () -> {
            AdsHelper.loadBanner(this, adContainer, getString(R.string.banner_id));
            AdsHelper.preloadInterstitial(this, getString(R.string.interstial_id));
        });


        /**startButton*/
        startButton = (Button) findViewById(R.id.startButton);
        ratingme = findViewById(R.id.thisapprating);
        /**startButton*/
        ratingme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this,History.class));
            }
        });

        /**speed view image*/
        speedface = findViewById(R.id.imageView);
        /**speed view image*/


        dec = new DecimalFormat("#.##");
        decmbs = new DecimalFormat("#.###");

        tempBlackList = new HashSet<>();
        getSpeedTestHostsHandler = new GetSpeedTestHostsHandler();
        getSpeedTestHostsHandler.start();


        /**host , location , ping , textview*/
        hostname = findViewById(R.id.host);
        locationname = findViewById(R.id.location);
        pingTextView = (TextView) findViewById(R.id.pingTextView);
        /**host , location , ping , datametar type textview*/

        /**network type view , main head, datametar type   textview*/
        type_of_network = findViewById(R.id.type);
        mhead = findViewById(R.id.heder);
        mdatametartype = findViewById(R.id.datametartype);
        /**network type view , main head ,datametar type   textview*/

        /**upload , download textView/**/
        downloadTextView = (TextView) findViewById(R.id.downloadTextView);
        uploadTextView = (TextView) findViewById(R.id.uploadTextView);
        /**upload , download /**/


        /**set custom font*/

        /**set custom font id's*/
//        fhost = findViewById(R.id.fhost); /**Host name textview*/
        fping = findViewById(R.id.fping); /**ping name textview*/
//        flocation = findViewById(R.id.flocation); /**location name textview*/
        fdownload = findViewById(R.id.textView2); /**download name textview*/
        fupload = findViewById(R.id.textView3); /**upload name textview*/
        /**set custom font id's*/

        /**set custom font typeface*/

        /** Custom type face install */
        // Bento type: Plus Jakarta Sans (bold for values and the title).
        type = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.jakarta_semibold);
        Typeface heavy = androidx.core.content.res.ResourcesCompat.getFont(this, R.font.jakarta_extrabold);
        /** Custom type face install */

//        fhost.setTypeface(type); /**Host name textview set type face*/
        fping.setTypeface(type); /**ping name  textview set type face*/
//        flocation.setTypeface(type); /**location name textview set type face*/
        fdownload.setTypeface(type); /** download name textview set type face*/
        fupload.setTypeface(type); /** upload name textview set type face*/

        type_of_network.setTypeface(type);/**network type  textview  set type face*/
        mhead.setTypeface(heavy);/**main head  textview  set type face*/
        mdatametartype.setTypeface(type); /**datametar type set type face*/

        downloadTextView.setTypeface(heavy);/**download textView  set type face/**/
        uploadTextView.setTypeface(heavy);/**upload textView  set type face/**/

        hostname.setTypeface(type);/**host  set type face*/
        hostname.setPaintFlags(hostname.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        locationname.setTypeface(type);/**location  set type face*/
        pingTextView.setTypeface(type);/**ping  set type face*/

        startButton.setTypeface(type); /**startButton  set type face*/

        /**set custom font typeface*/

        /**set custom font*/


        /**rippleSwitch*/
        rs = findViewById(R.id.rippleSwitch);
        rs.setOnCheckedChangeListener(MainActivity.this);
        /**rippleSwitch*/

        hostname.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this,ServersActivity.class));
            }
        });



        /**get network type*/
        ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) { // connected to the internet
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                type_of_network.setText("WIFI");
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                type_of_network.setText("MOBILE");
            } else {
                type_of_network.setText("OFF");
            }
        }
        /**get network type*/
        if (!serversPreference.getBestServer()){
            runnable=new Runnable() {
                @Override
                public void run() {
                    mapValue = getSpeedTestHostsHandler.getMapValue();
                    if (mapValue!=null){
                        if(mapValue.size()>=serversPreference.getIntValue()){
                            startButton.setEnabled(true);
                            hostname.setEnabled(true);
                            hostname.setText(mapValue.get(serversPreference.getIntValue()).get(5));
                            locationname.setText(mapValue.get(serversPreference.getIntValue()).get(3));

                        }
                        handler.postDelayed(runnable,2000);
                    }



                }

            };
            handler.postDelayed(runnable,2000);
        }

        else{
            HashMap<Integer, String> mapKey = getSpeedTestHostsHandler.getMapKey();

            double selfLat = getSpeedTestHostsHandler.getSelfLat();
            double selfLon = getSpeedTestHostsHandler.getSelfLon();
            double tmp = 19349458;
            double dist = 0.0;

            for (int index : mapKey.keySet()) {


                Location source = new Location("Source");
                source.setLatitude(selfLat);
                source.setLongitude(selfLon);

                List<String> ls = mapValue.get(index);
                Location dest = new Location("Dest");
                dest.setLatitude(Double.parseDouble(ls.get(0)));
                dest.setLongitude(Double.parseDouble(ls.get(1)));

                double distance = source.distanceTo(dest);
                if (tmp > distance) {
                    tmp = distance;
                    dist = distance;
                    findServerIndex = index;
                }
            }

            handlerd.postDelayed(new Runnable() {
                @Override
                public void run() {
                    startButton.setEnabled(true);
                    hostname.setEnabled(true);
                    mapValue=getSpeedTestHostsHandler.getMapValue();
                    hostname.setText(mapValue.get(findServerIndex).get(5));
                    locationname.setText(mapValue.get(findServerIndex).get(3));
                }
            },2000);
        }





        startButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {

                startButton.setText("Running");

                mainrun();




            }
        });
    }

    public void mainrun(){
        startButton.setEnabled(false);

        if (getSpeedTestHostsHandler == null) {
            getSpeedTestHostsHandler = new GetSpeedTestHostsHandler();
            getSpeedTestHostsHandler.start();
        }

        new Thread(new Runnable() {
            RotateAnimation rotate;
            ImageView barImageView = (ImageView) findViewById(R.id.barImageView);


            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mhead.setText("Selecting best server");
                    }
                });

                //Get egcodes.speedtest hosts
                int timeCount = 600; //1min
                while (!getSpeedTestHostsHandler.isFinished()) {
                    timeCount--;
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                    }
                    if (timeCount <= 0) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "No Connection...", Toast.LENGTH_LONG).show();
                                startButton.setEnabled(true);
                                startButton.setText("Restart Test");
                                try {
                                    mhead.setText(R.string.app_name);
                                } catch (Exception O) {
                                }
                            }
                        });
                        getSpeedTestHostsHandler = null;
                        return;
                    }
                }

                //Find closest server
                HashMap<Integer, String> mapKey = getSpeedTestHostsHandler.getMapKey();

                Log.d("TAG","Errsst"+tempBlackList.size());
                double dist = 0.0;
                String testAddr = mapKey.get(0).replace("http://", "https://");
                final List<String> info = mapValue.get(serversPreference.getIntValue());

                final double distance = dist;

                if (info == null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mhead.setText("There was a problem");
                        }
                    });
                    return;
                }




                //Reset value, graphics
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        pingTextView.setText("0 ms");
                        if (rs.isChecked()) {
                            downloadTextView.setText("0 MB/s");
                            uploadTextView.setText("0 MB/s");
                        } else {
                            downloadTextView.setText("0 Mbps");
                            uploadTextView.setText("0 Mbps");
                        }

                    }
                });
                final List<Double> pingRateList = new ArrayList<>();
                final List<Double> downloadRateList = new ArrayList<>();
                final List<Double> uploadRateList = new ArrayList<>();
                Boolean pingTestStarted = false;
                Boolean pingTestFinished = false;
                Boolean downloadTestStarted = false;
                Boolean downloadTestFinished = false;
                Boolean uploadTestStarted = false;
                Boolean uploadTestFinished = false;

                //Init Test
                final PingTest pingTest = new PingTest(info.get(6).replace(":8080", ""), 3);
                final DevilDownloadTest downloadTest = new DevilDownloadTest(testAddr.replace(testAddr.split("/")[testAddr.split("/").length - 1], ""));
                final HttpUploadTest uploadTest = new HttpUploadTest(testAddr);


                //Tests
                while (true) {
                    if (!pingTestStarted) {
                        pingTest.start();
                        pingTestStarted = true;
                    }
                    if (pingTestFinished && !downloadTestStarted) {
                        downloadTest.start();
                        downloadTestStarted = true;
                    }
                    if (downloadTestFinished && !uploadTestStarted) {
                        uploadTest.start();
                        uploadTestStarted = true;
                    }


                    //Ping Test
                    if (pingTestFinished) {
                        //Failure
                        if (pingTest.getAvgRtt() == 0) {
                            System.out.println("Ping error...");
                        } else {
                            //Success
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    pingTextView.setText(pingText(dec, pingTest.getAvgRtt()));
                                    try {
                                        mhead.setText("PING");
                                    } catch (Exception O) {
                                    }
                                }
                            });
                        }
                    } else {
                        pingRateList.add(pingTest.getInstantRtt());

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                pingTextView.setText(pingText(dec, pingTest.getInstantRtt()));
                                try {
                                    mhead.setText("PING");
                                } catch (Exception O) {
                                }
                            }
                        });

                        //Update chart
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Creating an  XYSeries for Income
                                XYSeries pingSeries = new XYSeries("");
                                pingSeries.setTitle("");

                                int count = 0;
                                List<Double> tmpLs = new ArrayList<>(pingRateList);
                                for (Double val : tmpLs) {
                                    pingSeries.add(count++, val);
                                }

                                XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
                                dataset.addSeries(pingSeries);


                            }
                        });
                    }


                    //Download Test
                    if (pingTestFinished) {
                        if (downloadTestFinished) {
                            //Failure
                            if (downloadTest.getFinalDownloadRate() == 0) {
                                System.out.println("Download error...");
                            } else {
                                //Success
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (rs.isChecked()) {
                                            downloadTextView.setText(decmbs.format(downloadTest.getFinalDownloadRate() / 8) + " MB/s");
                                        } else {
                                            downloadTextView.setText(dec.format(downloadTest.getFinalDownloadRate()) + " Mbps");
                                        }

                                        try {
                                            mhead.setText("DOWNLOAD");
                                        } catch (Exception O) {
                                        }
                                    }
                                });
                            }
                        } else {
                            //Calc position
                            double downloadRate = downloadTest.getInstantDownloadRate();
                            downloadRateList.add(downloadRate);
                            if (rs.isChecked()) {
                                position = getPositionByRate(downloadRate / 8);
                            } else {
                                position = getPositionByRate(downloadRate);
                            }
                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    rotate = new RotateAnimation(lastPosition, position, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                                    rotate.setInterpolator(new LinearInterpolator());
                                    rotate.setDuration(100);
                                    barImageView.startAnimation(rotate);
                                    if (rs.isChecked()) {
                                        downloadTextView.setText(decmbs.format(downloadTest.getInstantDownloadRate() / 8) + " MB/s");
                                    } else {
                                        downloadTextView.setText(dec.format(downloadTest.getInstantDownloadRate()) + " Mbps");
                                    }
                                    try {
                                        mhead.setText("DOWNLOAD");
                                    } catch (Exception O) {
                                    }

                                }

                            });
                            lastPosition = position;

                            /**Update chart*/
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // Creating an  XYSeries for Income
                                    XYSeries downloadSeries = new XYSeries("");
                                    downloadSeries.setTitle("");
                                    List<Double> tmpLs = new ArrayList<>(downloadRateList);
                                    int count = 0;
                                    for (Double val : tmpLs) {
                                        downloadSeries.add(count++, val);
                                    }
                                    XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
                                    dataset.addSeries(downloadSeries);
                                }
                            });

                        }
                    }


                    //Upload Test
                    if (downloadTestFinished) {
                        if (uploadTestFinished) {
                            //Failure
                            if (uploadTest.getFinalUploadRate() == 0) {
                                System.out.println("Upload error...");
                            } else {
                                //Success
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (rs.isChecked()) {
                                            uploadTextView.setText(decmbs.format(uploadTest.getFinalUploadRate() / 8) + " MM/s");
                                        } else {
                                            uploadTextView.setText(dec.format(uploadTest.getFinalUploadRate()) + " Mbps");
                                        }
                                        try {
                                            mhead.setText("UPLOAD");
                                        } catch (Exception O) {
                                        }
                                    }
                                });
                            }
                        } else {
                            //Calc position
                            double uploadRate = uploadTest.getInstantUploadRate();
                            uploadRateList.add(uploadRate);
                            if (rs.isChecked()) {
                                position = getPositionByRate(uploadRate / 8);
                            } else {
                                position = getPositionByRate(uploadRate);
                            }

                            runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    rotate = new RotateAnimation(lastPosition, position, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                                    rotate.setInterpolator(new LinearInterpolator());
                                    rotate.setDuration(100);
                                    barImageView.startAnimation(rotate);
                                    if (rs.isChecked()) {
                                        uploadTextView.setText(decmbs.format(uploadTest.getInstantUploadRate() / 8) + " MB/s");
                                    } else {
                                        uploadTextView.setText(dec.format(uploadTest.getInstantUploadRate()) + " Mbps");
                                    }

                                    try {
                                        mhead.setText("UPLOAD");
                                    } catch (Exception O) {
                                    }

                                }

                            });
                            lastPosition = position;

                            //Update chart
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    // Creating an  XYSeries for Income
                                    XYSeries uploadSeries = new XYSeries("");
                                    uploadSeries.setTitle("");

                                    int count = 0;
                                    List<Double> tmpLs = new ArrayList<>(uploadRateList);
                                    for (Double val : tmpLs) {
                                        if (count == 0) {
                                            val = 0.0;
                                        }
                                        uploadSeries.add(count++, val);
                                    }

                                    XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
                                    dataset.addSeries(uploadSeries);
                                }
                            });

                        }
                    }

                    //Test bitti
                    if (pingTestFinished && downloadTestFinished && uploadTest.isFinished()) {
                        break;
                    }

                    if (pingTest.isFinished()) {
                        pingTestFinished = true;
                    }
                    if (downloadTest.isFinished()) {
                        downloadTestFinished = true;
                    }
                    if (uploadTest.isFinished()) {
                        uploadTestFinished = true;
                    }

                    if (pingTestStarted && !pingTestFinished) {
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                        }
                    } else {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                        }
                    }
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startButton.setEnabled(true);
                        startButton.setText("Restart Test");

                        try {
                            mhead.setText(R.string.app_name);
                        } catch (Exception O) {
                        }
                        // Natural break after a finished test: capped interstitial, then the result dialog.
                        AdsHelper.showInterstitialIfReady(MainActivity.this, getString(R.string.interstial_id),
                                () -> { if (!isFinishing() && !isDestroyed()) sharing(); });

                        /**
                         * add data base
                         */
                        String currentTime = new SimpleDateFormat("HH:mm a", Locale.getDefault()).format(new Date());
                        MyDatabaseHelper myDB = new MyDatabaseHelper(MainActivity.this);
                        myDB.addBook(
                                currentTime.trim(),
                                type_of_network.getText().toString().trim(),
                                pingTextView.getText().toString().trim(),
                                downloadTextView.getText().toString().trim(),
                                uploadTextView.getText().toString().trim());
                        /**
                         * add data base
                         */
                    }
                });


            }
        }).start();
    }


    public int getPositionByRate(double rate) {
        if (rate <= 1) {
            return (int) (rate * 30);

        } else if (rate <= 10) {
            return (int) (rate * 6) + 30;

        } else if (rate <= 30) {
            return (int) ((rate - 10) * 3) + 90;

        } else if (rate <= 50) {
            return (int) ((rate - 30) * 1.5) + 150;

        } else if (rate <= 100) {
            return (int) ((rate - 50) * 1.2) + 180;
        }

        return 0;
    }

    @Override
    public void onCheckChanged(boolean b) {

        if (rs.isChecked()) {
            mdatametartype.setText("MEGABYTE PER SECOND");
            downloadTextView.setText("0 MB/s");
            uploadTextView.setText("0 MB/s");
            speedface.setImageResource(R.drawable.megabyteps);
        } else {
            mdatametartype.setText("MEGABIT PER SECOND");
            downloadTextView.setText("0 Mbps");
            uploadTextView.setText("0 Mbps");
            speedface.setImageResource(R.drawable.megabitps);
        }

    }

    public void sharing() {
        final AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this, R.style.CustomDialog);
        View mView = getLayoutInflater().inflate(R.layout.share_activity, null);

        /**linelyout install*/
        LinearLayout resultview = (LinearLayout) mView.findViewById(R.id.result);
        /**linelyout install*/


        /**share windows settext textview install*/
        TextView rping = (TextView) mView.findViewById(R.id.rping);//
        TextView rdownload = (TextView) mView.findViewById(R.id.rdownload);//
        TextView rupload = (TextView) mView.findViewById(R.id.rupload);//
        TextView tping = (TextView) mView.findViewById(R.id.tping);//
        TextView tdownload = (TextView) mView.findViewById(R.id.tdownload);//
        TextView tupload = (TextView) mView.findViewById(R.id.tupload);//
        /**share windows settext textview install*/


        /**closed button*/
        Button share = (Button) mView.findViewById(R.id.closed);
        /**closed button*/

        /**share result textview*/
        TextView sheader = (TextView) mView.findViewById(R.id.sharehead);
        TextView subhead = (TextView)mView.findViewById(R.id.subhead);
        /**share result textview*/

        /**time and date textview install*/
        TextView times = (TextView) mView.findViewById(R.id.timeview);
        TextView dates = (TextView) mView.findViewById(R.id.dateview);
        /**set time and date*/
        String currentTime = new SimpleDateFormat("HH:mm:ss a", Locale.getDefault()).format(new Date());
        times.setText(currentTime);
        //////////////////////////////
        String currentDate = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
        dates.setText(currentDate);
        /**set time and date*/
        /**time and date textview install*/


        /**host and location text install*/
        TextView sharhost = (TextView) mView.findViewById(R.id.thostingname);
        TextView sharlocation = (TextView)mView.findViewById(R.id.tlocationname);
        /**host and location text install*/


        /**set custom font typeface all textview sharewindows*/
        rping.setTypeface(type);
        rdownload.setTypeface(type);
        rupload.setTypeface(type);
        tping.setTypeface(type);
        tdownload.setTypeface(type);
        tupload.setTypeface(type);
        sheader.setTypeface(type);
        subhead.setTypeface(type);
        sharhost.setTypeface(type);
        sharlocation.setTypeface(type);
        /**set custom font typeface all textview sharewindows*/


        /**set text*/
        rping.setText(pingTextView.getText().toString());
        rdownload.setText(downloadTextView.getText().toString());
        rupload.setText(uploadTextView.getText().toString());
        sharhost.setText("SERVER : "+hostname.getText().toString());
        sharlocation.setText("LOCATION : "+locationname.getText().toString());
        /**set text*/

        alert.setView(mView);
        final AlertDialog alertDialog = alert.create();
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.show();

        /**share button work*/
        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                share_part();

            }

            public void share_part(){
                // Shared through the app cache + FileProvider: no storage permission needed (scoped storage).
                bitmap = ScreenshotUtil.getInstance().takeScreenshotForView(resultview);
                if (bitmap == null) {
                    Toast.makeText(MainActivity.this, R.string.app_name, Toast.LENGTH_SHORT).show();
                    return;
                }
                File dir = new File(getCacheDir(), "shared");
                File out = new File(dir, System.currentTimeMillis() + ".png");
                try {
                    if (!dir.isDirectory() && !dir.mkdirs()) {
                        throw new IOException("Cannot create " + dir);
                    }
                    try (FileOutputStream fos = new FileOutputStream(out)) {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    }
                } catch (IOException e) {
                    Log.e("GREC", "Cannot save screenshot", e);
                    return;
                }
                Uri bmpUri = FileProvider.getUriForFile(MainActivity.this, BuildConfig.APPLICATION_ID + ".provider", out);
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_STREAM, bmpUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setType("image/png");
                startActivity(Intent.createChooser(intent, "Share Image:"));
                alertDialog.dismiss();
            }

        });
        /**share button work*/

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == APP_PERMISSION_REQUEST && resultCode == RESULT_OK) {
            Toast.makeText(this, "Draw over other app permission enable.", Toast.LENGTH_SHORT).show();
        } else {

        }
    }

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(getApplicationContext(), "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);

    }




    /** Ping label; values outside 0..60 s (failed ping, unset timer) show a dash instead of garbage. */
    static String pingText(java.text.DecimalFormat dec, double ms) {
        if (Double.isNaN(ms) || ms <= 0 || ms > 60_000) {
            return "— ms";
        }
        return dec.format(ms) + " ms";
    }
}
