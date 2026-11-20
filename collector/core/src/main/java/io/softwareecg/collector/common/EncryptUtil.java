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


import javax.crypto.*;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.IvParameterSpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.spec.AlgorithmParameterSpec;


/**
 * EncryptUtil.
 * No singleton - Cipher not threadsafe.
 */
public class EncryptUtil {

    /**
     * The cipher encryption.
     */
    public static final String CIPHER_ENCRYPTION = "DES/CBC/PKCS5Padding";

    /**
     * The ecipher.
     */
    private final Cipher ecipher;

    /**
     * The dcipher.
     */
    private final Cipher dcipher;

    /**
     * Instantiates a new encrypt datamanagement.
     */
    public EncryptUtil() {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("DES");
            DESKeySpec ks = new DESKeySpec(new byte[]{1, 2, 3, 4, 5, 6, 7, 8});
            SecretKey key = factory.generateSecret(ks);
            byte[] iv = new byte[]{(byte) 0x8E, 0x12, 0x39, (byte) 0x9C, 0x07, 0x72, 0x6F, 0x5A};
            AlgorithmParameterSpec paramSpec = new IvParameterSpec(iv);
            ecipher = Cipher.getInstance(CIPHER_ENCRYPTION);
            dcipher = Cipher.getInstance(CIPHER_ENCRYPTION);

            ecipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
            dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
        } catch (Exception e) {
            throw new IllegalStateException("Encrypt datamanagement could not initialized.", e);
        }
    }

    /**
     * Gets the encrypt stream.
     *
     * @param out the out
     * @return the encrypt stream
     */
    public OutputStream getEncryptStream(OutputStream out) {
        return new CipherOutputStream(out, ecipher);
    }

    /**
     * Gets the decrypt stream.
     *
     * @param in the in
     * @return the decrypt stream
     */
    public InputStream getDecryptStream(InputStream in) {
        return new CipherInputStream(in, dcipher);
    }
}
