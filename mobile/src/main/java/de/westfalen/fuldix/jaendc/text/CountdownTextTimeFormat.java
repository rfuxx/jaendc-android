package de.westfalen.fuldix.jaendc.text;

import java.text.DecimalFormat;

public class CountdownTextTimeFormat {
    private final DecimalFormat flex = new DecimalFormat("#0");
    private final DecimalFormat whole = new DecimalFormat("00");

    public StringBuffer format(final double formatValue, final StringBuffer buffer) {
        final double value = formatValue / 1000;
        if(value > 3600 * 24) {
            final int days = (int) (value/(3600*24));
            final int hours = (int) (value/3600%24);
            final int minutes = (int) (value/60%60);
            final int seconds = (int) (value%60);
            buffer.append(days).append("d ");
            buffer.append(flex.format(hours)).append(':');
            buffer.append(whole.format(minutes)).append(':');
            buffer.append(whole.format(seconds));
        } else if(value >= 3600) {
            final int hours = (int) (value/3600);
            final int minutes = (int) (value/60%60);
            final int seconds = (int) (value%60);
            buffer.append(flex.format(hours)).append(':');
            buffer.append(whole.format(minutes)).append(':');
            buffer.append(whole.format(seconds));
        } else if(value >= 60) {
            int minutes = (int) (value/60);
            int seconds = (int) (value%60);
            buffer.append(flex.format(minutes)).append(':');
            buffer.append(whole.format(seconds));
        } else {
            buffer.append("0:");
            buffer.append(whole.format((int) value));
        }
        return buffer;
    }

    public int delayToNext(final int remaining) {
        final int timeStep = 1000;
        final int fraction = remaining % timeStep;
        return fraction != 0 ? fraction : timeStep;
    }

    public StringBuffer format(final long value, final StringBuffer buffer) {
        return this.format((double) value, buffer);
    }

    public StringBuffer format(final long value) {
        return format(value, new StringBuffer());
    }
}
