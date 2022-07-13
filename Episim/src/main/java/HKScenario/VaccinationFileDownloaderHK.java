
package HKScenario;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import picocli.CommandLine;

import java.io.BufferedWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.concurrent.Callable;

import static picocli.CommandLine.*;

/**
 * Executable class, see description.
 */
@Command(
		name = "downloadVaccinationData",
		description = "Download number of vaccinated people from rki.",
		mixinStandardHelpOptions = true
)
public class VaccinationFileDownloaderHK implements Callable<Integer> {

	private static final String URL = "https://api.data.gov.hk/v1/historical-archive/get-file?url=https%3A%2F%2Fwww.fhb.gov.hk%2Fdownload%2Fopendata%2FCOVID19%2Fvaccination-rates-over-time-by-age.csv&amp;time=20211207-1036";

	private static Logger log = LogManager.getLogger(VaccinationFileDownloaderHK.class);

	@Option(names = "--output", description = "Output file", defaultValue = "HKVaccine.csv")
	private Path output;


	public static void main(String[] args) {
		System.exit(new CommandLine(new VaccinationFileDownloaderHK()).execute(args));
	}

	@Override
	public Integer call() throws Exception {

		BufferedWriter writer = Files.newBufferedWriter(output, StandardOpenOption.CREATE);
		CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withCommentMarker('#').withHeaderComments(
				"Source: https://www.rki.de/DE/Content/InfAZ/N/Neuartiges_Coronavirus/Daten/Impfquoten-Tab.html"
		).withHeader(
				"Date",	"Age Group",	"sex",	"Sinovac 1st dose",	"Sinovac 2nd dose",	"Sinovac 3rd dose",	
				"BioNTech 1st dose",	"BioNTech 2nd dose",	"BioNTech 3rd dose"
				)
		);

		try (var in = new URL(URL).openStream()) {

			Workbook wb = WorkbookFactory.create(in);

			Sheet sheet = wb.getSheetAt(3);

			for (Iterator<Row> it = sheet.rowIterator(); it.hasNext(); ) {

				Row row = it.next();

				try {
					LocalDateTime date = row.getCell(0).getLocalDateTimeCellValue();
					String ageGroup = row.getCell(1).getStringCellValue();
					String sex = row.getCell(2).getStringCellValue();
					double sn1 = row.getCell(3).getNumericCellValue();
					double sn2 = row.getCell(4).getNumericCellValue();
					double sn3 = row.getCell(5).getNumericCellValue();
					double mrna1 = row.getCell(6).getNumericCellValue();
					double mrna2 = row.getCell(7).getNumericCellValue();
					double mrna3 = row.getCell(8).getNumericCellValue();
					
					printer.printRecord(date.toLocalDate().format(DateTimeFormatter.ISO_DATE), ageGroup,sex, sn1,sn2,sn3,mrna1,mrna2,mrna3);
				} catch (Exception e) {
					// ignore
				}
			}

		}

		writer.close();

		return 0;
	}

}
