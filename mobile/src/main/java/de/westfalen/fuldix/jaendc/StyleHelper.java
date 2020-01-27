package de.westfalen.fuldix.jaendc;

import android.content.res.Resources;
import android.content.res.TypedArray;

public class StyleHelper {
    public static int getStyledColor(final Resources.Theme theme, final int resource, final int defaultValue) {
        try {
        final TypedArray styled = theme.obtainStyledAttributes(new int[]{resource});
            try {
                return styled.getColor(0, defaultValue);
            } finally {
                styled.recycle();
            }
        } catch(final Exception e) {
                e.printStackTrace();
        }
        return defaultValue;
    }

    public static int getStyledResourceId(final Resources.Theme theme, final int resource, final int defaultValue) {
        try {
            final TypedArray styled = theme.obtainStyledAttributes(new int[]{resource});
            try {
                return styled.getResourceId(0, defaultValue);
            } finally {
                styled.recycle();
            }
        } catch(final Exception e) {
            e.printStackTrace();
        }
        return defaultValue;
    }
}
