package MontrealScenario;

import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;
import org.matsim.scenarioCreation.DownSampleScenario;
import org.matsim.scenarioCreation.DownloadGoogleMobilityReport;
import org.matsim.scenarioCreation.DownloadWeatherData;

public class ScenarioGeneration {
public static void main(String[] args) {
	String popFile = "MontrealData/output_plans.xml.gz";
	String eventFile = "MontrealData/output_events.xml.gz";
	String facilityFile = "MontrealData/output_facilities.xml.gz";
	String outputFileLoc = "MontrealData/Montreal0.1";
	String googleDataLoc = "MontrealData/googleData.csv";
	String weatherDataLoc = "MontrealData/weatherData.csv";
	
	double scale = 1.0;
	
//	Set<String> acts = new HashSet<>();
//	Population pop = PopulationUtils.readPopulation(popFile);
//	pop.getPersons().entrySet().stream().forEach(p->{
//		p.getValue().getSelectedPlan().getPlanElements().forEach(pl->{
//			if(pl instanceof Activity) {
//				Activity a = (Activity)pl;
//				acts.add(a.getType());
//				//System.out.println(a.getType());
//			}
//		});
//	});
//	
//	acts.remove("pt interaction");
//	acts.forEach(a->System.out.println(a));
//	
	
	String[] args1 = new String[]{
			//DownSampleScenario.class.getName(),
			Double.toString(scale),
			"--population", popFile,
			"--events", eventFile,
			"--facilities",facilityFile,
			"--output", outputFileLoc
	};
	DownSampleScenario.main(args1);
	
	args1 = new String[]{
			"--region", "CA",
			"--sub-region","CA-QC",
			"--from","2020-02-01",
			"--output", googleDataLoc
	};
	DownloadGoogleMobilityReport.main(args1);
	
	args1 = new String[]{ "SOK6B",
			"--from","2020-02-01",
			"--output", weatherDataLoc
	};
	
	DownloadWeatherData.main(args1);
	
}
}
