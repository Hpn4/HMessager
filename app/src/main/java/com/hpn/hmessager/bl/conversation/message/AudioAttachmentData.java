package com.hpn.hmessager.bl.conversation.message;

import android.content.Context;
import android.net.Uri;
import android.util.Pair;

import com.hpn.hmessager.bl.io.StorageManager;
import com.hpn.hmessager.bl.utils.HByteArrayInputStream;
import com.hpn.hmessager.bl.utils.MediaHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AudioAttachmentData extends MediaAttachmentData {

    public static final int WAVEFORM_COUNT = 40;

    private int duration; // duration in seconds

    private List<Integer> waveform;

    public AudioAttachmentData() {
    }

    public void setupMetadata(Uri uri, Context context) {
        super.setupMetadata(uri, context);
        Pair<List<Integer>, Integer> metadata = MediaHelper.getAudioMetadata(context, uri);

        waveform = metadata.first;
        duration = metadata.second;
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
