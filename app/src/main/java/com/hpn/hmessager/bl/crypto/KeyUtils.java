package com.hpn.hmessager.bl.crypto;

import org.bouncycastle.crypto.digests.SHA3Digest;
import org.bouncycastle.crypto.generators.HKDFBytesGenerator;
import org.bouncycastle.crypto.params.HKDFParameters;
import org.bouncycastle.jcajce.spec.RawEncodedKeySpec;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class KeyUtils {

    public static final short DH_PRIV_KEY_SIZE = 83;

    public static final short DH_PUB_KEY_SIZE = 32;

    public static final short SIGN_PRIV_KEY_SIZE = 83;

    public static final short SIGN_PUB_KEY_SIZE = 32;

    private static final String DH_ALGORITHM = "X25519";

    private static final String SIGNING_ALGORITHM = "Ed25519";

    private static final String PBKDF_ALGORITHM = "PBKDF2WithHmacSHA3-512";

    private static final String AES_ALGORITHM = "AES/CBC/PKCS5Padding";

    private static final String HMAC_ALGORITHM = "HMacSHA3-256";


    /*
     * X25519 constants
     */
    private static final short DH_MS = 32;
    private static KeyAgreement xecdh;
    private static KeyPairGenerator xecdhGen;
    /*
     * AES constants
     */
    private static Cipher AES_CIPHER;
    /*
     * Ed25519 constants
     */
    private static Signature eddsaSign;

    /*
     * Digest constants
     */
    private static MessageDigest sha512;
    private static MessageDigest sha256;

    private static HKDFBytesGenerator hkdf;

    /*
     *************************************************************************************
     ****************************                             ****************************
     **************************** Diffie-Hellman key exchange ****************************
     ****************************                             ****************************
     *************************************************************************************
     */

    static {
        Security.removeProvider("BC");
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Generate a X25519 key pair
     *
     * @return A X25519 key pair
     * @throws GeneralSecurityException If the key pair generator is not available
     */
    public static X25519KeyPair generateX25519KeyPair() throws GeneralSecurityException {
        if (xecdhGen == null) xecdhGen = KeyPairGenerator.getInstance(DH_ALGORITHM, "BC");

        KeyPair pair = xecdhGen.generateKeyPair();

        return new X25519KeyPair(pair.getPublic(), pair.getPrivate());
    }

    public static byte[] diffieHellman(PrivateKey pkey, byte[] publicKey, boolean raw) throws GeneralSecurityException {
        return diffieHellman(pkey, getPubKeyFromX25519(publicKey, raw));
    }

    public static byte[] diffieHellman(PrivateKey pkey, Key publicKey) throws GeneralSecurityException {
        if (xecdh == null) xecdh = KeyAgreement.getInstance(DH_ALGORITHM, "BC");

        xecdh.init(pkey);

        xecdh.doPhase(publicKey, true);

        return xecdh.generateSecret();
    }

    public static byte[] doX3DH(PrivateKey idK, PrivateKey eKey, byte[] idKRemote, byte[] eKRemote, boolean ignite) throws GeneralSecurityException {
        byte[] ms = new byte[3 * DH_MS], dh;

        Key idKRemoteKey = getPubKeyFromX25519(idKRemote, true);
        Key eKRemoteKey = getPubKeyFromX25519(eKRemote, true);

        if(ignite) {
            dh = KeyUtils.diffieHellman(idK, eKRemoteKey);
            System.arraycopy(dh, 0, ms, 0, DH_MS);

            dh = KeyUtils.diffieHellman(eKey, idKRemoteKey);
            System.arraycopy(dh, 0, ms, DH_MS, DH_MS);
        } else {
            dh = KeyUtils.diffieHellman(eKey, idKRemoteKey);
            System.arraycopy(dh, 0, ms, 0, DH_MS);

            dh = KeyUtils.diffieHellman(idK, eKRemoteKey);
            System.arraycopy(dh, 0, ms, DH_MS, DH_MS);
        }

        dh = KeyUtils.diffieHellman(eKey, eKRemoteKey);
        System.arraycopy(dh, 0, ms, 2 * DH_MS, DH_MS);

        return KeyUtils.sha256(ms);
    }

    /*
     ****************************************************************************************
     ****************************                                ****************************
     **************************** Key reconstruction (Ed and Ec) ****************************
     ****************************                                ****************************
     ****************************************************************************************
     */
    public static PublicKey getPubKeyFromX25519(byte[] key, boolean raw) throws GeneralSecurityException {
        KeySpec pubKeySpec = raw ? new RawEncodedKeySpec(key) : new X509EncodedKeySpec(key);

        return KeyFactory.getInstance(DH_ALGORITHM, "BC").generatePublic(pubKeySpec);
    }

    public static PrivateKey getPrivKeyFromX25519(byte[] key) throws GeneralSecurityException {
        KeySpec privKeySpec = new PKCS8EncodedKeySpec(key);

        return KeyFactory.getInstance(DH_ALGORITHM, "BC").generatePrivate(privKeySpec);
    }

    /**
     * Retrieve and construct ad Ed25519 public key from a byte array
     *
     * @param key The byte array containing: first byte if X is odd or not, last 32 the public key
     * @param raw If the key is raw (Xodd + Y pos) or not (X509 encoded)
     * @return The Ed25519 public key
     */
    public static PublicKey getPubKeyFromEd25519(byte[] key, boolean raw) throws GeneralSecurityException {
        KeySpec pubKeySpec = raw ? new RawEncodedKeySpec(key) : new X509EncodedKeySpec(key);

        return KeyFactory.getInstance(SIGNING_ALGORITHM, "BC").generatePublic(pubKeySpec);
    }

    public static PrivateKey getPrivKeyFromEd25519(byte[] key) throws GeneralSecurityException {
        return KeyFactory.getInstance(SIGNING_ALGORITHM, "BC").generatePrivate(new PKCS8EncodedKeySpec(key));
    }

    /*
     ********************************************************************************************
     ****************************                                    ****************************
     **************************** Key derivation (PBKDF and Ratchet) ****************************
     ****************************                                    ****************************
     ********************************************************************************************
     */

    /**
     * Derive a root key (or a master secret for X3DH) from a DH master secret and an info
     *
     * @param rootKey  The previous root key or the master key (from the X3DH protocol)
     * @param dhOutput A master secret from a DH exchange
     * @param info     An application-specific info
     * @return The derived root key
     */
    public static byte[] deriveRootKey(byte[] rootKey, byte[] dhOutput, byte[] info) {
        return HKDF(rootKey, dhOutput, info, 32);
    }


    /**
     * Derive a chain key and a message key from a chain key.
     *
     * @param chainKey The previous chain key or the root key
     * @return The derived chain key and message key
     */
    public static ChainMessageK deriveCKandMK(byte[] chainKey) throws GeneralSecurityException {
        SecretKeySpec secretKeySpec = new SecretKeySpec(chainKey, HMAC_ALGORITHM);
        Mac mac = Mac.getInstance(HMAC_ALGORITHM, "BC");

        mac.init(secretKeySpec);

        byte[] mk = mac.doFinal(new byte[]{0x01});
        byte[] ck = mac.doFinal(new byte[]{0x02});

        return new ChainMessageK(ck, mk);
    }

    public static byte[] PBKDF(String password, String info) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF_ALGORITHM);
        KeySpec spec = new PBEKeySpec(password.toCharArray(), info.getBytes(), 65536, 256);

        return factory.generateSecret(spec).getEncoded();
    }

    public static byte[] HKDF(byte[] salt, byte[] inputKeyMat, byte[] info, int outLength) {
        if(hkdf == null)
            hkdf = new HKDFBytesGenerator(new SHA3Digest());
        byte[] derivedKey = new byte[outLength];

        // Input key mat, salt, info
        hkdf.init(new HKDFParameters(inputKeyMat, salt, info));
        hkdf.generateBytes(derivedKey, 0, outLength);

        return derivedKey;
    }


    /*
     ***********************************************************************************
     ****************************                           ****************************
     **************************** AES encryption/decryption ****************************
     ****************************                           ****************************
     ***********************************************************************************
     */
    public static byte[] encrypt(byte[] mk, byte[] plainText) throws GeneralSecurityException {
        return doAES(mk, plainText, Cipher.ENCRYPT_MODE);
    }

    public static byte[] decrypt(byte[] mk, byte[] cipheredText) throws GeneralSecurityException {
        return doAES(mk, cipheredText, Cipher.DECRYPT_MODE);
    }

    private static byte[] doAES(byte[] mk, byte[] input, int mode) throws GeneralSecurityException {
        if (AES_CIPHER == null) AES_CIPHER = Cipher.getInstance(AES_ALGORITHM);

        byte[] derived = HKDF(new byte[64], mk, "hmessager".getBytes(), 48);
        byte[] encryptionKey = new byte[32];
        byte[] IV = new byte[16];

        System.arraycopy(derived, 0, encryptionKey, 0, 32);
        System.arraycopy(derived, 32, IV, 0, 16);

        AES_CIPHER.init(mode,
                new SecretKeySpec(encryptionKey, "AES"),
                new IvParameterSpec(IV));

        return AES_CIPHER.doFinal(input);
    }

    /*
     ************************************************************************************
     ****************************                            ****************************
     **************************** Digest (SHA512 and SHA256) ****************************
     ****************************                            ****************************
     ************************************************************************************
     */
    public static byte[] sha256(byte[] input) throws GeneralSecurityException {
        if (sha256 == null) sha256 = MessageDigest.getInstance("SHA3-256", "BC");

        return sha256.digest(input);
    }

    public static byte[] sha512(byte[] input) throws GeneralSecurityException {
        if (sha512 == null) sha512 = MessageDigest.getInstance("SHA3-512", "BC");

        return sha512.digest(input);
    }

    /*
     ***************************************************************************************
     ****************************                               ****************************
     **************************** Ed25519 Signing and verifying ****************************
     ****************************                               ****************************
     ***************************************************************************************
     */
    public static SigningKeyPair generateSigningKeyPair() throws GeneralSecurityException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(SIGNING_ALGORITHM, "BC");

        KeyPair signingPair = keyPairGenerator.generateKeyPair();

        return new SigningKeyPair(signingPair.getPublic(), signingPair.getPrivate());
    }

    public static byte[] sign(byte[] message, PrivateKey privateKey) throws GeneralSecurityException {
        if (eddsaSign == null) eddsaSign = Signature.getInstance(SIGNING_ALGORITHM, "BC");

        byte[] hashed = sha512(message);

        eddsaSign.initSign(privateKey);
        eddsaSign.update(hashed);

        return eddsaSign.sign();
    }

    public static boolean verify(byte[] message, byte[] signature, PublicKey publicKey) throws GeneralSecurityException {
        if (eddsaSign == null) eddsaSign = Signature.getInstance(SIGNING_ALGORITHM, "BC");

        byte[] hashed = sha512(message);

        eddsaSign.initVerify(publicKey);
        eddsaSign.update(hashed);

        return eddsaSign.verify(signature);
    }

    public static class ChainMessageK {

        private final byte[] chainKey;

        private final byte[] messageKey;

        public ChainMessageK(byte[] chainKey, byte[] messageKey) {
            this.chainKey = chainKey;
            this.messageKey = messageKey;
        }

        public byte[] getChainKey() {
            return chainKey;
        }

        public byte[] getMessageKey() {
            return messageKey;
        }
    }
}
