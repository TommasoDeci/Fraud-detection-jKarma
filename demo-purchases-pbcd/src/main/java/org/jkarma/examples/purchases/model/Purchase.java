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

public class Purchase implements Transaction<Product>{
	
	private static int lastTid = 0;
	
	private int tid;
	private Instant timestamp;
	private Set<Product> items;
	
	public Purchase(Product... products) {
		this.tid = Purchase.lastTid;
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
