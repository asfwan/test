package com.umranium.ebook;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;

import com.actionbarsherlock.app.SherlockFragment;
import com.umranium.ebook.R;
import com.umranium.ebook.R.id;
import com.umranium.ebook.R.layout;
import com.umranium.ebook.model.Book;
import com.umranium.ebook.viewer.EbookViewer;

public class ReaderTestFragment extends SherlockFragment {

    public static ReaderTestFragment createFragment() {
        ReaderTestFragment frag = new ReaderTestFragment();

        return frag;
    }


    private Book book;
    private View rootView;
    private EbookViewer ebookView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ReaderActivity activity = (ReaderActivity) this.getActivity();

        this.book = activity.getBook();
        assert (this.book != null);

        rootView = inflater.inflate(R.layout.reader_reader, container, false);
        ebookView = (EbookViewer) rootView.findViewById(R.id.reader_reader_ebookviewer);

        ebookView.setWebHostService(activity.getWebHostService());
        ebookView.setBookId(activity.getBook().getIdentifier());

        ebookView.getSettings().setAppCachePath(activity.getApplicationContext().getCacheDir().getAbsolutePath());
        ebookView.getSettings().setCacheMode(WebSettings.LOAD_NORMAL);
        ebookView.getSettings().setAppCacheEnabled(true);

        ebookView.loadUrl("http://www.google.com.au");

        return rootView;
    }
}
