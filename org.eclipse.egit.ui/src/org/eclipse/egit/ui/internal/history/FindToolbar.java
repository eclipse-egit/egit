/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 * Copyright (C) 2013, Robin Stocker <robin@nibor.org>
 * Copyright (C) 2016, Thomas Wolf <thomas.wolf@paranor.ch>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.internal.UIIcons;
import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.resource.ResourceManager;
import org.eclipse.jgit.revwalk.RevFlag;
import org.eclipse.jgit.revwalk.RevObject;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

/**
 * A toolbar for the history page.
 *
 * @see FindToolbarJob
 * @see FindResults
 * @see GitHistoryPage
 */
public class FindToolbar extends Composite {
	/**
	 * Preference value for searching all the fields
	 */
	public static final int PREFS_FINDIN_ALL = 0;

	private static final int PREFS_FINDIN_COMMENTS = 1;

	private static final int PREFS_FINDIN_AUTHOR = 2;

	private static final int PREFS_FINDIN_COMMITID = 3;

	private static final int PREFS_FINDIN_COMMITTER = 4;

	private static final int PREFS_FINDIN_REFERENCE = 5;

	private Color errorBackgroundColor;

	/**
	 * The results (matches) of the current find operation.
	 */
	private final FindResults findResults;

	private IPersistentPreferenceStore store = (IPersistentPreferenceStore) Activator.getDefault().getPreferenceStore();

	private List<Listener> eventList = new ArrayList<>();

	private Table historyTable;

	private SWTCommit[] fileRevisions;

	private Text patternField;

	private Button nextButton;

	private Button previousButton;

	private Label currentPositionLabel;

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

	private Image nextIcon;

	private Image previousIcon;

	private Image allIcon;

	private Image commitIdIcon;

	private Image commentsIcon;

	private Image authorIcon;

	private Image committerIcon;

	private Image branchesIcon;

	private FindToolbarJob job;

	private int currentPosition = -1;

	/**
	 * Creates the toolbar.
	 *
	 * @param parent
	 *            the parent widget
	 */
	public FindToolbar(Composite parent) {
		super(parent, SWT.NULL);
		findResults = new FindResults(createFindListener());
		createToolbar();
	}

	private void createToolbar() {
		errorBackgroundColor = new Color(getDisplay(), new RGB(255, 150, 150));
		ResourceManager resourceManager = Activator.getDefault()
				.getResourceManager();
		nextIcon = UIIcons.getImage(resourceManager, UIIcons.ELCL16_NEXT);
		previousIcon = UIIcons.getImage(resourceManager,
				UIIcons.ELCL16_PREVIOUS);
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
		findLayout.marginHeight = 2;
		findLayout.marginWidth = 2;
		findLayout.numColumns = 5;
		setLayout(findLayout);
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		patternField = new Text(this,
				SWT.SEARCH | SWT.ICON_CANCEL | SWT.ICON_SEARCH);
		GridData findTextData = new GridData(SWT.FILL, SWT.LEFT, true, false);
		findTextData.minimumWidth = 50;
		patternField.setLayoutData(findTextData);
		patternField.setTextLimit(100);

		nextButton = new Button(this, SWT.PUSH);
		nextButton.setImage(nextIcon);
		nextButton.setText(UIText.HistoryPage_findbar_next);
		nextButton.setToolTipText(UIText.FindToolbar_NextTooltip);

		previousButton = new Button(this, SWT.PUSH);
		previousButton.setImage(previousIcon);
		previousButton.setText(UIText.HistoryPage_findbar_previous);
		previousButton.setToolTipText(UIText.FindToolbar_PreviousTooltip);

		final ToolBar toolBar = new ToolBar(this, SWT.FLAT);
		new ToolItem(toolBar, SWT.SEPARATOR);

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

		currentPositionLabel = new Label(this, SWT.NULL);
		GridData totalLabelData = new GridData();
		totalLabelData.horizontalAlignment = SWT.FILL;
		totalLabelData.grabExcessHorizontalSpace = true;
		currentPositionLabel.setLayoutData(totalLabelData);
		currentPositionLabel.setAlignment(SWT.RIGHT);
		currentPositionLabel.setText(""); //$NON-NLS-1$

		patternField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				final FindToolbarJob finder = createFinder();
				finder.setUser(true);
				finder.schedule(200);
			}
		});

		patternField.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				if (e.detail != SWT.ICON_CANCEL
						&& !patternField.getText().isEmpty()) {
					// ENTER or the search icon clicked
					final FindToolbarJob finder = createFinder();
					finder.setUser(true);
					finder.schedule();
				}
			}
		});

		final Listener findButtonsListener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (patternField.getText().length() > 0
						&& findResults.size() == 0) {
					// If the toolbar was cleared and has a pattern typed,
					// then we redo the find with the new table data.
					final FindToolbarJob finder = createFinder();
					finder.setUser(true);
					finder.schedule();
					patternField.setSelection(0, 0);
				} else {
					int currentIx = historyTable.getSelectionIndex();
					int newIx = -1;
					if (event.widget == nextButton) {
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
					sendEvent(event.widget, newIx);

					String current = null;
					currentPosition = findResults.getMatchNumberFor(newIx);
					if (currentPosition == -1) {
						current = "-"; //$NON-NLS-1$
					} else {
						current = String.valueOf(currentPosition);
					}
					currentPositionLabel
							.setText(current + '/' + findResults.size());
				}
			}
		};
		nextButton.addListener(SWT.Selection, findButtonsListener);
		previousButton.addListener(SWT.Selection, findButtonsListener);

		patternField.addKeyListener(new KeyAdapter() {
			private Event event = new Event();

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_DOWN) {
					if (nextButton.isEnabled()) {
						event.widget = nextButton;
						findButtonsListener.handleEvent(event);
					}
				} else if (e.keyCode == SWT.ARROW_UP) {
					if (previousButton.isEnabled()) {
						event.widget = previousButton;
						findButtonsListener.handleEvent(event);
					}
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
				if (job != null) {
					job.cancel();
					job = null;
				}
				prefsMenu.dispose();
				errorBackgroundColor.dispose();
			}
		});
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
		final String currentPattern = patternField.getText();

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
							if (myJob != job) {
								// Job superseded by another one
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
		// this may cause a FindBugs warning, but
		// copying the array is probably not a good
		// idea
		this.fileRevisions = commitArray;
		this.historyTable = historyTable;
		findResults.setHighlightFlag(hFlag);
	}

	private void findCompletionUpdate(String pattern, boolean overflow) {
		int total = findResults.size();
		if (total > 0) {
			String position = (currentPosition < 0) ? "1" //$NON-NLS-1$
					: Integer.toString(currentPosition);
			if (overflow) {
				currentPositionLabel.setText(UIText.HistoryPage_findbar_exceeded
						+ ' ' + position + '/' + total);
			} else {
				currentPositionLabel.setText(position + '/' + total);
			}
			if (currentPosition < 0) {
				currentPosition = 1;
				int ix = findResults.getFirstIndex();
				sendEvent(null, ix);
			}
			patternField.setBackground(null);
			nextButton.setEnabled(true);
			previousButton.setEnabled(true);
			lastErrorPattern = null;
		} else {
			currentPosition = -1;
			if (pattern.length() > 0) {
				patternField.setBackground(errorBackgroundColor);
				currentPositionLabel
						.setText(UIText.HistoryPage_findbar_notFound);
				// Don't keep beeping every time if the user is deleting
				// a long not found pattern
				if (lastErrorPattern == null
						|| !lastErrorPattern.startsWith(pattern)) {
					getDisplay().beep();
					nextButton.setEnabled(false);
					previousButton.setEnabled(false);
				}
				lastErrorPattern = pattern;
			} else {
				patternField.setBackground(null);
				currentPositionLabel.setText(""); //$NON-NLS-1$
				nextButton.setEnabled(false);
				previousButton.setEnabled(false);
				lastErrorPattern = null;
			}
		}
		historyTable.clearAll();

		if (overflow) {
			Display display = getDisplay();
			currentPositionLabel.setForeground(display
					.getSystemColor(SWT.COLOR_RED));
			display.beep();
		} else {
			currentPositionLabel.setForeground(null);
		}
	}

	/**
	 * Clears the toolbar.
	 */
	void clear() {
		patternField.setBackground(null);
		lastErrorPattern = null;

		if (job != null) {
			job.cancel();
			job = null;
		}

		findResults.clear();

		if (patternField.getText().length() > 0) {
			patternField.selectAll();
		}
	}

	private void sendEvent(Widget widget, int index) {
		Event event = new Event();
		event.type = SWT.Selection;
		event.index = index;
		event.widget = widget;
		event.data = fileRevisions[index];
		for (Listener listener : eventList) {
			listener.handleEvent(event);
		}
	}

	/**
	 * Adds a selection event listener. The toolbar generates events when it
	 * selects an item in the history table
	 *
	 * @param listener
	 *            the listener that will receive the event
	 */
	void addSelectionListener(Listener listener) {
		eventList.add(listener);
	}

	private IFindListener createFindListener() {
		return new IFindListener() {

			private static final long UPDATE_INTERVAL = 200L; // ms

			private long lastUpdate = 0L;

			@Override
			public void itemAdded(int index, RevObject rev) {
				long now = System.currentTimeMillis();
				if (now - lastUpdate > UPDATE_INTERVAL) {
					final boolean firstUpdate = lastUpdate == 0L;
					lastUpdate = now;
					getDisplay().asyncExec(new Runnable() {

						@Override
						public void run() {
							int total = findResults.size();
							currentPositionLabel.setForeground(null);
							if (total > 0) {
								if (currentPosition == -1) {
									currentPositionLabel.setText("-/" + total); //$NON-NLS-1$
								} else {
									currentPositionLabel.setText(
											Integer.toString(currentPosition)
													+ '/' + total);
								}
								nextButton.setEnabled(true);
								previousButton.setEnabled(true);
								patternField.setBackground(null);
								if (firstUpdate) {
									historyTable.clearAll();
								}
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
				currentPositionLabel.setText(""); //$NON-NLS-1$
				nextButton.setEnabled(false);
				previousButton.setEnabled(false);
				if (historyTable != null) {
					historyTable.clearAll();
				}
			}
		};
	}
}
