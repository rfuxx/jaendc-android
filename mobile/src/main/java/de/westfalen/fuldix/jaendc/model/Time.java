package de.westfalen.fuldix.jaendc.model;

import de.westfalen.fuldix.jaendc.text.CameraTimeFormat;

public class Time {
    public final static double[][] times = {
            {
                1d/8000, 1d/6400, 1d/5000,
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
                4, 5, 6,
                8, 10, 13,
                15, 20, 25,
                30, 40, 50,
                60
            }, {
                1d/8000, 1d/6400, 1d/5000,
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
                1d/4, 1d/3.2d, 1d/2.5d,
                1d/2, 1d/1.6d, 1d/1.3d,
                1, 1.3d, 1.6d,
                2, 2.5d, 3.2d,
                4, 5, 6,
                8, 10, 13,
                15, 20, 25,
                30, 40, 50,
                60
        }, {
                1d/8000, 1d/6000,
                1d/4000, 1d/3000,
                1d/2000, 1d/1500,
                1d/1000, 1d/750,
                1d/500, 1d/350,
                1d/250, 1d/180,
                1d/125, 1d/90,
                1d/60, 1d/45,
                1d/30, 1d/20,
                1d/15, 1d/10,
                1d/8, 1d/6,
                1d/4, 0.3d,
                1d/2, 0.7d,
                1, 1.5d,
                2, 3,
                4, 6,
                8, 10,
                15, 20,
                30, 45,
                60
        }, {
                1d/8000, 1d/6000,
                1d/4000, 1d/3000,
                1d/2000, 1d/1500,
                1d/1000, 1d/750,
                1d/500, 1d/350,
                1d/250, 1d/180,
                1d/125, 1d/90,
                1d/60, 1d/45,
                1d/30, 1d/20,
                1d/15, 1d/10,
                1d/8, 1d/6,
                1d/4, 1d/3,
                1d/2, 1d/1.3d,
                1, 1.5d,
                2, 3,
                4, 6,
                8, 10,
                15, 20,
                30, 45,
                60
            }
    };

    public static boolean isTimeStyleWithDecimalPlacesFractions(final int timeStyle) {
        switch(timeStyle) {
            case 1:
            case 3: {
                // Panasonic Format: reciprocal value (3.2/2.5/2/1.6/1.3)
                return true;
            }
            default: {
                // Canon Format: decimal places (0"3/0"4/0"5/0"6/0"8)
                return false;
            }
        }
    }

    public static String[] getTimeTexts(final int timeStyle) {
        final String[] timeTexts = new String[times[timeStyle].length];
        final CameraTimeFormat cameraTimeFormat = new CameraTimeFormat(timeStyle);
        for(int i=timeTexts.length-1; i>=0; i--) {
            timeTexts[i] = cameraTimeFormat.format(times[timeStyle][i]);
        }
        return timeTexts;
    }

    public static double roundTimeToCameraTime(final double time, final double roundLimit, final int timeStyle) {
        if(time >= roundLimit) {
            return time;
        }
        double returnTime = time;
        double delta = Double.MAX_VALUE;
        for(final double ct : times[timeStyle]) {
            if(ct == time) {
                return time;
            } else if (ct < time) {
                double tDelta = time-ct;
                if(tDelta <= delta) {
                    returnTime = ct;
                    delta = tDelta;
                }
            } else { // ct > time
                double tDelta = ct-time;
                if(tDelta <= delta) {   // including = for 1/2 stops calculation of 0"3 * 2 = 0"6 should round to 0"7 (not 0"5)
                    returnTime = ct;
                    break; // can break here because next ones will be even bigger (more delta)
                }
            }
        }
        return returnTime;
    }
}
