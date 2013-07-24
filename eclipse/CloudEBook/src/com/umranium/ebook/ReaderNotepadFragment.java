package com.umranium.ebook;

import java.sql.SQLException;
import java.util.List;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.stmt.QueryBuilder;
import com.umranium.ebook.model.BaseEntity;
import com.umranium.ebook.model.Book;
import com.umranium.ebook.model.BookSegment;
import com.umranium.ebook.model.MetaAnnotation;
import com.umranium.ebook.model.MetaBookmark;
import com.umranium.ebook.model.MetaMark;
import com.umranium.ebook.model.UserBookDetails;
import com.umranium.ebook.model.UserBookSegmentDetails;
import com.umranium.ebook.model.UserDetails;
import com.umranium.ebook.services.DatabaseHelper;
import com.umranium.ebook.services.IWebHostService;

public class ReaderNotepadFragment extends SherlockFragment {

    private static final String TAG = "ReaderNotepadFragment";

//	private static final boolean LIMIT_CONTENT_TO_CURRENT_SEGMENT = true;

    private static final String TAG_ANNOTATIONS = "annotations";
    private static final String TAG_BOOKMARKS = "bookmarks";

    private boolean initialized;
    private TabHost tabHost;
    private ReaderActivity readerActivity;
    private IWebHostService webHostService;
    private DatabaseHelper databaseHelper;
    private RuntimeExceptionDao<MetaAnnotation, Long> annotationDao;
    private RuntimeExceptionDao<MetaMark, Long> markDao;
    private RuntimeExceptionDao<MetaBookmark, Long> bookmarkDao;
    private RuntimeExceptionDao<UserBookSegmentDetails, Long> bookSegmentDetailsDao;
    private Book book;
    private UserDetails userDetails;
    private UserBookDetails bookDetails;
    private ReaderState readerState;
    private AnnotationsAdapter annotationsAdapter;
    private BookmarksAdapter bookmarksAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        View view = inflater.inflate(R.layout.reader_notebook, null);
        tabHost = (TabHost) view.findViewById(android.R.id.tabhost);
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        this.setRetainInstance(true);
    }

    @Override
    public void onAttach(Activity activity) {
        Log.d(TAG, "onAttach");
        super.onAttach(activity);
        this.readerActivity = (ReaderActivity) activity;
    }

    @Override
    public void onDetach() {
        Log.d(TAG, "onDetach");
        super.onDetach();
        this.readerActivity = null;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        attemptInit();
    }

    public void setWebHostService(IWebHostService webHostService) {
        Log.d(TAG, "setWebHostService");
        this.webHostService = webHostService;
        attemptInit();
    }

    private void attemptInit() {
        if (initialized || this.webHostService == null || tabHost == null) {
            return;
        }

        initialized = true;
        this.databaseHelper = this.webHostService.getDatabaseHelper();
        this.annotationDao = this.databaseHelper.getClassDao(MetaAnnotation.class);
        this.markDao = this.databaseHelper.getClassDao(MetaMark.class);
        this.bookmarkDao = this.databaseHelper.getClassDao(MetaBookmark.class);
        this.bookSegmentDetailsDao = this.databaseHelper.getClassDao(UserBookSegmentDetails.class);
        this.book = this.readerActivity.getBook();
        assert (this.book != null);

        this.userDetails = webHostService.getLoggedInUser();
        assert (this.userDetails != null);
        assert (this.userDetails.userLibrary != null);

        this.bookDetails = webHostService.getUserBookDetails(userDetails, book);
        if (this.bookDetails == null) {
            this.bookDetails = webHostService.createUserBookDetails(userDetails, book);
        }
        assert (this.bookDetails != null);

        this.readerState = new ReaderState();
        this.annotationsAdapter = new AnnotationsAdapter();
        this.bookmarksAdapter = new BookmarksAdapter();

        tabHost.setup();

        TabSpec annotationsTab = this.tabHost.newTabSpec(TAG_ANNOTATIONS);
        annotationsTab.setContent(new TabHost.TabContentFactory() {
            @Override
            public View createTabContent(String tag) {
                ListView listView = new ListView(getActivity());
                listView.setTag(tag);
                listView.setAdapter(annotationsAdapter);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                        onAnnotationClicked(position, id);
                    }
                });
                return listView;
            }
        });
        annotationsTab.setIndicator("Annotations");

        TabSpec bookmarksTab = this.tabHost.newTabSpec(TAG_BOOKMARKS);
        bookmarksTab.setContent(new TabHost.TabContentFactory() {
            @Override
            public View createTabContent(String tag) {
                ListView listView = new ListView(getActivity());
                listView.setTag(tag);
                listView.setAdapter(bookmarksAdapter);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view,
                                            int position, long id) {
                        onBookmarkClicked(position, id);
                    }
                });
                return listView;
            }
        });
        bookmarksTab.setIndicator("Bookmarks");

        tabHost.addTab(annotationsTab);
        tabHost.addTab(bookmarksTab);
    }

    public void updateValues() {
        this.readerState.queryCurrentSegment();

        annotationsAdapter.updateData();
        bookmarksAdapter.updateData();
    }

    private void onAnnotationClicked(int position, long annId) {
        QueryBuilder<MetaMark, Long> query = markDao.queryBuilder();
        MetaMark mark;
        try {
            query.where().eq(MetaMark.COLUMN_NAME_ANNOTATION, annId);
            query.orderBy(MetaMark.COLUMN_NAME_START_LOC, false);
            query.orderBy(MetaMark.COLUMN_NAME_START_TEXT_LOC, false);
            mark = DatabaseHelper.getFirstOrNull(query.query());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (mark == null) {
            throw new RuntimeException("No marks for annotation ID=" + annId + " were found!");
        }

        if (readerActivity != null) {
            readerActivity.onAnnotationSelected(mark);
        }
    }

    private void onBookmarkClicked(int position, long id) {
        MetaBookmark bookmark = bookmarkDao.queryForId(id);
        if (bookmark == null) {
            throw new RuntimeException("Bookmark with ID=" + id + " not found!");
        }

        if (readerActivity != null) {
            readerActivity.onBookmarkSelected(bookmark);
        }
    }

    private class ReaderState {
        Long currentSegmentId;
        BookSegment currentBookSegment;

        ReaderState() {
            this.currentSegmentId = null;
            this.currentBookSegment = null;
        }

        void queryCurrentSegment() {
            String contentHref = readerActivity.getCurrentContentHref();
            if (contentHref == null) {
                currentBookSegment = null;
                currentSegmentId = null;
                if (currentSegmentId != null) {
                    onBookSegmentChanged(null);
                }
                return;
            }

            BookSegment bookSegment = book.findBookSegment(contentHref);
            if (!bookSegment.getId().equals(currentSegmentId)) {
                currentSegmentId = bookSegment.getId();
                currentBookSegment = bookSegment;
                onBookSegmentChanged(currentBookSegment);
            }
        }

        private void onBookSegmentChanged(BookSegment newSegment) {
            UserBookSegmentDetails bookSegmentDetails = bookDetails.findUserBookSegmentDetails(newSegment);
            if (bookSegmentDetails == null) {
                bookSegmentDetails = webHostService.createUserBookSegmentDetails(bookDetails, newSegment);
            }
            assert (bookSegmentDetails != null);

            annotationsAdapter.setBookSegmentDetails(bookSegmentDetails);
        }

    }


    private abstract class QueryAdapter<E extends BaseEntity<E>> extends BaseAdapter {

        protected UserBookSegmentDetails bookSegmentDetails;
        protected List<E> data;
        private Long lastMaxUpdate = null;

        public void setBookSegmentDetails(
                UserBookSegmentDetails bookSegmentDetails) {
            this.bookSegmentDetails = bookSegmentDetails;
        }

        @Override
        public int getCount() {
            if (data != null) {
                Log.d(TAG, "getCount() = " + data.size());
                return data.size();
            } else {
                Log.d(TAG, "getCount() = " + 0);
                return 0;
            }
        }

        @Override
        public Object getItem(int position) {
            if (data == null) {
                return null;
            }

            return data.get(position);
        }

        @Override
        public long getItemId(int position) {
            if (data == null) {
                return -1;
            }

            return data.get(position).getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater layoutInflater = readerActivity.getLayoutInflater();
                convertView = layoutInflater.inflate(R.layout.reader_notebook_annotation_summary, null);
            }

            setupView(position, convertView);

            return convertView;
        }

        abstract protected void setupView(int position, View view);

        public abstract List<E> queryData();

        private boolean hasChanged(List<E> newData) {
            if ((data == null) != (newData == null)) {
                return true;
            }

            if (data == null && newData == null) {
                return false;
            }

            if (data.size() != newData.size()) {
                return true;
            }

            if (data.isEmpty()) {
                return false;
            }

            long maxUpdate = 0L;
            for (int i = 0; i < data.size(); ++i) {
                BaseEntity<E> en = data.get(i);
                long lastUpdate = en.getLastUpdateTime().getTime();
                maxUpdate = Math.max(maxUpdate, lastUpdate);
            }

            if (lastMaxUpdate == null || maxUpdate > lastMaxUpdate) {
                lastMaxUpdate = maxUpdate;
                return true;
            }

            return false;
        }

        public void updateData() {
            List<E> newData = queryData();
            if (hasChanged(newData)) {
                data = newData;
                this.notifyDataSetChanged();
            }
        }
    }

    private class AnnotationsAdapter extends QueryAdapter<MetaAnnotation> {

        public List<MetaAnnotation> queryData() {
            try {
                QueryBuilder<MetaAnnotation, Long> query = annotationDao.queryBuilder();

                if (bookSegmentDetails != null) {
                    query.where().eq(
                            MetaAnnotation.COLUMN_NAME_USER_BOOK_SEGMENT_DETAILS,
                            bookSegmentDetails.getId());
                } else {
                    QueryBuilder<UserBookSegmentDetails, Long> bookSegDetailsQuery =
                            bookSegmentDetailsDao.queryBuilder();
                    bookSegDetailsQuery.where().eq(
                            UserBookSegmentDetails.COLUMN_NAME_USER_BOOK_DETAILS,
                            bookDetails.getId());
                    query.join(bookSegDetailsQuery);
                }

                query.orderBy(MetaAnnotation.COLUMN_NAME_START_LOC, true);
                query.orderBy(MetaAnnotation.COLUMN_NAME_START_TEXT_LOC, true);

                List<MetaAnnotation> data = query.query();

                Log.d(TAG, "Annotations queried, current count = " + data.size());

                return data;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            if (data == null) {
                return 0;
            }

            MetaAnnotation ann = data.get(position);

            if (ann.note == null) {
                return 0;
            } else {
                return 1;
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater layoutInflater = readerActivity.getLayoutInflater();
                if (getItemViewType(position) == 0) {
                    convertView = layoutInflater.inflate(R.layout.reader_notebook_annotation_summary, null);
                } else {
                    convertView = layoutInflater.inflate(R.layout.reader_notebook_annotation_note, null);
                }
            }

            setupView(position, convertView);

            return convertView;
        }

        protected void setupView(int position, View view) {
            Log.d(TAG, "Setup view [" + position + "]");

            MetaAnnotation ann = null;
            if (data != null) {
                ann = data.get(position);
            }

            if (ann != null) {
                if (ann.note != null) {
                    TextView note = (TextView) view.findViewById(R.id.reader_notebook_annotation_row_note);
                    note.setText(ann.note);
                } else {
                    TextView summary = (TextView) view.findViewById(R.id.reader_notebook_annotation_row_summary);
                    summary.setText(ann.getSummary());
                }
            } else {
                TextView summary = (TextView) view.findViewById(R.id.reader_notebook_annotation_row_summary);
                summary.setText("???");
            }
        }

    }


    private class BookmarksAdapter extends QueryAdapter<MetaBookmark> {

        public List<MetaBookmark> queryData() {
            try {
                QueryBuilder<MetaBookmark, Long> query = bookmarkDao.queryBuilder();

                if (bookSegmentDetails != null) {
                    query.where().eq(
                            MetaAnnotation.COLUMN_NAME_USER_BOOK_SEGMENT_DETAILS,
                            bookSegmentDetails.getId());
                } else {
                    QueryBuilder<UserBookSegmentDetails, Long> bookSegDetailsQuery =
                            bookSegmentDetailsDao.queryBuilder();
                    bookSegDetailsQuery.where().eq(
                            UserBookSegmentDetails.COLUMN_NAME_USER_BOOK_DETAILS,
                            bookDetails.getId());
                    query.join(bookSegDetailsQuery);
                }

                query.orderBy(MetaBookmark.COLUMN_NAME_START_LOC, true);
                query.orderBy(MetaBookmark.COLUMN_NAME_START_TEXT_LOC, true);

                List<MetaBookmark> data = query.query();

                Log.d(TAG, "Bookmarks queried, current count = " + data.size());

                return data;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        protected void setupView(int position, View view) {
            Log.d(TAG, "Setup view [" + position + "]");

            MetaBookmark bookmark = null;
            if (data != null) {
                bookmark = data.get(position);
            }

            TextView summary = (TextView) view.findViewById(R.id.reader_notebook_annotation_row_summary);

            if (bookmark != null) {
                summary.setText(bookmark.getSummary());
            } else {
                summary.setText("???");
            }
        }

    }

}
