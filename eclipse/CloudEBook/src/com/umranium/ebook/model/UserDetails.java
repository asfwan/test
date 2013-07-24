package com.umranium.ebook.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;

import com.j256.ormlite.field.DatabaseField;

@Entity(name = "user_details")
public class UserDetails extends BaseEntity<UserDetails> implements Serializable {

    private static final long serialVersionUID = -7241329492917896232L;

    @Column(nullable = false, length = DbCommon.MAX_DESCR_STR_LEN, unique = true)
    protected String username;

    @DatabaseField(foreign = true, canBeNull = true, foreignAutoRefresh = true)
    public UserLibrary userLibrary;

//	@Column(nullable=false, length=DbCommon.MAX_DESCR_STR_LEN)
//	String firstName;
//	
//	@Column(nullable=false, length=DbCommon.MAX_DESCR_STR_LEN)
//	String lastName;

    public UserDetails() {
    }

    public UserDetails(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }


}
