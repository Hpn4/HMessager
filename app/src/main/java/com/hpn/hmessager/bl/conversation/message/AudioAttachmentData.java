package com.hpn.hmessager.bl.conversation.message;

import android.content.Context;
import android.net.Uri;

import com.hpn.hmessager.bl.io.StorageManager;
import com.hpn.hmessager.bl.utils.HByteArrayInputStream;
import com.hpn.hmessager.bl.utils.MediaHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import linc.com.amplituda.Amplituda;
import linc.com.amplituda.AmplitudaResult;

public class AudioAttachmentData extends MediaAttachmentData {

    private static final int WAVEFORM_COUNT = 40;

    private int duration; // duration in seconds

    private List<Integer> waveform;

    public AudioAttachmentData() {
    }

    public void setupMetadata(Uri uri, Context context) {
        super.setupMetadata(uri, context);

        Amplituda amp = MediaHelper.getAmplituda(context);

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
    }

    private List<Integer> compressArray(List<Integer> amps) {
        int stepSize = (int) Math.ceil((float) amps.size() / WAVEFORM_COUNT);
        List<Integer> newAmps = new ArrayList<>(WAVEFORM_COUNT);

        for(int i = 0; i < amps.size(); ++i) {
            int totalHeuristic = newAmps.size() + (amps.size() - i);

            if(totalHeuristic <= WAVEFORM_COUNT) {
                newAmps.add(amps.get(i));
                continue;
            }

            int sum = 0, j = 0;
            while(i + j < amps.size() && j < stepSize) {
                sum += amps.get(i + j);
                ++j;
            }

            i += j;
            newAmps.add(sum / j);
        }

        return newAmps;
    }

    public void loadMetadata(HByteArrayInputStream bais, boolean fromNetwork) {
        super.loadMetadata(bais, fromNetwork);

        if (!fromNetwork) {
            duration = bais.readInt();
            int size = bais.readInt();

            waveform = new ArrayList<>(size);
            for (int i = 0; i < size; ++i)
                waveform.add(bais.readInt());
        }
    }

    public void constructMetadata(ByteArrayOutputStream baos, boolean forNetwork) throws IOException {
        super.constructMetadata(baos, forNetwork);

        if (!forNetwork) {
            baos.write(StorageManager.intToByte(duration));
            baos.write(StorageManager.intToByte(waveform.size()));

            for (int i = 0; i < waveform.size(); ++i)
                baos.write(StorageManager.intToByte(waveform.get(i)));
        }
    }

    public int getDuration() {
        return duration;
    }

    public List<Integer> getWaveform() {
        return waveform;
    }
}
