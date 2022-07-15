package utils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

public class VaccinDataPreperation {
public static void main(String[] args) throws IOException {
	String fileName = "vaccinData/owid-covid-data.csv";
	String location = "Hong Kong";//"Canada"
	
	Reader in = new FileReader("path/to/file.csv");
	Iterable<CSVRecord> records = CSVFormat.RFC4180.withFirstRecordAsHeader().parse(in);
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-mm-dd");
	for (CSVRecord record : records) {
	    LocalDate date = LocalDate.parse(record.get("date"), formatter);
	    Double newCases = Double.parseDouble(record.get("new_cases"));
	    Double totalCases = Double.parseDouble(record.get("total_cases"));
	    Double newDeath = Double.parseDouble(record.get("new_death"));
	    Double totalDeath = Double.parseDouble(record.get("total_death"));
	    Double icuPatient = Double.parseDouble(record.get("icu_patients"));
	    Double hospPatient = Double.parseDouble(record.get("hosp_patients"));
	    Double newTest = Double.parseDouble(record.get("new_tests"));
	    Double totalest = Double.parseDouble(record.get("total_tests"));
	    Double peopleVaccinated = Double.parseDouble(record.get("people_vaccinated"));
	    Double newVaccinated = Double.parseDouble(record.get("new_vaccinations"));
	    Double boosterVaccinated = peopleVaccinated-newVaccinated;
	    
	}
	
	
}


}
