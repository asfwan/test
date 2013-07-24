package com.umranium.ebook;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnActionExpandListener;
import com.actionbarsherlock.widget.SearchView;
import com.actionbarsherlock.widget.SearchView.OnCloseListener;
import com.actionbarsherlock.widget.SearchView.OnQueryTextListener;
import com.umranium.ebook.LibraryFragment.LibraryFragmentListener;
import com.umranium.ebook.model.UserDetails;

public class MainActivity extends WebHostServiceClientFragmentActivity
		implements LibraryFragmentListener {
	Context context;

	static final String TAG = "MainActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		context = this;
	}

	@Override
	protected void onServerStarted() {
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Log.d(TAG, "server started");

				initUi();

				if (webHostService.getLoggedInUser() == null) {
					Log.d(TAG, "No logged in user");

					// get account
					AccountManager accountManager = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);
					Account[] googleAccounts = accountManager
							.getAccountsByType("com.google");
					Log.d(TAG, "google accounts found: "
							+ googleAccounts.length);

					// check existence
					if (googleAccounts != null && googleAccounts.length > 0) {
						String username = googleAccounts[0].name;
						Log.d(TAG, "using username: " + username);
						UserDetails userDetails = webHostService
								.getUser(username);
						if (userDetails == null) {
							Log.d(TAG, "user not found");
							userDetails = webHostService.createUser(username);
							if (userDetails == null) {
								throw new RuntimeException(
										"Unable to create user: " + username);
							}
							if (userDetails.userLibrary == null) {
								throw new RuntimeException(
										"Unable to create user library: "
												+ username);
							}
							Log.d(TAG, "user created");
						}
						webHostService.logInUser(userDetails);
						Log.d(TAG,
								"user logged in: " + userDetails.getUsername());
					}
				}

				if (webHostService.getLoggedInUser() != null) {
					assert (webHostService.getLoggedInUser().userLibrary != null);

					Log.d(TAG, "displaying user library");
					displayLibrary();
				} else {
					AlertDialog.Builder bldr = new AlertDialog.Builder(
							MainActivity.this);
					bldr.setMessage("Please add a google account first.");
					bldr.setIcon(android.R.drawable.ic_dialog_alert);
					bldr.setPositiveButton("OK", null);
					AlertDialog dialog = bldr.create();
					dialog.setOnDismissListener(new Dialog.OnDismissListener() {
						@Override
						public void onDismiss(DialogInterface dialog) {
							MainActivity.this.finish();
						}
					});
					dialog.show();
				}

				// if (webHostService.getLoggedInUser()!=null) {
				// displayLibrary();
				// } else {
				// displayLoginDialog();
				// }
			}
		});
	}

	private void initUi() {
		ActionBar actionBar = this.getSupportActionBar();
		actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME
				| ActionBar.DISPLAY_SHOW_TITLE | ActionBar.DISPLAY_SHOW_CUSTOM);
	}

	private void displayLibrary() {
//		Toast.makeText(this, "Refreshing..", Toast.LENGTH_SHORT).show();
		LibraryFragment fragment = new LibraryFragment();
		getSupportFragmentManager().beginTransaction()
				.replace(R.id.container, fragment).commit();
	}

	// private void displayLoginDialog() {
	// LibraryFragment fragment = new LibraryFragment();
	// getSupportFragmentManager().beginTransaction()
	// .replace(R.id.container, fragment)
	// .commit();
	// }

	@Override
	public void onBookSelected(long bookId) {
		Log.d(TAG, "Launching ReaderActivity for bookId=" + bookId);
		Intent intent = new Intent(getApplicationContext(),
				ReaderActivity.class);
		intent.putExtra(ReaderActivity.EXTRA_BOOK_ID, bookId);
		startActivity(intent);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.main, menu);

		final SearchView searchView = new SearchView(getSupportActionBar()
				.getThemedContext());

		searchView.setQueryHint("Search");
		menu.add(Menu.NONE, Menu.NONE, 1, "Search")
				.setIcon(R.drawable.abs__ic_search)
				.setActionView(searchView)
				.setShowAsAction(
						MenuItem.SHOW_AS_ACTION_ALWAYS
								| MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

		searchView.setOnQueryTextListener(new OnQueryTextListener() {
			@Override
			public boolean onQueryTextChange(String newText) {
				if (newText.length() > 0) {
					// Search

				} else {
					// Do something when there's no input
				}
				return false;
			}

			@SuppressLint("HandlerLeak")
			@Override
			public boolean onQueryTextSubmit(final String query) {

				InputMethodManager imm = (InputMethodManager) context
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(searchView.getWindowToken(), 0);

				setSupportProgressBarIndeterminateVisibility(true);

				final Handler handler = new Handler() {
					@Override
					public void handleMessage(Message msg) {
						super.handleMessage(msg);
						// Toast.makeText(getBaseContext(),
						// msg.getData().getString("query"), //query
						// Toast.LENGTH_SHORT).show();
						LibraryFragment.querySearch(query);
						displayLibrary();
					}
				};

				handler.post(new Runnable() {
					public void run() {
						// Bundle b = new Bundle();
						// b.putString("query", query);
						// Message m = new Message();
						// m.setData(b);
						// handler.sendMessage(m);
						handler.sendEmptyMessage(0);
						setSupportProgressBarIndeterminateVisibility(false);
					}
				});

				return false;
			}
		});

		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.changelayout:
			LibraryFragment.LISTVIEW = !LibraryFragment.LISTVIEW;
			if (LibraryFragment.LISTVIEW)
				item.setTitle("GridView");
			else
				item.setTitle("ListView");
			displayLibrary();
			break;
		case R.id.sort:
			LibraryFragment.SORT_FLAG=true;
			LibraryFragment.SORT_ASC=!LibraryFragment.SORT_ASC;
			if(LibraryFragment.SORT_ASC)item.setTitle("Sort Ascending");
			else if(!LibraryFragment.SORT_ASC)item.setTitle("Sort Descending");
			LibraryFragment.sort();
			displayLibrary();
			break;
			
		}
		item.setOnActionExpandListener(new OnActionExpandListener() {

			@Override
			public boolean onMenuItemActionExpand(MenuItem item) {
				// Toast.makeText(context, "EXPAND", Toast.LENGTH_SHORT).show();
				return true;
			}

			@Override
			public boolean onMenuItemActionCollapse(MenuItem item) {
				// Toast.makeText(context, "COLLAPSE",
				// Toast.LENGTH_SHORT).show();
				LibraryFragment.SEARCH = false;
				LibraryFragment.SORT_FLAG=false;
				displayLibrary();
				// Toast.makeText(context, "CLOSED", Toast.LENGTH_SHORT).show();
				return true;
			}
		});
		return super.onOptionsItemSelected(item);
	}

}
