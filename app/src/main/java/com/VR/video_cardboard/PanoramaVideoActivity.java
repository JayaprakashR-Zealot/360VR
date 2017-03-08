/*
 * CollectionPlayer
 * Android example of Panframe library
 * The example enumerates movie files found on external storage, which can be selected for playback.
 * 
 * (c) 2012-2013 Mindlight. All rights reserved.
 * Visit www.panframe.com for more information. 
 * 
 */

package com.VR.video_cardboard;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.panframe.android.lib.PFAsset;
import com.panframe.android.lib.PFAssetObserver;
import com.panframe.android.lib.PFAssetStatus;
import com.panframe.android.lib.PFNavigationMode;
import com.panframe.android.lib.PFObjectFactory;
import com.panframe.android.lib.PFView;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

public class PanoramaVideoActivity extends FragmentActivity implements PFAssetObserver, LoaderCallbacks<Cursor>, OnSeekBarChangeListener {

	PFView _pfview;
	PFAsset _pfasset;
    PFNavigationMode _currentNavigationMode = PFNavigationMode.MOTION;
	
	LoaderManager _loadermanager;
    CursorLoader _cursorLoader;
    Vector<String> _movielist;
    
	boolean 			_updateThumb = true;
    Timer _scrubberMonitorTimer;

    ViewGroup _frameContainer;
	ListView _listview;
	ListAdapter _adapter;
	Button _stopButton;
	Button _playButton;
	Button _touchButton;
	SeekBar _scrubber;
	EditText _intro;
	TextView txt360Videos,vr_video_name;
	ImageButton img_btn_panoVideo_close;
	String titlesArray[];
	private static final int PERMISSION_REQUEST_CODE = 1;
	private static final int INTERNET_PERMISSION_REQUEST_CODE = 2;
	/**
	 * Creation and initalization of the Activitiy.
	 * Initializes variables, listeners, and starts request of a movie list.
	 *
	 * @param  savedInstanceState  a saved instance of the Bundle
	 */
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_pano_video);


        _frameContainer = (ViewGroup) findViewById(R.id.framecontainer);
        //_frameContainer.setBackgroundColor(0xFF000000);
        
        _listview = (ListView) findViewById(R.id.listview_pano_video_list);
		_playButton = (Button)findViewById(R.id.playbutton);
		_stopButton = (Button)findViewById(R.id.stopbutton);
		_touchButton = (Button)findViewById(R.id.touchbutton);
		_scrubber = (SeekBar)findViewById(R.id.scrubber);
		_intro = (EditText)findViewById(R.id.intro);
		_intro.setEnabled(false);
		txt360Videos = (TextView)findViewById(R.id.txt360Videos);
		vr_video_name = (TextView)findViewById(R.id.vr_video_name);
		img_btn_panoVideo_close = (ImageButton)findViewById(R.id.img_btn_panoVideo_close);
		img_btn_panoVideo_close.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if(_pfasset!=null){
					_pfasset.stop();
					onStopVideo();
				}
				txt360Videos.setVisibility(View.VISIBLE);
				vr_video_name.setVisibility(View.GONE);
				img_btn_panoVideo_close.setVisibility(View.GONE);
			}
		});
		
		_playButton.setOnClickListener(playListener);
		_stopButton.setOnClickListener(stopListener);        		
		_touchButton.setOnClickListener(touchListener);         
		_scrubber.setOnSeekBarChangeListener(this);
		
        //_listview.setBackgroundColor(0xFF000000);
        _listview.setOnItemClickListener(new OnItemClickListener() {
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos, long row) {
				startVideo(_movielist.get((int) row));
				_listview.setVisibility(View.INVISIBLE);
				txt360Videos.setVisibility(View.GONE);
				vr_video_name.setVisibility(View.VISIBLE);
				img_btn_panoVideo_close.setVisibility(View.VISIBLE);
				vr_video_name.setText(titlesArray[pos]);
			}
		});

		int locPermissionCheck = ContextCompat.checkSelfPermission(this,
				Manifest.permission.READ_EXTERNAL_STORAGE);
		if(locPermissionCheck  == PackageManager.PERMISSION_GRANTED){
			loadListView();
		}else{
			// Show permission dialog Android 6.0 & above
			checkPermission();
		}
    }


	/*@Override
	protected void onResume() {
		super.onResume();
		int locPermissionCheck = ContextCompat.checkSelfPermission(this,
				Manifest.permission.READ_EXTERNAL_STORAGE);
		if(locPermissionCheck  == PackageManager.PERMISSION_GRANTED){
			loadListView();
		}else{
			// Show permission dialog Android 6.0 & above
			checkPermission();
		}
	}*/

	private void loadListView(){
		showControls(false);
		showIntro(false);

		_loadermanager=getSupportLoaderManager();
		_loadermanager.initLoader(0, null, this);

		_adapter =  new ArrayAdapter<String>(this, R.layout.pano_video_list_item);
		_listview.setAdapter(_adapter);
	}
   
	/**
	 * Show/Hide the playback controls
	 *
	 * @param  bShow  Show or hide the controls. Pass either true or false.
	 */
    public void showControls(boolean bShow)
    {
    	int visibility = View.GONE;
    	
    	if (bShow)
    		visibility = View.VISIBLE;
    		
		_playButton.setVisibility(visibility);
		//_stopButton.setVisibility(visibility);
		_touchButton.setVisibility(visibility);		
		_scrubber.setVisibility(visibility);		
		
		if (_pfview != null)
		{
			if (!_pfview.supportsNavigationMode(PFNavigationMode.MOTION))
				_touchButton.setVisibility(View.GONE);
		}		
    }
    
	/**
	 * Show/Hide the intro text
	 *
	 * @param  bShow  Show or hide the intro text. Pass either true or false.
	 */
    public void showIntro(boolean bShow)
    {
    	int visibility = View.GONE;
    	
    	if (bShow)
    		visibility = View.VISIBLE;
    		
    	_intro.setVisibility(visibility);
    }
    
	/**
	 * Show/Hide the movie list view
	 *
	 * @param  bShow  Show or hide the  movie list view. Pass either true or false.
	 */
    public void showListView(boolean bShow)
    {
    	int visibility = View.GONE;
    	
    	if (bShow)
    		visibility = View.VISIBLE;
    		
    	_listview.setVisibility(visibility);
    }
    
	/**
	 * Handle the stop and cleanup of the video. Restores the UI state.
	 *
	 */
    public void onStopVideo()
    {
    	showControls(false);
		
        _frameContainer.removeView(_pfview.getView());     
        _pfasset = null;
        _pfview = null;

        _loadermanager=getSupportLoaderManager();
        _loadermanager.initLoader(0, null, this);
        
    	showListView(true);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }
    
	/**
	 * Start the video with a local file path
	 *
	 * @param  filename  The file path on device storage
	 */
    public void startVideo(String filename)
    {
    	showListView(false);
		
        _pfview = PFObjectFactory.view(this);
        _pfasset = PFObjectFactory.assetFromUri(this, Uri.parse(filename), this);
        
        _pfview.displayAsset(_pfasset);
        _pfview.setNavigationMode(_currentNavigationMode);

        _frameContainer.addView(_pfview.getView(), 0);     

        showControls(true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
    }
	
	/**
	 * Status callback from the PFAsset instance.
	 * Based on the status this function selects the appropriate action.
	 *
	 * @param  asset  The asset who is calling the function
	 * @param  status The current status of the asset.
	 */
	public void onStatusMessage(final PFAsset asset, PFAssetStatus status) {
		switch (status)
		{
			case LOADED:
				Log.d("SimplePlayer", "Loaded");
				asset.play();
				break;
			case DOWNLOADING:
				Log.d("SimplePlayer", "Downloading 360� movie: " + _pfasset.getDownloadProgress() + " percent complete");
				break;
			case DOWNLOADED:
				Log.d("SimplePlayer", "Downloaded to " + asset.getUrl());
				break;
			case DOWNLOADCANCELLED:
				Log.d("SimplePlayer", "Download cancelled");
				break;
			case PLAYING:
				Log.d("SimplePlayer", "Playing");
				_scrubber.setMax((int) asset.getDuration());
				_playButton.setText("pause");
				_scrubberMonitorTimer = new Timer();
				final TimerTask task = new TimerTask() {
					public void run() {
						if (_updateThumb)
							_scrubber.setProgress((int) asset.getPlaybackTime());						
					}
				};
				_scrubberMonitorTimer.schedule(task, 0, 33);
				break;
			case PAUSED:
				Log.d("SimplePlayer", "Paused");
				_playButton.setText("play");
				break;
			case STOPPED:
				Log.d("SimplePlayer", "Stopped");
				_playButton.setText("play");
				_scrubberMonitorTimer.cancel();
				_scrubberMonitorTimer = null;
				break;
			case COMPLETE:
				Log.d("SimplePlayer", "Complete");
				_playButton.setText("play");
				_scrubberMonitorTimer.cancel();
				_scrubberMonitorTimer = null;
				break;
			case ERROR:
				Log.d("SimplePlayer", "Error");
				break;
		}
	}
	
	/**
	 * Click listener for the play/pause button
	 *
	 */
	private OnClickListener playListener = new OnClickListener() {
		public void onClick(View v) {
			if (_pfasset.getStatus() == PFAssetStatus.PLAYING)
			{
				_pfasset.pause();
			}
			else
				_pfasset.play();
		}
	};
    
	/**
	 * Click listener for the stop/back button
	 *
	 */
	private OnClickListener stopListener = new OnClickListener() {
		public void onClick(View v) {
			_pfasset.stop();
			onStopVideo();
		}
	};

	@Override
	public void onBackPressed() {

		if(_pfasset!=null){
			_pfasset.stop();
			onStopVideo();
			txt360Videos.setVisibility(View.VISIBLE);
			vr_video_name.setVisibility(View.GONE);
			img_btn_panoVideo_close.setVisibility(View.GONE);
		}else{
			super.onBackPressed();
		}
	}

	/**
	 * Click listener for the navigation mode (touch/motion (if available))
	 *
	 */
	private OnClickListener touchListener = new OnClickListener() {
		public void onClick(View v) {
			if (_pfview != null)
			{
				Button touchButton = (Button)findViewById(R.id.touchbutton);
				if (_currentNavigationMode == PFNavigationMode.TOUCH)
				{
					_currentNavigationMode = PFNavigationMode.MOTION;
					touchButton.setText("motion");
				}
				else
				{
					_currentNavigationMode = PFNavigationMode.TOUCH;
					touchButton.setText("touch");
				}
				_pfview.setNavigationMode(_currentNavigationMode);
			}
		}
	};
		
	/**
	 * Setup the options menu
	 *
	 * @param menu The options menu
	 */
   /* public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }*/
    
	/**
	 * Called when pausing the app.
	 * This function pauses the playback of the asset when it is playing.
	 *
	 */
    public void onPause() {
        super.onPause(); 
        if (_pfasset != null)
        {
	        if (_pfasset.getStatus() == PFAssetStatus.PLAYING)
	        	_pfasset.pause();
        }
    }

	/**
	 * Called by the LoaderManager as the loader is created.
	 * This function creates a CursorLoader which will search for movie files on external storage.
	 *
	 * @param id The ID whose loader is to be created.
	 * @param args Any arguments supplied by the caller.
	 * @return Return a new Loader instance that is ready to start loading.
	 */
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String[] projection = { MediaStore.Video.Media._ID, MediaStore.Video.Media.DATA, MediaStore.Video.Media.TITLE};
		return new CursorLoader(this, MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection,
                null,
                null, null);
	}

	/**
	 * Called when a previously created loader has finished its load. 
	 * This function expects a list of TITLE/DATA columns to pupulate the movielist (for playback) and the listview (for selection)
	 * 
	 * @param loader The Loader that has finished.
	 * @param cursor The data generated by the Loader.
	 * 
	 */
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		Vector<String> titles = new Vector<String>();
        _movielist = new Vector<String>();
		
        /*for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
        	
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE);
            titles.add(cursor.getString(column_index));
            column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            _movielist.add(cursor.getString(column_index));

        }*/

		/*String panoVideo = "android.resource://"+getPackageName()+  "/" + R.raw.art_of_living_four_k_intro;
		_movielist.add(panoVideo);
		titles.add("River rejuvenation project - 45 seconds");*/

		String panoVideo2 = "android.resource://"+getPackageName()+  "/" + R.raw.video;
		_movielist.add(panoVideo2);
		titles.add("Test 360 Video");

		/*String panoVideo3 = "file:///android_asset/pano_interaction/tour.html";
		_movielist.add(panoVideo3);
		titles.add("Jatayu Adventure Park");*/

        
        if (titles.size() == 0)
        	showIntro(true);
        else
        	showIntro(false);

		titlesArray= new String[titles.size()];
        titles.toArray(titlesArray);
        _adapter =  new ArrayAdapter<String>(this, R.layout.pano_video_list_item, titlesArray);
        _listview.setAdapter(_adapter);
	}

	/**
	 * Called when a previously created loader is being reset, and thus making its data unavailable.
	 * 
	 * @param loader The Loader that is being reset.
	 * 
	 */
	public void onLoaderReset(Loader<Cursor> loader) {
	}

	/**
	 * Called when a previously created loader is being reset, and thus making its data unavailable.
	 * 
	 * @param seekbar The SeekBar whose progress has changed
	 * @param progress The current progress level.
	 * @param fromUser True if the progress change was initiated by the user.
	 * 
	 */
	public void onProgressChanged (SeekBar seekbar, int progress, boolean fromUser) {
	}

	/**
	 * Notification that the user has started a touch gesture.
	 * In this function we signal the timer not to update the playback thumb while we are adjusting it.
	 * 
	 * @param seekbar The SeekBar in which the touch gesture began
	 * 
	 */
	public void onStartTrackingTouch(SeekBar seekbar) {
		_updateThumb = false;
	}

	/**
	 * Notification that the user has finished a touch gesture. 
	 * In this function we request the asset to seek until a specific time and signal the timer to resume the update of the playback thumb based on playback.
	 * 
	 * @param seekbar The SeekBar in which the touch gesture began
	 * 
	 */
	public void onStopTrackingTouch(SeekBar seekbar) {
		_pfasset.setPLaybackTime(seekbar.getProgress());
		_updateThumb = true;
	}

	private void checkPermission(){
		// Assume thisActivity is the current activity
		int locPermissionCheck = ContextCompat.checkSelfPermission(this,
				Manifest.permission.READ_EXTERNAL_STORAGE);
		if(locPermissionCheck  != PackageManager.PERMISSION_GRANTED){
			/*if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
					Manifest.permission.READ_EXTERNAL_STORAGE)) {
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
						PERMISSION_REQUEST_CODE);
			}else{
				loadListView();
			}*/
			ActivityCompat.requestPermissions(this,
					new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
					PERMISSION_REQUEST_CODE);
		}

		/*int internetPermissionCheck = ContextCompat.checkSelfPermission(this,
				Manifest.permission.INTERNET);
		if(internetPermissionCheck  != PackageManager.PERMISSION_GRANTED){
			if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
					Manifest.permission.INTERNET)) {
				ActivityCompat.requestPermissions(this,
						new String[]{Manifest.permission.INTERNET},
						INTERNET_PERMISSION_REQUEST_CODE );
			}
		}*/
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		switch (requestCode) {
			case PERMISSION_REQUEST_CODE:
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					loadListView();
					Toast.makeText(this,"Read data permission is granted.",Toast.LENGTH_SHORT).show();
					// Implement feature related methods

					// permission was granted, yay! Do the
					// contacts-related task you need to do.

				} else {
					Toast.makeText(this,"Read data permission is denied.",Toast.LENGTH_SHORT).show();
					showPermissionAlert();
					// permission denied, boo! Disable the
					// functionality that depends on this permission.
				}
				return;


			/*case INTERNET_PERMISSION_REQUEST_CODE: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0
						&& grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					Toast.makeText(this,"Internet permission is granted.",Toast.LENGTH_SHORT).show();
					// Implement feature related methods

					// permission was granted, yay! Do the
					// contacts-related task you need to do.

				} else {
					Toast.makeText(this,"Internet permission is denied.",Toast.LENGTH_SHORT).show();
					showPermissionAlert();
					// permission denied, boo! Disable the
					// functionality that depends on this permission.
				}
				return;
			}*/

			// other 'case' lines to check for other
			// permissions this app might request
		}
	}

	private void showPermissionAlert(){
		new AlertDialog.Builder(this)
				.setTitle("Access permission")
				.setMessage("If you need to explore this feature, please click allow permission to proceed further.")
				.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// continue with delete
						//checkLocationPermission();
						ActivityCompat.requestPermissions(PanoramaVideoActivity.this,
								new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.INTERNET},
								PERMISSION_REQUEST_CODE);
					}
				})
				.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int which) {
						// do nothing
						finish();
					}
				})
				.setIcon(android.R.drawable.ic_dialog_alert)
				.show();
	}
}