package com.itextpdf.kernel.pdf;

import com.itextpdf.io.source.ByteArrayOutputStream;
import com.itextpdf.kernel.pdf.annot.PdfAnnotation;
import com.itextpdf.test.ExtendedITextTest;
import com.itextpdf.test.annotations.type.UnitTest;

import java.util.Arrays;
import java.util.Collections;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(UnitTest.class)
public class PdfPageTest extends ExtendedITextTest {
    private PdfDocument dummyDoc;

    @Before
    public void before() {
        dummyDoc = new PdfDocument(new PdfWriter(new ByteArrayOutputStream()));
        dummyDoc.addNewPage();
    }

    @After
    public void after() {
        dummyDoc.close();
    }

    @Test
    public void removeLastAnnotationTest() {
        PdfDictionary pageDictionary = new PdfDictionary();
        pageDictionary.makeIndirect(dummyDoc);
        PdfDictionary annotDictionary = new PdfDictionary();
        pageDictionary.put(PdfName.Annots, new PdfArray(Collections.singletonList(annotDictionary)));

        PdfPage pdfPage = new PdfPage(pageDictionary);
        pdfPage.removeAnnotation(PdfAnnotation.makeAnnotation(annotDictionary));

        Assert.assertTrue(pdfPage.getAnnotations().isEmpty());
        Assert.assertFalse(pageDictionary.containsKey(PdfName.Annots));
        Assert.assertTrue(pageDictionary.isModified());
    }

    @Test
    public void removeAnnotationTest() {
        PdfDictionary pageDictionary = new PdfDictionary();
        simulateIndirectState(pageDictionary);
        PdfDictionary annotDictionary1 = new PdfDictionary();
        PdfDictionary annotDictionary2 = new PdfDictionary();
        pageDictionary.put(PdfName.Annots, new PdfArray(
                Arrays.asList(annotDictionary1, annotDictionary2))
        );

        PdfPage pdfPage = new PdfPage(pageDictionary);
        pdfPage.removeAnnotation(PdfAnnotation.makeAnnotation(annotDictionary1));

        Assert.assertEquals(1, pdfPage.getAnnotations().size());
        Assert.assertTrue(pageDictionary.isModified());
    }

    @Test
    public void removeAnnotationWithIndirectAnnotsArrayTest() {
        PdfDictionary pageDictionary = new PdfDictionary();
        simulateIndirectState(pageDictionary);
        PdfDictionary annotDictionary1 = new PdfDictionary();
        PdfDictionary annotDictionary2 = new PdfDictionary();
        PdfArray annotsArray = new PdfArray(
                Arrays.asList(annotDictionary1, annotDictionary2)
        );
        simulateIndirectState(annotsArray);

        pageDictionary.put(PdfName.Annots, annotsArray);

        PdfPage pdfPage = new PdfPage(pageDictionary);
        pdfPage.removeAnnotation(PdfAnnotation.makeAnnotation(annotDictionary1));

        Assert.assertEquals(1, pdfPage.getAnnotations().size());
        Assert.assertFalse(pageDictionary.isModified());
        Assert.assertTrue(annotsArray.isModified());
    }

    /**
     * Simulates indirect state of object making sure it is not marked as modified.
     */
    private void simulateIndirectState(PdfObject obj) {
        obj.setIndirectReference(new PdfIndirectReference(dummyDoc, 0));
    }
}
