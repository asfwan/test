package com.umranium.ebook.services;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.xml.sax.InputSource;

import android.app.Service;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.flurry.android.FlurryAgent;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.umranium.ebook.epub.DomHelper;
import com.umranium.ebook.epub.EbookException;
import com.umranium.ebook.epub.EpubDomPreprocessor;
import com.umranium.ebook.model.Book;
import com.umranium.ebook.model.BookSegment;
import com.umranium.ebook.model.Settings;
import com.umranium.ebook.model.UserBookDetails;
import com.umranium.ebook.model.UserBookSegmentDetails;
import com.umranium.ebook.model.UserDetails;
import com.umranium.ebook.model.UserLibrary;
import com.umranium.ebook.sharedres.DbSharedResources;
import com.umranium.ebook.sharedres.SharedResources;
import com.umranium.ebookextra.Constants;

public class WebHostService extends Service {

    private static final String TAG = "WebHostService";

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static int CACHE_DURATION = 24 * 3600;

    private WebHostServiceBinder binder = new WebHostServiceBinder();
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Server server;
    private int serverPort;
    private boolean serverReady = false;

    private EpubDomPreprocessor epubDomPreprocessor;
    private TargetContentHandler unknownTargetHandler;
    private TargetContentHandler errorTargetHandler;
    private DbResourceTargetHandler dbResourceTargetHandler;

    private Handler handler = new AbstractHandler() {
        @Override
        public void handle(String target, Request request, HttpServletRequest mainRequestObject,
                           HttpServletResponse response) throws IOException, ServletException {
            Log.d(TAG, "request: target=" + target);

            dbResourceTargetHandler.handle(target, request, mainRequestObject, response);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");
        return binder;
    }

    private DatabaseHelper databaseHelper;
    private SharedResources sharedResources;
    private Settings settings;
    private UserDetails loggedInUser;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "onCreate()");

        FlurryAgent.setCaptureUncaughtExceptions(true);
        FlurryAgent.setLogEnabled(true);
        FlurryAgent.setLogLevel(Log.DEBUG);
        FlurryAgent.setLogEvents(true);
        FlurryAgent.onStartSession(this, Constants.FLURRY_KEY);
        executor.execute(new InitServerRunnable());

        //this.startService(new Intent(this, this.getClass()));
    }

    class InitServerRunnable implements Runnable {
        @Override
        public void run() {
            Log.d(TAG, "Initializing Database");
            databaseHelper = new DatabaseHelper(WebHostService.this);
            databaseHelper.checkAndUpdateCommonResources();

            Log.d(TAG, "Loading settings");
            settings = databaseHelper.getClassDao(Settings.class).queryForId(0L);
            setLoggedInUser(settings.getLastLoggedInUser());

            sharedResources = new DbSharedResources(databaseHelper, false);

            Log.d(TAG, "Preparing Dom Preprocessor");
            epubDomPreprocessor = new EpubDomPreprocessor(sharedResources);
            Log.d(TAG, "Dom Preprocessor Ready");

            Log.d(TAG, "Starting Jetty Server");
            server = new Server(7685);
            server.setHandler(handler);

            try {
                server.start();
            } catch (Exception e) {
                Log.e(TAG, "Error while starting web server", e);
            }

            serverPort = server.getConnectors()[0].getLocalPort();
            Log.d(TAG, "serverPort=" + serverPort);

            try {
                unknownTargetHandler = new UnknownTargetHandler();
            } catch (IOException e) {
                throw new RuntimeException("Error while creating unknown target handler", e);
            }

            try {
                errorTargetHandler = new ErrorTargetHandler();
            } catch (IOException e) {
                throw new RuntimeException("Error while creating error target handler", e);
            }

            dbResourceTargetHandler = new DbResourceTargetHandler();

            checkLoadEpubs();

            serverReady = true;
            Log.d(TAG, "Server ready");

            binder.notifyServerStarted();
        }
    }

    @Override
    public void onDestroy() {

        Log.d(TAG, "onDestroy()");

        executor.shutdownNow();

        if (server != null) {
            try {
                if (server.isStarted()) {
                    server.stop();
                }
                server.destroy();
            } catch (Exception e) {
                Log.e(TAG, "Error while stopping web server", e);
            }
        }

        server = null;
        serverPort = -1;
        serverReady = false;

        FlurryAgent.onEndSession(this);
        super.onDestroy();
    }

    private void setLoggedInUser(UserDetails loggedInUser) {
        this.loggedInUser = loggedInUser;

        if (settings != null) {
            settings.setLastLoggedInUser(loggedInUser);
        }

        if (this.loggedInUser != null) {
            FlurryAgent.setUserId(this.loggedInUser.getUsername());
        } else {
            FlurryAgent.setUserId(null);
        }
    }

    private void checkLoadEpubs() {
        RuntimeExceptionDao<com.umranium.ebook.model.Book, Long> bookDao =
                databaseHelper.getClassDao(com.umranium.ebook.model.Book.class);

        if (!bookDao.queryForAll().isEmpty()) {
            return;
        }

        Log.i(TAG, "Loading ePubs from embedded assets");

        RuntimeExceptionDao<com.umranium.ebook.model.BookResource, Long> resourceDao =
                databaseHelper.getClassDao(com.umranium.ebook.model.BookResource.class);
        RuntimeExceptionDao<com.umranium.ebook.model.BookSegment, Long> segmentDao =
                databaseHelper.getClassDao(com.umranium.ebook.model.BookSegment.class);
        RuntimeExceptionDao<com.umranium.ebook.model.BookTocEntry, Long> tocEntryDao =
                databaseHelper.getClassDao(com.umranium.ebook.model.BookTocEntry.class);

        AssetManager assetManager = this.getAssets();
        try {
            String[] assets = assetManager.list("");
            for (String assetName : assets) {
                if (assetName.toLowerCase(Locale.getDefault()).endsWith(".epub")) {

                    nl.siegmann.epublib.epub.EpubReader epubReader = new nl.siegmann.epublib.epub.EpubReader();
                    nl.siegmann.epublib.domain.Book epub = epubReader.readEpub(assetManager.open(assetName));

                    com.umranium.ebook.model.Book book = new com.umranium.ebook.model.Book(
                            assetName.replaceAll("[^0-9A-Za-z_]+", "_"));

                    nl.siegmann.epublib.domain.Metadata meta = epub.getMetadata();

                    {
                        StringBuilder metaSb = new StringBuilder();
                        if (meta.getAuthors() != null && !meta.getAuthors().isEmpty()) {
                            boolean first = true;
                            for (nl.siegmann.epublib.domain.Author author : meta.getAuthors()) {
                                if (first) {
                                    first = false;
                                } else {
                                    metaSb.append(", ");
                                }
                                metaSb.append(author.getFirstname()).append(" ").append(author.getLastname());
                            }
                        }
                        book.authors = metaSb.toString();
                    }
                    {
                        StringBuilder metaSb = new StringBuilder();
                        if (meta.getPublishers() != null && !meta.getPublishers().isEmpty()) {
                            boolean first = true;
                            for (String publisher : meta.getPublishers()) {
                                if (first) {
                                    first = false;
                                } else {
                                    metaSb.append(", ");
                                }
                                metaSb.append(publisher);
                            }
                        }
                        book.publisher = metaSb.toString();
                    }
                    book.edition = "";
                    book.title = epub.getTitle();
                    if (epub.getCoverImage() != null) {
                        book.coverResourceHref = "/" + book.getIdentifier() + "/" + epub.getCoverImage().getHref();
                    }
                    bookDao.create(book);

                    for (nl.siegmann.epublib.domain.Resource epubRes : epub.getResources().getAll()) {
                        com.umranium.ebook.model.BookResource bkRes = new com.umranium.ebook.model.BookResource(book);
                        bkRes.href = "/" + book.getIdentifier() + "/" + DomHelper.removeAnyAnchor(epubRes.getHref());
                        bkRes.encoding = epubRes.getInputEncoding();
                        bkRes.mediaType = epubRes.getMediaType().getName();
                        bkRes.data = epubRes.getData();

                        resourceDao.create(bkRes);
                    }

                    for (nl.siegmann.epublib.domain.Resource content : epub.getContents()) {
                        com.umranium.ebook.model.BookSegment bkSeg = new com.umranium.ebook.model.BookSegment(book);
                        bkSeg.href = "/" + book.getIdentifier() + "/" + DomHelper.removeAnyAnchor(content.getHref());

                        segmentDao.create(bkSeg);
                    }

                    if (epub.getTableOfContents() != null && !epub.getTableOfContents().getTocReferences().isEmpty()) {
                        processToc(tocEntryDao, book, epub.getTableOfContents().getTocReferences(), null);
                    } else {
                        for (int i = 0; i < epub.getContents().size(); ++i) {
                            nl.siegmann.epublib.domain.Resource content = epub.getContents().get(i);

                            com.umranium.ebook.model.BookTocEntry entry = new com.umranium.ebook.model.BookTocEntry(book);

                            entry.title = "Chapter " + (i + 1);
                            if (content.getTitle() != null) {
                                entry.title = content.getTitle();
                            }
                            entry.href = "/" + book.getIdentifier() + "/" + content.getHref();

                            tocEntryDao.create(entry);
                        }
                    }

                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Error while loading ePub assets", e);
        }
    }

    private void processToc(RuntimeExceptionDao<com.umranium.ebook.model.BookTocEntry, Long> tocEntryDao,
                            com.umranium.ebook.model.Book book, List<nl.siegmann.epublib.domain.TOCReference> refs,
                            com.umranium.ebook.model.BookTocEntry parent) {
        for (int i = 0; i < refs.size(); ++i) {
            nl.siegmann.epublib.domain.TOCReference tocRef = refs.get(i);
            com.umranium.ebook.model.BookTocEntry entry = new com.umranium.ebook.model.BookTocEntry(book);

            if (parent != null) {
                entry.title = parent.title + "." + (i + 1);
            } else {
                entry.title = "Chapter " + (i + 1);
            }
            if (tocRef.getTitle() != null) {
                entry.title = tocRef.getTitle();
            }
            entry.href = "/" + book.getIdentifier() + "/" + tocRef.getCompleteHref();
            entry.parent = parent;

            tocEntryDao.create(entry);

            if (tocRef.getChildren() != null && !tocRef.getChildren().isEmpty()) {
                processToc(tocEntryDao, book, tocRef.getChildren(), entry);
            }
        }
    }

    private class DbResourceTargetHandler implements TargetContentHandler {

        @Override
        public void handle(String target, Request request,
                           HttpServletRequest mainRequestObject, HttpServletResponse response)
                throws IOException, ServletException {
            RuntimeExceptionDao<com.umranium.ebook.model.BookResource, Long> resourceDao =
                    databaseHelper.getClassDao(com.umranium.ebook.model.BookResource.class);
            List<com.umranium.ebook.model.BookResource> foundResources =
                    resourceDao.queryForFieldValues(Collections.singletonMap("href", (Object) target));
            if (foundResources.isEmpty()) {
                unknownTargetHandler.handle(target, request, mainRequestObject, response);
                return;
            }

            com.umranium.ebook.model.BookResource resource = foundResources.get(0);

            String encoding = resource.encoding;
            String mediaType = resource.mediaType;

            Log.d(TAG, "Fetching resource " + resource.href);
            Log.d(TAG, "encoding = " + encoding);
            Log.d(TAG, "mediaType = " + mediaType);

            boolean requiresPreprocessing = false;

            if (mediaType.equalsIgnoreCase("application/xhtml+xml")) {
                mediaType = "text/html";
                requiresPreprocessing = true;
            } else {
                response.setHeader("Cache-Control", "max-age=" + CACHE_DURATION);
            }

            if (requiresPreprocessing) {
                Log.i(TAG, target + " requires preprocessing.");
                ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(resource.data);
                try {
                    String content = epubDomPreprocessor.preprocess(new InputSource(byteArrayInputStream));

                    response.setContentType(mediaType);
                    response.setCharacterEncoding(encoding);
                    response.setContentLength(content.length());
                    response.getWriter().write(content);
                    response.setStatus(HttpServletResponse.SC_OK);
                    ((Request) mainRequestObject).setHandled(true);
                } catch (EbookException e) {
                    Log.e(TAG, "Error while preprocessing content from resouce: " + resource.href, e);
                    errorTargetHandler.handle(target, request, mainRequestObject, response);
                }
            } else {
                response.setContentType(mediaType);
                response.setCharacterEncoding(encoding);
                response.setContentLength(resource.data.length);
                response.getOutputStream().write(resource.data);
                response.setStatus(HttpServletResponse.SC_OK);
                ((Request) mainRequestObject).setHandled(true);
            }
        }

    }

    private class UnknownTargetHandler implements TargetContentHandler {
        String errorHtml;

        UnknownTargetHandler() throws IOException {
            AssetManager assetManager = getAssets();
            errorHtml = DomHelper.streamToString(assetManager.open("unfound_target.html"), UTF8);
        }

        @Override
        public void handle(String target, Request request,
                           HttpServletRequest mainRequestObject, HttpServletResponse response)
                throws IOException, ServletException {
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write(String.format(errorHtml, target));
            ((Request) mainRequestObject).setHandled(true);
        }
    }

    ;

    private class ErrorTargetHandler implements TargetContentHandler {
        String errorHtml;

        ErrorTargetHandler() throws IOException {
            AssetManager assetManager = getAssets();
            errorHtml = DomHelper.streamToString(assetManager.open("content_error.html"), UTF8);
        }

        @Override
        public void handle(String target, Request request,
                           HttpServletRequest mainRequestObject, HttpServletResponse response)
                throws IOException, ServletException {
            response.setContentType("text/html");
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Cache-Control", "no-cache");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(errorHtml);
            ((Request) mainRequestObject).setHandled(true);
        }
    }

    ;

    /*
    private class StaticTextContentHandler implements TargetContentHandler {
        String content;
        String contentType;

        StaticTextContentHandler(String content, String contentType) {
            this.content = content;
            this.contentType = contentType;
        }

        @Override
        public void handle(String target, Request request,
                HttpServletRequest mainRequestObject, HttpServletResponse response)
                throws IOException, ServletException {
            response.setContentType(contentType);
            response.setHeader("Cache-Control", "max-age="+CACHE_DURATION);
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentLength(content.length());
            response.getWriter().write(content);
            ((Request)mainRequestObject).setHandled(true);
            Log.d(TAG, "Static text content "+target+" ("+contentType+") served.");
        }
    };

    private class StaticBinaryContentHandler implements TargetContentHandler {
        byte[] content;
        String contentType;

        StaticBinaryContentHandler(byte[] content, String contentType) {
            this.content = content;
            this.contentType = contentType;
        }

        @Override
        public void handle(String target, Request request,
                HttpServletRequest mainRequestObject, HttpServletResponse response)
                throws IOException, ServletException {
            response.setContentType(contentType);
            response.setHeader("Cache-Control", "max-age="+CACHE_DURATION);
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentLength(content.length);
            response.getOutputStream().write(content);
            ((Request)mainRequestObject).setHandled(true);
            Log.d(TAG, "Static binary content "+target+" ("+contentType+") served.");
        }
    };
    */
    private class WebHostServiceBinder extends Binder implements IWebHostService {
        final List<WebHostEventListener> listeners = new ArrayList<WebHostEventListener>();

        @Override
        public void addEventListener(WebHostEventListener listener) {
            synchronized (listeners) {
                listeners.add(listener);
            }
        }

        @Override
        public void removeEventListener(WebHostEventListener listener) {
            synchronized (listeners) {
                listeners.remove(listener);
            }
        }

        private void notifyServerStarted() {
            synchronized (listeners) {
                for (WebHostEventListener l : listeners) {
                    try {
                        l.serverStarted();
                    } catch (Exception e) {
                        Log.e(TAG, "Error while notifying listener that server has started", e);
                    }
                }
            }
        }

        @Override
        public boolean isServerStarted() {
            return server != null && server.isStarted() && serverReady;
        }

        @Override
        public int getPort() {
            return serverPort;
        }

        @Override
        public String getHostUrl() {
            return "http://127.0.0.1:" + serverPort;
        }

        @Override
        public DatabaseHelper getDatabaseHelper() {
            return databaseHelper;
        }

        @Override
        public Settings getSettings() {
            RuntimeExceptionDao<com.umranium.ebook.model.Settings, Long> dao =
                    databaseHelper.getClassDao(com.umranium.ebook.model.Settings.class);
            return dao.queryForId(0L);
        }

        @Override
        public UserDetails getLoggedInUser() {
            return loggedInUser;
        }

        @Override
        public UserDetails getUser(String username) {
            return DatabaseHelper.getFirstOrNull(
                    databaseHelper.getClassDao(UserDetails.class).queryForEq(
                            "username", username));
        }

        @Override
        public UserDetails createUser(String username) {
            UserDetails userDetails = new UserDetails(username);

            databaseHelper.getClassDao(UserDetails.class).create(userDetails);

            UserLibrary userLibrary = new UserLibrary(userDetails);

            databaseHelper.getClassDao(UserLibrary.class).create(userLibrary);

            userDetails.userLibrary = userLibrary;
            databaseHelper.getClassDao(UserDetails.class).update(userDetails);

            RuntimeExceptionDao<UserBookDetails, Long> userBookDetailsDao =
                    databaseHelper.getClassDao(UserBookDetails.class);
            for (Book book : databaseHelper.getClassDao(Book.class).queryForAll()) {
                UserBookDetails bookDetails = new UserBookDetails(userLibrary, book);
                userBookDetailsDao.create(bookDetails);
            }

            return databaseHelper.getClassDao(UserDetails.class).queryForId(userDetails.getId());
        }

        @Override
        public void logInUser(UserDetails user) {
            setLoggedInUser(user);
            Log.i(TAG, "User logged in " + user.getUsername());
        }

        @Override
        public UserBookDetails getUserBookDetails(UserDetails user, Book book) {
            RuntimeExceptionDao<UserBookDetails, Long> dao =
                    databaseHelper.getClassDao(UserBookDetails.class);
            Map<String, Object> fields = new HashMap<String, Object>(2);
            fields.put(UserBookDetails.COLUMN_NAME_USER_LIBRARY, user.userLibrary.getId());
            fields.put(UserBookDetails.COLUMN_NAME_BOOK, book.getId());
            return DatabaseHelper.getFirstOrNull(dao.queryForFieldValues(fields));
        }

        @Override
        public UserBookDetails createUserBookDetails(UserDetails user, Book book) {
            RuntimeExceptionDao<UserBookDetails, Long> dao =
                    databaseHelper.getClassDao(UserBookDetails.class);
            UserBookDetails userBookDetails = new UserBookDetails(user.userLibrary, book);
            dao.create(userBookDetails);
            return dao.queryForId(userBookDetails.getId());
        }

        @Override
        public UserBookSegmentDetails createUserBookSegmentDetails(
                UserBookDetails userBookDetails, BookSegment bookSegment) {
            RuntimeExceptionDao<UserBookSegmentDetails, Long> dao =
                    databaseHelper.getClassDao(UserBookSegmentDetails.class);
            UserBookSegmentDetails userBookSegmentDetails = new UserBookSegmentDetails(userBookDetails, bookSegment);
            dao.create(userBookSegmentDetails);
            return dao.queryForId(userBookSegmentDetails.getId());
        }

        public List<com.umranium.ebook.model.Book> getLibraryBooks() {
            RuntimeExceptionDao<com.umranium.ebook.model.Book, Long> dao =
                    databaseHelper.getClassDao(com.umranium.ebook.model.Book.class);
            return dao.queryForAll();
        }

        public List<com.umranium.ebook.model.BookResource> getResource(String href) {
            RuntimeExceptionDao<com.umranium.ebook.model.BookResource, Long> dao =
                    databaseHelper.getClassDao(com.umranium.ebook.model.BookResource.class);
            Map<String, Object> fieldValues = new HashMap<String, Object>(1);
            fieldValues.put("href", href);
            return dao.queryForFieldValues(fieldValues);
        }

        @Override
        public com.umranium.ebook.model.Book getBook(long id) {
            RuntimeExceptionDao<com.umranium.ebook.model.Book, Long> dao =
                    databaseHelper.getClassDao(com.umranium.ebook.model.Book.class);
            return dao.queryForId(id);
        }


    }

}
