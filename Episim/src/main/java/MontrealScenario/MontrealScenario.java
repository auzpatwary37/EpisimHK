package MontrealScenario;


import HKScenario.CreateRestrictionsFromMobilityDataHK;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.episim.EpisimConfigGroup;
import org.matsim.episim.TracingConfigGroup;
import org.matsim.episim.model.*;
import org.matsim.episim.model.progression.DefaultDiseaseStatusTransitionModel;
import org.matsim.episim.model.progression.DiseaseStatusTransitionModel;
import org.matsim.episim.model.vaccination.RandomVaccination;
import org.matsim.episim.model.vaccination.VaccinationModel;
import org.matsim.episim.policy.FixedPolicy;
import org.matsim.episim.policy.Restriction;
import org.matsim.run.RunEpisim;
import org.matsim.run.modules.AbstractSnzScenario2020;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
@Deprecated
public class MontrealScenario extends AbstractModule {
    boolean tracing = true;

    /**
     * Activity names of the default params.
     */
    public String[] DEFAULT_ACTIVITIES = {
            "other",
            "education",
            "shop",
            "work",
            "leisure",
            "home"
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

//        Population pop = PopulationUtils.readPopulation("MontrealData/Montreal0.1/population1.0.xml.gz");
        Set<String> acts = new HashSet<>();
        for (String s:DEFAULT_ACTIVITIES) {
            acts.add(s);
        }
//        pop.getPersons().entrySet().stream().forEach(p->{
//            p.getValue().getSelectedPlan().getPlanElements().stream().forEach(pl->{
//                if(pl instanceof Activity)acts.add(((Activity)pl).getType());
//            });
//        });
//        acts.remove("pt interaction");

        Config config = ConfigUtils.createConfig(new EpisimConfigGroup());

        EpisimConfigGroup episimConfig = ConfigUtils.addOrGetModule(config, EpisimConfigGroup.class);

        // Optional network
        config.network().setInputFile("MontrealData/output_network.xml.gz");
        // config.network().setInputFile("https://svn.vsp.tu-berlin.de/repos/public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-10pct/input/berlin-v5-network.xml.gz");

        // String episimEvents_1pct = "../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct-schools/output-berlin-v5.4-1pct-schools/berlin-v5.4-1pct-schools.output_events_for_episim.xml.gz";
        // String episimEvents_1pct = "../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-1pct/output-berlin-v5.4-1pct/berlin-v5.4-1pct.output_events_for_episim.xml.gz";
        // String episimEvents_10pct = "../public-svn/matsim/scenarios/countries/de/berlin/berlin-v5.4-10pct-schools/output-berlin-v5.4-10pct-schools/berlin-v5.4-10pct-schools.output_events_for_episim.xml.gz";

        String url = "MontrealData/Montreal0.1/output_events-1.0.xml.gz";

        episimConfig.setInputEventsFile(url);

        LocalDate startDate = LocalDate.of(2020, 2, 20);

        episimConfig.setStartDate(startDate);
        episimConfig.setFacilitiesHandling(EpisimConfigGroup.FacilitiesHandling.bln);
        episimConfig.setSampleSize(0.01);
        episimConfig.setCalibrationParameter(2);

        episimConfig.setHospitalFactor(0.5);
        episimConfig.setProgressionConfig(AbstractSnzScenario2020.baseProgressionConfig(Transition.config()).build());
        CreateRestrictionsFromMobilityDataHK mobilityRestriction = new CreateRestrictionsFromMobilityDataHK().setInput(new File("MontrealData/googleData.csv").toPath());

        episimConfig.setPolicy(
                FixedPolicy.config().restrict(startDate, Restriction.ofMask(FaceMask.SURGICAL, .99),
                        "education",
                        "pt",
                        "hospital",
                        "shop",
                        "work",
                        "leisure",
                        "other").build());

//        episimConfig.setPolicy(
//                FixedPolicy.config().restrict(startDate, Restriction.ofMask(FaceMask.SURGICAL, .7),
//                        "work",
//                        "business",
//                        "leisure",
//                        "park",
//                        "errand",
//                        "religious").build());


        try {
            episimConfig.setPolicy(mobilityRestriction.createPolicy().build());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        long closingIteration = 14;

        addDefaultParams(episimConfig,acts);

        int spaces = 20;
        config.controler().setOutputDirectory("MontrealData/output/0.01Percent");
        //contact intensities
        episimConfig.getOrAddContainerParams("pt", "tr").setContactIntensity(1.0).setSpacesPerFacility(spaces);
        episimConfig.getOrAddContainerParams("work").setContactIntensity(1.47).setSpacesPerFacility(spaces);
        episimConfig.getOrAddContainerParams("leisure").setContactIntensity(1.24).setSpacesPerFacility(spaces).setSeasonal(true);
//		episimConfig.getOrAddContainerParams("restaurant").setContactIntensity(9.24).setSpacesPerFacility(spaces).setSeasonal(true);
//        episimConfig.getOrAddContainerParams("religious").setContactIntensity(11.0).setSpacesPerFacility(spaces);
        episimConfig.getOrAddContainerParams("education").setContactIntensity(1.5).setSpacesPerFacility(spaces);
 //       episimConfig.getOrAddContainerParams("restaurants").setContactIntensity(11.0).setSpacesPerFacility(spaces);
        episimConfig.getOrAddContainerParams("shop").setContactIntensity(1.).setSpacesPerFacility(spaces);
        //episimConfig.getOrAddContainerParams("shop_other").setContactIntensity(0.88).setSpacesPerFacility(spaces);
        episimConfig.getOrAddContainerParams("other").setContactIntensity(1).setSpacesPerFacility(spaces);
  //      episimConfig.getOrAddContainerParams("business").setContactIntensity(1.47).setSpacesPerFacility(spaces);
  //      episimConfig.getOrAddContainerParams("family").setContactIntensity(9.24).setSpacesPerFacility(spaces); // 33/3.57
        episimConfig.getOrAddContainerParams("home").setContactIntensity(3.0).setSpacesPerFacility(1); // 33/33
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
            tracingConfig.setTracingPeriod_days(2); //Is it day from infection? 2 means quaratine for 2 days?
            tracingConfig.setMinContactDuration_sec(15 * 60.); //What does it mean?
            tracingConfig.setQuarantineHouseholdMembers(true);
            tracingConfig.setEquipmentRate(1.);
            tracingConfig.setTracingDelay_days(5);
            tracingConfig.setTraceSusceptible(true);
            tracingConfig.setCapacityType(TracingConfigGroup.CapacityType.PER_PERSON);
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
        Network net = NetworkUtils.readNetwork("MontrealData/output_network.xml.gz");
        String[] args2 = new String[] {
                "--verbose",
                "--modules", MontrealScenario.class.getName()
        };
        RunEpisim.main(args2);
    }
}
