package de.westfalen.fuldix.jaendc.text;

import android.content.Context;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

import de.westfalen.fuldix.jaendc.R;
import de.westfalen.fuldix.jaendc.model.Time;

public class OutputTimeFormat extends NumberFormat{
    private static final char DOUBLE_PRIME = '\u2033';
    private static final char SINGLE_PRIME = '\u2032';

    private final int timeStyle;
    private final String daysSymbol;
    private final String hoursSymbol;

    public OutputTimeFormat(final Context context, final int timeStyle) {
        this.timeStyle = timeStyle;
        this.daysSymbol = context.getResources().getString(R.string.text_days);
        this.hoursSymbol = context.getResources().getString(R.string.text_hours);
    }

    public StringBuffer format(final double formatValue, final StringBuffer buffer, final FieldPosition field) {
        final double maxCameraTime = Time.times[timeStyle][Time.times[timeStyle].length-1];
        final double value = Time.roundTimeToCameraTime(formatValue, maxCameraTime, timeStyle);
        if(value > maxCameraTime) {
            final long roundedValue = Math.round(value);
            if(value > 3600 * 24) {
                final int days = (int) (roundedValue/(3600*24));
                final int hours = (int) (roundedValue/3600%24);
                final int minutes = (int) (roundedValue/60%60);
                final int seconds = (int) (roundedValue%60);
                buffer.append(days).append(daysSymbol);
                if(hours > 0 && minutes > 0 && seconds > 0) {
                    buffer.append(hours).append(hoursSymbol);
                }
                if (minutes > 0 && seconds > 0) {
                    buffer.append(minutes).append(SINGLE_PRIME);
                }
                if (seconds > 0) {
                    buffer.append(seconds).append(DOUBLE_PRIME);
                }
            } else if(value > 3600) {
                final int hours = (int) (roundedValue/3600);
                final int minutes = (int) (roundedValue/60%60);
                final int seconds = (int) (roundedValue%60);
                buffer.append(hours).append(hoursSymbol);
                if (minutes > 0 && seconds > 0) {
                    buffer.append(minutes).append(SINGLE_PRIME);
                }
                if (seconds > 0) {
                    buffer.append(seconds).append(DOUBLE_PRIME);
                }
            } else if(value > 60) {
                final long minutes = roundedValue / 60;
                final int seconds = (int) (roundedValue % 60);
                buffer.append(minutes).append(SINGLE_PRIME);
                if (seconds > 0) {
                    buffer.append(seconds).append(DOUBLE_PRIME);
                }
            } else {
                buffer.append(roundedValue).append(DOUBLE_PRIME);
            }
        } else if(value > 0.25) {
            if(value < 1 && Time.isTimeStyleWithDecimalPlacesFractions(timeStyle)) {
                // Panasonic Format: reciprocal value (3.2/2.5/2/1.6/1.3)
                final int timesTen = (int) Math.round(1/value * 10);
                final int main = timesTen / 10;
                final int decimal = timesTen % 10;
                buffer.append(main);
                if(decimal > 0) {
                    buffer.append('.').append(decimal);
                }
            } else {
                // Canon Format: decimal places (0"3/0"4/0"5/0"6/0"8)
                final int timesTen = (int) Math.round(value * 10);
                final int seconds = timesTen / 10;
                final int decimal = timesTen % 10;
                buffer.append(seconds).append(DOUBLE_PRIME);
                if (decimal > 0) {
                    buffer.append(decimal);
                }
            }
        } else {
            buffer.append(Math.round(1 / value));
        }
        return buffer;
    }

    @Override
    public StringBuffer format(final long value, final StringBuffer buffer, final FieldPosition field) {
        return this.format((double) value, buffer, field);
    }

    @Override
    public Number parse(final String string, final ParsePosition position) {
        return null;
    }
}
