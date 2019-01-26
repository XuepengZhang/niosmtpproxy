package com.sailing.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Util {
    private static final Logger log = LoggerFactory.getLogger(Util.class);


    public static byte[] toByteArray(InputStream is) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] bs = new byte[1024];
        int len = -1;
        try {
            while ((len = is.read(bs)) != -1) {
                bos.write(bs, 0, len);
            }
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        } finally {
            try {
                bos.close();
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }

        byte b[] = bos.toByteArray();

        return b;
    }
}
