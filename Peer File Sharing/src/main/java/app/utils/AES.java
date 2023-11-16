package app.utils;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * AES is a utility class that provides methods to encrypt and decrypt data using the AES
 * (Advanced Encryption Standard) algorithm with CBC (Cipher Block Chaining) mode and PKCS5 padding.
 */
public class AES {

    // Specifies the algorithm used for cryptographic operations.
    private static final String ALGORITHM = "AES";

    /**
     * Encrypts plaintext using AES with a given secret key.
     *
     * @param secretKey The SecretKey object to be used for encryption.
     * @param plaintext The byte array containing the plaintext to encrypt.
     * @return A byte array containing the IV and the encrypted data.
     * @throws Exception If any cryptographic errors occur during encryption.
     */
    public static byte[] encrypt(SecretKey secretKey, byte[] plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] ivBytes = new byte[cipher.getBlockSize()];
        SecureRandom secureRandom = new SecureRandom();
        secureRandom.nextBytes(ivBytes);
        IvParameterSpec iv = new IvParameterSpec(ivBytes);

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv);
        byte[] ciphertext = cipher.doFinal(plaintext);

        return concatenate(ivBytes, ciphertext);
    }

    /**
     * Decrypts ciphertext using AES with a given secret key.
     *
     * @param secretKey The SecretKey object to be used for decryption.
     * @param ciphertext The byte array containing the IV followed by the encrypted data.
     * @return A byte array containing the decrypted plaintext.
     * @throws Exception If any cryptographic errors occur during decryption.
     */
    public static byte[] decrypt(SecretKey secretKey, byte[] ciphertext) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] ivBytes = Arrays.copyOfRange(ciphertext, 0, cipher.getBlockSize());
        IvParameterSpec iv = new IvParameterSpec(ivBytes);

        byte[] ciphertextWithoutIv = Arrays.copyOfRange(ciphertext, cipher.getBlockSize(), ciphertext.length);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);

        return cipher.doFinal(ciphertextWithoutIv);
    }

    /**
     * Concatenates two byte arrays into one.
     *
     * @param a The first byte array.
     * @param b The second byte array.
     * @return A concatenated byte array containing 'a' followed by 'b'.
     */
    private static byte[] concatenate(byte[] a, byte[] b) {
        int aLen = a.length;
        int bLen = b.length;
        byte[] c = new byte[aLen + bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }

    /**
     * Writes a SecretKey object to a file.
     *
     * @param key      The SecretKey to be written to the file.
     * @param filePath The path to the file where the key will be written.
     * @throws IOException If an I/O error occurs writing to or creating the file.
     */
    public static void writeKeyToFile(SecretKey key, String filePath) throws IOException {
        Path path = Paths.get(filePath);
        byte[] encodedKey = key.getEncoded();

        // Create the file if it does not exist
        if (!Files.exists(path)) {
            Files.createFile(path);
        }

        Files.write(path, encodedKey);
    }

    /**
     * Generates a SecretKey object from a byte array.
     *
     * @param keyBytes The byte array representing the key.
     * @return A SecretKey object.
     */
    public static SecretKey getSecretKey(byte[] keyBytes) {
        return new SecretKeySpec(keyBytes, ALGORITHM);
    }
}
