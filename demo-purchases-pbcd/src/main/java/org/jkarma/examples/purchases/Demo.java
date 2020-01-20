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
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.sun.xml.internal.rngom.parse.host.Base;
import org.jkarma.examples.purchases.model.FPOF;
import org.jkarma.examples.purchases.model.Product;
import org.jkarma.examples.purchases.model.Transazione;
import org.jkarma.examples.purchases.model.Utils;
import org.jkarma.mining.joiners.TidSet;
import org.jkarma.mining.providers.BaseProvider;
import org.jkarma.mining.providers.TidSetProvider;
import org.jkarma.mining.structures.MiningStrategy;
import org.jkarma.mining.structures.Pair;
import org.jkarma.mining.structures.Strategies;
import org.jkarma.mining.windows.CumulativeLandmarkStrategy;
import org.jkarma.mining.windows.WindowingStrategy;
import org.jkarma.mining.windows.Windows;
import org.jkarma.model.LabeledEdge;
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
	private static List<Transazione> transazioni;
	private static Map<String,Float> itemCount;
	private static  Map<String,Float> patternSup;
	private static Set<String> keys;
	/**
	 * Returns an in-memory stream of purchases.
	 * @return an in-memory stream of purchases.
	 */
	public static Stream<Transazione> getDataset(String fileCSV)
	{
		transazioni = new LinkedList<>();
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

		itemCount = new HashMap<>();
		Iterator<Transazione> itTrans = transazioni.iterator(); //iteratore su tutte le transazioni
		Iterator<String> itString;
		while(itTrans.hasNext())
		{
			itString = itTrans.next().iterator();  //iteratore sui valori di una singola transazione
			String currentItem;
			while(itString.hasNext())
			{
				currentItem = itString.next();
				if(itemCount.containsKey(currentItem)){  //se l'elemento e' gia' stato letto allora incrementa il suo conteggio di 1
					itemCount.replace(currentItem,itemCount.get(currentItem)+1);
				} else {                                //altrimenti inseriscilo in Map
					itemCount.put(currentItem,1f);
				}
			}
		}

		keys = itemCount.keySet();
		itString = keys.iterator();
		String currentKey;
		while(itString.hasNext())
		{
			currentKey = itString.next();
			itemCount.replace(currentKey,itemCount.get(currentKey)/transazioni.size());     //sostituisco tutti i conteggi con il supporto
		}


//		itTrans = transazioni.iterator();           //iteratore su tutte le transazioni
	//	Transazione currentTrans;
//		Integer currentID;
//		while(itTrans.hasNext())
//		{
//			currentTrans = itTrans.next();
//			itString = currentTrans.iterator();     //iteratore sui valori di una singola transazione
//			currentID = currentTrans.getId();
//			float support = 1;     //inizializzo il supporto della transazione a 1 dato che sara' per forza coperta dall'insieme vuoto
//			while(itString.hasNext())
//			{
//				support += itemCount.get(itString.next());      //il supporto sara' uguale alla somma di tutti i supporti dei valori che la transazione possiede
//			}
//			patternSup.put(currentID.toString(),support);   //inserisco il supporto totale della transazione in una Map
//		}

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
				.uponItemsets(new HashSet<String>()).limitDepth(3).eclat(minFreq).dfs(accessor);
		
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
		int blockSize = 4;
		float minFreq = 0.7f;
		float minChange = 0.5f;
		Supplier<Stream<Transazione>> streamSupplier = () -> Demo.getDataset("10011-319-150-226.csv");
		Stream<Transazione> dataset = streamSupplier.get();
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


		//we consume the dataset
//		dataset.forEach(detector);			//calcola i pattern frequenti
//		patternSup = new HashMap<>();
//		String currentPatt;
//		for(Pattern<String ,TidSet> p : detector.getLattice())
//		{
//			currentPatt = p.toString().split("}")[0].replace("{","");		//pattern corrente, es.: visa,dist2Null
//			patternSup.put(currentPatt,1f);		//inizializzo il supporto del pattern a 1 dato che sara' per forza coperto dall'insieme vuoto
//			Iterator<String> itKeys = keys.iterator();		//iteratore sui possibili valori nel .CSV
//			String currentKey;
//			while(itKeys.hasNext())
//			{
//				currentKey = itKeys.next();		//valore corrente
//				if(currentPatt.contains(currentKey))		//se il pattern contiene quel valore, visa ad esempio
//				{
//					patternSup.replace(currentPatt,patternSup.get(currentPatt) + itemCount.get(currentKey)); 	//il suo supporto viene incrementato in base a quello del singolo valore
//				}
//			}
//		}
//
//		System.out.println(patternSup);

//		dataset = streamSupplier.get();
		dataset.forEach(trans -> {
			System.out.println(trans);
		});



		//	Map<String,Float> supports = new HashMap<>();
//		dataset.forEach(transaction -> {
//			detector.accept(transaction);
//			for(Pattern<String,TidSet> p : detector.getLattice())
//			{
//				try
//				{
//					if (supports.containsKey(p.toString()))
//					{
//						supports.replace(p.toString(), supports.get(p.toString()) +  p.getSecondEval().getAbsoluteFrequency());
//					} else {
//						supports.put(p.toString(), (float) (p.getFirstEval().getAbsoluteFrequency() + p.getSecondEval().getAbsoluteFrequency()));
//					}
//				}
//				catch (NullPointerException e)
//				{
//
//				}
//
//			}
//		});

//		for (Map.Entry<String,Float> entry : supports.entrySet())
//		{
//			supports.replace(entry.getKey(), entry.getValue()/46);
//		}
//
//		dataset = streamSupplier.get();
//		Float max = Collections.max(supports.values());
//		Set<String> keys = supports.keySet();
//		dataset.forEach(transaction -> {
//			Iterator<String> itSup = transaction.iterator();
//			Double transactionSupport = 0d;
//			while(itSup.hasNext())
//			{
//				String currentString = itSup.next();
//				Iterator<String> itKeys = keys.iterator();
//				while(itKeys.hasNext())
//				{
//					String currentKey = itKeys.next();
//					if(currentKey.contains(currentString))
//					{
//						transactionSupport += supports.get(currentKey);
//					}
//				}
//			}
//			System.out.println(transaction.getId() + " has FPOF of: " + transactionSupport/max);
//
//		});

	}

}
