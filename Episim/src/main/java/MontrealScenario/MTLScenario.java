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

import static org.matsim.episim.model.Transition.to;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.episim.EpisimConfigGroup;
import org.matsim.episim.EpisimPerson;
import org.matsim.episim.TestingConfigGroup;
import org.matsim.episim.TracingConfigGroup;
import org.matsim.episim.TracingConfigGroup.CapacityType;
import org.matsim.episim.VaccinationConfigGroup;
import org.matsim.episim.VirusStrainConfigGroup;
import org.matsim.episim.VirusStrainConfigGroup.StrainParams;
import org.matsim.episim.model.AgeAndProgressionDependentInfectionModelWithSeasonality;
import org.matsim.episim.model.AntibodyModel;
import org.matsim.episim.model.ContactModel;
import org.matsim.episim.model.DefaultAntibodyModel;
import org.matsim.episim.model.DefaultFaceMaskModel;
import org.matsim.episim.model.FaceMask;
import org.matsim.episim.model.FaceMaskModel;
import org.matsim.episim.model.InfectionModel;
import org.matsim.episim.model.SymmetricContactModel;
import org.matsim.episim.model.Transition;
import org.matsim.episim.model.VaccinationType;
import org.matsim.episim.model.VirusStrain;
import org.matsim.episim.model.progression.AgeDependentDiseaseStatusTransitionModel;
import org.matsim.episim.model.progression.DiseaseStatusTransitionModel;
import org.matsim.episim.model.testing.TestType;
import org.matsim.episim.model.vaccination.RandomVaccination;
import org.matsim.episim.model.vaccination.VaccinationModel;
import org.matsim.episim.policy.FixedPolicy;
import org.matsim.episim.policy.Restriction;
import org.matsim.run.RunEpisim;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import de.xypron.jcobyla.Calcfc;
import de.xypron.jcobyla.Cobyla;
import de.xypron.jcobyla.CobylaExitStatus;

/**
 * Scenario based on the publicly available OpenBerlin scenario (https://github.com/matsim-scenarios/matsim-berlin).
 */
public class MTLScenario extends AbstractModule {
	
	public static final String calibrationParameterName = "calibrationParameter";
	public static final String TracingCapacityName = "TracingCapacity";
	public static final String contactIntensityPtName = "contactIntensityPt";
	public static final String contactIntensityHomeName = "contactIntensityHome";
	public static final String contactIntensityWorkName = "contactIntensityWork";
	public static final String contactIntensityEducationName = "contactIntensityEducation";
	public static final String contactIntensityShoppingName = "contactIntensityShopping";
	public static final String contactIntensityOtherName = "contactIntensityOther";
	public static final String contactIntensityQtName = "contactIntensityQt";
	public static final String strainInfectiousnessName = "strainInfectiousness";
	
	
	public static double calibrationParameter = 0.0001;
	public static int TracingCapacity = 20000;
	public static double contactIntensityPt = 1;
	public static double contactIntensityHome = 1.0;
	public static double contactIntensityWork = .5;
	public static double contactIntensityEducation = .5;
	public static double contactIntensityShopping = .3;
	public static double contactIntensityOther = .5;
	public static double contactIntensityQt = .3;
	public static double strainInfectiousness = 1.0;
	public static double multiplier = 1.0;
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
		bind(DiseaseStatusTransitionModel.class).to(AgeDependentDiseaseStatusTransitionModel.class).in(Singleton.class);
		bind(InfectionModel.class).to(AgeAndProgressionDependentInfectionModelWithSeasonality.class).in(Singleton.class);
		bind(VaccinationModel.class).to(RandomVaccination.class).in(Singleton.class);
		bind(FaceMaskModel.class).to(DefaultFaceMaskModel.class).in(Singleton.class);
		bind(AntibodyModel.class).to(DefaultAntibodyModel.class);
		AntibodyModel.Config antibodyConfig = new AntibodyModel.Config();
		 antibodyConfig.setImmuneReponseSigma(3.0);
		 bind(AntibodyModel.Config.class).toInstance(antibodyConfig);
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
		//config.network().setInputFile("MontrealData/output_network.xml.gz");
		config.facilities().setInputFile("MontrealData/Montreal0.1New/facilities1.0.xml.gz");
		config.plans().setInputFile("MontrealData/Montreal0.1New/population1.0.xml.gz");
		config.households().setInputFile("MontrealData/montrealCalibrated/output_households.xml.gz");
		config.global().setCoordinateSystem("EPSG:32188");
		// config.network().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-10pct/input/berlin-v5-network.xml.gz");

		// String episimEvents_1pct = "../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct-schools/output-berlin-v5.4-1pct-schools/berlin-v5.4-1pct-schools.output_events_for_episim.xml.gz";
		// String episimEvents_1pct = "../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/output-berlin-v5.4-1pct/berlin-v5.4-1pct.output_events_for_episim.xml.gz";
		// String episimEvents_10pct = "../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-10pct-schools/output-berlin-v5.4-10pct-schools/berlin-v5.4-10pct-schools.output_events_for_episim.xml.gz";

		String url = "MontrealData/Montreal0.1New/output_events-1.0.xml.gz";

		episimConfig.setInputEventsFile(url);

		LocalDate startDate = LocalDate.of(2021, 12, 1);

		episimConfig.setStartDate(startDate);
		//episimConfig.setFacilitiesHandling(EpisimConfigGroup.FacilitiesHandling.bln);
		episimConfig.setSampleSize(1.0);
		episimConfig.setCalibrationParameter(calibrationParameter);
		
		episimConfig.setHospitalFactor(0.002);
		episimConfig.setProgressionConfig(baseProgressionConfig(Transition.config()).build());
		CreateRestrictionsFromMobilityDataMontreal mobilityRestriction = new CreateRestrictionsFromMobilityDataMontreal().setInput(new File("MontrealData/googleData.csv").toPath());
		
		episimConfig.setPolicy(
				FixedPolicy.config().restrict(startDate, Restriction.ofMask(FaceMask.SURGICAL, .95),
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
		config.controler().setOutputDirectory("MontrealData/output/0.05Percent_allactOpen");
		//contact intensities
		episimConfig.setFacilitiesHandling(EpisimConfigGroup.FacilitiesHandling.snz);
		episimConfig.getOrAddContainerParams("pt", "tr").setContactIntensity(contactIntensityPt*multiplier).setSpacesPerFacility(spaces);
		episimConfig.getOrAddContainerParams("work").setContactIntensity(contactIntensityWork*multiplier).setSpacesPerFacility(10);
		//episimConfig.getOrAddContainerParams("leisure").setContactIntensity(9.24).setSpacesPerFacility(spaces).setSeasonality(1.0);
		episimConfig.getOrAddContainerParams("education").setContactIntensity(contactIntensityEducation*multiplier).setSpacesPerFacility(spaces);
		episimConfig.getOrAddContainerParams("shop").setContactIntensity(contactIntensityShopping*multiplier).setSpacesPerFacility(10);
		episimConfig.getOrAddContainerParams("home").setContactIntensity(contactIntensityHome*multiplier).setSpacesPerFacility(5); // 33/33
		episimConfig.getOrAddContainerParams("other").setContactIntensity(contactIntensityOther*multiplier).setSpacesPerFacility(2); // 33/33
		episimConfig.getOrAddContainerParams("quarantine_home").setContactIntensity(contactIntensityQt*multiplier).setSpacesPerFacility(1);
		episimConfig.setPolicy(FixedPolicy.class, FixedPolicy.config()
				.restrict(startDate, Restriction.ofClosingHours(18, 25), "leisure", "education")
				.restrict(startDate, Restriction.ofClosingHours(18, 25), "work", "business", "other")
				//.restrict(startDate.plusDays(closingIteration), Restriction.of(0.0), "shop", "errand")
				//.restrict(startDate.plusDays(closingIteration), Restriction.of(0.0), "pt")
				//.restrict(startDate.plusDays(closingIteration + 60), Restriction.none(), DEFAULT_ACTIVITIES)
				.build()
		);
		 
		
		if (tracing) {
			TracingConfigGroup tracingConfig = ConfigUtils.addOrGetModule(config, TracingConfigGroup.class);
//			int offset = (int) (ChronoUnit.DAYS.between(episimConfig.getStartDate(), LocalDate.parse("2020-04-01")) + 1);
			int offset = 0;
			tracingConfig.setPutTraceablePersonsInQuarantineAfterDay(offset);
			
			tracingConfig.setTracingProbability(1.);
			tracingConfig.setTracingPeriod_days(3); //Is it day from infection? 2 means quaratine for 2 days?
			tracingConfig.setMinContactDuration_sec(0.); //What does it mean?
			tracingConfig.setQuarantineHouseholdMembers(false);
			tracingConfig.setEquipmentRate(1.);
			tracingConfig.setTracingDelay_days(0);
			tracingConfig.setTraceSusceptible(false);
			tracingConfig.setCapacityType(CapacityType.PER_PERSON);
			int tracingCapacity = TracingCapacity;
			Map<LocalDate,Integer> trCap = new HashMap<>();
			for(long i = 0;i<100;i++) {
				trCap.put(startDate.plusDays(i), (int)tracingCapacity);
			}
			tracingConfig.setTracingCapacity_pers_per_day(trCap);
		}
		
		VaccinationConfigGroup group = ConfigUtils.addOrGetModule(config, VaccinationConfigGroup.class);
		group.getOrAddParams(VaccinationType.mRNA)
		.setDaysBeforeFullEffect(14)
		.setEffectiveness(VaccinationConfigGroup.forStrain(VirusStrain.OMICRON_BA1)
				.atDay(0, 0.25)
				.atDay(14, 0.5)
				.atDay(43, 0.59)
				.atDay(180, 0.6)
				
		).setBoostEffectiveness(VaccinationConfigGroup.forStrain(VirusStrain.OMICRON_BA1)
				.atDay(0, 0.59)
				.atDay(14, 0.88)
				.atDay(43, 0.89)
				.atFullEffect(0.89))
		.setFactorSeriouslySick(VaccinationConfigGroup.forStrain(VirusStrain.OMICRON_BA1)
				.atFullEffect(.0003))
		
		
		.setBoostWaitPeriod(180);
		group.getOrAddParams(VaccinationType.vector)
		.setDaysBeforeFullEffect(14)
		.setEffectiveness(VaccinationConfigGroup.forStrain(VirusStrain.OMICRON_BA1)
				.atDay(0, .25)
				.atDay(14, 0.5)
				.atDay(43, 0.55)
				.atDay(180, .6)
		).setBoostEffectiveness(VaccinationConfigGroup.forStrain(VirusStrain.OMICRON_BA1)
				.atDay(0, .6)
				.atDay(14, 0.85)
				.atDay(43, 0.88)
				.atFullEffect(0.89))
		.setBoostWaitPeriod(180);
		// testing 
		
		TestingConfigGroup testingConfigGroup = ConfigUtils.addOrGetModule(config, TestingConfigGroup.class);

		 

		 //TestingConfigGroup.TestingParams rapidTest = testingConfigGroup.getOrAddParams(TestType.RAPID_TEST);
		 TestingConfigGroup.TestingParams pcrTest = testingConfigGroup.getOrAddParams(TestType.PCR);

	
		
		 

		 pcrTest.setFalseNegativeRate(0.1);
		 pcrTest.setFalsePositiveRate(0.01);

		 testingConfigGroup.setHouseholdCompliance(1.0);

		 pcrTest.setTestingCapacity_pers_per_day(1000);
		 
		 testingConfigGroup.setStrategy(TestingConfigGroup.Strategy.ACTIVITIES);
		 List<String> activities = new ArrayList<>();
		 for(String s:DEFAULT_ACTIVITIES)activities.add(s);
		 testingConfigGroup.setActivities(activities);
		 
		 





		//group.set
		String vaccineFileLoc = "MontrealData/vaccineData/VaccineMontreal.csv";// vaccination per day in Montreal
		String vaccineAgeFileLoc = "MontrealData/vaccineData/QuebecVaccinationRateFirstDose.csv";//Distribution of age for vaccinated and non vaccinated people
		String vaccineTypeFileLoc = "MontrealData/vaccineData/vaccination-distribution.csv";//Distribution of vaccine in Quebec province over days
		String infectionFileLoc = "MontrealData/vaccineData/MontrealConfirmedCases.csv";//Infection per day in Montreal
		ReadMontrealData mtlData = new ReadMontrealData(vaccineFileLoc,vaccineAgeFileLoc,vaccineTypeFileLoc,infectionFileLoc,.05);
		Map<VirusStrain,Integer> initInfection = new HashMap<>();
		initInfection.put(VirusStrain.OMICRON_BA1, mtlData.getInfection().get(startDate));
		//episimConfig.setInitialInfections(mtlData.getInfection().get(startDate));
		group.setCompliancePerAge(mtlData.getAgeCompliance(startDate));
		group.setVaccinationCapacity_pers_per_day(mtlData.getVaccineCount());
		group.setReVaccinationCapacity_pers_per_day(mtlData.getReVaccineCount());
		group.setVaccinationShare(mtlData.getVaccineType());
		episimConfig.setInfections_pers_per_day(VirusStrain.OMICRON_BA1,mtlData.getInfection());
		VirusStrainConfigGroup strainConfig = ConfigUtils.addOrGetModule(config, VirusStrainConfigGroup.class);
		StrainParams strain = strainConfig.getOrAddParams(VirusStrain.OMICRON_BA1);
		strain.setInfectiousness(strainInfectiousness);
		strain.setFactorCritical(.002);
		strain.setFactorSeriouslySick(0.0003);
		
		return config;
	}
	
	
	
	
	public static void main(String[] args) {
//		String[] args1 = new String[]{
//				"--region", "HK",
//				"--from","2020-02-01",
//				"--output", ""
//		};
//		DownloadGoogleMobilityReport.main(args1);
		MTLScenario.calibrationParameter = 0.00017;
		String[] args2 = new String[] {
			"--iterations","250",
			"--verbose",
			"--modules",MTLScenario.class.getName()
		};
		RunEpisim.main(args2);
//		String vaccineFileLoc = "MontrealData/vaccineData/VaccineMontreal.csv";// vaccination per day in Montreal
//		String vaccineAgeFileLoc = "MontrealData/vaccineData/QuebecVaccinationRateFirstDose.csv";//Distribution of age for vaccinated and non vaccinated people
//		String vaccineTypeFileLoc = "MontrealData/vaccineData/vaccination-distribution.csv";//Distribution of vaccine in Quebec province over days
//		String infectionFileLoc = "MontrealData/vaccineData/MontrealConfirmedCases.csv";//Infection per day in Montreal
//		ReadMontrealData mtlData = new ReadMontrealData(vaccineFileLoc,vaccineAgeFileLoc,vaccineTypeFileLoc,infectionFileLoc,.05);
//		
//		Map<String,Double> initialParameter = new HashMap<>();
//		Map<String,Tuple<Double,Double>> limit = new HashMap<>();
		
//		initialParameter.put(MTLScenario.calibrationParameterName,.00003);
//		initialParameter.put(MTLScenario.TracingCapacityName, 20000.);
//		initialParameter.put(MTLScenario.contactIntensityPtName, .8);
//		initialParameter.put(MTLScenario.contactIntensityHomeName,.8);
//		initialParameter.put(MTLScenario.contactIntensityEducationName, .8);
//		initialParameter.put(MTLScenario.contactIntensityWorkName, .8);
//		initialParameter.put(MTLScenario.contactIntensityOtherName, .8);
//		initialParameter.put(MTLScenario.contactIntensityQtName, .3);
//		initialParameter.put(MTLScenario.contactIntensityShoppingName, 1.);
//		initialParameter.put(MTLScenario.strainInfectiousnessName, 1.);
		
//		limit.put(MTLScenario.calibrationParameterName,new Tuple<>(.00001,.00004));
//		limit.put(MTLScenario.TracingCapacityName, new Tuple<>(1000.,10000.));
//		limit.put(MTLScenario.contactIntensityPtName, new Tuple<>(0.,1.));
//		limit.put(MTLScenario.contactIntensityHomeName,new Tuple<>(0.,1.));
//		limit.put(MTLScenario.contactIntensityEducationName, new Tuple<>(0.,1.));
//		limit.put(MTLScenario.contactIntensityWorkName, new Tuple<>(0.,1.));
//		limit.put(MTLScenario.contactIntensityOtherName, new Tuple<>(0.,1.));
//		limit.put(MTLScenario.contactIntensityQtName, new Tuple<>(0.,1.));
//		limit.put(MTLScenario.contactIntensityShoppingName, new Tuple<>(0.,1.));
//		limit.put(MTLScenario.strainInfectiousnessName, new Tuple<>(0.,1.));
		
//		Map<String,Double> result = calibrateEpisim(initialParameter,limit,mtlData.getInfection(VirusStrain.OMICRON_BA1),"MontrealData/output/0.05Percent_allactOpen","MontrealData/output/iterLogger.csv");
		
	}
	
	public static double calcObj(Map<VirusStrain,Map<LocalDate,Integer>> infectionObj, String outputFolder) {
		double obj = 0;
		Map<LocalDate, Double> infection = new HashMap<>();
		infectionObj.entrySet().forEach(inf->{
			inf.getValue().entrySet().forEach(i->{
				infection.compute(i.getKey(), (k,v)->v==null?i.getValue():v+i.getValue());
			});
		});
		Map<LocalDate,Double> simInfection = new HashMap<>();
		try {
			BufferedReader bf = new BufferedReader(new FileReader(new File(outputFolder+"/infections.txt")));
			bf.readLine();
			String line = null;
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
			while((line = bf.readLine())!=null) {
				String[] part = line.split("\t");
				LocalDate date = LocalDate.parse(part[2],formatter);
				double infected = Double.parseDouble(part[14]);
				simInfection.put(date, infected);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		for(LocalDate d:infection.keySet()) {
			
			if(simInfection.containsKey(d)) {
				double simInf = simInfection.get(d);
				obj+=Math.pow(simInf-infection.get(d),2);
			}
		}
		
		return obj;
	}
	
	
	public static Map<String,Double> calibrateEpisim(Map<String,Double> initialParam, Map<String,Tuple<Double,Double>>limit, Map<VirusStrain,Map<LocalDate,Integer>>infections, String outputFolder, String optimizationFileLoc){
		Map<String,Double> result = new HashMap<>(initialParam);
		List<String> keyOrder = new ArrayList<>();
		keyOrder.add(MTLScenario.calibrationParameterName);
//		keyOrder.add(MTLScenario.TracingCapacityName);
//		keyOrder.add(MTLScenario.contactIntensityPtName);
//		keyOrder.add(MTLScenario.contactIntensityHomeName);
//		keyOrder.add(MTLScenario.contactIntensityEducationName);
//		keyOrder.add(MTLScenario.contactIntensityWorkName);
//		keyOrder.add(MTLScenario.contactIntensityOtherName);
//		keyOrder.add(MTLScenario.contactIntensityQtName);
//		keyOrder.add(MTLScenario.contactIntensityShoppingName);
		keyOrder.add(MTLScenario.strainInfectiousnessName);
		Calcfc fc = null;
		try {
			fc = new Calcfc() {
				double bestobj = Double.POSITIVE_INFINITY;
				double[] bestx = new double[10];
				int iter = 0;
				FileWriter fw = new FileWriter(new File(optimizationFileLoc));
				@Override
				public double compute(int n, int m, double[] x, double[] con) {
					apply(x,limit);
					String[] args2 = new String[] {
							"--iterations","150",
							"--verbose",
							"--modules",MTLScenario.class.getName()
						};
					RunEpisim.main(args2);
					for(int i = 0;i<x.length;i++) {
						con[i*2] = x[i];
						con[i*2+1] = 100-x[i];
					}
					double obj = calcObj(infections,outputFolder);
					if(bestobj>obj) {
						bestobj = obj;
						bestx = x;
					}
					try {
						writeDownDetails(obj,x);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					iter++;
					return obj;
				}
				
				public void writeDownDetails(double obj,double[] x) throws IOException {
					
					if(iter == 0) {
						
						fw.append("iter");
						for (String s : keyOrder) {
							fw.append(s+"_best,");
						}
						fw.append("bestObj"+",");
						for (String s : keyOrder) {
							fw.append(s+"_current,");
						}
						fw.append("currentObj"+"\n");
						fw.flush();
					}
					
					fw.append(iter+",");
					Map<String,Double> best = scaleUp(this.bestx, limit);
					Map<String,Double> current = scaleUp(x, limit);
					for (String s : keyOrder) {
						fw.append(best.get(s)+",");
					}
					fw.append(this.bestobj+",");
					for (String s : keyOrder) {
						fw.append(current.get(s)+",");
					}
					fw.append(obj+"\n");
					fw.flush();
				}
				
			};
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		double[] x = scaleDown(initialParam,limit);
		CobylaExitStatus out =  Cobyla.findMinimum(fc,2,4,x,10,.5,3,50);
		result = scaleUp(x,limit);
		return result;
	}
	
	
	public static double[] scaleDown(Map<String,Double> param, Map<String,Tuple<Double,Double>>limit) {
		
		double x[] = new double[param.size()];
		x[0] = 100*(param.get(MTLScenario.calibrationParameterName)-limit.get(MTLScenario.calibrationParameterName).getFirst())/(limit.get(MTLScenario.calibrationParameterName).getSecond()-limit.get(MTLScenario.calibrationParameterName).getFirst());
//		x[1] = 100*(param.get(MTLScenario.TracingCapacityName)-limit.get(MTLScenario.TracingCapacityName).getFirst())/(limit.get(MTLScenario.TracingCapacityName).getSecond()-limit.get(MTLScenario.TracingCapacityName).getFirst());
//		x[2] = 100*(param.get(MTLScenario.contactIntensityPtName)-limit.get(MTLScenario.contactIntensityPtName).getFirst())/(limit.get(MTLScenario.contactIntensityPtName).getSecond()-limit.get(MTLScenario.contactIntensityPtName).getFirst());
//		x[3] = 100*(param.get(MTLScenario.contactIntensityHomeName)-limit.get(MTLScenario.contactIntensityHomeName).getFirst())/(limit.get(MTLScenario.contactIntensityHomeName).getSecond()-limit.get(MTLScenario.contactIntensityHomeName).getFirst());
//		x[4] = 100*(param.get(MTLScenario.contactIntensityEducationName)-limit.get(MTLScenario.contactIntensityEducationName).getFirst())/(limit.get(MTLScenario.contactIntensityEducationName).getSecond()-limit.get(MTLScenario.contactIntensityEducationName).getFirst());
//		x[5] = 100*(param.get(MTLScenario.contactIntensityWorkName)-limit.get(MTLScenario.contactIntensityWorkName).getFirst())/(limit.get(MTLScenario.contactIntensityWorkName).getSecond()-limit.get(MTLScenario.contactIntensityWorkName).getFirst());
//		x[6] = 100*(param.get(MTLScenario.contactIntensityOtherName)-limit.get(MTLScenario.contactIntensityOtherName).getFirst())/(limit.get(MTLScenario.contactIntensityOtherName).getSecond()-limit.get(MTLScenario.contactIntensityOtherName).getFirst());
//		x[7] = 100*(param.get(MTLScenario.contactIntensityQtName)-limit.get(MTLScenario.contactIntensityQtName).getFirst())/(limit.get(MTLScenario.contactIntensityQtName).getSecond()-limit.get(MTLScenario.contactIntensityQtName).getFirst());
//		x[8] = 100*(param.get(MTLScenario.contactIntensityShoppingName)-limit.get(MTLScenario.contactIntensityShoppingName).getFirst())/(limit.get(MTLScenario.contactIntensityShoppingName).getSecond()-limit.get(MTLScenario.contactIntensityShoppingName).getFirst());
		x[1] = 100*(param.get(MTLScenario.strainInfectiousnessName)-limit.get(MTLScenario.strainInfectiousnessName).getFirst())/(limit.get(MTLScenario.strainInfectiousnessName).getSecond()-limit.get(MTLScenario.strainInfectiousnessName).getFirst());
		return x;
	}
	
	public static void apply(double[] x, Map<String,Tuple<Double,Double>>limit) {
		Map<String,Double> d =scaleUp(x, limit);
		
		
		MTLScenario.calibrationParameter = d.get(MTLScenario.calibrationParameterName);
//		MTLScenario.TracingCapacity =  d.get(MTLScenario.TracingCapacityName).intValue();
//		MTLScenario.contactIntensityPt = d.get(MTLScenario.contactIntensityPtName);
//		MTLScenario.contactIntensityHome = d.get(MTLScenario.contactIntensityHomeName);
//		MTLScenario.contactIntensityEducation = d.get(MTLScenario.contactIntensityEducationName);
//		MTLScenario.contactIntensityWork =d.get(MTLScenario.contactIntensityWorkName);
//		MTLScenario.contactIntensityOther = d.get(MTLScenario.contactIntensityOtherName);
//		MTLScenario.contactIntensityQt = d.get(MTLScenario.contactIntensityQtName);
//		MTLScenario.contactIntensityShopping = d.get(MTLScenario.contactIntensityShoppingName);
		MTLScenario.strainInfectiousness = d.get(MTLScenario.strainInfectiousnessName);		

	}
	
	public static Map<String,Double> scaleUp(double[] x, Map<String,Tuple<Double,Double>> limit) {
		
		Map<String,Double> param = new HashMap<>();
		
		param.put(MTLScenario.calibrationParameterName,limit.get(MTLScenario.calibrationParameterName).getFirst()+ x[0]/100*(limit.get(MTLScenario.calibrationParameterName).getSecond()-limit.get(MTLScenario.calibrationParameterName).getFirst()));
//		param.put(MTLScenario.TracingCapacityName, limit.get(MTLScenario.TracingCapacityName).getFirst()+x[1]/100*(limit.get(MTLScenario.TracingCapacityName).getSecond()-limit.get(MTLScenario.TracingCapacityName).getFirst()));
//		param.put(MTLScenario.contactIntensityPtName, limit.get(MTLScenario.contactIntensityPtName).getFirst()+x[2]/100*(limit.get(MTLScenario.contactIntensityPtName).getSecond()-limit.get(MTLScenario.contactIntensityPtName).getFirst()));
//		param.put(MTLScenario.contactIntensityHomeName,limit.get(MTLScenario.contactIntensityHomeName).getFirst()+ x[3]/100*(limit.get(MTLScenario.contactIntensityHomeName).getSecond()-limit.get(MTLScenario.contactIntensityHomeName).getFirst()));
//		param.put(MTLScenario.contactIntensityEducationName, limit.get(MTLScenario.contactIntensityEducationName).getFirst()+x[4]/100*(limit.get(MTLScenario.contactIntensityEducationName).getSecond()-limit.get(MTLScenario.contactIntensityEducationName).getFirst()));
//		param.put(MTLScenario.contactIntensityWorkName, limit.get(MTLScenario.contactIntensityWorkName).getFirst()+x[5]/100*(limit.get(MTLScenario.contactIntensityWorkName).getSecond()-limit.get(MTLScenario.contactIntensityWorkName).getFirst()));
//		param.put(MTLScenario.contactIntensityOtherName, limit.get(MTLScenario.contactIntensityOtherName).getFirst()+x[6]/100*(limit.get(MTLScenario.contactIntensityOtherName).getSecond()-limit.get(MTLScenario.contactIntensityOtherName).getFirst()));
//		param.put(MTLScenario.contactIntensityQtName, limit.get(MTLScenario.contactIntensityQtName).getFirst()+x[7]/100*(limit.get(MTLScenario.contactIntensityQtName).getSecond()-limit.get(MTLScenario.contactIntensityQtName).getFirst()));
//		param.put(MTLScenario.contactIntensityShoppingName, limit.get(MTLScenario.contactIntensityShoppingName).getFirst()+x[8]/100*(limit.get(MTLScenario.contactIntensityShoppingName).getSecond()-limit.get(MTLScenario.contactIntensityShoppingName).getFirst()));
		param.put(MTLScenario.strainInfectiousnessName, limit.get(MTLScenario.strainInfectiousnessName).getFirst()+x[1]/100*(limit.get(MTLScenario.strainInfectiousnessName).getSecond()-limit.get(MTLScenario.strainInfectiousnessName).getFirst()));
		return param;
	}
	
	public static Transition.Builder baseProgressionConfig(Transition.Builder builder) {
		return builder
				// Inkubationszeit: Die Inkubationszeit [ ... ] liegt im Mittel (Median) bei 5–6 Tagen (Spannweite 1 bis 14 Tage)
				.from(EpisimPerson.DiseaseStatus.infectedButNotContagious,
						to(EpisimPerson.DiseaseStatus.contagious, Transition.logNormalWithMedianAndStd(1., 1.)))

// Dauer Infektiosität:: Es wurde geschätzt, dass eine relevante Infektiosität bereits zwei Tage vor Symptombeginn vorhanden ist und die höchste Infektiosität am Tag vor dem Symptombeginn liegt
// Dauer Infektiosität: Abstrichproben vom Rachen enthielten vermehrungsfähige Viren bis zum vierten, aus dem Sputum bis zum achten Tag nach Symptombeginn
				.from(EpisimPerson.DiseaseStatus.contagious,
						to(EpisimPerson.DiseaseStatus.showingSymptoms, Transition.logNormalWithMedianAndStd(2., 1.)),    //80%
						to(EpisimPerson.DiseaseStatus.recovered, Transition.logNormalWithMedianAndStd(7., 2.)))            //20%

// Erkankungsbeginn -> Hospitalisierung: Eine Studie aus Deutschland zu 50 Patienten mit eher schwereren Verläufen berichtete für alle Patienten eine mittlere (Median) Dauer von vier Tagen (IQR: 1–8 Tage)
				.from(EpisimPerson.DiseaseStatus.showingSymptoms,
						to(EpisimPerson.DiseaseStatus.seriouslySick, Transition.logNormalWithMedianAndStd(5., 2.)),
						to(EpisimPerson.DiseaseStatus.recovered, Transition.logNormalWithMedianAndStd(5., 2.)))

// Hospitalisierung -> ITS: In einer chinesischen Fallserie betrug diese Zeitspanne im Mittel (Median) einen Tag (IQR: 0–3 Tage)
				.from(EpisimPerson.DiseaseStatus.seriouslySick,
						to(EpisimPerson.DiseaseStatus.critical, Transition.logNormalWithMedianAndStd(1., 1.)),
						to(EpisimPerson.DiseaseStatus.recovered, Transition.logNormalWithMedianAndStd(5., 2.)))

// Dauer des Krankenhausaufenthalts: „WHO-China Joint Mission on Coronavirus Disease 2019“ wird berichtet, dass milde Fälle im Mittel (Median) einen Krankheitsverlauf von zwei Wochen haben und schwere von 3–6 Wochen
				.from(EpisimPerson.DiseaseStatus.critical,
						to(EpisimPerson.DiseaseStatus.seriouslySickAfterCritical, Transition.logNormalWithMedianAndStd(14., 7.)))

				.from(EpisimPerson.DiseaseStatus.seriouslySickAfterCritical,
						to(EpisimPerson.DiseaseStatus.recovered, Transition.logNormalWithMedianAndStd(7., 7.)))

				.from(EpisimPerson.DiseaseStatus.recovered,
						to(EpisimPerson.DiseaseStatus.susceptible, Transition.logNormalWithMean(120, 30)))

				;

		// yyyy Quellen für alle Aussagen oben??  "Es" oder "Eine Studie aus ..." ist mir eigentlich nicht genug.  kai, aug'20
		// yyyy Der obige Code existiert nochmals in ConfigurableProgressionModel.  Können wir in konsolidieren?  kai, oct'20

	}
}



