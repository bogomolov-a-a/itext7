/*
    This file is part of the iText (R) project.
    Copyright (c) 1998-2021 iText Group NV
    Authors: iText Software.

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
package com.itextpdf.signatures;

import com.itextpdf.commons.utils.DateTimeUtil;
import com.itextpdf.commons.utils.MessageFormatUtil;
import com.itextpdf.kernel.exceptions.PdfException;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.signatures.exceptions.SignExceptionMessageConstant;
import com.itextpdf.signatures.testutils.TimeTestUtil;
import com.itextpdf.test.ExtendedITextTest;
import com.itextpdf.test.annotations.type.UnitTest;
import com.itextpdf.test.signutils.Pkcs12FileHelper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509CRL;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ocsp.BasicOCSPResponse;
import org.bouncycastle.cert.ocsp.BasicOCSPResp;
import java.util.List;
import java.util.stream.Collectors;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.tsp.TimeStampToken;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(UnitTest.class)
public class PdfPKCS7Test extends ExtendedITextTest {
    private static final String SOURCE_FOLDER = "./src/test/resources/com/itextpdf/signatures/PdfPKCS7Test/";
    private static final String CERTS_SRC = "./src/test/resources/com/itextpdf/signatures/certs/";

    private static final char[] PASSWORD = "testpass".toCharArray();

    private static final double EPS = 0.001;

    private static Certificate[] chain;
    private static PrivateKey pk;

    @BeforeClass
    public static void init()
            throws KeyStoreException, IOException, CertificateException,
            NoSuchAlgorithmException, UnrecoverableKeyException {
        Security.addProvider(new BouncyCastleProvider());

        pk = Pkcs12FileHelper.readFirstKey(CERTS_SRC + "signCertRsa01.p12", PASSWORD, PASSWORD);
        chain = Pkcs12FileHelper.readFirstChain(CERTS_SRC + "signCertRsa01.p12", PASSWORD);
    }

    @Test
    // PdfPKCS7 is created here the same way it's done in PdfSigner#signDetached,
    // only the hash algorithm is altered
    public void unknownHashAlgorithmTest() {
        String hashAlgorithm = "";
        Exception e = Assert.assertThrows(PdfException.class,
                () -> new PdfPKCS7(null, chain, hashAlgorithm, null,
                        new BouncyCastleDigest(), false));
        Assert.assertEquals(
                MessageFormatUtil.format(SignExceptionMessageConstant.UNKNOWN_HASH_ALGORITHM, hashAlgorithm),
                e.getMessage());
    }

    @Test
    public void simpleCreationTest()
            throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
        String hashAlgorithm = DigestAlgorithms.SHA256;
        PdfPKCS7 pkcs7 = new PdfPKCS7(null, chain, hashAlgorithm, null,
                new BouncyCastleDigest(), false);

        String expectedOid = DigestAlgorithms.getAllowedDigest(hashAlgorithm);
        Assert.assertEquals(expectedOid, pkcs7.getDigestAlgorithmOid());
        Assert.assertEquals(chain[0], pkcs7.getSigningCertificate());
        Assert.assertArrayEquals(chain, pkcs7.getCertificates());
        Assert.assertNull(pkcs7.getDigestEncryptionAlgorithmOid());

        // test default fields
        Assert.assertEquals(1, pkcs7.getVersion());
        Assert.assertEquals(1, pkcs7.getSigningInfoVersion());
    }

    @Test
    public void simpleCreationWithPrivateKeyTest()
            throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
        String hashAlgorithm = DigestAlgorithms.SHA256;
        PdfPKCS7 pkcs7 = new PdfPKCS7(pk, chain, hashAlgorithm, null, new BouncyCastleDigest(), false);

        String expectedOid = DigestAlgorithms.getAllowedDigest(hashAlgorithm);
        Assert.assertEquals(expectedOid, pkcs7.getDigestAlgorithmOid());
        Assert.assertEquals(chain[0], pkcs7.getSigningCertificate());
        Assert.assertArrayEquals(chain, pkcs7.getCertificates());
        Assert.assertEquals(SecurityIDs.ID_RSA, pkcs7.getDigestEncryptionAlgorithmOid());
    }

    @Test
    public void reasonSetGetTest()
            throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
        PdfPKCS7 pkcs7 = createSimplePdfPKCS7();
        Assert.assertNull(pkcs7.getReason());

        String testReason = "testReason";
        pkcs7.setReason(testReason);
        Assert.assertEquals(testReason, pkcs7.getReason());
    }

    @Test
    public void locationSetGetTest()
            throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
        PdfPKCS7 pkcs7 = createSimplePdfPKCS7();
        Assert.assertNull(pkcs7.getLocation());

        String testLocation = "testLocation";
        pkcs7.setLocation(testLocation);
        Assert.assertEquals(testLocation, pkcs7.getLocation());
    }

    @Test
    public void signNameSetGetTest()
            throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
        PdfPKCS7 pkcs7 = createSimplePdfPKCS7();
        Assert.assertNull(pkcs7.getSignName());

        String testSignName = "testSignName";
        pkcs7.setSignName(testSignName);
        Assert.assertEquals(testSignName, pkcs7.getSignName());
    }

    @Test
    public void signDateSetGetTest()
            throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
        PdfPKCS7 pkcs7 = createSimplePdfPKCS7();
        Assert.assertEquals(TimestampConstants.UNDEFINED_TIMESTAMP_DATE, pkcs7.getSignDate());

        Calendar testSignDate = DateTimeUtil.getCurrentTimeCalendar();
        pkcs7.setSignDate(testSignDate);
        Assert.assertEquals(testSignDate, pkcs7.getSignDate());
    }

    @Test
    public void ocspGetTest() throws IOException {
        PdfDocument outDocument = new PdfDocument(
                new PdfReader(SOURCE_FOLDER + "ltvEnabledSingleSignatureTest01.pdf"));
        SignatureUtil sigUtil = new SignatureUtil(outDocument);
        PdfPKCS7 pkcs7 = sigUtil.readSignatureData("Signature1");

        Assert.assertNull(pkcs7.getCRLs());
        // it's tested here that ocsp and time stamp token were found while
        // constructing PdfPKCS7 instance
        TimeStampToken timeStampToken = pkcs7.getTimeStampToken();
        Assert.assertNotNull(timeStampToken);

        // The number corresponds to 3 September, 2021 13:32:33.
        double expectedMillis = (double) 1630675953000L;
        Assert.assertEquals(
                TimeTestUtil.getFullDaysMillis(expectedMillis),
                TimeTestUtil.getFullDaysMillis(DateTimeUtil.getUtcMillisFromEpoch(
                        DateTimeUtil.getCalendar(timeStampToken.getTimeStampInfo().getGenTime()))),
                EPS);
        Assert.assertEquals(
                TimeTestUtil.getFullDaysMillis(expectedMillis),
                TimeTestUtil.getFullDaysMillis(DateTimeUtil.getUtcMillisFromEpoch(
                        DateTimeUtil.getCalendar(pkcs7.getOcsp().getProducedAt()))),
                EPS);
    }

    @Test
    public void verifyTimestampImprintSimpleSignatureTest() throws IOException, GeneralSecurityException {
        PdfDocument outDocument = new PdfDocument(
                new PdfReader(SOURCE_FOLDER + "simpleSignature.pdf"));
        PdfPKCS7 pkcs7 = new SignatureUtil(outDocument).readSignatureData("Signature1");
        Assert.assertFalse(pkcs7.verifyTimestampImprint());
    }

    @Test
    public void verifyTimestampImprintTimeStampSignatureTest() throws IOException, GeneralSecurityException {
        PdfDocument outDocument = new PdfDocument(
                new PdfReader(SOURCE_FOLDER + "timeStampSignature.pdf"));
        PdfPKCS7 pkcs7 = new SignatureUtil(outDocument).readSignatureData("timestampSig1");
        Assert.assertFalse(pkcs7.verifyTimestampImprint());
    }

    @Test
    public void verifyTimestampImprintEmbeddedTimeStampSignatureTest() throws IOException, GeneralSecurityException {
        PdfDocument outDocument = new PdfDocument(
                new PdfReader(SOURCE_FOLDER + "embeddedTimeStampSignature.pdf"));
        PdfPKCS7 pkcs7 = new SignatureUtil(outDocument).readSignatureData("Signature1");
        Assert.assertTrue(pkcs7.verifyTimestampImprint());
    }

    @Test
    public void verifyTimestampImprintCorruptedTimeStampSignatureTest() throws IOException, GeneralSecurityException {
        PdfDocument outDocument = new PdfDocument(
                new PdfReader(SOURCE_FOLDER + "embeddedTimeStampCorruptedSignature.pdf"));
        PdfPKCS7 pkcs7 = new SignatureUtil(outDocument).readSignatureData("Signature1");
        Assert.assertTrue(pkcs7.verifyTimestampImprint());
    }

    @Test
    public void findCrlIsNotNullTest() throws IOException, CRLException {
        PdfDocument outDocument = new PdfDocument(
                new PdfReader(SOURCE_FOLDER + "singleSignatureNotEmptyCRL.pdf"));
        SignatureUtil sigUtil = new SignatureUtil(outDocument);
        PdfPKCS7 pkcs7 = sigUtil.readSignatureData("Signature1");
        List<X509CRL> crls = pkcs7.getCRLs().stream().map(crl -> (X509CRL)crl).collect(Collectors.toList());
        Assert.assertEquals(2, crls.size());
        Assert.assertArrayEquals(crls.get(0).getEncoded(), Files.readAllBytes(Paths.get(SOURCE_FOLDER, "firstCrl.bin")));
        Assert.assertArrayEquals(crls.get(1).getEncoded(), Files.readAllBytes(Paths.get(SOURCE_FOLDER, "secondCrl.bin")));
    }

    @Test
    public void findCrlNullSequenceNoExceptionTest()
            throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
        PdfPKCS7 pkcs7 = createSimplePdfPKCS7();
        pkcs7.findCRL(null);
        Assert.assertTrue(pkcs7.getCRLs().isEmpty());
    }

    @Test
    public void isRevocationValidWithInvalidOcspTest() throws IOException {
        PdfDocument outDocument = new PdfDocument(
                new PdfReader(SOURCE_FOLDER + "signatureWithInvalidOcspTest.pdf"));
        SignatureUtil sigUtil = new SignatureUtil(outDocument);
        PdfPKCS7 pkcs7 = sigUtil.readSignatureData("Signature1");
        Assert.assertFalse(pkcs7.isRevocationValid());
    }

    @Test
    public void isRevocationValidWithValidOcspTest() throws IOException {
        PdfDocument outDocument = new PdfDocument(
                new PdfReader(SOURCE_FOLDER + "signatureWithValidOcspTest.pdf"));
        SignatureUtil sigUtil = new SignatureUtil(outDocument);
        PdfPKCS7 pkcs7 = sigUtil.readSignatureData("Signature1");
        Assert.assertTrue(pkcs7.isRevocationValid());
    }

    @Test
    public void isRevocationValidOcspResponseIsNullTest()
            throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, IOException {
        PdfPKCS7 pkcs7 = createSimplePdfPKCS7();
        pkcs7.basicResp = null;
        Assert.assertFalse(pkcs7.isRevocationValid());
    }

    @Test
    public void isRevocationValidLackOfSignCertsTest()
            throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, IOException {
        PdfPKCS7 pkcs7 = createSimplePdfPKCS7();
        pkcs7.basicResp = new BasicOCSPResp(BasicOCSPResponse.getInstance(new ASN1InputStream(
                Files.readAllBytes(Paths.get(SOURCE_FOLDER, "simpleOCSPResponse.bin"))).readObject()));
        pkcs7.signCerts = Collections.singleton(chain[0]);
        Assert.assertFalse(pkcs7.isRevocationValid());
    }

    @Test
    public void isRevocationValidExceptionDuringValidationTest()
            throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, IOException {
        PdfPKCS7 pkcs7 = createSimplePdfPKCS7();
        pkcs7.basicResp = new BasicOCSPResp(BasicOCSPResponse.getInstance(new ASN1InputStream(
                Files.readAllBytes(Paths.get(SOURCE_FOLDER, "simpleOCSPResponse.bin"))).readObject()));
        pkcs7.signCerts = Arrays.asList(new Certificate[]{null, null});
        Assert.assertFalse(pkcs7.isRevocationValid());
    }

    // PdfPKCS7 is created here the same way it's done in PdfSigner#signDetached
    private static PdfPKCS7 createSimplePdfPKCS7()
            throws NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException {
        return new PdfPKCS7(null, chain, DigestAlgorithms.SHA256, null,
                new BouncyCastleDigest(), false);
    }
}
