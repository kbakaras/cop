package org.htmlcleaner;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * 
 * @author Eugene Sapozhnikov (blackorangebox@gmail.com)
 *
 */
public class UtilsTest extends Utils {
	
	@Test
	public void sanitizeHtmlAttributeNamesTest(){
		assertEquals("banana", Utils.sanitizeHtmlAttributeName("banana"));
		assertEquals("banana", Utils.sanitizeHtmlAttributeName("ban>ana"));
		assertEquals("banana", Utils.sanitizeHtmlAttributeName(">ban>ana"));
		assertEquals("banana", Utils.sanitizeHtmlAttributeName("ban/ana"));
		assertEquals("banana", Utils.sanitizeHtmlAttributeName("ban ana"));
		assertEquals("banana", Utils.sanitizeHtmlAttributeName("ban=\"ana\""));
		assertFalse(Utils.isValidHtmlAttributeName("ban=ana"));
		assertTrue(Utils.isValidHtmlAttributeName("banana"));

	}
	
	@Test
	public void tokenizeTest(){
		assertEquals(3, Utils.tokenize("ba-na-na", "-").length);
	}
	
	@Test
	public void numberUtilsTest(){
		assertTrue(Utils.isValidInt("1", 10));
		assertTrue(Utils.isValidInt("-10", 10));
		assertFalse(Utils.isValidInt("banana", 10));
	}
	
	@Test
	public void characterUtilsTest(){
		assertEquals("l", Utils.bchomp("\nl\n"));
		assertEquals("l", Utils.chomp("l\n"));
		assertEquals("lx", Utils.chomp("lx\n"));
		assertEquals("l", Utils.chomp("l"));
		assertEquals("", Utils.chomp("\n"));
		assertEquals("", Utils.chomp("\r"));
		assertEquals("", Utils.chomp("\r\n"));
		assertEquals("", Utils.chomp("\n"));
		assertEquals("", Utils.chomp(""));
		assertEquals("l", Utils.chomp("l\r"));
		assertEquals("lx", Utils.chomp("lx"));
		assertEquals("l", Utils.lchomp("\nl"));
		assertEquals("l", Utils.lchomp("l"));
		assertEquals("", Utils.lchomp("\n"));
		assertEquals("", Utils.lchomp(""));
		assertEquals("l", Utils.lchomp("\n\rl"));
		assertEquals("", Utils.lchomp("\n\r"));
		assertEquals("", Utils.lchomp("\r"));
		assertEquals("", Utils.ltrim(" "));
		assertEquals("banana", Utils.ltrim(" banana"));
		assertNull(Utils.ltrim(null));
		assertNull(Utils.lchomp(null));
		assertFalse(Utils.isWhitespaceString(null));
		assertTrue(Utils.isWhitespaceString(" "));
		assertTrue(Utils.isWhitespaceString(" \n "));
		assertFalse(Utils.isWhitespaceString(" banana "));
	}
	
	@Test
	public void validXmlIdentifiersTest(){
		assertTrue(Utils.isValidXmlIdentifier("p"));
		assertFalse(Utils.isValidXmlIdentifier("<^^>"));
		assertFalse(Utils.isValidXmlIdentifier("~~"));
		assertFalse(Utils.isValidXmlIdentifier("="));
		assertFalse(Utils.isValidXmlIdentifier(null));		
	}
	
	@Test
	public void xmlStartCharactersTest(){
		assertTrue(Utils.isValidXmlIdentifierStartChar("p"));
		assertFalse(Utils.isValidXmlIdentifierStartChar("-"));
		assertFalse(Utils.isValidXmlIdentifierStartChar("."));
		assertFalse(Utils.isValidXmlIdentifierStartChar("1"));
	}
	
	@Test
	public void sanitizeXmlIdentifiersTest(){
		assertEquals("p", Utils.sanitizeXmlIdentifier("p<>"));
		assertEquals("p20", Utils.sanitizeXmlIdentifier("p%20"));
		assertEquals("pop", Utils.sanitizeXmlIdentifier("p<>op"));
		assertEquals("pp", Utils.sanitizeXmlIdentifier("p<p%"));
		assertEquals("hc-generated-1p", Utils.sanitizeXmlIdentifier("1p"));
		assertEquals(null, Utils.sanitizeXmlIdentifier(";^^>"));
		assertEquals("pop", Utils.sanitizeXmlIdentifier("1pop",""));
		assertEquals("p", Utils.sanitizeXmlIdentifier("1p",""));
		assertEquals("p", Utils.sanitizeXmlIdentifier("1p",""));
		assertEquals(null, Utils.sanitizeXmlIdentifier("1",""));

	}
	
	@Test
	public void convertNcr(){
		// &#8364;	to &euro;
		StringBuilder builder = new StringBuilder();
		assertEquals("&euro;", Utils.escapeXml("&#8364",false,false,false,false,false,false,true));
	}
	
	/**
	 * Test for code points above 65535 - see bug #152
	 */
	@Test
	public void testConvertUnicode(){
		String result = new String("UTF-8");
		
		String input = "&#128526;";
		String output = "😎";
		result = Utils.escapeXml(input, true, true, true, false, false, false);
		assertEquals(output, result);
		
		input = "&#128591;";
		output = "🙏";
		result = Utils.escapeXml(input, true, true, true, false, false, false);
		assertEquals(output, result);
	}
	
	@Test
    public void testEscapeXml_transResCharsToNCR() {
        String res = Utils.escapeXml("1.&\"'<>", true, true, true, false, true, false);
        assertEquals("1.&#38;&#34;&#39;&#60;&#62;", res);
        
        res = Utils.escapeXml("2.&amp;&quot;&apos;&lt;&gt;", true, true, true, false, true, false);
        assertEquals("2.&#38;&#34;&#39;&#60;&#62;", res);
        
        res = Utils.escapeXml("1.&\"'<>", true, true, true, false, false, false);
        assertEquals("1.&amp;&quot;&apos;&lt;&gt;", res);
        
        res = Utils.escapeXml("2.&amp;&quot;&apos;&lt;&gt;", true, true, true, false, false, false);
        assertEquals("2.&amp;&quot;&apos;&lt;&gt;", res);
    }
    
	@Test
    public void testEscapeXml_recognizeUnicodeChars() {
        String res = Utils.escapeXml("[&alpha;][&eacute;][&oline;]", true, false, true, false, false, false);
        assertEquals("[&#945;][&#233;][&#8254;]", res);
        
        res = Utils.escapeXml("[&alpha;][&eacute;][&oline;][&#931;]", true, true, true, false, false, false);
        assertEquals("[α][é][‾][Σ]", res);
    }
    
	@Test
    public void testEscapeXml_transSpecialEntitiesToNCR_withHex() {
        String res = Utils.escapeXml("&#x27;&#xa1;", true, false, true, false, false, true);
        assertEquals("&#x27;&#xa1;", res);   
        
        res = Utils.escapeXml("&#39;&#161;", true, false, true, false, false, true);
        assertEquals("&#39;&#161;", res);   
        
        res = Utils.escapeXml("&#x27;&#xa1;", true, false, true, false, false, false);
        assertEquals("&apos;¡", res);   
    }
}
