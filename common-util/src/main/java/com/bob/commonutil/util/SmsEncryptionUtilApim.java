package com.bob.commonutil.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.*;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import jakarta.annotation.PostConstruct;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource.PSpecified;
import javax.crypto.spec.SecretKeySpec;

@Component
@Slf4j
public class SmsEncryptionUtilApim {

	private static final int GCM_TAG_LENGTH = 16;
	private static final String TRANSFORMATION = "AES/GCM/NoPadding";
	private static final String ALGO_AES = "AES";
	private static final String HEADER_ENC_ALGO = "RSA/ECB/OAEPPadding";
	private static final String SIGNATURE = "SHA256withRSA";

    @Value("${sms.certificate.bank.public.key}")
    private Resource bankPublicKeyFile;

    @Value("${sms.certificate.private.key}")
    private Resource privateKeyFile;

    private String bankPublicKey;
    private String privateKey;

	@PostConstruct
	public void init() throws IOException {
		bankPublicKey = new String(FileCopyUtils.copyToByteArray(bankPublicKeyFile.getInputStream()));
		privateKey = new String(FileCopyUtils.copyToByteArray(privateKeyFile.getInputStream()));
	}

    public String encryptPayload(String plainText, byte[] key, byte[] iv)
            throws Exception {

        SecretKeySpec secretKey = new SecretKeySpec(key, ALGO_AES);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);
        byte[] encrytedBytes = cipher.doFinal(plainText.getBytes());

        return Base64.getEncoder().encodeToString(encrytedBytes);
    }

	public String encryptSymKey(String data)
			throws Exception {

		PublicKey publicKey = extractPublicKey(bankPublicKey);

		Cipher cipher = Cipher.getInstance(HEADER_ENC_ALGO);
		OAEPParameterSpec oaepParam = new OAEPParameterSpec("SHA-256", "MGF1", new MGF1ParameterSpec("SHA-256"),
				PSpecified.DEFAULT);
		cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParam);
		byte[] plainText = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
		return Base64.getEncoder().encodeToString(plainText);
	}

	public byte[] getIVFromAESKey(byte[] encoded) {
		return Arrays.copyOfRange(encoded, 0, 12);
	}
	
	
	public String genSign(String input)
			throws Exception {

		// Clean the PEM key format and decode to bytes
		byte[] keyBytes = Base64.getDecoder().decode(privateKey.replaceAll("-----BEGIN PRIVATE KEY-----", "").replaceAll("-----END PRIVATE KEY-----", "").replaceAll("\\s", ""));
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
		PrivateKey privateKeyObj = keyFactory.generatePrivate(spec);

		Signature privateSignature = Signature.getInstance(SIGNATURE);
		privateSignature.initSign(privateKeyObj);
		byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
		privateSignature.update(bytes);
		byte[] s = privateSignature.sign();
		return Base64.getEncoder().encodeToString(s);
	}

    /**
     * Extracts only the certificate content between BEGIN and END markers.
     *
     * @param fullCertContent The full certificate content (may include bag attributes)
     * @return Clean certificate content with markers
     */
    public static String extractCertificateContent(String fullCertContent) {
        // Extract only the certificate content between BEGIN and END markers
        int beginIndex = fullCertContent.indexOf("-----BEGIN CERTIFICATE-----");
        int endIndex = fullCertContent.indexOf("-----END CERTIFICATE-----");

        if (beginIndex != -1 && endIndex != -1) {
            // Return only the certificate content including the markers
            return fullCertContent.substring(beginIndex, endIndex + "-----END CERTIFICATE-----".length());
        }

        // If no markers found, return the original content
        return fullCertContent;
    }

    /**
     * Extracts PublicKey from certificate content.
     * Handles both PEM public key format and X.509 certificate format.
     *
     * @param certificateContent The certificate or public key content
     * @return PublicKey extracted from the certificate
     * @throws Exception if certificate processing fails
     */
    public static PublicKey extractPublicKey(String certificateContent) throws Exception {
        PublicKey publicKey;

        // Check if the key is in PEM public key format or certificate format
        if (certificateContent.contains("-----BEGIN PUBLIC KEY-----")) {
            // Handle PEM public key format
            log.debug("Processing PEM public key format");
            byte[] keyBytes = Base64.getDecoder()
                    .decode(certificateContent
                            .replaceAll("-----BEGIN PUBLIC KEY-----", "")
                            .replaceAll("-----END PUBLIC KEY-----", "")
                            .replaceAll("\\s", ""));
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            publicKey = keyFactory.generatePublic(spec);
        } else {
            // Handle certificate format (CER with bag attributes or clean certificate)
            log.debug("Processing X.509 certificate format");
            String certContent = extractCertificateContent(certificateContent);
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            InputStream certInputStream = new ByteArrayInputStream(certContent.getBytes());
            X509Certificate certificate = (X509Certificate) certFactory.generateCertificate(certInputStream);
            publicKey = certificate.getPublicKey();
        }

        return publicKey;
    }

    public static byte[] hexToByte(String hexPwd) {
        byte[] hexChars = new byte[hexPwd.length() / 2];
        for (int j = 0; j < hexChars.length; j++) {
            int index = j * 2;
            int v = Integer.parseInt(hexPwd.substring(index, index + 2), 16);
            hexChars[j] = (byte) v;
        }
        return hexChars;
    }

    private static void byte2hex(byte b, StringBuffer buf) {
        char[] hexChars = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };
        int high = ((b & 0xf0) >> 4);
        int low = (b & 0x0f);
        buf.append(hexChars[high]);
        buf.append(hexChars[low]);
    }

}
