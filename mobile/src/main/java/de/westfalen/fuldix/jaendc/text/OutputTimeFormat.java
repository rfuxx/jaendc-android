package de.westfalen.fuldix.jaendc.text;

import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

import de.westfalen.fuldix.jaendc.model.Time;

public class OutputTimeFormat extends NumberFormat{
    private static final char DOUBLE_PRIME = '\u2033';
    public StringBuffer format(double value, StringBuffer buffer, FieldPosition field) {
        value = Time.roundTimeToCameraTime(value, 3);
        if(value > 0.25) {
            if(value < 1) {
                // Panasonic Format: reciprocal value (3.2/2.5/2/1.6/1.3)
                int timesTen = (int) Math.round(1/value * 10);
                int main = timesTen / 10;
                int decimal = timesTen % 10;
                if(decimal > 0) {
                    buffer.append(main).append('.').append(decimal);
                } else {
                    buffer.append(main);
                }
                buffer.append(" = ");
            }
            // Canon Format: decimal places (0"3/0"4/0"5/0"6/0"8)
            int timesTen = (int) Math.round(value * 10);
            int seconds = timesTen / 10;
            int decimal = timesTen % 10;
            if(decimal > 0) {
                buffer.append(seconds).append(DOUBLE_PRIME).append(decimal);
            } else {
                buffer.append(seconds).append(DOUBLE_PRIME);
            }
            if(value < 1) {
                buffer.append(')');
            }
            return buffer;
        } else {
            return buffer.append(Math.round(1 / value));
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
