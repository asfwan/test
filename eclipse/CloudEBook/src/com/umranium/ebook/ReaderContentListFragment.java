package com.umranium.ebook;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.umranium.ebook.model.BookTocEntry;

public class ReaderContentListFragment extends SherlockListFragment {

    private static final String TAG = "ReaderContentListFragment";

    private TocEntryGroupHelper tocEntryGroupHelper;
    private ReaderActivity readerActivity;
    private ReaderContentsAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        this.setRetainInstance(true);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        BookTocEntry entry = (BookTocEntry) adapter.getItem(position);
//		readerActivity.goToBookHref(entry.href);
        if (readerActivity != null) {
            readerActivity.onContentListEntrySelected(entry);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        Log.d(TAG, "onAttach");
        super.onAttach(activity);
        readerActivity = (ReaderActivity) activity;
    }

    @Override
    public void onDetach() {
        Log.d(TAG, "onDetach");
        super.onDetach();
        readerActivity = null;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        attemptInit();
    }

    public void setTocEntryGroupHelper(TocEntryGroupHelper tocEntryGroupHelper) {
        Log.d(TAG, "setTocEntryGroupHelper");
        this.tocEntryGroupHelper = tocEntryGroupHelper;
        attemptInit();
    }

    private void attemptInit() {
        if (this.tocEntryGroupHelper == null || !isAdded()) {
            return;
        }

        Log.d(TAG, "attemptInit");
        adapter = new ReaderContentsAdapter(readerActivity, tocEntryGroupHelper);
        this.setListAdapter(adapter);
    }

}
