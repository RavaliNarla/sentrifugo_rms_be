package com.bob.commonutil.util;

import com.bob.commonutil.exception.CommonException;
import com.bob.commonutil.exception.InvalidCredentialsException;
import com.bob.commonutil.model.CredModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

@Component
@Slf4j
public class RSADecryptionUtil {

    private static PrivateKey privateKey;

    static {
        try {
            loadPrivateKey();
        } catch (Exception e) {
            throw new CommonException("Failed to load RSA Private Key");
        }
    }

    public CredModel decryptToken(String credentials){

            String decrypted = RSADecryptionUtil.decryptCredentials(credentials);

            String[] parts = decrypted.split("\\|", 2);
            if (parts.length != 2) {
                throw new InvalidCredentialsException("Decrypted credentials format is invalid");
            }
            return  CredModel.builder().email(parts[0])
                    .password(parts[1])
                    .build();

    }

    /**
     * Decrypts the encrypted credentials string and returns "email|hashedPassword"
     */
    public static String decryptCredentials(String encryptedCredentials) {
        if (encryptedCredentials == null || encryptedCredentials.trim().isEmpty()) {
            throw new IllegalArgumentException("Credentials cannot be null or empty");
        }
        return decrypt(encryptedCredentials);
    }

    /**
     * Decrypt Base64 RSA encrypted text
     */
    private static String decrypt(String encryptedText) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
            OAEPParameterSpec oaepParameterSpec = new OAEPParameterSpec("SHA-256","MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT);
            cipher.init(Cipher.DECRYPT_MODE, privateKey,oaepParameterSpec);


            byte[] decodedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);

            return new String(decryptedBytes);

        }
        catch (Exception e) {
            log.info("RSA Decryption error: {}",e.getMessage());
            throw new CommonException("Something went wrong.Please try again.");
        }
    }

    /**
     * Loads RSA private key from PEM file (must be PKCS#8 format)
     */
    private static void loadPrivateKey() throws Exception {
        // Use the class loader to get the resource as an InputStream (works in static context)
//        try (InputStream is = RSADecryptionUtil.class.getClassLoader().getResourceAsStream("encryption-keys/private_key.pem")) {
//            if (is == null) {
//                throw new IllegalStateException("Private key resource not found: encryption-keys/private_key.pem");
//            }
//
//            String keyContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
//
//            keyContent = keyContent
//                    .replace("-----BEGIN PRIVATE KEY-----", "")
//                    .replace("-----END PRIVATE KEY-----", "")
//                    .replaceAll("\\s+", "");

            String keyContent = "MIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQDQKUvg1WSMEDsrSqnX5NkfVwjhQaFny1RSk6F4tlfASYQXnzhPG9ZA5EGtGRrP2xpeGvUwMoTQp0RLVjZsQZo3Tp2fphBV5skiLfQYrIMR0t6xvZ3yf00D3FVPvmj6dbHIPxxHdJK1r3qMc5MrvpRT7ExJ5Jq3XAKiDrkhD5GwvbKz9zWBQEX9xQ0o4459XSMXUAk6XygsPwIYeN6HvAf/LCAgwzXeYP/xhRX/xynZpzIIZgDEc4H7EgOITUVto47CRX+hlP2yhZaEfno8MgwgPvpadf6+v0gC5dkYYjkDcmBDoFLm1mrMg50zS2hQJZM1ZLmOv+uw5ENHqbaP9atBAgMBAAECggEAApnXfr9P1q+vu5Re36mWfG2jGORv6rr3ymHZmUdm/Io2njBxPMzPXidz+iCXjQwS7eTWljP1ZKGIaQwSWUMvFPyUm91wdBBSiEyscX/7UcZRncUto21rhg4zWzAZAfMlRegi93IZkK9Y/8cEal4i3pMT53L1q2+ZAVMfhU4bXwrT+B2Wj+uNO2TiBDOpaDWvP2/2JQEgGog3g11cPGUGzJM8FErKKK5eFWQahUN4clXVPOOD+WZbJUHPm9hYc42Psit6GNunzeSI2l7CqT7FpzHbQrbB/TeX8uZXxdLCb1DlE2N5M1LwleHpF+AxFWhE1GfhX8D4NPrlN5GzSau8AQKBgQD2oqr6RJfpNW/Li0JgeBvsg0NZ3Otz6xSsLgALY/tiaLh1SwKuqyM6x+jHV7OspJHfaEl5GxV41uDzDZ89ELdDSKXLt6NZJ7OlhUkQC5WLCBC4J736J+AUZBnAQYuuYTjCUrFxiQf3ABvHJQ2qBFivMKWw9lhp3Wg/yCdken4rgQKBgQDYEKeUSwq2mctWxQDtCPBBwW6MwKovJQvXMxRS4lUBJ520wJzdfldIlEYq+xSigmjPQQiamhAPl8xI+e6dNCkjvRi42aXH2ozuVI8Cyalkie8LWYR9HR44vdnqcNWZ47vl1ZAqn3Pl2DVa3tQg0eJPO6nODbnvVqMls9I9pbJfwQKBgCQZ8X8KtVXMnZ2kt8WZLzUyjZRE0y43leISJa2FKBHilEWPAkiBAnojMv6i5sj5meSixkZ04XK7uVe6gbmVjc3Kf4JPUhjFx/UNioFPwMcGGjRqlNNeLN2vPVm3/nM7Bezj39dnoIPWPzqqQNLHKhgSvd4mYRC1+QGFEccSaw+BAoGAcHSKpBZCpzN4r9qtVrG2SqMgpMWKTitb9E+znkgKsiUqYe92NZoSV8ZElw5DeSmi32wbzEuvXE5HlxvuY4fIeb9r5JnzPQe1Hj6IiIzpS1i3QBMBcBT9q860Yt8DbasoGsGC3YxMSC615mvfwI082rxU7tgaFEXleDQVktxw8IECgYBtqrfRQ5f0wrEmlBJDNOXFedF+lwlv4pBpQvp5xv3OIm3ju18rfMi9IIUpUSTKMPwPfo1dmdR7pH4TjuaEKnBGV/j2mQdoEu9UVJXmDvLMwjOSbstv6CXtjMFA/8BkQaaBVT88bjK9YANYlhqzD2uVWxrCoIbhUVx/t7atZ63n7g==";
            byte[] keyBytes = Base64.getDecoder().decode(keyContent);

            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            privateKey = KeyFactory.getInstance("RSA").generatePrivate(spec);

    }

}
