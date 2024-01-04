/*
    This file is part of the iText (R) project.
    Copyright (c) 1998-2024 Apryse Group NV
    Authors: Apryse Software.

    This program is offered under a commercial and under the AGPL license.
    For commercial licensing, contact us at https://itextpdf.com/sales.  For AGPL licensing, see below.

    AGPL licensing:
    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.itextpdf.signatures.sign;

import com.itextpdf.bouncycastleconnector.BouncyCastleFactoryCreator;
import com.itextpdf.commons.bouncycastle.IBouncyCastleFactory;
import com.itextpdf.commons.utils.FileUtil;
import com.itextpdf.commons.utils.MessageFormatUtil;
import com.itextpdf.forms.form.element.SignatureFieldAppearance;
import com.itextpdf.kernel.exceptions.PdfException;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.signatures.DigestAlgorithms;
import com.itextpdf.signatures.PadesTwoPhaseSigningHelper;
import com.itextpdf.signatures.PrivateKeySignature;
import com.itextpdf.signatures.SignerProperties;
import com.itextpdf.signatures.cms.CMSContainer;
import com.itextpdf.signatures.exceptions.SignExceptionMessageConstant;
import com.itextpdf.signatures.testutils.PemFileHelper;
import com.itextpdf.signatures.testutils.client.TestCrlClient;
import com.itextpdf.signatures.testutils.client.TestOcspClient;
import com.itextpdf.signatures.testutils.client.TestTsaClient;
import com.itextpdf.test.ExtendedITextTest;
import com.itextpdf.test.annotations.type.BouncyCastleIntegrationTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(BouncyCastleIntegrationTest.class)
public class PadesTwoPhaseSigningTest extends ExtendedITextTest {
    private static final IBouncyCastleFactory FACTORY = BouncyCastleFactoryCreator.getFactory();
    private static final String sourceFolder = "./src/test/resources/com/itextpdf/signatures/sign/PadesTwoPhaseSigningTest/";
    private static final String destinationFolder = "./target/test/com/itextpdf/signatures/sign/PadesTwoPhaseSigningTest/";
    private static final String certsSrc = "./src/test/resources/com/itextpdf/signatures/certs/";
    private static final char[] PASSWORD = "testpassphrase".toCharArray();

    @BeforeClass
    public static void before() {
        Security.addProvider(FACTORY.getProvider());
        createOrClearDestinationFolder(destinationFolder);
    }

    @Test
    public void differentDigestAlgorithmsTest() throws Exception {
        String fileName = "differentDigestAlgorithmsTest.pdf";
        String outFileName = destinationFolder + fileName;
        String srcFileName = sourceFolder + "helloWorldDoc.pdf";
        String signCertFileName = certsSrc + "signCertRsa01.pem";
        String tsaCertFileName = certsSrc + "tsCertRsa.pem";
        String rootCertFileName = certsSrc + "rootRsa.pem";

        X509Certificate signCert = (X509Certificate) PemFileHelper.readFirstChain(signCertFileName)[0];
        X509Certificate rootCert = (X509Certificate) PemFileHelper.readFirstChain(rootCertFileName)[0];
        Certificate[] certChain = new X509Certificate[] {signCert, rootCert};
        PrivateKey signPrivateKey = PemFileHelper.readFirstKey(signCertFileName, PASSWORD);
        Certificate[] tsaChain = PemFileHelper.readFirstChain(tsaCertFileName);
        PrivateKey tsaPrivateKey = PemFileHelper.readFirstKey(tsaCertFileName, PASSWORD);
        PrivateKey caPrivateKey = PemFileHelper.readFirstKey(rootCertFileName, PASSWORD);

        PadesTwoPhaseSigningHelper twoPhaseSigningHelper = new PadesTwoPhaseSigningHelper();

        TestTsaClient testTsa = new TestTsaClient(Arrays.asList(tsaChain), tsaPrivateKey);
        TestCrlClient crlClient = new TestCrlClient().addBuilderForCertIssuer(rootCert, caPrivateKey);
        crlClient.addBuilderForCertIssuer(rootCert, caPrivateKey);
        TestOcspClient ocspClient = new TestOcspClient().addBuilderForCertIssuer(rootCert, caPrivateKey);
        ocspClient.addBuilderForCertIssuer(rootCert, caPrivateKey);

        twoPhaseSigningHelper.setCrlClient(crlClient).setOcspClient(ocspClient).setTSAClient(testTsa)
                .setTimestampSignatureName("timestampSig1");

        try (ByteArrayOutputStream preparedDoc = new ByteArrayOutputStream()) {
            CMSContainer container = twoPhaseSigningHelper.createCMSContainerWithoutSignature(certChain,
                    DigestAlgorithms.SHA256, new PdfReader(srcFileName), preparedDoc, createSignerProperties());

            Exception exception = Assert.assertThrows(PdfException.class, () ->
                    twoPhaseSigningHelper.signCMSContainerWithBaselineLTAProfile(
                            new PrivateKeySignature(signPrivateKey, DigestAlgorithms.SHA512, FACTORY.getProviderName()),
                            new PdfReader(new ByteArrayInputStream(preparedDoc.toByteArray())),
                            FileUtil.getFileOutputStream(outFileName), "Signature1", container));
            Assert.assertEquals(MessageFormatUtil.format(SignExceptionMessageConstant.DIGEST_ALGORITHMS_ARE_NOT_SAME,
                    "SHA256", "SHA512"), exception.getMessage());
        }
    }

    @Test
    public void missingTimestampClientTest() throws Exception {
        String fileName = "missingTimestampClientTest.pdf";
        String outFileName = destinationFolder + fileName;
        String srcFileName = sourceFolder + "helloWorldDoc.pdf";
        String signCertFileName = certsSrc + "signCertRsa01.pem";
        String rootCertFileName = certsSrc + "rootRsa.pem";

        X509Certificate signCert = (X509Certificate) PemFileHelper.readFirstChain(signCertFileName)[0];
        X509Certificate rootCert = (X509Certificate) PemFileHelper.readFirstChain(rootCertFileName)[0];
        Certificate[] certChain = new X509Certificate[] {signCert, rootCert};
        PrivateKey signPrivateKey = PemFileHelper.readFirstKey(signCertFileName, PASSWORD);
        PrivateKey caPrivateKey = PemFileHelper.readFirstKey(rootCertFileName, PASSWORD);

        PadesTwoPhaseSigningHelper twoPhaseSigningHelper = new PadesTwoPhaseSigningHelper();
        
        TestCrlClient crlClient = new TestCrlClient().addBuilderForCertIssuer(rootCert, caPrivateKey);
        crlClient.addBuilderForCertIssuer(rootCert, caPrivateKey);
        TestOcspClient ocspClient = new TestOcspClient().addBuilderForCertIssuer(rootCert, caPrivateKey);
        ocspClient.addBuilderForCertIssuer(rootCert, caPrivateKey);

        twoPhaseSigningHelper.setCrlClient(crlClient).setOcspClient(ocspClient)
                .setTimestampSignatureName("timestampSig1");

        try (ByteArrayOutputStream preparedDoc = new ByteArrayOutputStream()) {
            CMSContainer container = twoPhaseSigningHelper.createCMSContainerWithoutSignature(certChain,
                    DigestAlgorithms.SHA256, new PdfReader(srcFileName), preparedDoc, createSignerProperties());

            Exception exception = Assert.assertThrows(PdfException.class, () ->
                    twoPhaseSigningHelper.signCMSContainerWithBaselineLTAProfile(
                            new PrivateKeySignature(signPrivateKey, DigestAlgorithms.SHA256, FACTORY.getProviderName()),
                            new PdfReader(new ByteArrayInputStream(preparedDoc.toByteArray())),
                            FileUtil.getFileOutputStream(outFileName), "Signature1", container));
            Assert.assertEquals(SignExceptionMessageConstant.TSA_CLIENT_IS_MISSING, exception.getMessage());
        }
    }

    private SignerProperties createSignerProperties() {
        SignerProperties signerProperties = new SignerProperties();
        signerProperties.setFieldName("Signature1");
        SignatureFieldAppearance appearance = new SignatureFieldAppearance(signerProperties.getFieldName())
                .setContent("Approval test signature.\nCreated by iText.");
        signerProperties.setPageRect(new Rectangle(50, 650, 200, 100))
                .setSignatureAppearance(appearance);

        return signerProperties;
    }
}