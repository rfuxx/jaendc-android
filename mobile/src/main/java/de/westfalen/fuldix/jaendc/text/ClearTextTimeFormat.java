package de.westfalen.fuldix.jaendc.text;

import android.content.Context;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;

import de.westfalen.fuldix.jaendc.R;
import de.westfalen.fuldix.jaendc.model.Time;

public class ClearTextTimeFormat extends DecimalFormat {
    private final DecimalFormat decimal = new DecimalFormat("#0.##");

    private final int timeStyle;
    private final String daysSymbol;
    private final String hoursSymbol;
    private final String minutesSymbol;
    private final String secondsSymbol;

    public ClearTextTimeFormat(final Context context, final int timeStyle) {
        this.timeStyle = timeStyle;
        this.daysSymbol = context.getResources().getString(R.string.text_days);
        this.hoursSymbol = context.getResources().getString(R.string.text_hours);
        this.minutesSymbol = context.getResources().getString(R.string.text_minutes);
        this.secondsSymbol = context.getResources().getString(R.string.text_seconds);
    }

    @Override
    public StringBuffer format(final double formatValue, final StringBuffer buffer, final FieldPosition field) {
        final double value = Time.roundTimeToCameraTime(formatValue, 0.25, timeStyle);
        if(value > 3600 * 24) {
            final int days = (int) (value / (3600*24));
            final int hours = (int) (value / 3600 % 24);
            final int minutes = (int) (value / 60 % 60);
            final int seconds = (int) (value % 60);
            buffer.append(days).append(daysSymbol);
            if (hours > 0 && minutes > 0 && seconds > 0) {
                buffer.append(' ').append(hours).append(hoursSymbol);
            }
            if (minutes > 0 && seconds > 0) {
                buffer.append(' ').append(minutes).append(minutesSymbol);
            }
            if (seconds > 0) {
                buffer.append(' ').append(seconds).append(secondsSymbol);
            }
        } else if(value > 3600) {
            final int hours = (int) (value/3600);
            final int minutes = (int) (value/60%60);
            final int seconds = (int) (value%60);
            buffer.append(hours).append(hoursSymbol);
            if(minutes > 0 || seconds > 0) {
                buffer.append(' ').append(minutes).append(minutesSymbol);
                if (seconds > 0) {
                    buffer.append(' ').append(seconds).append(secondsSymbol);
                }
            }
        } else if(value > 60) {
            int minutes = (int) (value/60);
            double seconds = value%60;
            buffer.append(minutes).append(minutesSymbol);
            if(seconds > 0) {
                buffer.append(' ').append(decimal.format(seconds)).append(secondsSymbol);
            }
        } else if(value > 0.25) {
            buffer.append(decimal.format(value)).append(secondsSymbol);
            if(value < 1 && Time.isTimeStyleWithDecimalPlacesFractions(timeStyle)) {
                if (Math.round(value * 100) / 100.0d == value) {
                    // extra pedantic:
                    // equals sign only when the rounded displayed value (to two decimal places)
                    // is really equal
                    buffer.append(" = ");
                } else {
                    // if not exacty equal, use the "almost equal sign".
                    buffer.append(" \u2248 ");
                }
                // Panasonic Format: reciprocal value (3.2/2.5/2/1.6/1.3)
                final int timesTen = (int) Math.round(1/value * 10);
                final int main = timesTen / 10;
                final int decimalPlace = timesTen % 10;
                buffer.append("1/").append(main);
                if(decimalPlace > 0) {
                    buffer.append(decimal.getDecimalFormatSymbols().getDecimalSeparator()).append(decimalPlace);
                    // here (only here!) format the decimal places in the fraction "correct" according to the locale
                    // all other occurrances just use decimal point
                    // --  to match the cameras' displays, which appear to ignore it, too
                }
                buffer.append(secondsSymbol);
            }
        } else {
            buffer.append("1/").append(Math.round(1 / value)).append(secondsSymbol);
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
