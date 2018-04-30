/*
    This file is part of the iText (R) project.
    Copyright (c) 1998-2018 iText Group NV
    Authors: iText Software.

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License version 3
    as published by the Free Software Foundation with the addition of the
    following permission added to Section 15 as permitted in Section 7(a):
    FOR ANY PART OF THE COVERED WORK IN WHICH THE COPYRIGHT IS OWNED BY
    ITEXT GROUP. ITEXT GROUP DISCLAIMS THE WARRANTY OF NON INFRINGEMENT
    OF THIRD PARTY RIGHTS

    This program is distributed in the hope that it will be useful, but
    WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
    or FITNESS FOR A PARTICULAR PURPOSE.
    See the GNU Affero General Public License for more details.
    You should have received a copy of the GNU Affero General Public License
    along with this program; if not, see http://www.gnu.org/licenses or write to
    the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
    Boston, MA, 02110-1301 USA, or download the license from the following URL:
    http://itextpdf.com/terms-of-use/

    The interactive user interfaces in modified source and object code versions
    of this program must display Appropriate Legal Notices, as required under
    Section 5 of the GNU Affero General Public License.

    In accordance with Section 7(b) of the GNU Affero General Public License,
    a covered work must retain the producer line in every PDF that is created
    or manipulated using iText.

    You can be released from the requirements of the license by purchasing
    a commercial license. Buying such a license is mandatory as soon as you
    develop commercial activities involving the iText software without
    disclosing the source code of your own applications.
    These activities include: offering paid services to customers as an ASP,
    serving PDFs on the fly in a web application, shipping iText with a closed
    source product.

    For more information, please contact iText Software Corp. at this
    address: sales@itextpdf.com
 */
package com.itextpdf.svg.processors.impl;

import com.itextpdf.styledxmlparser.css.ICssResolver;
import com.itextpdf.styledxmlparser.resolver.resource.ResourceResolver;
import com.itextpdf.svg.css.impl.DefaultSvgStyleResolver;
import com.itextpdf.svg.processors.ISvgConverterProperties;
import com.itextpdf.svg.renderers.factories.DefaultSvgNodeRendererFactory;
import com.itextpdf.svg.renderers.factories.ISvgNodeRendererFactory;

import java.nio.charset.StandardCharsets;

/**
 * Default and fallback implementation of {@link ISvgConverterProperties} for
 * {@link DefaultSvgProcessor}
 */
public class DefaultSvgConverterProperties implements ISvgConverterProperties {

    private ResourceResolver resourceResolver;
    private ICssResolver cssResolver;
    private ISvgNodeRendererFactory rendererFactory;

    /**
     * Creates a DefaultSvgConverterProperties object. Instantiates its members, ICssResolver and ISvgNodeRenderer, to its default implementations.
     */
    public DefaultSvgConverterProperties(){
        this.rendererFactory = new DefaultSvgNodeRendererFactory();
        this.cssResolver = new DefaultSvgStyleResolver();
        this.resourceResolver = new ResourceResolver("");
    }

    @Override
    public ICssResolver getCssResolver() {
        return this.cssResolver;
    }

    @Override
    public ISvgNodeRendererFactory getRendererFactory() {
        return this.rendererFactory;
    }

    @Override
    public String getCharset() {
        // may also return null, but null will always default to UTF-8 in JSoup
        return StandardCharsets.UTF_8.name();
    }

    @Override
    public ResourceResolver getResourceResolver() {
        return this.resourceResolver;
    }
}
