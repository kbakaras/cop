/*  Copyright (c) 2006-2019, the HtmlCleaner Project
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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.jdom2.input.DOMBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

public class DomSerializerTest extends AbstractHtmlCleanerTest {
	
	@Test
	public void removeInvalidTags3() throws Exception{
	    String html="<p><^-^></p>";
	    final TagNode tagNode = new HtmlCleaner().clean(html);
        final CleanerProperties cleanerProperties = new CleanerProperties();
        final Document doc = new DomSerializer(cleanerProperties).createDOM(tagNode);
        assertEquals("&lt;^-^&gt;", doc.getElementsByTagName("p").item(0).getChildNodes().item(0).getTextContent());
	}
	
	// See bug #203
	@Test
	public void parse2() throws Exception
	{
	    String html = "<div foo=\"aaa&quot;bbb&amp;ccc&gt;ddd&lt;eee\">content</div>";
	    String expected = "<div foo=\"aaa&quot;bbb&amp;ccc&gt;ddd&lt;eee\">content</div>";
        final CleanerProperties cleanerProperties = new CleanerProperties();
	    final TagNode tagNode = new HtmlCleaner().clean(html);
	    cleanerProperties.setOmitHtmlEnvelope(true);
	    cleanerProperties.setOmitXmlDeclaration(true);
	    String out = new SimpleXmlSerializer(cleanerProperties).getAsString(html);
	    assertEquals(expected, out);
	}
	
	// See bug #212
	@Test
	public void parse() throws Exception
	{
	    String html = "<?xml version = \"1.0\"?><img src=\"http://xwiki.org?a=&amp;b\"/>";
	    String expected = "<img src=\"http://xwiki.org?a=&amp;b\" />";
        final CleanerProperties cleanerProperties = new CleanerProperties();
	    final TagNode tagNode = new HtmlCleaner().clean(html);
        final Document doc = new DomSerializer(cleanerProperties, true).createDOM(tagNode);
	    assertEquals("http://xwiki.org?a=&amp;b", 
	    		doc.getElementsByTagName("img").item(0).getAttributes().getNamedItem("src").getTextContent());
	    cleanerProperties.setOmitHtmlEnvelope(true);
	    cleanerProperties.setOmitXmlDeclaration(true);
	    String out = new SimpleXmlSerializer(cleanerProperties).getAsString(html);
	    assertEquals(expected, out);
	}
	
	@Test
	public void removeInvalidTags() throws Exception{
	    String html="<p><^-^></p>";
	    final TagNode tagNode = new HtmlCleaner().clean(html);
        final CleanerProperties cleanerProperties = new CleanerProperties();
        final Document doc = new DomSerializer(cleanerProperties, false).createDOM(tagNode);
        assertEquals("&lt;^-^&gt;", doc.getElementsByTagName("p").item(0).getChildNodes().item(0).getTextContent());
	}
	
	@Test
	public void removeInvalidTags2() throws Exception{
	    String html="<p><1o/></p>";
	    final TagNode tagNode = new HtmlCleaner().clean(html);
        final CleanerProperties cleanerProperties = new CleanerProperties();
        final Document doc = new DomSerializer(cleanerProperties, false).createDOM(tagNode);
        assertEquals("&lt;1o/&gt;", doc.getElementsByTagName("p").item(0).getChildNodes().item(0).getTextContent());
	}
	
	@Test
	public void detectUnicodeSpaces() throws Exception{
	    String html="<meta\u00A0property=\"test\" content=\"value\">";
	    String expectedOutput= "test";
	    final TagNode tagNode = new HtmlCleaner().clean(html);
        final CleanerProperties cleanerProperties = new CleanerProperties();
        final Document doc = new DomSerializer(cleanerProperties, false).createDOM(tagNode);
        assertEquals(expectedOutput, doc.getElementsByTagName("meta").item(0).getAttributes().getNamedItem("property").getTextContent());
	}
	
	@Test
	public void preserveUnicodeTest() throws Exception
	{
	    final String nonAsciiWord = "hemförsäkring";
	    final String html = "<html>"
	            + "<body>"
	            + "<p>"
	            + nonAsciiWord
	            + "</p>"
	            + "</body>"
	            + "</html>";

	    final String expectedOutput = 
	            "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
	            + "<html>\n" + 
	            "    <head/>\n" + 
	            "    <body>\n" + 
	            "        <p>" + nonAsciiWord + "</p>\n" + 
	            "    </body>\n" + 
	            "</html>\n"
	            + "";

	        final TagNode tagNode = new HtmlCleaner().clean(html);
	        final CleanerProperties cleanerProperties = new CleanerProperties();
	        final Document doc = new DomSerializer(cleanerProperties, false).createDOM(tagNode);
	        assertEquals(expectedOutput, documentToString(doc));
	}
	
	// See Bug #215
	@Test
	public void invalidXMLElementName() throws ParserConfigurationException{
		
	    final String HTML = "<img srcset=\"<p%20\">";

        final CleanerProperties cleanerProperties = new CleanerProperties();
        //
        // When we set allow to true, then we parse the attribute value as text
        //
        cleanerProperties.setAllowHtmlInsideAttributes(true);
        TagNode tagNode = new HtmlCleaner(cleanerProperties).clean(HTML);
        assertEquals(tagNode.getChildTags()[1].getChildTags()[0].getAttributeByName("srcset"),"<p%20");
        //
        // When we set allow to false, then we identify tags in attribute as new tags, and break
        // into a new tag
        //
        cleanerProperties.setAllowHtmlInsideAttributes(false);
        tagNode = new HtmlCleaner(cleanerProperties).clean(HTML);
        
        //
        // Not an issue for HTML, which accepts pretty much anything in a tag name
        //
        cleanerProperties.setOmitXmlDeclaration(true);
        String output = new SimpleHtmlSerializer(cleanerProperties).getAsString(tagNode);
        assertEquals("<html><head></head><body><img srcset=\"\" /><p%20></p%20></body></html>", output);
        
        //
        // But for XML DOM, we must follow the rules for building valid names, which means
        // getting rid of the % sign
        //
        final Document doc = new DomSerializer(cleanerProperties, false).createDOM(tagNode);
        assertEquals(1, doc.getDocumentElement().getElementsByTagName("p20").getLength());

	}	
	
	@Test
	public void errorChecking() throws ParserConfigurationException{
		TagNode node = cleaner.clean("<p>");
    	DomSerializer ser = new DomSerializer(cleaner.getProperties(), true, true, false);
    	Document document = ser.createDocument(node);
    	assertFalse(document.getStrictErrorChecking());
	}
    
	/**
	 * See issue 108
	 * @throws IOException
	 */
    @Test
    @Ignore
    public void html5doctype() throws Exception{
    	cleaner.getProperties().setUseCdataForScriptAndStyle(true);
    	cleaner.getProperties().setOmitCdataOutsideScriptAndStyle(true);
    	String initial = readFile("src/test/resources/test23.html");
    	TagNode tagNode = cleaner.clean(initial);
    	DomSerializer ser = new DomSerializer(cleaner.getProperties());
    	Document dom = ser.createDOM(tagNode);
    	assertNotNull(dom.getChildNodes().item(0).getChildNodes().item(0));
    	assertEquals("head", dom.getChildNodes().item(0).getChildNodes().item(0).getNodeName());
    }
    
	/**
	 * See issue 127
	 * @throws IOException
	 */
    @Test
    public void rootNodeAttributes() throws Exception{
    	cleaner.getProperties().setUseCdataForScriptAndStyle(true);
    	cleaner.getProperties().setOmitCdataOutsideScriptAndStyle(true);
    	String initial = readFile("src/test/resources/test29.html");
    	TagNode tagNode = cleaner.clean(initial);
    	DomSerializer ser = new DomSerializer(cleaner.getProperties());
    	Document dom = ser.createDOM(tagNode);
    	assertNotNull(dom.getChildNodes().item(0).getChildNodes().item(0));
    	assertEquals("http://unknown.namespace.com", dom.getChildNodes().item(0).getAttributes().getNamedItem("xmlns").getNodeValue());
    	assertEquals("27", dom.getChildNodes().item(0).getAttributes().getNamedItem("id").getNodeValue());
    	//
    	// Check we have a real ID attribute in the DOM and not just a regular attribute
    	//
    	assertEquals("http://unknown.namespace.com", dom.getElementById("27").getAttribute("xmlns"));
    }
    
    @Test
    public void cdata() throws Exception{
    	cleaner.getProperties().setUseCdataForScriptAndStyle(true);
    	cleaner.getProperties().setOmitCdataOutsideScriptAndStyle(true);
    	String initial = "<script> this &gt; that </script>";
    	TagNode tagNode = cleaner.clean(initial);
    	DomSerializer ser = new DomSerializer(cleaner.getProperties(), cleaner.getProperties().isAdvancedXmlEscape(), true);
    	Document dom = ser.createDOM(tagNode);
    	DOMBuilder in = new DOMBuilder();
    	org.jdom2.Document jdomDoc = in.build(dom);
		XMLOutputter outputter = new XMLOutputter(Format.getRawFormat().setEncoding("UTF-8").setLineSeparator("\n"));
		String actual = outputter.outputString(jdomDoc);
        Assert.assertTrue(actual.contains("this > that"));
    }
    
    @Test
    public void cdata2() throws Exception{
    	cleaner.getProperties().setUseCdataForScriptAndStyle(true);
    	cleaner.getProperties().setOmitCdataOutsideScriptAndStyle(true);
    	String initial = "<script> this &gt; that </script>";
    	TagNode tagNode = cleaner.clean(initial);
    	DomSerializer ser = new DomSerializer(cleaner.getProperties(), cleaner.getProperties().isAdvancedXmlEscape(), false);
    	Document dom = ser.createDOM(tagNode);
    	DOMBuilder in = new DOMBuilder();
    	org.jdom2.Document jdomDoc = in.build(dom);
		XMLOutputter outputter = new XMLOutputter(Format.getRawFormat().setEncoding("UTF-8").setLineSeparator("\n"));
		String actual = outputter.outputString(jdomDoc);
        Assert.assertTrue(actual.contains("this &gt; that"));
    }
    
    @Test
    public void escaping() throws Exception { 	
		cleaner.getProperties().setTranslateSpecialEntities(true);
		cleaner.getProperties().setAdvancedXmlEscape(true);
		TagNode tagNode = cleaner.clean("<div>£, &pound; and &#163;</div>");
		DomSerializer ser = new DomSerializer(cleaner.getProperties(), true);
		Document dom = ser.createDOM(tagNode);
		String actual = dom.getElementsByTagName("div").item(0).getTextContent();
		Assert.assertEquals(("£, £ and £"),actual);
    }
    
    @Test
    public void escaping_2() throws Exception {
		cleaner.getProperties().setTranslateSpecialEntities(false);
		TagNode tagNode = cleaner.clean("<div>£, &pound; and &#163;</div>");
		DomSerializer ser = new DomSerializer(cleaner.getProperties(), false);
		Document dom = ser.createDOM(tagNode);
		String actual = dom.getElementsByTagName("div").item(0).getTextContent();
		Assert.assertEquals(("£, &pound; and &#163;"),actual);
    }
    
    @Test
    public void escaping_3() throws Exception {
		cleaner.getProperties().setTranslateSpecialEntities(false);
		TagNode tagNode = cleaner.clean("<div>£, &pound; and &#163;</div>");
		DomSerializer ser = new DomSerializer(cleaner.getProperties(), true);
		Document dom = ser.createDOM(tagNode);
		String actual = dom.getElementsByTagName("div").item(0).getTextContent();
		Assert.assertEquals(("£, &pound; and £"),actual);
    }
    
    @Test
    public void escaping_4() throws Exception {
		cleaner.getProperties().setRecognizeUnicodeChars(false);
		TagNode tagNode = cleaner.clean("<div>£, &pound; and &#163;</div>");
		DomSerializer ser = new DomSerializer(cleaner.getProperties(), true);
		Document dom = ser.createDOM(tagNode);
		String actual = dom.getElementsByTagName("div").item(0).getTextContent();
		Assert.assertEquals(("£, &pound; and &pound;"),actual);
    }
    
    @Test
    public void escapingReservedCharactersTest() throws Exception {
		cleaner.getProperties().setRecognizeUnicodeChars(false);
		TagNode tagNode = cleaner.clean("<div>\" < > &</div>");
		DomSerializer ser = new DomSerializer(cleaner.getProperties(), true);
		Document dom = ser.createDOM(tagNode);
		String actual = dom.getElementsByTagName("div").item(0).getTextContent();
		Assert.assertEquals(("&quot; &lt; &gt; &amp;"),actual);
    }

    //
    // We shouldn't escape any characters in a comment
    //
    @Test
    public void escapingCommentsTest() throws Exception {
		cleaner.getProperties().setRecognizeUnicodeChars(false);
		TagNode tagNode = cleaner.clean("<div><!--\" \' < > &--></div>");
		DomSerializer ser = new DomSerializer(cleaner.getProperties(), true);
		Document dom = ser.createDOM(tagNode);
		String actual = dom.getElementsByTagName("div").item(0).getChildNodes().item(0).getTextContent();
		Assert.assertEquals(("\" \' < > &"),actual);
    }

    
    @Test
	public void ncr() throws Exception {

		cleaner.getProperties().setOmitComments(true);
		cleaner.getProperties().setNamespacesAware(false);
		cleaner.getProperties().setUseCdataForScriptAndStyle(true);
		cleaner.getProperties().setTranslateSpecialEntities(true);

		TagNode tagNode = cleaner.clean("<div> &#8217; &#1078; &#253; &#247; &divide; </div>");
		DomSerializer ser = new DomSerializer(cleaner.getProperties(), cleaner.getProperties().isAdvancedXmlEscape(), false);
		Document dom = ser.createDOM(tagNode);
		DOMBuilder in = new DOMBuilder();
		org.jdom2.Document jdomDoc = in.build(dom);
		XMLOutputter outputter = new XMLOutputter(Format.getRawFormat().setEncoding("UTF-8").setLineSeparator("\n"));
		String actual = outputter.outputString(jdomDoc);

		Assert.assertTrue(actual.contains("’ ж ý ÷ ÷"));
	}
	
}
