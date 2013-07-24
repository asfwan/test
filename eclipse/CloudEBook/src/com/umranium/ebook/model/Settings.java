package com.umranium.ebook.model;

import java.io.Serializable;

import javax.persistence.Entity;

import android.util.Log;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.misc.BaseDaoEnabled;
import com.umranium.ebookextra.Constants;

@Entity(name = "settings")
public class Settings extends BaseDaoEnabled<Settings, Long> implements Serializable {

    private static final long serialVersionUID = -6739346735658389323L;

    @DatabaseField(canBeNull = false, id = true)
    protected Long id;

    //	@OneToMany
//	@Column(nullable=true)
    @DatabaseField(foreign = true, canBeNull = true, foreignAutoRefresh = true)
    protected UserDetails lastLoggedInUser;

    public Settings() {
        this.id = 0L;
    }

    public UserDetails getLastLoggedInUser() {
        return lastLoggedInUser;
    }

    public void setLastLoggedInUser(UserDetails lastLoggedInUser) {
        this.lastLoggedInUser = lastLoggedInUser;
        internUpdate();
    }

    private void internUpdate() {
        try {
            super.update();
        } catch (Exception e) {
            Log.e(Constants.TAG, "Error while updating settings DB", e);
        }
    }

}
