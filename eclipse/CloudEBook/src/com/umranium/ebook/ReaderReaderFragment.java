package com.umranium.ebook;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebSettings.TextSize;

import com.actionbarsherlock.app.SherlockFragment;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.umranium.ebook.epub.DomHelper;
import com.umranium.ebook.model.BaseMetaEntity;
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
import com.umranium.ebook.viewer.AndroidAnnotationInterface;
import com.umranium.ebook.viewer.EbookViewer;
import com.umranium.ebookextra.WaitDlgHelper;

@SuppressWarnings("deprecation")
public class ReaderReaderFragment extends SherlockFragment {

    private static final String TAG = "ReaderReaderFragment";

    /**
     * HREF of the content to load/fetch from the server.
     */
    private static final String ARG_CONTENT_HREF = "content_href";

    /**
     * HREF to go to once content is loaded.
     */
    private static final String ARG_GOTO_HREF = "goto_href";

    /**
     * HREF to go to once content is loaded.
     */
    private static final String ARG_SIDE_PANEL_SIDE = "side_panel_side";

    private static final long WAIT_DIALOG_TIMEOUT = 60 * 1000L;  // 1 min

    public enum SidePanelSide {
        LEFT(0, "Left"),
        RIGHT(1, "Right");

        private int val;
        private String name;

        SidePanelSide(int val, String name) {
            this.val = val;
            this.name = name;
        }

        public int getVal() {
            return val;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

//	private static final String PREF_SAVED_ANNOTATIONS = "annotations";	

    public interface ReaderFragmentEventsHandler {
        void onSelectionStarted();

        void onSelectionEnded();
    }

    public static ReaderReaderFragment createFragment(
            String contentHref, String goToHref, SidePanelSide sidePanelSide,
            ReaderFragmentEventsHandler eventsHandler) {
        ReaderReaderFragment frag = new ReaderReaderFragment();

        frag.setEventsHandler(eventsHandler);

        Bundle args = new Bundle();
        args.putString(ARG_CONTENT_HREF, contentHref);
        args.putString(ARG_GOTO_HREF, goToHref);
        args.putInt(ARG_SIDE_PANEL_SIDE, sidePanelSide.ordinal());

        frag.setArguments(args);

        return frag;
    }

    boolean initialized = false;
    private Handler handler;
    private AlertDialog waitDialog;
    private IWebHostService webHostService;
    private DatabaseHelper databaseHelper;
    //    private RuntimeExceptionDao<MetaAnnotation, Long> annotationDao;
//    private RuntimeExceptionDao<MetaMark, Long> markDao;
//    private RuntimeExceptionDao<MetaBookmark, Long> bookmarkDao;
    private Book book;
    private BookSegment bookSegment;
    private UserDetails userDetails;
    private UserBookDetails bookDetails;
    private UserBookSegmentDetails bookSegmentDetails;

    private MetaAnnotationModel annotationModel;
    private MetaMarkModel markModel;
    private MetaModel<MetaBookmark> bookmarkModel;

    private View rootView;
    private EbookViewer ebookView;
    private String contentHref;
    private String goToHref = null;
    private SidePanelSide sidePanelSide;
    private ReaderFragmentEventsHandler eventsHandler;

    public ReaderReaderFragment() {
        this.setHasOptionsMenu(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        this.setRetainInstance(true);
//    	ReaderActivity activity = (ReaderActivity)this.getActivity();
//    	LayoutInflater inflater = activity.getLayoutInflater();
//    	rootView = inflater.inflate(R.layout.reader_reader, null, false);
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.reader_reader, null, false);
        }
        return rootView;
    }

    public void setWebHostService(IWebHostService webHostService) {
        Log.d(TAG, "setWebHostService");
        this.webHostService = webHostService;
        attemptInitReader();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated");
        super.onActivityCreated(savedInstanceState);
        attemptInitReader();
    }

    private void attemptInitReader() {
//    	Log.d(TAG, "this.readerActivity="+this.readerActivity+
//    			", this.rootView="+this.rootView+
//    			", this.webHostService="+this.webHostService);
        if (initialized || this.rootView == null || this.webHostService == null)
            return;

        initialized = true;
        Log.d(TAG, "initReader");

        ReaderActivity readerActivity = (ReaderActivity) getActivity();

        Bundle args = this.getArguments();

        String contentHref = args.getString(ARG_CONTENT_HREF);

        if (args.containsKey(ARG_GOTO_HREF)) {
            goToHref = args.getString(ARG_GOTO_HREF);
        } else {
            goToHref = null;
        }

        sidePanelSide = SidePanelSide.values()[args.getInt(ARG_SIDE_PANEL_SIDE)];
        this.handler = new Handler(readerActivity.getMainLooper());
        this.databaseHelper = webHostService.getDatabaseHelper();
//    	this.annotationDao = this.databaseHelper.getClassDao(MetaAnnotation.class);
//    	this.markDao = this.databaseHelper.getClassDao(MetaMark.class);
//        this.bookmarkDao = this.databaseHelper.getClassDao(MetaBookmark.class);

        this.book = readerActivity.getBook();
        assert (this.book != null);

        this.bookSegment = book.findBookSegment(contentHref);
        assert (this.bookSegment != null);

        this.userDetails = webHostService.getLoggedInUser();
        assert (this.userDetails != null);
        assert (this.userDetails.userLibrary != null);

        this.bookDetails = webHostService.getUserBookDetails(userDetails, book);
        if (this.bookDetails == null) {
            this.bookDetails = webHostService.createUserBookDetails(userDetails, book);
        }
        assert (this.bookDetails != null);

        this.bookSegmentDetails = bookDetails.findUserBookSegmentDetails(bookSegment);
        if (this.bookSegmentDetails == null) {
            this.bookSegmentDetails = webHostService.createUserBookSegmentDetails(bookDetails, bookSegment);
        }
        assert (this.bookSegmentDetails != null);

        populateModels();

        Log.d(TAG, "onCreateView contentHref=" + contentHref + ", goToHref=" + goToHref);

        ebookView = (EbookViewer) rootView.findViewById(R.id.reader_reader_ebookviewer);

        ebookView.getSettings().setAppCachePath(readerActivity.getApplicationContext().getCacheDir().getAbsolutePath());
        ebookView.getSettings().setCacheMode(WebSettings.LOAD_NORMAL);
        ebookView.getSettings().setAppCacheEnabled(true);

        ebookView.addJavascriptInterface(
                new AndroidAnnotationInterface.AndroidAnnoationInterfaceExceptionWrapper(
                        TAG, this.androidAnnotationInterface),
                "androidAnnotationInterface");

        ebookView.setWebHostService(webHostService);
        ebookView.setBookId(readerActivity.getBook().getIdentifier());
        ebookView.setEbookEventsHandler(ebookViewerEventsHandler);

        load(contentHref, goToHref);
    }

    private void populateModels() {
        int annotationSz = bookSegmentDetails.annotations.size() * 2;
        if (annotationSz < 100) {
            annotationSz = 100;
        }

        annotationModel = new MetaAnnotationModel(annotationSz, 10, this.databaseHelper.getClassDao(MetaAnnotation.class));
        markModel = new MetaMarkModel(annotationSz * 10, annotationModel, this.databaseHelper.getClassDao(MetaMark.class));
        for (MetaAnnotation ann : bookSegmentDetails.annotations.getWrappedIterable()) {
            annotationModel.add(ann);
            for (MetaMark mark : ann.marks.getWrappedIterable()) {
                markModel.add(mark);
            }
        }

        int bookmarkSz = bookSegmentDetails.bookmarks.size() * 2;
        if (bookmarkSz < 100) {
            bookmarkSz = 100;
        }
        bookmarkModel = new MetaModel<MetaBookmark>(bookmarkSz, this.databaseHelper.getClassDao(MetaBookmark.class));
        for (MetaBookmark bookmark : bookSegmentDetails.bookmarks.getWrappedIterable()) {
            bookmarkModel.add(bookmark);
        }
    }

    @Override
    public void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop");
        ebookView.stopLoading();
        super.onStop();
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        Log.d(TAG, "onViewStateRestored");
        super.onViewStateRestored(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        Log.d(TAG, "onAttach");
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        Log.d(TAG, "onDetach");
        super.onDetach();
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    public void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
    }

    ;

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView");
        if (rootView != null && rootView.getParent() != null) {
            ViewGroup container = (ViewGroup) rootView.getParent();
            container.removeView(rootView);
            Log.d(TAG, "rootView removed from parent");
        }
        super.onDestroyView();
    }

    public void load(String contentHref, String goToHref) {
        Log.d(TAG, "loading " + contentHref + ", " + goToHref);


        waitDialog = WaitDlgHelper.showWaitDialog(getActivity(), R.string.reader_loading_content_wait_msg);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                closeWaitDialog();
            }
        }, WAIT_DIALOG_TIMEOUT);

        this.contentHref = contentHref;
        this.goToHref = goToHref;

        loadUrl(contentHref);
    }

    public void loadUrl(String url) {
        Log.d(TAG, "loading URL:" + url);
        ebookView.loadUrl(webHostService.getHostUrl() + url);
        //ebookView.loadUrl("http://www.google.com.au");
    }

    public String getContentHref() {
        return contentHref;
    }

    public String getGoToHref() {
        return goToHref;
    }

    public ReaderFragmentEventsHandler getEventsHandler() {
        return eventsHandler;
    }

    public void setEventsHandler(ReaderFragmentEventsHandler eventsHandler) {
        this.eventsHandler = eventsHandler;
    }

    @Override
    public void onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu,
                                    com.actionbarsherlock.view.MenuInflater inflater) {
        inflater.inflate(R.menu.reader_reader, menu);
    }

    @Override
    public boolean onOptionsItemSelected(
            com.actionbarsherlock.view.MenuItem item) {
        switch (item.getItemId()) {
            case R.id.reader_menu_fontsize_smallest:
                ebookView.getSettings().setTextSize(TextSize.SMALLEST);
                return true;
            case R.id.reader_menu_fontsize_smaller:
                ebookView.getSettings().setTextSize(TextSize.SMALLER);
                return true;
            case R.id.reader_menu_fontsize_normal:
                ebookView.getSettings().setTextSize(TextSize.NORMAL);
                return true;
            case R.id.reader_menu_fontsize_larger:
                ebookView.getSettings().setTextSize(TextSize.LARGER);
                return true;
            case R.id.reader_menu_fontsize_largest:
                ebookView.getSettings().setTextSize(TextSize.LARGEST);
                return true;
            case R.id.reader_menu_addbookmark:
                ebookView.startBookmarkingMode();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class MetaModel<EntityType extends BaseMetaEntity<?>> {

        Map<String, EntityType> identifierMap;
        Map<Long, EntityType> idMap;
        RuntimeExceptionDao<EntityType, Long> dao;

        public MetaModel(int initCapacity, RuntimeExceptionDao<EntityType, Long> dao) {
            identifierMap = new HashMap<String, EntityType>(1000);
            idMap = new HashMap<Long, EntityType>(1000);
            this.dao = dao;
        }

        public void add(EntityType entity) {
            dao.create(entity);
            identifierMap.put(entity.getIdentifier(), entity);
            idMap.put(entity.getId(), entity);
        }

        public void update(EntityType entity) {
            dao.update(entity);
        }

        public EntityType get(String identifier) {
            return identifierMap.get(identifier);
        }

        public EntityType get(Long id) {
            return idMap.get(id);
        }

        public void delete(EntityType entity) {
            identifierMap.remove(entity.getIdentifier());
            idMap.remove(entity.getId());
            dao.delete(entity);
        }

        public Set<Long> getIds() {
            return idMap.keySet();
        }
    }

    private class MetaAnnotationModel extends MetaModel<MetaAnnotation> {
        int initSetCapacity;
        Map<Long, Set<Long>> marks;


        public MetaAnnotationModel(int initCapacity, int initSetCapacity, RuntimeExceptionDao<MetaAnnotation, Long> dao) {
            super(initCapacity, dao);
            this.initSetCapacity = initSetCapacity;
            marks = new HashMap<Long, Set<Long>>(initCapacity);
        }

        @Override
        public void add(MetaAnnotation entity) {
            super.add(entity);
            marks.put(entity.getId(), new HashSet<Long>(initSetCapacity));
        }

        @Override
        public void delete(MetaAnnotation entity) {
            marks.remove(entity.getId());
            super.delete(entity);
        }

        public Set<Long> getMarksOf(Long annId) {
            return marks.get(annId);
        }

        public void addMark(Long annId, Long markId) {
            Set<Long> set = marks.get(annId);
            assert (set != null);
            set.add(markId);
        }

        public void deleteMark(Long annId, Long markId) {
            Set<Long> set = marks.get(annId);
            assert (set != null);
            set.remove(markId);
        }

        public List<MetaAnnotation> findAllAnnotationsWithoutMarks() {
            List<MetaAnnotation> anns = new ArrayList<MetaAnnotation>(marks.size());
            for (Map.Entry<Long, Set<Long>> entry : marks.entrySet()) {
                if (entry.getValue().isEmpty()) {
                    anns.add(get(entry.getKey()));
                }
            }
            return anns;
        }

    }

    private class MetaMarkModel extends MetaModel<MetaMark> {

        private MetaAnnotationModel annotationModel;

        public MetaMarkModel(int initCapacity, MetaAnnotationModel annotationModel, RuntimeExceptionDao<MetaMark, Long> dao) {
            super(initCapacity, dao);
            this.annotationModel = annotationModel;
        }

        @Override
        public void add(MetaMark entity) {
            super.add(entity);
            annotationModel.addMark(entity.getAnnotation().getId(), entity.getId());
        }

        @Override
        public void delete(MetaMark entity) {
            super.delete(entity);
            annotationModel.deleteMark(entity.getAnnotation().getId(), entity.getId());
        }

    }

    private MetaAnnotation findAnnotation(String annId) {
        return annotationModel.get(annId);
//    	Map<String,Object> fields = new HashMap<String,Object>(2);
//    	fields.put(MetaAnnotation.COLUMN_NAME_USER_BOOK_SEGMENT_DETAILS, bookSegmentDetails.getId());
//    	fields.put(MetaAnnotation.COLUMN_NAME_IDENTIFIER, annId);
//    	return DatabaseHelper.getFirstOrNull(annotationDao.queryForFieldValues(fields));
    }

    private MetaMark findMark(MetaAnnotation ann, String markId) {
        return markModel.get(markId);
//    	Map<String,Object> fields = new HashMap<String,Object>(2);
//    	fields.put(MetaMark.COLUMN_NAME_ANNOTATION, ann.getId());
//    	fields.put(MetaMark.COLUMN_NAME_IDENTIFIER, markId);
//    	MetaMark mark = DatabaseHelper.getFirstOrNull(markDao.queryForFieldValues(fields));
//    	return mark;
    }

    private MetaAnnotation findAnnotationWithMark(String markId) {
        MetaMark mark = markModel.get(markId);
        return mark.getAnnotation();
//    	try {
//        	QueryBuilder<MetaAnnotation, Long> annQuery = annotationDao.queryBuilder();
//        	annQuery.where().eq(
//    				MetaAnnotation.COLUMN_NAME_USER_BOOK_SEGMENT_DETAILS,
//    				bookSegmentDetails.getId());
//        	QueryBuilder<MetaMark, Long> markQuery = markDao.queryBuilder();
//        	markQuery.join(annQuery);
//        	markQuery.where().eq(MetaMark.COLUMN_NAME_IDENTIFIER, markId);
//        	Log.d(TAG, "Query for mark with id "+markId+" in book-segment "+bookSegmentDetails.getId()+":\n "
//        			+markQuery.prepareStatementString());
//        	MetaMark mark = markQuery.queryForFirst();
//        	MetaAnnotation ann = null;
//        	Log.d(TAG, "\tresult mark="+mark);
//        	if (mark!=null) {
//        		ann = annotationDao.queryForId(mark.getAnnotation().getId());
//        	}
//        	Log.d(TAG, "\tresult ann="+ann);
//        	return ann;
//    	} catch (Exception e) {
//    		throw new RuntimeException(e);
//    	}
    }

    private MetaBookmark findBookmark(String bookmarkId) {
        return bookmarkModel.get(bookmarkId);
//    	Map<String,Object> fields = new HashMap<String,Object>(2);
//    	fields.put(MetaBookmark.COLUMN_NAME_BOOK_SEGMENT, bookSegmentDetails.getId());
//    	fields.put(MetaBookmark.COLUMN_NAME_IDENTIFIER, bookmarkId);
//    	return DatabaseHelper.getFirstOrNull(bookmarkDao.queryForFieldValues(fields));
    }

    public void goToBookSection(String href) {
        String pageHref = DomHelper.removeAnyAnchor(href);

        //	check if href is within this context, if it is, load this context
        if (contentHref.equals(pageHref)) {
            loadUrl(href);
            return;
        }

        // otherwise, find which content and load content
        ReaderActivity activity = (ReaderActivity) getActivity();
        activity.goToBookHref(href);
    }

    public void goToBookmark(MetaBookmark bookmark) {
        this.ebookView.sendSelectionClickResult("gotoBookmark", bookmark.getIdentifier().toString());
    }

    public void goToMark(MetaMark mark) {
        this.ebookView.sendSelectionClickResult("gotoMark", mark.getIdentifier().toString());
    }

    private void closeWaitDialog() {
        AlertDialog dialog = waitDialog;
        waitDialog = null;

        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private AndroidAnnotationInterface androidAnnotationInterface = new AndroidAnnotationInterface() {
        @Override
        public int annotationCache_getSidePanelSide() {
            return sidePanelSide.val;
        }

        @Override
        public void annotationCache_createNewAnnotation(String annId, String summary) {
            MetaAnnotation ann = new MetaAnnotation(bookSegmentDetails, annId, summary);
//			annotationDao.create(ann);
            annotationModel.add(ann);
        }

        @Override
        public String cachedAnnotation_getSummary(String annId) {
            MetaAnnotation ann = findAnnotation(annId);
            return ann.getSummary();
        }

        @Override
        public void cachedAnnotation_setNote(String annId, String note) {
            MetaAnnotation ann = findAnnotation(annId);
            ann.note = note;
            annotationModel.update(ann);
//			annotationDao.update(ann);
        }

        @Override
        public void cachedAnnotation_removeMark(String annId, String markId) {
//			MetaMark mark = findMark(findAnnotation(annId), markId);
//			markDao.delete(mark);
            MetaMark mark = markModel.get(markId);
            markModel.delete(mark);
        }

        private int compareLoc(String nodeLocsA, Integer textLocA, String nodeLocsB, Integer textLocB) {
            int v = 0;
            if (nodeLocsA == null && nodeLocsB != null) {
                v = -1;
            }
            if (nodeLocsA != null && nodeLocsB == null) {
                v = 1;
            }
            if (nodeLocsA != null && nodeLocsB != null) {
                v = nodeLocsA.compareTo(nodeLocsB);
            }
            if (v == 0 && !(textLocA == null && textLocB == null)) {
                if (textLocA == null && textLocB != null) {
                    v = -1;
                }
                if (textLocA != null && textLocB == null) {
                    v = 1;
                }
                if (textLocA != null && textLocB != null) {
                    v = textLocA - textLocB;
                }
            }
            return v;
        }

        @Override
        public void cachedAnnotation_markAdded(String annId, String markId,
                                               String markJsonStr) {
            MetaAnnotation ann = findAnnotation(annId);
            MetaMark mark;
            try {
                JSONObject markJson = new JSONObject(markJsonStr);
                JSONObject loc = markJson.getJSONObject("loc");
                JSONObject start = loc.getJSONObject("start");
                JSONArray nodeLocs = start.getJSONArray("nodeLocs");
                String indexHash = BaseMetaEntity.nodeLocsToIndexHash(nodeLocs);
                mark = new MetaMark(ann, markId, markJsonStr, indexHash,
                        start.isNull("textLoc") ? null : start.getInt("textLoc"));
            } catch (Exception e) {
                Log.e(TAG, "Error parsing mark JSON: " + markJsonStr, e);
                return;
            }

            markModel.add(mark);
//			markDao.create(mark);

            String markStartLoc = mark.getStartLoc();
            Integer markStartTextLoc = mark.getStartTextLoc();
            String annStartLoc = ann.getStartLoc();
            Integer annStartTextLoc = ann.getStartTextLoc();
            if (annStartLoc == null || (markStartLoc != null &&
                    compareLoc(markStartLoc, markStartTextLoc, annStartLoc, annStartTextLoc) < 0)) {
                ann.setStartLoc(markStartLoc);
                ann.setStartTextLoc(markStartTextLoc);
//				annotationDao.update(ann);
                annotationModel.update(ann);
            }
        }

        @Override
        public boolean cachedAnnotation_hasNote(String annId) {
            MetaAnnotation ann = findAnnotation(annId);
            return ann.note != null;
        }

        @Override
        public boolean cachedAnnotation_hasMarks(String annId) {
            MetaAnnotation ann = findAnnotation(annId);
            return !ann.marks.isEmpty();
        }

        @Override
        public boolean cachedAnnotation_hasMark(String annId, String markId) {
            MetaAnnotation ann = findAnnotation(annId);
            return findMark(ann, markId) != null;
        }

        @Override
        public String cachedAnnotation_getNote(String annId) {
            MetaAnnotation ann = findAnnotation(annId);
            return ann.note;
        }

        @Override
        public String cachedAnnotation_getMarks(String annId) {
            MetaAnnotation ann = findAnnotation(annId);
            Set<Long> marks = annotationModel.getMarksOf(ann.getId());
            JSONArray array = new JSONArray();
            for (Long markId : marks) {
                MetaMark mark = markModel.get(markId);
                array.put(mark.getIdentifier());
            }

//			CloseableWrappedIterable<MetaMark> it = ann.marks.getWrappedIterable();
//			try {
//				for (MetaMark mark:it) {
//					array.put(mark.getIdentifier());
//				}
//			} finally {
//				try {
//					it.close();
//				} catch (SQLException e) {
//					//	ignore
//				}
//			}

            return array.toString();
        }

        @Override
        public String cachedAnnotation_getMark(String annId, String markId) {
            MetaAnnotation ann = findAnnotation(annId);
            return findMark(ann, markId).getJson();
        }

        @Override
        public void cachedAnnotation_copyMark(String dstAnnId, String markId,
                                              String srcAnnId) {
            MetaAnnotation ann = findAnnotation(dstAnnId);
            MetaMark srcMark = findMark(ann, srcAnnId);
            MetaMark dstMark = new MetaMark(ann, markId,
                    srcMark.getJson(),
                    srcMark.getStartLoc(),
                    srcMark.getStartTextLoc()
            );
            markModel.add(dstMark);
//			markDao.create(dstMark);
        }

        @Override
        public void annotationCache_removeAllAnnotations() {
            bookSegmentDetails.annotations.clear();
        }

        @Override
        public void annotationCache_marksReplaced(String jsonMarkIdList,
                                                  String newMarkId) {
            annotationCache_marksDeleted(jsonMarkIdList);
        }

        @Override
        public void annotationCache_marksDeleted(String jsonMarkIdList) {
            try {
//				Map<String,Object> fields = new HashMap<String,Object>(2);

                JSONArray array = new JSONArray(jsonMarkIdList);
                for (int i = 0; i < array.length(); ++i) {
                    String markId = array.getString(i);

                    MetaMark mark = markModel.get(markId);
                    markModel.delete(mark);

//			    	fields.put(MetaMark.COLUMN_NAME_IDENTIFIER, markId);
//			    	
//			    	MetaMark mark = DatabaseHelper.getFirstOrNull(markDao.queryForFieldValues(fields));
//			    	if (mark!=null) {
//			    		mark.delete();
//			    	}
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing deleted marks", e);
            }

        }

        @Override
        public boolean annotationCache_hasAnnotations() {
            return !bookSegmentDetails.annotations.isEmpty();
        }

        @Override
        public String annotationCache_getAnnotations() {
            JSONArray array = new JSONArray();

            for (Long annId : annotationModel.getIds()) {
                MetaAnnotation ann = annotationModel.get(annId);
                array.put(ann.getIdentifier());
            }

//			CloseableWrappedIterable<MetaAnnotation> it = bookSegmentDetails.annotations.getWrappedIterable();
//			try {
//				for (MetaAnnotation ann:it) {
//					array.put(ann.getIdentifier());
//				}
//			} finally {
//				try {
//					it.close();
//				} catch (SQLException e) {
//					//	ignore
//				}
//			}

            return array.toString();
        }

        @Override
        public String annotationCache_createNewMarkId() {
            return createNewMetaId("m");
        }

        @Override
        public String annotationCache_createNewAnnotationId() {
            return createNewMetaId("a");
        }

        private String createNewMetaId(String prefix) {
            long v = ++bookDetails.latestIssuedMetaId;
            databaseHelper.updateObject(bookDetails);
            String id = String.format("%s%08X", prefix, v);
            Log.d(TAG, "\tid = " + id);
            return id;
        }

        @Override
        public void annotationCache_cleanAnnotations() {
            List<MetaAnnotation> toBeDeleted = annotationModel.findAllAnnotationsWithoutMarks();

//			int sz = bookSegmentDetails.annotations.size();
//			List<MetaAnnotation> toBeDeleted = new ArrayList<MetaAnnotation>(sz);
//			CloseableWrappedIterable<MetaAnnotation> it = bookSegmentDetails.annotations.getWrappedIterable();
//			try {
//				for (MetaAnnotation ann:it) {
//					if (ann.marks.isEmpty()) {
//						toBeDeleted.add(ann);
//					}
//				}
//			} finally {
//				try {
//					it.close();
//				} catch (SQLException e) {
//					//	ignore
//				}
//			}

            for (MetaAnnotation ann : toBeDeleted) {
                annotationModel.delete(ann);
//				try {
//					ann.delete();
//				} catch (SQLException e) {
//					Log.e(TAG, "Error deleting annotation "+ann.getIdentifier(), e);
//				}					
            }
        }

        @Override
        public String annotationCache_annotationWithMark(String markId) {
            MetaAnnotation ann = findAnnotationWithMark(markId);
            if (ann != null) {
                return ann.getIdentifier();
            } else {
                return null;
            }
        }

        @Override
        public String annotationCache_createNewBookmarkId() {
            return createNewMetaId("b");
        }

        @Override
        public void annotationCache_newBookmark(String bookmarkId,
                                                String summary, String pointJson) {
            MetaBookmark bookmark;
            try {
                JSONObject point = new JSONObject(pointJson);
                JSONArray nodeLocs = point.getJSONArray("nodeLocs");
                String indexHash = BaseMetaEntity.nodeLocsToIndexHash(nodeLocs);
                bookmark = new MetaBookmark(bookSegmentDetails,
                        bookmarkId, summary, indexHash,
                        point.isNull("textLoc") ? null : point.getInt("textLoc"));
            } catch (Exception e) {
                Log.e(TAG, "Error while parsing point JSON: " + pointJson, e);
                return;
            }
            bookmark.pointJson = pointJson;
            bookmarkModel.add(bookmark);
//			bookmarkDao.create(bookmark);
        }

        @Override
        public void annotationCache_deleteBookmark(String bookmarkId) {
            MetaBookmark bookmark = findBookmark(bookmarkId);
            bookmarkModel.delete(bookmark);
//			bookmarkDao.delete(bookmark);
        }

        @Override
        public String annotationCache_getBookmarks() {
            JSONArray array = new JSONArray();

            for (Long bookmarkId : bookmarkModel.getIds()) {
                MetaBookmark bookmark = bookmarkModel.get(bookmarkId);
                array.put(bookmark.getIdentifier());
            }

//			CloseableWrappedIterable<MetaBookmark> it = bookSegmentDetails.bookmarks.getWrappedIterable();
//			try {
//				for (MetaBookmark ann:it) {
//					array.put(ann.getIdentifier());
//				}
//			} finally {
//				try {
//					it.close();
//				} catch (SQLException e) {
//					//	ignore
//				}
//			}
            return array.toString();
        }

        @Override
        public String annotationCache_getBookmark(String bookmarkId) {
            MetaBookmark bookmark = findBookmark(bookmarkId);
            return bookmark.pointJson;
        }
    };

    private EbookViewer.EbookViewerEventsHandler ebookViewerEventsHandler = new EbookViewer.EbookViewerEventsHandler() {

        @Override
        public void openExternally(String href) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(href));
            ReaderReaderFragment.this.startActivity(intent);
        }

        @Override
        public void onSelectionStarted() {
            if (eventsHandler != null) {
                eventsHandler.onSelectionStarted();
            }
        }

        @Override
        public void onSelectionEnded() {
            if (eventsHandler != null) {
                eventsHandler.onSelectionEnded();
            }
        }

        @Override
        public void onDocumentReady() {
            Log.d(TAG, "onDocumentReady");
            if (goToHref != null) {
                Log.d(TAG, "goto: " + goToHref);
                loadUrl(goToHref);
                goToHref = null;
            }
            closeWaitDialog();
        }

        @Override
        public void onDocumentFatalErrors() {
            Log.d(TAG, "onDocumentFatalErrors");
            if (goToHref != null) {
                Log.d(TAG, "goto: " + goToHref);
                loadUrl(goToHref);
                goToHref = null;
            }
            closeWaitDialog();
        }


        @Override
        public void goToBookSection(String href) {
            ReaderReaderFragment.this.goToBookSection(href);
        }
    };
}
