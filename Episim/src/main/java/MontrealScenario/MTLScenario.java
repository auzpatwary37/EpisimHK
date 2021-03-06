/*-
 * #%L
 * MATSim Episim
 * %%
 * Copyright (C) 2020 matsim-org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package MontrealScenario;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import HKScenario.ReadVaccineData;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.episim.EpisimConfigGroup;
import org.matsim.episim.TracingConfigGroup;
import org.matsim.episim.VaccinationConfigGroup;
import org.matsim.episim.VirusStrainConfigGroup;
import org.matsim.episim.TracingConfigGroup.CapacityType;
import org.matsim.episim.VirusStrainConfigGroup.StrainParams;
import org.matsim.episim.model.AgeDependentInfectionModelWithSeasonality;
import org.matsim.episim.model.ContactModel;
import org.matsim.episim.model.DefaultFaceMaskModel;
import org.matsim.episim.model.DefaultInfectionModel;
import org.matsim.episim.model.FaceMask;
import org.matsim.episim.model.FaceMaskModel;
import org.matsim.episim.model.InfectionModel;
import org.matsim.episim.model.SymmetricContactModel;
import org.matsim.episim.model.Transition;
import org.matsim.episim.model.VaccinationType;
import org.matsim.episim.model.VirusStrain;
import org.matsim.episim.model.progression.AgeDependentDiseaseStatusTransitionModel;
import org.matsim.episim.model.progression.DefaultDiseaseStatusTransitionModel;
import org.matsim.episim.model.progression.DiseaseStatusTransitionModel;
import org.matsim.episim.model.vaccination.RandomVaccination;
import org.matsim.episim.model.vaccination.VaccinationByAge;
import org.matsim.episim.model.vaccination.VaccinationModel;
import org.matsim.episim.policy.AdaptivePolicy.ConfigBuilder;
import org.matsim.episim.policy.FixedPolicy;
import org.matsim.episim.policy.Restriction;
import org.matsim.run.RunEpisim;
import org.matsim.run.modules.AbstractSnzScenario2020;
import org.matsim.scenarioCreation.DownloadGoogleMobilityReport;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Scenario based on the publicly available OpenBerlin scenario (https://github.com/matsim-scenarios/matsim-berlin).
 */
public class MTLScenario extends AbstractModule {
	boolean tracing = true;

	/**
	 * Activity names of the default params from {@link #addDefaultParams(EpisimConfigGroup)}.
	 */
	public String[] DEFAULT_ACTIVITIES = {
			"education",
			"work",
			"home",
			"leisure",
			"shop",
			"other"
	};

	/**
	 * Adds default parameters that should be valid for most scenarios.
	 */
	public static void addDefaultParams(EpisimConfigGroup config, Set<String> acts) {
		// pt
		config.getOrAddContainerParams("pt", "tr");
		
		
		acts.stream().forEach(a->config.getOrAddContainerParams(a));
		config.getOrAddContainerParams("quarantine_home");
	}
	@Override
	protected void configure() {
		bind(ContactModel.class).to(SymmetricContactModel.class).in(Singleton.class);
		bind(DiseaseStatusTransitionModel.class).to(DefaultDiseaseStatusTransitionModel.class).in(Singleton.class);
		bind(InfectionModel.class).to(DefaultInfectionModel.class).in(Singleton.class);
		bind(VaccinationModel.class).to(RandomVaccination.class).in(Singleton.class);
		bind(FaceMaskModel.class).to(DefaultFaceMaskModel.class).in(Singleton.class);
	}
	@SuppressWarnings("deprecation")
	@Provides
	@Singleton
	public Config config() {
		
//		Population pop = PopulationUtils.readPopulation("HKData\\output\\eventsHongKong_0.1\\output_plans_cleaned.xml.gz");
		Set<String> acts = new HashSet<>();
//		pop.getPersons().entrySet().stream().forEach(p->{
//			p.getValue().getSelectedPlan().getPlanElements().stream().forEach(pl->{
//				if(pl instanceof Activity)acts.add(((Activity)pl).getType());
//			});
//		});
//		acts.remove("pt interaction");
		for(String s:this.DEFAULT_ACTIVITIES)acts.add(s);
		Config config = ConfigUtils.createConfig(new EpisimConfigGroup());
		
		EpisimConfigGroup episimConfig = ConfigUtils.addOrGetModule(config, EpisimConfigGroup.class);

		// Optional network
		config.network().setInputFile("MontrealData/output_network.xml.gz");
		config.facilities().setInputFile("MontrealData/Montreal0.1/facilities1.0.xml.gz");
		config.global().setCoordinateSystem("EPSG:32188");
		// config.network().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-10pct/input/berlin-v5-network.xml.gz");

		// String episimEvents_1pct = "../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct-schools/output-berlin-v5.4-1pct-schools/berlin-v5.4-1pct-schools.output_events_for_episim.xml.gz";
		// String episimEvents_1pct = "../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/output-berlin-v5.4-1pct/berlin-v5.4-1pct.output_events_for_episim.xml.gz";
		// String episimEvents_10pct = "../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-10pct-schools/output-berlin-v5.4-10pct-schools/berlin-v5.4-10pct-schools.output_events_for_episim.xml.gz";

		String url = "MontrealData/Montreal0.1/output_events-1.0.xml.gz";

		episimConfig.setInputEventsFile(url);

		LocalDate startDate = LocalDate.of(2022, 2, 01);

		episimConfig.setStartDate(startDate);
		episimConfig.setFacilitiesHandling(EpisimConfigGroup.FacilitiesHandling.bln);
		episimConfig.setSampleSize(1.0);
		episimConfig.setCalibrationParameter(2);
		
		episimConfig.setHospitalFactor(0.5);
		episimConfig.setProgressionConfig(AbstractSnzScenario2020.baseProgressionConfig(Transition.config()).build());
		CreateRestrictionsFromMobilityDataMontreal mobilityRestriction = new CreateRestrictionsFromMobilityDataMontreal().setInput(new File("HKData/output/HKMobilityReport.csv").toPath());
		
		episimConfig.setPolicy(
				FixedPolicy.config().restrict(startDate, Restriction.ofMask(FaceMask.SURGICAL, .75),
				"education",
				"pt",
				"shop",
				"work").build());
		
		
		
		try {
			episimConfig.setPolicy(mobilityRestriction.createPolicy().build());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long closingIteration = 14;

		addDefaultParams(episimConfig,acts);
		episimConfig.setMaxContacts(4);
		
		int spaces = 20;
		config.controler().setOutputDirectory("MontrealData/output/0.05Percent");
		//contact intensities
		episimConfig.getOrAddContainerParams("pt", "tr").setContactIntensity(10.0).setSpacesPerFacility(spaces);
		episimConfig.getOrAddContainerParams("work").setContactIntensity(1.47).setSpacesPerFacility(spaces);
		episimConfig.getOrAddContainerParams("leisure").setContactIntensity(9.24).setSpacesPerFacility(spaces).setSeasonal(true);
		episimConfig.getOrAddContainerParams("education").setContactIntensity(11.0).setSpacesPerFacility(spaces);
		episimConfig.getOrAddContainerParams("shop").setContactIntensity(11.).setSpacesPerFacility(spaces);
		episimConfig.getOrAddContainerParams("home").setContactIntensity(1.0).setSpacesPerFacility(5); // 33/33
		episimConfig.getOrAddContainerParams("other").setContactIntensity(1.0).setSpacesPerFacility(2); // 33/33
		
//		episimConfig.setPolicy(FixedPolicy.class, FixedPolicy.config()
//				.restrict(startDate.plusDays(closingIteration), Restriction.of(0.0), "leisure", "education")
//				.restrict(startDate.plusDays(closingIteration), Restriction.of(0.2), "work", "business", "other")
//				.restrict(startDate.plusDays(closingIteration), Restriction.of(0.3), "shop", "errand")
//				.restrict(startDate.plusDays(closingIteration), Restriction.of(0.5), "pt")
//				.restrict(startDate.plusDays(closingIteration + 60), Restriction.none(), DEFAULT_ACTIVITIES)
//				.build()
//		);
		if (tracing) {
			TracingConfigGroup tracingConfig = ConfigUtils.addOrGetModule(config, TracingConfigGroup.class);
//			int offset = (int) (ChronoUnit.DAYS.between(episimConfig.getStartDate(), LocalDate.parse("2020-04-01")) + 1);
			int offset = 46;
			tracingConfig.setPutTraceablePersonsInQuarantineAfterDay(offset);
			tracingConfig.setTracingProbability(0.5);
			tracingConfig.setTracingPeriod_days(2); //Is it day from infection? 2 means quaratine for 2 days?
			tracingConfig.setMinContactDuration_sec(15 * 60.); //What does it mean?
			tracingConfig.setQuarantineHouseholdMembers(true);
			tracingConfig.setEquipmentRate(1.);
			tracingConfig.setTracingDelay_days(5);
			tracingConfig.setTraceSusceptible(true);
			tracingConfig.setCapacityType(CapacityType.PER_PERSON);
			int tracingCapacity = 200;
			tracingConfig.setTracingCapacity_pers_per_day(Map.of(
					LocalDate.of(2020, 4, 1), (int) (tracingCapacity * 0.2),
					LocalDate.of(2020, 6, 15), tracingCapacity
			));
		}
		
		VaccinationConfigGroup group = ConfigUtils.addOrGetModule(config, VaccinationConfigGroup.class);
		group.getOrAddParams(VaccinationType.mRNA)
		.setDaysBeforeFullEffect(30)
		.setEffectiveness(VaccinationConfigGroup.forStrain(VirusStrain.OMICRON)
				.atDay(10, 0.5)
				.atDay(20, 0.8)
				.atFullEffect(0.99)
				.atDay(100, 0.8)
		);
		group.getOrAddParams(VaccinationType.vector)
		.setDaysBeforeFullEffect(30)
		.setEffectiveness(VaccinationConfigGroup.forStrain(VirusStrain.OMICRON)
				.atDay(10, 0.4)
				.atDay(20, 0.7)
				.atFullEffect(0.90)
				.atDay(100, 0.75)
		);
		group.getOrAddParams(VaccinationType.generic)
		.setDaysBeforeFullEffect(30)
		.setEffectiveness(VaccinationConfigGroup.forStrain(VirusStrain.OMICRON)
				.atDay(10, 0.3)
				.atDay(20, 0.5)
				.atFullEffect(0.65)
				.atDay(100, 0.6)
		);
		
		//group.set
		String vaccineFileLoc = "MontrealData/vaccineData/VaccineMontreal.csv";// vaccination per day in Montreal
		String vaccineAgeFileLoc = "MontrealData/vaccineData/QuebecVaccinationRateFirstDose.csv";//Distribution of age for vaccinated and non vaccinated people
		String vaccineTypeFileLoc = "MontrealData/vaccineData/vaccination-distribution.csv";//Distribution of vaccine in Quebec province over days
		String infectionFileLoc = "MontrealData/vaccineData/MontrealConfirmedCases.csv";//Infection per day in Montreal
		ReadMontrealData mtlData = new ReadMontrealData(vaccineFileLoc,vaccineAgeFileLoc,vaccineTypeFileLoc,infectionFileLoc,.05);
		
		group.setCompliancePerAge(mtlData.getAgeCompliance(startDate));
		group.setVaccinationCapacity_pers_per_day(mtlData.getVaccineCount());
		group.setReVaccinationCapacity_pers_per_day(mtlData.getReVaccineCount());
		episimConfig.setInfections_pers_per_day(mtlData.getInfection());
		VirusStrainConfigGroup strainConfig = ConfigUtils.addOrGetModule(config, VirusStrainConfigGroup.class);
		StrainParams strain = strainConfig.getOrAddParams(VirusStrain.OMICRON);
		strain.setInfectiousness(0.8);
		strain.setFactorSeriouslySick(0.01);
		
		return config;
	}
	
	public static void main(String[] args) {
//		String[] args1 = new String[]{
//				"--region", "HK",
//				"--from","2020-02-01",
//				"--output", ""
//		};
//		DownloadGoogleMobilityReport.main(args1);
		
		String[] args2 = new String[] {
			"--verbose",
			"--modules",MTLScenario.class.getName()
		};
		RunEpisim.main(args2);
	}
	
	
}



