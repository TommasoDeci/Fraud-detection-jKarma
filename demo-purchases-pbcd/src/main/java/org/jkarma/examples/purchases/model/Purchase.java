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
package org.jkarma.examples.purchases.model;

import java.time.Instant;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.jkarma.model.Transaction;

import com.google.common.collect.Sets;


/**
 * Class defining a purchase as a transaction of products.
 * The purchase follows the transactional notation. Therefore
 * it is uniquely identified by a transaction id (tid), it is
 * registered at a given timestamp and contains a set of products
 * purchased together.
 * @author Angelo Impedovo
 */
public class Purchase implements Transaction<Product>{
	
	/**
	 * Private counter of transaction ids.
	 */
	private static int lastTid = 0;
	
	/**
	 * Returns a previously unused transaction id.
	 * @return
	 */
	public static int getNextTid() {
		int value = Purchase.lastTid;
		Purchase.lastTid++;
		return value;
	}
	
	
	/**
	 * The transaction id of the purchase.
	 */
	private int tid;
	
	
	/**
	 * The timestamp of the purchase.
	 */
	private Instant timestamp;
	
	
	/**
	 * The set of products associated with the purchase.
	 */
	private Set<Product> items;
	
	
	/**
	 * Constructs a purchase of different products.
	 * @param products The products that have been purchased together.
	 */
	public Purchase(Product... products) {
		this.tid = Purchase.getNextTid();
		this.timestamp = Instant.now();
		this.items = Sets.newHashSet(products);
	}

	public int getId() {
		return this.tid;
	}

	public Collection<Product> getItems() {
		return this.items;
	}

	public Instant getTimestamp() {
		return this.timestamp;
	}
	
	public Iterator<Product> iterator() {
		return this.items.iterator();
	}

}
