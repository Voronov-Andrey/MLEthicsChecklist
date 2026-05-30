package com.example.mlethics.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class UiUtils {
    private UiUtils() {
    }

    public static int dp(Context context, int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    public static TextView title(Context context, String text, int sp) {
        TextView view = text(context, text, sp, Color.rgb(31, 41, 51));
        view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    public static TextView text(Context context, String text, int sp, int color) {
        TextView view = new TextView(context);
        view.setText(text == null ? "" : text);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setLineSpacing(0, 1.08f);
        return view;
    }

    public static LinearLayout vertical(Context context, int paddingDp) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        int p = dp(context, paddingDp);
        layout.setPadding(p, p, p, p);
        return layout;
    }

    public static LinearLayout horizontal(Context context, int paddingDp) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.HORIZONTAL);
        int p = dp(context, paddingDp);
        layout.setPadding(p, p, p, p);
        return layout;
    }

    public static void margin(View view, int left, int top, int right, int bottom) {
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        lp.setMargins(left, top, right, bottom);
        view.setLayoutParams(lp);
    }

    public static String safe(String value) {
        return value == null || value.trim().length() == 0 ? "-" : value;
    }
}
