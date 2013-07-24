package com.umranium.ebookextra;

import com.umranium.ebook.R;
import com.umranium.ebook.R.id;
import com.umranium.ebook.R.layout;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class WaitDlgHelper {

    public static AlertDialog showWaitDialog(Activity activity, int msgId) {
        LayoutInflater inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View waitScreen = inflater.inflate(R.layout.wait_screen, null, false);
        ((TextView) waitScreen.findViewById(R.id.wait_screen_wait_message)).setText(msgId);
        builder.setView(waitScreen);
        builder.setCancelable(false);
        AlertDialog waitDialog = builder.create();
        waitDialog.show();

        return waitDialog;
    }

}
