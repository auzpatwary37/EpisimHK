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

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.episim.EpisimConfigGroup;
import org.matsim.episim.TracingConfigGroup;
import org.matsim.episim.TracingConfigGroup.CapacityType;
import org.matsim.episim.VaccinationConfigGroup;
import org.matsim.episim.VirusStrainConfigGroup;
import org.matsim.episim.EpisimConfigGroup.ContagiousOptimization;
import org.matsim.episim.EpisimConfigGroup.DistrictLevelRestrictions;
import org.matsim.episim.VirusStrainConfigGroup.StrainParams;
import org.matsim.episim.model.ContactModel;
import org.matsim.episim.model.DefaultFaceMaskModel;
import org.matsim.episim.model.DefaultInfectionModel;
import org.matsim.episim.model.FaceMask;
import org.matsim.episim.model.FaceMaskModel;
import org.matsim.episim.model.InfectionModel;
import org.matsim.episim.model.InfectionModelWithAntibodies;
import org.matsim.episim.model.SymmetricContactModel;
import org.matsim.episim.model.Transition;
import org.matsim.episim.model.VaccinationType;
import org.matsim.episim.model.VirusStrain;
import org.matsim.episim.model.progression.AntibodyDependentTransitionModel;
import org.matsim.episim.model.progression.DefaultDiseaseStatusTransitionModel;
import org.matsim.episim.model.progression.DiseaseStatusTransitionModel;
import org.matsim.episim.model.vaccination.RandomVaccination;
import org.matsim.episim.model.vaccination.VaccinationByAge;
import org.matsim.episim.model.vaccination.VaccinationModel;
import org.matsim.episim.policy.FixedPolicy;
import org.matsim.episim.policy.Restriction;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.FacilitiesWriter;
import org.matsim.run.RunEpisim;
import org.matsim.run.modules.AbstractSnzScenario2020;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * Scenario based on the publicly available OpenBerlin scenario (https://github.com/matsim-scenarios/matsim-berlin).
 */
public class HKScenario extends AbstractModule {
	boolean tracing = true;
	int tracingNo = 20000;
	int facilityTol = 50;
	public HKScenario() {
		
	}
	public HKScenario(int tracingNo) {
		this.tracingNo = tracingNo;
	}
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
		bind(DiseaseStatusTransitionModel.class).to(AntibodyDependentTransitionModel.class).in(Singleton.class);//DefaultDiseaseStatusTransitionModel
		bind(InfectionModel.class).to(InfectionModelWithAntibodies.class).in(Singleton.class);//DefaultInfectionModel
		bind(VaccinationModel.class).to(RandomVaccination.class).in(Singleton.class);//RandomVaccination
		bind(FaceMaskModel.class).to(DefaultFaceMaskModel.class).in(Singleton.class);
	}

	@Provides
	@Singleton
	public Config config() {
		
//		Population pop = PopulationUtils.readPopulation("HKData/output/eventsHongKong_0.1/output_plans_cleaned.xml.gz");
		Set<String> acts = new HashSet<>();
//		ActivityFacilities facilities = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getActivityFacilities();
		Network odNet = NetworkUtils.readNetwork("HKData/odNetwork.xml");
//		Map<String,Network> facNet = new HashMap<>();
//		Map<String,Integer> facCounter = new HashMap<>();
//		pop.getPersons().entrySet().stream().forEach(p->{
//			p.getValue().getAttributes().putAttribute("age",((Double)p.getValue().getAttributes().getAttribute("age")).intValue());
//			
//			p.getValue().getSelectedPlan().getPlanElements().stream().forEach(pl->{
//				String TPUSBID = null;
//				if(pl instanceof Activity) {
//					acts.add(((Activity)pl).getType());
//					Activity act = (Activity)pl;
//					//if(act.getType().contains("interaction"))return;
//					if(TPUSBID==null)TPUSBID = NetworkUtils.getNearestNode(odNet,act.getCoord()).getId().toString();
//					if(p.getValue().getAttributes().getAttribute("TPUSB")!=null) {
//						p.getValue().getAttributes().putAttribute("TPUSB",TPUSBID);
//					}
//					p.getValue().getAttributes().putAttribute("district",TPUSBID);
//					Id<ActivityFacility> facid = null;
//					ActivityFacility fac = null;
//					Node n = null;
//					if(facNet.get(act.getType())!=null) {
//						n = NetworkUtils.getNearestNode(facNet.get(act.getType()),act.getCoord());
//					}else {
//						facNet.put(act.getType(), NetworkUtils.createNetwork());
//					}
//					if(n!=null && NetworkUtils.getEuclideanDistance(n.getCoord(),act.getCoord())<=facilityTol) {
//						facid = Id.create(n.getId(),ActivityFacility.class);
//						fac = facilities.getFacilities().get(facid);
//						act.setFacilityId(facid);
//					}else {
//						facid = Id.create(act.getType()+facCounter.get(act.getType()),ActivityFacility.class);
//						fac = facilities.getFactory().createActivityFacility(facid, act.getCoord());
//						fac.getAttributes().putAttribute("TPUSB",NetworkUtils.getNearestNode(odNet, act.getCoord()).getId().toString());
//						fac.getAttributes().putAttribute("district",NetworkUtils.getNearestNode(odNet, act.getCoord()).getId().toString());
//						facilities.addActivityFacility(fac);
//						facNet.get(act.getType()).addNode(facNet.get(act.getType()).getFactory().createNode(Id.createNodeId(facid.toString()), fac.getCoord()));
//						act.setFacilityId(facid);
//						facCounter.compute(act.getType(), (k,v)->v==null?1:v+1);
//					}
//				}
//			});
//		});
//		new PopulationWriter(pop).write("HKData/output/eventsHongKong_0.1/output_plans_cleaned_fac.xml.gz");
//		new FacilitiesWriter(facilities).write("HKData/output/eventsHongKong_0.1/facilities.xml.gz");
//		ReadAndChangeEventFile.readAndModifyEventFile("HKData/output/eventsHongKong_0.1/events_cleaned.xml", "HKData/output/eventsHongKong_0.1/events_cleaned_new.xml", pop);
//		acts.remove("pt interaction");
		Config config = ConfigUtils.createConfig(new EpisimConfigGroup());
		config.controler().setLastIteration(40);
		EpisimConfigGroup episimConfig = ConfigUtils.addOrGetModule(config, EpisimConfigGroup.class);

		// Optional network
		config.network().setInputFile("HKData/output_network.xml.gz");
		config.facilities().setInputFile("HKData/output/eventsHongKong_0.1/facilities.xml.gz");
		config.plans().setInputFile("HKData/output/eventsHongKong_0.1/output_plans_cleaned_fac.xml.gz");
		episimConfig.setDistrictLevelRestrictions(DistrictLevelRestrictions.yes);
		episimConfig.setDistrictLevelRestrictionsAttribute("TPUSB");
		//episimConfig.setInitialInfectionDistrict("268008.0");
		
		// config.network().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-10pct/input/berlin-v5-network.xml.gz");

		// String episimEvents_1pct = "../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct-schools/output-berlin-v5.4-1pct-schools/berlin-v5.4-1pct-schools.output_events_for_episim.xml.gz";
		// String episimEvents_1pct = "../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/output-berlin-v5.4-1pct/berlin-v5.4-1pct.output_events_for_episim.xml.gz";
		// String episimEvents_10pct = "../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-10pct-schools/output-berlin-v5.4-10pct-schools/berlin-v5.4-10pct-schools.output_events_for_episim.xml.gz";

		String url = "HKData/output/eventsHongKong_0.1/events_cleaned_new.xml";

		episimConfig.setInputEventsFile(url);
		//episimConfig.setFacilitiesHandling(FacilitiesHandling)
		
		LocalDate startDate = LocalDate.of(2022, 2, 01);

		episimConfig.setStartDate(startDate);
		
		episimConfig.setFacilitiesHandling(EpisimConfigGroup.FacilitiesHandling.snz);
		episimConfig.setSampleSize(1.0);
		//episimConfig.setCalibrationParameter(2);
		episimConfig.setHospitalFactor(.06);
		episimConfig.setProgressionConfig(AbstractSnzScenario2020.baseProgressionConfig(Transition.config()).build());
		//episimConfig.setProgressionConfig(Transition.config()
		
		CreateRestrictionsFromMobilityDataHK mobilityRestriction = new CreateRestrictionsFromMobilityDataHK().setInput(new File("HKData/output/HKMobilityReport.csv").toPath());
		Map<String,Double> rf = odNet.getNodes().keySet().stream().collect(Collectors.toMap(k->k.toString(), k->1.0));
		episimConfig.setPolicy(
				FixedPolicy.config().restrict(startDate, Restriction.ofLocationBasedRf(rf),
						"education",
						"pt",
						"hospital",
						"shopping",
						"mainland",
						"airport",
						"work",
						"business",
						"leisure",
						"park",
						"errand",
						"religious").build());
		
		episimConfig.setPolicy(
				FixedPolicy.config().restrict(startDate, Restriction.ofMask(FaceMask.SURGICAL, .99),
				"education",
				"pt",
				"hospital",
				"shopping",
				"mainland",
				"airport").build());
		episimConfig.setPolicy(
				FixedPolicy.config().restrict(startDate, Restriction.ofMask(FaceMask.SURGICAL, 0.97),
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
		
		episimConfig.setContagiousOptimization(ContagiousOptimization.yes);// what is this?
		for(String s:DEFAULT_ACTIVITIES)acts.add(s);
		addDefaultParams(episimConfig,acts);
		episimConfig.setMaxContacts(5);
		
		int spaces = 5;
		config.controler().setOutputDirectory("HKData/output/0.1Percent");
		//contact intensities
		episimConfig.getOrAddContainerParams("pt", "tr").setContactIntensity(1.0).setSpacesPerFacility(1000);
		episimConfig.getOrAddContainerParams("work").setContactIntensity(1.0).setSpacesPerFacility(20);
		episimConfig.getOrAddContainerParams("leisure").setContactIntensity(1.0).setSpacesPerFacility(100);
//		episimConfig.getOrAddContainerParams("restaurant").setContactIntensity(9.24).setSpacesPerFacility(spaces).setSeasonal(true);
		episimConfig.getOrAddContainerParams("religious").setContactIntensity(1.0).setSpacesPerFacility(100);
		episimConfig.getOrAddContainerParams("education").setContactIntensity(1.0).setSpacesPerFacility(100);
		episimConfig.getOrAddContainerParams("restaurants").setContactIntensity(1.0).setSpacesPerFacility(20);
		episimConfig.getOrAddContainerParams("shopping").setContactIntensity(1.).setSpacesPerFacility(100);
		//episimConfig.getOrAddContainerParams("shop_other").setContactIntensity(0.88).setSpacesPerFacility(spaces);
		episimConfig.getOrAddContainerParams("park").setContactIntensity(.5).setSpacesPerFacility(100);
		episimConfig.getOrAddContainerParams("business").setContactIntensity(1.0).setSpacesPerFacility(20);
		episimConfig.getOrAddContainerParams("family").setContactIntensity(1.0).setSpacesPerFacility(10); // 33/3.57
		episimConfig.getOrAddContainerParams("home").setContactIntensity(1.0).setSpacesPerFacility(5); // 33/33
		episimConfig.getOrAddContainerParams("quarantine_home").setContactIntensity(.3).setSpacesPerFacility(1); // 33/33
		episimConfig.setCalibrationParameter(.01);
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
			int tracingCapacity = this.tracingNo;
			Map<LocalDate,Integer> trCap = new HashMap<>();
			for(long i = 0;i<100;i++) {
				trCap.put(startDate.plusDays(i), tracingCapacity);
			}
			tracingConfig.setTracingCapacity_pers_per_day(trCap);
		}
		
		VaccinationConfigGroup group = ConfigUtils.addOrGetModule(config, VaccinationConfigGroup.class);
		group.getOrAddParams(VaccinationType.mRNA)
		.setDaysBeforeFullEffect(14)
		.setEffectiveness(VaccinationConfigGroup.forStrain(VirusStrain.OMICRON_BA1)
				.atDay(0, 0.)
				.atDay(3, 0.45)
				.atDay(5, 0.85)
				.atFullEffect(0.95)
		).setBoostEffectiveness(VaccinationConfigGroup.forStrain(VirusStrain.OMICRON_BA1)
				.atDay(0, 0.45)
				.atDay(3, 0.65)
				.atDay(5, 0.8)
				.atFullEffect(0.99))
		.setFactorSeriouslySick(VaccinationConfigGroup.forStrain(VirusStrain.OMICRON_BA1)
				.atFullEffect(.019))
		.setFactorShowingSymptoms(VaccinationConfigGroup.forStrain(VirusStrain.OMICRON_BA1)
				.atFullEffect(.33))
		
		.setBoostWaitPeriod(180);
		group.getOrAddParams(VaccinationType.vector)
		.setDaysBeforeFullEffect(14)
		.setEffectiveness(VaccinationConfigGroup.forStrain(VirusStrain.OMICRON_BA1)
				.atDay(0, 0.)
				.atDay(3, 0.4)
				.atDay(5, 0.8)
				.atFullEffect(0.9)
		).setBoostEffectiveness(VaccinationConfigGroup.forStrain(VirusStrain.OMICRON_BA1)
				.atDay(0, 0.4)
				.atDay(3, 0.6)
				.atDay(5, 0.8)
				.atFullEffect(0.95))
		.setBoostWaitPeriod(180);
		
		
		//group.set
		ReadVaccineData vd = new ReadVaccineData("vaccinData/HKVac_new.csv", "vaccinData/Age_complianceHK.csv","vaccinData/owid-covid-data.csv",.1);
		group.setCompliancePerAge(vd.createAgeCompliance());
		group.setVaccinationCapacity_pers_per_day(vd.getVaccinationCapacity());
		group.setReVaccinationCapacity_pers_per_day(vd.getReVaccinationCapacity());
		group.setVaccinationShare(vd.createVaccineShare());
		episimConfig.setInfections_pers_per_day(VirusStrain.OMICRON_BA1,vd.getInfections());
		VirusStrainConfigGroup strainConfig = ConfigUtils.addOrGetModule(config, VirusStrainConfigGroup.class);
		StrainParams strain = strainConfig.getOrAddParams(VirusStrain.OMICRON_BA1);
		strain.setInfectiousness(1.0);
		strain.setFactorCritical(.001);
		strain.setFactorSeriouslySick(0.006);
		
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



