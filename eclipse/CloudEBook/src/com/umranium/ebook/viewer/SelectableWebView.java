package com.umranium.ebook.viewer;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.Region;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;

import com.blahti.drag.DragController;
import com.blahti.drag.DragLayer;
import com.blahti.drag.DragListener;
import com.blahti.drag.DragSource;
import com.blahti.drag.MyAbsoluteLayout;
import com.umranium.ebook.R;

import static com.umranium.ebookextra.Constants.TAG;

public class SelectableWebView extends WebView implements OnTouchListener, OnLongClickListener, DragListener, JsCaller {

    private final String JS_INTERFACE_NAME = "TextSelection";

    /**
     * Context.
     */
    protected Context ctx;

    /**
     * The drag layer for selection.
     */
    private DragLayer mSelectionDragLayer;

    /**
     * The drag controller for selection.
     */
    private DragController mDragController;

    /**
     * The start selection handle.
     */
    private ImageView mStartSelectionHandle;

    /**
     * the end selection handle.
     */
    private ImageView mEndSelectionHandle;

    /**
     * The selection bounds.
     */
    private Rect mSelectionBounds = null;

    /**
     * The previously selected region.
     */
    protected Region lastSelectedRegion = null;

    /**
     * Selection mode flag.
     */
    protected boolean inSelectionMode = false;

    /**
     * The current content width.
     */
    protected int contentWidth = 0;

    /**
     * Identifier for the selection start handle.
     */
    private final int SELECTION_START_HANDLE = 0;

    /**
     * Identifier for the selection end handle.
     */
    private final int SELECTION_END_HANDLE = 1;

    /**
     * Last touched selection handle.
     */
    private int mLastTouchedSelectionHandle = -1;

    /**
     * Helper function to convert to and from density independent values
     */
    protected ScreenDensityHelper screenDensityHelper;

    /**
     * Helper class for calling javascript
     */
    private ToJsComm toJsComm;

    /**
     * Handler for calling code on the UI thread
     */
    protected Handler uiHandler;

    public SelectableWebView(Context context) {
        super(context);

        this.setup(context);
    }

    public SelectableWebView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        this.setup(context);
    }

    public SelectableWebView(Context context, AttributeSet attrs) {
        super(context, attrs);

        this.setup(context);
    }

    // *****************************************************
    // *
    // * Touch Listeners
    // *
    // *****************************************************

    private boolean mScrolling = false;
    private float mScrollDiffY = 0;
    private float mLastTouchY = 0;
    private float mScrollDiffX = 0;
    private float mLastTouchX = 0;

    @Override
    public boolean onTouch(View v, MotionEvent event) {

        Log.d(TAG, "onTouch");

        float xPoint = screenDensityHelper.getDensityIndependentValue(event.getX())
                / screenDensityHelper.getDensityIndependentValue(this.getScale());
        float yPoint = screenDensityHelper.getDensityIndependentValue(event.getY())
                / screenDensityHelper.getDensityIndependentValue(this.getScale());

        // TODO: Need to update this to use this.getScale() as a factor.

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            mLastTouchX = xPoint;
            mLastTouchY = yPoint;

            toJsComm.onTouch(xPoint, yPoint);

            // Flag scrolling for first touch
            // if(!this.isInSelectionMode())
            // mScrolling = true;

        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            // Check for scrolling flag
            if (!mScrolling) {
                this.endSelectionMode();
            }

            mScrollDiffX = 0;
            mScrollDiffY = 0;
            mScrolling = false;

        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {

            mScrollDiffX += (xPoint - mLastTouchX);
            mScrollDiffY += (yPoint - mLastTouchY);

            mLastTouchX = xPoint;
            mLastTouchY = yPoint;

            // Only account for legitimate movement.
            if (Math.abs(mScrollDiffX) > 10 || Math.abs(mScrollDiffY) > 10) {
                mScrolling = true;
            }

        }

        // If this is in selection mode, then nothing else should handle this
        // touch
        return false;
    }

    @Override
    public boolean onLongClick(View v) {

        Log.d(TAG, "onLongClick");

        // Tell the javascript to handle this if not in selection mode
        // if(!this.isInSelectionMode()){
//		this.loadUrl("javascript:android.selection.longTouch();");
        toJsComm.longTouch();
        mScrolling = true;
        // }

        // Don't let the webview handle it
        return true;
    }

    // *****************************************************
    // *
    // * Setup
    // *
    // *****************************************************

    /**
     * Setups up the web view.
     *
     * @param context
     */
    @SuppressLint("SetJavaScriptEnabled")
    private void setup(Context context) {
        this.ctx = context;

        if (this.isInEditMode()) {
            return;
        }

        this.screenDensityHelper = new ScreenDensityHelper(context);
        this.toJsComm = new ToJsComm();
        this.uiHandler = new Handler(context.getMainLooper());

        // On Touch Listener
        this.setOnLongClickListener(this);
        this.setOnTouchListener(this);

        // Webview setup
        this.getSettings().setJavaScriptEnabled(true);
        //this.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

//		if (android.os.Build.VERSION.SDK_INT<9) {
//			this.getSettings().setPluginsEnabled(true);
//		} else {
//			this.getSettings().setPluginState(PluginState.ON_DEMAND);
//		}

        this.getSettings().setBuiltInZoomControls(false);

        // Zoom out fully
        // this.getSettings().setLoadWithOverviewMode(true);
        // this.getSettings().setUseWideViewPort(true);

        // Javascript interfaces
        this.addJavascriptInterface(this.javascriptInterface, JS_INTERFACE_NAME);

        // Create the selection handles
        createSelectionLayer(context);

        // Set to the empty region
        Region region = new Region();
        region.setEmpty();
        this.lastSelectedRegion = region;
    }

    protected void runOnUiThread(final Runnable runnable, boolean sync) {
        if (!sync) {
            uiHandler.post(runnable);
            return;
        }

        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            runnable.run();
        } else {
            final Object signal = new Object();
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    runnable.run();
                    synchronized (signal) {
                        signal.notify();
                    }
                }
            });
            synchronized (signal) {
                try {
                    signal.wait(5000);
                } catch (InterruptedException e) {
                    Log.d(TAG, "Interrupted while waiting for signal", e);
                }
            }
        }
    }

    // *****************************************************
    // *
    // * Selection Layer Handling
    // *
    // *****************************************************

    /**
     * Creates the selection layer.
     *
     * @param context
     */
    protected void createSelectionLayer(Context context) {

        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mSelectionDragLayer = (DragLayer) inflater.inflate(
                R.layout.selection_drag_layer, null);

        // Make sure it's filling parent
        this.mDragController = new DragController(context);
        this.mDragController.setDragListener(this);
        this.mDragController.addDropTarget(mSelectionDragLayer);
        this.mSelectionDragLayer.setDragController(mDragController);

        this.mStartSelectionHandle = (ImageView) this.mSelectionDragLayer
                .findViewById(R.id.startHandle);
        this.mStartSelectionHandle.setTag(Integer
                .valueOf(SELECTION_START_HANDLE));
        this.mEndSelectionHandle = (ImageView) this.mSelectionDragLayer
                .findViewById(R.id.endHandle);
        this.mEndSelectionHandle.setTag(Integer.valueOf(SELECTION_END_HANDLE));

        OnTouchListener handleTouchListener = new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                boolean handledHere = false;

                final int action = event.getAction();

                // Down event starts drag for handle.
                if (action == MotionEvent.ACTION_DOWN) {
                    handledHere = startDrag(v);
                    mLastTouchedSelectionHandle = (Integer) v.getTag();
                }

                return handledHere;

            }

        };

        this.mStartSelectionHandle.setOnTouchListener(handleTouchListener);
        this.mEndSelectionHandle.setOnTouchListener(handleTouchListener);

    }

    /**
     * Starts selection mode on the UI thread
     */
    private Runnable startSelectionModeHandler = new Runnable() {

        public void run() {

            if (mSelectionBounds == null)
                return;

            addView(mSelectionDragLayer);

            drawSelectionHandles();

            int contentHeight = (int) Math.ceil(
                    screenDensityHelper.getDensityDependentValue(getContentHeight()));

            // Update Layout Params
            ViewGroup.LayoutParams layerParams = mSelectionDragLayer
                    .getLayoutParams();
            layerParams.height = contentHeight;
            layerParams.width = contentWidth;
            mSelectionDragLayer.setLayoutParams(layerParams);

        }

    };

    /**
     * Starts selection mode.
     *
     * @param selectionBounds
     */
    public void startSelectionMode() {
//		this.startSelectionModeHandler.sendEmptyMessage(0);
        runOnUiThread(startSelectionModeHandler, true);
    }

    // Ends selection mode on the UI thread
    private Runnable endSelectionModeHandler = new Runnable() {
        public void run() {
            Log.d(TAG, "endSelectionModeHandler");

            removeView(mSelectionDragLayer);

            //TODO: Handle end of selection events (e.g. context menu)
//			if (getParent() != null && mContextMenu != null
//					&& contextMenuVisible) {
//				// This will throw an error if the webview is being redrawn.
//				// No error handling needed, just need to stop the crash.
//				try {
//					mContextMenu.dismiss();
//				} catch (Exception e) {
//
//				}
//			}

            mSelectionBounds = null;
            mLastTouchedSelectionHandle = -1;
//			loadUrl("javascript: android.selection.clearSelection();");
            toJsComm.endSelection();

        }
    };

    /**
     * Ends selection mode.
     */
    public void endSelectionMode() {

//		this.endSelectionModeHandler.sendEmptyMessage(0);
        runOnUiThread(endSelectionModeHandler, true);

    }

    /**
     * Calls the handler for drawing the selection handles.
     */
    private void drawSelectionHandles() {
//		this.drawSelectionHandlesHandler.sendEmptyMessage(0);
        runOnUiThread(drawSelectionHandlesHandler, true);
    }

    /**
     * Handler for drawing the selection handles on the UI thread.
     */
    private Runnable drawSelectionHandlesHandler = new Runnable() {
        public void run() {
            MyAbsoluteLayout.LayoutParams startParams = (com.blahti.drag.MyAbsoluteLayout.LayoutParams) mStartSelectionHandle
                    .getLayoutParams();
            startParams.x = (int) (mSelectionBounds.left - mStartSelectionHandle.getDrawable().getIntrinsicWidth() / 2);
            startParams.y = (int) (mSelectionBounds.top - mStartSelectionHandle.getDrawable().getIntrinsicHeight() + 1);

            // Stay on screen.
            startParams.x = (startParams.x < 0) ? 0 : startParams.x;
            startParams.y = (startParams.y < 0) ? 0 : startParams.y;

            mStartSelectionHandle.setLayoutParams(startParams);

            MyAbsoluteLayout.LayoutParams endParams = (com.blahti.drag.MyAbsoluteLayout.LayoutParams) mEndSelectionHandle
                    .getLayoutParams();
            endParams.x = (int) mSelectionBounds.right - mEndSelectionHandle.getDrawable().getIntrinsicWidth() / 2 - 2;
            endParams.y = (int) mSelectionBounds.bottom - 1;

            // Stay on screen
            endParams.x = (endParams.x < 0) ? 0 : endParams.x;
            endParams.y = (endParams.y < 0) ? 0 : endParams.y;

            mEndSelectionHandle.setLayoutParams(endParams);
        }
    };

    /**
     * Checks to see if this view is in selection mode.
     *
     * @return
     */
    public boolean isInSelectionMode() {

        return this.mSelectionDragLayer.getParent() != null;

    }

    // *****************************************************
    // *
    // * DragListener Methods
    // *
    // *****************************************************

    /**
     * Start dragging a view.
     */
    private boolean startDrag(View v) {
        // Let the DragController initiate a drag-drop sequence.
        // I use the dragInfo to pass along the object being dragged.
        // I'm not sure how the Launcher designers do this.
        Object dragInfo = v;
        mDragController.startDrag(v, mSelectionDragLayer, dragInfo,
                DragController.DRAG_ACTION_MOVE);
        return true;
    }

    @Override
    public void onDragStart(DragSource source, Object info, int dragAction) {
    }

    @Override
    public void onDragMove() {
        updateSelection(false);
//		this.runOnUiThread(new Runnable() {
//			@Override
//			public void run() {
//				SelectableWebView.this.invalidate();
//			}
//		}, false);
    }

    @Override
    public void onDragEnd() {
        updateSelection(true);
    }

    private void updateSelection(boolean finalPos) {
        float scale = screenDensityHelper.getDensityIndependentValue(this.getScale());

        if (mLastTouchedSelectionHandle == SELECTION_START_HANDLE) {
            MyAbsoluteLayout.LayoutParams startHandleParams = (MyAbsoluteLayout.LayoutParams) this.mStartSelectionHandle
                    .getLayoutParams();

            float startX = startHandleParams.x - this.getScrollX() + this.mStartSelectionHandle.getDrawable().getIntrinsicWidth() / 2;
            float startY = startHandleParams.y - this.getScrollY() + this.mStartSelectionHandle.getDrawable().getIntrinsicHeight();

            startX = screenDensityHelper.getDensityIndependentValue(startX) / scale;
            startY = screenDensityHelper.getDensityIndependentValue(startY) / scale;

            if (startX > 0 && startY > 0) {
                toJsComm.setStartPos(startX, startY, finalPos);
            }
        }

        if (mLastTouchedSelectionHandle == SELECTION_END_HANDLE) {
            MyAbsoluteLayout.LayoutParams endHandleParams = (MyAbsoluteLayout.LayoutParams) this.mEndSelectionHandle
                    .getLayoutParams();

            float endX = endHandleParams.x - this.getScrollX() + this.mEndSelectionHandle.getDrawable().getIntrinsicWidth() / 2;
            float endY = endHandleParams.y - this.getScrollY();

            endX = screenDensityHelper.getDensityIndependentValue(endX) / scale;
            endY = screenDensityHelper.getDensityIndependentValue(endY) / scale;

            if (endX > 0 && endY > 0) {
                toJsComm.setEndPos(endX, endY, finalPos);
            }
        }
    }


    // *****************************************************
    // *
    // * Text Selection Javascript Interface Listener
    // *
    // *****************************************************

    private class SetContentWidthRunnable implements Runnable {

        float contentWidth;

        public SetContentWidthRunnable(float contentWidth) {
            this.contentWidth = contentWidth;
        }

        @Override
        public void run() {
            SelectableWebView.this.contentWidth =
                    (int) screenDensityHelper.getDensityDependentValue(contentWidth);
        }
    }

    private class UpdateHandlesRunnable implements Runnable {

        String handleBounds;
        String menuBounds;

        public UpdateHandlesRunnable(String handleBounds, String menuBounds) {
            this.handleBounds = handleBounds;
            this.menuBounds = menuBounds;
        }

        @Override
        public void run() {
            try {
                JSONObject selectionBoundsObject = new JSONObject(handleBounds);
                JSONObject menuBoundsObject = new JSONObject(menuBounds);

                float scale = screenDensityHelper.getDensityIndependentValue(getScale());

                Rect handleRect = new Rect();
                handleRect.left = (int) (screenDensityHelper.getDensityDependentValue(
                        selectionBoundsObject.getInt("left")) * scale);
                handleRect.top = (int) (screenDensityHelper.getDensityDependentValue(
                        selectionBoundsObject.getInt("top")) * scale);
                handleRect.right = (int) (screenDensityHelper.getDensityDependentValue(
                        selectionBoundsObject.getInt("right")) * scale);
                handleRect.bottom = (int) (screenDensityHelper.getDensityDependentValue(
                        selectionBoundsObject.getInt("bottom")) * scale);

                mSelectionBounds = handleRect;

                Rect displayRect = new Rect();
                displayRect.left = (int) (screenDensityHelper.getDensityDependentValue(
                        menuBoundsObject.getInt("left")) * scale);
                displayRect.top = (int) (screenDensityHelper.getDensityDependentValue(
                        menuBoundsObject.getInt("top") - 25) * scale);
                displayRect.right = (int) (screenDensityHelper.getDensityDependentValue(
                        menuBoundsObject.getInt("right")) * scale);
                displayRect.bottom = (int) (screenDensityHelper.getDensityDependentValue(
                        menuBoundsObject.getInt("bottom") + 25) * scale);

                if (!isInSelectionMode()) {
                    SelectableWebView.this.startSelectionMode();
                }

                // This will send the menu rect
                //showContextMenu(displayRect);

                drawSelectionHandles();

            } catch (JSONException e) {
                Log.d(TAG, "Error while processing selection changed event from JS", e);
            }
        }
    }

    SelectionJavascriptInterface javascriptInterface = new SelectionJavascriptInterface() {

        @Override
        public void setContentWidth(float contentWidth) {
            uiHandler.post(new SetContentWidthRunnable(contentWidth));
        }

        @Override
        public void updateHandles(String handleBounds, String menuBounds) {
            uiHandler.post(new UpdateHandlesRunnable(handleBounds, menuBounds));
        }

        @Override
        public void jsError(String error) {
            Log.e(TAG, "JSError: " + error);
        }
    };

    public void callJs(final String command) {
        Log.d(TAG, "JavaScript Call:" + command);
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            SelectableWebView.super.loadUrl("javascript:" + command);
        } else {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    SelectableWebView.super.loadUrl("javascript:" + command);
                }
            });
        }
    }

    public void loadUrl(final String url) {
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            SelectableWebView.super.loadUrl(url);
        } else {
            uiHandler.post(new Runnable() {
                @Override
                public void run() {
                    SelectableWebView.super.loadUrl(url);
                }
            });
        }
    }

    private class ToJsComm {
        public void onTouch(float x, float y) {
            callJs(String.format("android.selection.onTouch(%f, %f);", x, y));
        }

        public void longTouch() {
            callJs(String.format("android.selection.longTouch();"));
        }

        public void endSelection() {
            callJs(String.format("android.selection.endSelection();"));
        }

        public void setStartPos(float x, float y, boolean finalPos) {
            callJs(String.format("android.selection.setStartPos(%f, %f, %b);", x, y, finalPos));
        }

        public void setEndPos(float x, float y, boolean finalPos) {
            callJs(String.format("android.selection.setEndPos(%f, %f, %b);", x, y, finalPos));
        }
    }

    ;

}
