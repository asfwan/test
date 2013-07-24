package com.umranium.ebook.epub;

import static com.umranium.ebookextra.Constants.TAG;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import android.util.Log;

import com.umranium.ebook.epub.DomHelper.NodeSelector;
import com.umranium.ebook.sharedres.SharedResources;


public class EpubDomPreprocessor {

    private static final Locale HTML_LOCALE = Locale.US;

    private static final NodeSelector SELECT_HEAD = new DomHelper.NodeTagSelector("head") {

        @Override
        public boolean checkNodeChildren(LinkedList<Node> hierarchy, Node node) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                return false;
            }
            Element el = (Element) node;
            return el.getTagName().toLowerCase(HTML_LOCALE).equals("html");
        }

    };

    private static final NodeSelector SELECT_BODY = new DomHelper.NodeTagSelector("body") {

        @Override
        public boolean checkNodeChildren(LinkedList<Node> hierarchy, Node node) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                return false;
            }
            Element el = (Element) node;
            return el.getTagName().toLowerCase(HTML_LOCALE).equals("html");
        }

    };

    private static final NodeSelector SELECT_META = new DomHelper.NodeTagSelector("meta") {

        @Override
        public boolean checkNodeChildren(LinkedList<Node> hierarchy, Node node) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                return false;
            }
            Element el = (Element) node;
            return el.getTagName().toLowerCase(HTML_LOCALE).equals("html") ||
                    el.getTagName().toLowerCase(HTML_LOCALE).equals("head");
        }

    };

    private SharedResources resources;

    public EpubDomPreprocessor(SharedResources resources) {
        this.resources = resources;
    }

    public String preprocess(InputSource inputSource) throws EbookException {
        // TODO: Convert this to a stream parser and processor to save on memory.

        Log.d(TAG, "Parsing input into DOM.");
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        docFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder;
        try {
            docBuilder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new EbookException("Error while preparing to parse ePub document", e);
        }
        Document doc;
        try {
            doc = docBuilder.parse(inputSource);
        } catch (SAXException e) {
            throw new EbookException("Error while parsing ePub document", e);
        } catch (IOException e) {
            throw new EbookException("Error while parsing ePub document", e);
        }

//		Log.d(TAG, "original content:");
//		DomHelper.Printer printer = new DomHelper.Printer(false);
//		printer.print(doc.getDocumentElement());

        Element head = (Element) DomHelper.findFirst(doc.getDocumentElement(), SELECT_HEAD);
        Element body = (Element) DomHelper.findFirst(doc.getDocumentElement(), SELECT_BODY);


        removeAllMetas(head);
        organizeContentStructure(doc, body);
        addHeadExtras(doc, head);
        addBodyExtras(doc, body);

//		Log.d(TAG, "final content:");
//		DomHelper.Printer fullPrint = new DomHelper.Printer(true);
//		fullPrint.print(doc.getDocumentElement());


        // Use a Transformer for output
        Log.d(TAG, "Transforming content to text");
        TransformerFactory tFactory = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = tFactory.newTransformer();
        } catch (TransformerConfigurationException e) {
            throw new EbookException("Error while preparing to write transformed ePub content", e);
        }
        Properties transformerProperties = new Properties();
        transformerProperties.setProperty("omit-xml-declaration", "yes");
        transformerProperties.setProperty("method", "html");
        transformer.setOutputProperties(transformerProperties);

        StringWriter stringWriter = new StringWriter();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(stringWriter);
        try {
            transformer.transform(source, result);
        } catch (TransformerException e) {
            throw new EbookException("Error while writing transformed ePub content", e);
        }
        stringWriter.flush();

        String html = stringWriter.toString();

//		if (html.contains("&lt;")) {
//			Log.e(TAG, "HTML Escaped!!", new Exception());
//		}

//		Log.d(TAG, "resulting HTML:"+DomHelper.summarize(html, 200));

        Log.d(TAG, "HTML ready.");

        return html;
    }

    private void removeAllMetas(Element head) {
        Log.d(TAG, "Removing all meta nodes");
        List<Node> metas = DomHelper.findNodes(head, SELECT_META, Integer.MAX_VALUE);
        for (Node node : metas) {
            Log.d(TAG, "\tremoving meta: " + node.toString());
            node.getParentNode().removeChild(node);
        }
    }

    private void organizeContentStructure(final Document doc, final Element body) {
        Log.d(TAG, "Organizing content structure");

        Map<String, String> attrs = new HashMap<String, String>();

        attrs.clear();
        NamedNodeMap bodyAttrs = body.getAttributes();
        for (int i = 0; i < bodyAttrs.getLength(); ++i) {
            Node attr = bodyAttrs.item(i);
            String name = attr.getNodeName();
            String value = attr.getNodeValue();

            name = name.toLowerCase(HTML_LOCALE);
            if (!name.equals("id")) {
                attrs.put(name, value);
            }

            bodyAttrs.removeNamedItem(name);
        }

        String prevClass = "";
        if (attrs.containsKey("class")) {
            prevClass = attrs.get("class");
        }
        prevClass += " ebook-container";
        attrs.put("class", prevClass);
        attrs.put("id", "root_ebook_container");

        Element rootContainer = DomHelper.createElement(doc, "div", DomHelper.mapToPairList(attrs));
        while (body.hasChildNodes()) {
            rootContainer.appendChild(body.getFirstChild());
        }


//		attrs.clear();
//		attrs.put("class", "ebook-side-panel");
//		attrs.put("id", "ebook_side_panel");
//		Element notePanel = DomHelper.createElement(doc, "div", DomHelper.mapToPairList(attrs));

        attrs.clear();
        attrs.put("class", "ebook-inner-container");
        Element innerContainer = DomHelper.createElement(doc, "div", DomHelper.mapToPairList(attrs));
//		innerContainer.appendChild(notePanel);
        innerContainer.appendChild(doc.createTextNode("\n\t"));
        innerContainer.appendChild(rootContainer);

        attrs.clear();
        attrs.put("class", "ebook-outer-container");
        attrs.put("style", "display:none");
        Element outerContainer = DomHelper.createElement(doc, "div", DomHelper.mapToPairList(attrs));
        outerContainer.appendChild(innerContainer);

        body.appendChild(outerContainer);
    }

    private void addHeadExtras(Document doc, Element head) {
        Map<String, String> attrs = new HashMap<String, String>();

        Log.d(TAG, "Adding JQuery");
        Element jquery = DomHelper.createElement(doc, "script", null);
        jquery.setTextContent("\n" + resources.getJquery() + "\n");

        Log.d(TAG, "Adding JQuery No-Conflict Script");
        Element jqueryNoConflict = DomHelper.createElement(doc, "script", null);
        jqueryNoConflict.setTextContent("\nvar $jquery = jQuery.noConflict();\n");

        Log.d(TAG, "Adding document ready script");
        Element displayWhenReady = DomHelper.createElement(doc, "script", null);
        displayWhenReady.setTextContent("\n" +
                "\t$jquery(document).ready(function(){\n" +
                "\t\t$jquery(\".ebook-outer-container\").css(\"display\", \"block\");\n" +
                "\t});\n"
        );

        Log.d(TAG, "Adding android.selection script");
        Element androidSelection = DomHelper.createElement(doc, "script", null);
        androidSelection.setTextContent("\n" + resources.getAndroidSelection() + "\n");

        Log.d(TAG, "Adding dom.interop script");
        Element domInterop = DomHelper.createElement(doc, "script", null);
        domInterop.setTextContent("\n" + resources.getDomInterop() + "\n");

        Log.d(TAG, "Adding system styles css");
        attrs.clear();
        attrs.put("media", "screen");
        attrs.put("type", "text/css");
        Element systemStyles = DomHelper.createElement(doc, "style", DomHelper.mapToPairList(attrs));
        systemStyles.setTextContent("\n" + resources.getSystemStyles() + "\n");

        head.appendChild(jquery);
        head.appendChild(jqueryNoConflict);
        head.appendChild(displayWhenReady);
        head.appendChild(androidSelection);
        head.appendChild(domInterop);
        head.appendChild(systemStyles);
    }

    private void addBodyExtras(Document doc, Element body) {
        //Map<String,String> attrs = new HashMap<String,String>();

        Log.d(TAG, "Adding ebook.dart");
        Element ebookDart = DomHelper.createElement(doc, "script", null);
        ebookDart.setTextContent("\n" + resources.getEbookDart() + "\n");

        Log.d(TAG, "Adding dart boot-strap");
        Element dart = DomHelper.createElement(doc, "script", null);
        dart.setTextContent("\n" + resources.getDart() + "\n");

        body.appendChild(doc.createTextNode("\n\t"));
        body.appendChild(ebookDart);
        body.appendChild(doc.createTextNode("\n\t"));
        body.appendChild(dart);
    }

}

