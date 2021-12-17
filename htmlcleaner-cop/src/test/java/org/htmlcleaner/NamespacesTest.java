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

import java.io.IOException;

import org.junit.Test;

public class NamespacesTest  extends AbstractHtmlCleanerTest{


	/**
	 * Tests that we can add in the xlink NS declaration automatically if there is an xlink:href attribute with 
	 * no xmlns attribute.
	 * @throws IOException
	 */
	@Test
	public void missingDeclaration() throws IOException{
		String initial = "<p xlink:href=\"#someHeading\"/>";
		String expected = "<html xmlns:xlink=\"http://www.w3.org/1999/xlink\">\n<head />\n<body><p xlink:href=\"#someHeading\"></p></body></html>";
		assertCleaned(initial, expected);
	}
	
	/**
	 * Tests that we can handle XMLNS="" attributes. See issue #135
	 * @throws IOException
	 */
	@Test
	public void xmlnsAttributeInUpperCase() throws IOException{
		String initial = "<BANANA XMLNS=\"BANANA\"/>";
		String expected = "<html>\n<head />\n<body><BANANA XMLNS=\"BANANA\" /></body></html>";
		assertCleaned(initial, expected);
	}
	@Test
	public void xmlnsAttributeAndPrefix() throws IOException{
		String initial = "\n<head />\n<body><xxx:BANANA xmlns:xxx=\"http://www.w3.org/1998/Math/MathML\"/>";
		String expected = "<html>\n<head />\n<body>\n<xxx:BANANA xmlns:xxx=\"http://www.w3.org/1998/Math/MathML\" /></body></html>";
		assertCleaned(initial, expected);
	}
	@Test
	public void xmlnsAttributeAndPrefix2() throws IOException{
		String initial = "<xxx:BANANA xmlns:xxx=\"http://www.w3.org/1998/Math/MathML\"/>";
		String expected = "<html>\n<head />\n<body><xxx:BANANA xmlns:xxx=\"http://www.w3.org/1998/Math/MathML\" /></body></html>";
		assertCleaned(initial, expected);
	}
	
	/**
	 * Tests that we can handle xmlns="" attributes. See issue #135
	 * @throws IOException
	 */
	@Test
	public void emptyNamespaces() throws IOException{
		String initial = readFile("src/test/resources/test32.html");
		String expected = "<html>\n<head />\n<body><a href=\"link.html\"><img /></a><p>Text</p></body></html>";
		assertCleaned(initial, expected);
	}
	
	/**
	 * Uses an RDFa example to test that we retain namespace declarations. See issue #63
	 * @throws IOException
	 */
    @Test
    public void RDFa() throws IOException{
		String initial = readFile("src/test/resources/test13.html");
		String expected = readFile("src/test/resources/test13_expected.html");
		assertCleaned(initial, expected);
    }
    
    /**
     * Uses a namespace prefix for an element. See issue #63
     * @throws IOException
     */
    @Test
    public void DCElement() throws IOException{
		String initial = readFile("src/test/resources/test14.html");
		String expected = readFile("src/test/resources/test14_expected.html");
		assertCleaned(initial, expected);
    }

    /**
     * Uses a namespace prefix for an attribute. See issue #63
     * @throws IOException
     */
    @Test
    public void DCAttribute() throws IOException{
		String initial = readFile("src/test/resources/test15.html");
		String expected = readFile("src/test/resources/test15_expected.html");
		assertCleaned(initial, expected);
    }
    
    /**
     * If we aren't NS aware, strip out the xmlns attr and process everything 
     * as HTML.
     */
	@Test
	public void testTableCellsWithoutNamespaceAwareness() throws IOException{
		cleaner.getProperties().setNamespacesAware(false);
		String initial = readFile("src/test/resources/test26.html");
		String expected = readFile("src/test/resources/test26_expected.html");
		assertCleaned(initial, expected);
	}
	
	/**
	 * If we are namespace-aware and use the legacy HTML namespace, we should 
	 * treat the content as HTML. See issue #115
	 */
	@Test
	public void testTableCellsUsingNamespaceAwareAndLegacyHtmlNS() throws IOException{
		cleaner.getProperties().setNamespacesAware(true);
		cleaner.getProperties().setOmitUnknownTags(true);
		String initial = readFile("src/test/resources/test26.html");
		String expected = readFile("src/test/resources/test26_expected.html");
		assertCleaned(initial, expected);
	}
	
	/**
	 * If we're NS-aware and using XHTML, treat the content as HTML tags and 
	 * insert TBODY into the table (etc) but retain the xmlns attr on the html 
	 * tag
	 */
	@Test
	public void testTableCellsUsingNamespaceAwareAndXhtmlNS() throws IOException{
		cleaner.getProperties().setNamespacesAware(true);
		cleaner.getProperties().setOmitUnknownTags(true);
		String initial = readFile("src/test/resources/test27.html");
		String expected = readFile("src/test/resources/test27_expected.html");
		assertCleaned(initial, expected);
	}
	
	/**
	 * If we are namespace-aware and use an unknown namespace,
	 * all the content will be treated as foreign markup; this means
	 * there will be no insertion of TBODY tags as the table element
	 * is not interpreted as being a HTML table element
	 */
	@Test
	public void testTableCellsUsingNamespaceAwareAndUnknownNS() throws IOException{
		cleaner.getProperties().setNamespacesAware(true);
		cleaner.getProperties().setOmitUnknownTags(true);
		String initial = readFile("src/test/resources/test28.html");
		String expected = readFile("src/test/resources/test28_expected.html");
		assertCleaned(initial, expected);
	}
}
