package com.hpn.hmessager.bl.utils;

import android.content.Context;

import linc.com.amplituda.Amplituda;

public class MediaHelper {

    private static Amplituda amp;

    public static Amplituda getAmplituda(Context context) {
        if(amp == null)
            amp = new Amplituda(context);

        return amp;
    }

    public static void release() {
        amp = null;
    }
}
