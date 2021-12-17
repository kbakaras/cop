package org.htmlcleaner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class HtmlCleanerTest extends AbstractHtmlCleanerTest {
	
	// Test for bug 221. Thanks to Simon Urli for contributing the test case.
	@Test
    public void parseWithUnicodeChars() throws Exception
    {
        CleanerProperties cleanerProperties = new CleanerProperties();

        cleanerProperties.setDeserializeEntities(true);
        cleanerProperties.setRecognizeUnicodeChars(true);
        cleanerProperties.setTranslateSpecialEntities(false);

        HtmlCleaner cleaner = new HtmlCleaner(cleanerProperties);
        TagNode tagNode = cleaner.clean("<?xml version = \"1.0\"?>"
            + "<div foo=\"&#169;\">&#169;</div>"
            + "<div foo=\"baz&gt;buz\">baz&gt;buz</div>"
            + "<div foo=\"baz&buz\">baz&buz</div>");
        List<? extends TagNode> divList = tagNode.getElementListByName("div", true);
        assertEquals(3, divList.size());

        assertEquals("©", divList.get(0).getText().toString());
        assertEquals("©", divList.get(0).getAttributeByName("foo"));

        assertEquals("baz>buz", divList.get(1).getText().toString());
        assertEquals("baz>buz", divList.get(1).getAttributeByName("foo"));

        assertEquals("baz&buz", divList.get(2).getText().toString());
        assertEquals("baz&buz", divList.get(2).getAttributeByName("foo"));

        DomSerializer domSerializer = new DomSerializer(cleanerProperties, false);
        Document document = domSerializer.createDOM(tagNode);

        NodeList nodeList = document.getElementsByTagName("div");
        assertEquals(3, nodeList.getLength());
        assertEquals("©", nodeList.item(0).getTextContent());
        assertEquals("©", nodeList.item(0).getAttributes().getNamedItem("foo").getTextContent());

        assertEquals("baz>buz", nodeList.item(1).getAttributes().getNamedItem("foo").getTextContent());

        assertEquals("baz&buz", nodeList.item(2).getAttributes().getNamedItem("foo").getTextContent());

        // BUG: This is failing with baz&gt;buz
        assertEquals("baz>buz", nodeList.item(1).getTextContent());
        // BUG: This is failing with baz&amp;buz
        assertEquals("baz&buz", nodeList.item(2).getTextContent());
    }
	
	// See Bug 219. Ensure we don't see any odd structure changes
	// when handling foreign markup.
	@Test
	public void unknownNS() throws Exception
	{
		String initial = 
				  "<html xmlns='foo'>"
                + "<head>"
                + "</head>"
                + "<body>"
                + "<a><br>"
                + "<h3\">text</h3>"
                + "<div\">"
                + "<cite\">link</cite>"
                + "</div></a>"               
                + "</body>"
                + "</html>";
		String expected = 				  
				  "<html xmlns=\"foo\">"
                + "<head>"
                + "</head>"
                + "<body>"
                + "<a><br>"
                + "<h3>text</h3>"
                + "<div>"
                + "<cite>link</cite>"
                + "</div></br></a>"               
                + "</body>"
                + "</html>";
		assertCleanedHtml(initial, expected);
	}
	
	// See Bug 218. Thanks to Lukas Lehmann and Dennis Ratzke
	@Test
	public void structuredListTest() throws Exception
	{
		String initial = "<em>*</em>";
		String expected = "<html><head></head><body><em>*</em></body></html>";
		assertCleanedHtml(initial, expected);
		
		initial = "<label>banana</label>";
		expected = "<html><head></head><body><label>banana</label></body></html>";
		assertCleanedHtml(initial, expected);
		
		initial = "<label>banana<em>*</em></label>";
		expected = "<html><head></head><body><label>banana<em>*</em></label></body></html>";
		assertCleanedHtml(initial, expected);
		
		initial = "<label>banana<em>*</em></label><select><option>d</option></select>";
		expected = "<html><head></head><body><label>banana<em>*</em></label><select><option>d</option></select></body></html>";
		assertCleanedHtml(initial, expected);
		
		initial = "<dl>\n" +
				"<div>\n" + 
				"<label>bb<em>*</em></label>\n" + 
				"<select>\n" + 
				"<option>d</option>\n" + 
				"</select>\n" + 
				"</div>\n" + 
				"</dl>\n";
		expected = "<html><head></head><body>" + 
				"<dl>\n"+
				"<div>\n" +
				"<label>bb<em>*</em></label>\n" +
				"<select>\n" +
				"<option>d</option>\n" +
				"</select>\n" +
				"</div>\n" +
				"</dl>\n"+
				"</body></html>";
		assertCleanedHtml(initial, expected);
		
		initial = "<dl><label>banana</label></dl>";
		expected = "<html><head></head><body><dl><div><label>banana</label></div></dl></body></html>";
		assertCleanedHtml(initial, expected);


		
	}
	
	
	@Test
    public void doubleEscapingInAttributesDom() throws Exception
    {
        CleanerProperties cleanerProperties = new CleanerProperties();
        HtmlCleaner cleaner = new HtmlCleaner(cleanerProperties);
        TagNode tagNode = cleaner.clean("<?xml version = \"1.0\"?><div foo=\"&amp;quot;\"></div>");
        DomSerializer domSerializer = new DomSerializer(cleanerProperties, true);
        Document document = domSerializer.createDOM(tagNode);

        Node node = document.getElementsByTagName("div").item(0);
        assertEquals("&quot;", node.getAttributes().getNamedItem("foo").getTextContent());
        
        domSerializer = new DomSerializer(cleanerProperties, false);
        document = domSerializer.createDOM(tagNode);

        node = document.getElementsByTagName("div").item(0);
        assertEquals("&amp;quot;", node.getAttributes().getNamedItem("foo").getTextContent());
    }
	
	@Test
    public void doubleEscapingInAttributesJDom() throws Exception
    {
        CleanerProperties cleanerProperties = new CleanerProperties();
        HtmlCleaner cleaner = new HtmlCleaner(cleanerProperties);
        TagNode tagNode = cleaner.clean("<?xml version = \"1.0\"?><div foo=\"&amp;quot;\"></div>");
        JDomSerializer domSerializer = new JDomSerializer(cleanerProperties, true);
        org.jdom2.Document document = domSerializer.createJDom(tagNode);
        org.jdom2.Element node = document.getRootElement().getChild("body").getChild("div");
        assertEquals("&quot;", node.getAttribute("foo").getValue());
        
        domSerializer = new JDomSerializer(cleanerProperties, false);
        document = domSerializer.createJDom(tagNode);
        node = document.getRootElement().getChild("body").getChild("div");
        assertEquals("&amp;quot;", node.getAttribute("foo").getValue());
        
        cleanerProperties.setDeserializeEntities(true);
        tagNode = cleaner.clean("<?xml version = \"1.0\"?><div foo=\"&amp;quot;\"></div>");
        domSerializer = new JDomSerializer(cleanerProperties, false);
        document = domSerializer.createJDom(tagNode);
        node = document.getRootElement().getChild("body").getChild("div");
        assertEquals("&quot;", node.getAttribute("foo").getValue());
    }
	
	@Test
    public void doubleEscapingInAttributesTagNode() throws Exception
    {
        CleanerProperties cleanerProperties = new CleanerProperties();
        HtmlCleaner cleaner = new HtmlCleaner(cleanerProperties);
        TagNode tagNode = cleaner.clean("<?xml version = \"1.0\"?><div foo=\"&amp;quot;\"></div>");
        assertEquals("&amp;quot;", tagNode.findElementHavingAttribute("foo", true).getAttributeByName("foo"));
        
        cleanerProperties = new CleanerProperties();
        cleanerProperties.setDeserializeEntities(true);
        cleaner = new HtmlCleaner(cleanerProperties);
        tagNode = cleaner.clean("<?xml version = \"1.0\"?><div foo=\"&amp;quot;\"></div>");
        assertEquals("&quot;", tagNode.findElementHavingAttribute("foo", true).getAttributeByName("foo"));
        
        cleanerProperties.setDeserializeEntities(false);
        cleaner = new HtmlCleaner(cleanerProperties);
        tagNode = cleaner.clean("<?xml version = \"1.0\"?><div foo=\"&amp;quot;\"></div>");
        assertEquals("&amp;quot;", tagNode.findElementHavingAttribute("foo", true).getAttributeByName("foo"));
    }
	
	@Test
    public void doubleEscapingInAttributesHtml() throws Exception
    {
        CleanerProperties cleanerProperties = new CleanerProperties();
        HtmlCleaner cleaner = new HtmlCleaner(cleanerProperties);
        cleanerProperties.setDeserializeEntities(true);
        TagNode tagNode = cleaner.clean("<?xml version = \"1.0\"?><div foo=\"&amp;quot;\"></div>");
        
        HtmlSerializer ser = new SimpleHtmlSerializer(cleaner.getProperties());
        assertTrue(ser.getAsString(tagNode).contains("foo=\"&quot;\""));
    }
	
	// test discussion item
	@Test
	public void encodingTest() throws Exception{
		String html="<div>Today I want to talk about the &amp;lt; character reference.</div>";
		CleanerProperties props = new CleanerProperties();
	    props.setOmitHtmlEnvelope(true);
	    props.setDeserializeEntities(true);
	    HtmlCleaner cleaner = new HtmlCleaner(props);
	    TagNode rootTag = cleaner.clean(html);
	    ContentNode contentNode = (ContentNode)((TagNode)rootTag.getAllChildren().get(0)).getAllChildren().get(0);
	    assertEquals("Today I want to talk about the &lt; character reference.", contentNode.content);
	}

	
	// test discussion item
	@Test
	public void encodingTest2() throws Exception{
		String html="<div>this &amp; that</div>";
		CleanerProperties props = new CleanerProperties();
	    props.setOmitHtmlEnvelope(true);
	    props.setOmitXmlDeclaration(true);
	    props.setDeserializeEntities(true);
	    props.setTranslateSpecialEntities(true);
	    props.setHtmlVersion(5);
	    HtmlCleaner cleaner = new HtmlCleaner(props);
	    TagNode rootTag = cleaner.clean(html);
	    ContentNode contentNode = (ContentNode)((TagNode)rootTag.getAllChildren().get(0)).getAllChildren().get(0);
	    assertEquals("this & that", contentNode.content);
		String cleaned = new SimpleHtmlSerializer(cleaner.getProperties(), false).getAsString(cleaner.clean(html));
	}
	
	@Test
	public void encodingTest3() throws Exception{
		String html="<div>Today I want to talk about the &amp;lt; character reference.</div>";
		CleanerProperties props = new CleanerProperties();
	    props.setOmitHtmlEnvelope(true);
	    props.setDeserializeEntities(false);
	    HtmlCleaner cleaner = new HtmlCleaner(props);
	    TagNode rootTag = cleaner.clean(html);
	    ContentNode contentNode = (ContentNode)((TagNode)rootTag.getAllChildren().get(0)).getAllChildren().get(0);
	    assertEquals("Today I want to talk about the &amp;lt; character reference.", contentNode.content);
	}
	
	// test bug #65
	@Test
	public void nbspTest() throws Exception{
		String html="<span>Change text size&nbsp;</span>";
		String expected="<span>Change text size\u00A0</span>";
    	cleaner.getProperties().setOmitXmlDeclaration(false);
    	cleaner.getProperties().setAddNewlineToHeadAndBody(false);
		assertHTML(expected, html);
	}
	
	// Test bug #196. Doesn't currently pass.
	@Ignore 
	@Test
	public void emptyString() throws Exception{
		String html = " ";
		
		CleanerProperties props = new CleanerProperties();
	    props.setOmitXmlDeclaration(true);
	    props.setOmitHtmlEnvelope(true);
	    
	    HtmlCleaner cleaner = new HtmlCleaner(props);
	    TagNode rootTag = cleaner.clean(html);
	    XmlSerializer serializer = new SimpleXmlSerializer(props);
	    ContentNode contentNode = (ContentNode)rootTag.getAllChildren().get(0);

	    String cleanHtml = serializer.getAsString(rootTag, false);
	    assertEquals(" ", contentNode.content);
	    assertEquals(" ", cleanHtml);
	}
	
	@Test
	public void linefeedsInElements() throws IOException {
		HtmlCleaner theCleaner = new HtmlCleaner();
		theCleaner.getProperties().setRecognizeUnicodeChars(false);
		theCleaner.getProperties().setOmitHtmlEnvelope(true);
		theCleaner.getProperties().setOmitXmlDeclaration(true);
		String initial = "<dfn>something: &#xA;</dfn>";
		String expected = "<dfn>something: &#xA;</dfn>";
		String cleaned = new SimpleHtmlSerializer(theCleaner.getProperties()).getAsString(theCleaner.clean(initial));
	    assertEquals(cleaned, expected);
		cleaned = new SimpleXmlSerializer(theCleaner.getProperties()).getAsString(theCleaner.clean(initial));
	    assertEquals(cleaned, expected);
	}
	
	@Test
	public void operatorsInAttributes() throws IOException {
		HtmlCleaner theCleaner = new HtmlCleaner();
		theCleaner.getProperties().setAllowHtmlInsideAttributes(true);
		theCleaner.getProperties().setOmitXmlDeclaration(true);
		String initial = "<dfn title=\"something is < 100\">";
		String expected = "<html><head></head><body><dfn title=\"something is &lt; 100\"></dfn></body></html>";
		String cleaned = new SimpleHtmlSerializer(theCleaner.getProperties()).getAsString(theCleaner.clean(initial));
	    assertEquals(cleaned, expected);
	}
	
	@Test
	public void operatorsInAttributes2() throws IOException {
		String initial = "<dfn title=\"something is &lt; 100\">";
		String expected = "<html><head></head><body><dfn title=\"something is &lt; 100\"></dfn></body></html>";
		assertCleanedHtml(initial, expected);
	}
	
	@Test
	public void operatorsInAttributes3() throws IOException {
		HtmlCleaner theCleaner = new HtmlCleaner();
		theCleaner.getProperties().setAllowHtmlInsideAttributes(false);
		theCleaner.getProperties().setOmitXmlDeclaration(true);
		String initial = "<dfn title=\"something is < 100\">";
		String expected = "<html><head></head><body><dfn title=\"something is\">&lt; 100&quot;&gt;</dfn></body></html>";
		String cleaned = new SimpleHtmlSerializer(theCleaner.getProperties()).getAsString(theCleaner.clean(initial));
	    assertEquals(cleaned, expected);
	}
	
	@Test
	public void operatorsInAttributes4() throws IOException {
		HtmlCleaner theCleaner = new HtmlCleaner();
		theCleaner.getProperties().setAllowHtmlInsideAttributes(false);
		theCleaner.getProperties().setOmitXmlDeclaration(true);
		String initial = "<dfn title=\"something is &lt; 100\">";
		String expected = "<html><head></head><body><dfn title=\"something is &lt; 100\"></dfn></body></html>";
		String cleaned = new SimpleHtmlSerializer(theCleaner.getProperties()).getAsString(theCleaner.clean(initial));
	    assertEquals(cleaned, expected);
	}
	
	@Test
	public void escaped() throws IOException{
		HtmlCleaner thecleaner = new HtmlCleaner();
		String initial = readFile("src/test/resources/escaped.html");
		initial = initial.replaceAll("&lt;", "<");
		initial = initial.replaceAll("&gt;", ">");
		TagNode node = thecleaner.clean(initial);
		new SimpleHtmlSerializer(thecleaner.getProperties()).getAsString(node);
	}

	@Test
	public void unescapedtagtokens() throws IOException{
		String initial = "<div id=\"1234\">My name is ABC and my age is < 30. </div>";
		String expected = "<html><head></head><body><div id=\"1234\">My name is ABC and my age is &lt; 30. </div></body></html>";
		assertCleanedHtml(initial, expected);
	}
	
	@Test
	public void href() throws IOException{
		String initial = "<a href=\"/test?a=1&b=2\">";
		String expected = "<html><head></head><body><a href=\"/test?a=1&amp;b=2\"></a></body></html>";
		assertCleanedHtml(initial, expected);
		expected = "<html>\n<head />\n<body><a href=\"/test?a=1&amp;b=2\"></a></body></html>";
		assertCleaned(initial, expected);
	}
	
	@Test
	public void href2() throws IOException{
		String initial = "<a href=\"/test?a=1&amp;b=2\">";
		String expected = "<html><head></head><body><a href=\"/test?a=1&amp;b=2\"></a></body></html>";
		assertCleanedHtml(initial, expected);
		expected = "<html>\n<head />\n<body><a href=\"/test?a=1&amp;b=2\"></a></body></html>";
		assertCleaned(initial, expected);
	}
	
	@Test
	public void src() throws IOException{
		String initial = "<img src=\"/test?a=1&b=2\">";
		String expected = "<html><head></head><body><img src=\"/test?a=1&amp;b=2\" /></body></html>";
		TagNode tagNode = cleaner.clean(initial);
		TagNode img = tagNode.getElementsHavingAttribute("src", true)[0];
		assertCleanedHtml(initial, expected);
		expected = "<html>\n<head />\n<body><img src=\"/test?a=1&amp;b=2\" /></body></html>";
		assertCleaned(initial, expected);
	}
	
	@Test
	public void src2() throws IOException{
		String initial = "<img src=\"/test?a=1&amp;b=2\">";
		String expected = "<html><head></head><body><img src=\"/test?a=1&amp;b=2\" /></body></html>";
		cleaner.getProperties().setAdvancedXmlEscape(true);
		cleaner.getProperties().setTransSpecialEntitiesToNCR(false);
		TagNode tagNode = cleaner.clean(initial);
		TagNode img = tagNode.getElementsHavingAttribute("src", true)[0];
		assertCleanedHtml(initial, expected);
		expected = "<html>\n<head />\n<body><img src=\"/test?a=1&amp;b=2\" /></body></html>";
		assertCleaned(initial, expected);
	}
	
	/**
	 * Check for OOME with TIME element - see bug #191
	 */
	@Test
	public void timeElement() throws IOException{
		String initial = "<time><b><li>";
		String expected = "<html><head></head><body><ul><li></li></ul><time><b></b></time></body></html>";
		assertCleanedHtml(initial, expected);
	}
	@Test
	public void timeElement2() throws IOException{
		String initial = "<time><b>today<li>";
		String expected = "<html><head></head><body><ul><li></li></ul><time><b>today</b></time></body></html>";
		assertCleanedHtml(initial, expected);
	}
	
	/**
	 * Check intervening tags works with NS - see bug #190
	 */
	@Test
	public void minimalNSTest() throws IOException{
		String initial = "<html xmlns=\"x\"><ul><a>";
		String expected = "<html xmlns=\"x\"><head></head><body><ul><a></a></ul></body></html>";
		assertCleanedHtml(initial, expected);
	}
	
	/**
	 * Prune tags test - see bug #188
	 */
	@Test
	public void pruneTest() throws Exception {
		String initial = "<p>alert using script:<scr<script>ipt>alert(\"Hello\");</scr<script>ipt></p>\n";
		String expected = "<p>alert using script:<scr></scr></p>";
		cleaner.getProperties().setPruneTags("script");
		cleaner.getProperties().setOmitHtmlEnvelope(true);
		assertCleanedHtml(initial, expected);
	}
	
	/**
	 * first attribute of duplicates is selected - see bug #57
	 */
	@Test
	public void duplicateAttributes() throws Exception {
	    cleaner.getProperties().setOmitHtmlEnvelope(true);

	    assertCleanedHtml("<p class=\"A\" class=\"B\"></p>", "<p class=\"A\"></p>");
	    assertCleanedHtml("<p class=\"B\" class=\"A\"></p>", "<p class=\"B\"></p>");
	    assertCleanedHtml("<p CLASS=\"A\" class=\"B\"></p>", "<p class=\"A\"></p>");
	    assertCleanedHtml("<p class=\"A\" CLASS=\"B\"></p>", "<p class=\"A\"></p>");

	}
	
	/**
	 * Element names in HTML and XML
	 */
	@Test
	public void elementNames() throws Exception {
	    cleaner.getProperties().setOmitHtmlEnvelope(true);
	    cleaner.getProperties().setNamespacesAware(true);
	    
	    assertCleanedDom("<p><^-^></p>", "<p>&amp;lt;^-^&amp;gt;</p>");
	    assertCleanedJDom("<p><^-^></p>", "<p>&amp;lt;^-^&amp;gt;</p>");
	    assertCleanedDom("<p><1o></p>", "<p>&amp;lt;1o&amp;gt;</p>");
	    assertCleanedJDom("<p><1o></p>", "<p>&amp;lt;1o&amp;gt;</p>");
	    assertCleanedDom("<p><b=></b=></p>", "<p>\n<b></b>\n</p>");
	    assertCleanedJDom("<p><b=></b=></p>", "<p><b /></p>");
	    assertCleanedDom("<p><b^b></b=></p>", "<p>\n<bb></bb>\n</p>");
	    assertCleanedJDom("<p><b^b></b=></p>", "<p><bb /></p>");
	}
	    
	/**
	 * attribute names for HTML and XML - see bug #175
	 */
	@Test
	public void attributeNames() throws Exception {
				
	    cleaner.getProperties().setOmitHtmlEnvelope(true);
	    cleaner.getProperties().setNamespacesAware(true);
	    
	    // Try to quietly fix bad names with no prefixes
	    assertCleanedDom("<p ba;nana=\"yy\"></p>", "<p banana=\"yy\"></p>");
	    assertCleanedJDom("<p ba;nana=\"yy\"></p>", "<p banana=\"yy\" />");

	    // OK - characters
	    assertCleanedHtml("<p xx=\"yy\"></p>", "<p xx=\"yy\"></p>");
	    assertCleanedHtml("<p value=yes></p>", "<p value=\"yes\"></p>");

	    assertCleaned("<p xx=\"yy\"></p>", "<p xx=\"yy\"></p>");
	    assertCleanedDom("<p xx=\"yy\"></p>", "<p xx=\"yy\"></p>");
	    assertCleanedJDom("<p xx=\"yy\"></p>", "<p xx=\"yy\" />");
	    
	    // Numbers - OK in HTML, invalid in XML
	    
	    // First, lets clean them with a prefix.
	    cleaner.getProperties().setInvalidXmlAttributeNamePrefix("hc-generated-");
	    assertCleanedDom("<p 1=\"yy\"></p>", "<p hc-generated-1=\"yy\"></p>");
	    assertCleanedJDom("<p 1=\"yy\"></p>", "<p hc-generated-1=\"yy\" />");
	    assertCleanedHtml("<p 1=\"yy\"></p>", "<p 1=\"yy\"></p>");
	    assertCleaned("<p 1=\"yy\"></p>", "<p hc-generated-1=\"yy\"></p>");
	    
	    // Now, without a prefix - they have to be removed
	    cleaner.getProperties().setInvalidXmlAttributeNamePrefix("");
	    assertCleanedHtml("<p 1=\"yy\"></p>", "<p 1=\"yy\"></p>");
	    assertCleaned("<p 1=\"yy\"></p>", "<p></p>");
	    assertCleanedDom("<p 1=\"yy\"></p>", "<p></p>");
	    assertCleanedJDom("<p 1=\"yy\"></p>", "<p />");
	    
	    // Colons - OK but assumed to be NS prefixes
	    assertCleanedHtml("<p clear:both=\"yy\"></p>", "<p clear:both=\"yy\"></p>");
	    assertCleaned("<p clear:both=\"yy\"></p>", "<p clear:both=\"yy\"></p>");
	    assertCleanedDom("<p clear:both=\"yy\"></p>", "<p clear:both=\"yy\"></p>");
	    assertCleanedJDom("<p clear:both=\"yy\"></p>", "<p xmlns:clear=\"clear\" clear:both=\"yy\" />");

	    // Dashes - OK in HTML and in XML
	    assertCleanedHtml("<p a-b=\"yy\"></p>", "<p a-b=\"yy\"></p>");
	    assertCleaned("<p a-b=\"yy\"></p>", "<p a-b=\"yy\"></p>");	 
	    assertCleanedDom("<p a-b=\"yy\"></p>", "<p a-b=\"yy\"></p>");	
	    assertCleanedJDom("<p a-b=\"yy\"></p>", "<p a-b=\"yy\" />");	    
	    
	    // Semicolons - OK in HTML, invalid in XML
	    cleaner.getProperties().setInvalidXmlAttributeNamePrefix("hc-generated-");
	    assertCleanedHtml("<p a;=\"yy\"></p>", "<p a;=\"yy\"></p>");
	    assertCleaned("<p a;=\"yy\"></p>", "<p a=\"yy\"></p>"); 
	    assertCleanedDom("<p a;=\"yy\"></p>", "<p a=\"yy\"></p>"); 
	    assertCleanedJDom("<p a;=\"yy\"></p>", "<p a=\"yy\" />"); 

	    assertCleanedHtml("<p ba;nana=\"yy\"></p>", "<p ba;nana=\"yy\"></p>"); 
	    assertCleaned("<p ba;nana=\"yy\"></p>", "<p banana=\"yy\"></p>"); 
	    assertCleanedDom("<p ba;nana=\"yy\"></p>", "<p banana=\"yy\"></p>"); 
	    assertCleanedJDom("<p ba;nana=\"yy\"></p>", "<p banana=\"yy\" />"); 

	    cleaner.getProperties().setAllowInvalidAttributeNames(true);
	    assertCleanedHtml("<p a;=\"yy\"></p>", "<p a;=\"yy\"></p>"); 
	    assertCleaned("<p ba;nana=\"yy\"></p>", "<p ba;nana=\"yy\"></p>"); 
	    assertCleanedDom("<p ba;nana=\"yy\"></p>", "<p ba;nana=\"yy\"></p>"); 
	    assertCleanedJDom("<p ba;nana=\"yy\"></p>", "<p />"); 
	    
	    cleaner.getProperties().setAllowInvalidAttributeNames(false);
	    cleaner.getProperties().setInvalidXmlAttributeNamePrefix("");
	    assertCleanedHtml("<p ba;nana=\"yy\"></p>", "<p ba;nana=\"yy\"></p>"); 
	    assertCleaned("<p ba;nana=\"yy\"></p>", "<p banana=\"yy\"></p>"); 
	    assertCleanedDom("<p ba;nana=\"yy\"></p>", "<p banana=\"yy\"></p>"); 
	    assertCleanedJDom("<p ba;nana=\"yy\"></p>", "<p banana=\"yy\" />"); 
	    
	    // SOLIDUS - invalid in both
	    assertCleanedHtml("<p 1/=\"yy\"></p>", "<p 1=\"1\" yy=\"yy\"></p>"); //
	    cleaner.getProperties().setAllowInvalidAttributeNames(false);
	    cleaner.getProperties().setInvalidXmlAttributeNamePrefix("hc-generated-");
	    assertCleanedHtml("<p 1/=\"yy\"></p>", "<p 1=\"1\" yy=\"yy\"></p>");
	    assertCleaned("<p 1/=\"yy\"></p>", "<p hc-generated-1=\"1\" yy=\"yy\"></p>");
	    assertCleanedDom("<p 1/=\"yy\"></p>", "<p hc-generated-1=\"1\" yy=\"yy\"></p>");
	    assertCleanedJDom("<p 1/=\"yy\"></p>", "<p hc-generated-1=\"1\" yy=\"yy\" />");

	    cleaner.getProperties().setAllowInvalidAttributeNames(true);
	    assertCleanedHtml("<p 1/=\"yy\"></p>", "<p 1=\"1\" yy=\"yy\"></p>");
	    assertCleaned("<p 1/=\"yy\"></p>", "<p 1=\"1\" yy=\"yy\"></p>");
	    assertCleanedDom("<p 1/=\"yy\"></p>", "<p 1=\"1\" yy=\"yy\"></p>");
	    assertCleanedJDom("<p 1/=\"yy\"></p>", "<p yy=\"yy\" />");
	    
	    // SOLIDUS
	    assertCleanedHtml("<p x\u002F=\"yy\"></p>", "<p x=\"x\" yy=\"yy\"></p>");
	    assertCleaned("<p x\u002F=\"yy\"></p>", "<p x=\"x\" yy=\"yy\"></p>");
	    assertCleanedDom("<p x\u002F=\"yy\"></p>", "<p x=\"x\" yy=\"yy\"></p>");
	    assertCleanedJDom("<p x\u002F=\"yy\"></p>", "<p x=\"x\" yy=\"yy\" />");

	    // APOS
	    assertCleanedHtml("<p x'=\"yy\"></p>", "<p x=\"x\" yy=\"yy\"></p>");
	    assertCleaned("<p x'=\"yy\"></p>", "<p x=\"x\" yy=\"yy\"></p>");
	    assertCleanedDom("<p x'=\"yy\"></p>", "<p x=\"x\" yy=\"yy\"></p>");
	    assertCleanedJDom("<p x'=\"yy\"></p>", "<p x=\"x\" yy=\"yy\" />");

	    // EQUALS
	    assertCleanedHtml("<p ==\"yy\"></p>", "<p yy=\"yy\"></p>");
	    assertCleaned("<p ==\"yy\"></p>", "<p yy=\"yy\"></p>");
	    assertCleanedDom("<p x'=\"yy\"></p>", "<p x=\"x\" yy=\"yy\"></p>");
	    assertCleanedJDom("<p x'=\"yy\"></p>", "<p x=\"x\" yy=\"yy\" />");

	    // NULL
	    assertCleanedHtml("<p \u0000=\"yy\"></p>", "<p yy=\"yy\"></p>");	
	    assertCleaned("<p \u0000=\"yy\"></p>", "<p yy=\"yy\"></p>");	
	    assertCleanedDom("<p \u0000=\"yy\"></p>", "<p yy=\"yy\"></p>");
	    assertCleanedJDom("<p \u0000=\"yy\"></p>", "<p yy=\"yy\" />");
	    
	    // No namespaces and using colon - if the localname is null we should 
	    // remove it
	    cleaner.getProperties().setNamespacesAware(false);
	    assertCleanedHtml("<p x:\u0000=\"yy\"></p>", "<p yy=\"yy\"></p>");
	    assertCleanedHtml("<p disabled></p>", "<p disabled=\"disabled\"></p>");	

	}
	
	@Test
	public void attributesRealExample() throws IOException{
	    cleaner.getProperties().setOmitHtmlEnvelope(true);
	    cleaner.getProperties().setAllowInvalidAttributeNames(true);
		String original = "<div #000000;=\"\" 1px=\"\" border-top:solid=\"\" clear:both;=\"\" 18px;=\"\" line-height:=\"\" 15px;text-align:center;=\"\" padding-top:=\"\" margin-top:25px;=\"\" margin-right:=\"\" 20px;=\"\" margin-left:=\"\" 10px;=\"\" style=\"font-size:\">";
		String expected = "<div #000000;=\"\" 1px=\"\" border-top:solid=\"\" clear:both;=\"\" 18px;=\"\" line-height:=\"\" 15px;text-align:center;=\"\" padding-top:=\"\" margin-top:25px;=\"\" margin-right:=\"\" 20px;=\"\" margin-left:=\"\" 10px;=\"\" style=\"font-size:\"></div>";
		assertCleanedHtml(original, expected);
	}
	
	//
	// Test for bug #142
	//
	@Test
	@Ignore // TODO Still need to fix this
	public void tokens() throws IOException{
		String html = "<!-BY_DAUM->TEST ONE <br>  --- TEST TWO (THREE) FOUR";
	    ByteArrayOutputStream htmlOutputStream = new ByteArrayOutputStream();
		HtmlCleaner cleaner = new HtmlCleaner();
	    CleanerProperties props = cleaner.getProperties();
	    props.setHtmlVersion(5);
	    props.setOmitUnknownTags(true);
	    props.setIgnoreQuestAndExclam(true);
	    TagNode node = cleaner.clean(html);
	    new SimpleHtmlSerializer(props).writeToStream(node, htmlOutputStream);
	    String htmlcontent = htmlOutputStream.toString();
	    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<html><head></head><body>TEST ONE <br />  --- TEST TWO (THREE) FOUR</body></html>", htmlcontent);
	}
	
	//
	// Tables with missing TDs
	//
	@Test
	public void tableFix() throws IOException{
		String html = "<table><tr><p>Hello</p></tr></table>";
		
	    ByteArrayOutputStream htmlOutputStream = new ByteArrayOutputStream();

		HtmlCleaner cleaner = new HtmlCleaner();
	    CleanerProperties props = cleaner.getProperties();
	    props.setHtmlVersion(5);
	    props.setAllowHtmlInsideAttributes(true);
	    TagNode node = cleaner.clean(html);
	    new SimpleHtmlSerializer(props).writeToStream(node, htmlOutputStream);
	    String htmlcontent = htmlOutputStream.toString();
	    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<html><head></head><body><table><tbody><tr><td><p>Hello</p></td></tr></tbody></table></body></html>", htmlcontent);
	}
	
	//
	// Tables with missing TDs
	//
	@Test
	public void tableFix2() throws IOException{
		String html = "<table><tr><div>Hello";
		
	    ByteArrayOutputStream htmlOutputStream = new ByteArrayOutputStream();

		HtmlCleaner cleaner = new HtmlCleaner();
	    CleanerProperties props = cleaner.getProperties();
	    props.setHtmlVersion(5);
	    props.setAllowHtmlInsideAttributes(true);
	    TagNode node = cleaner.clean(html);
	    new SimpleHtmlSerializer(props).writeToStream(node, htmlOutputStream);
	    String htmlcontent = htmlOutputStream.toString();
	    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<html><head></head><body><table><tbody><tr><td><div>Hello</div></td></tr></tbody></table></body></html>", htmlcontent);
	}
	
	//
	// Tables with missing TDs
	//
	@Test
	public void tableFix3() throws IOException{
		String html = "<table><tr><tr><p>Hello</p></tr></tr></table>";
		
	    ByteArrayOutputStream htmlOutputStream = new ByteArrayOutputStream();

		HtmlCleaner cleaner = new HtmlCleaner();
	    CleanerProperties props = cleaner.getProperties();
	    props.setHtmlVersion(5);
	    props.setAllowHtmlInsideAttributes(true);
	    TagNode node = cleaner.clean(html);
	    new SimpleHtmlSerializer(props).writeToStream(node, htmlOutputStream);
	    String htmlcontent = htmlOutputStream.toString();
	    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<html><head></head><body><table><tbody><tr></tr><tr><td><p>Hello</p></td></tr></tbody></table></body></html>", htmlcontent);
	}

	//
	// Test for bug #166 - ensure we insert a LI rather than just shove the tag into the parent UL
	//
	@Test
	public void html5pos() throws IOException{
		String html = "<ul><p>Hello</p></ul>";
		
	    ByteArrayOutputStream htmlOutputStream = new ByteArrayOutputStream();

		HtmlCleaner cleaner = new HtmlCleaner();
	    CleanerProperties props = cleaner.getProperties();
	    TagNode node = cleaner.clean(html);
	    new SimpleHtmlSerializer(props).writeToStream(node, htmlOutputStream);
	    String htmlcontent = htmlOutputStream.toString();
	    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<html><head></head><body><ul><li><p>Hello</p></li></ul></body></html>", htmlcontent);
	}
	
	//
	// Test for bug #170
	//
	@Test
	public void zoom() throws IOException{
		String html = "<a href=\"knife.jpg\" title=\"<19\" class=\"zoom\">test</a>";
		
	    ByteArrayOutputStream htmlOutputStream = new ByteArrayOutputStream();

		HtmlCleaner cleaner = new HtmlCleaner();
	    CleanerProperties props = cleaner.getProperties();
	    props.setAllowHtmlInsideAttributes(true);
	    TagNode node = cleaner.clean(html);
	    new SimpleHtmlSerializer(props).writeToStream(node, htmlOutputStream);
	    String htmlcontent = htmlOutputStream.toString();
	    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<html><head></head><body><a href=\"knife.jpg\" title=\"&lt;19\" class=\"zoom\">test</a></body></html>", htmlcontent);
	}
	
	
	//
	// Test for bug #168 - if ns-aware is false, we shouldn't have any xmlns attributes
	//
	@Test
	public void ignoreNStest() throws IOException{
		String html = "<html xmlns=\"http://www.w3.org/1999/xhtml\"><head></head><body>";
		
	    ByteArrayOutputStream htmlOutputStream = new ByteArrayOutputStream();

		HtmlCleaner cleaner = new HtmlCleaner();
	    CleanerProperties props = cleaner.getProperties();
	    props.setNamespacesAware(false);
	    TagNode node = cleaner.clean(html);
	    new SimpleHtmlSerializer(props).writeToStream(node, htmlOutputStream);
	    String htmlcontent = htmlOutputStream.toString();
	    assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<html><head></head><body></body></html>", htmlcontent);
	}
	
	//
	// Test for bug #173
	//
	@Test
	public void loopTest() throws IOException{
		String html = "<html><head></head><body><P xmlns=\"http://somesite.eu/some_schema/export\">Some text</P><P>Other text.</P></body></html>";
		
	    ByteArrayOutputStream htmlOutputStream = new ByteArrayOutputStream();

		HtmlCleaner cleaner = new HtmlCleaner();
	    CleanerProperties props = cleaner.getProperties();
	    props.setNamespacesAware(true);
	    TagNode node = cleaner.clean(html);
	    new SimpleHtmlSerializer(props).writeToStream(node, htmlOutputStream);
	    String htmlcontent = htmlOutputStream.toString();
	    assertTrue(htmlcontent.contains("<html><head></head><body><P xmlns=\"http://somesite.eu/some_schema/export\">Some text</P><p>Other text.</p></body></html>"));
	}
	
	
	//
	// Test for bug #182
	//
	@Test
	public void directivesIgnoreQuestandExclaim() throws IOException{
		String html = "<table><tr><td><!==><!==>Hmailserver service shutdown:</td><td><!==><!==>Ok</td></tr></table>";
		
	    ByteArrayOutputStream htmlOutputStream = new ByteArrayOutputStream();

		HtmlCleaner cleaner = new HtmlCleaner();
	    CleanerProperties props = cleaner.getProperties();
	    props.setIgnoreQuestAndExclam(false);
	    props.setNamespacesAware(true);
	    TagNode node = cleaner.clean(html);
	    new SimpleHtmlSerializer(props).writeToStream(node, htmlOutputStream);
	    String htmlcontent = htmlOutputStream.toString();
	    assertTrue(htmlcontent.contains("<tr><td>&lt;!==&gt;&lt;!==&gt;Hmailserver service shutdown:</td><td>&lt;!==&gt;&lt;!==&gt;Ok</td></tr>"));
	}
	
	//
	// Test for bug #183
	//
	@Test
	public void casing() throws IOException{
		String html = ""
	            + "<svg xmlns=\"http://www.w3.org/2000/svg\">"
	            + "<TITLE>about</TITLE>"
	           + "</svg>"
	           + "<SPAN>About INMA</SPAN>";
		
	    ByteArrayOutputStream htmlOutputStream = new ByteArrayOutputStream();

		HtmlCleaner cleaner = new HtmlCleaner();
	    CleanerProperties props = cleaner.getProperties();
	    props.setNamespacesAware(true);
	    TagNode node = cleaner.clean(html);
	    new SimpleHtmlSerializer(props).writeToStream(node, htmlOutputStream);
	    String htmlcontent = htmlOutputStream.toString();
	    assertTrue(htmlcontent.contains("<html><head></head><body><svg xmlns=\"http://www.w3.org/2000/svg\"><TITLE>about</TITLE></svg><span>About INMA</span></body></html>"));
	}
	
	//
	// Test for bug #178
	//
	@Test
	public void arrayError(){

		final String HTML = 
				"<html>"
						+ "<body>"
						+ "<table>"
						+ "<ul>"
						+ "<p>d</p>"
						+ "</ul>"
						+ "<table>"
						+ "</table>"
						+ "</body>"
						+ "</html>";

		final HtmlCleaner cleaner = new HtmlCleaner();  
		cleaner.clean(HTML);
	}
	
	//
	// See issue #118
	//
	@Test
	public void nbsp() throws IOException{
		String html = "<b>One&nbsp;</b>Two";
		
	    ByteArrayOutputStream htmlOutputStream = new ByteArrayOutputStream();

		HtmlCleaner cleaner = new HtmlCleaner();
	    CleanerProperties props = cleaner.getProperties();
	    props.setTranslateSpecialEntities(false);
	    TagNode node = cleaner.clean(html);
	    new SimpleHtmlSerializer(props).writeToStream(node, htmlOutputStream);
	    String htmlcontent = htmlOutputStream.toString();
	    assertTrue(htmlcontent.contains("<b>One&nbsp;</b>Two"));
	}
	
	//
	// See issue #118
	//
	@Test
	public void pound() throws IOException{
		String html = "<b>&pound;160</b>";
		
	    ByteArrayOutputStream htmlOutputStream = new ByteArrayOutputStream();

		HtmlCleaner cleaner = new HtmlCleaner();
	    CleanerProperties props = cleaner.getProperties();
	    props.setTranslateSpecialEntities(false);
	    TagNode node = cleaner.clean(html);
	    new SimpleHtmlSerializer(props).writeToStream(node, htmlOutputStream);
	    String htmlcontent = htmlOutputStream.toString();
	    assertTrue(htmlcontent.contains("<b>&pound;160</b>"));
	}
	
	//
	// Test for issue #176
	//
	@Test
    public void invalidIUnicodeCodePoint()
    {
        final String HTML = "<html>"
                + "<body>Brine&#2013266066;s."
                + "</body>"
                + "</html>";
        try
        {
            final TagNode tagNode = new HtmlCleaner().clean(HTML);
            final CleanerProperties cleanerProperties = new CleanerProperties();
            new DomSerializer(cleanerProperties).createDOM(tagNode);
        }
        catch (IllegalArgumentException e)
        {
            fail();
        }
        catch (ParserConfigurationException e)
        {
            fail();
        }
    }
	
	//
	// Tests for \u0000 (UTF8 Null) - see issue #165
	//
	@Test
	public void UTFnulls() throws IOException{
        String input = "<html><body>\u0000</body></html>";
        InputStream is = new ByteArrayInputStream(input.getBytes());

        HtmlCleaner cleaner = new HtmlCleaner();
        cleaner.getProperties().setTranslateSpecialEntities(true);
        TagNode html = cleaner.clean(is, "UTF-8");

        String cleanHtml = new SimpleXmlSerializer(cleaner.getProperties()).getAsString(html);
        if(cleanHtml.contains("\u0000")) throw new AssertionError("U+0000 is an invalid XHTML char.");
	}
	
	@Test
	public void whiteSpace() throws IOException{
		String html = "<b>One </b>Two";
		TagNode node = cleaner.clean(html);
		StringWriter writer = new StringWriter();
		new PrettyHtmlSerializer(cleaner.getProperties(), " ")
				.serialize(node, writer);

	}
	
	//
	// MathML-specific test - see bug #172
	//
	@Test public void mtdMissingParentDefinition() throws IOException{ 
		String initial = "<math><mtable><mtr><mtd>S</mtd></mtr></mtable></math>"; 
		String expected = "<html><head /><body><math><mtable><mtr><mtd>S</mtd></mtr></mtable></math></body></html>"; 
		cleaner.getProperties().setAddNewlineToHeadAndBody(false); 
		cleaner.getProperties().setNamespacesAware(true); 
		TagNode cleaned = cleaner.clean(initial);
		String output = serializer.getAsString(cleaned); 
		assertEquals(expected, output); 
	}

	@Test
	public void testScriptEscape() throws IOException
	{
		final String input = "<head><script>a &lt; b</script></head>";
		HtmlCleaner cleaner = new HtmlCleaner();
		cleaner.getProperties().setUseCdataForScriptAndStyle(true);
		cleaner.getProperties().setAdvancedXmlEscape(true);
		cleaner.getProperties().setDeserializeEntities(true);
		TagNode cleaned = cleaner.clean(input);
			StringWriter writer = new StringWriter();
			serializer = new SimpleXmlSerializer(cleaner.getProperties());
			serializer.write(cleaned, writer, "UTF-8");
	}

	
	@Test
	public void testEscape() throws IOException
	{
		final String input = "<html><body><pre class=\"executable\">&lt;?xml version=\"1.0\"?&gt;<Root></Root></pre></body></html>";
		HtmlCleaner cleaner = new HtmlCleaner();
		TagNode cleaned = cleaner.clean(input);
			StringWriter writer = new StringWriter();
			serializer = new PrettyHtmlSerializer(cleaner.getProperties());
			serializer.write(cleaned, writer, "UTF-8");
	}

	/**

	 * @throws IOException 
	 */
    @Test
    @Ignore // Still an issue with this one - basically self-closing tags don't seem to close properly
    public void testSelfClosingTagNonHtml() throws IOException
    {
        final String input = "<html xmlns=\"FOO\"><head></head><body><table><tbody><tr><td></td></tr></tbody></table><BR /><div></div></body></html>";
    	final String expected = "<html xmlns=\"FOO\"><head></head><body><table><tbody><tr><td></td></tr></tbody></table><BR></BR><div></div></body></html>";
        TagNode cleaned = new HtmlCleaner().clean(input);
		StringWriter writer = new StringWriter();
		serializer = new SimpleHtmlSerializer(cleaner.getProperties());
		serializer.write(cleaned, writer, "UTF-8");
		assertEquals(expected, writer.toString());
    }
    
	/**

	 * @throws IOException 
	 */
    @Test
    public void testSelfClosingTag() throws IOException
    {
        final String input = "<html><head></head><body><table><tbody><tr><td></td></tr></tbody></table><BR /><div></div></body></html>";
    	final String expected = "<html><head></head><body><table><tbody><tr><td></td></tr></tbody></table><br /><div></div></body></html>";
        TagNode cleaned = new HtmlCleaner().clean(input);
		StringWriter writer = new StringWriter();
		serializer = new SimpleHtmlSerializer(cleaner.getProperties());
		serializer.write(cleaned, writer, "UTF-8");
		assertEquals(expected, writer.toString());
    }
    
	/**
	 * Test for bug #158
	 * @throws IOException 
	 */
    @Test
    public void testNPE() throws IOException
    {
        final String HTML = "<html xmlns=\"foo\" >"
                + "<head>"
                + "</head>"
                + "<body>"
                + "<table>"
                + "<tr>"
                + "<td>"
                + "<br></br>"
                + "</td>"   
                + "</tr>"
                + "</table>"
                + "<div>"
                + "</div>"
                + "</body>"
                + "</html>";
        final String expected = "<html xmlns=\"foo\"><head></head><body><table><tr><td><br /></td></tr></table><div></div></body></html>";
        TagNode cleaned = new HtmlCleaner().clean(HTML);
		StringWriter writer = new StringWriter();
		serializer = new SimpleHtmlSerializer(cleaner.getProperties());
		serializer.write(cleaned, writer, "UTF-8");
		assertEquals(expected, writer.toString());
    }
    
	/**
	 * Test for bug #156
	 * @throws IOException
	 */
	@Test
	public void testStyleIsNotRemoved() throws IOException{
		final String original = "<div><style type=\"text/css\">h1 {color:black;}</style>42</div>";
		final String expected = "<div><style type=\"text/css\">h1 {color:black;}</style>42</div>";

		cleaner.getProperties().setOmitHtmlEnvelope(true);
		TagNode node = cleaner.clean(original);
		StringWriter writer = new StringWriter();
		serializer = new SimpleHtmlSerializer(cleaner.getProperties());
		serializer.write(node, writer, "UTF-8");
		assertEquals(expected, writer.toString());
	}
	
	/**
	 * Test for bug #154
	 * @throws IOException
	 */
	@Test
	public void attributeSerialization() throws IOException{
	    final String original =     "<p data-double-quote-attr=\"foo&quot;bar'baz\" data-single-quote-attr='foo\"bar&apos;baz'>text</p>";
	    final String expectedHtml = "<p data-double-quote-attr=\"foo&quot;bar'baz\" data-single-quote-attr=\"foo&quot;bar'baz\">text</p>";
	    final String expectedXml =  "<p data-double-quote-attr=\"foo&quot;bar&apos;baz\" data-single-quote-attr=\"foo&quot;bar&apos;baz\">text</p>";

	    cleaner.getProperties().setOmitHtmlEnvelope(true);
	    TagNode node = cleaner.clean(original);
	    StringWriter writer = new StringWriter();
	    serializer = new SimpleHtmlSerializer(cleaner.getProperties());
	    serializer.write(node, writer, "UTF-8");
	    assertEquals(expectedHtml, writer.toString());

	    writer = new StringWriter();
	    serializer = new SimpleXmlSerializer(cleaner.getProperties());
	    serializer.write(node, writer, "UTF-8");
	    assertEquals(expectedXml, writer.toString());
	}

	/**
	 * This is to test issue #157
	 * @throws IOException
	 */
	@Test
	public void math() throws IOException{
		String initial = "<math><math></math></math>";
		String expected = "<html><head /><body><math></math><math></math></body></html>";
		cleaner.getProperties().setAddNewlineToHeadAndBody(false);
		cleaner.getProperties().setNamespacesAware(true);
        TagNode cleaned = cleaner.clean(initial);
        String output = serializer.getAsString(cleaned);
        assertEquals(expected, output);	
        
		initial = "<math><mtable><mtr></mtr></mtable></math>";
		expected = "<html><head /><body><math><mtable><mtr /></mtable></math></body></html>";
		cleaner.getProperties().setAddNewlineToHeadAndBody(false);
		cleaner.getProperties().setNamespacesAware(true);
        cleaned = cleaner.clean(initial);
        output = serializer.getAsString(cleaned);
        assertEquals(expected, output);	
        
		initial = "<math><mi>1</mi><math><mi>2</mi></math></math>";
		expected = "<html><head /><body><math><mi>1</mi></math><math><mi>2</mi></math></body></html>";
		cleaner.getProperties().setAddNewlineToHeadAndBody(false);
		cleaner.getProperties().setNamespacesAware(true);
        cleaned = cleaner.clean(initial);
        output = serializer.getAsString(cleaned);
        assertEquals(expected, output);	
	}
	
	
	/**
	 * This is to test issue #131
	 * @throws IOException
	 */
	//@Ignore // We should fix this, but it isn't critical
	//@Test
	public void moveTableContent() throws IOException{
		String initial = "<table><tbody><h2>hi</h2><tr><td></td></tr></tbody></table>";
		String expected = "<html><head /><body><h2>hi</h2><table><tbody><tr><td></td></tr></tbody></table></body></html>";
		cleaner.getProperties().setAddNewlineToHeadAndBody(false);
        TagNode cleaned = cleaner.clean(initial);
        String output = serializer.getAsString(cleaned);
        assertEquals(expected, output);	
	}
	
	/**
	 * This is to test issue #136
	 * @throws IOException
	 */
	@Test
	public void emptyXmlns() throws IOException{
		String initial = "<html><head><meta xmlns=\"\" name=\"a\" content=\"1\"><meta xmlns=\"\" name=\"b\" content=\"2\"><meta xmlns=\"\" name=\"c\" content=\"3\"><meta xmlns=\"\" name=\"d\" content=\"4\"></head><body></body></html>";
		String expected = "<html><head><meta name=\"a\" content=\"1\" /><meta name=\"b\" content=\"2\" /><meta name=\"c\" content=\"3\" /><meta name=\"d\" content=\"4\" /></head><body></body></html>";
		cleaner.getProperties().setAddNewlineToHeadAndBody(false);
		cleaner.getProperties().setNamespacesAware(true);
        TagNode cleaned = cleaner.clean(initial);
        String output = serializer.getAsString(cleaned);
        assertEquals(expected, output);	
	}
	
	
	/**
	 * This is to test issue #139
	 * @throws IOException
	 */
	@Test
	public void optGroupTest() throws IOException{
		String initial = "<select><optgroup><option>x</option></optgroup></select>";
		String expected = "<html><head /><body><select><optgroup><option>x</option></optgroup></select></body></html>";
		cleaner.getProperties().setAddNewlineToHeadAndBody(false);
        TagNode cleaned = cleaner.clean(initial);
        String output = serializer.getAsString(cleaned);
        assertEquals(expected, output);	
	}
	
	/**
	 * This is to test issue #149
	 * @throws IOException 
	 */
	@Test
	public void rbTest() throws IOException{
		
		String initial = "<p><br /> <br /><rb></rb><rtc><rt></rtc><br /></p>";
		String expected = "<html><head /><body><p><br /> <br /><br /><ruby><rb /><rtc><rt /></rtc></ruby></p></body></html>";
		cleaner.getProperties().setAddNewlineToHeadAndBody(false);
        TagNode cleaned = cleaner.clean(initial);
        String output = serializer.getAsString(cleaned);
        assertEquals(expected, output);	
	}
	
	// See bug #147
	@Test
	public void testCorrectUlStructure(){
		String initial = "<UL><LI>1</LI><LI>2</LI><UL></LI></UL><LI>3</LI></UL>";
		String expected = "<html><head /><body><ul><li>1</li><li>2</li><ul></ul><li>3</li></ul></body></html>";
		cleaner.getProperties().setAddNewlineToHeadAndBody(false);
        TagNode cleaned = cleaner.clean(initial);
        String output = serializer.getAsString(cleaned);
        assertEquals(expected, output);	
	}
	
	// See bug #145
	@Test
	@Ignore // We do want to fix this, but its not critical
	public void testCorrectTableStructure(){
		String initial = "<table><tr><td><div><td></div></td> </tr></table>";
		String expected = "<html><head /><body><table><tbody><tr><td><div></div></td><td></td></tr></tbody></table></body></html>";
		cleaner.getProperties().setAddNewlineToHeadAndBody(false);
        TagNode cleaned = cleaner.clean(initial);
        String output = serializer.getAsString(cleaned);
        assertEquals(expected, output);	
	}
	
	// See bug #146
	@Test
	public void testMissingTr(){
		String initial = "<table><td>banana</td></table>";
		String expected = "<html><head /><body><table><tbody><tr><td>banana</td></tr></tbody></table></body></html>";
		cleaner.getProperties().setAddNewlineToHeadAndBody(false);
        TagNode cleaned = cleaner.clean(initial);
        String output = serializer.getAsString(cleaned);
        assertEquals(expected, output);	
    }
	
	// See bug #129
	@Test
	public void testLegend(){
		String initial = "<form><legend>banana";
		String expected = "<html><head /><body><form><fieldset><legend>banana</legend></fieldset></form></body></html>";
		cleaner.getProperties().setAddNewlineToHeadAndBody(false);
        TagNode cleaned = cleaner.clean(initial);
        String output = serializer.getAsString(cleaned);
        assertEquals(expected, output);	
    }
	
	// See bug #126
	@Test
	public void testFragment(){
		String initial = "<table><rt><td>";
		String expected = "<html><head /><body><ruby><rt /></ruby><table><tbody><tr><td></td></tr></tbody></table></body></html>";
		cleaner.getProperties().setAddNewlineToHeadAndBody(false);
        TagNode cleaned = cleaner.clean(initial);
        String output = serializer.getAsString(cleaned);
        assertEquals(expected, output);	
    }
	
	// See bug #140
	@Test
	public void testSource(){
		String initial = "<source />";
		String expected = "<html><head /><body><audio><source /></audio></body></html>";
		cleaner.getProperties().setAddNewlineToHeadAndBody(false);
        TagNode cleaned = cleaner.clean(initial);
        String output = serializer.getAsString(cleaned);
        assertEquals(expected, output);	
    }
	
	@Test
	public void testTwiddleTR(){
		String initial = "<table><tr><td>test</td></rt></table>";
		String expected = "<html><head /><body><table><tbody><tr><td>test</td></tr></tbody></table></body></html>";
		cleaner.getProperties().setAddNewlineToHeadAndBody(false);
        TagNode cleaned = cleaner.clean(initial);
        String output = serializer.getAsString(cleaned);
        assertEquals(expected, output);	
    }
	
	@Test
	public void testMissingRuby(){
		String initial = "<rt>test</rt>";
		String expected = "<html><head /><body><ruby>"+initial+"</ruby></body></html>";
		cleaner.getProperties().setAddNewlineToHeadAndBody(false);
        TagNode cleaned = cleaner.clean(initial);
        String output = serializer.getAsString(cleaned);
        assertEquals(expected, output);	
    }
	
	@Test
	public void testMissingRt(){
		String initial = "<rp>(</rp>ㄏㄢˋ<rp>)</rp>";
		String expected = "<html><head /><body><ruby>"+initial+"</ruby></body></html>";
		cleaner.getProperties().setAddNewlineToHeadAndBody(false);
        TagNode cleaned = cleaner.clean(initial);
        String output = serializer.getAsString(cleaned);
        assertEquals(expected, output);	
	}
	
	/**
	 * Label tag - see Bug #138
	 */
	@Test
	public void testLabel(){
		String initial = "<form><label for=\"male\">Male</label><input type=\"radio\" name=\"sex\" id=\"male\" value=\"male\" /><label for=\"female\">Female</label><input type=\"radio\" name=\"sex\" id=\"female\" value=\"female\" /><input type=\"submit\" value=\"Submit\" /></form>";
		String expected = "<html><head /><body>"+initial+"</body></html>";
		cleaner.getProperties().setNamespacesAware(true);
		cleaner.getProperties().setAddNewlineToHeadAndBody(false);
        TagNode cleaned = cleaner.clean(initial);
        String output = serializer.getAsString(cleaned);
        assertEquals(expected, output);		
	}
	
	/**
	 * Option tags have two fatal tags - see Bug #137
	 */
	@Test
	public void testSelect(){
		String initial = "<select><option>test1</option></select>";
		String expected = "<html><head /><body><select><option>test1</option></select></body></html>";
		cleaner.getProperties().setNamespacesAware(true);
		cleaner.getProperties().setAddNewlineToHeadAndBody(false);
        TagNode cleaned = cleaner.clean(initial);
        String output = serializer.getAsString(cleaned);
        assertEquals(expected, output);		
	}
	
	/**
	 * This is to test that we don't get an NPE with a malformed HTTPS XHTML namespace. See issue #133
	 */
	@Test
	public void testNPEWithHttpsNamespace(){
		String initial="<html xmlns=\"https://www.w3.org/1999/xhtml\"><head></head><body><SPAN><BR></SPAN><EM></EM></body></html>";
		String expected="<html xmlns=\"http://www.w3.org/1999/xhtml\"><head /><body><span><br /></span><em></em></body></html>";
		cleaner.getProperties().setNamespacesAware(true);
		cleaner.getProperties().setAddNewlineToHeadAndBody(false);
        TagNode cleaned = cleaner.clean(initial);
        String output = serializer.getAsString(cleaned);
        assertEquals(expected, output);		
	}
	
	/**
	 * This is to test issue #132
	 * @throws IOException 
	 */
	@Test
	public void classCastTest() throws IOException{
		String initial = readFile("src/test/resources/test30.html");
		TagNode node = cleaner.clean(initial);
	}
	
	/**
	 * This is to test issue #93
	 */
	@Test
	public void closingDiv(){
		//
		// Check that when a tag is self-closing, we close it and start again rather than
		// let it remain open and enclose the following tags
		//
		String initial = "<div id=\"y\"/><div id=\"z\">something</div>";
		String expected = "<html>\n<head />\n<body><div id=\"y\"></div><div id=\"z\">something</div></body></html>";
        TagNode cleaned = cleaner.clean(initial);
        String output = serializer.getAsString(cleaned);
        assertEquals(expected, output);
        
        //
        // This should also result in the same output
        //
        initial = "<div id=\"y\"></div><div id=\"z\">something</div>";
        cleaned = cleaner.clean(initial);
        output = serializer.getAsString(cleaned);
        assertEquals(expected, output);
	}


    /**
     * This is to test issue #67
     */
    @Test
    public void testXmlNoExtraWhitesapce(){
        CleanerProperties cleanerProperties = new CleanerProperties();
        cleanerProperties.setOmitXmlDeclaration(false);
        cleanerProperties.setOmitDoctypeDeclaration(false);
        cleanerProperties.setIgnoreQuestAndExclam(true);
        cleanerProperties.setAddNewlineToHeadAndBody(false);
 
    	HtmlCleaner theCleaner = new HtmlCleaner(cleanerProperties);

        String initial = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n<html><head /><body><p>test</p></body></html>\n";
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n<html><head /><body><p>test</p></body></html>";

        TagNode cleaned = theCleaner.clean(initial);
                
        Serializer theSerializer = new SimpleXmlSerializer(theCleaner.getProperties());
        String output = theSerializer.getAsString(cleaned);
        assertEquals(expected, output);
    }
    
    /**
     * Test for #2901.
     */
    @Test
	public void testWhitespaceInHead() throws IOException {
		String initial = readFile("src/test/resources/Real_1.html");
		String expected = readFile("src/test/resources/Expected_1.html");
		assertCleaned(initial, expected);
	}

	/**
	 * Mentioned in #2901 - we should eliminate the first <tr>
	 * TODO: Passes but not with ideal result.
	 */
    @Test
	public void testUselessTr() throws IOException {
		cleaner.getProperties().setAddNewlineToHeadAndBody(false);
		String start = "<html><head /><body><table>";
		String end = "</body></html>";
		assertCleaned(start + "<tr><tr><td>stuff</td></tr>" + end,
				//start+"<tbody><tr><td>stuff</td></tr></tbody></table>" + end // "ideal" output
				start + "<tbody><tr /><tr><td>stuff</td></tr><tr></tr></tbody></table>" + end // actual
		);
	}

	/**
	 * Collapsing empty tr to <tr />
	 */
    @Test
	public void testUselessTr2() throws IOException {
		cleaner.getProperties().setAddNewlineToHeadAndBody(false);
		String start = "<html><head /><body><table>";
		String end = "</table></body></html>";
		assertCleaned(start + "<tr> </tr><tr><td>stuff</td></tr>" + end,
				start + "<tbody><tr /><tr><td>stuff</td></tr></tbody>" + end);
	}

	/**
	 * For #2940
	 */
    @Test
	public void testCData() throws IOException {
		cleaner.getProperties().setAddNewlineToHeadAndBody(false);
		String start = "<html><head>";
		String end = "</head><body>1</body></html>";
		assertCleaned(start + "<style type=\"text/css\">/*<![CDATA[*/\n#ampmep_188 { }\n/*]]>*/</style>" + end,
				start + "<style type=\"text/css\">/*<![CDATA[*/\n#ampmep_188 { }\n/*]]>*/</style>" + end);
	}

	/**
	 * Report in issue #64 as causing issues.
	 * @throws Exception
	 */
    @Test
	public void testChineseParsing() throws Exception {
	    String initial = readFile("src/test/resources/test-chinese-issue-64.html");
	    TagNode node = cleaner.clean(initial);
	    final TagNode[] imgNodes = node.getElementsByName("img", true);
	    assertEquals(5, imgNodes.length);
	}
    
    /**
     * Report in issue #70 as causing issues.
     * @throws Exception
     */
    @Test
    public void testOOME_70() throws Exception {
        String initial = readFile("src/test/resources/oome_70.html");
        TagNode node = cleaner.clean(initial);
        final TagNode[] imgNodes = node.getElementsByName("img", true);
        assertEquals(17, imgNodes.length);
    }

    @Test
    public void testOOME_59() throws Exception {
        String in = "<html><body><table><fieldset><legend>";
        CleanerProperties cp = new CleanerProperties();
        cp.setOmitUnknownTags(true);
        HtmlCleaner c = new HtmlCleaner(cp);
        TagNode root = c.clean(in);
        assertEquals(1, root.getElementsByName("legend", true).length);
    }
    
    /**
     * Check that we no longer require block-level restrictions for anchors, as per HTML5. See issue #82
     * @throws IOException
     */
	@Test
	public void noAnchorBlockLevelRestriction() throws IOException{
        
		String initial = readFile("src/test/resources/test24.html");
		String expected = readFile("src/test/resources/test24_expected.html"); 
		
		assertCleaned(initial,expected);
	}
}
