package de.westfalen.fuldix.jaendc;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;

public class StyleHelper {
    public static int getStyledColor(final Resources.Theme theme, final int resource, final int defaultValue) {
        final TypedArray styled = theme.obtainStyledAttributes(new int[]{resource});
        try {
            return styled.getColor(0, defaultValue);
        } finally {
            styled.recycle();
        }
    }

    public static int getStyledColorSafe(final Resources.Theme theme, final int resource, final int defaultValue) {
        try {
            return getStyledColor(theme, resource, defaultValue);
        } catch(final Exception e) {
            e.printStackTrace();
        }
        return defaultValue;
    }

    public static int getStyledResourceId(final Resources.Theme theme, final int resource, final int defaultValue) {
        final TypedArray styled = theme.obtainStyledAttributes(new int[]{resource});
        try {
            return styled.getResourceId(0, defaultValue);
        } finally {
            styled.recycle();
        }
    }

    public static int getStyledResourceIdSafe(final Resources.Theme theme, final int resource, final int defaultValue) {
        try {
            return getStyledResourceId(theme, resource, defaultValue);
        } catch(final Exception e) {
            e.printStackTrace();
        }
        return defaultValue;
    }

    public static void setDialogIcon(final AlertDialog.Builder builder, final Context context, final int iconResource, final int tintResouce) {
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            builder.setIcon(iconResource);
        } else {
            final Drawable drawable = context.getResources().getDrawable(iconResource);
            if (drawable != null) {
                try {
                    final int colorToUse = StyleHelper.getStyledColor(context.getTheme(), tintResouce, 0x808080);
                    drawable.setColorFilter(colorToUse, PorterDuff.Mode.SRC_ATOP);
                    builder.setIcon(drawable);
                } catch(final Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
