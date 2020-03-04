package org.jkarma.pbad.detectors;

import org.jkarma.model.Transaction;
import org.jkarma.pbcd.events.ChangeDetectedEvent;
import org.jkarma.pbcd.events.ChangeNotDetectedEvent;

import com.google.common.eventbus.Subscribe;

public interface PBADEventListener<A extends Transaction<B>, B extends Comparable<B>,C> {

	/**
	 * Method called when the PBAD pipeline succeeds in detecting an anomaly, 
	 * that is when the anomaly score does not exceed the minimum anomaly threshold.  
	 * @param event An AnomalyDetectedEvent associated to the detection event.
	 */
	@Subscribe
	public void anomalyDetected(AnomalyDetectedEvent<A,B> event);



	/**
	 * Method called when the PBAD pipeline fails in detecting an anomaly, 
	 * that is when the anomaly score exceeds the minimum anomaly threshold.  
	 * @param event An AnomalyNotDetectedEvent associated to the detection event.
	 */
	@Subscribe
	public void anomalyNotDetected(AnomalyNotDetectedEvent<A,B> event);
	
	
	
	/**
	 * Method called when the PBCD associated to the PBAB succeeds in detecting a change,
	 * that is when the change score exceeds the minimum change threshold.
	 * @param event A ChangeDetectedEvent associated to the detection event.
	 */
	@Subscribe
	public void changeDetected(ChangeDetectedEvent<B,C> event);
	
	
	
	/**
	 * Method called when the PBCD associated to the PBAB fails in detecting a change,
	 * that is when the change score does not exceed the minimum change threshold.
	 * @param event A ChangeNotDetectedEvent associated to the detection event.
	 */
	@Subscribe
	public void changeNotDetected(ChangeNotDetectedEvent<B,C> event); 

}
