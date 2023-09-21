package com.hpn.hmessager.bl.conversation.message;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.hpn.hmessager.bl.io.StorageManager;
import com.hpn.hmessager.bl.utils.HByteArrayInputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MediaAttachmentData {

    private String name;

    private long size;

    public MediaAttachmentData() {
    }

    public void extractNameAndSize(Uri uri, Context context) {
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

    public void setupMetadata(Uri uri, Context context) {
        extractNameAndSize(uri, context);
    }

    public void loadMetadata(HByteArrayInputStream bais, boolean fromNetwork) {
        if(fromNetwork) {
            size = bais.readLong();
            name = bais.readString();
        }
    }

    public void constructMetadata(ByteArrayOutputStream baos, boolean forNetwork) throws IOException {
        if (forNetwork) {
            byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);

            baos.write(StorageManager.longToByte(size));
            baos.write(StorageManager.intToByte(nameBytes.length));
            baos.write(nameBytes);
        }
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }
}
