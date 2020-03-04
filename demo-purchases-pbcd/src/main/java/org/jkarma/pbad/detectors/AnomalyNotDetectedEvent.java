package org.jkarma.pbad.detectors;

import org.jkarma.model.Transaction;

public class AnomalyNotDetectedEvent<A extends Transaction<B>, B extends Comparable<B>> extends PBADEvent<A,B> {

	public AnomalyNotDetectedEvent(A transaction, double anomalyScore) {
		super(transaction, anomalyScore);
		// TODO Auto-generated constructor stub
	}

}
