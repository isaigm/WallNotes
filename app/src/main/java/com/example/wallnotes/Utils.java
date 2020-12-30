package com.example.wallnotes;

import android.content.Context;
import android.widget.Toast;

public class Utils {
    public static void showMessage(Context context, String msg)
    {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }
}
