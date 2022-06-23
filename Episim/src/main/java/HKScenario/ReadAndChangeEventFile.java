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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
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

		Double proportion = 0.1;

		String inputFile = "HKData/output_events.xml.gz";//"output\\scenario\\output_events-0.1.xml";
		String outEventFileIntermediate = "HKData/output/eventsHongKong_" + proportion;
		String outputFile = outEventFileIntermediate + "/events_cleaned.xml";
		String populationFileLoc = "HKData/output_plans.xml.gz";
		String populationFileLoc1 = "HKData/output_plans_noGV_age.xml.gz";
		String outputPopulationFileLocation = outEventFileIntermediate + "/output_plans_cleaned.xml.gz";
		String googleMobilityDataRecord = "HKData/output/HKMobilityReport.csv";
		String personAgeFile = "HKData/memberAge.csv";


		//________________________Delete goods vehicle and insert age____________________________________________
		Map<String, Double> ageOfPersons = new HashMap<>();

		try {
			BufferedReader bf1 = new BufferedReader(new FileReader(new File(personAgeFile)));

			String line = null;

			while ((line = bf1.readLine()) != null) {
				String[] part = line.split(",");
				ageOfPersons.put(part[0], Double.parseDouble(part[1]));
			}
			bf1.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		{
			Population pp = PopulationUtils.readPopulation(populationFileLoc);
			Map<Id<Person>, ? extends Person> personMap = pp.getPersons();

			personMap.entrySet().forEach(e -> {
				if (PopulationUtils.getSubpopulation(e.getValue()).contains("GV")) {
					pp.removePerson(e.getKey());
					return;
				} else {
					String mId = e.getKey().toString().split("_")[0] + "_" + e.getKey().toString().split("_")[1];
					if (ageOfPersons.get(mId) != null) {
						e.getValue().getAttributes().putAttribute("age", ageOfPersons.get(mId));
					} else {
						throw new IllegalArgumentException("No Age found for person Id = " + e.getKey().toString());
					}
				}

			});

			new PopulationWriter(pp).write(populationFileLoc1);
		}

		//____________cerate episim scenario(event file)______________________
		String[] args1 = new String[]{
				//DownSampleScenario.class.getName(),
				Double.toString(proportion),
				"--population", populationFileLoc1,
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
		Document doc = DocumentBuilderFactory.newInstance()
		            .newDocumentBuilder().parse(new InputSource(IOUtils.getBufferedReader(outEventFileIntermediate+"/output_events-"+proportion+".xml.gz")));

		// 2- Locate the node(s) with xpath
		XPath xpath = XPathFactory.newInstance().newXPath();
		NodeList nodes = (NodeList)xpath.evaluate("//*[@actType]",
		                                          doc, XPathConstants.NODESET);
		System.out.println(nodes.getLength());
		// 3- Make the change on the selected nodes
		for (int idx = 0; idx < nodes.getLength(); idx++) {
		    Node value = nodes.item(idx).getAttributes().getNamedItem("actType");
		    String val = value.getNodeValue();
		    if(actRepl.containsKey(val)) {
		    	value.setNodeValue(actRepl.get(val));
		    }else {
		    	System.out.println("No mapping found for activity "+ val);
		    }
		}

		// 4- Save the result to a new XML doc
		Transformer xformer = TransformerFactory.newInstance().newTransformer();
		xformer.transform(new DOMSource(doc), new StreamResult(new File(outputFile)));
		

		Population pop = PopulationUtils.readPopulation(outEventFileIntermediate+"/population"+proportion+".xml.gz");
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
		
		//____________________________google Mobility data download _______________________________________________
		args1 = new String[]{
				"--region", "HK",
				"--sub-region","",
				"--from","2020-02-01",
				"--output", googleMobilityDataRecord
		};
		DownloadGoogleMobilityReport.main(args1);
	}
	

}
