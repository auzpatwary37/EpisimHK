package HKScenario;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ReadAndChangeEventFile{
	
	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException, XPathExpressionException, TransformerFactoryConfigurationError, TransformerException {
		String inputFile = "output\\scenario\\output_events-0.1.xml";
		String outputFile = "output\\scenario\\output_events-0.1_cleaned.xml";
		Map<String,String> actRepl = new HashMap<>();
		try {
			BufferedReader fr = new BufferedReader(new FileReader(new File("output/scenario/acts.csv")));
			String line = null;
			
			while((line = fr.readLine())!=null) {
				String[] part = line.split(",");
				actRepl.put(part[0], part[1]);
			}
			fr.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// 1- Build the doc from the XML file
		Document doc = DocumentBuilderFactory.newInstance()
		            .newDocumentBuilder().parse(new InputSource(inputFile));

		// 2- Locate the node(s) with xpath
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList nodes = (NodeList)xpath.evaluate("//*[@actType]",
		                                          doc, XPathConstants.NODESET);
		System.out.println(nodes.getLength());
		// 3- Make the change on the selected nodes
		for (int idx = 0; idx < nodes.getLength(); idx++) {
		    Node value = nodes.item(idx).getAttributes().getNamedItem("actType");
		    String val = value.getNodeValue();
		    value.setNodeValue(actRepl.get(val));
		}

		// 4- Save the result to a new XML doc
		Transformer xformer = TransformerFactory.newInstance().newTransformer();
		xformer.transform(new DOMSource(doc), new StreamResult(new File(outputFile)));
		
	}
	

}
