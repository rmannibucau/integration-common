/*
 * Copyright (C) 2017 Black Duck Software Inc.
 * http://www.blackducksoftware.com/
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Black Duck Software ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Black Duck Software.
 */
package com.blackducksoftware.integration.certificate;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.log.IntLogger;
import com.blackducksoftware.integration.log.LogLevel;
import com.blackducksoftware.integration.log.PrintStreamIntLogger;

public class CertificateHandlerTest {

    private static final IntLogger logger = new PrintStreamIntLogger(System.out, LogLevel.TRACE);

    private static final CertificateHandler CERT_HANDLER = new CertificateHandler(logger);

    private static URL url;

    private static File originalCertificate;

    private File temporaryCertificateFile;

    @BeforeClass
    public static void init() throws Exception {
        final String urlString = System.getProperty("HTTPS_URL");
        // assumeTrue expects the condition to be true, if it is not then it skips the test
        Assume.assumeTrue(StringUtils.isNotBlank(urlString));
        url = new URL(urlString);
        try {
            final boolean isCertificateInKeystore = CERT_HANDLER.isCertificateInKeystore(url, null);
            if (isCertificateInKeystore) {
                originalCertificate = File.createTempFile("originalCertificate", ".tmp");
                exportHttpsCertificateFromKeystore(url, originalCertificate, null);
                CERT_HANDLER.removeHttpsCertificate(url, null);
            } else {
                logger.error(String.format("Certificate for %s is not in the keystore.", url.getHost()));
            }
        } catch (final IntegrationException e) {
            logger.error(e.getMessage());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (originalCertificate != null && originalCertificate.exists()) {
            CERT_HANDLER.importHttpsCertificateFromFile(url, originalCertificate, null);
            originalCertificate.delete();
        }
    }

    @Before
    public void testSetup() throws IOException {
        if (temporaryCertificateFile != null && temporaryCertificateFile.exists()) {
            temporaryCertificateFile.delete();
        }
        temporaryCertificateFile = File.createTempFile("certificate", ".tmp");
    }

    @After
    public void cleanUp() {
        if (temporaryCertificateFile != null && temporaryCertificateFile.exists()) {
            temporaryCertificateFile.delete();
        }
    }

    @Test
    public void testCertificateRetrieval() throws Exception {
        final CertificateHandler certificateHandler = new CertificateHandler(logger);
        final String output = certificateHandler.retrieveHttpsCertificateFromURL(url);
        assertTrue(output.contains("BEGIN CERTIFICATE"));
    }

    @Test
    public void testCertificateRetrievalAndSaveToFile() throws Exception {
        final CertificateHandler certificateHandler = new CertificateHandler(logger);
        final File outputFile = certificateHandler.retrieveAndSaveHttpsCertificate(url, temporaryCertificateFile);
        final String output = readFile(outputFile);
        assertTrue(output.contains("BEGIN CERTIFICATE"));
    }

    @Test
    public void testCertificateRetrievalAndImport() throws Exception {
        final CertificateHandler certificateHandler = new CertificateHandler(logger);
        final File outputFile = certificateHandler.retrieveAndSaveHttpsCertificate(url, temporaryCertificateFile);
        final String output = readFile(outputFile);
        assertTrue(output.contains("BEGIN CERTIFICATE"));
        certificateHandler.importHttpsCertificateFromFile(url, outputFile, null);
        assertTrue(certificateHandler.isCertificateInKeystore(url, null));
        certificateHandler.removeHttpsCertificate(url, null);
        assertFalse(certificateHandler.isCertificateInKeystore(url, null));
    }

    @Test
    public void testRetrieveAndImportHttpsCertificate() throws Exception {
        final CertificateHandler certificateHandler = new CertificateHandler(logger);
        certificateHandler.retrieveAndImportHttpsCertificate(url, null);
        assertTrue(certificateHandler.isCertificateInKeystore(url, null));
        certificateHandler.removeHttpsCertificate(url, null);
        assertFalse(certificateHandler.isCertificateInKeystore(url, null));
    }

    private String readFile(final File file) throws IOException {
        final StringBuilder stringBuilder = new StringBuilder();
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }
        return stringBuilder.toString();
    }

    private static void exportHttpsCertificateFromKeystore(final URL url, final File certificateFile, String optionalKeyStorePass) throws IntegrationException {
        final String javaHome = System.getProperty("java.home");
        File jssecacerts = new File(javaHome);
        jssecacerts = new File(jssecacerts, "lib");
        jssecacerts = new File(jssecacerts, "security");
        jssecacerts = new File(jssecacerts, "jssecacerts");
        final String keyStore = jssecacerts.getAbsolutePath();
        logger.info(String.format("Removing the certificate from %s", keyStore));
        if (StringUtils.isBlank(optionalKeyStorePass)) {
            optionalKeyStorePass = "changeit";
        }
        final String[] command = { "keytool", "-export", "-keystore", keyStore, "-storepass", optionalKeyStorePass, "-alias", url.getHost(), "-noprompt",
                "-file", certificateFile.getAbsolutePath() };
        try {
            final ProcessBuilder processBuilder = new ProcessBuilder(command);
            final Process proc = processBuilder.start();
            final int exitCode = proc.waitFor();
            final String output = readInputStream(proc.getInputStream());
            final String errorOutput = readInputStream(proc.getErrorStream());
            // destroy() will cleanup the process resources, including the streams
            proc.destroy();
            if (StringUtils.isNotBlank(output)) {
                if (exitCode != 0) {
                    logger.error(output);
                } else {
                    logger.info(output);
                }
            }
            if (StringUtils.isNotBlank(errorOutput)) {
                logger.warn(errorOutput);
            }
            logger.debug(String.format("Exit code %d", exitCode));
            if (proc.exitValue() != 0) {
                throw new IntegrationException(String.format("Failed to find the certificate in %s", keyStore));
            }
        } catch (final Exception e) {
            throw new IntegrationException(e);
        }
    }

    private static String readInputStream(final InputStream stream) throws IOException {
        return IOUtils.toString(stream, StandardCharsets.UTF_8);
    }
}
