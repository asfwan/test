package com.umranium.ebook.sharedres;

import static com.umranium.ebookextra.Constants.TAG;

import java.util.HashMap;
import java.util.Map;

import android.util.Log;

import com.j256.ormlite.dao.RuntimeExceptionDao;
import com.umranium.ebook.model.CommonResource;
import com.umranium.ebook.services.DatabaseHelper;

public class DbSharedResources extends SharedResources {

    private DatabaseHelper databaseHelper;
    private boolean cacheValues;
    private static final Map<String, String> resources = new HashMap<String, String>(10);

    public DbSharedResources(DatabaseHelper databaseHelper, boolean cacheValues) {
        this.databaseHelper = databaseHelper;
        this.cacheValues = cacheValues;
    }

    private CommonResource findResource(String name) {
        RuntimeExceptionDao<CommonResource, Long> dao =
                databaseHelper.getClassDao(CommonResource.class);
        CommonResource resource = DatabaseHelper.getFirstOrNull(dao.queryForEq(
                CommonResource.COLUMN_NAME_NAME,
                (Object) name
        ));
        if (resource == null) {
            throw new RuntimeException("Unable to load resource '" + name + "' from database.");
        }
        return resource;
    }

    private String loadContent(String name) {
        CommonResource resource = findResource(name);
        String content = resource.content;
        return content;
    }

    @Override
    public String getResource(String name) {
        if (cacheValues) {
            if (!resources.containsKey(name)) {
                synchronized (resources) {
                    if (!resources.containsKey(name)) {
                        Log.d(TAG, "\tLoading shared resource '" + name + "' from database");
                        String content = loadContent(name);
                        resources.put(name, content);
                    }
                }
            }
            return resources.get(name);
        } else {
            return loadContent(name);
        }
    }

}