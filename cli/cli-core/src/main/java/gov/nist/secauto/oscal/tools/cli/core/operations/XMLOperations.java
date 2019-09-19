/**
 * Portions of this software was developed by employees of the National Institute
 * of Standards and Technology (NIST), an agency of the Federal Government.
 * Pursuant to title 17 United States Code Section 105, works of NIST employees are
 * not subject to copyright protection in the United States and are considered to
 * be in the public domain. Permission to freely use, copy, modify, and distribute
 * this software and its documentation without fee is hereby granted, provided that
 * this notice and disclaimer of warranty appears in all copies.
 *
 * THE SOFTWARE IS PROVIDED 'AS IS' WITHOUT ANY WARRANTY OF ANY KIND, EITHER
 * EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT LIMITED TO, ANY WARRANTY
 * THAT THE SOFTWARE WILL CONFORM TO SPECIFICATIONS, ANY IMPLIED WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, AND FREEDOM FROM
 * INFRINGEMENT, AND ANY WARRANTY THAT THE DOCUMENTATION WILL CONFORM TO THE
 * SOFTWARE, OR ANY WARRANTY THAT THE SOFTWARE WILL BE ERROR FREE. IN NO EVENT
 * SHALL NIST BE LIABLE FOR ANY DAMAGES, INCLUDING, BUT NOT LIMITED TO, DIRECT,
 * INDIRECT, SPECIAL OR CONSEQUENTIAL DAMAGES, ARISING OUT OF, RESULTING FROM, OR
 * IN ANY WAY CONNECTED WITH THIS SOFTWARE, WHETHER OR NOT BASED UPON WARRANTY,
 * CONTRACT, TORT, OR OTHERWISE, WHETHER OR NOT INJURY WAS SUSTAINED BY PERSONS OR
 * PROPERTY OR OTHERWISE, AND WHETHER OR NOT LOSS WAS SUSTAINED FROM, OR AROSE OUT
 * OF THE RESULTS OF, OR USE OF, THE SOFTWARE OR SERVICES PROVIDED HEREUNDER.
 */

package gov.nist.secauto.oscal.tools.cli.core.operations;

import net.sf.saxon.jaxp.SaxonTransformerFactory;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.URIResolver;
import javax.xml.transform.sax.SAXResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

public class XMLOperations {
  private static final Logger log = LogManager.getLogger(XMLOperations.class);
  public static Source getStreamSource(URL url) throws IOException {
    return new StreamSource(url.openStream(), url.toString());
  }

  public static List<ValidationFinding> validate(File file, List<? extends Source> schemaSources)
      throws SAXException, IOException {
    SchemaFactory schemafactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
    // schemafactory.setResourceResolver(new ClasspathResourceResolver());
    Schema schema;
    if (!schemaSources.isEmpty()) {
      schema = schemafactory.newSchema(schemaSources.toArray(new Source[schemaSources.size()]));
    } else {
      schema = schemafactory.newSchema();
    }

    Validator validator = schema.newValidator();
    XmlValidationErrorHandler errorHandler = new XmlValidationErrorHandler();
    validator.setErrorHandler(errorHandler);
    Source xmlFile = new StreamSource(file);
    validator.validate(xmlFile);

    return errorHandler.getFindings();
  }

  public static void renderCatalogHTML(File input, File result) throws IOException, TransformerException {
    render(input, result,
        XMLOperations.getStreamSource(XMLOperations.class.getResource("/xsl/oscal-for-bootstrap-html.xsl")));
  }

  public static void renderProfileHTML(File input, File result) throws IOException, TransformerException {
    SaxonTransformerFactory transfomerFactory = (SaxonTransformerFactory) net.sf.saxon.TransformerFactoryImpl.newInstance();
//    Templates resolver = transfomerFactory.newTemplates();
//    Templates renderer = transfomerFactory.newTemplates();

    File temp = File.createTempFile("resolved-profile", ".xml");
    
    try {
      Transformer transformer = transfomerFactory.newTransformer(XMLOperations.getStreamSource(XMLOperations.class.getResource("/xsl/profile-resolver.xsl")));
      transformer.transform(new StreamSource(input), new StreamResult(temp));

      transformer = transfomerFactory.newTransformer(XMLOperations.getStreamSource(XMLOperations.class.getResource("/xsl/oscal-for-bootstrap-html.xsl")));
      transformer.transform(new StreamSource(temp), new StreamResult(result));
    } finally {
      temp.delete();
    }
    
//    TransformerHandler resolverHandler = transfomerFactory.newTransformerHandler(resolver);
//    TransformerHandler rendererHandler = transfomerFactory.newTransformerHandler(renderer);
//
//    resolverHandler.setResult(new SAXResult(rendererHandler));
//    rendererHandler.setResult(new StreamResult(result));
//
//    Transformer t = transfomerFactory.newTransformer();
//    File sourceFile = input.getAbsoluteFile();
//    StreamSource source = new StreamSource();
//    String sourceSystemId = sourceFile.toURI().toASCIIString();
//    log.info("Source: "+sourceSystemId);
//    source.setSystemId(sourceSystemId);
//    t.setURIResolver(new LoggingURIResolver(t.getURIResolver()));
//    resolver.setParameter("document-uri", sourceSystemId);
//    t.transform(source, new SAXResult(resolverHandler));
  }

  public static void render(File input, File result, Source transform) throws IOException, TransformerException {
    TransformerFactory transfomerFactory = net.sf.saxon.TransformerFactoryImpl.newInstance();
    Transformer transformer = transfomerFactory.newTransformer(transform);
    transformer.transform(new StreamSource(input), new StreamResult(result));
  }

  public static void convertXMLToJSON(File input, File result) throws IOException, TransformerException {
    TransformerFactory transfomerFactory = net.sf.saxon.TransformerFactoryImpl.newInstance();
    Transformer transformer = transfomerFactory
        .newTransformer(XMLOperations.getStreamSource(XMLOperations.class.getResource("/xsl/oscal-json-write.xsl")));
    transformer.transform(new StreamSource(input), new StreamResult(result));
  }

  private static class LoggingURIResolver implements URIResolver {
    private final URIResolver delegate;

    
    public LoggingURIResolver(URIResolver delegate) {
      super();
      this.delegate = delegate;
    }


    @Override
    public Source resolve(String href, String base) throws TransformerException {
      log.info("Resolve: base='{}', href='{}'", base, href);
      return delegate.resolve(href,  base);
    }
    
  }
}
