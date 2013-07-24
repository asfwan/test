package com.umranium.ebook.model;

import java.io.Serializable;

import javax.persistence.Entity;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;

@Entity(name = "user_library")
public class UserLibrary extends BaseEntity<UserLibrary> implements Serializable {

    private static final long serialVersionUID = 8872688229064811886L;

    @DatabaseField(foreign = true, canBeNull = true, foreignAutoRefresh = true)
    protected UserDetails userDetails;

    public UserLibrary() {
    }

    public UserLibrary(UserDetails userDetails) {
        this.userDetails = userDetails;
    }

    public UserDetails getUserDetails() {
        return userDetails;
    }

    @ForeignCollectionField(eager = false, foreignFieldName = "userLibrary", orderColumnName = "id")
    public ForeignCollection<UserBookDetails> userBookDetails;

}
