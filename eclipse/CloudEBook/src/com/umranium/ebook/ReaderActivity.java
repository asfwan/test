package com.umranium.ebook;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;

import com.actionbarsherlock.app.ActionBar;
import com.slidingmenu.lib.CustomViewAbove;
import com.slidingmenu.lib.SlidingMenu;
import com.umranium.ebook.ReaderReaderFragment.SidePanelSide;
import com.umranium.ebook.epub.DomHelper;
import com.umranium.ebook.model.BookSegment;
import com.umranium.ebook.model.UserBookSegmentDetails;
import com.umranium.ebook.services.DatabaseHelper;
import com.umranium.ebook.viewer.ScreenDensityHelper;

public class ReaderActivity extends WebHostServiceClientSlidingFragmentActivity
		implements ActionBar.OnNavigationListener {

	private static final String TAG = "ReaderActivity";

	private static final int PAGE_RIGHT = 2;

	public static final String EXTRA_BOOK_ID = "extra_book_id";

	public static final float MAX_PERCENTAGE_SCREEN_WIDTH = 0.85f;

	private static final String FRAG_TAG_CONTENT = "frag_content";
	private static final String FRAG_TAG_NOTEPAD = "frag_notepad";
	private static final String FRAG_TAG_READER = "reader";

	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * current dropdown position.
	 */
	private static final String SAVED_BOOK_LOCATION = "saved_book_location";

	private DatabaseHelper databaseHelper;
	private ScreenDensityHelper screenDensityHelper;
	private com.umranium.ebook.model.Book book;
	private TocEntryGroupHelper tocEntryGroupHelper;
	private ReaderContentListFragment contentListFragment;
	private ReaderNotepadFragment notepadFragment;
	private ReaderReaderFragment currentReader;
	private String currentContentHref;
	private String currentGoToHref;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");

		super.onCreate(savedInstanceState);

		this.screenDensityHelper = new ScreenDensityHelper(this);

		Log.d(TAG, "setContentView");
		setContentView(R.layout.activity_reader);

		Log.d(TAG, "setBehindContentView");
		setBehindContentView(R.layout.menu_frame);

		Log.d(TAG, "initializing sliding menu");
		SlidingMenu sm = getSlidingMenu();
		sm.setMode(SlidingMenu.LEFT_RIGHT);
		sm.setTouchModeAbove(SlidingMenu.TOUCHMODE_FULLSCREEN);
		Log.d(TAG, "initialization of sliding menu... DONE");

		Display display = getWindowManager().getDefaultDisplay();
		@SuppressWarnings("deprecation")
		int screenWidth = display.getWidth();

		sm.setSecondaryMenu(R.layout.menu_frame_two);

		sm.setShadowWidthRes(R.dimen.reader_content_slider_shadow_width);
		sm.setShadowDrawable(R.drawable.slider_shadow);
		sm.setFadeDegree(0.35f);

		// sm.setBehindWidthRes(R.dimen.reader_content_slider_width);
		float behindWid = screenDensityHelper.getDensityDependentValue(this
				.getResources().getDimension(
						R.dimen.reader_content_slider_width));
		if (behindWid > screenWidth * MAX_PERCENTAGE_SCREEN_WIDTH) {
			behindWid = screenWidth * MAX_PERCENTAGE_SCREEN_WIDTH;
		}
		sm.setBehindWidth((int) behindWid);

		sm.getViewAbove().setOnPageChangeListener(
				new CustomViewAbove.OnPageChangeListener() {

					@Override
					public void onPageSelected(int position) {
						Log.d(TAG, "onPageSelected " + position);
						if (position == PAGE_RIGHT) {
							if (notepadFragment != null) {
								notepadFragment.updateValues();
							}
						}
					}

					@Override
					public void onPageScrolled(int position,
							float positionOffset, int positionOffsetPixels) {
					}
				});

		if (savedInstanceState != null) {
			Log.d(TAG, "Found savedInstanceState");
			contentListFragment = (ReaderContentListFragment) getSupportFragmentManager()
					.findFragmentByTag(FRAG_TAG_CONTENT);
			notepadFragment = (ReaderNotepadFragment) getSupportFragmentManager()
					.findFragmentByTag(FRAG_TAG_NOTEPAD);
			currentReader = (ReaderReaderFragment) getSupportFragmentManager()
					.findFragmentByTag(FRAG_TAG_READER);
			if (currentReader != null) {
				currentContentHref = currentReader.getContentHref();
				currentGoToHref = currentReader.getGoToHref();
			}
		}

		// Log.d(TAG, "initializing action bar");
		// ActionBar actionBar = this.getSupportActionBar();
		// actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_HOME |
		// ActionBar.DISPLAY_SHOW_TITLE |
		// ActionBar.DISPLAY_SHOW_CUSTOM);

		Log.d(TAG, "onCreate (Done)");
	}

	@Override
	protected void onServerStarted() {
		Log.d(TAG, "onServerStarted");

		long bookId = this.getIntent().getLongExtra(EXTRA_BOOK_ID, -1);

		databaseHelper = getWebHostService().getDatabaseHelper();
		assert (databaseHelper != null);
		book = getWebHostService().getBook(bookId);
		assert (book != null);
		tocEntryGroupHelper = new TocEntryGroupHelper(book);

		if (contentListFragment != null) {
			contentListFragment.setTocEntryGroupHelper(tocEntryGroupHelper);
		}
		if (notepadFragment != null) {
			notepadFragment.setWebHostService(webHostService);
		}
		if (currentReader != null) {
			currentReader.setWebHostService(webHostService);
		}

		updateUiBasedOnBook();
	}

	@Override
	protected void onResume() {
		super.onResume();

		// if (webHostService!=null && webHostService.isBookLoaded(bookId)) {
		// loadSavedSectionLocation();
		// }
	}

	public com.umranium.ebook.model.Book getBook() {
		return book;
	}

	public String getCurrentContentHref() {
		return currentContentHref;
	}

	public String getCurrentGoToHref() {
		return currentGoToHref;
	}

	public ReaderReaderFragment getCurrentReader() {
		return currentReader;
	}

	public TocEntryGroupHelper getTocEntryGroupHelper() {
		return tocEntryGroupHelper;
	}

	// @Override
	// public void onRestoreInstanceState(Bundle savedInstanceState) {
	// // Restore the previously serialized current dropdown position.
	// if (savedInstanceState.containsKey(STATE_SELECTED_NAVIGATION_ITEM)) {
	// getActionBar().setSelectedNavigationItem(
	// savedInstanceState.getInt(STATE_SELECTED_NAVIGATION_ITEM));
	// }
	// }
	//
	// @Override
	// public void onSaveInstanceState(Bundle outState) {
	// // Serialize the current dropdown position.
	// outState.putInt(STATE_SELECTED_NAVIGATION_ITEM,
	// getActionBar().getSelectedNavigationIndex());
	// }

	@Override
	public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
		getSupportMenuInflater().inflate(R.menu.reader, menu);
		return true;
	}

	/**
	 * Load the section, given the book table-of-contents entry of the section,
	 * and a href to goto (possibly containing an anchor to the area in the
	 * content).
	 * 
	 * @param entry
	 *            Ebook section to load.
	 * @param goToHref
	 *            Href to location to go to once section is loaded.
	 */
	private void loadSection(com.umranium.ebook.model.BookTocEntry entry,
			String goToHref) {
		Log.d(TAG, "loadSection: " + entry.title + " goToHRef=" + goToHref);

		if (goToHref == null) {
			goToHref = entry.href;
		}

		String contentHref = DomHelper.removeAnyAnchor(entry.href);

		SharedPreferences prefs = this.getSharedPreferences(
				book.getIdentifier(), Context.MODE_PRIVATE);
		Editor edit = prefs.edit();
		edit.putString(SAVED_BOOK_LOCATION, goToHref);
		edit.apply();

		this.currentContentHref = contentHref;
		this.currentGoToHref = goToHref;

		boolean createNew = true;

		// check whether we need to create a new reader fragment
		if (currentReader != null) {
			Log.d(TAG,
					"currentReader.getContentHref()="
							+ currentReader.getContentHref());
			Log.d(TAG, "contentHref=" + contentHref);
			// check whether the content is already, loaded
			if (contentHref.equals(currentReader.getContentHref())) {
				// is the content-href the same as the goto-href,
				// would only be different if the goto-href has an anchor
				if (!contentHref.equals(goToHref)) {
					currentReader.goToBookSection(goToHref);
					createNew = false;
				}
			}
		}

		if (createNew) {
			currentReader = ReaderReaderFragment.createFragment(contentHref,
					goToHref, SidePanelSide.LEFT, readerFragmentEventsHandler);
			if (webHostService != null) {
				currentReader.setWebHostService(webHostService);
			}
		}

		// even if the reader hasn't been freshly created,
		// the ancestor class replaces the container with a waiting fragment
		// we have to replace the reader back
		getSupportFragmentManager().beginTransaction()
				.replace(R.id.container, currentReader, FRAG_TAG_READER)
				.commit();
	}

	private void updateUiBasedOnBook() {
		// SlidingMenu sm = getSlidingMenu();
		// sm.setSecondaryMenu(R.layout.menu_frame_two);
		// sm.setSecondaryShadowDrawable(R.drawable.shadowright);

		Log.d(TAG, "Creating the content list fragment");
		if (contentListFragment == null) {
			// only create if fragment hasn't been created
			contentListFragment = new ReaderContentListFragment();
			contentListFragment.setTocEntryGroupHelper(tocEntryGroupHelper);
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.menu_frame, contentListFragment).commit();
		}

		Log.d(TAG, "Creating the notepad fragment");
		if (notepadFragment == null) {
			// only create if fragment hasn't been created
			notepadFragment = new ReaderNotepadFragment();
			notepadFragment.setWebHostService(webHostService);
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.menu_frame_two, notepadFragment).commit();
		}

		// getSupportFragmentManager()
		// .beginTransaction()
		// .replace(R.id.menu_frame_two, new ReaderContentListFragment())
		// .commit();

		// Set up the action bar to show a dropdown list.
		// final ActionBar actionBar = getSupportActionBar();
		// actionBar.setDisplayShowTitleEnabled(false);
		// actionBar.setTitle(book.title);
		// actionBar.setSubtitle(book.authors);

		// if (book.coverResourceHref!=null) {
		// List<com.umranium.ebook.model.BookResource> covers =
		// getWebHostService().getResource(book.coverResourceHref);
		// if (!covers.isEmpty()) {
		// com.umranium.ebook.model.BookResource coverRes = covers.get(0);
		// Bitmap cover = BitmapFactory.decodeByteArray(coverRes.data, 0,
		// coverRes.data.length);
		// ImageView logo = (ImageView) findViewById(android.R.id.home);
		// logo.setImageBitmap(cover);
		// }
		// }

		loadSavedSectionLocation();

		// ReaderTestFragment fragment = ReaderTestFragment.createFragment();;
		// getSupportFragmentManager()
		// .beginTransaction()
		// .replace(R.id.container, fragment)
		// .commit();

	}

	private void loadSavedSectionLocation() {
		SharedPreferences prefs = ReaderActivity.this.getSharedPreferences(
				book.getIdentifier(), Context.MODE_PRIVATE);
		if (prefs.contains(SAVED_BOOK_LOCATION)) {
			String goToHref = prefs.getString(SAVED_BOOK_LOCATION, null);
			goToBookHref(goToHref);
		} else {
			if (!tocEntryGroupHelper.getRootEntries().isEmpty()) {
				goToBookHref(tocEntryGroupHelper.getRootEntries().get(1).href);
			}
		}
	}

	/**
	 * This loads the content given by href into the reader. The href refers to
	 * a section of the ebook. It may also contain an anchor.
	 * 
	 * @param href
	 *            A reference to content from the current ebook. Possibly
	 *            containing an anchor.
	 */
	public void goToBookHref(String href) {
		Log.d(TAG, "goToBookHref href=" + href);

		com.umranium.ebook.model.BookTocEntry foundEntry = tocEntryGroupHelper
				.findNearestEntry(href);

		if (foundEntry == null) {
			Log.e(TAG, "Unable to find the book section matching to the href:"
					+ href);
			return;
		}

		loadSection(foundEntry, href);
	}

	public void onContentListEntrySelected(
			com.umranium.ebook.model.BookTocEntry entry) {
		goToBookHref(entry.href);
		if (getSlidingMenu().isMenuShowing()) {
			getSlidingMenu().showContent();
		}
		this.showContent();
	}

	public void onBookmarkSelected(
			com.umranium.ebook.model.MetaBookmark bookmark) {
		UserBookSegmentDetails bookSegmentDetails = databaseHelper
				.getFullObject(bookmark.getBookSegment());
		BookSegment bookSegment = databaseHelper
				.getFullObject(bookSegmentDetails.getBookSegment());
		Log.d(TAG, "bookSegment=" + bookSegment.getId() + ", href="
				+ bookSegment.href + ", currentContentHref="
				+ currentContentHref);

		boolean loadable = false;
		if (bookSegment.href.equals(currentContentHref)) {
			ReaderReaderFragment readerFragment = getCurrentReader();
			if (readerFragment != null) {
				readerFragment.goToBookmark(bookmark);
				loadable = true;
			} else {
				Log.e(TAG, "Reader fragment is NULL");
			}
		} else {
			Log.e(TAG, "Bookmark requires content reloading. Current: "
					+ currentContentHref + ", required:" + bookSegment.href);
		}

		if (loadable) {
			if (getSlidingMenu().isSecondaryMenuShowing()) {
				getSlidingMenu().showContent();
			}
		}
	}

	public void onAnnotationSelected(com.umranium.ebook.model.MetaMark mark) {
		boolean loadable = false;
		com.umranium.ebook.model.MetaAnnotation annotation = databaseHelper
				.getFullObject(mark.getAnnotation());
		UserBookSegmentDetails bookSegmentDetails = databaseHelper
				.getFullObject(annotation.getBookSegment());
		BookSegment bookSegment = databaseHelper
				.getFullObject(bookSegmentDetails.getBookSegment());
		Log.d(TAG, "bookSegment=" + bookSegment.getId() + ", href="
				+ bookSegment.href + ", currentContentHref="
				+ currentContentHref);
		if (bookSegment.href.equals(currentContentHref)) {
			ReaderReaderFragment readerFragment = getCurrentReader();
			if (readerFragment != null) {
				readerFragment.goToMark(mark);
				loadable = true;
			} else {
				Log.e(TAG, "Reader fragment is NULL");
			}
		} else {
			Log.e(TAG, "Annotation requires content reloading. Current: "
					+ currentContentHref + ", required:" + bookSegment.href);
		}

		if (loadable) {
			if (getSlidingMenu().isSecondaryMenuShowing()) {
				getSlidingMenu().showContent();
			}
		}
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		return false;
	}

	private ReaderReaderFragment.ReaderFragmentEventsHandler readerFragmentEventsHandler = new ReaderReaderFragment.ReaderFragmentEventsHandler() {
		@Override
		public void onSelectionStarted() {
			Log.d(TAG, "readerFragmentEventsHandler.onSelectionStarted()");
			getSlidingMenu().setSlidingEnabled(false);
		}

		@Override
		public void onSelectionEnded() {
			Log.d(TAG, "readerFragmentEventsHandler.onSelectionEnded()");
			getSlidingMenu().setSlidingEnabled(true);
		}
	};

}
