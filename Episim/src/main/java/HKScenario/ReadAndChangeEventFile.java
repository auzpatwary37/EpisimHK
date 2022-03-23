package HKScenario;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.scenarioCreation.DownSampleScenario;
import org.matsim.scenarioCreation.DownloadGoogleMobilityReport;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class ReadAndChangeEventFile{
	
	public static void main(String[] args) throws SAXException, IOException, ParserConfigurationException, XPathExpressionException, TransformerFactoryConfigurationError, TransformerException {
		String inputFile = "HKData/output_events.xml.gz";//"output\\scenario\\output_events-0.1.xml";
		String outputFile = "HKData/output/eventsCleaned_.1.xml";
		String outEventFileIntermediate = "HKData/output/eventsIntermediate_0.1";
		String populationFileLoc = "HKData/output_plans.xml.gz";
		String outputPopulationFileLocation = "HKData/output/output_plans_cleaned_0.1.xml.gz";
		String googleMobilityDataRecord = "HKData/output/HKMobilityReport.csv";
		
		String[] args1 = new String[]{
				//DownSampleScenario.class.getName(),
				Double.toString(.1),
				"--population", populationFileLoc,
				"--events", inputFile,
				"--output", outEventFileIntermediate
		};
		DownSampleScenario.main(args1);

		Map<String,String> actRepl = new HashMap<>();
		try {
			BufferedReader fr = new BufferedReader(new FileReader(new File("HKData\\acts.csv")));
			
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
//		Document doc = DocumentBuilderFactory.newInstance()
//		            .newDocumentBuilder().parse(new InputSource(IOUtils.getBufferedReader(outEventFileIntermediate+"/output_events-1.0.xml.gz")));
//
//		// 2- Locate the node(s) with xpath
//		XPath xpath = XPathFactory.newInstance().newXPath();
//		NodeList nodes = (NodeList)xpath.evaluate("//*[@actType]",
//		                                          doc, XPathConstants.NODESET);
//		System.out.println(nodes.getLength());
//		// 3- Make the change on the selected nodes
//		for (int idx = 0; idx < nodes.getLength(); idx++) {
//		    Node value = nodes.item(idx).getAttributes().getNamedItem("actType");
//		    String val = value.getNodeValue();
//		    value.setNodeValue(actRepl.get(val));
//		}
//
//		// 4- Save the result to a new XML doc
//		Transformer xformer = TransformerFactory.newInstance().newTransformer();
//		xformer.transform(new DOMSource(doc), new StreamResult(new File(outputFile)));
		

		Population pop = PopulationUtils.readPopulation(outEventFileIntermediate+"/population1.0.xml.gz");
		pop.getPersons().values().stream().forEach(p->{
			p.getPlans().forEach(pl->{
				pl.getPlanElements().forEach(pe->{
					if(pe instanceof Activity) {
						Activity a = (Activity)pe;
						a.setType(actRepl.get(a.getType()));
					}
				});
			});
		});
		new PopulationWriter(pop).write(outputPopulationFileLocation);
		
		args1 = new String[]{
				"--region", "HK",
				"--sub-region","",
				"--from","2020-02-01",
				"--output", googleMobilityDataRecord
		};
		DownloadGoogleMobilityReport.main(args1);
	}
	

}
