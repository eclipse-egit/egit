/******************************************************************************
 *  Copyright (c) 2012, 2015 SAP AG and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *    Christian Halstrick (SAP AG) - initial implementation
 *****************************************************************************/
package org.eclipse.egit.ui.internal.repository;

import java.text.NumberFormat;
import java.util.Properties;

import org.eclipse.egit.core.AdapterUtils;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jgit.api.GarbageCollectCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
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

	@Override
	protected Control createContents(Composite parent) {
		noDefaultAndApplyButton();
		Table table = new Table(parent, SWT.MULTI | SWT.BORDER
				| SWT.FULL_SELECTION);
		String[] titles = { UIText.RepositoryStatistics_Description,
				UIText.RepositoryStatistics_LooseObjects,
				UIText.RepositoryStatistics_PackedObjects };
		for (int i = 0; i < titles.length; i++) {
			TableColumn column = new TableColumn(table, SWT.NONE);
			column.setText(titles[i]);
		}

		Repository repo = AdapterUtils.adapt(getElement(), Repository.class);
		if (repo == null) {
			return table;
		}
		try (Git git = new Git(repo)) {
			GarbageCollectCommand gc = git.gc();
			Properties stats = gc.getStatistics();

			table.setLinesVisible(true);
			table.setHeaderVisible(true);
			GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
			data.heightHint = 200;
			table.setLayoutData(data);

			TableItem item = new TableItem(table, SWT.NONE);
			item.setText(0, UIText.RepositoryStatistics_NrOfObjects);
			item.setText(1, getStatsAsString(stats, "numberOfLooseObjects")); //$NON-NLS-1$
			item.setText(2, getStatsAsString(stats, "numberOfPackedObjects")); //$NON-NLS-1$

			item = new TableItem(table, SWT.NONE);
			item.setText(0, UIText.RepositoryStatistics_NrOfPackfiles);
			item.setText(2, getStatsAsString(stats, "numberOfPackFiles")); //$NON-NLS-1$

			item = new TableItem(table, SWT.NONE);
			item.setText(0, UIText.RepositoryStatistics_NrOfRefs);
			item.setText(1, getStatsAsString(stats, "numberOfLooseRefs")); //$NON-NLS-1$
			item.setText(2, getStatsAsString(stats, "numberOfPackedRefs"));//$NON-NLS-1$

			item = new TableItem(table, SWT.NONE);
			item.setText(0, UIText.RepositoryStatistics_SpaceNeededOnFilesystem);
			item.setText(1,
					describeSize(getStatsAsLong(stats, "sizeOfLooseObjects"))); //$NON-NLS-1$
			item.setText(2,
					describeSize(getStatsAsLong(stats, "sizeOfPackedObjects"))); //$NON-NLS-1$

			for (int i = 0; i < titles.length; i++) {
				table.getColumn(i).pack();
			}
			parent.pack();
		} catch (GitAPIException e) {
			Activator.handleError(e.getMessage(), e, false);
		}
		return table;
	}

	private String getStatsAsString(Properties stats, String key) {
		return bigIntFmt.format(firstNonNull(stats.get(key), "")); //$NON-NLS-1$
	}

	private static long getStatsAsLong(Properties stats, String key) {
		Object value = stats.get(key);
		if (value instanceof Number)
			return ((Number) value).longValue();
		return 0L;
	}

	private static <T> T firstNonNull(T first, T second) {
		return first != null ? first : second;
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
