package com.VR.video_cardboard;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ConfigurationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class PanoramaImageActivity extends AppCompatActivity {
    WebView myBrowser;
    EditText Msg;
    Button btnSendMsg;
    String strPanoImageUrl=null,strPanoImageTitle;

    ImageButton img_btn_prev_panoImage,img_btn_next_panoImage,img_btn_panoImage_close;

    //List<String> vrHTMLList=new ArrayList<String>();
    //String[] panoImageUrlArray=new String[]{ "sannidanam.jpg","pano_sample.jpg"};

    //String[] panoImageUrlArray=new String[]{ "file:///android_asset/kerala/tour.html","http://96.126.113.253:8000/assets/augray_PROD/Demo/CommercialTour/commercial_tour/tour.html"};

    public static int PANO_IMAGE_COUNT;

    ProgressDialog loadingDialog;
    TextView vr_image_name;

    public static final int VR_JATAYU_OUTER_VIEW=0;
    public static final int VR_JATAYU_INNER_VIEW=1;

    int index=0;
    String titleInteraction=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingUpForFullscreen();
        setContentView(R.layout.activity_pano_vr_webview);

        if(getIntent().getExtras()!=null){
            index= Integer.valueOf(getIntent().getStringExtra("InteractionIndex"));
            titleInteraction=getIntent().getStringExtra("InteractionTitle");
        }

        loadingDialog=new ProgressDialog(PanoramaImageActivity.this);
        loadingDialog.setMessage("Loading, please wait...");
        loadingDialog.show();

        // Checking for_streetview feature
        checkStreetviewSupported();
        vr_image_name=(TextView)findViewById(R.id.vr_image_name);
        myBrowser = (WebView)findViewById(R.id.webView_panoramaImage);
        img_btn_prev_panoImage=(ImageButton)findViewById(R.id.img_btn_prev_panoImage);
        img_btn_next_panoImage=(ImageButton)findViewById(R.id.img_btn_next_panoImage);
        img_btn_panoImage_close=(ImageButton)findViewById(R.id.img_btn_panoImage_close);

        img_btn_prev_panoImage.setVisibility(View.INVISIBLE);
        img_btn_next_panoImage.setVisibility(View.INVISIBLE);


        // App header
        ActionBar ab = getSupportActionBar();
        assert ab != null;
        ab.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#ffce19")));  //0xffffdd00
        /*if(strPanoImageTitle!=null) getSupportActionBar().setTitle(Html.fromHtml("<font color=\"black\">" + "Jatayu Adventure Park - VR interaction" + "</font>"));
        else ab.hide();*/
        //getSupportActionBar().setTitle(Html.fromHtml("<font color=\"black\">" + "Jatayu Adventure Park - VR interaction" + "</font>"));
        ab.hide();

        vr_image_name.setText("VR interaction");

        // Loading 360 image in webview
        loadingPanoWebview();


        img_btn_panoImage_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PANO_IMAGE_COUNT=0;
                finish();
            }
        });
    }

    private void loadingPanoWebview(){

        try {
            myBrowser.getSettings().setJavaScriptEnabled(true);
            myBrowser.getSettings().setPluginState(WebSettings.PluginState.ON);
            myBrowser.getSettings().setAllowFileAccess(true);
            myBrowser.getSettings().setAllowContentAccess(true);
            myBrowser.getSettings().setAllowFileAccessFromFileURLs(true);
            myBrowser.getSettings().setAllowUniversalAccessFromFileURLs(true);

            // For white screen issue added on 8th March 2016
            myBrowser.getSettings().setLoadWithOverviewMode(true);
            myBrowser.getSettings().setUseWideViewPort(true);

            // Added for webview loading fast on 23 Feb 2016
            myBrowser.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
            if (Build.VERSION.SDK_INT >= 19) {
                myBrowser.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }
            else {
                myBrowser.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
            // To close webview if no internet connection while loading page
            myBrowser.setWebViewClient(new WebViewClient() {

                @Override
                public void onPageStarted(WebView view, String url, Bitmap favicon) {
                    super.onPageStarted(view, url, favicon);
                    if(!loadingDialog.isShowing()){
                        loadingDialog.show();
                    }
                }

                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    view.clearCache(true);
                    if(loadingDialog.isShowing()){
                        loadingDialog.dismiss();
                    }
                }

                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    //myBrowser.loadUrl("file:///android_asset/myerrorpage.html");
                    Toast.makeText(PanoramaImageActivity.this, "page loading error.", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

            // To clear cache on 8 mar 2016
            myBrowser.clearCache(true);
            CookieSyncManager.createInstance(this);
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.removeAllCookie();

            /*MyJavaScriptInterface myJavaScriptInterface
                    = new MyJavaScriptInterface(this);
            myBrowser.addJavascriptInterface(myJavaScriptInterface, "AndroidFunction");*/
            //myBrowser.getSettings().setJavaScriptEnabled(true);

            myBrowser.loadUrl("file:///android_asset/vtour/tour.html");  // ****** Working one ******

            /*switch (index){
                case VR_JATAYU_OUTER_VIEW:
                    myBrowser.loadUrl("file:///android_asset/pano_interaction/tour.html");
                    break;
                case VR_JATAYU_INNER_VIEW:
                    myBrowser.loadUrl("file:///android_asset/vr_interaction_jatayu_inside/tour.html");
                    //myBrowser.loadUrl("file:///android_asset/pano_test_video/index.html");
                    break;
            }*/


            //vr_interaction_jatayu_inside
            //myBrowser.loadUrl("http://virtualtour.augray.com/videos/vrtest/tourism_pano_video.html"); //************************ Remove it
        }catch (Exception ex){
            Log.e("PanoJSLib", "JS error:" + ex.getMessage());
        }
    }

    private void checkStreetviewSupported(){
        PackageManager pm = getPackageManager();

        //checking OPENGLES VERSION 2.0 OR NOT
        final ActivityManager activityManager =
                (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        final ConfigurationInfo configurationInfo =
                activityManager.getDeviceConfigurationInfo();
        final boolean supportsEs2 = configurationInfo.reqGlEsVersion >= 0x20000;

        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA) &&
                pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER) &&
                pm.hasSystemFeature(PackageManager.FEATURE_SENSOR_COMPASS) &&
                pm.hasSystemFeature(PackageManager.FEATURE_LOCATION) &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH &&
                supportsEs2) {
            //Toast.makeText(DashboardActivity.this,"This device does  support street view feature",Toast.LENGTH_LONG).show();
            Log.d("Panorama", "This device support 360 images");
        }else {
            Toast.makeText(PanoramaImageActivity.this, "Not supported panorama", Toast.LENGTH_LONG).show();
        }
    }

    private void settingUpForFullscreen(){
        if (Build.VERSION.SDK_INT < 16) { //ye olde method
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else { // Jellybean and up, new hotness
            View decorView = getWindow().getDecorView();
            // Hide the status bar.
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
            // Remember that you should never show the action bar if the
            // status bar is hidden, so hide that too if necessary.
        }
    }

    public void showAlertDialog(Context context, String title, String message,Boolean status)
    {
        AlertDialog alertDialog = new AlertDialog.Builder(context).create();
        // Setting Dialog Title
        alertDialog.setTitle(title);
        // Setting Dialog Message
        alertDialog.setMessage(message);
        if(status != null)
            // Setting alert dialog icon
            // Setting OK Button
            alertDialog.setButton("OK", new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    finish();
                }
            });

        // Showing Alert Message
        alertDialog.show();
    }

    private ArrayList<String> parseMultipleMedia(String mediaFiles){
        String[] separated = mediaFiles.split(",");
        ArrayList<String> list=new ArrayList<String>(Arrays.asList(separated));
        return list;
    }

    public static void trimCache(Context context) {
        try {
            File dir = context.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                deleteDir(dir);
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }



    @Override
    public void onBackPressed() {
        super.onBackPressed();
        PANO_IMAGE_COUNT=0;
        finish();
    }
}