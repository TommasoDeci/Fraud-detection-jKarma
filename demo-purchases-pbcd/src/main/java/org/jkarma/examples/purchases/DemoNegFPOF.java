/*******************************************************************************
 * Copyright 2019 Angelo Impedovo
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package org.jkarma.examples.purchases;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.stream.Stream;

import org.jkarma.examples.purchases.model.Transazione;
import org.jkarma.mining.heuristics.AreaHeuristic;
import org.jkarma.mining.joiners.TidSet;
import org.jkarma.mining.providers.NegativeTidSetProvider;
import org.jkarma.mining.providers.WindowedProvider;
import org.jkarma.mining.structures.MiningStrategy;
import org.jkarma.mining.structures.Strategies;
import org.jkarma.mining.windows.Windows;
import org.jkarma.pbad.anomalies.NegativeFPOFAnomaly;
import org.jkarma.pbad.detectors.AnomalyDetectedEvent;
import org.jkarma.pbad.detectors.AnomalyNotDetectedEvent;
import org.jkarma.pbad.detectors.PBAD;
import org.jkarma.pbad.detectors.PBADEventListener;
import org.jkarma.pbcd.descriptors.Descriptors;
import org.jkarma.pbcd.detectors.Detectors;
import org.jkarma.pbcd.detectors.PBCD;
import org.jkarma.pbcd.events.ChangeDetectedEvent;
import org.jkarma.pbcd.events.ChangeNotDetectedEvent;
import org.jkarma.pbcd.patterns.Patterns;
import org.jkarma.pbcd.similarities.UnweightedJaccard;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.OptionHandlerFilter;

import com.github.habernal.confusionmatrix.ConfusionMatrix;


public class DemoNegFPOF{

	@Option(name="-ms", aliases="--minSupport", usage="Threshold above which a pattern is considered")
	public float minSup = 0.5f;

	@Option(name="-mc", aliases="--minChange", usage="Threshold above which two blocks are different")
	public float minChange = 0.8f;

	@Option(name="-bs", aliases="--blockSize", usage="Number of examples into a single block")
	public int blockSize = 4;

	@Option(name="-ma", aliases="--minAnomaly", usage="Threshold below which an example is anomalous")
	public float minAnomaly = 0.1f;

	@Option(name="-d", aliases="--depth", usage="Depth of research space of patterns")
	public int depth = 3;

	@Option(name="-f", aliases="--file", usage="Fully qualified path and name of file.", required=true)
	private static String fileName;

	public void run() throws IOException {
		//we load the dataset
		Stream<Transazione> dataset =  this.getDataset(fileName);

		//we build a change detector based on frequent closed itemsets.
		PBCD<Transazione,String,TidSet,Boolean> pbcd = this.getPBCD(this.minSup);
		
		//then we build an anomaly detector based on the change detector delegate.
		NegativeFPOFAnomaly<Transazione, String, TidSet> scoring = new NegativeFPOFAnomaly<>(this.minSup);
		PBAD<Transazione, String, TidSet> pbad = new PBAD<>(
			pbcd, scoring, this.minAnomaly
		);
		
		//we collect prediction
		Vector<Boolean> predetti = new Vector<>();
		Vector<Boolean> reali = new Vector<>();
		
		//we listen for anomaly detection events
		pbad.registerListener(new PBADEventListener<Transazione, String, TidSet>(){

			@Override
			public void anomalyDetected(AnomalyDetectedEvent<Transazione, String> event) {
				Transazione t = event.getTransaction();
				if(t.isAnomaly()) {
					System.err.println("t:"+t.getId()+", score:"+event.getAnomalyScore());
				}else {
					System.out.println("t:"+t.getId()+", score:"+event.getAnomalyScore());
				}
				
				predetti.add(true);
				reali.add(t.isAnomaly());
			}

			@Override
			public void anomalyNotDetected(AnomalyNotDetectedEvent<Transazione, String> event) {
				Transazione t = event.getTransaction();
				if(t.isAnomaly()) {
					System.err.println("t:"+t.getId()+", score:"+event.getAnomalyScore());
				}else {
					System.out.println("t:"+t.getId()+", score:"+event.getAnomalyScore());
				}
				
				predetti.add(false);
				reali.add(t.isAnomaly());
			}

			@Override
			public void changeDetected(ChangeDetectedEvent<String, TidSet> event) {
				System.out.println("change detected "+event.getAmount());
			}

			@Override
			public void changeNotDetected(ChangeNotDetectedEvent<String, TidSet> event) {
				System.out.println("change detected "+event.getAmount());
			}
			
		});
		
		//we consume every data point
		dataset.forEach(pbad);
		
		//once finished we compute the confusion matrix
		ConfusionMatrix cm = new ConfusionMatrix();
		for(int i=0; i<predetti.size(); i++) {
			if(predetti.get(i) == false && reali.get(i) == false){
				cm.increaseValue("NON-FRAUD", "NON-FRAUD", 1);
			}else if(predetti.get(i) == true && reali.get(i) == false){
				cm.increaseValue("NON-FRAUD", "FRAUD", 1);
			}else if(predetti.get(i) == false && reali.get(i) == true){
				cm.increaseValue("FRAUD", "NON-FRAUD", 1);
			}else{
				cm.increaseValue("FRAUD", "FRAUD", 1);
			}
		}
		System.out.println(cm);
		System.out.println(cm.getPrecisionForLabels());
		System.out.println(cm.getRecallForLabels());
		System.out.println(cm.getMacroFMeasure());
		System.out.println(cm.getAccuracy());
	}



	private Stream<Transazione> getDataset(String fileCSV) throws IOException{
		List<Transazione> transazioni = new LinkedList<>();
		File file = new File(fileCSV);


		BufferedReader br = new BufferedReader(new FileReader(file));
		String st;
		String values[];
		st = br.readLine();
		while ((st = br.readLine()) != null){
			values = st.split(",");
			transazioni.add(new Transazione(values));
		}
		br.close();

		return transazioni.stream();
	}


	private PBCD<Transazione, String, TidSet, Boolean> getPBCD(float minSup){
		//we prepare the time window model and the data accessor
		WindowedProvider<String, TidSet> accessor = new NegativeTidSetProvider<>(Windows.blockwiseSliding());
		
		//we instantiate the mining strategy
		MiningStrategy<String, TidSet> strategy = Strategies
			.uponItemsets(new HashSet<String>())
			.limitDepth(this.depth).eclat(this.minSup)
			.beam(accessor, new AreaHeuristic<String,TidSet>(), 5);

		//we assemble the PBCD
		return Detectors.upon(strategy)
			.unweighted((p,t) -> Patterns.isFrequent(p,minSup,t), new UnweightedJaccard())
			.describe(Descriptors.partialEps(this.minSup, 1.00))
			.build(this.minChange, this.blockSize);
	}
	
	
	
	
	
	
	
	
	
	
	
	public static void main(String[] args) throws IOException{
		final DemoNegFPOF demo = new DemoNegFPOF();
		final CmdLineParser argsParser = new CmdLineParser(demo);
		try {
			//we parse the command line arguments
			argsParser.parseArgument(args);

			//we run the algorithm
			demo.run();
		} catch (CmdLineException e) {
			System.err.println(e.getMessage());
			System.err.println("java -jar [jar-file] [options...] arguments...");
			argsParser.printUsage(System.err);
			System.err.println();
			System.err.println(" Example: java -jar jKarma-fraud-detection"+
					argsParser.printExample(OptionHandlerFilter.ALL)
					);
		}

	}
}