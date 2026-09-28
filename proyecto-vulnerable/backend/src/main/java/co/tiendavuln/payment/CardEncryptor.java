package co.tiendavuln.payment;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class CardEncryptor {

    private static final byte[] KEY = "tienda12".getBytes(StandardCharsets.UTF_8);

    public String encrypt(String cardNumber) throws Exception {
        Cipher cipher = Cipher.getInstance("DES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY, "DES"));
        return Base64.getEncoder().encodeToString(cipher.doFinal(cardNumber.getBytes(StandardCharsets.UTF_8)));
    }
}
