package com.umranium.ebook.viewer;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

public class ScreenDensityHelper {

    private static double getScreenDensity(Context context) {
        // Get display from context
        Display display = ((WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        // Calculate min bound based on metrics
        DisplayMetrics metrics = new DisplayMetrics();
        display.getMetrics(metrics);
        return metrics.densityDpi / 160.0;
    }

    //	private Context context;
    private double density;

    public ScreenDensityHelper(Context context) {
//		this.context = context;
        this.density = getScreenDensity(context);
    }

    // *****************************************************
    // *
    // * Density Conversion
    // *
    // *****************************************************

    /**
     * Returns the density dependent value of the given float
     *
     * @param val
     * @return
     */
    public float getDensityDependentValue(float val) {
        return (float) (val * density);
    }

    /**
     * Returns the density independent value of the given float
     *
     * @param val
     * @return
     */
    public float getDensityIndependentValue(float val) {
        return (float) (val / density);
    }

}
