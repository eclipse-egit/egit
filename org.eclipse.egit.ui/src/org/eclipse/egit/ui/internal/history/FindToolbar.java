/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2013, Robin Stocker <robin@nibor.org>
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 * Copyright (C) 2019, Simon Muschel <smuschel@gmx.de>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevFlag;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/**
 * A toolbar for the history page.
 *
 * @see FindToolbarJob
 * @see FindResults
 * @see GitHistoryPage
 */
public class FindToolbar extends Composite {

	/**
	 * Interface to receive status messages from the {@link FindToolbar}. The
	 * toolbar produces messages indicating a search result overflow, or the
	 * number of hits, or, when navigation among search results occurs, which
	 * entry is the current one.
	 */
	public interface StatusListener {

		/**
		 * Invoked whenever the {@link FindToolbar} produces a new message. The
		 * message may be empty.
		 *
		 * @param originator
		 *            of the message
		 * @param text
		 *            of the message
		 */
		public void setMessage(FindToolbar originator, String text);
	}

	private static final String CCS_CLASS_KEY = "org.eclipse.e4.ui.css.CssClassName"; //$NON-NLS-1$

	private static final String NO_RESULTS_CLASS = "org-eclipse-egit-ui-FindToolbar-noResults"; //$NON-NLS-1$
	/**
	 * Preference value for searching all the fields
	 */
	public static final int PREFS_FINDIN_ALL = 0;

	private static final int PREFS_FINDIN_COMMENTS = 1;

	private static final int PREFS_FINDIN_AUTHOR = 2;

	private static final int PREFS_FINDIN_COMMITID = 3;

	private static final int PREFS_FINDIN_COMMITTER = 4;

	private static final int PREFS_FINDIN_REFERENCE = 5;

	/**
	 * The results (matches) of the current find operation.
	 */
	private final FindResults findResults;

	private IFindListener listener;

	private IPersistentPreferenceStore store = (IPersistentPreferenceStore) Activator.getDefault().getPreferenceStore();

	private List<Listener> eventList = new ArrayList<>();

	private Table historyTable;

	private SWTCommit[] fileRevisions;

	private Text patternField;

	private ModifyListener patternModifyListener;

	private Action findNextAction;

	private Action findPreviousAction;

	private String lastSearchPattern;

	private String lastErrorPattern;

	private ToolItem prefsDropDown;

	private Menu prefsMenu;

	private MenuItem caseItem;

	private MenuItem allItem;

	private MenuItem commitIdItem;

	private MenuItem commentsItem;

	private MenuItem authorItem;

	private MenuItem committerItem;

	private MenuItem referenceItem;

	private Image allIcon;

	private Image commitIdIcon;

	private Image commentsIcon;

	private Image authorIcon;

	private Image committerIcon;

	private Image branchesIcon;

	private FindToolbarJob job;

	private int currentPosition = -1;

	/**
	 * Id of a commit that shall be moved to initially if it is part of the
	 * search results. If not set, or there is no such commit in the search
	 * results, the first search result will be revealed.
	 */
	private ObjectId preselect;

	private CopyOnWriteArrayList<StatusListener> layoutListeners = new CopyOnWriteArrayList<>();

	/** Whether the text field has the "no results" background. */
	private boolean noResults = false;

	/**
	 * Creates the toolbar.
	 *
	 * @param parent
	 *            the parent widget
	 */
	public FindToolbar(Composite parent) {
		super(parent, SWT.NULL);
		findResults = new FindResults();
		listener = createFindListener();
		findResults.addFindListener(listener);
		setBackground(null);
		createToolbar();
	}

	@SuppressWarnings("unused")
	private void createToolbar() {
		ResourceManager resourceManager = Activator.getDefault()
				.getResourceManager();
		allIcon = UIIcons.getImage(resourceManager, UIIcons.SEARCH_COMMIT);
		commitIdIcon = UIIcons.getImage(resourceManager,
				UIIcons.ELCL16_ID);
		commentsIcon = UIIcons.getImage(resourceManager,
				UIIcons.ELCL16_COMMENTS);
		authorIcon = UIIcons.getImage(resourceManager, UIIcons.ELCL16_AUTHOR);
		committerIcon = UIIcons.getImage(resourceManager,
				UIIcons.ELCL16_COMMITTER);
		branchesIcon = UIIcons.getImage(resourceManager, UIIcons.BRANCHES);
		GridLayout findLayout = new GridLayout();
		findLayout.marginHeight = 0;
		findLayout.marginBottom = 1;
		findLayout.marginWidth = 0;
		findLayout.numColumns = 2;
		setLayout(findLayout);
		setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));

		patternField = new Text(this,
				SWT.SEARCH | SWT.ICON_CANCEL | SWT.ICON_SEARCH);
		GridData findTextData = new GridData(SWT.FILL, SWT.TOP, true,
				false);
		findTextData.minimumWidth = 150;
		patternField.setLayoutData(findTextData);
		patternField.setMessage(UIText.HistoryPage_findbar_find_msg);
		patternField.setTextLimit(100);
		// Do _not_ set the font to JFaceResources.getDialogFont() here. It'll
		// scale if changed, but the Text field may not adjust.
		ToolBarManager manager = new ToolBarManager(SWT.HORIZONTAL);
		findNextAction = new Action() {
			@Override
			public void run() {
				findNext();
			}
		};
		findNextAction.setImageDescriptor(UIIcons.ELCL16_NEXT);
		findNextAction.setText(UIText.HistoryPage_findbar_next);
		findNextAction.setToolTipText(UIText.FindToolbar_NextTooltip);
		findNextAction.setEnabled(false);
		manager.add(findNextAction);
		findPreviousAction = new Action() {
			@Override
			public void run() {
				findPrevious();
			}
		};
		findPreviousAction.setImageDescriptor(UIIcons.ELCL16_PREVIOUS);
		findPreviousAction.setText(UIText.HistoryPage_findbar_previous);
		findPreviousAction.setToolTipText(UIText.FindToolbar_PreviousTooltip);
		findPreviousAction.setEnabled(false);
		manager.add(findPreviousAction);
		final ToolBar toolBar = manager.createControl(this);
		toolBar.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

		prefsDropDown = new ToolItem(toolBar, SWT.DROP_DOWN);
		prefsMenu = new Menu(getShell(), SWT.POP_UP);
		caseItem = new MenuItem(prefsMenu, SWT.CHECK);
		caseItem.setText(UIText.HistoryPage_findbar_ignorecase);
		new MenuItem(prefsMenu, SWT.SEPARATOR);
		allItem = createFindInMenuItem();
		allItem.setText(UIText.HistoryPage_findbar_all);
		allItem.setImage(allIcon);
		commentsItem = createFindInMenuItem();
		commentsItem.setText(UIText.HistoryPage_findbar_comments);
		commentsItem.setImage(commentsIcon);
		authorItem = createFindInMenuItem();
		authorItem.setText(UIText.HistoryPage_findbar_author);
		authorItem.setImage(authorIcon);
		commitIdItem = createFindInMenuItem();
		commitIdItem.setText(UIText.HistoryPage_findbar_commit);
		commitIdItem.setImage(commitIdIcon);
		committerItem = createFindInMenuItem();
		committerItem.setText(UIText.HistoryPage_findbar_committer);
		committerItem.setImage(committerIcon);
		referenceItem = createFindInMenuItem();
		referenceItem.setText(UIText.HistoryPage_findbar_reference);
		referenceItem.setImage(branchesIcon);

		prefsDropDown.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (event.detail == SWT.ARROW) {
					// Arrow clicked, show drop down menu
					Rectangle itemBounds = prefsDropDown.getBounds();
					Point point = toolBar.toDisplay(itemBounds.x, itemBounds.y
							+ itemBounds.height);
					prefsMenu.setLocation(point);
					prefsMenu.setVisible(true);
				} else {
					// Button clicked, cycle to next option
					if (allItem.getSelection())
						selectFindInItem(commentsItem);
					else if (commentsItem.getSelection())
						selectFindInItem(authorItem);
					else if (authorItem.getSelection())
						selectFindInItem(commitIdItem);
					else if (commitIdItem.getSelection())
						selectFindInItem(committerItem);
					else if (committerItem.getSelection())
						selectFindInItem(referenceItem);
					else if (referenceItem.getSelection())
						selectFindInItem(allItem);
				}
			}
		});

		patternModifyListener = new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				String pattern = getSearchPattern();
				if (pattern.equals(lastSearchPattern)) {
					// Don't bother if it's still the same.
					return;
				}
				if (pattern.isEmpty()) {
					setNormalBackgroundColor();
				}
				final FindToolbarJob finder = createFinder();
				finder.setUser(false);
				finder.schedule(200);
			}
		};

		patternField.addModifyListener(patternModifyListener);

		patternField.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				if (e.detail != SWT.ICON_CANCEL
						&& !getSearchPattern().isEmpty()) {
					// ENTER or the search icon clicked
					final FindToolbarJob finder = createFinder();
					finder.setUser(false);
					finder.schedule();
				}
			}
		});

		patternField.addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_DOWN) {
					findNext();
				} else if (e.keyCode == SWT.ARROW_UP) {
					findPrevious();
				}
			}
		});

		caseItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				store.setValue(UIPreferences.FINDTOOLBAR_IGNORE_CASE,
						caseItem.getSelection());
				if (store.needsSaving()){
					try {
						store.save();
					} catch (IOException e1) {
						Activator.handleError(e1.getMessage(), e1, false);
					}
				}
				clear();
			}
		});
		caseItem.setSelection(store
				.getBoolean(UIPreferences.FINDTOOLBAR_IGNORE_CASE));

		int selectedPrefsItem = store.getInt(UIPreferences.FINDTOOLBAR_FIND_IN);
		if (selectedPrefsItem == PREFS_FINDIN_ALL)
			selectFindInItem(allItem);
		else if (selectedPrefsItem == PREFS_FINDIN_COMMENTS)
			selectFindInItem(commentsItem);
		else if (selectedPrefsItem == PREFS_FINDIN_AUTHOR)
			selectFindInItem(authorItem);
		else if (selectedPrefsItem == PREFS_FINDIN_COMMITID)
			selectFindInItem(commitIdItem);
		else if (selectedPrefsItem == PREFS_FINDIN_COMMITTER)
			selectFindInItem(committerItem);
		else if (selectedPrefsItem == PREFS_FINDIN_REFERENCE)
			selectFindInItem(referenceItem);

		registerDisposal();
	}

	private void registerDisposal() {
		addDisposeListener(new DisposeListener() {

			@Override
			public void widgetDisposed(DisposeEvent e) {
				findResults.removeFindListener(listener);
				findResults.clear();
				listener = null;
				if (job != null) {
					job.cancel();
					job = null;
				}
				prefsMenu.dispose();
				if (historyTable != null && !historyTable.isDisposed()) {
					historyTable.clearAll();
				}
			}
		});
	}

	private void setNotFoundBackgroundColor() {
		patternField.setData(CCS_CLASS_KEY, NO_RESULTS_CLASS);
		patternField.reskin(SWT.ALL);
		noResults = true;
	}

	private void setNormalBackgroundColor() {
		if (noResults) {
			Color currentColor = patternField.getBackground();
			patternField.setData(CCS_CLASS_KEY, null);
			patternField.reskin(SWT.ALL);
			if (currentColor.equals(patternField.getBackground())) {
				// If the theme has no definition for the text field's
				// background, it remains unchanged. Reset it to the SWT default
				// in that case.
				patternField.setBackground(null);
			}
			noResults = false;
		}
	}

	/**
	 * Defines the commit to be set initially. If {@code null} or the search
	 * results do not contain such a commit, the first search result will be
	 * revealed.
	 *
	 * @param commitId
	 *            to reveal
	 */
	public void setPreselect(ObjectId commitId) {
		preselect = commitId;
	}

	@Override
	public boolean setFocus() {
		return patternField.setFocus();
	}

	/**
	 * Sets the text of the widget's search text field, and optionally triggers
	 * a search.
	 *
	 * @param text
	 *            to set
	 * @param search
	 *            if {@code true}, triggers a search after having set the text
	 */
	public void setText(String text, boolean search) {
		if (!search) {
			patternField.removeModifyListener(patternModifyListener);
		}
		patternField.setText(text);
		if (!search) {
			patternField.addModifyListener(patternModifyListener);
		}
	}

	/**
	 * Sets the text of the widget's search text field without triggering a
	 * search.
	 *
	 * @param text
	 *            to set
	 */
	public void setText(String text) {
		setText(text, false);
	}

	/**
	 * Retrieves the current text in the widget's search text field.
	 *
	 * @return the text
	 */
	public String getText() {
		return patternField.getText();
	}

	private String getSearchPattern() {
		return getText().trim();
	}

	@Override
	public void addListener(int evtType, Listener mouseListener) {
		patternField.addListener(evtType, mouseListener);
	}

	@Override
	public void removeListener(int evtType, Listener mouseListener) {
		patternField.removeListener(evtType, mouseListener);
	}

	@Override
	public void addKeyListener(KeyListener keyListener) {
		patternField.addKeyListener(keyListener);
	}

	@Override
	public void removeKeyListener(KeyListener keyListener) {
		patternField.removeKeyListener(keyListener);
	}

	/**
	 * Adds the given listener to the widget, if it hasn't been added yet.
	 *
	 * @param layoutListener
	 *            to add
	 */
	public void addStatusListener(StatusListener layoutListener) {
		layoutListeners.addIfAbsent(layoutListener);
	}

	/**
	 * Removes the given listener if it had been added.
	 *
	 * @param layoutListener
	 *            to remove
	 */
	public void removeStatusListener(StatusListener layoutListener) {
		layoutListeners.remove(layoutListener);
	}

	private void notifyStatus(String text) {
		for (StatusListener l : layoutListeners) {
			l.setMessage(this, text);
		}
	}

	private void findNext() {
		find(true);
	}

	private void findPrevious() {
		find(false);
	}

	private void find(boolean next) {
		if (!getSearchPattern().isEmpty() && findResults.size() == 0) {
			// If the toolbar was cleared and has a pattern typed,
			// then we redo the find with the new table data.
			final FindToolbarJob finder = createFinder();
			finder.setUser(false);
			finder.schedule();
			patternField.setSelection(0, 0);
		} else {
			int currentIx = historyTable.getSelectionIndex();
			int newIx = -1;
			if (next) {
				newIx = findResults.getIndexAfter(currentIx);
				if (newIx == -1) {
					newIx = findResults.getFirstIndex();
				}
			} else {
				newIx = findResults.getIndexBefore(currentIx);
				if (newIx == -1) {
					newIx = findResults.getLastIndex();
				}
			}
			notifyListeners(newIx);

			String current = null;
			currentPosition = findResults.getMatchNumberFor(newIx);
			if (currentPosition == -1) {
				current = "-"; //$NON-NLS-1$
			} else {
				current = String.valueOf(currentPosition);
			}
			notifyStatus(current + '/' + findResults.size());
		}
	}

	private MenuItem createFindInMenuItem() {
		final MenuItem menuItem = new MenuItem(prefsMenu, SWT.RADIO);
		menuItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectFindInItem(menuItem);
			}
		});
		return menuItem;
	}

	private void selectFindInItem(final MenuItem menuItem) {
		if (menuItem == allItem)
			selectFindInItem(menuItem, PREFS_FINDIN_ALL, allIcon,
					UIText.HistoryPage_findbar_changeto_comments);
		else if (menuItem == commentsItem)
			selectFindInItem(menuItem, PREFS_FINDIN_COMMENTS, commentsIcon,
					UIText.HistoryPage_findbar_changeto_author);
		else if (menuItem == authorItem)
			selectFindInItem(menuItem, PREFS_FINDIN_AUTHOR, authorIcon,
					UIText.HistoryPage_findbar_changeto_commit);
		else if (menuItem == commitIdItem)
			selectFindInItem(menuItem, PREFS_FINDIN_COMMITID, commitIdIcon,
					UIText.HistoryPage_findbar_changeto_committer);
		else if (menuItem == committerItem)
			selectFindInItem(menuItem, PREFS_FINDIN_COMMITTER, committerIcon,
					UIText.HistoryPage_findbar_changeto_reference);
		else if (menuItem == referenceItem)
			selectFindInItem(menuItem, PREFS_FINDIN_REFERENCE, branchesIcon,
					UIText.HistoryPage_findbar_changeto_all);
	}

	private void selectFindInItem(MenuItem menuItem, int preferenceValue,
			Image dropDownIcon, String dropDownToolTip) {
		prefsDropDown.setImage(dropDownIcon);
		prefsDropDown.setToolTipText(dropDownToolTip);
		findInPreferenceChanged(preferenceValue, menuItem);
	}

	private void findInPreferenceChanged(int findin, MenuItem item) {
		store.setValue(UIPreferences.FINDTOOLBAR_FIND_IN, findin);
		if (store.needsSaving()){
			try {
				store.save();
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, false);
			}
		}
		allItem.setSelection(false);
		commitIdItem.setSelection(false);
		commentsItem.setSelection(false);
		authorItem.setSelection(false);
		committerItem.setSelection(false);
		referenceItem.setSelection(false);
		item.setSelection(true);
		clear();
	}

	private FindToolbarJob createFinder() {
		if (job != null) {
			job.cancel();
		}
		final String currentPattern = getSearchPattern();

		job = new FindToolbarJob(MessageFormat
				.format(UIText.HistoryPage_findbar_find, currentPattern),
				findResults);
		job.pattern = currentPattern;
		job.fileRevisions = fileRevisions;
		job.ignoreCase = caseItem.getSelection();
		if (allItem.getSelection()) {
			job.findInCommitId = true;
			job.findInComments = true;
			job.findInAuthor = true;
			job.findInCommitter = true;
			job.findInReference = true;
		} else {
			job.findInCommitId = commitIdItem.getSelection();
			job.findInComments = commentsItem.getSelection();
			job.findInAuthor = authorItem.getSelection();
			job.findInCommitter = committerItem.getSelection();
			job.findInReference = referenceItem.getSelection();
		}
		job.addJobChangeListener(new JobChangeAdapter() {

			private final FindToolbarJob myJob = job;

			@Override
			public void done(final IJobChangeEvent event) {
				if (event.getResult().isOK()) {
					getDisplay().asyncExec(new Runnable() {

						@Override
						public void run() {
							if (myJob != job
									|| myJob.fileRevisions != fileRevisions) {
								// Job superseded by another one; or input
								// changed
								return;
							}
							if (!isDisposed()) {
								findCompletionUpdate(currentPattern,
										findResults.isOverflow());
							}
						}
					});
				}
			}
		});
		return job;
	}

	/**
	 * Sets the table that will have its selected items changed by this toolbar.
	 * Sets the list to be searched.
	 *
	 * @param hFlag
	 * @param historyTable
	 * @param commitArray
	 */
	void setInput(final RevFlag hFlag, final Table historyTable,
			final SWTCommit[] commitArray) {
		// This may cause a FindBugs warning, but copying the array is probably
		// not a good idea.
		if (job != null) {
			job.cancel();
		}
		// Reset last used pattern -- we must not prevent a re-search when the
		// input changed.
		this.lastSearchPattern = null;
		this.fileRevisions = commitArray;
		this.historyTable = historyTable;
		findResults.setHighlightFlag(hFlag);
	}

	private void findCompletionUpdate(String pattern, boolean overflow) {
		lastSearchPattern = pattern;
		int total = findResults.size();
		String label;
		if (total > 0) {
			String position = (currentPosition < 0) ? "1" //$NON-NLS-1$
					: Integer.toString(currentPosition);
			if (overflow) {
				label = UIText.HistoryPage_findbar_exceeded + ' ' + position
						+ '/' + total;
			} else {
				label = position + '/' + total;
			}
			if (currentPosition < 0) {
				currentPosition = 1;
				int ix = findResults.getFirstIndex();
				notifyListeners(ix);
			}
			setNormalBackgroundColor();
			findNextAction.setEnabled(total > 1);
			findPreviousAction.setEnabled(total > 1);
			lastErrorPattern = null;
		} else {
			currentPosition = -1;
			if (pattern.length() > 0) {
				setNotFoundBackgroundColor();
				label = UIText.HistoryPage_findbar_notFound;
				// Don't keep beeping every time if the user is deleting
				// a long not found pattern
				if (lastErrorPattern == null
						|| !lastErrorPattern.startsWith(pattern)) {
					getDisplay().beep();
					findNextAction.setEnabled(false);
					findPreviousAction.setEnabled(false);
				}
				lastErrorPattern = pattern;
			} else {
				setNormalBackgroundColor();
				label = ""; //$NON-NLS-1$
				findNextAction.setEnabled(false);
				findPreviousAction.setEnabled(false);
				lastErrorPattern = null;
			}
		}
		historyTable.clearAll();

		if (overflow) {
			Display display = getDisplay();
			display.beep();
		}
		notifyStatus(label);
	}

	/**
	 * Clears the toolbar.
	 */
	void clear() {
		if (!isDisposed()) {
			setNormalBackgroundColor();
			if (patternField.getText().length() > 0) {
				patternField.selectAll();
			}
		}
		lastErrorPattern = null;

		if (job != null) {
			job.cancel();
			job = null;
		}

		findResults.clear();
	}

	private void notifyListeners(int index) {
		if (index >= 0) {
			Event event = new Event();
			event.type = SWT.Selection;
			event.index = index;
			event.widget = this;
			event.data = fileRevisions[index];
			for (Listener toNotify : eventList) {
				toNotify.handleEvent(event);
			}
		}
	}

	/**
	 * Adds a selection event listener. The toolbar generates events when it
	 * selects an item in the history table
	 *
	 * @param selectionListener
	 *            the listener that will receive the event
	 */
	public void addSelectionListener(Listener selectionListener) {
		eventList.add(selectionListener);
	}

	/**
	 * Removes a selection listener if it had been added.
	 *
	 * @param selectionListener
	 *            to remove
	 */
	public void removeSelectionListener(Listener selectionListener) {
		eventList.remove(selectionListener);
	}

	private IFindListener createFindListener() {
		return new IFindListener() {

			private static final long UPDATE_INTERVAL = 200L; // ms

			private long lastUpdate = 0L;

			@Override
			public void itemAdded(final int index, RevObject rev) {
				long now = System.currentTimeMillis();
				if (preselect != null && preselect.equals(rev.getId())
						|| preselect == null && currentPosition < 0) {
					currentPosition = findResults.getMatchNumberFor(index);
					preselect = null;
					getDisplay().asyncExec(new Runnable() {

						@Override
						public void run() {
							notifyListeners(index);
						}
					});
				}
				if (now - lastUpdate > UPDATE_INTERVAL && !isDisposed()) {
					final boolean firstUpdate = lastUpdate == 0L;
					lastUpdate = now;
					getDisplay().asyncExec(new Runnable() {

						@Override
						public void run() {
							if (isDisposed()) {
								return;
							}
							int total = findResults.size();
							if (total > 0) {
								String label;
								if (currentPosition == -1) {
									label = "-/" + total; //$NON-NLS-1$
								} else {
									label = Integer.toString(currentPosition)
											+ '/' + total;
								}
								findNextAction.setEnabled(total > 1);
								findPreviousAction.setEnabled(total > 1);
								setNormalBackgroundColor();
								if (firstUpdate) {
									historyTable.clearAll();
								}
								notifyStatus(label);
							} else {
								clear();
							}
						}
					});
				}
			}

			@Override
			public void cleared() {
				lastUpdate = 0L;
				if (Display.getCurrent() == null) {
					getDisplay().asyncExec(new Runnable() {

						@Override
						public void run() {
							clear();
						}
					});
				} else {
					clear();
				}
			}

			private void clear() {
				currentPosition = -1;
				if (!isDisposed()) {
					findNextAction.setEnabled(false);
					findPreviousAction.setEnabled(false);
					notifyStatus(""); //$NON-NLS-1$
				}
				if (historyTable != null && !historyTable.isDisposed()) {
					historyTable.clearAll();
				}
			}
		};
	}

}
