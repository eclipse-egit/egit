/*******************************************************************************
 * Copyright (C) 2018, Romain WALLON <romain.wallon@orange.fr>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.core.internal.gitmoji;

import java.util.Objects;

/**
 * The representation of a Gitmoji, containing all the data needed to use it in
 * a commit message.
 */
public final class Gitmoji {

	/**
	 * The emoji of this Gitmoji.
	 */
	private final String emoji;

	/**
	 * The code of this Gitmoji.
	 */
	private final String code;

	/**
	 * The description of this Gitmoji, describing when to use it.
	 */
	private final String description;

	/**
	 * Creates a new Gitmoji.
	 *
	 * @param emoji
	 *            The emoji of the Gitmoji.
	 * @param code
	 *            The code of the Gitmoji.
	 * @param description
	 *            The description of the Gitmoji, describing when to use it.
	 *
	 * @throws NullPointerException
	 *             If one of the arguments is {@code null}.
	 */
	Gitmoji(String emoji, String code, String description) {
		this.emoji = Objects.requireNonNull(emoji);
		this.code = Objects.requireNonNull(code);
		this.description = Objects.requireNonNull(description);
	}

	/**
	 * Gives the emoji of this Gitmoji.
	 *
	 * @return The emoji of this Gitmoji.
	 */
	public String getEmoji() {
		return emoji;
	}

	/**
	 * Gives the code of this Gitmoji.
	 *
	 * @return The code of this Gitmoji.
	 */
	public String getCode() {
		return code;
	}

	/**
	 * Gives the description of this Gitmoji.
	 *
	 * @return The description of when to use this Gitmoji.
	 */
	public String getDescription() {
		return description;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return emoji + ' ' + description;
	}

}
