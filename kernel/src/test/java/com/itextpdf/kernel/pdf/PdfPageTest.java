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
