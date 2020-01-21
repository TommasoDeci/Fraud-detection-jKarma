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

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.jkarma.examples.purchases.model.Transazione;
import org.jkarma.mining.interfaces.ItemSet;
import org.jkarma.mining.joiners.TidSet;
import org.jkarma.mining.providers.TidSetProvider;
import org.jkarma.mining.structures.MiningStrategy;
import org.jkarma.mining.structures.Pair;
import org.jkarma.mining.structures.Strategies;
import org.jkarma.mining.windows.Windows;
import org.jkarma.pbcd.descriptors.Descriptors;
import org.jkarma.pbcd.detectors.Detectors;
import org.jkarma.pbcd.detectors.PBCD;
import org.jkarma.pbcd.events.ChangeDescriptionCompletedEvent;
import org.jkarma.pbcd.events.ChangeDescriptionStartedEvent;
import org.jkarma.pbcd.events.ChangeDetectedEvent;
import org.jkarma.pbcd.events.ChangeNotDetectedEvent;
import org.jkarma.pbcd.events.PBCDEventListener;
import org.jkarma.pbcd.events.PatternUpdateCompletedEvent;
import org.jkarma.pbcd.events.PatternUpdateStartedEvent;
import org.jkarma.pbcd.patterns.Pattern;
import org.jkarma.pbcd.patterns.Patterns;
import org.jkarma.pbcd.similarities.UnweightedJaccard;


/**
 * An application showing how to define a PBCD an how to run it 
 * over a Stream<Purchase> instance. In this demo the stream is
 * created in-memory by hand.
 * @author Angelo Impedovo
 */
public class Demo
{
	/**
	 * Returns an in-memory stream of purchases.
	 * @return an in-memory stream of purchases.
	 */
	public static Stream<Transazione> getDataset(String fileCSV)
	{
		List<Transazione> transazioni = new LinkedList<>();
		File file = new File(fileCSV);

		try
		{
			BufferedReader br = new BufferedReader(new FileReader(file));
			String st;
			String values[];
			st = br.readLine();
			while ((st = br.readLine()) != null)
			{
				values = st.split(",");
				transazioni.add(new Transazione(values));
			}

		}
		catch(IOException e)
		{
			System.err.println("FIle not found!");
		}

		return transazioni.stream();
	}

//	public static Stream<Transazione> getData(String pathName) throws FileNotFoundException {
//		return Utils.parseStream(new File(pathName), Transazione.class)
//				.sorted(Comparator.comparing(Transazione::getTimestamp));
//	}

	/**
	 * Builds a PBCD based on frequent combinations of foods and drinks
	 * in the blockwise sliding model. This PBCD computes the change score
	 * by means of a binary jaccard score, and explain changes by extracting
	 * the emerging patterns.
	 * @param minFreq The minimum frequency threshold.
	 * @param minChange The minimum change threshold.
	 * @param blockSize The number of transactions to be consumed together.
	 * @return the PBCD delegate.
	 */
	public static PBCD<Transazione, String, TidSet, Boolean> getPBCD(float minFreq, float minChange, int blockSize){

		//we prepare the time window model and the data accessor
		TidSetProvider<String> accessor = new TidSetProvider<>(Windows.cumulativeSliding());

		//we instantiate the mining strategy
		MiningStrategy<String, TidSet> strategy = Strategies
				.uponItemsets(new HashSet<String>()).limitDepth(19).eclat(minFreq).dfs(accessor);

		//we assemble the PBCD
		return Detectors.upon(strategy)
				.unweighted((p,t) -> Patterns.isFrequent(p, minFreq, t), new UnweightedJaccard())
				.describe(Descriptors.partialEps(minFreq, 1.00))
				.build(minChange, blockSize);
	}

	/**
	 * Entry point of the application.
	 * @param args
	 */
	public static void main(String[] args){
		int blockSize = 1;
		float minFreq = 0.8f;
		float minChange = 0.98f;
		Stream<Transazione> dataset =  Demo.getDataset("10011-319-150-226.csv");
		PBCD<Transazione,String,TidSet,Boolean> detector = Demo.getPBCD(minFreq, minChange, blockSize);

		//we listen for events
		detector.registerListener(
				new PBCDEventListener<String,TidSet>() {

					@Override
					public void patternUpdateCompleted(PatternUpdateCompletedEvent<String, TidSet> arg0) {
						//do nofing;
					}

					@Override
					public void patternUpdateStarted(PatternUpdateStartedEvent<String, TidSet> arg0) {
						//do nothing
					}

					@Override
					public void changeDetected(ChangeDetectedEvent<String, TidSet> event) {
						//we show the change score
						System.out.println("change detected: "+event.getAmount());
//						System.out.println("\tdescribed by:");
//
//						//and the associated explanation
//						event.getDescription().forEach(p -> {
//							double freqReference = p.getFirstEval().getRelativeFrequency()*100;
//							double freqTarget = p.getSecondEval().getRelativeFrequency()*100;
//
//							String message;
//							if(freqTarget > freqReference) {
//								message="increased frequency from ";
//							}else {
//								message="decreased frequency from ";
//							}
//							message+=Double.toString(freqReference)+"% to "+Double.toString(freqTarget)+"%";
//							System.out.println("\t\t"+p.getItemSet()+" "+message);
//						});
					}

					@Override
					public void changeNotDetected(ChangeNotDetectedEvent<String, TidSet> arg0) {
						//we show the change score only
						System.out.println("change not detected: "+arg0.getAmount());
					}

					@Override
					public void changeDescriptionCompleted(ChangeDescriptionCompletedEvent<String, TidSet> arg0) {
						//do nothing
					}

					@Override
					public void changeDescriptionStarted(ChangeDescriptionStartedEvent<String, TidSet> arg0) {
						//do nothing
					}

				}
		);

		dataset.forEach(transaction -> {
			detector.accept(transaction);
			float num = 0;
			float den = 0;
			for(Pattern<String,TidSet> p : detector.getLattice())
			{
				Set<String> valori = new HashSet<>();
				addValue(valori,p.getItemSet());		//funzione ricorsiva che aggiunge al set tutti gli elementi del pattern
				if(transaction.getItems().containsAll(valori))
				{
					try {
						num += p.getFirstEval().getAbsoluteFrequency();		//incremento il numeratore col supporto del relativo pattern che la copre
					}catch(NullPointerException e){

					}
				}

				try {
					den += p.getFirstEval().getAbsoluteFrequency();
				}catch(NullPointerException e){

				}
			}

			if(den == 0){
				System.out.println("TransactionID: " + transaction.getId() + " has FPOF of: 0");
			}else{
				System.out.println("TransactionID: " + transaction.getId() + " has FPOF of: " + num/den);
			}
		});

	}

	private static void addValue(Set<String> set, ItemSet<String,Pair<TidSet>> itemSet)
	{
		if(itemSet.getPrefix() != null) {
			set.add(itemSet.getSuffix());
			addValue(set,itemSet.getPrefix());		//passo ricorsivo
		}
	}

}
