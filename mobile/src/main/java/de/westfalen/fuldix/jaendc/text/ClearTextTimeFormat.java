package de.westfalen.fuldix.jaendc.text;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

import de.westfalen.fuldix.jaendc.model.Time;

public class ClearTextTimeFormat extends DecimalFormat {
    private static final char HOURS = 'h';
    private static final char MINUTES = 'm';
    private static final char SECONDS = 's';
    private final NumberFormat decimal = new DecimalFormat("#0.##");

    @Override
    public StringBuffer format(double value, StringBuffer buffer, FieldPosition field) {
        value = Time.roundTimeToCameraTime(value, 0.25);
        if(value > 3600) {
            int hours = (int) (value/3600);
            int minutes = (int) (value/60%60);
            int seconds = (int) (value%60);
            if(minutes > 0) {
                if (seconds > 0) {
                    return buffer.append(hours).append(HOURS).append(' ').append(minutes).append(MINUTES).append(' ').append(seconds).append(SECONDS);
                } else {
                    return buffer.append(hours).append(HOURS).append(' ').append(minutes).append(MINUTES);
                }
            } else {
                if(seconds > 0) {
                    return buffer.append(hours).append(HOURS).append(' ').append(seconds).append(SECONDS);
                } else {
                    return buffer.append(hours).append(HOURS);
                }
            }
        } else if(value > 60) {
            int minutes = (int) (value/60);
            double seconds = value%60;
            if(seconds > 0) {
                return buffer.append(minutes).append(MINUTES).append(' ').append(decimal.format(seconds)).append(SECONDS);
            } else {
                return buffer.append(minutes).append(MINUTES);
            }
        } else if(value > 0.25) {
            return buffer.append(decimal.format(value)).append(SECONDS);
        } else {
            return buffer.append("1/").append(Math.round(1 / value)).append(SECONDS);
        }
    }

    @Override
    public StringBuffer format(long value, StringBuffer buffer, FieldPosition field) {
        return this.format((double) value, buffer, field);
    }

    @Override
    public Number parse(String string, ParsePosition position) {
        return null;
    }
}
