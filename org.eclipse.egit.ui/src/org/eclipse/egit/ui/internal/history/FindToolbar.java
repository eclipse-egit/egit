/*******************************************************************************
 * Copyright (C) 2007, Dave Watson <dwatson@mimvista.com>
 * Copyright (C) 2008, Roger C. Soares <rogersoares@intelinet.com.br>
 * Copyright (C) 2008, Shawn O. Pearce <spearce@spearce.org>
 * Copyright (C) 2010, Mathias Kinzler <mathias.kinzler@sap.com>
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.egit.ui.internal.history;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIIcons;
import org.eclipse.egit.ui.UIPreferences;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jgit.revwalk.RevFlag;
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
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;

/**
 * A toolbar for the history page.
 *
 * @see FindToolbarThread
 * @see FindResults
 * @see GitHistoryPage
 */
public class FindToolbar extends Composite {
	private static final int PREFS_FINDIN_COMMENTS = 1;

	private static final int PREFS_FINDIN_AUTHOR = 2;

	private static final int PREFS_FINDIN_COMMITID = 3;

	private static final int PREFS_FINDIN_COMMITTER = 4;

	private Color errorBackgroundColor;

	/**
	 * The results (matches) of the current find operation.
	 */
	public final FindResults findResults = new FindResults();

	private IPersistentPreferenceStore store = (IPersistentPreferenceStore) Activator
			.getDefault().getPreferenceStore();

	private List<Listener> eventList = new ArrayList<Listener>();

	private Table historyTable;

	private SWTCommit[] fileRevisions;

	private Text patternField;

	private Button nextButton;

	private Button previousButton;

	private Label currentPositionLabel;

	private ProgressBar progressBar;

	private String lastErrorPattern;

	private Menu prefsMenu;

	private MenuItem commitIdItem;

	private MenuItem commentsItem;

	private MenuItem authorItem;

	private MenuItem committerItem;

	private Image nextIcon;

	private Image previousIcon;

	private Image commitIdIcon;

	private Image commentsIcon;

	private Image authorIcon;

	private Image committerIcon;

	/**
	 * Creates the toolbar.
	 *
	 * @param parent
	 *            the parent widget
	 */
	public FindToolbar(Composite parent) {
		super(parent, SWT.NULL);
		createToolbar();
	}

	private void createToolbar() {
		errorBackgroundColor = new Color(getDisplay(), new RGB(255, 150, 150));
		nextIcon = UIIcons.ELCL16_NEXT.createImage();
		previousIcon = UIIcons.ELCL16_PREVIOUS.createImage();
		commitIdIcon = UIIcons.ELCL16_ID.createImage();
		commentsIcon = UIIcons.ELCL16_COMMENTS.createImage();
		authorIcon = UIIcons.ELCL16_AUTHOR.createImage();
		committerIcon = UIIcons.ELCL16_COMMITTER.createImage();

		GridLayout findLayout = new GridLayout();
		findLayout.marginHeight = 2;
		findLayout.marginWidth = 2;
		findLayout.numColumns = 8;
		setLayout(findLayout);
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		Label findLabel = new Label(this, SWT.NULL);
		findLabel.setText(UIText.HistoryPage_findbar_find);

		patternField = new Text(this, SWT.SEARCH);
		GridData findTextData = new GridData(SWT.FILL, SWT.CENTER, true, false);
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

		final ToolItem prefsItem = new ToolItem(toolBar, SWT.DROP_DOWN);
		prefsMenu = new Menu(getShell(), SWT.POP_UP);
		final MenuItem caseItem = new MenuItem(prefsMenu, SWT.CHECK);
		caseItem.setText(UIText.HistoryPage_findbar_ignorecase);
		new MenuItem(prefsMenu, SWT.SEPARATOR);
		commentsItem = new MenuItem(prefsMenu, SWT.RADIO);
		commentsItem.setText(UIText.HistoryPage_findbar_comments);
		commentsItem.setImage(commentsIcon);
		authorItem = new MenuItem(prefsMenu, SWT.RADIO);
		authorItem.setText(UIText.HistoryPage_findbar_author);
		authorItem.setImage(authorIcon);
		commitIdItem = new MenuItem(prefsMenu, SWT.RADIO);
		commitIdItem.setText(UIText.HistoryPage_findbar_commit);
		commitIdItem.setImage(commitIdIcon);
		committerItem = new MenuItem(prefsMenu, SWT.RADIO);
		committerItem.setText(UIText.HistoryPage_findbar_committer);
		committerItem.setImage(committerIcon);

		prefsItem.addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				if (event.detail == SWT.ARROW) {
					Rectangle itemBounds = prefsItem.getBounds();
					Point point = toolBar.toDisplay(itemBounds.x, itemBounds.y
							+ itemBounds.height);
					prefsMenu.setLocation(point);
					prefsMenu.setVisible(true);
				} else {
					switch (store.getInt(UIPreferences.FINDTOOLBAR_FIND_IN)) {
					case PREFS_FINDIN_COMMENTS:
						commentsItem.notifyListeners(SWT.Selection, null);
						break;
					case PREFS_FINDIN_AUTHOR:
						authorItem.notifyListeners(SWT.Selection, null);
						break;
					case PREFS_FINDIN_COMMITID:
						commitIdItem.notifyListeners(SWT.Selection, null);
						break;
					case PREFS_FINDIN_COMMITTER:
						committerItem.notifyListeners(SWT.Selection, null);
						break;
					}
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

		progressBar = new ProgressBar(this, SWT.HORIZONTAL);
		GridData findProgressBarData = new GridData();
		findProgressBarData.heightHint = 12;
		findProgressBarData.widthHint = 35;
		progressBar.setLayoutData(findProgressBarData);
		progressBar.setMinimum(0);
		progressBar.setMaximum(100);

		final FindToolbar thisToolbar = this;
		patternField.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				final FindToolbarThread finder = new FindToolbarThread();
				finder.pattern = ((Text) e.getSource()).getText();
				finder.fileRevisions = fileRevisions;
				finder.toolbar = thisToolbar;
				finder.ignoreCase = caseItem.getSelection();
				finder.findInCommitId = commitIdItem.getSelection();
				finder.findInComments = commentsItem.getSelection();
				finder.findInAuthor = authorItem.getSelection();
				finder.findInCommitter = committerItem.getSelection();
				getDisplay().timerExec(200, new Runnable() {
					public void run() {
						finder.start();
					}
				});
			}
		});

		final Listener findButtonsListener = new Listener() {
			public void handleEvent(Event event) {
				if (patternField.getText().length() > 0
						&& findResults.size() == 0) {
					// If the toolbar was cleared and has a pattern typed,
					// then we redo the find with the new table data.
					final FindToolbarThread finder = new FindToolbarThread();
					finder.pattern = patternField.getText();
					finder.fileRevisions = fileRevisions;
					finder.toolbar = thisToolbar;
					finder.ignoreCase = caseItem.getSelection();
					finder.findInCommitId = commitIdItem.getSelection();
					finder.findInComments = commentsItem.getSelection();
					finder.findInAuthor = authorItem.getSelection();
					finder.findInCommitter = committerItem.getSelection();
					finder.start();
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
					int currentValue = findResults.getMatchNumberFor(newIx);
					if (currentValue == -1) {
						current = "-"; //$NON-NLS-1$
					} else {
						current = String.valueOf(currentValue);
					}
					currentPositionLabel.setText(current + "/" //$NON-NLS-1$
							+ findResults.size());
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
			public void widgetSelected(SelectionEvent e) {
				store.setValue(UIPreferences.FINDTOOLBAR_IGNORE_CASE,
						caseItem.getSelection());
				if (store.needsSaving()) {
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

		commentsItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				prefsItem.setImage(commentsIcon);
				prefsItem
						.setToolTipText(UIText.HistoryPage_findbar_changeto_author);
				prefsItemChanged(PREFS_FINDIN_AUTHOR, commentsItem);
			}
		});
		if (selectedPrefsItem == PREFS_FINDIN_COMMENTS) {
			commentsItem.setSelection(true);
			prefsItem.setImage(commentsIcon);
			prefsItem
					.setToolTipText(UIText.HistoryPage_findbar_changeto_author);
		}

		authorItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				prefsItem.setImage(authorIcon);
				prefsItem
						.setToolTipText(UIText.HistoryPage_findbar_changeto_commit);
				prefsItemChanged(PREFS_FINDIN_COMMITID, authorItem);
			}
		});
		if (selectedPrefsItem == PREFS_FINDIN_AUTHOR) {
			authorItem.setSelection(true);
			prefsItem.setImage(authorIcon);
			prefsItem
					.setToolTipText(UIText.HistoryPage_findbar_changeto_commit);
		}

		commitIdItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				prefsItem.setImage(commitIdIcon);
				prefsItem
						.setToolTipText(UIText.HistoryPage_findbar_changeto_committer);
				prefsItemChanged(PREFS_FINDIN_COMMITTER, commitIdItem);
			}
		});
		if (selectedPrefsItem == PREFS_FINDIN_COMMITID) {
			commitIdItem.setSelection(true);
			prefsItem.setImage(commitIdIcon);
			prefsItem
					.setToolTipText(UIText.HistoryPage_findbar_changeto_committer);
		}

		committerItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				prefsItem.setImage(committerIcon);
				prefsItem
						.setToolTipText(UIText.HistoryPage_findbar_changeto_comments);
				prefsItemChanged(PREFS_FINDIN_COMMENTS, committerItem);
			}
		});
		if (selectedPrefsItem == PREFS_FINDIN_COMMITTER) {
			committerItem.setSelection(true);
			prefsItem.setImage(committerIcon);
			prefsItem
					.setToolTipText(UIText.HistoryPage_findbar_changeto_comments);
		}

		registerDisposal();
	}

	private void registerDisposal() {
		addDisposeListener(new DisposeListener() {

			public void widgetDisposed(DisposeEvent e) {
				prefsMenu.dispose();
				errorBackgroundColor.dispose();
				nextIcon.dispose();
				previousIcon.dispose();
				commitIdIcon.dispose();
				commentsIcon.dispose();
				authorIcon.dispose();
				committerIcon.dispose();
			}
		});
	}

	private void prefsItemChanged(int findin, MenuItem item) {
		store.setValue(UIPreferences.FINDTOOLBAR_FIND_IN, findin);
		if (store.needsSaving()) {
			try {
				store.save();
			} catch (IOException e) {
				Activator.handleError(e.getMessage(), e, false);
			}
		}
		commitIdItem.setSelection(false);
		commentsItem.setSelection(false);
		authorItem.setSelection(false);
		committerItem.setSelection(false);
		item.setSelection(true);
		clear();
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

	void progressUpdate(int percent) {
		int total = findResults.size();
		currentPositionLabel.setText("-/" + total); //$NON-NLS-1$
		currentPositionLabel.setForeground(null);
		if (total > 0) {
			nextButton.setEnabled(true);
			previousButton.setEnabled(true);
			patternField.setBackground(null);
		} else {
			nextButton.setEnabled(false);
			previousButton.setEnabled(false);
		}
		progressBar.setSelection(percent);
		historyTable.clearAll();
	}

	void findCompletionUpdate(String pattern, boolean overflow) {
		int total = findResults.size();
		if (total > 0) {
			if (overflow) {
				currentPositionLabel
						.setText(UIText.HistoryPage_findbar_exceeded + " 1/" //$NON-NLS-1$
								+ total);
			} else {
				currentPositionLabel.setText("1/" + total); //$NON-NLS-1$
			}
			int ix = findResults.getFirstIndex();
			sendEvent(null, ix);

			patternField.setBackground(null);
			nextButton.setEnabled(true);
			previousButton.setEnabled(true);
			lastErrorPattern = null;
		} else {
			if (pattern.length() > 0) {
				patternField.setBackground(errorBackgroundColor);
				currentPositionLabel
						.setText(UIText.HistoryPage_findbar_notFound);
				// Don't keep beeping every time if the user is deleting
				// a long not found pattern
				if (lastErrorPattern == null
						|| (lastErrorPattern != null && !lastErrorPattern
								.startsWith(pattern))) {
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
		progressBar.setSelection(0);
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
		if (patternField.getText().length() > 0) {
			patternField.selectAll();
			nextButton.setEnabled(true);
			previousButton.setEnabled(true);
		} else {
			nextButton.setEnabled(false);
			previousButton.setEnabled(false);
		}
		currentPositionLabel.setText(""); //$NON-NLS-1$
		progressBar.setSelection(0);
		lastErrorPattern = null;

		findResults.clear();
		if (historyTable != null) {
			historyTable.clearAll();
		}

		FindToolbarThread.updateGlobalThreadIx();
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

}
