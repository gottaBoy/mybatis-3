/**
 *    Copyright 2009-2019 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package org.apache.ibatis.parsing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.ibatis.builder.BuilderException;
import org.apache.ibatis.io.Resources;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

class XPathParserTest {

  @Test
  void shouldTestXPathParserMethods() throws Exception {
    String resource = "resources/nodelet_test.xml";
    try (InputStream inputStream = Resources.getResourceAsStream(resource)) {
      XPathParser parser = new XPathParser(inputStream, false, null, null);
      assertEquals((Long) 1970L, parser.evalLong("/employee/birth_date/year"));
      assertEquals((short) 6, (short) parser.evalShort("/employee/birth_date/month"));
      assertEquals((Integer) 15, parser.evalInteger("/employee/birth_date/day"));
      assertEquals((Float) 5.8f, parser.evalFloat("/employee/height"));
      assertEquals((Double) 5.8d, parser.evalDouble("/employee/height"));
      assertEquals("${id_var}", parser.evalString("/employee/@id"));
      assertEquals(Boolean.TRUE, parser.evalBoolean("/employee/active"));
      assertEquals("<id>${id_var}</id>", parser.evalNode("/employee/@id").toString().trim());
      assertEquals(7, parser.evalNodes("/employee/*").size());
      XNode node = parser.evalNode("/employee/height");
      assertEquals("employee/height", node.getPath());
      assertEquals("employee[${id_var}]_height", node.getValueBasedIdentifier());
    }
  }


  @Test
  void xpathTest() throws Exception {
    StringBuilder xmlStringBuilder = new StringBuilder();
    xmlStringBuilder.append("<?xml version=\"1.0\"?> <class>1970</class>");
    ByteArrayInputStream inputStream =  new ByteArrayInputStream(
            xmlStringBuilder.toString().getBytes("UTF-8"));
      XPathParser parser = new XPathParser(inputStream, false, null, null);
      assertEquals((Long) 1970L, parser.evalLong("/class"));
  }

  private Document createDocument(InputSource inputSource) {
    // important: this must only be called AFTER common constructor
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setValidating(false);

      factory.setNamespaceAware(false);
      factory.setIgnoringComments(true);
      factory.setIgnoringElementContentWhitespace(false);
      factory.setCoalescing(false);
      factory.setExpandEntityReferences(true);

      DocumentBuilder builder = factory.newDocumentBuilder();
      builder.setEntityResolver(null);
      builder.setErrorHandler(new ErrorHandler() {
        @Override
        public void error(SAXParseException exception) throws SAXException {
          throw exception;
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
          throw exception;
        }

        @Override
        public void warning(SAXParseException exception) throws SAXException {
        }
      });
      return builder.parse(inputSource);
    } catch (Exception e) {
      throw new BuilderException("Error creating document instance.  Cause: " + e, e);
    }
  }
}
