package org.htmlcleaner;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Tests for some common use of <script> tags within <head> elements
 * @author scottw
 *
 */
public class ScriptTest extends AbstractHtmlCleanerTest {
	
	@Test
	public void another() throws IOException{
		HtmlCleaner htmlCleaner = new HtmlCleaner();
		CleanerProperties props = htmlCleaner.getProperties();
		props.setAllowHtmlInsideAttributes(true);
		props.setAllowMultiWordAttributes(true);
		props.setRecognizeUnicodeChars(true);
		props.setOmitComments(true);
		TagNode rootNode = htmlCleaner.clean(new File("src/test/resources/script_test.html"));
	}
	
	
	@Test
	public void getScripts() throws IOException{
	    HtmlCleaner cleaner = new HtmlCleaner();
        TagNode html = cleaner.clean( new File("src/test/resources/script_test.html") );
        TagNode head = html.findElementByName("head", false);
        
        ArrayList<TagNode> scripts = new ArrayList<TagNode>();
		List<TagNode> children = head.getChildTagList();	
		
		for(TagNode child : children){						
			if(child.getName().equals("script")){				
				scripts.add(child);
			}			
		}
		assertEquals(3, scripts.size());
		assertEquals("x.js", scripts.get(0).getAttributeByName("src"));
		assertEquals("y.js", scripts.get(1).getAttributeByName("src"));
		assertEquals("z.js", scripts.get(2).getAttributeByName("src"));

	}
	
	@Test
	public void scriptAttribute() throws IOException{
		cleaner.getProperties().setUseCdataForScriptAndStyle(true);
		String initial = "<button onclick='aaa(\"bbb\")'>Click here!</button>";
		String expected ="<html>\n<head />\n<body><button onclick=\"aaa(&quot;bbb&quot;)\">Click here!</button></body></html>";
		assertCleaned(initial, expected);
	}
	
	/*
	 * Test for issue #88 - thanks to Serge Dyomin
	 */
	@Test
	public void scriptAttributeQuotes() throws IOException{
		 HtmlCleaner thecleaner=new HtmlCleaner();
         CleanerProperties props = thecleaner.getProperties();
         props.setOmitXmlDeclaration(true);
         props.setOmitComments(false);  
         props.setTranslateSpecialEntities(true);  
         
        String initial = readFile("src/test/resources/test16.html");
        String expected = readFile("src/test/resources/test16_expected.html"); 
        String output = new SimpleHtmlSerializer(thecleaner.getProperties()).getAsString(thecleaner.clean(initial));
        
        assertEquals(expected,output);
	}
}
