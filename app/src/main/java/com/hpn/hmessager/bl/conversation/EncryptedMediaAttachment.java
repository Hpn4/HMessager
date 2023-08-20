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

public class EncryptedMediaAttachment {

    private String name;

    private long size;

    private MediaType mediaType;

    private byte[] data;

    public EncryptedMediaAttachment(byte[] data) {
        parseFromStorage(data);
    }

    public EncryptedMediaAttachment(Uri uri, Context context) {
        loadMetadata(uri, context); // Get type, name and size
        loadMediaData(uri, context); // Get content of the file
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

    private void loadMediaData(Uri uri, Context context) {
        try {
            BufferedInputStream inputStream = new BufferedInputStream(context.getContentResolver().openInputStream(uri));

            data = new byte[inputStream.available()];

            int offset = 0;
            while(offset < data.length)
                offset += inputStream.read(data, offset, data.length - offset);

            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parseFromStorage(byte[] data) {
        HByteArrayInputStream inputStream = new HByteArrayInputStream(data);

        mediaType = MediaType.fromCode(inputStream.readByte());
        name = inputStream.readString();
        size = inputStream.readLong();
        this.data = inputStream.readRemainingBytes();
    }

    public byte[] constructForStorage() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            outputStream.write(new byte[]{mediaType.getCode()});

            outputStream.write(StorageManager.intToByte(name.length()));
            outputStream.write(name.getBytes());

            outputStream.write(StorageManager.longToByte(size));

            outputStream.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outputStream.toByteArray();
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

    public byte[] getData() {
        return data;
    }
}
