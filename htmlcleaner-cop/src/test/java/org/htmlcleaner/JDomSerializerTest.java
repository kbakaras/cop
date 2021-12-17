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

import org.jdom2.Document;
import org.jdom2.Namespace;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.Test;

public class JDomSerializerTest extends AbstractHtmlCleanerTest {

	//
	// Test that we create valid element names
	//
	@Test
	public void elementNames() throws IOException{
		String initial = "<img srcset=\"<p%20\">";
		String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<html><head /><body><img srcset=\"\" /><p20 /></body></html>\n";
		CleanerProperties props = new CleanerProperties();
		props.setAddNewlineToHeadAndBody(false);
		TagNode tagNode = new HtmlCleaner(props).clean(initial);
		Document doc = new JDomSerializer(props, true).createJDom(tagNode);
		XMLOutputter outputter = new XMLOutputter(Format.getRawFormat().setEncoding("UTF-8").setLineSeparator("\n"));
		String output = outputter.outputString(doc);
		assertEquals(expected, output);		
	}
	
	/**
	 * Tests that we comment CDATA in JDom
	 * @throws IOException
	 */
	@Test
	public void safeCData1() throws IOException{
		String initial = "<head><script type=\"text/javascript\"><![CDATA[alert(\"Hello World\")]]></script></head>";
		String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<html><head><script type=\"text/javascript\">/*<![CDATA[*/\nalert(\"Hello World\")\n/*]]>*/</script></head><body /></html>\n";
		CleanerProperties props = new CleanerProperties();
		props.setOmitCdataOutsideScriptAndStyle(true);
		props.setAddNewlineToHeadAndBody(false);
		TagNode tagNode = new HtmlCleaner(props).clean(initial);
		Document doc = new JDomSerializer(props, true).createJDom(tagNode);
		XMLOutputter outputter = new XMLOutputter(Format.getRawFormat().setEncoding("UTF-8").setLineSeparator("\n"));
		String output = outputter.outputString(doc);
		assertEquals(expected, output);		
	}
	
	/**
	 * Tests that we comment CDATA in JDom; in this case preserving existing comments
	 * @throws IOException
	 */
	@Test
	public void safeCData2() throws IOException{
		String initial = "<head><script type=\"text/javascript\">//<![CDATA[\nalert(\"Hello World\")\n//]]></script></head>";
		String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<html><head><script type=\"text/javascript\">/*<![CDATA[*/\nalert(\"Hello World\")\n/*]]>*/</script></head><body /></html>\n";
		CleanerProperties props = new CleanerProperties();
		props.setOmitCdataOutsideScriptAndStyle(true);
		props.setAddNewlineToHeadAndBody(false);
		TagNode tagNode = new HtmlCleaner(props).clean(initial);
		Document doc = new JDomSerializer(props, true).createJDom(tagNode);
		XMLOutputter outputter = new XMLOutputter(Format.getRawFormat().setEncoding("UTF-8").setLineSeparator("\n"));
		String output = outputter.outputString(doc);
		assertEquals(expected, output);		
	}
	
	/**
	 * Tests that we comment CDATA in JDom; in this case that we normalise comment style
	 * @throws IOException
	 */
	@Test
	public void safeCData3() throws IOException{
		String initial = "<head><script type=\"text/javascript\">/*<![CDATA[*/alert(\"Hello World\")\n/*]]>*/</script></head>";
		String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<html><head><script type=\"text/javascript\">/*<![CDATA[*/\nalert(\"Hello World\")\n/*]]>*/</script></head><body /></html>\n";
		CleanerProperties props = new CleanerProperties();
		props.setOmitCdataOutsideScriptAndStyle(true);
		props.setAddNewlineToHeadAndBody(false);
		TagNode tagNode = new HtmlCleaner(props).clean(initial);
		Document doc = new JDomSerializer(props, true).createJDom(tagNode);
		XMLOutputter outputter = new XMLOutputter(Format.getRawFormat().setEncoding("UTF-8").setLineSeparator("\n"));
		String output = outputter.outputString(doc);
		assertEquals(expected, output);		
	}
	
	/**
	 * Tests that we comment CDATA in JDom; in this case a more complex example
	 * @throws IOException
	 */
	@Test
	public void safeCData4() throws IOException{
		String initial = readFile("src/test/resources/test33.html");
		String expected = readFile("src/test/resources/test33_expected.html");;
		CleanerProperties props = new CleanerProperties();
		props.setOmitCdataOutsideScriptAndStyle(true);
		props.setAddNewlineToHeadAndBody(false);
		TagNode tagNode = new HtmlCleaner(props).clean(initial);
		Document doc = new JDomSerializer(props, true).createJDom(tagNode);
		XMLOutputter outputter = new XMLOutputter(Format.getRawFormat().setEncoding("UTF-8").setLineSeparator("\n"));
		String output = outputter.outputString(doc);
		assertEquals(expected, output);		
	}
	
	/**
	 * Tests that we comment CDATA in JDom
	 * @throws IOException
	 */
	@Test
	public void safeCData5() throws IOException{
		String initial = "<head><script>&lt;&gt;</script></head>";
		String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<html><head><script>/*<![CDATA[*/\n<>\n/*]]>*/</script></head><body /></html>\n";
		CleanerProperties props = new CleanerProperties();
		props.setOmitCdataOutsideScriptAndStyle(true);
		props.setUseCdataForScriptAndStyle(true);
		props.setDeserializeEntities(true);
		props.setAddNewlineToHeadAndBody(false);
		TagNode tagNode = new HtmlCleaner(props).clean(initial);
		Document doc = new JDomSerializer(props, true).createJDom(tagNode);
		XMLOutputter outputter = new XMLOutputter(Format.getRawFormat().setEncoding("UTF-8").setLineSeparator("\n"));
		String output = outputter.outputString(doc);
		assertEquals(expected, output);		
	}
	
	/**
	 * Tests that we comment CDATA in JDom; this test uses CSS 
	 * @throws IOException
	 */
	@Test
	public void safeCData6() throws IOException{
		String initial = "<head><style type=\"text/css\"><![CDATA[\na { color: red; }\n]]></style></head>";
		String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<html><head><style type=\"text/css\">/*<![CDATA[*/\na { color: red; }\n/*]]>*/</style></head><body /></html>\n";
		CleanerProperties props = new CleanerProperties();
		props.setOmitCdataOutsideScriptAndStyle(true);
		props.setUseCdataForScriptAndStyle(true);
		props.setAddNewlineToHeadAndBody(false);
		TagNode tagNode = new HtmlCleaner(props).clean(initial);
		Document doc = new JDomSerializer(props, true).createJDom(tagNode);
		XMLOutputter outputter = new XMLOutputter(Format.getRawFormat().setEncoding("UTF-8").setLineSeparator("\n"));
		String output = outputter.outputString(doc);
		assertEquals(expected, output);		
	}

	/**
	 * See issue #95
	 */
	@Test
	public void testNPE(){
		String validhtml5StringCode = "<html></html>";
		CleanerProperties props = new CleanerProperties();
		props.setOmitHtmlEnvelope(true);
		TagNode tagNode = new HtmlCleaner(props).clean(validhtml5StringCode);
		new JDomSerializer(props, true).createJDom(tagNode);
	}
	
	/**
	 * See issue 106
	 * @throws IOException
	 */
    @Test
    public void CDATA() throws Exception{
    	cleaner.getProperties().setUseCdataForScriptAndStyle(true);
    	cleaner.getProperties().setOmitCdataOutsideScriptAndStyle(true);
    	String initial = readFile("src/test/resources/test22.html");
    	TagNode tagNode = cleaner.clean(initial);
    	JDomSerializer ser = new JDomSerializer(cleaner.getProperties());
    	Document doc = ser.createJDom(tagNode);
    	assertEquals("org.jdom2.CDATA", doc.getRootElement().getChild("head").getChild("script").getContent().get(1).getClass().getName());
    }
    
	/**
	 * See issue 106
	 * @throws IOException
	 */
    @Test
    public void noCDATA() throws Exception{
    	cleaner.getProperties().setUseCdataForScriptAndStyle(false);
    	cleaner.getProperties().setOmitCdataOutsideScriptAndStyle(true);
    	String initial = readFile("src/test/resources/test22.html");
    	TagNode tagNode = cleaner.clean(initial);
    	JDomSerializer ser = new JDomSerializer(cleaner.getProperties());
    	Document doc = ser.createJDom(tagNode);
    	assertEquals("org.jdom2.Text", doc.getRootElement().getChild("head").getChild("script").getContent().get(0).getClass().getName());
    }
    
    /**
     * Test we handle foreign markup OK
     * @throws Exception
     */
    @Test
    public void namespaces() throws Exception{
	    cleaner.getProperties().setNamespacesAware(true);
		String initial = readFile("src/test/resources/test21.html");
		TagNode tagNode = cleaner.clean(initial);
		JDomSerializer ser = new JDomSerializer(cleaner.getProperties());
		Document doc = ser.createJDom(tagNode);
		
		//
		// These will fail with an NPE if the namespaces are not correct
		//
		doc.getRootElement().getChild("body", Namespace.getNamespace("http://www.w3.org/1999/xhtml")).getNamespaceURI();
		doc.getRootElement().getChild("body", Namespace.getNamespace("http://www.w3.org/1999/xhtml")).getChild("svg", Namespace.getNamespace("http://www.w3.org/2000/svg")).getNamespaceURI();
		doc.getRootElement().getChild("body", Namespace.getNamespace("http://www.w3.org/1999/xhtml")).getChild("svg", Namespace.getNamespace("http://www.w3.org/2000/svg")).getChild("title", Namespace.getNamespace("http://www.w3.org/2000/svg"));

    }
}
