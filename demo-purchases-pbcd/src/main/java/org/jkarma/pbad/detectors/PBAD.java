/*******************************************************************************
 * Copyright 2020 Angelo Impedovo
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
package org.jkarma.pbad.detectors;

import java.util.LinkedList;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.jkarma.mining.interfaces.Lattice;
import org.jkarma.model.Transaction;
import org.jkarma.pbcd.detectors.PBCD;
import org.jkarma.pbcd.events.ChangeDescriptionCompletedEvent;
import org.jkarma.pbcd.events.ChangeDescriptionStartedEvent;
import org.jkarma.pbcd.events.ChangeDetectedEvent;
import org.jkarma.pbcd.events.ChangeNotDetectedEvent;
import org.jkarma.pbcd.events.PBCDEventListener;
import org.jkarma.pbcd.events.PatternUpdateCompletedEvent;
import org.jkarma.pbcd.events.PatternUpdateStartedEvent;
import org.jkarma.pbcd.patterns.Pattern;

import com.google.common.eventbus.EventBus;

public class PBAD<A extends Transaction<B>, B extends Comparable<B>, C> implements Consumer<A>{
	
	/**
	 * The change detector associated to this anomaly detector.
	 */
	private PBCD<A,B,C,?> changeDetector;
	
	/**
	 * The delegate responsible of computing the anomaly scores.
	 */
	private BiFunction<A, Lattice<Pattern<B,C>>, Number> anomalyScore;
	
	/**
	 * The minimum anomaly threshold for detecting anomalies.
	 */
	private double minAnomaly;
	
	/**
	 * An inner event bus for event listening.
	 */
	protected EventBus eventBus;
	
	private int bCount = 0;
	private int tCount = 0;
	private LinkedList<A> bucket;

	
	
	
	/**
	 * Instantiate a Pattern-based Anomaly Detector (PBAD) on top of an
	 * existing Pattern-based Change Detection (PBCD) strategy.
	 * 
	 * @param changeDetector
	 * @param test
	 */
	public PBAD(PBCD<A,B,C,?> changeDetector, BiFunction<A, Lattice<Pattern<B,C>>, Number> anomalyScore, double minAnomaly) {
		if(changeDetector==null || anomalyScore==null) {
			throw new IllegalArgumentException();
		}
		this.eventBus = new EventBus();
		this.bucket = new LinkedList<A>();
		this.anomalyScore = anomalyScore;
		this.changeDetector = changeDetector;
		this.changeDetector.registerListener(
			new PBCDEventListener<B,C>(){
				@Override
				public void changeDescriptionCompleted(ChangeDescriptionCompletedEvent<B, C> arg0) {
					// TODO Auto-generated method stub
				}

				@Override
				public void changeDescriptionStarted(ChangeDescriptionStartedEvent<B, C> arg0) {
					// TODO Auto-generated method stub
				}

				@Override
				public void changeDetected(ChangeDetectedEvent<B, C> arg0) {
					eventBus.post(arg0);
					
					//then we test for anomalies, before jKarma dispose old data
					this.checkAnomalies();
					tCount = changeDetector.getBlockSize();
				}

				@Override
				public void changeNotDetected(ChangeNotDetectedEvent<B, C> arg0) {
					eventBus.post(arg0);
					System.out.println("change not detected "+arg0.getAmount());
					
					//then we test for anomalies, before jKarma dispose old data 
					//(eventually, depending on the time window model)
					this.checkAnomalies();
				}

				@Override
				public void patternUpdateCompleted(PatternUpdateCompletedEvent<B, C> arg0) {
					// TODO Auto-generated method stub
				}

				@Override
				public void patternUpdateStarted(PatternUpdateStartedEvent<B, C> arg0) {
					// TODO Auto-generated method stub
				}
				
				private void checkAnomalies() {
					int blockSize = changeDetector.getBlockSize();
					
					//We test for anomalies on previously cached transactions in the bucket.
					//Then, we clear the bucket.
					if(bucket.size() == blockSize) {
						for(A tSaved : bucket) {
							double score = anomalyScore.apply(tSaved, changeDetector.getLattice()).doubleValue();
							boolean isAnomaly = (score <= minAnomaly);
							if(isAnomaly) {
								//we dispatch an anomaly-detected-event on tSaved transaction
								eventBus.post(new AnomalyDetectedEvent<A,B>(tSaved, score));
							}else {
								//otherwise we dispatch an anomaly-not-detected-event on tSaved transaction
								eventBus.post(new AnomalyNotDetectedEvent<A,B>(tSaved, score));
							}
						}
						bucket.clear();
					}
				}
			}
		);
	}
	

	@Override
	public void accept(A t) {
		int blockSize = this.changeDetector.getBlockSize();
		
		//increase the transaction count.
		this.tCount++;
		this.bCount = ((this.tCount-1) / blockSize)+1;
		//int offset = ((this.tCount-1) % blockSize)+1;
		//System.out.println("id#"+t.getId()+", trans#"+tCount+", blockId#"+bCount+", offset#"+offset);
		
		//We should wait the consumption of at least 2 blocks of transactions.
		//This ensures that jKarma has accumulated at least two evaluations (time windows)
		//for each pattern. we temporally save incoming transactions in a bucket for later use.
		if(bCount>1) {
			bucket.add(t);
		}
		
		//we let the PBCD consume the transaction
		this.changeDetector.accept(t);	
	}
	
	
	/**
	 * Register an event listener to this PBCD.
	 * @param eventListener The EventListener object.
	 */
	public void registerListener(PBADEventListener<A,B,C> eventListener) {
		this.eventBus.register(eventListener);
	}
	
	
	/**
	 * Unregister an event listener previously registered with this PBCD.
	 * @param eventListener The EventListener object.
	 */
	public void unregisterListener(PBADEventListener<A,B,C> eventListener) {
		this.eventBus.unregister(eventListener);
	}
}
