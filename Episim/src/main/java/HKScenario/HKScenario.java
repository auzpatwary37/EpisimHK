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
import org.matsim.episim.policy.FixedPolicy;
import org.matsim.episim.policy.Restriction;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Scenario based on the publicly available OpenBerlin scenario (https://github.com/matsim-scenarios/matsim-berlin).
 */
public class HKScenario extends AbstractModule {

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

		String url = "output\\scenario\\output_events-0.1_cleaned.xml";

		episimConfig.setInputEventsFile(url);

		LocalDate startDate = LocalDate.of(2020, 2, 20);

		episimConfig.setStartDate(startDate);
		episimConfig.setFacilitiesHandling(EpisimConfigGroup.FacilitiesHandling.bln);
		episimConfig.setSampleSize(0.01);
		episimConfig.setCalibrationParameter(2);
		//  episimConfig.setOutputEventsFolder("events");

		long closingIteration = 14;

		addDefaultParams(episimConfig,acts);

		
		
		episimConfig.setPolicy(FixedPolicy.class, FixedPolicy.config()
				.restrict(startDate.plusDays(closingIteration), Restriction.of(0.0), "leisure", "education")
				.restrict(startDate.plusDays(closingIteration), Restriction.of(0.2), "work", "business", "other")
				.restrict(startDate.plusDays(closingIteration), Restriction.of(0.3), "shop", "errand")
				.restrict(startDate.plusDays(closingIteration), Restriction.of(0.5), "pt")
				.restrict(startDate.plusDays(closingIteration + 60), Restriction.none(), DEFAULT_ACTIVITIES)
				.build()
		);

		return config;
	}
	

	
	
}



