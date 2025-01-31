package com.veeva.vault.vapil.extension;

import com.veeva.vault.vapil.TestProperties;
import com.veeva.vault.vapil.api.client.VaultClient;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public abstract class AbstractVaultClientParameterResolver  implements ParameterResolver {

    final TestProperties testProperties = new TestProperties();
    private static final String CURRENT_JVM = "current.jvm";
    private static final String SESSION_SUFFIX = ".sessionid";
    private VaultClient vaultClient;

    public AbstractVaultClientParameterResolver() {
        try {
            String sessionId = getCurrentSessionId();
            if (sessionId != null) {
                vaultClient = VaultClient
                        .newClientBuilder(VaultClient.AuthenticationType.SESSION_ID)
                        .withVaultDNS(testProperties.getVaultDNS())
                        .withVaultSessionId(sessionId)
                        .withVaultClientId(testProperties.getVaultClientId())
                        .build();
            } else {
                vaultClient = VaultClient
                        .newClientBuilder(VaultClient.AuthenticationType.BASIC)
                        .withVaultDNS(testProperties.getVaultDNS(getVaultPropertyTag()))
                        .withVaultUsername(testProperties.getVaultUsername())
                        .withVaultPassword(testProperties.getVaultPassword())
                        .withVaultClientId(testProperties.getVaultClientId())
                        .build();


                sessionId = vaultClient.getAuthenticationResponse().getSessionId();
                writeCurrentSessionId(sessionId);
            }
        }
        catch (Exception e) {

        }
    }

    private  String getVaultSessionTag() {
        return getVaultPropertyTag()+SESSION_SUFFIX;
    }
    protected abstract String getVaultPropertyTag();

    private void writeCurrentSessionId(String sessionId) {
        Properties p2 = new Properties();
        p2.setProperty(CURRENT_JVM, TestRunHelper.getJVMId());
        p2.setProperty(getVaultSessionTag(), sessionId);
        try {
            File sessFile = TestRunHelper.getVapilSessionFile();
            if (sessFile != null) {
                if (sessFile.exists()) {
                    FileInputStream is = new FileInputStream(sessFile);
                    Properties p = new Properties();
                    p.load(is);
                    is.close();
                    p.forEach((k,v)-> {
                        if (!p2.containsKey(k)) {
                            p2.setProperty((String)k,(String)v);
                        }
                    } );
                }
                FileOutputStream fos = new FileOutputStream(sessFile);
                p2.store(fos, null);
                fos.flush();
                fos.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    private String getCurrentSessionId() {
        String sessionId = null;
        File sessFile = TestRunHelper.getVapilSessionFile();
        if (sessFile != null) {
            if (sessFile.exists()) {
                Properties properties = new Properties();
                try {
                    FileInputStream is = new FileInputStream(sessFile);
                    properties.load(is);
                    is.close();
                    if (properties.containsKey(CURRENT_JVM)
                            && TestRunHelper.getJVMId().equals(properties.getProperty(CURRENT_JVM))
                            && properties.containsKey(getVaultSessionTag())) {
                        sessionId = properties.getProperty(getVaultSessionTag());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sessionId;
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return (parameterContext.getParameter().getType() == VaultClient.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return vaultClient;
    }
}
