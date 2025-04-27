package dev.group.cybershield.common.utils;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
public class CryptographicUtil {
    public static final String symmetricKey = "5/CnXPJP/8ndR8D7OoV8Bc8xlqymfX31vRxrm1zeavQ=";
    public static final String symmetricAlgoWithPadding = "AES/ECB/PKCS5Padding";
    public static final String symmetricAlgo = "AES";

    public static String symmetricEncrypt(String data) {
        String returnData = data;
        try {
            Cipher cp = Cipher.getInstance(symmetricAlgoWithPadding);
            byte[] decodedKey = Base64.getDecoder().decode(symmetricKey);
            SecretKey secretKey = new SecretKeySpec(decodedKey, symmetricAlgo);
            cp.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedData = cp.doFinal(data.getBytes());
            returnData = Base64.getEncoder().encodeToString(encryptedData);
        } catch (Exception e) {
            log.error("symmetricEncrypt_error : " + e.getMessage() + "\n stacktrace : ");
            e.printStackTrace();
        }
        return returnData;
    }

    public static String symmetricDecrypt(String data) {
        String returnData = null;
        try {
            Cipher cp = Cipher.getInstance(symmetricAlgoWithPadding);
            byte[] decodedKey = Base64.getDecoder().decode(symmetricKey);
            SecretKey secretKey = new SecretKeySpec(decodedKey, symmetricAlgo);
            cp.init(Cipher.DECRYPT_MODE, secretKey);

            byte[] decodedData = Base64.getDecoder().decode(data);
            byte[] decryptedData = cp.doFinal(decodedData);

            // Convert decrypted byte array to a string
            returnData = new String(decryptedData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("symmetricDecrypt_error: " + e.getMessage(), e);
        }
        return returnData;
    }


    public static void main(String[] args) {
        String encryptedData = symmetricEncrypt("helloMyNameIsCyberShield");
        System.out.println("encryptedData: " + encryptedData);

        String decrptedData = symmetricDecrypt(encryptedData);
        System.out.println("decrptedData: " + decrptedData);
    }
}
