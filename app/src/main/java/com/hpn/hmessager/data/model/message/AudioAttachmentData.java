package com.hpn.hmessager.data.model.message;

import static com.hpn.hmessager.converter.DataConverter.intToByte;

import android.content.Context;
import android.net.Uri;
import android.util.Pair;

import com.hpn.hmessager.domain.utils.HByteArrayInputStream;
import com.hpn.hmessager.domain.utils.HMediaHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

@Getter
public class AudioAttachmentData extends MediaAttachmentData {

    public static final int WAVEFORM_COUNT = 40;

    private int duration; // duration in seconds

    private List<Integer> waveform;

    public void loadMetadata(Uri uri, Context context) {
        super.loadMetadata(uri, context);
        Pair<List<Integer>, Integer> metadata = HMediaHelper.getAudioMetadata(context, uri);

        waveform = metadata.first;
        duration = metadata.second;
    }

    public void decodeMetadata(HByteArrayInputStream bais, boolean fromNetwork) {
        super.decodeMetadata(bais, fromNetwork);

        if (!fromNetwork) {
            duration = bais.readInt();
            int size = bais.readInt();

            waveform = new ArrayList<>(size);
            for (int i = 0; i < size; ++i)
                waveform.add(bais.readInt());
        }
    }

    public void encodeMetadata(ByteArrayOutputStream baos, boolean forNetwork) throws IOException {
        super.encodeMetadata(baos, forNetwork);

        if (!forNetwork) {
            baos.write(intToByte(duration));
            baos.write(intToByte(waveform.size()));

            for (int i = 0; i < waveform.size(); ++i)
                baos.write(intToByte(waveform.get(i)));
        }
    }

}
