/*******************************************************************************
 * Copyright (c) 2006-2010 eBay Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *******************************************************************************/
package org.ebayopensource.turmeric.eclipse.ui;

import org.ebayopensource.turmeric.eclipse.exception.validation.ValidationInterruptedException;
import org.ebayopensource.turmeric.eclipse.repositorysystem.core.GlobalRepositorySystem;
import org.ebayopensource.turmeric.eclipse.repositorysystem.core.ISOARepositorySystem;
import org.ebayopensource.turmeric.eclipse.utils.ui.UIUtil;
import org.ebayopensource.turmeric.eclipse.validator.core.ISOAPreValidator;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

/**
 * The Class SOABaseWizard.
 *
 * @author smathew This is the base class for all new SOA wizards that are
 * contributed to creation wizards extension point
 */
public abstract class SOABaseWizard extends Wizard implements INewWizard,
		IExecutableExtension {
	private IStructuredSelection selection;
	private static final int MIN_WIDTH = 600;
	private static final int MIN_HEIGHT = 600;

	/**
	 * Instantiates a new sOA base wizard.
	 */
	public SOABaseWizard() {
		super();
		setWindowTitle("Turmeric SOA"); //not setting the window title would cause NPE when activating the context help
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean performFinish() {
		return false;
	}

	/** The config. */
	protected IConfigurationElement fConfig;
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setInitializationData(IConfigurationElement config,
			String propertyName, Object data) throws CoreException {
		fConfig = config;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final void init(IWorkbench workbench, IStructuredSelection selection) {
		this.selection = selection;
	}

	/**
	 * Gets the selection.
	 *
	 * @return the selection
	 */
	public IStructuredSelection getSelection() {
		return selection;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see org.eclipse.jface.wizard.Wizard#addPages() This is overriden here to
	 * include the pre flight checks at a common place. Implementors if
	 * overriding this page should call super to get the validation done
	 */
	@Override
	public void addPages() {
		// center the wizard
		if (getContainer() != null)
			setShellAtCenter(getContainer().getShell());
		IStatus model = null;
		try {
			model = preValidate();
		} catch (ValidationInterruptedException e) {
			UIUtil.showErrorDialog(e);
			return;
		}

		if (model.getSeverity() == IStatus.ERROR) {
			IWizardPage errorPage = new SOATextPage("Pre-validation Failed",
					"Additional setup required", "The "
							+ GlobalRepositorySystem.instanceOf()
									.getActiveRepositorySystem()
									.getDisplayName()
							+ " requires additional setup before being used.",
					model);
			addPage(errorPage);
		} else {
			for (IWizardPage page : getContentPages()) {
				addPage(page);
			}
		}

		super.addPages();
	}

	/**
	 * Pre validate.
	 *
	 * @return the i status
	 * @throws ValidationInterruptedException the validation interrupted exception
	 */
	public IStatus preValidate() throws ValidationInterruptedException {
		ISOARepositorySystem activeRepositorySystem = GlobalRepositorySystem
				.instanceOf().getActiveRepositorySystem();
		ISOAPreValidator validator = activeRepositorySystem.getPreValidator();
		return validator.validateProjectCreation(getCreatingType());
	}

	/**
	 * Gets the creating type.
	 *
	 * @return the creating type
	 */
	protected Object getCreatingType() {
		return ISOAPreValidator.OTHERS;
	}

	/**
	 * Gets the content pages.
	 *
	 * @return Since addPages are overriden Here is the place for implemntors to
	 * supply their wizard pages except for error pages, everything goes
	 * by base wizard alogorithm for navigation. ie it goes by array
	 * index
	 */
	public abstract IWizardPage[] getContentPages();

	/**
	 * Gets the minimum width.
	 *
	 * @return the minimum width
	 */
	public int getMinimumWidth() {
		return MIN_WIDTH;
	}

	/**
	 * Gets the minimum height.
	 *
	 * @return the minimum height
	 */
	public int getMinimumHeight() {
		return MIN_HEIGHT;
	}

	// Center the active shell
	/**
	 * Sets the shell at center.
	 *
	 * @param activeShell the new shell at center
	 */
	public void setShellAtCenter(Shell activeShell) {
		Rectangle rect = activeShell.getDisplay().getPrimaryMonitor()
				.getBounds();

		// Calculate the left and top
		int left = (rect.width - getMinimumWidth()) / 2;
		int top = (rect.height - getMinimumHeight()) / 2;

		// Set shell bounds,
		activeShell.setBounds(left, top, getMinimumWidth(), getMinimumHeight());
	}

	/**
	 * Change perspective.
	 */
	protected void changePerspective() {
		UIJob fNotifierJob = new UIJob("Change to Turmeric Development Perspective") {
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				BasicNewProjectResourceWizard.updatePerspective(fConfig);
				return Status.OK_STATUS;
			}
		};
		fNotifierJob.setSystem(true);
		fNotifierJob.schedule();
	}
}
