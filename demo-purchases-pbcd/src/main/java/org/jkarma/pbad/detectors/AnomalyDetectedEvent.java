package org.jkarma.pbad.detectors;

import org.jkarma.model.Transaction;

public class AnomalyDetectedEvent<A extends Transaction<B>, B extends Comparable<B>> extends PBADEvent<A,B> {

	public AnomalyDetectedEvent(A transaction, double anomalyScore) {
		super(transaction, anomalyScore);
		// TODO Auto-generated constructor stub
	}

}
