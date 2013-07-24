package com.umranium.ebook.model;

import javax.persistence.Column;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.j256.ormlite.field.DatabaseField;

public class BaseMetaEntity<T extends BaseMetaEntity<?>> extends BaseEntity<T> {

    public static final String COLUMN_NAME_IDENTIFIER = "identifier";

    @DatabaseField(canBeNull = false, index = true)
    protected String identifier;

    @Column(nullable = false)
    protected String startLoc;

    @Column(nullable = true)
    protected Integer startTextLoc;

    public BaseMetaEntity() {
    }

    public BaseMetaEntity(String identifier, String startLoc, Integer startTextLoc) {
        this.identifier = identifier;
        this.startLoc = startLoc;
        this.startTextLoc = startTextLoc;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getStartLoc() {
        return startLoc;
    }

    public Integer getStartTextLoc() {
        return startTextLoc;
    }

    public static final String DEF_START_LOC = "000";

    public static String nodeLocsToIndexHash(JSONArray nodeLocs) throws JSONException {
        StringBuffer sb = new StringBuffer(nodeLocs.length() * 3);
        for (int i = 0; i < nodeLocs.length(); ++i) {
            JSONObject nodeLoc = nodeLocs.getJSONObject(i);
            int index = nodeLoc.getInt("index");
            sb.append(String.format("%03d", index));
        }
        return sb.toString();
    }


}
