package com.hpn.hmessager.data.model.message;

import static com.hpn.hmessager.converter.DataConverter.byteToLong;
import static com.hpn.hmessager.converter.DataConverter.intToByte;

import android.content.Context;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.hpn.hmessager.converter.MessageConverter;
import com.hpn.hmessager.data.repository.ConversationStorage;
import com.hpn.hmessager.data.repository.FileRepository;
import com.hpn.hmessager.domain.utils.HByteArrayInputStream;
import com.hpn.hmessager.domain.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import lombok.Getter;

public class MediaAttachment {

    private static final FileRepository fileRepository = new FileRepository(null);

    private final Context context;

    private MediaAttachmentData data;

    @Getter
    private MediaType mediaType;

    @Getter
    private Uri file;

    private MediaAttachment(Context context) {
        this.context = context;
    }

    public static long getSizeFromMeta(byte[] metadata) {
        return byteToLong(metadata, MessageConverter.MSG_HEADER_LENGTH + 1);
    }

    public static MediaAttachment fromNetwork(byte[] metadata, byte[] media, ConversationStorage storage, Context context) {
        MediaAttachment mediaAttachment = new MediaAttachment(context);

        // Read metadata
        HByteArrayInputStream bais = new HByteArrayInputStream(metadata);

        MediaType type = MediaType.fromCode(bais.readByte());
        MediaAttachmentData data = (type == MediaType.AUDIO) ? new AudioAttachmentData() : new MediaAttachmentData();

        // Load metadata
        data.decodeMetadata(bais, true);

        // Retrieve the file
        File mediaFile = storage.getMediaFile(data.getName(), type);
        Uri file = FileProvider.getUriForFile(context, Utils.fileProvider, mediaFile);

        // Write media to file
        fileRepository.writeData(media, mediaFile);

        data.loadMetadata(file, context);

        mediaAttachment.mediaType = type;
        mediaAttachment.data = data;
        mediaAttachment.file = file;

        return mediaAttachment;
    }

    public static MediaAttachment fromDisk(byte[] meta, Context context) {
        MediaAttachment mediaAttachment = new MediaAttachment(context);

        HByteArrayInputStream bais = new HByteArrayInputStream(meta);

        // Load type and path
        MediaType type = MediaType.fromCode(bais.readByte());
        Uri file = Uri.parse(bais.readString());

        // Setup data based on type
        MediaAttachmentData data = (type == MediaType.AUDIO) ? new AudioAttachmentData() : new MediaAttachmentData();

        // Load metadata
        data.loadMetadata(file, context);
        data.decodeMetadata(bais, false);

        mediaAttachment.data = data;
        mediaAttachment.mediaType = type;
        mediaAttachment.file = file;

        return mediaAttachment;
    }

    /**
     * This function is called only once, when the user create the media attachment (pick a file).
     * It will retrieve the type, name and size of the file.
     * And other metadata:
     * - For audio: duration and waveform
     *
     * @param uri The uri of the file the user picked
     */
    public static MediaAttachment fromUri(Uri uri, Context context) {
        MediaAttachment mediaAttachment = new MediaAttachment(context);

        String mimeType = context.getContentResolver().getType(uri);
        MediaType type = MediaType.fromMimeType(mimeType);

        MediaAttachmentData data = (type == MediaType.AUDIO) ? new AudioAttachmentData() : new MediaAttachmentData();
        data.loadMetadata(uri, context);

        mediaAttachment.mediaType = type;
        mediaAttachment.data = data;
        mediaAttachment.file = uri;

        return mediaAttachment;
    }

    public byte[] readContent() {
        byte[] data = null;
        try {
            data = fileRepository.readAllData(context.getContentResolver().openInputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return data;
    }

    public byte[] encodeMetadata(boolean forNetwork) {
        ByteArrayOutputStream bais = new ByteArrayOutputStream();

        try {
            // Write media type
            bais.write(new byte[]{mediaType.code});

            // Write file uri if it's for storage usage
            if (!forNetwork) {
                byte[] fileNameBytes = file.toString().getBytes(StandardCharsets.UTF_8);
                bais.write(intToByte(fileNameBytes.length));
                bais.write(fileNameBytes);
            }

            data.encodeMetadata(bais, forNetwork);

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
}
