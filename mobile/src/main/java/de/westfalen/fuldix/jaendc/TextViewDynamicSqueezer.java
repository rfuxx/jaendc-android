package de.westfalen.fuldix.jaendc;

import android.annotation.TargetApi;
import android.app.Activity;
import android.text.Layout;
import android.text.TextPaint;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

public class TextViewDynamicSqueezer {
    private static class AdjustTextScalexListener implements ViewTreeObserver.OnGlobalLayoutListener {
        private TextView textView;
        private CharSequence content;
        private float adjustedForWidth;
        private float adjustedForHeight;
        private float origTextScaleX;

        @TargetApi(11)
        @Override
        public void onGlobalLayout () {
            final Layout l = textView.getLayout();
            if (l != null) {
                final float allowedWidth = textView.getWidth() - textView.getTotalPaddingLeft() - textView.getTotalPaddingRight();
                final float allowedHeight = textView.getHeight() - textView.getTotalPaddingTop() - textView.getTotalPaddingBottom();
                final CharSequence currentContent = textView.getText();
                if (adjustedForHeight != allowedHeight || adjustedForWidth != allowedWidth || !currentContent.equals(content)) {
                    final TextPaint textPaint = textView.getPaint();
                    textPaint.setTextScaleX(origTextScaleX);
                    final float desiredWidth = Layout.getDesiredWidth(currentContent, textPaint);
// System.out.println("#####"+currentContent + "|  " + allowedWidth+"/"+desiredWidth + " and height " + allowedHeight+"/");
                    final float scaleX = allowedWidth < desiredWidth ? allowedWidth / desiredWidth : origTextScaleX;
                    if(scaleX != textView.getTextScaleX()) {
                        textView.setTextScaleX(scaleX);
                    }
                    content = currentContent;
                    adjustedForWidth = allowedWidth;
                    adjustedForHeight = allowedHeight;
                }
            }
        }

        public void setTextView(final TextView textView) {
            this.textView = textView;
            final CharSequence text = textView.getText();
            if(text != null && text.length() > 0 && text.charAt(text.length()-1) != '\u00a0') {
                final CharSequence textWithEnd = text + "\u00a0";   // append protected space so that there is enough "visual buffer"
                textView.setText(textWithEnd);
            }
            origTextScaleX = textView.getTextScaleX();
            textView.setEllipsize(null);
        }

        public void restoreTextView() {
            textView.setTextScaleX(origTextScaleX);
        }
    }

    private final Activity activity;
    private final AdjustTextScalexListener adjustTextScalexListener = new AdjustTextScalexListener();
    private TextView textView;

    private static TextView getTextViewWithText(final View rootView, final CharSequence searchText) {
        if(rootView instanceof TextView) {
            final TextView textView = (TextView) rootView;
            final CharSequence textViewText = textView.getText();
            if(textViewText != null && textViewText.equals(searchText)) {
                return textView;
            }
        }
        if(rootView instanceof ViewGroup) {
            final ViewGroup viewGroup = (ViewGroup) rootView;
            for(int i=0; i<viewGroup.getChildCount(); i++) {
                final TextView returnedView = getTextViewWithText(viewGroup.getChildAt(i), searchText);
                if(returnedView != null) {
                    return returnedView;
                }
            }
        }
        return null;
    }

    public TextViewDynamicSqueezer(final Activity activity) {
        this.activity = activity;
    }

    public void onViewCreate(final TextView textView) {
        this.textView = textView;
        if(textView != null) {
            adjustTextScalexListener.setTextView(textView);
            textView.getViewTreeObserver().addOnGlobalLayoutListener(adjustTextScalexListener);
        }
    }

    public void onViewCreate(final CharSequence textContent, final int idInView) {
        if (android.os.Build.VERSION.SDK_INT >= 11) {
            final View viewWithId = activity.findViewById(idInView);
            if(viewWithId != null) {
                onViewCreate(getTextViewWithText(viewWithId.getRootView(), textContent));
            }
        }
    }

    public void onViewDestroy() {
        if (android.os.Build.VERSION.SDK_INT >= 16) {
            if(textView != null) {
                textView.getViewTreeObserver().removeOnGlobalLayoutListener(adjustTextScalexListener);
                adjustTextScalexListener.restoreTextView();
            }
        }
    }
}
