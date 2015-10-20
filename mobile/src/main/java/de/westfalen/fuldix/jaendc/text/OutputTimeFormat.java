package de.westfalen.fuldix.jaendc.text;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

public class OutputTimeFormat extends NumberFormat{
    private static final char DOUBLE_PRIME = '\u2033';
    public StringBuffer format(double value, StringBuffer buffer, FieldPosition field) {
        if(value > 0.25) {
            int seconds = (int) value;
            int decimal = (int) ((value - seconds) * 10);
            if(decimal > 0) {
                return buffer.append(seconds).append(DOUBLE_PRIME).append(decimal);
            } else {
                return buffer.append(seconds).append(DOUBLE_PRIME);
            }
        } else {
            return buffer.append((int) (1 / value));
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
