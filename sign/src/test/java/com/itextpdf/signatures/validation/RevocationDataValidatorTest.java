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
package com.itextpdf.signatures.validation;

import com.itextpdf.bouncycastleconnector.BouncyCastleFactoryCreator;
import com.itextpdf.commons.bouncycastle.IBouncyCastleFactory;
import com.itextpdf.commons.bouncycastle.operator.AbstractOperatorCreationException;
import com.itextpdf.commons.bouncycastle.pkcs.AbstractPKCSException;
import com.itextpdf.commons.utils.DateTimeUtil;
import com.itextpdf.commons.utils.MessageFormatUtil;
import com.itextpdf.signatures.ICrlClient;
import com.itextpdf.signatures.IssuingCertificateRetriever;
import com.itextpdf.signatures.testutils.PemFileHelper;
import com.itextpdf.signatures.testutils.TimeTestUtil;
import com.itextpdf.signatures.testutils.builder.TestCrlBuilder;
import com.itextpdf.signatures.testutils.builder.TestOcspResponseBuilder;
import com.itextpdf.signatures.testutils.client.TestCrlClient;
import com.itextpdf.signatures.testutils.client.TestOcspClient;
import com.itextpdf.signatures.validation.report.CertificateReportItem;
import com.itextpdf.signatures.validation.report.ValidationReport;
import com.itextpdf.test.ExtendedITextTest;
import com.itextpdf.test.annotations.type.BouncyCastleUnitTest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;

@Category(BouncyCastleUnitTest.class)
public class RevocationDataValidatorTest extends ExtendedITextTest {
    private static final IBouncyCastleFactory FACTORY = BouncyCastleFactoryCreator.getFactory();
    private static final String SOURCE_FOLDER =
            "./src/test/resources/com/itextpdf/signatures/validation/RevocationDataValidatorTest/";
    private static final char[] PASSWORD = "testpassphrase".toCharArray();
    private static final long MILLISECONDS_PER_DAY = 86_400_000L;

    private static X509Certificate caCert;
    private static PrivateKey caPrivateKey;
    private static X509Certificate checkCert;
    private static X509Certificate responderCert;
    private static PrivateKey ocspRespPrivateKey;

    @BeforeClass
    public static void before()
            throws CertificateException, IOException, AbstractOperatorCreationException, AbstractPKCSException {
        Security.addProvider(FACTORY.getProvider());

        String rootCertFileName = SOURCE_FOLDER + "rootCert.pem";
        String checkCertFileName = SOURCE_FOLDER + "signCert.pem";
        String ocspResponderCertFileName = SOURCE_FOLDER + "ocspResponderCert.pem";

        caCert = (X509Certificate) PemFileHelper.readFirstChain(rootCertFileName)[0];
        caPrivateKey = PemFileHelper.readFirstKey(rootCertFileName, PASSWORD);
        checkCert = (X509Certificate) PemFileHelper.readFirstChain(checkCertFileName)[0];
        responderCert = (X509Certificate) PemFileHelper.readFirstChain(ocspResponderCertFileName)[0];
        ocspRespPrivateKey = PemFileHelper.readFirstKey(ocspResponderCertFileName, PASSWORD);
    }

    @Test
    public void basicValidationWithOcspClientTest() throws GeneralSecurityException, IOException {
        Date checkDate = TimeTestUtil.TEST_DATE_TIME;
        TestOcspResponseBuilder builder = new TestOcspResponseBuilder(responderCert, ocspRespPrivateKey);
        builder.setProducedAt(DateTimeUtil.addDaysToDate(checkDate, 5));
        builder.setThisUpdate(DateTimeUtil.getCalendar(DateTimeUtil.addDaysToDate(checkDate, 5)));
        builder.setNextUpdate(DateTimeUtil.getCalendar(DateTimeUtil.addDaysToDate(checkDate, 10)));
        TestOcspClient ocspClient = new TestOcspClient().addBuilderForCertIssuer(caCert, builder);

        ValidationReport report = new ValidationReport();
        IssuingCertificateRetriever certificateRetriever = new IssuingCertificateRetriever();
        certificateRetriever.addTrustedCertificates(Collections.singletonList(caCert));

        RevocationDataValidator validator = new RevocationDataValidator()
                .setIssuingCertificateRetriever(certificateRetriever)
                .setOnlineFetching(RevocationDataValidator.OnlineFetching.NEVER_FETCH)
                .addOcspClient(ocspClient);
        validator.setFreshness(-2 * MILLISECONDS_PER_DAY);
        validator.validate(report, checkCert, checkDate);

        Assert.assertEquals(0, report.getFailures().size());
        Assert.assertEquals(2, report.getLogs().size());
        CertificateReportItem item = (CertificateReportItem) report.getLogs().get(0);
        Assert.assertEquals(RevocationDataValidator.REVOCATION_DATA_CHECK, item.getCheckName());
        Assert.assertEquals(RevocationDataValidator.TRUSTED_OCSP_RESPONDER, item.getMessage());
        item = (CertificateReportItem) report.getLogs().get(1);
        Assert.assertEquals(CertificateChainValidator.CERTIFICATE_CHECK, item.getCheckName());
        Assert.assertEquals(MessageFormatUtil.format(CertificateChainValidator.CERTIFICATE_TRUSTED,
                item.getCertificate().getSubjectX500Principal()), item.getMessage());
        Assert.assertEquals(ValidationReport.ValidationResult.VALID, report.getValidationResult());
    }

    @Test
    public void basicValidationWithCrlClientTest() throws GeneralSecurityException {
        Date checkDate = TimeTestUtil.TEST_DATE_TIME;
        Date revocationDate = DateTimeUtil.addDaysToDate(checkDate, -1);
        TestCrlBuilder builder = new TestCrlBuilder(caCert, caPrivateKey, checkDate);
        builder.setNextUpdate(DateTimeUtil.addDaysToDate(checkDate, 10));
        builder.addCrlEntry(checkCert, revocationDate, FACTORY.createCRLReason().getKeyCompromise());
        TestCrlClient crlClient = new TestCrlClient().addBuilderForCertIssuer(builder);

        ValidationReport report = new ValidationReport();
        IssuingCertificateRetriever certificateRetriever = new IssuingCertificateRetriever();
        certificateRetriever.addTrustedCertificates(Collections.singletonList(caCert));

        RevocationDataValidator validator = new RevocationDataValidator()
                .setIssuingCertificateRetriever(certificateRetriever)
                .setOnlineFetching(RevocationDataValidator.OnlineFetching.NEVER_FETCH)
                .addCrlClient(crlClient);
        validator.setFreshness(0);
        validator.validate(report, checkCert, checkDate);

        Assert.assertEquals(1, report.getFailures().size());
        Assert.assertEquals(3, report.getLogs().size());
        Assert.assertEquals(report.getFailures().get(0), report.getLogs().get(2));
        CertificateReportItem item = (CertificateReportItem) report.getLogs().get(0);
        Assert.assertEquals(caCert, item.getCertificate());
        Assert.assertEquals(CRLValidator.CRL_CHECK, item.getCheckName());
        Assert.assertEquals("Using crl nextUpdate date as validation date", item.getMessage());
        item = (CertificateReportItem) report.getLogs().get(1);
        Assert.assertEquals(caCert, item.getCertificate());
        Assert.assertEquals(CertificateChainValidator.CERTIFICATE_CHECK, item.getCheckName());
        Assert.assertEquals(MessageFormatUtil.format(CertificateChainValidator.CERTIFICATE_TRUSTED,
                item.getCertificate().getSubjectX500Principal()), item.getMessage());
        item = (CertificateReportItem) report.getLogs().get(2);
        Assert.assertEquals(checkCert, item.getCertificate());
        Assert.assertEquals(CRLValidator.CRL_CHECK, item.getCheckName());
        Assert.assertEquals(MessageFormatUtil.format(CRLValidator.CERTIFICATE_REVOKED,
                caCert.getSubjectX500Principal(), revocationDate), item.getMessage());

        Assert.assertEquals(ValidationReport.ValidationResult.INVALID, report.getValidationResult());
    }

    @Test
    public void useFreshCrlResponseTest() throws GeneralSecurityException {
        // Add client with indeterminate CRL, then with CRL which contains revoked checkCert.
        Date checkDate = TimeTestUtil.TEST_DATE_TIME;
        Date revocationDate = DateTimeUtil.addDaysToDate(checkDate, -1);

        TestCrlBuilder builder1 = new TestCrlBuilder(caCert, caPrivateKey, checkDate);
        builder1.setNextUpdate(DateTimeUtil.addDaysToDate(checkDate, 2));
        builder1.addCrlEntry(checkCert, revocationDate, FACTORY.createCRLReason().getKeyCompromise());
        TestCrlClient crlClient1 = new TestCrlClient().addBuilderForCertIssuer(builder1);

        Date thisUpdate2 = DateTimeUtil.addDaysToDate(checkDate, -2);
        TestCrlBuilder builder2 = new TestCrlBuilder(caCert, caPrivateKey, thisUpdate2);
        builder2.setNextUpdate(checkDate);
        TestCrlClient crlClient2 = new TestCrlClient().addBuilderForCertIssuer(builder2);

        ValidationReport report = new ValidationReport();
        IssuingCertificateRetriever certificateRetriever = new IssuingCertificateRetriever();
        certificateRetriever.addTrustedCertificates(Collections.singletonList(caCert));

        RevocationDataValidator validator = new RevocationDataValidator()
                .setIssuingCertificateRetriever(certificateRetriever)
                .setOnlineFetching(RevocationDataValidator.OnlineFetching.NEVER_FETCH)
                .addCrlClient(crlClient1)
                .addCrlClient(crlClient2);
        validator.setFreshness(0);
        validator.validate(report, checkCert, checkDate);

        Assert.assertEquals(1, report.getFailures().size());
        Assert.assertEquals(3, report.getLogs().size());
        Assert.assertEquals(report.getFailures().get(0), report.getLogs().get(2));
        CertificateReportItem item = (CertificateReportItem) report.getLogs().get(0);
        Assert.assertEquals(caCert, item.getCertificate());
        Assert.assertEquals(CRLValidator.CRL_CHECK, item.getCheckName());
        Assert.assertEquals("Using crl nextUpdate date as validation date", item.getMessage());
        item = (CertificateReportItem) report.getLogs().get(1);
        Assert.assertEquals(caCert, item.getCertificate());
        Assert.assertEquals(CertificateChainValidator.CERTIFICATE_CHECK, item.getCheckName());
        Assert.assertEquals(MessageFormatUtil.format(CertificateChainValidator.CERTIFICATE_TRUSTED,
                item.getCertificate().getSubjectX500Principal()), item.getMessage());
        item = (CertificateReportItem) report.getLogs().get(2);
        Assert.assertEquals(checkCert, item.getCertificate());
        Assert.assertEquals(CRLValidator.CRL_CHECK, item.getCheckName());
        Assert.assertEquals(MessageFormatUtil.format(CRLValidator.CERTIFICATE_REVOKED,
                caCert.getSubjectX500Principal(), revocationDate), item.getMessage());

        Assert.assertEquals(ValidationReport.ValidationResult.INVALID, report.getValidationResult());
    }

    @Test
    public void useFreshOcspResponseTest() throws GeneralSecurityException, IOException {
        // Add client with indeterminate OCSP, then with valid OCSP.
        Date checkDate = TimeTestUtil.TEST_DATE_TIME;

        TestOcspResponseBuilder builder1 = new TestOcspResponseBuilder(responderCert, ocspRespPrivateKey);
        builder1.setProducedAt(checkDate);
        builder1.setThisUpdate(DateTimeUtil.getCalendar(checkDate));
        builder1.setNextUpdate(DateTimeUtil.getCalendar(DateTimeUtil.addDaysToDate(checkDate, 5)));
        builder1.setCertificateStatus(FACTORY.createUnknownStatus());
        TestOcspClient ocspClient1 = new TestOcspClient().addBuilderForCertIssuer(caCert, builder1);

        TestOcspResponseBuilder builder2 = new TestOcspResponseBuilder(responderCert, ocspRespPrivateKey);
        builder2.setProducedAt(DateTimeUtil.addDaysToDate(checkDate, 5));
        builder2.setThisUpdate(DateTimeUtil.getCalendar(DateTimeUtil.addDaysToDate(checkDate, 5)));
        builder2.setNextUpdate(DateTimeUtil.getCalendar(DateTimeUtil.addDaysToDate(checkDate, 10)));
        TestOcspClient ocspClient2 = new TestOcspClient().addBuilderForCertIssuer(caCert, builder2);

        ValidationReport report = new ValidationReport();
        IssuingCertificateRetriever certificateRetriever = new IssuingCertificateRetriever();
        certificateRetriever.addTrustedCertificates(Collections.singletonList(caCert));

        RevocationDataValidator validator = new RevocationDataValidator()
                .setIssuingCertificateRetriever(certificateRetriever)
                .setOnlineFetching(RevocationDataValidator.OnlineFetching.NEVER_FETCH)
                .addOcspClient(ocspClient1)
                .addOcspClient(ocspClient2);
        validator.setFreshness(-2 * MILLISECONDS_PER_DAY);
        validator.validate(report, checkCert, checkDate);

        Assert.assertEquals(0, report.getFailures().size());
        Assert.assertEquals(2, report.getLogs().size());
        CertificateReportItem item = (CertificateReportItem) report.getLogs().get(0);
        Assert.assertEquals(RevocationDataValidator.REVOCATION_DATA_CHECK, item.getCheckName());
        Assert.assertEquals(RevocationDataValidator.TRUSTED_OCSP_RESPONDER, item.getMessage());
        item = (CertificateReportItem) report.getLogs().get(1);
        Assert.assertEquals(CertificateChainValidator.CERTIFICATE_CHECK, item.getCheckName());
        Assert.assertEquals(MessageFormatUtil.format(CertificateChainValidator.CERTIFICATE_TRUSTED,
                item.getCertificate().getSubjectX500Principal()), item.getMessage());

        Assert.assertEquals(ValidationReport.ValidationResult.VALID, report.getValidationResult());
    }

    @Test
    public void validityAssuredTest() throws CertificateException, IOException {
        String checkCertFileName = SOURCE_FOLDER + "validityAssuredSigningCert.pem";
        X509Certificate certificate = (X509Certificate) PemFileHelper.readFirstChain(checkCertFileName)[0];
        Date checkDate = TimeTestUtil.TEST_DATE_TIME;

        ValidationReport report = new ValidationReport();
        RevocationDataValidator validator = new RevocationDataValidator();
        validator.validate(report, certificate, checkDate);

        Assert.assertEquals(0, report.getFailures().size());
        Assert.assertEquals(1, report.getLogs().size());
        CertificateReportItem item = (CertificateReportItem) report.getLogs().get(0);
        Assert.assertEquals(certificate, item.getCertificate());
        Assert.assertEquals(RevocationDataValidator.REVOCATION_DATA_CHECK, item.getCheckName());
        Assert.assertEquals(RevocationDataValidator.VALIDITY_ASSURED, item.getMessage());
        Assert.assertEquals(ValidationReport.ValidationResult.VALID, report.getValidationResult());
    }

    @Test
    public void noRevocationDataTest() {
        ValidationReport report = new ValidationReport();
        RevocationDataValidator validator = new RevocationDataValidator()
                .setOnlineFetching(RevocationDataValidator.OnlineFetching.NEVER_FETCH);
        validator.validate(report, checkCert, TimeTestUtil.TEST_DATE_TIME);

        Assert.assertEquals(1, report.getFailures().size());
        Assert.assertEquals(1, report.getLogs().size());
        CertificateReportItem item = (CertificateReportItem) report.getLogs().get(0);
        Assert.assertEquals(RevocationDataValidator.REVOCATION_DATA_CHECK, item.getCheckName());
        Assert.assertEquals(RevocationDataValidator.NO_REVOCATION_DATA, item.getMessage());
        Assert.assertEquals(ValidationReport.ValidationResult.INDETERMINATE, report.getValidationResult());
    }

    @Test
    public void tryFetchRevocationDataOnlineTest() {
        ValidationReport report = new ValidationReport();
        RevocationDataValidator validator = new RevocationDataValidator()
                .setOnlineFetching(RevocationDataValidator.OnlineFetching.FETCH_IF_NO_OTHER_DATA_AVAILABLE);
        validator.validate(report, checkCert, TimeTestUtil.TEST_DATE_TIME);

        Assert.assertEquals(1, report.getFailures().size());
        Assert.assertEquals(1, report.getLogs().size());
        CertificateReportItem item = (CertificateReportItem) report.getLogs().get(0);
        Assert.assertEquals(RevocationDataValidator.REVOCATION_DATA_CHECK, item.getCheckName());
        Assert.assertEquals(RevocationDataValidator.NO_REVOCATION_DATA, item.getMessage());
        Assert.assertEquals(ValidationReport.ValidationResult.INDETERMINATE, report.getValidationResult());
    }

    @Test
    public void crlEncodingErrorTest() throws Exception {
        byte[] crl = new TestCrlBuilder(caCert,  caPrivateKey).makeCrl();
        crl[5] = 0;
        ValidationReport report = new ValidationReport();
        new RevocationDataValidator().setIssuingCertificateRetriever(new IssuingCertificateRetriever())
                .setOnlineFetching(RevocationDataValidator.OnlineFetching.NEVER_FETCH)
                .addCrlClient(new ICrlClient() {
                    @Override
                    public Collection<byte[]> getEncoded(X509Certificate checkCert, String url) {
                        return Collections.singletonList(crl);
                    }
                })
                .validate(report, checkCert, TimeTestUtil.TEST_DATE_TIME);

        CertificateReportItem item = (CertificateReportItem) report.getLogs().get(0);
        Assert.assertEquals(RevocationDataValidator.REVOCATION_DATA_CHECK, item.getCheckName());
        Assert.assertEquals(RevocationDataValidator.CRL_PARSING_ERROR, item.getMessage());
        item = (CertificateReportItem) report.getLogs().get(1);
        Assert.assertEquals(RevocationDataValidator.REVOCATION_DATA_CHECK, item.getCheckName());
        Assert.assertEquals(RevocationDataValidator.NO_REVOCATION_DATA, item.getMessage());
        Assert.assertEquals(ValidationReport.ValidationResult.INDETERMINATE, report.getValidationResult());
    }

    @Test
    public void sortResponsesTest() throws GeneralSecurityException, IOException {
        Date checkDate = TimeTestUtil.TEST_DATE_TIME;

        // The oldest one, but the only one valid.
        TestOcspResponseBuilder ocspBuilder1 = new TestOcspResponseBuilder(responderCert, ocspRespPrivateKey);
        ocspBuilder1.setProducedAt(checkDate);
        ocspBuilder1.setThisUpdate(DateTimeUtil.getCalendar(checkDate));
        ocspBuilder1.setNextUpdate(DateTimeUtil.getCalendar(DateTimeUtil.addDaysToDate(checkDate, 3)));
        TestOcspClient ocspClient1 = new TestOcspClient().addBuilderForCertIssuer(caCert, ocspBuilder1);

        TestOcspResponseBuilder ocspBuilder2 = new TestOcspResponseBuilder(responderCert, ocspRespPrivateKey);
        ocspBuilder2.setProducedAt(DateTimeUtil.addDaysToDate(checkDate, 3));
        ocspBuilder2.setThisUpdate(DateTimeUtil.getCalendar(DateTimeUtil.addDaysToDate(checkDate, 3)));
        ocspBuilder2.setNextUpdate(DateTimeUtil.getCalendar(DateTimeUtil.addDaysToDate(checkDate, 5)));
        ocspBuilder2.setCertificateStatus(FACTORY.createUnknownStatus());
        TestOcspClient ocspClient2 = new TestOcspClient().addBuilderForCertIssuer(caCert, ocspBuilder2);

        TestOcspResponseBuilder ocspBuilder3 = new TestOcspResponseBuilder(responderCert, ocspRespPrivateKey);
        ocspBuilder3.setProducedAt(DateTimeUtil.addDaysToDate(checkDate, 5));
        ocspBuilder3.setThisUpdate(DateTimeUtil.getCalendar(DateTimeUtil.addDaysToDate(checkDate, 5)));
        ocspBuilder3.setNextUpdate(DateTimeUtil.getCalendar(DateTimeUtil.addDaysToDate(checkDate, 10)));
        ocspBuilder3.setCertificateStatus(FACTORY.createUnknownStatus());
        TestOcspClient ocspClient3 = new TestOcspClient().addBuilderForCertIssuer(caCert, ocspBuilder3);

        TestCrlBuilder crlBuilder1 = new TestCrlBuilder(caCert, caPrivateKey, checkDate);
        crlBuilder1.setNextUpdate(DateTimeUtil.addDaysToDate(checkDate, 2));

        TestCrlBuilder crlBuilder2 = new TestCrlBuilder(caCert, caPrivateKey, DateTimeUtil.addDaysToDate(checkDate, 2));
        crlBuilder2.setNextUpdate(DateTimeUtil.addDaysToDate(checkDate, 5));
        TestCrlClient crlClient = new TestCrlClient()
                .addBuilderForCertIssuer(crlBuilder1)
                .addBuilderForCertIssuer(crlBuilder2);

        ValidationReport report = new ValidationReport();
        IssuingCertificateRetriever certificateRetriever = new IssuingCertificateRetriever();
        certificateRetriever.addTrustedCertificates(Collections.singletonList(caCert));

        RevocationDataValidator validator = new RevocationDataValidator()
                .setIssuingCertificateRetriever(certificateRetriever)
                .setOnlineFetching(RevocationDataValidator.OnlineFetching.NEVER_FETCH)
                .setCRLValidator(new CRLValidator().setFreshness(-5 * MILLISECONDS_PER_DAY))
                .addCrlClient(crlClient)
                .addOcspClient(ocspClient1)
                .addOcspClient(ocspClient2)
                .addOcspClient(ocspClient3);
        validator.validate(report, checkCert, checkDate);

        Assert.assertEquals(0, report.getFailures().size());
        Assert.assertEquals(6, report.getLogs().size());
        CertificateReportItem item = (CertificateReportItem) report.getLogs().get(0);
        Assert.assertEquals(checkCert, item.getCertificate());
        Assert.assertEquals(OCSPValidator.OCSP_CHECK, item.getCheckName());
        Assert.assertEquals(OCSPValidator.CERT_STATUS_IS_UNKNOWN, item.getMessage());
        item = (CertificateReportItem) report.getLogs().get(1);
        Assert.assertEquals(checkCert, item.getCertificate());
        Assert.assertEquals(OCSPValidator.OCSP_CHECK, item.getCheckName());
        Assert.assertEquals(OCSPValidator.CERT_STATUS_IS_UNKNOWN, item.getMessage());

        item = (CertificateReportItem) report.getLogs().get(2);
        Assert.assertEquals(checkCert, item.getCertificate());
        Assert.assertEquals(CRLValidator.CRL_CHECK, item.getCheckName());
        Assert.assertEquals(MessageFormatUtil.format(CRLValidator.FRESHNESS_CHECK,
                DateTimeUtil.addDaysToDate(checkDate, 2), checkDate, -5 * MILLISECONDS_PER_DAY), item.getMessage());
        item = (CertificateReportItem) report.getLogs().get(3);
        Assert.assertEquals(checkCert, item.getCertificate());
        Assert.assertEquals(CRLValidator.CRL_CHECK, item.getCheckName());
        Assert.assertEquals(MessageFormatUtil.format(CRLValidator.FRESHNESS_CHECK,
                checkDate, checkDate, -5 * MILLISECONDS_PER_DAY), item.getMessage());

        item = (CertificateReportItem) report.getLogs().get(4);
        Assert.assertEquals(RevocationDataValidator.REVOCATION_DATA_CHECK, item.getCheckName());
        Assert.assertEquals(RevocationDataValidator.TRUSTED_OCSP_RESPONDER, item.getMessage());
        item = (CertificateReportItem) report.getLogs().get(5);
        Assert.assertEquals(CertificateChainValidator.CERTIFICATE_CHECK, item.getCheckName());
        Assert.assertEquals(MessageFormatUtil.format(CertificateChainValidator.CERTIFICATE_TRUSTED,
                item.getCertificate().getSubjectX500Principal()), item.getMessage());
        Assert.assertEquals(ValidationReport.ValidationResult.VALID, report.getValidationResult());

        Assert.assertEquals(ValidationReport.ValidationResult.VALID, report.getValidationResult());
    }
}