package com.hpn.hmessager.data.model.message;

import static com.hpn.hmessager.converter.DataConverter.intToByte;
import static com.hpn.hmessager.converter.DataConverter.longToByte;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.hpn.hmessager.domain.utils.HByteArrayInputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import lombok.Getter;

@Getter
public class MediaAttachmentData {

    private String name;

    private long size;

    public MediaAttachmentData() {
    }

    public void loadMetadata(Uri uri, Context context) {
        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(uri, null, null, null, null);
        boolean metaLoaded = false;

        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);

            if (nameIndex != -1 && sizeIndex != -1) {
                name = cursor.getString(nameIndex);
                size = cursor.getLong(sizeIndex);

                metaLoaded = true;
            }

            cursor.close();
        }

        if (!metaLoaded) {
            name = uri.getLastPathSegment();
            size = -1;
        }
    }

    public void decodeMetadata(HByteArrayInputStream bais, boolean fromNetwork) {
        if(fromNetwork) {
            size = bais.readLong();
            name = bais.readString();
        }
    }

    public void encodeMetadata(ByteArrayOutputStream baos, boolean forNetwork) throws IOException {
        if (forNetwork) {
            byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);

            baos.write(longToByte(size));
            baos.write(intToByte(nameBytes.length));
            baos.write(nameBytes);
        }
    }
}
