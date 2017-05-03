/*******************************************************************************
 * Copyright (C) 2017, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.fetch;

import org.eclipse.jgit.events.RefsChangedEvent;

/**
 * Special {@link RefsChangedEvent} fired by EGit when "Fetch from Gerrit..." is
 * executed with "Update FETCH_HEAD only". JGit does not fire an event for
 * updates of FETCH_HEAD.
 */
public class FetchHeadChangedEvent extends RefsChangedEvent {

	// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=437362
}
