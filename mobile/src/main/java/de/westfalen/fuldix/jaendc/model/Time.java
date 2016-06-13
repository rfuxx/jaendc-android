package de.westfalen.fuldix.jaendc.model;

import de.westfalen.fuldix.jaendc.text.CameraTimeFormat;

public class Time {
    public final static double[] times = { 1d/8000, 1d/6400, 1d/5000,
            1d/4000, 1d/3200, 1d/2500,
            1d/2000, 1d/1600, 1d/1250,
            1d/1000, 1d/800, 1d/640,
            1d/500, 1d/400, 1d/320,
            1d/250, 1d/200, 1d/160,
            1d/125, 1d/100, 1d/80,
            1d/60, 1d/50, 1d/40,
            1d/30, 1d/25, 1d/20,
            1d/15, 1d/12, 1d/10,
            1d/8, 1d/6, 1d/5,
            1d/4, 0.3d, 0.4d,
            0.5d, 0.6d, 0.8d,
            1, 1.3d, 1.6d,
            2, 2.5d, 3.2d,
            3, 5, 6,
            8, 10, 13,
            15, 20, 25,
            30
    };

    public static String[] getTimeTexts() {
        String[] timeTexts = new String[times.length];
        CameraTimeFormat cameraTimeFormat = new CameraTimeFormat();
        for(int i=timeTexts.length-1; i>=0; i--) {
            timeTexts[i] = cameraTimeFormat.format(times[i]);
        }
        return timeTexts;
    }

    public static double roundTimeToCameraTime(double time, double roundLimit) {
        if(time >= roundLimit) {
            return time;
        }
        double returnTime = time;
        double delta = Double.MAX_VALUE;
        for(final double ct : times) {
            if(ct == time) {
                return time;
            } else if (ct < time) {
                double tDelta = time-ct;
                if(tDelta < delta) {
                    returnTime = ct;
                    delta = tDelta;
                }
            } else { // ct > time
                double tDelta = ct-time;
                if(tDelta < delta) {
                    returnTime = ct;
                    break; // can break here because next ones will be even bigger (more delta)
                }
            }
        }
        return returnTime;
    }
}
