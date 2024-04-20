package com.hpn.hmessager.converter;

import static com.hpn.hmessager.converter.DataConverter.dateToByte;
import static com.hpn.hmessager.converter.DataConverter.intToByte;

import com.hpn.hmessager.data.model.ConvMetadata;
import com.hpn.hmessager.domain.utils.HByteArrayInputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ConvMetadataConverter extends Converter<ConvMetadata> {

    @Override
    public byte[] encode(ConvMetadata metadata) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            // Conversation name
            byte[] tmp = metadata.getName().getBytes(StandardCharsets.UTF_8);
            bos.write(intToByte(tmp.length));
            bos.write(tmp);

            // Conversation icon
            if (metadata.getAvatarUrl() != null) {
                tmp = metadata.getAvatarUrl().getBytes(StandardCharsets.UTF_8);
                bos.write(intToByte(tmp.length));
                bos.write(tmp);
            } else {
                bos.write(intToByte(0));
            }

            // Number of unread messages
            bos.write(intToByte(metadata.getUnreadCount()));

            if (metadata.getLastMessage() != null) {
                tmp = metadata.getLastMessage().getBytes(StandardCharsets.UTF_8);
                bos.write(intToByte(tmp.length));
                bos.write(tmp);

                bos.write(dateToByte(metadata.getLastMessageDate()));
            }

            return bos.toByteArray();
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }
    }

    @Override
    public ConvMetadata decode(byte[] data, Object other) {
        ConvMetadata metadata = new ConvMetadata((int) other);

        try (HByteArrayInputStream bis = new HByteArrayInputStream(data)) {
            metadata.setName(bis.readString()); // Read conv name

            int len = bis.readInt(); // Read avatar url
            if (len > 0) {
                String url = bis.readString(len);
                metadata.setAvatarUrl(url);
            }

            metadata.setUnreadCount(bis.readInt()); // Read unread messages count

            // Read last message and date
            if (bis.available() > 0) {
                metadata.setLastMessage(bis.readString());
                metadata.setLastMessageDate(bis.readDate());
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }

        return metadata;
    }
}
