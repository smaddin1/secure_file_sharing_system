package app.utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.Cipher;

/**
 * The RSA class provides utility methods for RSA encryption and decryption.
 * It allows generating RSA key pairs, encrypting and decrypting data using RSA keys,
 * and converting keys to and from string format.
 */
public class RSA {
    private static final String ALGORITHM = "RSA";

    /**
     * Generates an RSA KeyPair with the specified key size.
     *
     * @param keySize The size of the key to generate.
     * @return A KeyPair object containing the RSA public and private keys.
     * @throws NoSuchAlgorithmException If the RSA algorithm is not available.
     */
    public static KeyPair generateKeyPair(int keySize) throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM);
        keyPairGenerator.initialize(keySize);
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * Encrypts data using an RSA public key.
     *
     * @param data The data to encrypt.
     * @param publicKey The public key for encryption.
     * @return The encrypted data.
     * @throws Exception If an encryption error occurs.
     */
    public static byte[] encrypt(byte[] data, Key publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

    /**
     * Decrypts data using an RSA private key.
     *
     * @param encryptedData The data to decrypt.
     * @param privateKey The private key for decryption.
     * @return The decrypted data.
     * @throws Exception If a decryption error occurs.
     */
    public static byte[] decrypt(byte[] encryptedData, Key privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(encryptedData);
    }

    /**
     * Retrieves a PublicKey object from its byte array representation.
     *
     * @param publicKeyBytes The byte array representing the public key.
     * @return A PublicKey object.
     * @throws InvalidKeySpecException If the provided data does not represent a valid public key.
     * @throws NoSuchAlgorithmException If the RSA algorithm is not available.
     */
    public static PublicKey getPublicKey(byte[] publicKeyBytes) throws InvalidKeySpecException, NoSuchAlgorithmException {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(publicKeyBytes);
        return KeyFactory.getInstance(ALGORITHM).generatePublic(spec);
    }

    /**
     * Retrieves a PrivateKey object from its byte array representation.
     *
     * @param privateKeyBytes The byte array representing the private key.
     * @return A PrivateKey object.
     * @throws InvalidKeySpecException If the provided data does not represent a valid private key.
     * @throws NoSuchAlgorithmException If the RSA algorithm is not available.
     */
    public static PrivateKey getPrivateKey(byte[] privateKeyBytes) throws InvalidKeySpecException, NoSuchAlgorithmException {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(privateKeyBytes);
        return KeyFactory.getInstance(ALGORITHM).generatePrivate(spec);
    }

    /**
     * Writes a cryptographic key to a file.
     *
     * @param key The key to write to the file.
     * @param fileName The name of the file to write the key to.
     * @throws IOException If an I/O error occurs while writing the key to the file.
     */
    public static void writeKeyToFile(Key key, String fileName) throws IOException {
        byte[] keyBytes = key.getEncoded();
        FileOutputStream fos = new FileOutputStream(fileName);
        fos.write(keyBytes);
        fos.close();
    }

    /**
     * Converts a cryptographic key into a string representation.
     *
     * @param key The key to convert to a string.
     * @return A Base64 encoded string representation of the key.
     */
    public static String keyToString(Key key) {
        byte[] keyBytes = key.getEncoded();
        return Base64.getEncoder().encodeToString(keyBytes);
    }
}
