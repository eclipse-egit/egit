package org.eclipse.egit.ui.internal.clone;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.egit.ui.Activator;
import org.eclipse.egit.ui.UIText;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * @author Qiangsheng Wang(wangqs_eclipse@yahoo.com)
 *
 */
public class RepositorySourceFileSelectionPage extends WizardPage {

	private String helpContext;

	private CheckboxTableViewer viewer;

	private List<String> repoUrls = new ArrayList<String>();

	private Text sourceText;

	private Text destText;

	private Button singleRepoBtn;

	private Button multiRepoBtn;

	private Button browseButton;

	private Button destButton;

	/**
	 */
	protected RepositorySourceFileSelectionPage() {
		super(RepositorySourceFileSelectionPage.class.getName());
	}

	public void createControl(Composite parent) {
		final Composite mainComp = new Composite(parent, SWT.None);
		mainComp.setLayout(new GridLayout(3, false));
		mainComp.setLayoutData(new GridData(GridData.FILL_BOTH));

		singleRepoBtn = new Button(mainComp, SWT.RADIO);
		singleRepoBtn.setText(UIText.RepositorySourceFileSelectionPage_0);

		multiRepoBtn = new Button(mainComp, SWT.RADIO);
		multiRepoBtn.setText(UIText.RepositorySourceFileSelectionPage_1);

		new Label(mainComp, SWT.None);

		Label repoLabel = new Label(mainComp, SWT.None);
		repoLabel.setText(UIText.RepositorySourceFileSelectionPage_2);
		sourceText = new Text(mainComp, SWT.BORDER);
		sourceText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		sourceText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				initURLList();
				setPageComplete(validatePage());

			}
		});

		browseButton = new Button(mainComp, SWT.PUSH);
		browseButton.setText(UIText.RepositorySourceFileSelectionPage_3);
		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog dialog = new FileDialog(getShell());
				String path = dialog.open();
				if (path != null && !"".equals(path)) { //$NON-NLS-1$
					sourceText.setText(path);
					setPageComplete(validatePage());
				}
			}
		});

		viewer = CheckboxTableViewer.newCheckList(mainComp, SWT.BORDER);
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new LabelProvider());
		GridData data = new GridData(GridData.FILL_BOTH);
		data.horizontalSpan = 3;
		viewer.getTable().setLayoutData(data);
		viewer.addSelectionChangedListener(new ISelectionChangedListener() {

			public void selectionChanged(SelectionChangedEvent event) {
				setPageComplete(validatePage());
			}
		});

		Label locationLabel = new Label(mainComp, SWT.None);
		locationLabel.setText(UIText.RepositorySourceFileSelectionPage_4);
		destText = new Text(mainComp, SWT.BORDER);
		destText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		destText.addModifyListener(new ModifyListener() {

			public void modifyText(ModifyEvent e) {
				setPageComplete(validatePage());
			}
		});
		destButton = new Button(mainComp, SWT.PUSH);
		destButton.setText(UIText.RepositorySourceFileSelectionPage_3);
		destButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				String path = dialog.open();
				if (path != null) {
					destText.setText(path);
				}
			}
		});

		singleRepoBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setWidgetsState(!singleRepoBtn.getSelection());
				setErrorMessage(null);
				setPageComplete(true);
			}
		});

		multiRepoBtn.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setWidgetsState(multiRepoBtn.getSelection());
				setPageComplete(validatePage());
			}
		});

		singleRepoBtn.setSelection(true);
		setWidgetsState(false);

		setControl(mainComp);
	}

	private boolean validatePage() {
		if (getURIs() == null || getURIs().length == 0) {
			setErrorMessage(UIText.RepositorySourceFileSelectionPage_5);
			return false;
		}

		if (getDestination() == null || "".equals(getDestination().trim())) {//$NON-NLS-1$
			setErrorMessage(UIText.RepositorySourceFileSelectionPage_6);
			return false;
		}

		String path = getDestination();
		File file = new File(path);
		if (!file.exists() || !file.isDirectory()) {
			setErrorMessage(UIText.RepositorySourceFileSelectionPage_9);
			return false;
		}

		setErrorMessage(null);
		return true;
	}

	private void initURLList() {
		String path = sourceText.getText();
		if (path != null && !"".equals(path)) { //$NON-NLS-1$
			repoUrls = getUrls(path);
		}

		viewer.setInput(repoUrls);
		viewer.setAllChecked(true);
	}

	private List<String> getUrls(String path) {
		SimpleRpositoryURLFileParser parser = new SimpleRpositoryURLFileParser();
		try {
			return parser.parse(path);
		} catch (IOException e) {
			Activator.logError(
					UIText.RepositorySourceFileSelectionPage_7, e);
		} catch (URISyntaxException e) {
			Activator.logError(
					UIText.RepositorySourceFileSelectionPage_8, e);
		}
		return new ArrayList<String>();
	}

	/**
	 * Set the ID for context sensitive help
	 *
	 * @param id
	 *            help context
	 */
	public void setHelpContext(String id) {
		helpContext = id;
	}

	@Override
	public void performHelp() {
		PlatformUI.getWorkbench().getHelpSystem().displayHelp(helpContext);
	}

	/**
	 * @return array of urls
	 */
	public Object[] getURIs() {
		return viewer.getCheckedElements();
	}

	/**
	 * @return git repository location.
	 */
	public String getDestination() {
		return destText.getText();
	}

	/**
	 * @return selection of the single or multiple repositories.
	 */
	public boolean isSingleRepo() {
		return singleRepoBtn.getSelection();
	}

	private void setWidgetsState(boolean state) {
		viewer.getTable().setEnabled(state);
		browseButton.setEnabled(state);
		destText.setEnabled(state);
		destButton.setEnabled(state);
		sourceText.setEnabled(state);
	}
}
