package com.itextpdf.kernel.pdf.annot;

import com.itextpdf.io.LogMessageConstant;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfArray;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfName;
import com.itextpdf.kernel.pdf.PdfNumber;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.navigation.PdfDestination;
import com.itextpdf.kernel.pdf.navigation.PdfExplicitDestination;
import com.itextpdf.kernel.utils.CompareTool;
import com.itextpdf.test.ExtendedITextTest;
import com.itextpdf.test.annotations.LogMessage;
import com.itextpdf.test.annotations.LogMessages;
import com.itextpdf.test.annotations.type.IntegrationTest;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
public class AddLinkAnnotationTest extends ExtendedITextTest {

    public static final String sourceFolder = "./src/test/resources/com/itextpdf/kernel/pdf/annot/AddLinkAnnotationTest/";
    public static final String destinationFolder = "./target/test/com/itextpdf/kernel/pdf/annot/AddLinkAnnotationTest/";

    @BeforeClass
    public static void beforeClass() {
        createDestinationFolder(destinationFolder);
    }

    @Test
    public void addLinkAnnotation01() throws Exception {
        PdfDocument document = new PdfDocument(new PdfWriter(destinationFolder + "linkAnnotation01.pdf"));

        PdfPage page1 = document.addNewPage();
        PdfPage page2 = document.addNewPage();

        PdfCanvas canvas = new PdfCanvas(page1);
        canvas.beginText();
        canvas.setFontAndSize(PdfFontFactory.createFont(StandardFonts.COURIER_BOLD), 14);
        canvas.moveText(100, 600);
        canvas.showText("Page 1");
        canvas.moveText(0, -30);
        canvas.showText("Link to page 2. Click here!");
        canvas.endText();
        canvas.release();
        page1.addAnnotation(new PdfLinkAnnotation(new Rectangle(100, 560, 260, 25)).setDestination(PdfExplicitDestination.createFit(page2)).setBorder(new PdfArray(new float[]{0, 0, 1})));
        page1.flush();

        canvas = new PdfCanvas(page2);
        canvas.beginText();
        canvas.setFontAndSize(PdfFontFactory.createFont(StandardFonts.COURIER_BOLD), 14);
        canvas.moveText(100, 600);
        canvas.showText("Page 2");
        canvas.endText();
        canvas.release();
        page2.flush();

        document.close();

        Assert.assertNull(new CompareTool().compareByContent(destinationFolder + "linkAnnotation01.pdf", sourceFolder + "cmp_linkAnnotation01.pdf", destinationFolder, "diff_"));

    }

    @Test
    public void addLinkAnnotation02() throws Exception {
        PdfDocument document = new PdfDocument(new PdfWriter(destinationFolder + "linkAnnotation02.pdf"));

        PdfPage page = document.addNewPage();

        PdfCanvas canvas = new PdfCanvas(page);
        canvas.beginText();
        canvas.setFontAndSize(PdfFontFactory.createFont(StandardFonts.COURIER_BOLD), 14);
        canvas.moveText(100, 600);
        canvas.showText("Click here to go to itextpdf site.");
        canvas.endText();
        canvas.release();

        page.addAnnotation(new PdfLinkAnnotation(new Rectangle(100, 590, 300, 25)).
                setAction(PdfAction.createURI("http://itextpdf.com")).
                setBorder(new PdfArray(new float[]{0, 0, 1})).
                setColor(new PdfArray(new float[]{1, 0, 0})));
        page.flush();

        document.close();

        Assert.assertNull(new CompareTool().compareByContent(destinationFolder + "linkAnnotation02.pdf", sourceFolder + "cmp_linkAnnotation02.pdf", destinationFolder, "diff_"));

    }

    @Test
    public void addAndGetLinkAnnotations() throws Exception {
        PdfDocument document = new PdfDocument(new PdfWriter(destinationFolder + "linkAnnotation03.pdf"));

        PdfPage page = document.addNewPage();

        PdfCanvas canvas = new PdfCanvas(page);
        canvas.beginText();
        canvas.setFontAndSize(PdfFontFactory.createFont(StandardFonts.COURIER_BOLD), 14);
        canvas.moveText(100, 600);
        canvas.showText("Click here to go to itextpdf site.");
        canvas.moveText(0, -50);
        canvas.showText("Click here to go to itextpdf blog.");
        canvas.moveText(0, -50);
        canvas.showText("Click here to go to itextpdf FAQ.");
        canvas.endText();
        canvas.release();
        int[] borders = {0, 0, 1};
        page.addAnnotation(new PdfLinkAnnotation(new Rectangle(100, 590, 300, 25)).
                setAction(PdfAction.createURI("http://itextpdf.com")).
                setBorder(new PdfArray(borders)).
                setColor(new PdfArray(new float[]{1, 0, 0})));
        page.addAnnotation(new PdfLinkAnnotation(new Rectangle(100, 540, 300, 25)).
                setAction(PdfAction.createURI("http://itextpdf.com/node")).
                setBorder(new PdfArray(borders)).
                setColor(new PdfArray(new float[]{0, 1, 0})));
        page.addAnnotation(new PdfLinkAnnotation(new Rectangle(100, 490, 300, 25)).
                setAction(PdfAction.createURI("http://itextpdf.com/salesfaq")).
                setBorder(new PdfArray(borders)).
                setColor(new PdfArray(new float[]{0, 0, 1})));
        page.flush();

        document.close();

        Assert.assertNull(new CompareTool().compareByContent(destinationFolder + "linkAnnotation03.pdf", sourceFolder + "cmp_linkAnnotation03.pdf", destinationFolder, "diff_"));


        document = new PdfDocument(new PdfReader(destinationFolder + "linkAnnotation03.pdf"));
        page = document.getPage(1);
        Assert.assertEquals(3, page.getAnnotsSize());
        List<PdfAnnotation> annotations = page.getAnnotations();
        Assert.assertEquals(3, annotations.size());
        PdfLinkAnnotation link = (PdfLinkAnnotation) annotations.get(0);
        Assert.assertEquals(page, link.getPage());
        document.close();
    }

    @Test
    @LogMessages(messages = {@LogMessage(messageTemplate = LogMessageConstant.DESTINATION_NOT_PERMITTED_WHEN_ACTION_IS_SET)})
    public void linkAnnotationActionDestinationTest() throws IOException, InterruptedException {
        String fileName = "linkAnnotationActionDestinationTest.pdf";
        PdfDocument pdfDocument = new PdfDocument(new PdfWriter(new FileOutputStream(destinationFolder + fileName)));
        PdfArray array = new PdfArray();
        array.add(pdfDocument.addNewPage().getPdfObject());
        array.add(PdfName.XYZ);
        array.add(new PdfNumber(36));
        array.add(new PdfNumber(100));
        array.add(new PdfNumber(1));

        PdfDestination dest = PdfDestination.makeDestination(array);

        PdfLinkAnnotation link = new PdfLinkAnnotation(new Rectangle(0, 0, 0, 0));
        link.setAction(PdfAction.createGoTo("abc"));
        link.setDestination(dest);

        pdfDocument.getPage(1).addAnnotation(link);

        pdfDocument.close();

        Assert.assertNull(new CompareTool().compareByContent(destinationFolder + fileName, sourceFolder + "cmp_" + fileName, destinationFolder, "diff_"));
    }

}
