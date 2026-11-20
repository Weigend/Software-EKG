//______________________________________________________________________________
//
//          Project:    Software EKG
//______________________________________________________________________________
//
//         Author:      Weigend AM 2009 - 2014
//______________________________________________________________________________
//
// Notice: This piece of software was created, designed and implemented by
// experienced craftsmen and innovators in Munich, Germany.
// Changes should be done with respect to the original design.
//______________________________________________________________________________

package io.softwareecg.collector.common;


import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;


/**
 * Created by johannes.weigend on 06.03.14.
 */
public class EncryptUtilTest {
    @Test
    public void testGetEncryptStream() throws Exception {
        byte[] data = {1, 2, 3, 4};
        byte[] data2 = new byte[data.length];
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        EncryptUtil util = new EncryptUtil();
        OutputStream sos = util.getEncryptStream(os);
        sos.write(data);
        sos.close();
        os.close();
        int r = new EncryptUtil().getDecryptStream(new ByteArrayInputStream(os.toByteArray())).read(data2);
        assertArrayEquals(data, data2);
    }

    @Test
    public void testGetDecryptStream() throws Exception {
        byte[] data = {1, 2, 3, 4};
        byte[] data2 = new byte[data.length];
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        EncryptUtil util = new EncryptUtil();
        OutputStream sos = util.getEncryptStream(os);
        sos.write(data);
        sos.close();
        os.close();
        int r = new EncryptUtil().getDecryptStream(new ByteArrayInputStream(os.toByteArray())).read(data2);
        assertArrayEquals(data, data2);

    }
}
