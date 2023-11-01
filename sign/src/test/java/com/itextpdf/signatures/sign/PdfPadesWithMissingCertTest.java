package com.itextpdf.signatures.sign;

import com.itextpdf.bouncycastleconnector.BouncyCastleFactoryCreator;
import com.itextpdf.commons.bouncycastle.IBouncyCastleFactory;
import com.itextpdf.commons.bouncycastle.operator.AbstractOperatorCreationException;
import com.itextpdf.commons.bouncycastle.pkcs.AbstractPKCSException;
import com.itextpdf.commons.utils.FileUtil;
import com.itextpdf.forms.form.element.SignatureFieldAppearance;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.signatures.IMissingCertificatesClient;
import com.itextpdf.signatures.MissingCertificatesClient;
import com.itextpdf.signatures.PdfPadesSigner;
import com.itextpdf.signatures.SignerProperties;
import com.itextpdf.signatures.TestSignUtils;
import com.itextpdf.signatures.logs.SignLogMessageConstant;
import com.itextpdf.signatures.testutils.PemFileHelper;
import com.itextpdf.test.ExtendedITextTest;
import com.itextpdf.test.annotations.LogMessage;
import com.itextpdf.test.annotations.LogMessages;
import com.itextpdf.test.annotations.type.BouncyCastleIntegrationTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
@Category(BouncyCastleIntegrationTest.class)
public class PdfPadesWithMissingCertTest extends ExtendedITextTest {
    private static final IBouncyCastleFactory FACTORY = BouncyCastleFactoryCreator.getFactory();

    private static final String certsSrc = "./src/test/resources/com/itextpdf/signatures/sign/PdfPadesWithMissingCertTest/certs/";
    private static final String sourceFolder = "./src/test/resources/com/itextpdf/signatures/sign/PdfPadesWithMissingCertTest/";
    private static final String destinationFolder = "./target/test/com/itextpdf/signatures/sign/PdfPadesWithMissingCertTest/";

    private static final char[] PASSWORD = "testpassphrase".toCharArray();
    
    private final String missingCertName1;
    private final String missingCertName2;

    @BeforeClass
    public static void before() {
        Security.addProvider(FACTORY.getProvider());
        createOrClearDestinationFolder(destinationFolder);
    }

    public PdfPadesWithMissingCertTest(Object missingCertName1, Object missingCertName2) {
        this.missingCertName1 = (String) missingCertName1;
        this.missingCertName2 = (String) missingCertName2;
    }

    @Parameterized.Parameters(name = "first missing cert: {0}; second missing cert: {1};")
    public static Iterable<Object[]> createParameters() {
        return Arrays.asList(new Object[] {"missing_cert1.cer", "missing_cert2.cer"},
                new Object[] {"missing_cert1.crt", "missing_cert2.crt"},
                new Object[] {null, "missing_certs.p7b"},
                new Object[] {"not_existing_file", "not_existing_file"},
                new Object[] {"missing_cert1.der", "missing_cert2.der"});
    }
    
    @Test
    @LogMessages(messages = @LogMessage(messageTemplate = SignLogMessageConstant.UNABLE_TO_PARSE_AIA_CERT), ignore = true)
    public void missingCertTest()
            throws GeneralSecurityException, IOException, AbstractOperatorCreationException, AbstractPKCSException {
        String srcFileName = sourceFolder + "helloWorldDoc.pdf";
        String signCertFileName = certsSrc + "sign_cert.pem";
        String fistIntermediateCertFileName = certsSrc + "first_intermediate_cert.pem";
        String secondIntermediateCertFileName = certsSrc + "second_intermediate_cert.pem";
        String rootCertFileName = certsSrc + "root_cert.pem";
        String firstMissingCertFileName = certsSrc + missingCertName1;
        String secondMissingCertFileName = certsSrc + missingCertName2;

        X509Certificate signCert = (X509Certificate) PemFileHelper.readFirstChain(signCertFileName)[0];
        X509Certificate fistIntermediateCert = (X509Certificate) PemFileHelper.readFirstChain(fistIntermediateCertFileName)[0];
        X509Certificate secondIntermediateCert = (X509Certificate) PemFileHelper.readFirstChain(secondIntermediateCertFileName)[0];
        X509Certificate rootCert = (X509Certificate) PemFileHelper.readFirstChain(rootCertFileName)[0];
        PrivateKey signPrivateKey = PemFileHelper.readFirstKey(signCertFileName, PASSWORD);

        SignerProperties signerProperties = createSignerProperties();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PdfPadesSigner padesSigner = createPdfPadesSigner(srcFileName, outputStream);
        IMissingCertificatesClient missingCertificatesClient = new MissingCertificatesClient() {
            @Override
            protected InputStream getIssuerCertByURI(String uri) throws IOException {
                if (uri.contains("intermediate")) {
                    return FileUtil.getInputStreamForFile(firstMissingCertFileName);
                }
                if (uri.contains("leaf")) {
                    return FileUtil.getInputStreamForFile(secondMissingCertFileName);
                }
                return null;
            }
        };
        padesSigner.setMissingCertificatesClient(missingCertificatesClient);
        
        padesSigner.signWithBaselineBProfile(signerProperties, new Certificate[]{signCert, rootCert}, signPrivateKey);
        
        TestSignUtils.basicCheckSignedDoc(new ByteArrayInputStream(outputStream.toByteArray()), "Signature1");
        List<X509Certificate> expectedCerts;
        if ("not_existing_file".equals(missingCertName1)) {
            expectedCerts = Arrays.asList(signCert, rootCert);
        } else {
            expectedCerts = Arrays.asList(signCert, fistIntermediateCert, secondIntermediateCert, rootCert);
        }
        TestSignUtils.signedDocumentContainsCerts(new ByteArrayInputStream(outputStream.toByteArray()), expectedCerts);
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

    private PdfPadesSigner createPdfPadesSigner(String srcFileName, OutputStream outputStream) throws IOException {
        return new PdfPadesSigner(new PdfReader(FileUtil.getInputStreamForFile(srcFileName)), outputStream);
    }
}