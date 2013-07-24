package com.umranium.ebook.model;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;

public class BaseEntity<T> {

    @DatabaseField(generatedId = true)
    protected Long id;

    @DatabaseField(version = true, dataType = DataType.DATE_LONG)
    protected java.util.Date lastUpdateTime;

    @DatabaseField(dataType = DataType.DATE_LONG)
    public java.util.Date lastServerUpdateTime;

    public Long getId() {
        return id;
    }

    public java.util.Date getLastUpdateTime() {
        return lastUpdateTime;
    }

}
