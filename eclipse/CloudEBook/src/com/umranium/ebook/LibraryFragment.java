package com.umranium.ebook;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.umranium.ebook.model.Book;
import com.umranium.ebook.services.IWebHostService;

/**
 * The fragment that displays the library.
 */
public class LibraryFragment extends SherlockFragment {
	public static boolean SEARCH = false, SORT_ASC = false, SORT_FLAG = false;
	ListView lv;
	static List<com.umranium.ebook.model.Book> books = null, tempBooks = null;
	static Context context = null;

	interface LibraryFragmentListener {
		void onBookSelected(long id);
	}

	public static boolean LISTVIEW = false; // saufauzan mod

	private IWebHostService webHostService;
	private View library;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		if (context == null)
			context = this.getActivity();
		if (!LISTVIEW)
			library = inflater.inflate(R.layout.main_library, container, false);
		else {
			lv = new ListView(getActivity());
			library = lv;
		}
		if (!(this.getActivity() instanceof WebHostServiceClient)) {
			throw new RuntimeException(
					"Unsupported use of LibraryFragment outside of a WebHostServiceClient");
		}

		WebHostServiceClient webHostServiceClient = (WebHostServiceClient) getActivity();
		webHostService = webHostServiceClient.getWebHostService();

		if (webHostService == null) {
			return null;
		}

		updateBooks();

		return library;
	}

	public void updateBooks() {

		Resources resources = this.getActivity().getResources();
		int maxWidth = resources
				.getDimensionPixelSize(R.dimen.main_library_book_thumbnail_maxwidth);
		int maxHeight = resources
				.getDimensionPixelSize(R.dimen.main_library_book_thumbnail_maxheight);
		Log.d(getTag(), "thumbnail max-width = " + maxWidth);
		Log.d(getTag(), "thumbnail max-height = " + maxHeight);

		if (!SEARCH && !SORT_FLAG)
			books = webHostService.getLibraryBooks(); // getLibBooks

		if (!LISTVIEW) {
			GridView gridView = (GridView) library
					.findViewById(R.id.main_library_gridview);
			gridView.setAdapter(new BookThumbnailAdapter((SEARCH ? tempBooks
					: books), maxWidth, maxHeight));

			gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					if (getActivity() instanceof LibraryFragmentListener) {
						LibraryFragmentListener listener = (LibraryFragmentListener) getActivity();
						listener.onBookSelected(id);
					}
				}
			});
		} else { // saufauzan mod
			lv.setAdapter(new BookThumbnailAdapter(books, maxWidth, maxHeight));
			lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					if (getActivity() instanceof LibraryFragmentListener) {
						LibraryFragmentListener listener = (LibraryFragmentListener) getActivity();
						listener.onBookSelected(id);
					}
				}
			});
		}
	}

	private class BookThumbnailAdapter extends BaseAdapter {

		List<com.umranium.ebook.model.Book> books;
		int maxWidth;
		int maxHeight;

		public BookThumbnailAdapter(List<com.umranium.ebook.model.Book> books,
				int maxWidth, int maxHeight) {
			this.books = books;
			this.maxWidth = maxWidth;
			this.maxHeight = maxHeight;
		}

		@Override
		public int getCount() {
			return books.size();
		}

		@Override
		public Object getItem(int position) {
			return books.get(position);
		}

		@Override
		public long getItemId(int position) {
			return books.get(position).getId();
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			com.umranium.ebook.model.Book book = books.get(position);

			if (convertView == null) {
				LayoutInflater inflater = (LayoutInflater) getActivity()
						.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
				convertView = inflater.inflate(
						(LISTVIEW ? R.layout.main_library_book_thumbnail2
								: R.layout.main_library_book_thumbnail), null);
			}

			Log.d(MainActivity.TAG,
					"BookThumbnailAdapter: Set up view for book at position "
							+ position);
			setupView(convertView, book);

			return convertView;
		}

		private void setupView(View view, com.umranium.ebook.model.Book book) {

			ImageView img = (ImageView) view
					.findViewById(R.id.frag_book_thumbnail_img);
			TextView title = (TextView) view
					.findViewById(R.id.frag_book_thumbnail_title);

			img.setImageResource(R.drawable.ic_launcher);
			if (book.coverResourceHref != null) {
				List<com.umranium.ebook.model.BookResource> covers = webHostService
						.getResource(book.coverResourceHref);
				if (!covers.isEmpty()) {
					com.umranium.ebook.model.BookResource cover = covers.get(0);
					Bitmap srcBm = BitmapFactory.decodeByteArray(cover.data, 0,
							cover.data.length);
					int srcWid = srcBm.getWidth();
					int srcHei = srcBm.getHeight();

					int dstWid;
					int dstHei;

					if (maxWidth <= maxHeight) {
						dstWid = maxWidth;
						dstHei = maxWidth * srcHei / srcWid;
					} else {
						dstWid = maxHeight * srcWid / srcHei;
						dstHei = maxHeight;
					}

					Bitmap dstBm = Bitmap.createScaledBitmap(srcBm, dstWid,
							dstHei, true);
					srcBm.recycle();

					img.setImageBitmap(dstBm);

					Log.d(MainActivity.TAG,
							"Cover for book " + book.getIdentifier()
									+ " loaded.");
				} else {
					Log.e(MainActivity.TAG, "No covers for book, cover href:"
							+ book.coverResourceHref);
				}
			} else {
				Log.e(MainActivity.TAG,
						"No covers for book: " + book.getIdentifier());
			}

			title.setText(book.title);

		}

	}

	public static void querySearch(String query) {
		boolean isCleared = false;
		tempBooks = books;
		if (books != null) {
			for (int i = 0; i < books.size(); i++) {
				Book book = books.get(i);
				String title = book.title;

				if (title.contains(query)) {
					Toast.makeText(context, title, Toast.LENGTH_SHORT).show();
					SEARCH = true;
					if (!isCleared) {
						tempBooks.clear();
						isCleared = true;
					}
					tempBooks.add(book);
					books.toArray();
				}
				// books=tempBooks;
			}
		}
		// if (LibraryFragment.context != null)
		// Toast.makeText(LibraryFragment.context, "test", Toast.LENGTH_SHORT)
		// .show();
	}

	public static void sort() {
		if (SORT_ASC) {
			Collections.sort(books, new Comparator<Book>() {
				@Override
				public int compare(Book b1, Book b2) {
					return b1.title.compareToIgnoreCase(b2.title);
				}
			});
		} else if (!SORT_ASC) {
			Collections.sort(books, new Comparator<Book>() {
				@Override
				public int compare(Book b1, Book b2) {
					return b2.title.compareToIgnoreCase(b1.title);
				}
			});
		}
		// Toast.makeText(context, books.get(0).title,
		// Toast.LENGTH_SHORT).show();
		// LibraryFragment.books = books;
		// return books;
	}
}