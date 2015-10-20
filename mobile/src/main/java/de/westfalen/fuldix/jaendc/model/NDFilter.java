package de.westfalen.fuldix.jaendc.model;

import android.content.Context;

import de.westfalen.fuldix.jaendc.R;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;

public class NDFilter {
    private static final double log2 = Math.log(2);
    public final NumberFormat decimalFormat = new DecimalFormat("#,###,###,###,##0.#");
    public final DecimalFormat textEnterFormat = new DecimalFormat("############0.#");
    private long id;
    private String name;
    private int factor;
    private int orderpos;

    public NDFilter() {
        textEnterFormatQuirk();
        this.orderpos = -1;
        this.id = Long.MIN_VALUE;
    }

    private NDFilter(long id, String name, int factor) {
        textEnterFormatQuirk();
        this.id = id;
        this.name = name;
        this.factor = factor;
        this.orderpos = -1;
    }

    public NDFilter(long id, String name, int factor, int orderpos) {
        textEnterFormatQuirk();
        this.id = id;
        this.name = name;
        this.factor = factor;
        this.orderpos = orderpos;
    }

    private void textEnterFormatQuirk() {
        // quirk for Android numberDecimal screenkeyboards
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setDecimalSeparator('.');
        symbols.setGroupingSeparator(',');
        textEnterFormat.setDecimalFormatSymbols(symbols);
    }

    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public int getFactor() {
        return factor;
    }
    public void setFactor(int factor) {
        this.factor = factor;
    }

    public void setND(double nd) {
        factor = (int) Math.round(Math.pow(10, nd));
    }
    public double getND() {
        return Math.log10(factor);
    }

    public void setFStops(double fstops) {
        factor = (int) Math.round(Math.pow(2, fstops));
    }
    public double getFstops() {
        return Math.log(factor)/log2;
    }

    public int getOrderpos() {
        return orderpos;
    }
    public void setOrderpos(int orderpos) {
        this.orderpos = orderpos;
    }

    public String getDescription(final Context context) {
        return "ND " + decimalFormat.format(getND()) + " | " + context.getString(R.string.factor) + decimalFormat.format(factor) + " | " + context.getString(R.string.fstops) + decimalFormat.format(getFstops());
    }

    public static boolean isValidND(double nd) {
        int factor = (int) Math.round(Math.pow(10, nd));
        double fstops = Math.log(factor)/log2;
        return factor >= 1 && nd >= 0 && !Double.isInfinite(nd) && !Double.isNaN(nd)
                && fstops >= 0 && !Double.isInfinite(fstops) && !Double.isNaN(fstops);
    }
    public static boolean isValidFactor(int factor) {
        double fstops = Math.log(factor)/log2;
        double nd = Math.log10(factor);
        return factor >= 1 && nd >= 0 && !Double.isInfinite(nd) && !Double.isNaN(nd)
                && fstops >= 0 && !Double.isInfinite(fstops) && !Double.isNaN(fstops);
    }
    public static boolean isValidFstops(double fstops) {
        int factor = (int) Math.round(Math.pow(2, fstops));
        double nd = Math.log10(factor);
        return factor >= 1 && nd >= 0 && !Double.isInfinite(nd) && !Double.isNaN(nd)
                && fstops >= 0 && !Double.isInfinite(fstops) && !Double.isNaN(fstops);
    }

    public static final NDFilter[] builtinFilters = {
            new NDFilter(-1, "BW 101", 2),
            new NDFilter(-2, "BW 102", 4),
            new NDFilter(-3, "BW 103", 8),
            new NDFilter(-4, "", 16),
            new NDFilter(-5, "", 32),
            new NDFilter(-6, "BW106", 64),
            new NDFilter(-7, "", 100),
            new NDFilter(-8, "", 256),
            new NDFilter(-9, "", 400),
            new NDFilter(-10, "Hoya NDX400", 512),
            new NDFilter(-11, "BW 110", 1000),
            new NDFilter(-12, "BW 113", 10000),
            new NDFilter(-13, "BW 120", 1000000)
    };
}
