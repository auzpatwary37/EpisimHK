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
package HKScenario;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.episim.EpisimConfigGroup;
import org.matsim.episim.TracingConfigGroup;
import org.matsim.episim.TracingConfigGroup.CapacityType;
import org.matsim.episim.model.AgeDependentInfectionModelWithSeasonality;
import org.matsim.episim.model.ContactModel;
import org.matsim.episim.model.DefaultFaceMaskModel;
import org.matsim.episim.model.DefaultInfectionModel;
import org.matsim.episim.model.FaceMask;
import org.matsim.episim.model.FaceMaskModel;
import org.matsim.episim.model.InfectionModel;
import org.matsim.episim.model.SymmetricContactModel;
import org.matsim.episim.model.Transition;
import org.matsim.episim.model.progression.AgeDependentDiseaseStatusTransitionModel;
import org.matsim.episim.model.progression.DefaultDiseaseStatusTransitionModel;
import org.matsim.episim.model.progression.DiseaseStatusTransitionModel;
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
public class HKScenario extends AbstractModule {
	boolean tracing = true;

	/**
	 * Activity names of the default params from {@link #addDefaultParams(EpisimConfigGroup)}.
	 */
	public String[] DEFAULT_ACTIVITIES = {
			"religious",
			"education",
			"business",
			"work",
			"errand",
			"mainland",
			"home",
			"airport",
			"restaurants",
			"park",
			"hotel",
			"family",
			"hospital",
			"leisure",
			"shopping",
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
		bind(VaccinationModel.class).to(VaccinationByAge.class).in(Singleton.class);
		bind(FaceMaskModel.class).to(DefaultFaceMaskModel.class).in(Singleton.class);
	}
	@SuppressWarnings("deprecation")
	@Provides
	@Singleton
	public Config config() {
		
		Population pop = PopulationUtils.readPopulation("output\\scenario\\population0.1_cleaned.xml.gz");
		Set<String> acts = new HashSet<>();
		pop.getPersons().entrySet().stream().forEach(p->{
			p.getValue().getSelectedPlan().getPlanElements().stream().forEach(pl->{
				if(pl instanceof Activity)acts.add(((Activity)pl).getType());
			});
		});
		acts.remove("pt interaction");
		Config config = ConfigUtils.createConfig(new EpisimConfigGroup());
		
		EpisimConfigGroup episimConfig = ConfigUtils.addOrGetModule(config, EpisimConfigGroup.class);

		// Optional network
		config.network().setInputFile("HKData\\output_network.xml.gz");
		// config.network().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-10pct/input/berlin-v5-network.xml.gz");

		// String episimEvents_1pct = "../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct-schools/output-berlin-v5.4-1pct-schools/berlin-v5.4-1pct-schools.output_events_for_episim.xml.gz";
		// String episimEvents_1pct = "../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/output-berlin-v5.4-1pct/berlin-v5.4-1pct.output_events_for_episim.xml.gz";
		// String episimEvents_10pct = "../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-10pct-schools/output-berlin-v5.4-10pct-schools/berlin-v5.4-10pct-schools.output_events_for_episim.xml.gz";

		String url = "HKData\\output\\eventsCleaned_.1.xml";

		episimConfig.setInputEventsFile(url);

		LocalDate startDate = LocalDate.of(2020, 2, 20);

		episimConfig.setStartDate(startDate);
		episimConfig.setFacilitiesHandling(EpisimConfigGroup.FacilitiesHandling.bln);
		episimConfig.setSampleSize(0.01);
		episimConfig.setCalibrationParameter(2);
		
		episimConfig.setHospitalFactor(0.5);
		episimConfig.setProgressionConfig(AbstractSnzScenario2020.baseProgressionConfig(Transition.config()).build());
		CreateRestrictionsFromMobilityDataHK mobilityRestriction = new CreateRestrictionsFromMobilityDataHK().setInput(new File("HKData/output/HKMobilityReport.csv").toPath());
		
		episimConfig.setPolicy(
				FixedPolicy.config().restrict(startDate, Restriction.ofMask(FaceMask.SURGICAL, .99),
				"education",
				"pt",
				"hospital",
				"shopping",
				"mainland",
				"airport").build());
		
		episimConfig.setPolicy(
				FixedPolicy.config().restrict(startDate, Restriction.ofMask(FaceMask.SURGICAL, .7),
				"work",
				"business",
				"leisure",
				"park",
				"errand",
				"religious").build());
		
		
		try {
			episimConfig.setPolicy(mobilityRestriction.createPolicy().build());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		long closingIteration = 14;

		addDefaultParams(episimConfig,acts);

		int spaces = 20;
		config.controler().setOutputDirectory("HKData/output/0.01Percent");
		//contact intensities
		episimConfig.getOrAddContainerParams("pt", "tr").setContactIntensity(10.0).setSpacesPerFacility(spaces);
		episimConfig.getOrAddContainerParams("work").setContactIntensity(1.47).setSpacesPerFacility(spaces);
		episimConfig.getOrAddContainerParams("leisure").setContactIntensity(9.24).setSpacesPerFacility(spaces).setSeasonal(true);
//		episimConfig.getOrAddContainerParams("restaurant").setContactIntensity(9.24).setSpacesPerFacility(spaces).setSeasonal(true);
		episimConfig.getOrAddContainerParams("religious").setContactIntensity(11.0).setSpacesPerFacility(spaces);
		episimConfig.getOrAddContainerParams("education").setContactIntensity(11.0).setSpacesPerFacility(spaces);
		episimConfig.getOrAddContainerParams("restaurants").setContactIntensity(11.0).setSpacesPerFacility(spaces);
		episimConfig.getOrAddContainerParams("shopping").setContactIntensity(11.).setSpacesPerFacility(spaces);
		//episimConfig.getOrAddContainerParams("shop_other").setContactIntensity(0.88).setSpacesPerFacility(spaces);
		episimConfig.getOrAddContainerParams("park").setContactIntensity(1.47).setSpacesPerFacility(spaces);
		episimConfig.getOrAddContainerParams("business").setContactIntensity(1.47).setSpacesPerFacility(spaces);
		episimConfig.getOrAddContainerParams("family").setContactIntensity(9.24).setSpacesPerFacility(spaces); // 33/3.57
		episimConfig.getOrAddContainerParams("home").setContactIntensity(1.0).setSpacesPerFacility(1); // 33/33
		episimConfig.getOrAddContainerParams("quarantine_home").setContactIntensity(1.0).setSpacesPerFacility(1); // 33/33
		
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
			tracingConfig.setTracingPeriod_days(2);
			tracingConfig.setMinContactDuration_sec(15 * 60.);
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
			"--modules",HKScenario.class.getName()
		};
		RunEpisim.main(args2);
	}
	
	
}



