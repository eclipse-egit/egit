/*******************************************************************************
 * Copyright (c) 2012 Kamil Sobon <kam.sobon@gmail.com>.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.ui.internal.push;

import java.util.Collection;

/**
 * Interface that provides information about users from some source.
 */
public interface IPersonProvider {

	/**
	 * Class that represent person.
	 */
	public class Person {
		/** Person login */
		private final String login;

		/** Person full name */
		private final String name;

		/**
		 * Constructor.
		 *
		 * @param login
		 *            person login, may not be <code>null</code>
		 * @param name
		 *            person full name
		 */
		public Person(String login, String name) {
			if (login == null) {
				throw new IllegalArgumentException("Login cannot be null"); //$NON-NLS-1$
			}

			this.login = login;
			this.name = name;
		}

		/**
		 * Returns person login.
		 *
		 * @return person login
		 */
		public String getLogin() {
			return login;
		}

		/**
		 * Returns person full name
		 *
		 * @return person full name
		 */
		public String getName() {
			return name;
		}

	}

	/**
	 * Returns collection of {@link Person} instances that keeps users'
	 * information.
	 *
	 * @return collection of {@link Person} instances that keeps users'
	 *         information
	 */
	Collection<Person> getPeople();
}
