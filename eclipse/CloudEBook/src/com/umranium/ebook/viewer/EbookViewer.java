package com.umranium.ebook.viewer;

import java.util.Collections;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;
import com.umranium.ebook.R;
import com.umranium.ebook.services.IWebHostService;
import com.umranium.ebook.viewer.JsInterfacing.BtnMsg;
import com.umranium.ebookextra.FlurryHelper;

public class EbookViewer extends SelectableWebView {

    private static final String TAG = "EbookViewer";

    public interface EbookViewerEventsHandler {
        void goToBookSection(String href);

        void openExternally(String href);

        void onDocumentReady();

        void onDocumentFatalErrors();

        void onSelectionStarted();

        void onSelectionEnded();
    }

    private class BookmarkingModeState {
        private ActionMode actionMode;

        public BookmarkingModeState() {
        }

        public void enter() {
            EbookViewer viewer = EbookViewer.this;

            actionMode = viewer.startActionMode(new ActionMode.Callback() {
                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return true;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    leave();
                }

                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    mode.setTitle("Add Bookmark");
                    mode.setSubtitle("Touch any text to create a bookmark.");
                    return true;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    return true;
                }
            });
        }

        public void leave() {
            EbookViewer viewer = EbookViewer.this;

			/*
             * Might be called multiple times. The ActionMode.finish() generates an onDestroyActionMode call
			 * on the ActionMode.Callback, which intern calls leave() again. This is because onDestroyActionMode
			 * might be called as a result of the user clicking the "Done" button on the action-mode bar also.
			 * 
			 * Hence, make sure to assign null to the actionMode field first before calling finish. 
			 */
            if (actionMode != null) {
                ActionMode tmpActionMode = actionMode;
                actionMode = null;
                tmpActionMode.finish();
            }

            viewer.bookmarkingModeState = null;
        }

        boolean onTouch(View v, MotionEvent event) {
            EbookViewer viewer = EbookViewer.this;

            if (event.getAction() == MotionEvent.ACTION_UP) {
                float xPoint = screenDensityHelper.getDensityIndependentValue(event.getX())
                        / screenDensityHelper.getDensityIndependentValue(viewer.getScale());
                float yPoint = screenDensityHelper.getDensityIndependentValue(event.getY())
                        / screenDensityHelper.getDensityIndependentValue(viewer.getScale());

                JSONObject obj = new JSONObject();
                try {
                    obj.put("cX", xPoint);
                    obj.put("cY", yPoint);
                    sendSelectionClickResult("addBookmark", obj.toString());
                    leave();
                } catch (JSONException e) {
                    Log.e(TAG, "Error creating JSON point");
                }
            }

            return true;
        }
    }

    private IWebHostService webHostService;
    private String bookId;
    private EbookViewerEventsHandler ebookEventsHandler;
    private BookmarkingModeState bookmarkingModeState;
    private OnClickListener onClickListener;

    public EbookViewer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setup(context);
    }

    public EbookViewer(Context context, AttributeSet attrs) {
        super(context, attrs);
        setup(context);
    }

    public EbookViewer(Context context) {
        super(context);
        setup(context);
    }

    private void setup(Context context) {
        this.addJavascriptInterface(dartOutgoingInterface, "androidInterface");
        //androidAnnotationInterface

        WebChromeClient chromeClient = new WebChromeClient() {
            @Override
            public void onConsoleMessage(String message, int lineNumber,
                                         String sourceID) {
                Log.d(TAG, "Console: " + message);
//				FlurryAgent.logEvent(FlurryHelper.EVENT_WEBVIEW_CONSOLE_MSG,
//						Collections.singletonMap("Message", message));
            }
        };
        this.setWebChromeClient(chromeClient);

        WebViewClient viewClient = new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode,
                                        String description, String failingUrl) {
                Log.e(TAG, "WebView error while loading " + failingUrl + " (code:" + errorCode + "): " + description);
                FlurryAgent.onError("WebView:ErrorCode" + errorCode, description, failingUrl);
                super.onReceivedError(view, errorCode, description, failingUrl);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d(TAG, "WebView shouldOverrideUrlLoading: " + url);

                if (webHostService != null) {
                    String hostUrl = webHostService.getHostUrl();
                    if (url.startsWith(hostUrl)) {
                        if (ebookEventsHandler != null) {
                            String goToHref = url.substring(hostUrl.length());
                            Log.d(TAG, "WebView go to document:" + goToHref);
                            ebookEventsHandler.goToBookSection(goToHref);
                        }
                    } else {
                        if (ebookEventsHandler != null) {
                            Log.d(TAG, "WebView go to browser:" + url);
                            ebookEventsHandler.openExternally(url);
                        }
                    }
                }

                return true;
            }

            @Override
            public void onLoadResource(WebView view, String url) {
                Log.d(TAG, "WebView loading: " + url);
                FlurryAgent.logEvent(FlurryHelper.EVENT_WEBVIEW_LOAD_RESOURCE,
                        Collections.singletonMap("URL", url));
            }
        };

        this.setWebViewClient(viewClient);
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        this.onClickListener = l;
        super.setOnClickListener(l);
    }

    public OnClickListener getOnClickListener() {
        return onClickListener;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (bookmarkingModeState != null) {
            return bookmarkingModeState.onTouch(v, event);
        } else {
            return super.onTouch(v, event);
        }
    }

    public IWebHostService getWebHostService() {
        return webHostService;
    }

    public void setWebHostService(IWebHostService webHostService) {
        this.webHostService = webHostService;
    }

    public String getBookId() {
        return bookId;
    }

    public void setBookId(String bookId) {
        this.bookId = bookId;
    }

    public EbookViewerEventsHandler getEbookEventsHandler() {
        return ebookEventsHandler;
    }

    public void setEbookEventsHandler(EbookViewerEventsHandler ebookEventsHandler) {
        this.ebookEventsHandler = ebookEventsHandler;
    }

    @Override
    public void startSelectionMode() {
        super.startSelectionMode();
        if (ebookEventsHandler != null) {
            ebookEventsHandler.onSelectionStarted();
        }
    }

    @Override
    public void endSelectionMode() {
        super.endSelectionMode();
        if (ebookEventsHandler != null) {
            ebookEventsHandler.onSelectionEnded();
        }
    }

    public void startBookmarkingMode() {
        if (bookmarkingModeState != null) {
            return;
        }

        bookmarkingModeState = new BookmarkingModeState();
        bookmarkingModeState.enter();
    }

    public void endBookmarkingMode() {
        bookmarkingModeState.leave();
        bookmarkingModeState = null;
    }

    public void sendSelectionClickResult(String btnId, String extra) {
        if (extra != null) {
            extra = JSONObject.quote(extra);
        }
        String command = String.format("selectionClickResult('%s',%s);",
                btnId, extra);
        EbookViewer.this.callJs(command);
    }

    private DartOutgoingInterface dartOutgoingInterface = new DartOutgoingInterface() {

        ActionMode actionMode = null;
        HighlightActionModeCallback actionModeCallback = null;

        @Override
        public void init() {
        }

//		@Override
//		public void onLoadingError(String error) {
//			Log.e(TAG, "Content Loading Error: "+error);
//			if (ebookEventsHandler!=null) {
//				ebookEventsHandler.onDocumentLoadErrors();
//			}
//		}

        @Override
        public void documentReady() {
            if (ebookEventsHandler != null) {
                ebookEventsHandler.onDocumentReady();
            }
        }

        @Override
        public void onSelectionStarted(double clientX, double clientY,
                                       double pageX, double pageY, String jsonButtons) {
            updateActionMode(jsonButtons);
        }

        @Override
        public void onSelectionUpdated(double clientX, double clientY,
                                       double pageX, double pageY, String jsonButtons) {
            updateActionMode(jsonButtons);
        }


        private void updateActionMode(final String jsonButtons) {
            Log.i(TAG, "Updating action mode:" + jsonButtons);

            EbookViewer.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    BtnMsg[] btns;
                    try {
                        btns = JsInterfacing.btnsFromJson(jsonButtons);
                    } catch (Exception e) {
                        Log.d(TAG, "Error while parsing JSON buttons msg: " + jsonButtons, e);
                        return;
                    }

                    Log.d(TAG, "Buttons Received:");
                    for (BtnMsg btn : btns) {
                        Log.d(TAG, "\t" + btn.toString());
                    }

                    if (actionMode != null) {
                        actionModeCallback.setBtns(btns);
                        actionMode.invalidate();
                    } else {
                        actionModeCallback = new HighlightActionModeCallback(btns);
                        actionMode = EbookViewer.this.startActionMode(actionModeCallback);
                    }
                }

                ;
            }, false);
        }

        @Override
        public void onSelectionFinished() {
            EbookViewer.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (actionMode != null) {
                        actionMode.finish();
                        actionMode = null;
                        actionModeCallback = null;
                    }
                }

                ;
            }, false);
        }

        @Override
        public void onNoteEdit(String title, String text, String jsonButtons,
                               String noteId) {
            Log.d(TAG, "EbookViwer received onNoteEdit");

            BtnMsg[] btns;
            try {
                btns = JsInterfacing.btnsFromJson(jsonButtons);
            } catch (Exception e) {
                Log.d(TAG, "Error while parsing JSON buttons msg: " + jsonButtons, e);
                return;
            }

            Log.d(TAG, "Buttons Received:");
            for (BtnMsg btn : btns) {
                Log.d(TAG, "\t" + btn.toString());
            }

            Activity activity = (Activity) EbookViewer.this.getContext();

            NoteEditDialogCallback callback = new NoteEditDialogCallback() {
                public void onResult(String finalText, BtnMsg btn) {
                    JSONObject resultJson = new JSONObject();
                    try {
                        resultJson.put("extra", btn.extra != null ? btn.extra : JSONObject.NULL);
                        resultJson.put("text", finalText);
                    } catch (JSONException e) {
                        Log.e(TAG, "Error while contructing JSON note-editor result message", e);
                        return;
                    }

                    sendSelectionClickResult(btn.id, resultJson.toString());
                }

                ;
            };
            NoteEditDialog dialog = new NoteEditDialog();
            dialog.setCancelable(false);
            dialog.setTitle(title);
            dialog.setPrevText(text);
            dialog.setBtns(btns);
            dialog.setDialogResultCallback(callback);
            dialog.show(activity.getFragmentManager(), "noteEdit");
        }

        public void onShowBookmarkingOptions(double clientX, double clientY, double pageX, double pageY, String jsonButtons) {
            Log.d(TAG, "EbookViwer received onShowBookmarkingOptions");

            openOptions("bookmarkingOptions", jsonButtons);
        }

        ;

        @Override
        public void onMetaNodeClicked(double clientX, double clientY, double pageX,
                                      double pageY, String jsonButtons) {
            Log.d(TAG, "EbookViwer received onMetaNodeClicked");

            openOptions("metaNodeClicked", jsonButtons);
        }

        private void openOptions(String tag, String jsonButtons) {
            BtnMsg[] btns;
            try {
                btns = JsInterfacing.btnsFromJson(jsonButtons);
            } catch (Exception e) {
                Log.d(TAG, "Error while parsing JSON buttons msg: " + jsonButtons, e);
                return;
            }

            Log.d(TAG, "Buttons Received:");
            for (BtnMsg btn : btns) {
                Log.d(TAG, "\t" + btn.toString());
            }

            Activity activity = (Activity) EbookViewer.this.getContext();

            OnHiglightOptionDialogResultCallback callback = new OnHiglightOptionDialogResultCallback() {
                @Override
                public void onResult(BtnMsg btn) {
                    sendSelectionClickResult(btn.id, btn.extra);
                }
            };
            HighlightClickOptionDialog dialog = new HighlightClickOptionDialog();
            dialog.setBtns(btns);
            dialog.setDialogResultCallback(callback);
            dialog.show(activity.getFragmentManager(), tag);
        }

        public void onTextCopy(final String text) {
            final Activity activity = (Activity) EbookViewer.this.getContext();
            Runnable r = new Runnable() {
                @Override
                public void run() {
                    Context context = EbookViewer.this.getContext();
                    ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("text", text);
                    clipboard.setPrimaryClip(clip);

                    Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show();
                }
            };
            activity.runOnUiThread(r);
        }

        @Override
        public void scrollToLocation(final double pageY) {
            Log.d(TAG, "EbookViwer received scrollToLocation(" + pageY + ")");

            runOnUiThread(new Runnable() {
                public void run() {
                    EbookViewer viewer = EbookViewer.this;
                    float yPoint = screenDensityHelper.getDensityDependentValue(
                            (float) pageY * screenDensityHelper.getDensityIndependentValue(viewer.getScale())
                    );
                    viewer.scrollTo(0, Math.round(yPoint));
                }

                ;
            }, false);
        }

        public void onErrorMsg(String msg, String loc, boolean quit) {
            Log.e(TAG, msg + "\nAt: " + loc);
            if (quit) {
                if (ebookEventsHandler != null) {
                    ebookEventsHandler.onDocumentFatalErrors();
                }
            }
        }

        ;
    };

    private class HighlightActionModeCallback implements ActionMode.Callback {

        private BtnMsg[] btns;

        public HighlightActionModeCallback(BtnMsg[] btns) {
            this.btns = btns;
        }

        @SuppressWarnings("unused")
        public BtnMsg[] getBtns() {
            return btns;
        }

        public void setBtns(BtnMsg[] btns) {
            this.btns = btns;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            Log.d(TAG, "onActionItemClicked: item=" + item.getTitle());
            return false;
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            updateMenuBtns(menu);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            Log.d(TAG, "onDestroyActionMode");
            EbookViewer.this.endSelectionMode();
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            updateMenuBtns(menu);
            return true;
        }

        private void updateMenuBtns(Menu menu) {
            menu.clear();
            for (int i = 0; i < btns.length; ++i) {
                final BtnMsg btn = btns[i];
                MenuItem menuItem = menu.add(btn.name);
                menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        sendSelectionClickResult(btn.id, btn.extra);
                        EbookViewer.this.endSelectionMode();
                        return true;
                    }
                });
            }
        }
    }

    public interface OnHiglightOptionDialogResultCallback {
        void onResult(BtnMsg btn);
    }

    public static class HighlightClickOptionDialog extends DialogFragment {
        private BtnMsg[] btns;
        private OnHiglightOptionDialogResultCallback dialogResultCallback;

        public void setBtns(BtnMsg[] btns) {
            this.btns = btns;
        }

        public void setDialogResultCallback(
                OnHiglightOptionDialogResultCallback dialogResultCallback) {
            this.dialogResultCallback = dialogResultCallback;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Context ctx = this.getActivity();

            String[] options = new String[btns.length];
            for (int i = 0; i < btns.length; ++i) {
                options[i] = btns[i].name;
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
            builder.setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialogResultCallback.onResult(btns[which]);
                }
            });
            builder.setCancelable(true);
            return builder.create();
        }
    }

    public interface NoteEditDialogCallback {
        void onResult(String finalText, BtnMsg btn);
    }

    public static class NoteEditDialog extends DialogFragment {
        private String title;
        private String prevText;
        private BtnMsg[] btns;
        private NoteEditDialogCallback dialogResultCallback;

        public void setTitle(String title) {
            this.title = title;
        }

        public void setPrevText(String prevText) {
            this.prevText = prevText;
        }

        public void setBtns(BtnMsg[] btns) {
            this.btns = btns;
        }

        public void setDialogResultCallback(
                NoteEditDialogCallback dialogResultCallback) {
            this.dialogResultCallback = dialogResultCallback;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Activity ctx = this.getActivity();

            LayoutInflater inflater = ctx.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.note_editor_dialog, null);

            final EditText editText = (EditText) dialogView.findViewById(R.id.note_editor_dialog_text_edit);
            editText.setText(prevText);

            LinearLayout btnPanel = (LinearLayout) dialogView.findViewById(R.id.note_editor_dialog_btn_panel);
            for (final BtnMsg btn : btns) {
                Button button = new Button(ctx);
                button.setText(btn.name);
                btnPanel.addView(button);

                button.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String text = editText.getText().toString();
                        NoteEditDialog.this.dismiss();
                        dialogResultCallback.onResult(text, btn);
                    }
                });
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
            builder.setTitle(title);
            builder.setView(dialogView);
            builder.setCancelable(false);
            Dialog dialog = builder.create();

            return dialog;
        }
    }


}
