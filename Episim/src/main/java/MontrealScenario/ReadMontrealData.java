package MontrealScenario;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.matsim.episim.model.VaccinationType;
import org.matsim.episim.model.VirusStrain;

public class ReadMontrealData {
	
	private Map<LocalDate,Integer> vaccineCount = new HashMap<>();	
	private Map<LocalDate,Integer> reVaccineCount = new HashMap<>();
	private Map<LocalDate,Map<Integer,Double>> ageCompliance = new HashMap<>();
	private Map<LocalDate,Map<VaccinationType,Double>> vaccineType = new HashMap<>();
	private Map<LocalDate,Integer> infection = new HashMap<>();
	private double scale = 1;
public ReadMontrealData (String vaccineFileLoc,String vaccineAgeFileLoc,String vaccineTypeFileLoc,String infectionFileLoc, double scale) {
	this.scale = scale;
	Reader in;

	try {
		in = new FileReader(vaccineFileLoc);
		Iterable<CSVRecord> records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss");
		for (CSVRecord record : records) {
			LocalDate date = LocalDate.parse(record.get("Date de vaccination"),formatter);
			int dose1 = Integer.parseInt(record.get("Dose 1"));
			int dose2 = Integer.parseInt(record.get("Dose 2"));
			int dose3 = Integer.parseInt(record.get("Dose 3"));
			int dose4 = Integer.parseInt(record.get("Dose 4"));
			vaccineCount.put(date, dose1);
			reVaccineCount.put(date, dose2+dose3+dose4);
		}
		in = new FileReader(vaccineAgeFileLoc);
		records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
		for (CSVRecord record : records) {
			LocalDate date = LocalDate.parse(record.get("Date de vaccination"),formatter);
			record.getParser().getHeaderNames().forEach(h->{
				if(h.equals("Date de vaccination")||h.equals("Ensemble du Québec")||h.equals("12 ans et plus")) return;
				double complianceProp = Double.parseDouble(record.get(h));
				int fromAge = Integer.parseInt(h.replace(" ans","").split("-")[0]);
				int toAge = Integer.parseInt(h.replace(" ans","").split("-")[1]);
				ageCompliance.put(date, new HashMap<>());
				for(int i = fromAge;i<=toAge;i++)ageCompliance.get(date).put(i, complianceProp/100);
			});
		}
		in = new FileReader(vaccineTypeFileLoc);
		records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
		formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		for (CSVRecord record : records) {
			if(!record.get("prename").equals("Quebec"))continue;
			LocalDate date = LocalDate.parse(record.get("report_date"),formatter);
			int total = Integer.parseInt(record.get("numtotal_all_distributed"));
			int phizer = Integer.parseInt(record.get("numtotal_pfizerbiontech_distributed"));
			int phizer5_11 = Integer.parseInt(record.get("numtotal_pfizerbiontech_5_11_distributed"));
			int moderna = Integer.parseInt(record.get("numtotal_moderna_distributed"));
			int oxford = Integer.parseInt(record.get("numtotal_astrazeneca_distributed"));
			int janssen = Integer.parseInt(record.get("numtotal_janssen_distributed"));
			int novavax = Integer.parseInt(record.get("numtotal_novavax_distributed"));
			int medicago = Integer.parseInt(record.get("numtotal_medicago_distributed"));
			
			int mRNA = phizer+phizer5_11+moderna;
			int vector = janssen+oxford;
			int generic = novavax+medicago;
			
			vaccineType.put(date,new HashMap<>());
			vaccineType.get(date).put(VaccinationType.mRNA, ((double)mRNA)/total);
			vaccineType.get(date).put(VaccinationType.vector, ((double)vector)/total);
			vaccineType.get(date).put(VaccinationType.generic, ((double)generic)/total);
			
		}
		in = new FileReader(infectionFileLoc);
		records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
		formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss");
		int oldInfection = 0;
		for (CSVRecord record : records) {
			
			LocalDate date = LocalDate.parse(record.get("Date de déclaration"),formatter);
			int infection = Integer.parseInt(record.get("Cas confirmés"));
			int newInfection = infection-oldInfection;
			oldInfection = infection;
			this.infection.put(date, newInfection);
		}
		
	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
		
}




public Map<LocalDate, Integer> getVaccineCount() {
	Map<LocalDate,Integer> vaccineCount = new HashMap<>(this.vaccineCount);
	vaccineCount.entrySet().forEach(e->e.setValue((int)Math.floor(e.getValue()*this.scale)));
	return vaccineCount;
}




public Map<LocalDate, Integer> getReVaccineCount() {
	Map<LocalDate,Integer> reVaccineCount = new HashMap<>(this.reVaccineCount);
	reVaccineCount.entrySet().forEach(e->e.setValue((int)Math.floor(e.getValue()*this.scale)));
	return reVaccineCount;
}




public Map<Integer, Double> getAgeCompliance(LocalDate startDate) {
	LocalDate current = null;
	if(this.ageCompliance.containsKey(startDate))return this.ageCompliance.get(startDate);
	for(Entry<LocalDate, Map<Integer, Double>> e:this.ageCompliance.entrySet()){
		if(current==null &&
				e.getKey().isBefore(startDate)) {
			current = e.getKey();
		}else if(e.getKey().isBefore(startDate) && current.isBefore(e.getKey())) {
				current = e.getKey();
		}
		
	}
	return this.ageCompliance.get(current);
}




public Map<LocalDate, Map<VaccinationType, Double>> getVaccineType() {
	return vaccineType;
}




public Map<LocalDate, Integer> getInfection() {
	Map<VirusStrain,Map<LocalDate,Integer>> out = new HashMap<>();
	
	
	infection.entrySet().forEach(e->e.setValue((int)Math.floor(e.getValue()*scale)));

	return infection;
}

public Map<VirusStrain,Map<LocalDate,Integer>> getInfection(VirusStrain vs) {
	Map<VirusStrain,Map<LocalDate,Integer>> out = new HashMap<>();
	Map<LocalDate,Integer> infection = new HashMap<>(this.infection);
	
	infection.entrySet().forEach(e->e.setValue((int)Math.floor(e.getValue()*scale)));
	out.put(vs, infection);
	return out;
}

public static void main(String[] args) {
	String vaccineFileLoc = "MontrealData/vaccineData/VaccineMontreal.csv";// vaccination per day in Montreal
	String vaccineAgeFileLoc = "MontrealData/vaccineData/QuebecVaccinationRateFirstDose.csv";//Distribution of age for vaccinated and non vaccinated people
	String vaccineTypeFileLoc = "MontrealData/vaccineData/vaccination-distribution.csv";//Distribution of vaccine in Quebec province over days
	String infectionFileLoc = "MontrealData/vaccineData/MontrealConfirmedCases.csv";//Infection per day in Montreal
	ReadMontrealData mtlData = new ReadMontrealData(vaccineFileLoc,vaccineAgeFileLoc,vaccineTypeFileLoc,infectionFileLoc,.05);
	Map<LocalDate,Integer> infection = mtlData.getInfection();
	Map<LocalDate, Integer> vaccineCap = mtlData.getVaccineCount();
	Map<LocalDate, Integer> reVaccineCap = mtlData.getReVaccineCount();
	Map<Integer, Double> ageCompliance = mtlData.getAgeCompliance(LocalDate.of(2022, 02, 01));
	System.out.println("Done!!!");
}
}
