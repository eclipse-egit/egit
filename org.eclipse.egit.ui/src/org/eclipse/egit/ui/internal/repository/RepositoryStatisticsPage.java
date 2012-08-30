/******************************************************************************
 *  Copyright (c) 2012 SAP AG.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *    Christian Halstrick (SAP AG) - initial implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.io.IOException;
import java.text.MessageFormat;

import org.eclipse.egit.ui.UIText;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jgit.storage.file.GC;
import org.eclipse.jgit.storage.file.GC.RepoStatistics;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * Page exposing statistic data for elements that can adapt to a
 * {@link Repository} object.
 */
public class RepositoryStatisticsPage extends PropertyPage {

	protected Control createContents(Composite parent) {
		Label l = new Label(parent, SWT.NONE);

		Repository repo = (Repository) getElement()
				.getAdapter(Repository.class);
		if (repo == null)
			return l;

		if (repo instanceof FileRepository) {
			GC gc = new GC((FileRepository) repo);
			try {
				RepoStatistics stats = gc.getStatistics();
				StringBuilder b=new StringBuilder();
				b.append(MessageFormat.format(
						UIText.RepositoryStatistics_NrOfLoosePackedObjects,
						Long.valueOf(stats.numberOfLooseObjects),
						Long.valueOf(stats.numberOfPackedObjects))).append('\n');
				b.append(MessageFormat.format(
						UIText.RepositoryStatistics_BytesUsedToPersistLoosePackedObjects,
						describeSize(stats.sizeOfLooseObjects),
						describeSize(stats.sizeOfPackedObjects))).append('\n');
				b.append(MessageFormat.format(
						UIText.RepositoryStatistics_NrOfLoosePackedRefs,
						Long.valueOf(stats.numberOfLooseRefs),
						Long.valueOf(stats.numberOfPackedRefs))).append('\n');
				b.append(MessageFormat.format(
						UIText.RepositoryStatistics_NrOfPackfiles,
						Long.valueOf(stats.numberOfPackFiles)));
				l.setText(b.toString());
			} catch (IOException e) {
				l.setText(e.getMessage());
			}
		}
		return l;
	}

	private String describeSize(long nrOfBytes) {
		if (nrOfBytes<1000L)
			return nrOfBytes+" Bytes"; //$NON-NLS-1$
		if (nrOfBytes<1000000L)
			return (nrOfBytes/1000L)+" kB"; //$NON-NLS-1$
		if (nrOfBytes<1000000000L)
			return (nrOfBytes/1000000L)+" MB"; //$NON-NLS-1$
		return (nrOfBytes/1000000000L)+" GB"; //$NON-NLS-1$
	}
}
