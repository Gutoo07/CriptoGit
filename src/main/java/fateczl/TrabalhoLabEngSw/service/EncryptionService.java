package fateczl.TrabalhoLabEngSw.service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

import org.springframework.stereotype.Service;

@Service
public class EncryptionService {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final int KEY_LENGTH = 256;
    
    // Chave fixa para o sistema (em produção, deve vir de configuração segura)
    private static final String SECRET_KEY_STRING = "MySecretKey12345MySecretKey12345"; // 32 caracteres para AES-256
    
    /**
     * Criptografa os dados usando AES-256
     * @param data Dados a serem criptografados
     * @return Array de bytes criptografados
     */
    public byte[] encrypt(byte[] data) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY_STRING.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        
        // Gerar IV aleatório para cada criptografia
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        byte[] encryptedData = cipher.doFinal(data);
        
        // Concatenar IV + dados criptografados
        byte[] result = new byte[iv.length + encryptedData.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encryptedData, 0, result, iv.length, encryptedData.length);
        
        return result;
    }
    
    /**
     * Descriptografa os dados usando AES-256
     * @param encryptedData Dados criptografados (IV + dados)
     * @return Array de bytes descriptografados
     */
    public byte[] decrypt(byte[] encryptedData) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY_STRING.getBytes(), ALGORITHM);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        
        // Separar IV dos dados criptografados
        byte[] iv = new byte[16];
        byte[] data = new byte[encryptedData.length - 16];
        System.arraycopy(encryptedData, 0, iv, 0, 16);
        System.arraycopy(encryptedData, 16, data, 0, data.length);
        
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
        
        return cipher.doFinal(data);
    }
    
    /**
     * Gera uma chave AES-256 aleatória (para uso futuro)
     * @return Chave secreta
     */
    public SecretKey generateKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
        keyGenerator.init(KEY_LENGTH);
        return keyGenerator.generateKey();
    }
}
