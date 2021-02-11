package com.payline.payment.equens.utils.security;

import com.payline.payment.equens.MockUtils;
import com.payline.payment.equens.utils.constant.PartnerConfigurationKeys;
import com.payline.pmapi.bean.configuration.PartnerConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

import static org.junit.jupiter.api.Assertions.*;

class RSAHolderTest {

    private static String pemCertificate;
    private static String pemPk;

    private static RSAHolder.RSAHolderBuilder builder;

    @BeforeEach
    void setup() {
        builder = new RSAHolder.RSAHolderBuilder();
        PartnerConfiguration partnerConfiguration = MockUtils.aPartnerConfiguration();
        pemCertificate = partnerConfiguration.getProperty(PartnerConfigurationKeys.CLIENT_CERTIFICATE);
        pemPk = partnerConfiguration.getProperty(PartnerConfigurationKeys.CLIENT_PRIVATE_KEY);
    }

    @Test
    void builder_uniqueClientCertificate() throws CertificateException, InvalidKeySpecException, NoSuchAlgorithmException, IOException, KeyStoreException, UnrecoverableKeyException {
        // given: a valid chain containing only one certificate and private key
        String chain = pemCertificate;
        String pk = pemPk;

        // when: building a RSAHolder
        RSAHolder rsaHolder = builder
                .parseChain(chain)
                .parsePrivateKey(pk)
                .build();

        // then: the keystore contains the corresponding entries
        assertNotNull(rsaHolder.getKeyStore());
        assertEquals(2, rsaHolder.getKeyStore().size());
        assertNotNull(rsaHolder.getPrivateKey());
        assertNotNull(rsaHolder.getClientCertificate());
        assertNotNull(rsaHolder.getPrivateKeyPassword());
    }

    @Test
    void builder_multipleCertChain() throws CertificateException, InvalidKeySpecException, NoSuchAlgorithmException, IOException, KeyStoreException {
        // given: a valid chain containing two certificates and private key
        String chain = pemCertificate + System.lineSeparator() + pemCertificate;
        String pk = pemPk;

        // when: building a RSAHolder
        RSAHolder rsaHolder = builder
                .parseChain(chain)
                .parsePrivateKey(pk)
                .build();

        // then: the keystore contains 2 certificates and 1 private key, so 3 entries
        assertNotNull(rsaHolder.getKeyStore());
        assertEquals(3, rsaHolder.getKeyStore().size());
    }

    @Test
    void builder_nullChain() {
        // given: a null certificate
        String chain = null;

        // when: trying to parse a null chain, then an exception is thrown
        assertThrows(IllegalStateException.class, () -> builder.parseChain(chain));
    }

    @Test
    void builder_nullPk() {
        // given: a null private key
        String pk = null;

        // when: trying to parse a null chain, then an exception is thrown
        assertThrows(IllegalStateException.class, () -> builder.parsePrivateKey(pk));
    }

    @Test
    void builder_buildWithoutChain() throws Exception {
        // given: a valid chain containing only one certificate and private key
        String chain = pemCertificate;
        String pk = pemPk;

        // when: building a RSAHolder, omitting the chain, then an exception is thrown
        builder = builder.parsePrivateKey(pk);
        assertThrows(IllegalStateException.class, () -> builder.build());
    }

    @Test
    void builder_buildWithoutPk() throws Exception {
        // given: a valid chain containing only one certificate and private key
        String chain = pemCertificate;
        String pk = pemPk;

        // when: building a RSAHolder, omitting the chain, then an exception is thrown
        builder = builder.parseChain(chain);
        assertThrows(IllegalStateException.class, () -> builder.build());
    }
}