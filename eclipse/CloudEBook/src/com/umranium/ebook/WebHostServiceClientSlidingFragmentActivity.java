package com.umranium.ebook;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

import com.slidingmenu.lib.SlidingMenu;
import com.slidingmenu.lib.app.SlidingActivityBase;
import com.slidingmenu.lib.app.SlidingActivityHelper;

public class WebHostServiceClientSlidingFragmentActivity extends
        WebHostServiceClientFragmentActivity implements SlidingActivityBase {

    private SlidingActivityHelper slidingActivityHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        slidingActivityHelper = new SlidingActivityHelper(this);
        slidingActivityHelper.onCreate(savedInstanceState);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        slidingActivityHelper.onPostCreate(savedInstanceState);
    }

    @Override
    public View findViewById(int id) {
        View v = super.findViewById(id);
        if (v != null)
            return v;
        return slidingActivityHelper.findViewById(id);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        slidingActivityHelper.onSaveInstanceState(outState);
    }

    @Override
    public void setContentView(int id) {
        setContentView(getLayoutInflater().inflate(id, null));
    }

    @Override
    public void setContentView(View v) {
        setContentView(v, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    @Override
    public void setContentView(View v, LayoutParams params) {
        super.setContentView(v, params);
        slidingActivityHelper.registerAboveContentView(v, params);
    }

    public void setBehindContentView(int id) {
        setBehindContentView(getLayoutInflater().inflate(id, null));
    }

    public void setBehindContentView(View v) {
        setBehindContentView(v, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    public void setBehindContentView(View v, LayoutParams params) {
        slidingActivityHelper.setBehindContentView(v, params);
    }

    public SlidingMenu getSlidingMenu() {
        return slidingActivityHelper.getSlidingMenu();
    }

    public void toggle() {
        slidingActivityHelper.toggle();
    }

    public void showContent() {
        slidingActivityHelper.showContent();
    }

    public void showMenu() {
        slidingActivityHelper.showMenu();
    }

    public void showSecondaryMenu() {
        slidingActivityHelper.showSecondaryMenu();
    }

    public void setSlidingActionBarEnabled(boolean b) {
        slidingActivityHelper.setSlidingActionBarEnabled(b);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean b = slidingActivityHelper.onKeyUp(keyCode, event);
        if (b) return b;
        return super.onKeyUp(keyCode, event);
    }

}
