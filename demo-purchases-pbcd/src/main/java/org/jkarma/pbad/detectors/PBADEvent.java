package org.jkarma.pbad.detectors;

import org.jkarma.model.Transaction;

public class PBADEvent<A extends Transaction<B>, B extends Comparable<B>> {
	
	/**
	 * The transaction associated with the event.
	 */
	private A transaction;
	
	/**
	 * The anomaly score computed on the transaction associated to the event.
	 */
	private double anomalyScore;
	
	public PBADEvent(A transaction, double anomalyScore) {
		if(transaction==null) {
			throw new IllegalArgumentException();
		}
		this.transaction = transaction;
		this.anomalyScore = anomalyScore;
	}

	public A getTransaction() {
		return transaction;
	}

	public double getAnomalyScore() {
		return anomalyScore;
	}
}
