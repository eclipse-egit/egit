/*******************************************************************************
 * Copyright (C) 2018, Romain WALLON <romain.wallon@orange.fr>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.egit.core.internal.gitmoji;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import com.google.gson.stream.JsonReader;

/**
 * A method object used to easily read the Gitmojis from the JSON remote file.
 */
final class GitmojiJsonReader implements Closeable {

	/**
	 * The key associated to the array of the Gitmojis in the JSON file.
	 */
	private static final String GITMOJIS = "gitmojis"; //$NON-NLS-1$

	/**
	 * The key associated to the emoji of a Gitmoji in the JSON file.
	 */
	private static final String EMOJI = "emoji"; //$NON-NLS-1$

	/**
	 * The key associated to the code of a Gitmoji in the JSON file.
	 */
	private static final String CODE = "code"; //$NON-NLS-1$

	/**
	 * The key associated to the description of a Gitmoji in the JSON file.
	 */
	private static final String DESCRIPTION = "description"; //$NON-NLS-1$

	/**
	 * The input stream to read from.
	 */
	private final InputStream inputStream;

	/**
	 * The reader used to parse the input stream as a JSON stream.
	 */
	private final JsonReader jsonReader;

	/**
	 * Creates a new GitmojiJsonReader.
	 *
	 * @param inputStream
	 *            The input stream to read from.
	 */
	GitmojiJsonReader(InputStream inputStream) {
		this.inputStream = inputStream;
		this.jsonReader = new JsonReader(new InputStreamReader(inputStream));
	}

	/**
	 * Reads the Gitmojis from the associated input stream.
	 *
	 * @return The read Gitmojis.
	 *
	 * @throws IOException
	 *             If an error occurs while reading.
	 */
	public List<Gitmoji> read() throws IOException {
		List<Gitmoji> gitmojis = new LinkedList<>();

		jsonReader.beginObject();
		while (jsonReader.hasNext()) {
			if (GITMOJIS.equals(jsonReader.nextName())) {
				readInto(gitmojis);
			}
		}
		jsonReader.endObject();

		return gitmojis;
	}

	/**
	 * Creates Gitmojis from a JSON array read from the input stream, and stores
	 * them into the given list.
	 *
	 * @param gitmojis
	 *            The list to store the Gitmojis into.
	 *
	 * @throws IOException
	 *             If an error occurs while reading.
	 */
	private void readInto(List<Gitmoji> gitmojis) throws IOException {
		jsonReader.beginArray();
		while (jsonReader.hasNext()) {
			gitmojis.add(readGitmoji());
		}
		jsonReader.endArray();
	}

	/**
	 * Creates a Gitmoji from a JSON object read from the input stream.
	 *
	 * @return The read Gitmoji.
	 *
	 * @throws IOException
	 *             If an error occurs while reading.
	 */
	private Gitmoji readGitmoji() throws IOException {
		String emoji = null;
		String code = null;
		String description = null;

		jsonReader.beginObject();
		while (jsonReader.hasNext()) {
			switch (jsonReader.nextName()) {
			case EMOJI:
				emoji = jsonReader.nextString();
				break;

			case CODE:
				code = jsonReader.nextString();
				break;

			case DESCRIPTION:
				description = jsonReader.nextString();
				break;

			default:
				jsonReader.skipValue();
			}
		}
		jsonReader.endObject();

		return new Gitmoji(emoji, code, description);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		jsonReader.close();
		inputStream.close();
	}

}
