package HKScenario;

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

public class ReadVaccineData {
	private Map<LocalDate,VaccineData> data = new HashMap<>();
	private Map<Integer,Double>ageComp = new HashMap<>();
	private Map<LocalDate,Integer> newCases = new HashMap<>();
	private double scale  = 1.0;
	public  ReadVaccineData(String vaccinefileLoc, String ageFileLoc, String openWorldDataLoc, double scale){
		Reader in;
		this.scale = scale;
		try {
			in = new FileReader(vaccinefileLoc);
			Iterable<CSVRecord> records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			for (CSVRecord record : records) {
				LocalDate date= LocalDate.parse(record.get("Date"), formatter);
				VaccineData vd;
				if(data.containsKey(date)) {
					vd = data.get(date);
				}else {
					vd = new VaccineData();
					data.put(date, vd);
				}
				String ageString = record.get("Age Group").replace(" and above", "-130");
				int age = Integer.parseInt(ageString.split("-")[0]);
				int sinoVac = Integer.parseInt(record.get("Sinovac 1st dose"));
				int mRNA = Integer.parseInt(record.get("BioNTech 1st dose"));
				int sinoVacBooster = Integer.parseInt(record.get("Sinovac 2nd dose"))+Integer.parseInt(record.get("Sinovac 3rd dose"));
				int mRNABooster = Integer.parseInt(record.get("BioNTech 2nd dose"))+Integer.parseInt(record.get("BioNTech 3rd dose"));
				int totalVaccine = sinoVac+mRNA;
				int totalBooster = sinoVacBooster+mRNABooster;
				vd.date = date;
				vd.totalVaccine = vd.totalVaccine+totalVaccine;
				vd.totalBooster = vd.totalBooster+totalBooster;
				
				vd.ageVaccineRatio.put(age, Double.valueOf(totalVaccine));
				vd.ageboosterVaccineRatio.put(age, Double.valueOf(totalVaccine));
				for(Entry<Integer, Double> d:vd.ageVaccineRatio.entrySet())d.setValue(d.getValue()/vd.totalVaccine);
				for(Entry<Integer, Double> d:vd.ageboosterVaccineRatio.entrySet())d.setValue(d.getValue()/vd.totalBooster);
				vd.vaccineNumberByType.compute(VaccinationType.vector, (k,v)->v==null?sinoVac:v+sinoVac);
				vd.vaccineNumberByType.compute(VaccinationType.mRNA, (k,v)->v==null?mRNA:v+mRNA);
				vd.boosterVaccineNumberByType.compute(VaccinationType.vector, (k,v)->v==null?sinoVacBooster:v+sinoVacBooster);
				vd.boosterVaccineNumberByType.compute(VaccinationType.mRNA, (k,v)->v==null?mRNABooster:v+mRNABooster);
			}
			in = new FileReader(ageFileLoc);
			records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
			for (CSVRecord record : records) {
				int fromAge = Integer.parseInt(record.get("Age").split("-")[0]);
				int toAge = Integer.parseInt(record.get("Age").split("-")[1]);
				for(int i=fromAge;i<=toAge;i++) {
					ageComp.put(i, Double.parseDouble(record.get("Dose1")));
				}
			}
			
			//String fileName = "vaccinData/owid-covid-data.csv";
			String location = "Hong Kong";//"Canada"
			
			in = new FileReader(openWorldDataLoc);
			records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
			formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			for (CSVRecord record : records) {
			    LocalDate date = LocalDate.parse(record.get("date"), formatter);
			    Double newCases = 0.;
			    if(!record.get("new_cases").equals(""))newCases = Double.parseDouble(record.get("new_cases"));
			    Double totalCases = 0.;
			    if(!record.get("total_cases").equals(""))totalCases = Double.parseDouble(record.get("total_cases"));
			    Double newDeath = 0.;
			    if(!record.get("new_deaths").equals(""))newDeath = Double.parseDouble(record.get("new_deaths"));
			    Double totalDeath = 0.;
			    if(!record.get("total_deaths").equals(""))totalDeath = Double.parseDouble(record.get("total_deaths"));
			    Double icuPatient = 0.;
			    if(!record.get("icu_patients").equals(""))icuPatient = Double.parseDouble(record.get("icu_patients"));
			    Double hospPatient = 0.;
			    if(!record.get("hosp_patients").equals(""))hospPatient = Double.parseDouble(record.get("hosp_patients"));
			    Double newTest = 0.;
			    if(!record.get("new_tests").equals(""))newTest = Double.parseDouble(record.get("new_tests"));
			    Double totalTest = 0.;
			    if(!record.get("total_tests").equals(""))totalTest = Double.parseDouble(record.get("total_tests"));
			    Double peopleVaccinated = 0.;
			    if(!record.get("people_vaccinated").equals(""))peopleVaccinated = Double.parseDouble(record.get("people_vaccinated"));
			    Double newVaccinated = 0.;
			    if(!record.get("new_vaccinations").equals(""))newVaccinated = Double.parseDouble(record.get("new_vaccinations"));
			    Double boosterVaccinated = peopleVaccinated-newVaccinated;
			    this.newCases.put(date,(int)Math.floor(newCases));
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	public Map<Integer,Double> createAgeCompliance(){
		
		return ageComp;
	}
	
	public Map<LocalDate,Map<VaccinationType,Double>> createVaccineShare(){
		Map<LocalDate,Map<VaccinationType,Double>>vaccinComp = new HashMap<>();
		for(VaccineData d:this.data.values()) {
			vaccinComp.put(d.date, d.vaccineNumberByType);
		
			d.boosterVaccineNumberByType.entrySet().forEach(e->{
				vaccinComp.get(d.date).put(e.getKey(), (e.getValue()+vaccinComp.get(d.date).get(e.getKey()))/(d.totalVaccine+d.totalBooster));
				
			});
		}
		
		return vaccinComp;
	}
	
	public Map<LocalDate,Integer> getVaccinationCapacity(){
		Map<LocalDate,Integer> cap = new HashMap<>();
		for(VaccineData d:this.data.values()) {
			cap.put(d.date, (int)Math.floor(d.totalVaccine*this.scale));
		}
		return cap;
	}
	public Map<LocalDate,Integer> getReVaccinationCapacity(){
		Map<LocalDate,Integer> cap = new HashMap<>();
		for(VaccineData d:this.data.values()) {
			cap.put(d.date, (int)Math.floor(d.totalBooster*this.scale));
		}
		return cap;
	}
	public Map<LocalDate,Integer> getInfections(){
		newCases.entrySet().forEach(e->e.setValue((int)Math.floor(e.getValue()*this.scale)));
		return this.newCases;
	}
	public static void main(String[] args) {
		ReadVaccineData vd = new ReadVaccineData("vaccinData/HKVac_new.csv", "vaccinData/Age_complianceHK.csv","vaccinData/owid-covid-data.csv",1.0);
		Map<LocalDate,Integer> vacCap = vd.getVaccinationCapacity();
		Map<LocalDate,Integer> reVacCap = vd.getReVaccinationCapacity();
		Map<LocalDate, Map<VaccinationType, Double>> vacShare = vd.createVaccineShare();
		Map<LocalDate,Integer> newCases = vd.getInfections();
		System.out.println("Done!!!");
	}
}
class VaccineData{
	LocalDate date;
	Map<Integer,Double> ageVaccineRatio = new HashMap<>();
	Map<VaccinationType,Double> vaccineNumberByType = new HashMap<>();
	Map<VaccinationType,Double> boosterVaccineNumberByType = new HashMap<>();
	Map<Integer,Double> ageboosterVaccineRatio = new HashMap<>();
	int totalVaccine= 0;
	int totalBooster =0;
}
