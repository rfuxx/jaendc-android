package de.westfalen.fuldix.jaendc;

import android.app.Activity;
import android.os.Build;
import android.text.Layout;
import android.text.TextPaint;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import android.widget.Toolbar;

public class TextViewDynamicSqueezer {
    private static class AdjustTextScalexListener implements ViewTreeObserver.OnGlobalLayoutListener {
        private static final int MAX_ITERATIONS = 10;
        private TextView textView;
        private CharSequence content;
        private float adjustedForWidth;
        private float adjustedForHeight;
        private float adjustedTextScaleX = 1;
        private float origTextScaleX;

        private static boolean isViewInToolbar(final View checkView) {
            if (Build.VERSION.SDK_INT >= 21) {
                if (checkView instanceof Toolbar) {
                    return true;
                } else if (checkView != null) {
                    final ViewParent parent = checkView.getParent();
                    return parent instanceof View && isViewInToolbar((View) parent);
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        @Override
        public void onGlobalLayout () {
            final Layout l = textView.getLayout();
            if (l != null) {
                final ViewParent parent = textView.getParent();
                final float allowedWidth;
                final float allowedHeight;
                if(parent instanceof View && !isViewInToolbar((View) parent)) {
                    final View parentView = (View) parent;
                    allowedWidth = parentView.getWidth() - parentView.getPaddingLeft() - parentView.getPaddingRight() - textView.getTotalPaddingLeft() - textView.getTotalPaddingRight();
                    allowedHeight = parentView.getHeight() - parentView.getPaddingTop() - parentView.getPaddingBottom() - textView.getTotalPaddingTop() - textView.getTotalPaddingBottom();
                } else {
                    allowedWidth = textView.getWidth() - textView.getTotalPaddingLeft() - textView.getTotalPaddingRight();
                    allowedHeight = textView.getHeight() - textView.getTotalPaddingTop() - textView.getTotalPaddingBottom();
                }
                final CharSequence currentContent = textView.getText();
                //System.out.println("check for :"+currentContent + "| currentTextScaleX: " + textView.getTextScaleX());
                if (allowedWidth > 0 && (adjustedForHeight != allowedHeight || adjustedForWidth != allowedWidth || !currentContent.equals(content))) {
                    final TextPaint textPaint = textView.getPaint();
                    float scaleX = origTextScaleX;
                    for(int i=0; i< MAX_ITERATIONS; i++) {
                        textPaint.setTextScaleX(scaleX);
                        final float desiredWidth = Layout.getDesiredWidth(currentContent, textPaint);
                        if(allowedWidth < desiredWidth) {
                            final float previousScaleX = scaleX;
                            scaleX = allowedWidth / desiredWidth * scaleX;
                            if(scaleX > 1 / desiredWidth && desiredWidth * (previousScaleX- scaleX) < 1) {  // if scale change was less than a pixel
                                //System.out.println("adjust " + scaleX + " because " + desiredWidth * (previousScaleX- scaleX));
                                scaleX -= (1 / desiredWidth);   // ensure the change is one pixel
                            }
                            //System.out.println("content:"+currentContent + "| scale:" + scaleX + ", width " + allowedWidth+"/"+desiredWidth + " and height " + allowedHeight+"/");
                        } else {
                            //System.out.println("finally content:"+currentContent + "| scale:" + scaleX + ", width " + allowedWidth+"/"+desiredWidth + " and height " + allowedHeight+"/");
                            break;
                        }
                    }
                    content = currentContent;
                    adjustedForWidth = allowedWidth;
                    adjustedForHeight = allowedHeight;
                    adjustedTextScaleX = scaleX;
                }
                if(adjustedTextScaleX != textView.getTextScaleX()) {
                    textView.setTextScaleX(adjustedTextScaleX);
                }
            }
        }

        public void setTextView(final TextView textView) {
            this.textView = textView;
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
