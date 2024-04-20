package com.hpn.hmessager.domain.utils;

import android.content.Context;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.util.Pair;

import com.hpn.hmessager.data.model.message.AudioAttachmentData;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import linc.com.amplituda.Amplituda;
import linc.com.amplituda.AmplitudaResult;

public class HMediaHelper {

    private static Amplituda amp;

    private static MediaRecorder recorder;

    // Load an amplituda instance
    private static Amplituda getAmplituda(Context context) {
        if (amp == null)
            amp = new Amplituda(context);

        return amp;
    }

    // Setup a media recorder instance
    private static void setupRecorder(Context context) {
        if (recorder == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                recorder = new MediaRecorder(context);
            } else recorder = new MediaRecorder();
        }
    }

    public static Pair<List<Integer>, Integer> getAudioMetadata(Context context, Uri uri) {
        int duration = 0;
        List<Integer> waveform = null;
        Amplituda amp = HMediaHelper.getAmplituda(context);

        try {
            InputStream is = context.getContentResolver().openInputStream(uri);

            if (is != null) {
                AmplitudaResult<InputStream> result = amp.processAudio(is).get();
                List<Integer> amps = result.amplitudesAsList();
                duration = (int) result.getAudioDuration(AmplitudaResult.DurationUnit.SECONDS);

                waveform = compressArray(amps);

                is.close();
            }
        } catch (IOException ignored) {
        }

        return new Pair<>(waveform, duration);
    }

    public static void startRecording(Context context, File outputFile) {
        setupRecorder(context);

        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        try {
            recorder.setOutputFile(new FileOutputStream(outputFile).getFD());
            recorder.prepare();
        } catch (IOException ignored) {
        }

        recorder.start();
    }

    public static void stopRecording() {
        recorder.stop();
    }

    private static List<Integer> compressArray(List<Integer> amps) {
        int stepSize = (int) Math.ceil((float) amps.size() / AudioAttachmentData.WAVEFORM_COUNT);
        List<Integer> newAmps = new ArrayList<>(AudioAttachmentData.WAVEFORM_COUNT);

        for (int i = 0; i < amps.size(); ++i) {
            int totalHeuristic = newAmps.size() + (amps.size() - i);

            if (totalHeuristic <= AudioAttachmentData.WAVEFORM_COUNT) {
                newAmps.add(amps.get(i));
                continue;
            }

            int sum = 0, j = 0;
            while (i + j < amps.size() && j < stepSize) {
                sum += amps.get(i + j);
                ++j;
            }

            i += j;
            newAmps.add(sum / j);
        }

        return newAmps;
    }

    public static void release() {
        amp.clearCache();
        amp = null;
        recorder.release();
        recorder = null;
    }
}
