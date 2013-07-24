package com.umranium.ebook.services;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

import com.j256.ormlite.android.apptools.OrmLiteSqliteOpenHelper;
import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;
import com.umranium.ebook.model.BaseEntity;
import com.umranium.ebook.model.CommonResource;
import com.umranium.ebook.model.Settings;
import com.umranium.ebook.sharedres.AssetsSharedResources;
import com.umranium.ebook.sharedres.SharedResources;
import com.umranium.ebookextra.Constants;

/**
 * Database helper class used to manage the creation and upgrading of your database. This class also usually provides
 * the DAOs used by the other classes.
 */
public class DatabaseHelper extends OrmLiteSqliteOpenHelper {

    private final static String TAG = "DatabaseHelper";

    private static final Class<?>[] DAO_CLASSES = {
            com.umranium.ebook.model.Book.class,
            com.umranium.ebook.model.BookResource.class,
            com.umranium.ebook.model.BookSegment.class,
            com.umranium.ebook.model.BookTocEntry.class,
            com.umranium.ebook.model.CommonResource.class,
            com.umranium.ebook.model.MetaAnnotation.class,
            com.umranium.ebook.model.MetaMark.class,
            com.umranium.ebook.model.MetaBookmark.class,
            com.umranium.ebook.model.Settings.class,
            com.umranium.ebook.model.UserBookDetails.class,
            com.umranium.ebook.model.UserBookSegmentDetails.class,
            com.umranium.ebook.model.UserDetails.class,
            com.umranium.ebook.model.UserLibrary.class,
    };

    private Context context;

    // the DAO object we use to access the SimpleData table
    private Map<Class<?>, RuntimeExceptionDao<?, Long>> classDaos =
            new HashMap<Class<?>, RuntimeExceptionDao<?, Long>>(DAO_CLASSES.length);

    public DatabaseHelper(Context context) {
        super(context,
                (new File(new File(
                        Environment.getExternalStorageDirectory(),
                        Constants.EXTERNAL_DIR),
                        Constants.DATABASE_NAME)).getAbsolutePath(),
                null,
                Constants.DATABASE_VERSION
                //(int)((System.currentTimeMillis()/(60*1000L)) & 0xFFFFFFFFL)
        );
        this.context = context;
    }


    /**
     * This is called when the database is first created. Usually you should call createTable statements here to create
     * the tables that will store your data.
     */
    @Override
    public void onCreate(SQLiteDatabase db, ConnectionSource connectionSource) {
        Log.i(TAG, "onCreate");

        try {
            for (Class<?> cl : DAO_CLASSES) {
                TableUtils.createTable(connectionSource, cl);
            }
        } catch (SQLException e) {
            Log.e(TAG, "Can't create database", e);
            throw new RuntimeException(e);
        }

        try {
            RuntimeExceptionDao<Settings, Long> dao =
                    getClassDao(Settings.class);

            Settings settings = new Settings();
            dao.create(settings);
        } catch (RuntimeException e) {
            Log.e(TAG, "Error while creating settings object", e);
            throw e;
        }
    }

    /**
     * This is called when your application is upgraded and it has a higher version number. This allows you to adjust
     * the various data to match the new version number.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, ConnectionSource connectionSource, int oldVersion, int newVersion) {
        try {
            Log.i(TAG, "onUpgrade");
            for (Class<?> cl : DAO_CLASSES) {
                TableUtils.dropTable(connectionSource, cl, true);
            }

            // after we drop the old databases, we create the new ones
            onCreate(db, connectionSource);
        } catch (SQLException e) {
            Log.e(TAG, "Can't drop databases", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the RuntimeExceptionDao (Database Access Object) version of a Dao for our SimpleData class. It will
     * create it or just give the cached value. RuntimeExceptionDao only through RuntimeExceptions.
     */
    @SuppressWarnings("unchecked")
    synchronized
    public <T> RuntimeExceptionDao<T, Long> getClassDao(Class<T> cl) {
        if (classDaos.containsKey(cl)) {
            return (RuntimeExceptionDao<T, Long>) classDaos.get(cl);
        }

        RuntimeExceptionDao<T, Long> dao = getRuntimeExceptionDao(cl);
        classDaos.put(cl, dao);
        //dao.setObjectCache(true);

        return dao;
    }


    @SuppressWarnings("unchecked")
    public <T extends BaseEntity<?>> T getFullObject(T idOnlyObject) {
        RuntimeExceptionDao<T, Long> dao = getClassDao((Class<T>) idOnlyObject.getClass());
        return dao.queryForId(idOnlyObject.getId());
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseEntity<?>> int updateObject(T object) {
        RuntimeExceptionDao<T, Long> dao = getClassDao((Class<T>) object.getClass());
        return dao.update(object);
    }

    public void checkAndUpdateCommonResources() {
        long lastAppUpdateTime = 0;
        PackageManager packageManager = context.getPackageManager();
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(
                    context.getPackageName(), 0);
            lastAppUpdateTime = packageInfo.lastUpdateTime;
        } catch (NameNotFoundException e) {
            throw new RuntimeException(e);
        }

        SharedResources sharedResources = new AssetsSharedResources(context.getAssets(), false);
        RuntimeExceptionDao<CommonResource, Long> dao = getClassDao(CommonResource.class);

        updateResource(dao, CommonResource.RESOURCE_JQUERY, lastAppUpdateTime, sharedResources);
        updateResource(dao, CommonResource.RESOURCE_ANDROID_SELECTION, lastAppUpdateTime, sharedResources);
        updateResource(dao, CommonResource.RESOURCE_DOM_INTEROP, lastAppUpdateTime, sharedResources);
        updateResource(dao, CommonResource.RESOURCE_SYSTEM_STYLES, lastAppUpdateTime, sharedResources);
        updateResource(dao, CommonResource.RESOURCE_EBOOK_DART, lastAppUpdateTime, sharedResources);
        updateResource(dao, CommonResource.RESOURCE_DART, lastAppUpdateTime, sharedResources);
    }

    private void updateResource(RuntimeExceptionDao<CommonResource, Long> dao, String id, long lastAppUpdate, SharedResources otherSource) {
        CommonResource res = getFirstOrNull(dao.queryForEq("name", id));
        if (res == null) {
            String value = otherSource.getResource(id);
            res = new CommonResource(id, value);
            dao.create(res);
        } else {
            if (res.getLastUpdateTime().getTime() < lastAppUpdate) {
                String value = otherSource.getResource(id);
                res.content = value;
                dao.update(res);
            }
        }
    }

    /**
     * Close the database connections and clear any cached DAOs.
     */
    @Override
    public void close() {
        super.close();
        classDaos.clear();
    }

    public static final <E> E getFirstOrDefault(List<E> list, E def) {
        if (list.isEmpty()) {
            return def;
        } else {
            return list.get(0);
        }
    }

    public static final <E> E getFirstOrNull(List<E> list) {
        return getFirstOrDefault(list, null);
    }

}
