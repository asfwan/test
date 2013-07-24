package com.umranium.ebook.viewer;

import static com.umranium.ebookextra.Constants.TAG;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class JsInterfacing {

    public static <T> T checkReplaceNull(T t) {
        if (t == JSONObject.NULL) {
            return null;
        } else {
            return t;
        }
    }

    public static String quoteStr(String v) {
        if (v == null) {
            return "<Null>";
        } else {
            return "'" + v + "'";
        }
    }

    public static class BtnMsg {
        final String id;
        final String name;
        final String msg;
        final String extra;

        public BtnMsg(String id, String name, String msg, String extra) {
            this.id = id;
            this.name = name;
            this.msg = msg;
            this.extra = extra;
        }

        public static BtnMsg fromJson(JSONObject object) throws JSONException {
            return new BtnMsg(
                    object.isNull("id") ? null : object.getString("id"),
                    object.isNull("name") ? null : object.getString("name"),
                    object.isNull("msg") ? null : object.getString("msg"),
                    object.isNull("extra") ? null : object.getString("extra")
            );
        }

        @Override
        public String toString() {
            return String.format("id=%s, name=%s, msg=%s, extra=%s",
                    quoteStr(id), quoteStr(name), quoteStr(msg), quoteStr(extra));
        }
    }

    public static BtnMsg[] btnsFromJson(String jsonString) throws JSONException {
        Log.d(TAG, "jsonString=" + jsonString);

        JSONArray array = new JSONArray(jsonString);
        int len = array.length();
        BtnMsg[] btns = new BtnMsg[len];

        for (int i = 0; i < len; ++i) {
            btns[i] = BtnMsg.fromJson(array.getJSONObject(i));
        }

        return btns;
    }

}
