package com.hpn.hmessager.converter;

import static com.hpn.hmessager.converter.DataConverter.intToByte;

import com.hpn.hmessager.data.model.Preference;
import com.hpn.hmessager.domain.utils.HByteArrayInputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PreferenceConverter extends Converter<Preference> {

    public byte[] encode(Preference pref) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        try {
            bos.write(intToByte(pref.getThemeId()));
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }

        return bos.toByteArray();
    }

    @Override
    public Preference decode(byte[] data, Object other) {
        Preference pref = new Preference();

        try (HByteArrayInputStream bis = new HByteArrayInputStream(data)) {
            pref.setThemeId(bis.readInt());
        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }

        return pref;
    }
}
