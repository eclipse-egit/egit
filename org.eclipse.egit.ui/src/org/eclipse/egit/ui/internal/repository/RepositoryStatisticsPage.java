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
import java.text.NumberFormat;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.internal.storage.file.GC;
import org.eclipse.jgit.internal.storage.file.GC.RepoStatistics;
import org.eclipse.jgit.util.SystemReader;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.dialogs.PropertyPage;

/**
 * Page exposing statistic data for elements that can adapt to a
 * {@link Repository} object.
 */
public class RepositoryStatisticsPage extends PropertyPage {
	private final NumberFormat bigFpFmt;

	private final NumberFormat bigIntFmt;

	/**
	 * Creates a new statistics page
	 */
	public RepositoryStatisticsPage() {
		bigFpFmt = NumberFormat.getInstance(SystemReader.getInstance()
				.getLocale());
		bigFpFmt.setMaximumFractionDigits(2);
		bigIntFmt = NumberFormat.getInstance(SystemReader.getInstance()
				.getLocale());
	}

	protected Control createContents(Composite parent) {
		Table table = new Table(parent, SWT.MULTI | SWT.BORDER
				| SWT.FULL_SELECTION);
		String[] titles = { UIText.RepositoryStatistics_Description,
				UIText.RepositoryStatistics_LooseObjects,
				UIText.RepositoryStatistics_PackedObjects };
		for (int i = 0; i < titles.length; i++) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(titles[i]);
		}

		Repository repo = (Repository) getElement()
				.getAdapter(Repository.class);
		if (repo == null)
			return table;
		if (repo instanceof FileRepository) {
			GC gc = new GC((FileRepository) repo);
			try {
				RepoStatistics stats = gc.getStatistics();

				table.setLinesVisible(true);
				table.setHeaderVisible(true);
				GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
				data.heightHint = 200;
				table.setLayoutData(data);

				TableItem item = new TableItem(table, SWT.NONE);
				item.setText(0, UIText.RepositoryStatistics_NrOfObjects);
				item.setText(1, bigIntFmt.format(stats.numberOfLooseObjects));
				item.setText(2, bigIntFmt.format(stats.numberOfPackedObjects));

				item = new TableItem(table, SWT.NONE);
				item.setText(0, UIText.RepositoryStatistics_NrOfPackfiles);
				item.setText(2, bigIntFmt.format(stats.numberOfPackFiles)
						.toString());

				item = new TableItem(table, SWT.NONE);
				item.setText(0, UIText.RepositoryStatistics_NrOfRefs);
				item.setText(1, bigIntFmt.format(stats.numberOfLooseRefs)
						.toString());
				item.setText(2, bigIntFmt.format(stats.numberOfPackedRefs)
						.toString());

				item = new TableItem(table, SWT.NONE);
				item.setText(0,
						UIText.RepositoryStatistics_SpaceNeededOnFilesystem);
				item.setText(1, describeSize(stats.sizeOfLooseObjects));
				item.setText(2, describeSize(stats.sizeOfPackedObjects));

				for (int i = 0; i < titles.length; i++) {
					table.getColumn(i).pack();
				}
				parent.pack();
			} catch (IOException e) {
			}
		}
		return table;
	}

	private String describeSize(long nrOfBytes) {
		if (nrOfBytes < 1000L)
			return bigIntFmt.format(nrOfBytes) + " Bytes"; //$NON-NLS-1$
		if (nrOfBytes < 1000000L)
			return bigFpFmt.format(nrOfBytes / 1000.0)
					+ " kB (" + bigIntFmt.format(nrOfBytes) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		if (nrOfBytes < 1000000000L)
			return bigFpFmt.format(nrOfBytes / 1000000.0)
					+ " MB (" + bigIntFmt.format(nrOfBytes) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		return bigFpFmt.format(nrOfBytes / 1000000000.0)
				+ " GB (" + bigIntFmt.format(nrOfBytes) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
