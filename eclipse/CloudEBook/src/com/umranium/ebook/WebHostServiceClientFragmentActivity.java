package com.umranium.ebook;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.umranium.ebook.services.IWebHostService;
import com.umranium.ebook.services.WebHostEventListener;
import com.umranium.ebook.services.WebHostService;

public class WebHostServiceClientFragmentActivity extends
		SherlockFragmentActivity implements WebHostServiceClient {

	static final String TAG = "WebHostServiceClientFragmentActivity";

	protected IWebHostService webHostService = null;

	private WebHostEventListener webHostEventListener = new WebHostEventListener() {
		@Override
		public void serverStarted() {
			Log.d(TAG, "serverStarted");
			onServerStarted();
		}
	};

	private ServiceConnection webHostServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			webHostService = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			Log.d(TAG, "onServiceConnected");
			webHostService = (IWebHostService) service;
			webHostService.addEventListener(webHostEventListener);
			if (webHostService.isServerStarted()) {
				Log.d(TAG, "serverStarted already");
				WebHostServiceClientFragmentActivity.this.onServerStarted();
			}
		}
	};

	@Override
	protected void onCreate(Bundle arg0) {
		Log.d(TAG, "onCreate");
		super.onCreate(arg0);

		WaitFragment fragment = new WaitFragment();
		getSupportFragmentManager().beginTransaction()
				.replace(R.id.container, fragment).commit();

		Intent webHostServiceIntent = new Intent(this, WebHostService.class);//modded-sauf
		this.bindService(webHostServiceIntent, webHostServiceConnection,
				Context.BIND_AUTO_CREATE);
		Thread.yield();
	}

	/**
	 * Called when the server is started, or if connected to the service after
	 * the server has started, then called immediately after connecting to the
	 * service.
	 */
	protected void onServerStarted() {

	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "onDestroy");
		webHostService.removeEventListener(webHostEventListener);
		webHostService = null;

		this.unbindService(webHostServiceConnection);

		super.onDestroy();
	}

	@Override
	public IWebHostService getWebHostService() {
		return webHostService;
	}

	/**
	 * Fragment displayed while loading the ebook.
	 */
	public static class WaitFragment extends Fragment {
		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.wait_screen, container,
					false);
			return rootView;
		}
	}

}
