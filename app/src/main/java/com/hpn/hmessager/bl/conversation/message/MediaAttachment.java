package com.hpn.hmessager.bl.conversation.message;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.hpn.hmessager.bl.io.ConversationStorage;
import com.hpn.hmessager.bl.io.StorageManager;
import com.hpn.hmessager.bl.utils.HByteArrayInputStream;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class MediaAttachment {

    private final Context context;

    private MediaAttachmentData data;

    private MediaType mediaType;

    private Uri file;

    // Constructor for media received from the network
    public MediaAttachment(byte[] metadata, byte[] media, ConversationStorage storage, Context context) {
        this.context = context;
        saveMedia(metadata, media, storage);
    }

    // Constructor for the creation of a media attachment
    public MediaAttachment(Uri uri, Context context) {
        file = uri;
        this.context = context;
        setupMetadata(uri); // Get type, name and size
    }

    // Constructor for media read from the disk
    public MediaAttachment(byte[] metadata, Context context) {
        this.context = context;
        loadMetadataFromDisk(metadata);
    }

    public static int getSizeFromMeta(byte[] metadata) {
        return StorageManager.byteToInt(metadata, 1);
    }

    private void saveMedia(byte[] metadata, byte[] media, ConversationStorage storage) {
        // Read metadata
        HByteArrayInputStream bais = new HByteArrayInputStream(metadata);

        mediaType = MediaType.fromCode(metadata[0]); // Type
        data = (mediaType == MediaType.AUDIO) ? new AudioAttachmentData() : new MediaAttachmentData();
        data.loadMetadata(bais, true);

        // Retrieve the file
        File mediaFile = storage.getMediaFile(data.getName(), mediaType);
        file = FileProvider.getUriForFile(context, "com.hpn.hmessager.ui.activity.ConvActivity.provider", mediaFile);

        // Write the file
        try {
            mediaFile.createNewFile();

            BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(mediaFile));
            outputStream.write(media);
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        data.setupMetadata(file, context);
    }

    /**
     * This function is called only once, when the user create the media attachment (pick a file).
     * It will retrieve the type, name and size of the file.
     * And other metadata:
     * - For audio: duration and waveform
     *
     * @param uri The uri of the file the user picked
     */
    private void setupMetadata(Uri uri) {
        ContentResolver contentResolver = context.getContentResolver();

        String mimeType = contentResolver.getType(uri);
        mediaType = MediaType.fromMimeType(mimeType);

        data = (mediaType == MediaType.AUDIO) ? new AudioAttachmentData() : new MediaAttachmentData();

        data.setupMetadata(uri, context);
    }

    private void loadMetadataFromDisk(byte[] meta) {
        HByteArrayInputStream bais = new HByteArrayInputStream(meta);

        mediaType = MediaType.fromCode(bais.readByte());
        file = Uri.parse(bais.readString());

        data = (mediaType == MediaType.AUDIO) ? new AudioAttachmentData() : new MediaAttachmentData();

        data.extractNameAndSize(file, context);
        data.loadMetadata(bais, false);
    }

    public byte[] readContent() {
        byte[] data = null;

        try {
            BufferedInputStream inputStream = new BufferedInputStream(context.getContentResolver().openInputStream(file));
            data = new byte[inputStream.available()];

            inputStream.read(data);
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return data;
    }

    public byte[] constructMetadata(boolean forNetwork) {
        ByteArrayOutputStream bais = new ByteArrayOutputStream();

        try {
            // Write media type
            bais.write(new byte[]{mediaType.code});

            // Write file uri if it's for storage usage
            if (!forNetwork) {
                byte[] fileNameBytes = file.toString().getBytes(StandardCharsets.UTF_8);
                bais.write(StorageManager.intToByte(fileNameBytes.length));
                bais.write(fileNameBytes);
            }

            data.constructMetadata(bais, forNetwork);

            return bais.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getName() {
        return data.getName();
    }

    public long getSize() {
        return data.getSize();
    }

    public AudioAttachmentData getAudioMetadata() {
        return mediaType == MediaType.AUDIO ? (AudioAttachmentData) data : null;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    public Uri getFile() {
        return file;
    }
}
