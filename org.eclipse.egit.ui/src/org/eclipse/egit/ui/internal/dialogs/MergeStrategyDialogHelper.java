package org.eclipse.egit.ui.internal.dialogs;

import org.eclipse.egit.ui.internal.UIText;
import org.eclipse.egit.ui.internal.preferences.MergeStrategyHelper;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;

/**
 * Helper to facilitate the creation of an area to select an optional merge
 * strategy in dialogs.
 */
public class MergeStrategyDialogHelper extends MergeStrategyHelper {

	/** */
	public MergeStrategyDialogHelper() {
		super(false);
	}

	/**
	 * Create an area in the dialog to optionally select the merge strategy.
	 *
	 * @param parent
	 */
	public void createMergeStrategyGroup(final Composite parent) {
		Button cbStrategy = new Button(parent, SWT.CHECK);
		cbStrategy.setText(UIText.MergeDialog_cbStrategy_Text);
		cbStrategy.setToolTipText(UIText.MergeDialog_cbStrategy_Tooltip);

		Composite c = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(c);
		FillLayout fillLayout = new FillLayout(SWT.HORIZONTAL);
		fillLayout.marginWidth = -5;
		c.setLayout(fillLayout);
		final ScrolledComposite sc = new ScrolledComposite(c, SWT.V_SCROLL);
		sc.setExpandVertical(true);
		sc.setExpandHorizontal(true);
		final Composite main = new Composite(sc, SWT.NONE);
		GridLayout gridLayout = new GridLayout(1, false);
		gridLayout.marginLeft = 0;
		gridLayout.horizontalSpacing = 0;
		main.setLayout(gridLayout);
		GridDataFactory.fillDefaults().grab(true, true)
				.minSize(SWT.DEFAULT, 200).applyTo(main);

		final Group strategyGroup = new Group(main, SWT.NONE);
		strategyGroup.setText(UIText.MergeTargetSelectionDialog_MergeStrategy);
		GridDataFactory.fillDefaults().grab(true, false).hint(SWT.DEFAULT, 10)
				.applyTo(strategyGroup);
		gridLayout = new GridLayout(1, false);
		gridLayout.marginLeft = 0;
		gridLayout.horizontalSpacing = 0;
		strategyGroup.setLayout(gridLayout);
		strategyGroup.setVisible(false);

		cbStrategy.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (((Button) e.widget).getSelection()) {
					strategyGroup.setVisible(true);
					GridDataFactory.fillDefaults().grab(true, true)
							.applyTo(strategyGroup);
					strategyGroup.pack(true);
					sc.setMinSize(main.computeSize(SWT.DEFAULT, SWT.DEFAULT));
					parent.layout(true);
				} else {
					strategyGroup.setVisible(false);
					GridDataFactory.fillDefaults().grab(true, false)
							.hint(SWT.DEFAULT, 10).applyTo(strategyGroup);
					strategyGroup.pack(true);
					sc.setMinSize(main.computeSize(SWT.DEFAULT, SWT.DEFAULT));
					parent.layout(true);
				}
			}
		});

		createContents(strategyGroup);
		load();

		sc.setContent(main);
		sc.setMinSize(main.computeSize(SWT.DEFAULT, SWT.DEFAULT));
		sc.setRedraw(true);
	}
}
