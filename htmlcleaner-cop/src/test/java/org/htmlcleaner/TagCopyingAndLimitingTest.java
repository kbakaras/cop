/*  Copyright (c) 2006-2014, The HtmlCleaner Project
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
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.parsers.ParserConfigurationException;

import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import junit.framework.TestCase;

/**
 * Tests the effect of successively having to copy identical tags in a list.
 */
public class TagCopyingAndLimitingTest extends TestCase {

    public void testTagCopyingAndLimitingHTML4() throws IOException, ParserConfigurationException {
    	StringBuilder sb = new StringBuilder();
        sb.append("<ul>");
        for (int i = 1; i <= 10; i++) {
            sb.append("<li><font>Item ").append(i).append("</font><font size=1>");
        }
        HtmlCleaner cleaner = new HtmlCleaner();
        cleaner.getProperties().setOmitXmlDeclaration(true);
        cleaner.getProperties().setHtmlVersion(HtmlCleaner.HTML_4); //Run with Html4TagProvider
        TagNode cleanedNode = cleaner.clean(new StringReader(sb.toString()));

        TagNode expectedNode = new TagNode("html");
        expectedNode.addChild(new TagNode("head"));
        TagNode bodyNode = new TagNode("body");
        expectedNode.addChild(bodyNode);
        TagNode listNode = new TagNode("ul");
        bodyNode.addChild(listNode);
        for (int i = 1; i <= 10; i++) {
            TagNode itemTag = new TagNode("li");
            listNode.addChild(itemTag);
            TagNode lastTag = itemTag;
            int fontTagsToAdd = Math.min(i - 1, 3); // mimic the limit added for the number of times identical tokens can be copied
            for (int n = 0; n < fontTagsToAdd; n++) {
                TagNode fontTag = new TagNode("font");
                fontTag.addAttribute("size", "1");
                lastTag.addChild(fontTag);
                lastTag = fontTag;
            }
            TagNode itemFontTag = new TagNode("font");
            itemFontTag.addChild(new ContentNode("Item " + i));
            lastTag.addChild(itemFontTag);
            TagNode fontTag = new TagNode("font");
            fontTag.addAttribute("size", "1");
            lastTag.addChild(fontTag);
        }

        String cleanedOutput = getOutput(cleanedNode, cleaner.getProperties());
        String expectedOutput = getOutput(expectedNode, cleaner.getProperties());
        assertEquals(expectedOutput, cleanedOutput);
    }
    
    //Testing with tag <b>
    public void testTagCopyingAndLimitingHTML5() throws IOException, ParserConfigurationException {
        StringBuilder sb = new StringBuilder();
        sb.append("<ul>");
        for (int i = 1; i <= 10; i++) {
            sb.append("<li><b>Item ").append(i).append("</b><b size=1>");
        }
        HtmlCleaner cleaner = new HtmlCleaner();
        cleaner.getProperties().setOmitXmlDeclaration(true);
        cleaner.getProperties().setHtmlVersion(HtmlCleaner.HTML_5); //Run with Html5TagProvider
        TagNode cleanedNode = cleaner.clean(new StringReader(sb.toString()));

        TagNode expectedNode = new TagNode("html");
        expectedNode.addChild(new TagNode("head"));
        TagNode bodyNode = new TagNode("body");
        expectedNode.addChild(bodyNode);
        TagNode listNode = new TagNode("ul");
        bodyNode.addChild(listNode);
        for (int i = 1; i <= 10; i++) {
            TagNode itemTag = new TagNode("li");
            listNode.addChild(itemTag);
            TagNode lastTag = itemTag;
            int fontTagsToAdd = Math.min(i - 1, 3); // mimic the limit added for the number of times identical tokens can be copied
            for (int n = 0; n < fontTagsToAdd; n++) {
                TagNode fontTag = new TagNode("b");
                fontTag.addAttribute("size", "1");
                lastTag.addChild(fontTag);
                lastTag = fontTag;
            }
            TagNode itemFontTag = new TagNode("b");
            itemFontTag.addChild(new ContentNode("Item " + i));
            lastTag.addChild(itemFontTag);
            TagNode fontTag = new TagNode("b");
            fontTag.addAttribute("size", "1");
            lastTag.addChild(fontTag);
        }

        String cleanedOutput = getOutput(cleanedNode, cleaner.getProperties());
        String expectedOutput = getOutput(expectedNode, cleaner.getProperties());
        assertEquals(expectedOutput, cleanedOutput);
    }
    
    

    private static String getOutput(TagNode node, CleanerProperties properties) throws IOException {
        Document jdom = new JDomSerializer(properties).createJDom(node);
        Format format = Format.getPrettyFormat();
        format.setIndent("  ");
        format.setOmitDeclaration(true);
        format.setExpandEmptyElements(true);
        XMLOutputter outputter = new XMLOutputter(format);
        StringWriter writer = new StringWriter();
        outputter.output(jdom, writer);
        return writer.toString();
    }
}
