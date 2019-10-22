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

import java.util.stream.Stream;

import org.jkarma.examples.purchases.model.Product;
import org.jkarma.examples.purchases.model.Purchase;
import org.jkarma.mining.joiners.TidSet;
import org.jkarma.mining.providers.TidSetProvider;
import org.jkarma.mining.structures.MiningStrategy;
import org.jkarma.mining.structures.Strategies;
import org.jkarma.mining.windows.WindowingStrategy;
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
import org.jkarma.pbcd.patterns.Patterns;
import org.jkarma.pbcd.similarities.UnweightedJaccard;

/**
 * An application showing how to define a PBCD an how to run it 
 * over a Stream<Purchase> instance. In this demo the stream is
 * created in-memory by hand.
 * @author Angelo Impedovo
 */
public class Demo {

	/**
	 * Returns an in-memory stream of purchases.
	 * @return an in-memory stream of purchases.
	 */
	public static Stream<Purchase> getDataset(){
		return Stream.of(
			new Purchase(Product.SUGAR, Product.WINE, Product.BREAD),
			new Purchase(Product.WINE, Product.BREAD),
			new Purchase(Product.CAKE, Product.BREAD),
			new Purchase(Product.CAKE, Product.WINE),
			new Purchase(Product.CAKE, Product.WINE, Product.BREAD),
			new Purchase(Product.CAKE, Product.SUGAR, Product.WINE),
			new Purchase(Product.WINE, Product.SUGAR),
			new Purchase(Product.WINE, Product.CAKE),
			new Purchase(Product.CAKE, Product.JUICE,Product.BREAD),
			new Purchase(Product.CAKE, Product.JUICE,Product.BREAD),
			new Purchase(Product.JUICE, Product.BREAD),
			new Purchase(Product.JUICE, Product.SUGAR),
			new Purchase(Product.JUICE, Product.SUGAR, Product.CAKE),
			new Purchase(Product.CAKE, Product.BREAD),
			new Purchase(Product.JUICE,Product.CAKE, Product.BREAD),
			new Purchase(Product.JUICE,Product.BREAD, Product.SUGAR)
		);
	}

	
	
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
	public static PBCD<Purchase, Product, TidSet, Boolean> getPBCD(float minFreq, float minChange, int blockSize){
		//we prepare the time window model and the data accessor
		WindowingStrategy<TidSet> model = Windows.blockwiseSliding();
		TidSetProvider<Product> accessor = new TidSetProvider<>(model);

		//we instantiate the pattern language delegate
		MixedProductJoiner language = new MixedProductJoiner();

		//we instantiate the mining strategy
		MiningStrategy<Product, TidSet> strategy = Strategies
			.upon(language).eclat(minFreq).dfs(accessor);

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
		float minFreq = 0.2f;
		float minChange = 0.5f;
		Stream<Purchase> dataset = Demo.getDataset();
		PBCD<Purchase,Product,TidSet,Boolean> detector = Demo.getPBCD(minFreq, minChange, blockSize);

		//we listen for events
		detector.registerListener(
			new PBCDEventListener<Product,TidSet>() {
			
				@Override
				public void patternUpdateCompleted(PatternUpdateCompletedEvent<Product, TidSet> arg0) {
					//do nothing	
				}

				@Override
				public void patternUpdateStarted(PatternUpdateStartedEvent<Product, TidSet> arg0) {
					//do nothing			
				}

				@Override
				public void changeDetected(ChangeDetectedEvent<Product, TidSet> event) {
					//we show the change score
					System.out.println("change detected: "+event.getAmount());
					System.out.println("\tdescribed by:");
					
					//and the associated explanation
					event.getDescription().forEach(p -> {
						double freqReference = p.getFirstEval().getRelativeFrequency()*100;
						double freqTarget = p.getSecondEval().getRelativeFrequency()*100;

						String message;
						if(freqTarget > freqReference) {
							message="increased frequency from ";
						}else {
							message="decreased frequency from ";
						}
						message+=Double.toString(freqReference)+"% to "+Double.toString(freqTarget)+"%";
						System.out.println("\t\t"+p.getItemSet()+" "+message);
					});
				}

				@Override
				public void changeNotDetected(ChangeNotDetectedEvent<Product, TidSet> arg0) {
					//we show the change score only
					System.out.println("change not detected: "+arg0.getAmount());
				}

				@Override
				public void changeDescriptionCompleted(ChangeDescriptionCompletedEvent<Product, TidSet> arg0) {
					//do nothing
				}

				@Override
				public void changeDescriptionStarted(ChangeDescriptionStartedEvent<Product, TidSet> arg0) {
					//do nothing			
				}  

			}
		);

		//we consume the dataset
		dataset.forEach(detector);
	}

}
