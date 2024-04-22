package com.hpn.hmessager.domain.utils;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;

public class Utils {

    public final static String fileProvider = "com.hpn.hmessager.presentation.activity.ConvActivity.provider";

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String getDateString(Instant instant) {
        LocalDate realDate = instant.atZone(ZoneId.systemDefault()).toLocalDate();
        Period d = Period.between(realDate, LocalDate.now());

        if (d.getYears() >= 1)
            return realDate.getDayOfMonth() + " " + realDate.getMonth().toString() + " " + realDate.getYear();

        if (d.getMonths() >= 1 || d.getDays() > 14) {
            String month = realDate.getMonth().toString();
            return realDate.getDayOfMonth() + " " + month.charAt(0) + month.substring(1).toLowerCase();
        }

        if (d.getDays() >= 7)
            return (d.getDays() / 7) + "w";

        if (d.getDays() >= 1)
            return d.getDays() + "d";

        Duration duration = Duration.between(instant, Instant.now());
        if (duration.toHours() >= 1)
            return duration.toHours() + "h";

        if (duration.toMinutes() >= 1)
            return duration.toMinutes() + "m";

        return "now";
    }
}
