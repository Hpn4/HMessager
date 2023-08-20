package com.hpn.hmessager.bl.conversation;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import com.hpn.hmessager.bl.io.StorageManager;
import com.hpn.hmessager.bl.utils.HByteArrayInputStream;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MediaAttachment {

    private String name;

    private long size;

    private MediaType mediaType;

    private Uri file;

    public MediaAttachment(byte[] data) {
        parseFromStorage(data);
    }

    public MediaAttachment(Uri uri, Context context) {
        file = uri;
        loadMetadata(uri, context); // Get type, name and size
    }

    private void loadMetadata(Uri uri, Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(uri, null, null, null, null);
        boolean metaLoaded = false;

        String mimeType = contentResolver.getType(uri);
        mediaType = MediaType.fromMimeType(mimeType);

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

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public Uri getFile() {
        return file;
    }
}
