/*  Copyright (c) 2006-2013, the HtmlCleaner Project
    All rights reserved.

    Redistribution and use of this software in source and binary forms,
    with or without modification, are permitted provided that the following
    conditions are met:

    * Redistributions of source code must retain the above
      copyright notice, this list of conditions and the
      following disclaimer.

    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the
      following disclaimer in the documentation and/or other
      materials provided with the distribution.

    * The name of HtmlCleaner may not be used to endorse or promote
      products derived from this software without specific prior
      written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
    ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
    SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
    INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
    CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
    POSSIBILITY OF SUCH DAMAGE.
*/
package org.htmlcleaner;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

public class SVGTest extends AbstractHtmlCleanerTest{
	
	@Test 
	public void nestedSVG()
    {
        String html = "<!DOCTYPE html>\n"
            + "<html lang=\"en\">\n"
            + "<head>\n"
            + "</head>\n"
            + "<body itemscope itemtype=\"http://schema.org/WebPage\">\n"
            + "<svg xmlns=\"http://www.w3.org/2000/\">\n"
            + "    <svg></svg>\n"
            + "</svg>\n"
            + "</body>\n"
            + "</html>";
        new HtmlCleaner().clean(html);

        html = "<!DOCTYPE html>\n"
            + "<html lang=\"en\">\n"
            + "<head>\n"
            + "</head>\n"
            + "<body itemscope itemtype=\"http://schema.org/WebPage\">\n"
            + "<svg xmlns=\"http://www.w3.org/2000/svg\">\n"
            + "    <circle cx=\"50\" cy=\"50\" r=\"40\" stroke=\"black\" stroke-width=\"3\" fill=\"red\" />\n"
            + "</svg>\n"
            + "</body>\n"
            + "</html>";
        new HtmlCleaner().clean(html);

        html = "<!DOCTYPE html>\n"
            + "<html lang=\"en\">\n"
            + "<head>\n"
            + "</head>\n"
            + "<body itemscope itemtype=\"http://schema.org/WebPage\">\n"
            + "<svg xmlns=\"http://www.w3.org/2000/svg\">\n"
            + "    <svg></svg>\n"
            + "</svg>\n"
            + "</body>\n"
            + "</html>";
        new HtmlCleaner().clean(html);
    }
	
	@Test
	public void svgCloseAssumedNS4() throws Exception{
		String html="<html><head></head><body><svg><h3>Title</h3><div>text</div></body></html>";
		CleanerProperties props = new CleanerProperties();
	    props.setNamespacesAware(true);
	    props.setOmitXmlDeclaration(true);
	    HtmlCleaner cleaner = new HtmlCleaner(props);
		String cleaned = new SimpleHtmlSerializer(cleaner.getProperties(), false).getAsString(cleaner.clean(html));
		assertEquals("<html><head></head><body><svg></svg><h3>Title</h3><div>text</div></body></html>", cleaned);
	}
	
	@Test
	@Ignore // This is a tricky one as "a" is allowed in SVG, so the rest is assumed to be OK.
	public void svgCloseAssumedNS3() throws Exception{
		String html="<html><head></head><body><svg><a><br><h3>Title</h3><div>text</cite></div></a></body></html>";
		CleanerProperties props = new CleanerProperties();
	    props.setNamespacesAware(true);
	    props.setOmitXmlDeclaration(true);
	    HtmlCleaner cleaner = new HtmlCleaner(props);
		String cleaned = new SimpleHtmlSerializer(cleaner.getProperties(), false).getAsString(cleaner.clean(html));
		assertEquals("<html><head></head><body><svg></svg><a><br /><h3>Title</h3><div>text</div></a></body></html>", cleaned);
	}
	
	@Test
	public void svgCloseAssumedNS2() throws Exception{
		String html="<html><head></head><body><svg><title></title></svg><a><br><h3>Title</h3><div>text</cite></div></a></body></html>";
		CleanerProperties props = new CleanerProperties();
	    props.setNamespacesAware(true);
	    props.setOmitXmlDeclaration(true);
	    HtmlCleaner cleaner = new HtmlCleaner(props);
		String cleaned = new SimpleHtmlSerializer(cleaner.getProperties(), false).getAsString(cleaner.clean(html));
		assertEquals("<html><head></head><body><svg><title></title></svg><a><br /><h3>Title</h3><div>text</div></a></body></html>", cleaned);
	}
	
	@Test
	public void svgCloseAssumedNS() throws Exception{
		String html="<html><head></head><body><svg></svg><a><br><h3>Title</h3><div>text</cite></div></a></body></html>";
		CleanerProperties props = new CleanerProperties();
	    props.setNamespacesAware(true);
	    props.setOmitXmlDeclaration(true);
	    HtmlCleaner cleaner = new HtmlCleaner(props);
		String cleaned = new SimpleHtmlSerializer(cleaner.getProperties(), false).getAsString(cleaner.clean(html));
		assertEquals("<html><head></head><body><svg></svg><a><br /><h3>Title</h3><div>text</div></a></body></html>", cleaned);
	}
	
	@Test
	public void missingSVGNamespace() throws IOException {
		String initial = "<html><head><title>Title of document</title></head><body><svg><title>A big circle.</title></svg></body></html>";
		String expected = "<html>\n<head><title>Title of document</title></head>\n<body><svg><title>A big circle.</title></svg></body></html>";
		assertCleaned(initial, expected);
	}

	@Test
	public void preserveSVGtags() throws IOException{
		
        cleaner.getProperties().setOmitXmlDeclaration(false);
        cleaner.getProperties().setOmitDoctypeDeclaration(false);
        cleaner.getProperties().setOmitUnknownTags(true);
        cleaner.getProperties().setNamespacesAware(true);
        
		String initial = readFile("src/test/resources/test18.html");
		String expected = readFile("src/test/resources/test18_expected.html"); 
		
		assertCleaned(initial,expected);
	}
	
	@Test
	public void preserveSVGtags2() throws IOException{
		
        cleaner.getProperties().setOmitXmlDeclaration(false);
        cleaner.getProperties().setOmitDoctypeDeclaration(false);
        cleaner.getProperties().setOmitUnknownTags(true);
        cleaner.getProperties().setNamespacesAware(true);
        
		String initial = readFile("src/test/resources/test19.html");
		String expected = readFile("src/test/resources/test19_expected.html"); 
		assertCleaned(initial,expected);
	}

	
	@Test
	public void preserveSVGtags3() throws IOException{
		
        cleaner.getProperties().setOmitXmlDeclaration(false);
        cleaner.getProperties().setOmitDoctypeDeclaration(false);
        cleaner.getProperties().setNamespacesAware(true);
        
		String initial = readFile("src/test/resources/test20.html");
		String expected = readFile("src/test/resources/test20_expected.html"); 

		assertCleaned(initial,expected);
	}
	
	@Test
	public void preserveSVGtagsWithTitle() throws IOException{
		
        cleaner.getProperties().setOmitXmlDeclaration(false);
        cleaner.getProperties().setOmitDoctypeDeclaration(false);
        cleaner.getProperties().setNamespacesAware(true);
        cleaner.getProperties().setOmitUnknownTags(true);
        
		String initial = readFile("src/test/resources/test21.html");
		String expected = readFile("src/test/resources/test21_expected.html"); 

		assertCleaned(initial,expected);
	}
	
	@Test
	public void preserveSVGstylesInPlace() throws IOException{
		
        cleaner.getProperties().setOmitXmlDeclaration(false);
        cleaner.getProperties().setOmitDoctypeDeclaration(false);
        cleaner.getProperties().setNamespacesAware(true);
        cleaner.getProperties().setOmitUnknownTags(true);
        
		String initial = readFile("src/test/resources/test25.html");
		String expected = readFile("src/test/resources/test25_expected.html"); 

		assertCleaned(initial,expected);
	}
}
