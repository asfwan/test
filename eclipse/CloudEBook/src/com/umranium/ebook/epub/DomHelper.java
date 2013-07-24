package com.umranium.ebook.epub;

import static com.umranium.ebookextra.Constants.TAG;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.umranium.ebookextra.Constants;

import android.annotation.SuppressLint;
import android.util.Log;
import android.util.Pair;

@SuppressLint("DefaultLocale")
public class DomHelper {

    public interface NodeSelector {
        boolean checkNodeChildren(LinkedList<Node> hierarchy, Node node);

        boolean isNodeSelected(LinkedList<Node> hierarchy, Node node);
    }

    private static void findNodes(Node parentNode, LinkedList<Node> hierarchy, NodeSelector nodeSelector, int maxCount, List<Node> results) {
        try {
            hierarchy.addLast(parentNode);

            NodeList childNodeList = parentNode.getChildNodes();
            for (int i = 0; i < childNodeList.getLength(); ++i) {
                Node childNode = childNodeList.item(i);
                if (nodeSelector.isNodeSelected(hierarchy, childNode)) {
                    results.add(childNode);
                }
                if (childNode.hasChildNodes() && nodeSelector.checkNodeChildren(hierarchy, childNode)) {
                    findNodes(parentNode, hierarchy, nodeSelector, maxCount, results);
                }
                if (results.size() > maxCount) {
                    return;
                }
            }
        } finally {
            hierarchy.removeLast();
        }
    }

    public static List<Node> findNodes(Node parentNode, NodeSelector nodeSelector, int maxCount) {
        List<Node> results = new ArrayList<Node>();
        LinkedList<Node> hierarchy = new LinkedList<Node>();

        {
            Node parent = parentNode.getParentNode();
            while (parent != null) {
                hierarchy.addFirst(parent);
                parent = parent.getParentNode();
            }
        }

        if (nodeSelector.isNodeSelected(hierarchy, parentNode)) {
            results.add(parentNode);
        }

        if (results.size() < maxCount && parentNode.hasChildNodes() && nodeSelector.checkNodeChildren(hierarchy, parentNode)) {
            findNodes(parentNode, hierarchy, nodeSelector, maxCount, results);
        }

        return results;
    }

    public static Node findFirst(Node parentNode, NodeSelector nodeSelector) {
        List<Node> nodes = findNodes(parentNode, nodeSelector, 1);
        if (nodes.isEmpty())
            return null;
        else
            return nodes.get(0);
    }


    public static class NodeTagSelector implements NodeSelector {
        private String tagName;

        public NodeTagSelector(String tagName) {
            this.tagName = tagName.toLowerCase();
        }

        @Override
        public boolean checkNodeChildren(LinkedList<Node> hierarchy, Node node) {
            return node.getNodeType() == Node.ELEMENT_NODE;
        }

        @Override
        public boolean isNodeSelected(LinkedList<Node> hierarchy, Node node) {
            if (node.getNodeType() != Node.ELEMENT_NODE) {
                return false;
            }
            Element el = (Element) node;
            return el.getTagName().toLowerCase().equals(tagName);
        }

    }

    public static class Printer {
        boolean printText;
        int tabs = 0;

        public Printer(boolean printText) {
            this.printText = printText;
        }

        private String printTabs() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < tabs; ++i) {
                sb.append("   ");
            }
            return sb.toString();
        }

        public void print(Element el) {
            ++tabs;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(printTabs());
            stringBuilder.append("<");
            stringBuilder.append(el.getTagName());
            for (int i = 0; i < el.getAttributes().getLength(); ++i) {
                Attr attr = (Attr) el.getAttributes().item(i);
                stringBuilder.append(" ");
                stringBuilder.append(attr.getName());
                stringBuilder.append("=\"");
                stringBuilder.append(attr.getValue());
                stringBuilder.append("\"");
            }
            stringBuilder.append(">");
            Log.d(TAG, stringBuilder.toString());
            NodeList nodeList = el.getChildNodes();
            for (int i = 0; i < nodeList.getLength(); ++i) {
                Node node = nodeList.item(i);
                switch (node.getNodeType()) {
                    case Node.ELEMENT_NODE: {
                        Element childEl = (Element) node;
                        print(childEl);
                        break;
                    }
                    case Node.TEXT_NODE: {
                        ++tabs;
                        Log.d(TAG, printTabs() + summarize(node.getTextContent(), 100));
                        --tabs;
                    }
                }
            }
            Log.d(TAG, printTabs() + "</" + el.getTagName() + ">");
            --tabs;
        }
    }

//	public static boolean isBlockTag(String tag) {
//		tag = tag.toLowerCase();
//		return tag.equals("div")
//				|| tag.equals("p")
//				|| tag.equals("tr")
//				|| tag.equals("td")
//				|| tag.equals("th")
//				|| tag.equals("hr")
//				;
//	}

    public static String readerToString(Reader reader, int initialSize) throws IOException {
        StringBuilder bldr = new StringBuilder(initialSize);
        try {
            int ch;
            while ((ch = reader.read()) >= 0) {
                bldr.append((char) ch);
            }
        } finally {
            reader.close();
        }
        return bldr.toString();
    }

    public static String readerToString(Reader reader) throws IOException {
        return readerToString(reader, Constants.DEF_READ_BUFF_SIZE);
    }

    public static String streamToString(InputStream is, Charset charset) throws IOException {
        try {
            return readerToString(new BufferedReader(
                    new InputStreamReader(is, charset),
                    Constants.DEF_READ_BUFF_SIZE),
                    Constants.DEF_READ_BUFF_SIZE);
        } finally {
            is.close();
        }
    }

    public static byte[] streamToByteArr(InputStream is) throws IOException {
        try {
            int initSize = Constants.DEF_READ_BUFF_SIZE;
            if (is.available() > initSize) {
                initSize = is.available();
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream(initSize);
            BufferedInputStream bis = new BufferedInputStream(is,
                    Constants.DEF_READ_BUFF_SIZE);
            int v;
            while ((v = bis.read()) != -1) {
                baos.write(v);
            }
            return baos.toByteArray();
        } finally {
            is.close();
        }
    }

    public static Element createElement(Document dom, String tag, List<Pair<String, String>> attributes) {
        Element el = dom.createElement(tag);
        if (attributes != null) {
            for (Pair<String, String> attrib : attributes) {
                el.setAttribute(attrib.first, attrib.second);
            }
        }
        el.appendChild(dom.createTextNode(" "));
        return el;
    }

    public static List<Pair<String, String>> mapToPairList(Map<String, String> map) {
        List<Pair<String, String>> list = new ArrayList<Pair<String, String>>(map.size());
        for (Map.Entry<String, String> e : map.entrySet()) {
            list.add(new Pair<String, String>(e.getKey(), e.getValue()));
        }
        return list;
    }

    private static final String ELLIPSIS = "...";

    public static String summarize(String s, int maxChars) {
        int lenElipsis = ELLIPSIS.length();
        if (s.length() > maxChars) {
            int startLen = (maxChars - lenElipsis) / 2;
            int endLen = maxChars - startLen - lenElipsis;
            return s.substring(0, startLen) + ELLIPSIS + s.substring(s.length() - endLen, s.length());
        } else {
            return s;
        }
    }

    public static String removeAnyAnchor(String url) {
        if (url.contains("#")) {
            url = url.substring(0, url.indexOf('#'));
        }
        return url;
    }

}
