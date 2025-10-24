package kellinwood.security.zipsigner.optional;


import java.security.KeyStore;
import pro.sketchware.utility.TranslationFunction;

public class JksKeyStore extends KeyStore {

    public JksKeyStore() {
        super(new JKS(), KeyStoreFileManager.getProvider(), "jks");
    }

}
