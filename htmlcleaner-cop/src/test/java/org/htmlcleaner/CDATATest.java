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
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Ignore;
import org.junit.Test;

public class CDATATest extends AbstractHtmlCleanerTest {
	
	/**
	 * Test for bug #189
	 * @throws Exception 
	 */
	@Test
	public void UnclosedCDATA() throws Exception{
		String html = "<script><![CDATA[";
		String x = "";
		for (int i = 0; i < 2048; i++){x+="x";};
		html += x;
		html += "</script><p>Test</p>";
		
		String expected = "<script>/*<![CDATA[*/\n" + x + "\n/*]]>*/</script><p>Test</p>";
		
		cleaner.getProperties().setOmitHtmlEnvelope(true);
		assertCleaned(html, expected);
		cleaner.getProperties().setOmitHtmlEnvelope(false);
	}
	
	/**
	 * Test for bug #211
	 * This passes, but is marked @Ignore because it takes a while to run. Comment
	 * out ignore and run this test before making any builds.
	 * @throws Exception 
	 */
	@Ignore
	@Test
	public void UnclosedCDATA2() throws Exception{
		String html = "<script><![CDATA[";
		String x = "";
		for (int i = 0; i < 513*1024; i++){x+="x";};
		html += x;
		html += "</script><p>Test</p>";
		
		String expected = "<script>/*<![CDATA[*/\n" + x + "\n/*]]>*/</script><p>Test</p>";
		
		cleaner.getProperties().setOmitHtmlEnvelope(true);
		assertCleaned(html, expected);
		cleaner.getProperties().setOmitHtmlEnvelope(false);
	}
	
	
	//
	// Test for bug #185
	//
	@Test
	public void noEndTokenLong() throws Exception{
		String initial = "<script><![CDATA[";
		String x = "";
		for (int i = 0; i < 1024; i++){x+="x";};
		String expected = "<script>"+x+"</script>";
		String expectedXml = "<script>/*<![CDATA[*/\n" + x + "\n/*]]>*/</script>";
		
		cleaner.getProperties().setOmitHtmlEnvelope(true);
		assertCleanedHtml(initial+x, expected);
		assertCleaned(initial+x, expectedXml);
		assertCleanedJDom(initial+x, expectedXml);
		cleaner.getProperties().setOmitHtmlEnvelope(false);
	}
	
	//
	// Test for bug #189
	//
	@Test
	public void noEndTokenReallyLong() throws Exception{
		String initial = "<script><![CDATA[";
		String x = "";
		for (int i = 0; i < 4096; i++){x+="x";};
		String expected = "<script>"+x+"</script>";
		String expectedXml = "<script>/*<![CDATA[*/\n" + x + "\n/*]]>*/</script>";
		
		cleaner.getProperties().setOmitHtmlEnvelope(true);
		assertCleanedHtml(initial+x, expected);
		assertCleaned(initial+x, expectedXml);
		assertCleanedJDom(initial+x, expectedXml);
		cleaner.getProperties().setOmitHtmlEnvelope(false);
	}
	
	/**
	 * This is to test issue #134
	 * @throws IOException 
	 */
	@Test
	public void strayEndTagInCDATA() throws IOException{
		String initial = readFile("src/test/resources/test31.html");
		cleaner.clean(initial);
	}

	/**
	 * Tests that we escape CDATA in regular HTML content
	 * @throws IOException
	 */
	@Test
	public void NotReallyCData() throws IOException{
		String initial = "<p><![CDATA ]]> is sometimes used in XML";
		String expected = "<html>\n<head />\n<body><p>&lt;![CDATA ]]&gt; is sometimes used in XML</p></body></html>";
		assertCleaned(initial, expected);
	}
	
	/**
	 * This is a simple no-op test; when we use a HTML serializer we don't
	 * automatically wrap the contents of script tags in a CDATA, as we do with
	 * the XML serializers
	 * 
	 * @throws IOException
	 */
	@Test
	public void NoCData() throws IOException{
		CleanerProperties cleanerProperties = new CleanerProperties();
        cleanerProperties.setOmitXmlDeclaration(true);
        cleanerProperties.setOmitDoctypeDeclaration(true);
        cleanerProperties.setIgnoreQuestAndExclam(false);
        cleanerProperties.setUseCdataForScriptAndStyle(true);
        this.cleaner = new HtmlCleaner(cleanerProperties);
        this.serializer = new SimpleHtmlSerializer(cleaner.getProperties());
        
		String initial = "<html><head><script>function testNoOp(){<>}</script></head><body></body></html>";
		String expected = initial;
		assertCleaned(initial, expected);
	}
	
	/**
	 * In this test the script has no CDATA, an unescaped CDATAsection in a
	 * script tag, and there is also an incorrect CDATA declaration in a
	 * paragraph tag.
	 * 
	 * @throws IOException
	 */
    @Test
    public void CDATAmixed() throws IOException{
		String initial = readFile("src/test/resources/test11.html");
		String expected = readFile("src/test/resources/test11_expected.html");
		assertCleaned(initial, expected);
    }
    
    @Test
    public void CDATAandDocType() throws IOException{
    	
        CleanerProperties cleanerProperties = new CleanerProperties();
        cleanerProperties.setOmitXmlDeclaration(false);
        cleanerProperties.setOmitDoctypeDeclaration(false);
        cleanerProperties.setIgnoreQuestAndExclam(false);
        this.cleaner = new HtmlCleaner(cleanerProperties);
        this.serializer = new SimpleXmlSerializer(cleaner.getProperties());

		String initial = readFile("src/test/resources/test12.html");
		String expected = readFile("src/test/resources/test12_expected.html");

		assertCleaned(initial, expected);
    }

    @Test
    public void scriptAndCData() throws IOException
    {

        CleanerProperties cleanerProperties = new CleanerProperties();
        cleanerProperties.setOmitXmlDeclaration(false);
        cleanerProperties.setOmitDoctypeDeclaration(false);
        cleanerProperties.setIgnoreQuestAndExclam(false);
        cleanerProperties.setAddNewlineToHeadAndBody(false);
        cleanerProperties.setUseCdataFor("script,style,altscript");
        this.cleaner = new HtmlCleaner(cleanerProperties);
        this.serializer = new SimpleXmlSerializer(cleaner.getProperties());

        
        assertHTML("<script type=\"text/javascript\">/*<![CDATA[*/\nalert(\"Hello World\")\n/*]]>*/</script>", 
        		"<script type=\"text/javascript\">//<![CDATA[\nalert(\"Hello World\")\n//]]></script>");
        
        assertHTML("<script type=\"text/javascript\">/*<![CDATA[*/\nalert(\"Hello World\")\n/*]]>*/</script>", 
        		"<script type=\"text/javascript\">//<![CDATA[\nalert(\"Hello World\")\n//]]></script>");

        assertHTML("<script type=\"text/javascript\">/*<![CDATA[*/\nalert(\"Hello World\")\n/*]]>*/</script>", 
        		"<script type=\"text/javascript\"><![CDATA[\nalert(\"Hello World\")\n]]></script>");

        assertHTML("<style type=\"text/css\">/*<![CDATA[*/\na { color: red; }\n/*]]>*/</style>", 
        		"<style type=\"text/css\"><![CDATA[\na { color: red; }\n]]></style>");


        assertHTML("<script type=\"text/javascript\">/*<![CDATA[*/\n// Comment \nalert(\"Hello World\")\n //\n/*]]>*/</script>",
        "<script type=\"text/javascript\">// Comment \nalert(\"Hello World\")\n //\n</script>");

        assertHTML("<script type=\"text/javascript\">/*<![CDATA[*/\nalert(\"Hello World\")\n/*]]>*/</script>",
        "<script type=\"text/javascript\"><![CDATA[\nalert(\"Hello World\")\n]]></script>");

        assertHTML("<script type=\"text/javascript\">/*<![CDATA[*/\n//\nalert(\"Hello World\")\n// \n/*]]>*/</script>",
            "<script type=\"text/javascript\"><![CDATA[\n//\nalert(\"Hello World\")\n// \n]]></script>");

        assertHTML("<script type=\"text/javascript\">/*<![CDATA[*/\n//\nalert(\"Hello World\")\n// \n/*]]>*/</script>",
        "<script type=\"text/javascript\">//<![CDATA[\n//\nalert(\"Hello World\")\n// ]]></script>");

        assertHTML("<script type=\"text/javascript\">/*<![CDATA[*/\n"
                + "// \n"
                + "function escapeForXML(origtext) {\n"
                + "   return origtext.replace(/\\&/g,'&'+'amp;').replace(/</g,'&'+'lt;')\n"
                + "       .replace(/>/g,'&'+'gt;').replace(/\'/g,'&'+'apos;').replace(/\"/g,'&'+'quot;');"
                + "}\n"
                + "// \n/*]]>*/"
                + "</script>", "<script type=\"text/javascript\">\n"
                + "// <![CDATA[\n"
                + "function escapeForXML(origtext) {\n"
                + "   return origtext.replace(/\\&/g,'&'+'amp;').replace(/</g,'&'+'lt;')\n"
                + "       .replace(/>/g,'&'+'gt;').replace(/\'/g,'&'+'apos;').replace(/\"/g,'&'+'quot;');"
                + "}\n"
                + "// ]]>\n"
                + "</script>");

        assertHTML("<script>/*<![CDATA[*/\n<>\n/*]]>*/</script>", "<script><></script>");

        assertHTML("<altscript>/*<![CDATA[*/\n<>\n/*]]>*/</altscript>", "<altscript><></altscript>");
        
        assertHTML(
        		"<script>/*<![CDATA[*/\nbanana(); //-->\n/*]]>*/</script><script>/*<![CDATA[*/\ntwo\n/*]]>*/</script>",
       		    "<script>//<![CDATA[\nbanana(); //--></script><script>two</script>"
        );
    }
    
    @Test
    public void scriptAndCDataDom() throws IOException, ParserConfigurationException
    {

        CleanerProperties cleanerProperties = new CleanerProperties();
        cleanerProperties.setOmitXmlDeclaration(false);
        cleanerProperties.setOmitDoctypeDeclaration(false);
        cleanerProperties.setIgnoreQuestAndExclam(false);
        cleanerProperties.setAddNewlineToHeadAndBody(false);
        cleanerProperties.setUseCdataFor("script,style,altscript");
        this.cleaner = new HtmlCleaner(cleanerProperties);
        
        assertHTMLUsingDomSerializer("<script type=\"text/javascript\">/*<![CDATA[*/\nalert(\"Hello World\")\n/*]]>*/</script>", 
        		"<script type=\"text/javascript\">//<![CDATA[\nalert(\"Hello World\")\n//]]></script>");
        
        assertHTMLUsingDomSerializer("<script type=\"text/javascript\">/*<![CDATA[*/\nalert(\"Hello World\")\n/*]]>*/</script>", 
        		"<script type=\"text/javascript\">//<![CDATA[\nalert(\"Hello World\")\n//]]></script>");

        assertHTMLUsingDomSerializer("<script type=\"text/javascript\">/*<![CDATA[*/\nalert(\"Hello World\")\n/*]]>*/</script>", 
        		"<script type=\"text/javascript\"><![CDATA[\nalert(\"Hello World\")\n]]></script>");

        assertHTMLUsingDomSerializer("<style type=\"text/css\">/*<![CDATA[*/\na { color: red; }\n/*]]>*/</style>", 
        		"<style type=\"text/css\"><![CDATA[\na { color: red; }\n]]></style>");


        assertHTMLUsingDomSerializer("<script type=\"text/javascript\">/*<![CDATA[*/\n// Comment \nalert(\"Hello World\")\n //\n/*]]>*/</script>",
        "<script type=\"text/javascript\">// Comment \nalert(\"Hello World\")\n //\n</script>");

        assertHTMLUsingDomSerializer("<script type=\"text/javascript\">/*<![CDATA[*/\nalert(\"Hello World\")\n/*]]>*/</script>",
        "<script type=\"text/javascript\"><![CDATA[\nalert(\"Hello World\")\n]]></script>");

        assertHTMLUsingDomSerializer("<script type=\"text/javascript\">/*<![CDATA[*/\n//\nalert(\"Hello World\")\n// \n/*]]>*/</script>",
            "<script type=\"text/javascript\"><![CDATA[\n//\nalert(\"Hello World\")\n// \n]]></script>");

        assertHTMLUsingDomSerializer("<script type=\"text/javascript\">/*<![CDATA[*/\n//\nalert(\"Hello World\")\n// \n/*]]>*/</script>",
        "<script type=\"text/javascript\">//<![CDATA[\n//\nalert(\"Hello World\")\n// ]]></script>");

        assertHTMLUsingDomSerializer("<script type=\"text/javascript\">/*<![CDATA[*/\n"
                + "// \n"
                + "function escapeForXML(origtext) {\n"
                + "   return origtext.replace(/\\&/g,'&'+'amp;').replace(/</g,'&'+'lt;')\n"
                + "       .replace(/>/g,'&'+'gt;').replace(/\'/g,'&'+'apos;').replace(/\"/g,'&'+'quot;');"
                + "}\n"
                + "// \n/*]]>*/"
                + "</script>", "<script type=\"text/javascript\">\n"
                + "// <![CDATA[\n"
                + "function escapeForXML(origtext) {\n"
                + "   return origtext.replace(/\\&/g,'&'+'amp;').replace(/</g,'&'+'lt;')\n"
                + "       .replace(/>/g,'&'+'gt;').replace(/\'/g,'&'+'apos;').replace(/\"/g,'&'+'quot;');"
                + "}\n"
                + "// ]]>\n"
                + "</script>");

        assertHTMLUsingDomSerializer("<script>/*<![CDATA[*/\n<>\n/*]]>*/</script>", "<script><></script>");

        assertHTMLUsingDomSerializer("<altscript>/*<![CDATA[*/\n<>\n/*]]>*/</altscript>", "<altscript><></altscript>");
        
        assertHTMLUsingDomSerializer(
        		"<script>/*<![CDATA[*/\nbanana(); //-->\n/*]]>*/</script><script>/*<![CDATA[*/\ntwo\n/*]]>*/</script>",
       		    "<script>//<![CDATA[\nbanana(); //--></script><script>two</script>"
        );
    }
    
    @Test
    public void scriptAndCDataJDom() throws IOException, ParserConfigurationException
    {

        CleanerProperties cleanerProperties = new CleanerProperties();
        cleanerProperties.setOmitXmlDeclaration(false);
        cleanerProperties.setOmitDoctypeDeclaration(false);
        cleanerProperties.setIgnoreQuestAndExclam(false);
        cleanerProperties.setAddNewlineToHeadAndBody(false);
        cleanerProperties.setUseCdataFor("script,style,altscript");
        this.cleaner = new HtmlCleaner(cleanerProperties);
        
        assertHTMLUsingJDomSerializer("<script type=\"text/javascript\">/*<![CDATA[*/\nalert(\"Hello World\")\n/*]]>*/</script>", 
        		"<script type=\"text/javascript\">//<![CDATA[\nalert(\"Hello World\")\n//]]></script>");
        
        assertHTMLUsingJDomSerializer("<script type=\"text/javascript\">/*<![CDATA[*/\nalert(\"Hello World\")\n/*]]>*/</script>", 
        		"<script type=\"text/javascript\">//<![CDATA[\nalert(\"Hello World\")\n//]]></script>");

        assertHTMLUsingJDomSerializer("<script type=\"text/javascript\">/*<![CDATA[*/\nalert(\"Hello World\")\n/*]]>*/</script>", 
        		"<script type=\"text/javascript\"><![CDATA[\nalert(\"Hello World\")\n]]></script>");

        assertHTMLUsingJDomSerializer("<style type=\"text/css\">/*<![CDATA[*/\na { color: red; }\n/*]]>*/</style>", 
        		"<style type=\"text/css\"><![CDATA[\na { color: red; }\n]]></style>");


        assertHTMLUsingJDomSerializer("<script type=\"text/javascript\">/*<![CDATA[*/\n// Comment \nalert(\"Hello World\")\n //\n/*]]>*/</script>",
        "<script type=\"text/javascript\">// Comment \nalert(\"Hello World\")\n //\n</script>");

        assertHTMLUsingJDomSerializer("<script type=\"text/javascript\">/*<![CDATA[*/\nalert(\"Hello World\")\n/*]]>*/</script>",
        "<script type=\"text/javascript\"><![CDATA[\nalert(\"Hello World\")\n]]></script>");

        assertHTMLUsingJDomSerializer("<script type=\"text/javascript\">/*<![CDATA[*/\n//\nalert(\"Hello World\")\n// \n/*]]>*/</script>",
            "<script type=\"text/javascript\"><![CDATA[\n//\nalert(\"Hello World\")\n// \n]]></script>");

        assertHTMLUsingJDomSerializer("<script type=\"text/javascript\">/*<![CDATA[*/\n//\nalert(\"Hello World\")\n// \n/*]]>*/</script>",
        "<script type=\"text/javascript\">//<![CDATA[\n//\nalert(\"Hello World\")\n// ]]></script>");

        assertHTMLUsingJDomSerializer("<script type=\"text/javascript\">/*<![CDATA[*/\n"
                + "// \n"
                + "function escapeForXML(origtext) {\n"
                + "   return origtext.replace(/\\&/g,'&'+'amp;').replace(/</g,'&'+'lt;')\n"
                + "       .replace(/>/g,'&'+'gt;').replace(/\'/g,'&'+'apos;').replace(/\"/g,'&'+'quot;');"
                + "}\n"
                + "// \n/*]]>*/"
                + "</script>", "<script type=\"text/javascript\">\n"
                + "// <![CDATA[\n"
                + "function escapeForXML(origtext) {\n"
                + "   return origtext.replace(/\\&/g,'&'+'amp;').replace(/</g,'&'+'lt;')\n"
                + "       .replace(/>/g,'&'+'gt;').replace(/\'/g,'&'+'apos;').replace(/\"/g,'&'+'quot;');"
                + "}\n"
                + "// ]]>\n"
                + "</script>");

        assertHTMLUsingJDomSerializer("<script>/*<![CDATA[*/\n<>\n/*]]>*/</script>", "<script><></script>");

        assertHTMLUsingJDomSerializer("<altscript>/*<![CDATA[*/\n<>\n/*]]>*/</altscript>", "<altscript><></altscript>");
        
        assertHTMLUsingJDomSerializer(
        		"<script>/*<![CDATA[*/\nbanana(); //-->\n/*]]>*/</script><script>/*<![CDATA[*/\ntwo\n/*]]>*/</script>",
       		    "<script>//<![CDATA[\nbanana(); //--></script><script>two</script>"
        );
    }

    @Test
    public void escapingCDATA() throws IOException{
        CleanerProperties cleanerProperties = new CleanerProperties();
        cleanerProperties.setOmitXmlDeclaration(false);
        cleanerProperties.setOmitDoctypeDeclaration(false);
        cleanerProperties.setIgnoreQuestAndExclam(false);
        cleanerProperties.setAdvancedXmlEscape(true);
        cleanerProperties.setAddNewlineToHeadAndBody(false);
        cleanerProperties.setDeserializeEntities(true);
        cleanerProperties.setUseCdataFor("script,style,altscript");
        this.cleaner = new HtmlCleaner(cleanerProperties);
        this.serializer = new SimpleXmlSerializer(cleaner.getProperties());
        assertHTML("<script>/*<![CDATA[*/\n<>\n/*]]>*/</script>", "<script>&lt;&gt;</script>");
        assertHTML("<altscript>/*<![CDATA[*/\n<>\n/*]]>*/</altscript>", "<altscript>&lt;&gt;</altscript>");
    }

    @Test
    public void removeCDATA() throws IOException{
        CleanerProperties cleanerProperties = new CleanerProperties();
        cleanerProperties.setOmitCdataOutsideScriptAndStyle(true);
        cleanerProperties.setAddNewlineToHeadAndBody(false);
        cleanerProperties.setUseCdataFor("script,style,altscript");
        cleaner = new HtmlCleaner(cleanerProperties);
        serializer = new SimpleXmlSerializer(cleaner.getProperties());

        // Verify that CDATA not inside SCRIPT or STYLE elements are considered comments in HTML and thus stripped
        // when cleaned.
        assertHTML("<p></p>", "<p><![CDATA[&]]></p>");
        assertHTML("<p>&amp;&amp;</p>", "<p>&<![CDATA[&]]>&</p>");
        assertHTML("<noaltscript />", "<noaltscript><![CDATA[&]]></noaltscript>");
    }

    /**
     * Using the default setup, we should strip out CData outside
     * of script and style tags.
     */
    @Test
    public void CDATAinthewrongplace(){

        CleanerProperties cleanerProperties = new CleanerProperties();
        cleanerProperties.setIgnoreQuestAndExclam(true);

        cleaner = new HtmlCleaner(cleanerProperties);

    	String testData = ""
        	+ "<p>"
        	+ "<![CDATA[\n"
        	+ "function helloWorld() {\n"
        	+ "};\n"
        	+ "]]>\n"
        	+ "</p>";
        	
        	TagNode cleaned = cleaner.clean(testData);
        	TagNode p = cleaned.findElementByName("p", true);
        	
        	//
        	// We should have no CData nodes, instead the contents should
        	// be processed as content and escaped as usual
        	//
        	assertTrue(p.getAllChildren().get(0) instanceof ContentNode);
    }
    
    @Test
    public void nonSafeCDATA(){
    	String testData = ""
        	+ "<script type=\"text/javascript\">"
        	+ "<![CDATA[\n"
        	+ "function helloWorld() {\n"
        	+ "};\n"
        	+ "]]>\n"
        	+ "</script>";
        	
        	TagNode cleaned = cleaner.clean(testData);
        	TagNode script = cleaned.findElementByName("script", true);
        	
        	
        	//
        	// We should have a CData node for the CDATA section
        	//
        	assertTrue(script.getAllChildren().get(0) instanceof CData);
        	CData cdata = (CData)script.getAllChildren().get(0);
        	
        	String content = cdata.getContentWithoutStartAndEndTokens();
        	assertEquals("\nfunction helloWorld() {\n};\n", content);
    }
    
    @Test
    public void safeOutput(){
    	String testData = ""
        	+ "<script type=\"text/javascript\">"
        	+ "<![CDATA[\n"
        	+ "function helloWorld() {\n"
        	+ "};\n"
        	+ "]]>\n"
        	+ "</script>";
        	
        	TagNode cleaned = cleaner.clean(testData);
        	TagNode script = cleaned.findElementByName("script", true);
        	
        	
        	//
        	// We should have a CData node for the CDATA section
        	//
        	assertTrue(script.getAllChildren().get(0) instanceof CData);
        	CData cdata = (CData)script.getAllChildren().get(0);
        	
        	String content = cdata.getContentWithoutStartAndEndTokens();
        	assertEquals("\nfunction helloWorld() {\n};\n", content);
        	
        	String safeContent = cdata.getContentWithStartAndEndTokens();
        	assertEquals("/*<![CDATA[*/\nfunction helloWorld() {\n};\n/*]]>*/", safeContent);
    }
    
    /**
     * For a CDATA section we need to ignore '<' and '>' and keep going to keep the content
     * within a single CData instance.
     */
    @Test
    public void safeCDATAAlternate(){
    	String testData = ""
        	+ "<script type=\"text/javascript\">\n"
        	+ "//<![CDATA[\n"
        	+ "function escapeForXML(origtext) {\n"
        	+ " return origtext.replace(/\\&/g,'&'+'amp;').replace(/</g,'&'+'lt;')\n"
        	+ " .replace(/>/g,'&'+'gt;').replace(/\'/g,'&'+'apos;').replace(/\"/g,'&'+'quot;');"
        	+ "}\n"
        	+ "//]]>\n"
        	+ "</script>";
        	
        	TagNode cleaned = cleaner.clean(testData);
        	TagNode script = cleaned.findElementByName("script", true);
        	
        	
        	//
        	// We should have a CData node for the CDATA section
        	//
        	assertTrue(script.getAllChildren().get(1) instanceof CData);
        	CData cdata = (CData)script.getAllChildren().get(1);
        	
        	String content = cdata.getContentWithoutStartAndEndTokens();
        	assertEquals("\nfunction escapeForXML(origtext) {\n return origtext.replace(/\\&/g,'&'+'amp;').replace(/</g,'&'+'lt;')\n .replace(/>/g,'&'+'gt;').replace(/'/g,'&'+'apos;').replace(/\"/g,'&'+'quot;');}\n", content);
    }
    
    /**
     * For a CDATA section we need to ignore '<' and '>' and keep going to keep the content
     * within a single CData instance
     */
    @Test
    public void safeCDATA(){
    	String testData = ""
        	+ "<script type=\"text/javascript\">\n"
        	+ "/*<![CDATA[*/\n"
        	+ "function escapeForXML(origtext) {\n"
        	+ " return origtext.replace(/\\&/g,'&'+'amp;').replace(/</g,'&'+'lt;')\n"
        	+ " .replace(/>/g,'&'+'gt;').replace(/\'/g,'&'+'apos;').replace(/\"/g,'&'+'quot;');"
        	+ "}\n"
        	+ "/*]]>*/>\n"
        	+ "</script>";
        	
        	TagNode cleaned = cleaner.clean(testData);
        	TagNode script = cleaned.findElementByName("script", true);
        	
        	
        	//
        	// We should have a CData node for the CDATA section
        	//
        	assertTrue(script.getAllChildren().get(1) instanceof CData);
        	CData cdata = (CData)script.getAllChildren().get(1);
        	
        	String content = cdata.getContentWithoutStartAndEndTokens();
        	assertEquals("\nfunction escapeForXML(origtext) {\n return origtext.replace(/\\&/g,'&'+'amp;').replace(/</g,'&'+'lt;')\n .replace(/>/g,'&'+'gt;').replace(/'/g,'&'+'apos;').replace(/\"/g,'&'+'quot;');}\n", content);
    }
    
    @Test
    public void style(){
    	String testData = "<style type=\"text/css\">/*<![CDATA[*/\n#ampmep_188 { }\n/*]]>*/</style>";
    	TagNode cleaned = cleaner.clean(testData);
    	TagNode style = cleaned.findElementByName("style", true);
    	
    	assertTrue(style.getAllChildren().get(0) instanceof CData);    	
    	
    	String content = (((CData)style.getAllChildren().get(0)).getContentWithoutStartAndEndTokens());

    	assertEquals("\n#ampmep_188 { }\n", content);

    }
    
    @Test
    public void preserveComments() throws IOException{
    	cleaner.getProperties().setOmitXmlDeclaration(false);
    	String initial = readFile("src/test/resources/test17.html");
    	String expected = readFile("src/test/resources/test17_expected.html");
    	assertCleaned(initial, expected);
    }
    
    @Test
    public void preserveCommentsXwiki() throws IOException{
    	cleaner.getProperties().setOmitXmlDeclaration(false);
    	cleaner.getProperties().setAddNewlineToHeadAndBody(false);
    	assertHTML("<script type=\"text/javascript\">/*<![CDATA[*/\n//\nalert(\"Hello World\")\n// \n/*]]>*/</script>",
    			   "<script type=\"text/javascript\">//<![CDATA[\n//\nalert(\"Hello World\")\n// ]]></script>"
    	);
    }
    
    @Test
    public void preserveComments2() throws IOException{
    	cleaner.getProperties().setOmitXmlDeclaration(false);
    	cleaner.getProperties().setAddNewlineToHeadAndBody(false);
    	assertHTML("<script type=\"text/javascript\">/*<![CDATA[*/\n//alert(\"Hello World\")\n/*]]>*/</script>",
    			   "<script type=\"text/javascript\"><![CDATA[//alert(\"Hello World\")]]></script>"
    	);
    }

}
