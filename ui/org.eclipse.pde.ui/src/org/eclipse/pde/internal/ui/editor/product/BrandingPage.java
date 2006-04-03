/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.product;

import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;


public class BrandingPage extends PDEFormPage {
	
	public static final String PAGE_ID = "branding"; //$NON-NLS-1$

	public BrandingPage(FormEditor editor) {
		super(editor, PAGE_ID, PDEUIMessages.BrandingPage_title); 
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.PDEFormPage#createFormContent(org.eclipse.ui.forms.IManagedForm)
	 */
	protected void createFormContent(IManagedForm managedForm) {
		super.createFormContent(managedForm);
		ScrolledForm form = managedForm.getForm();
		FormToolkit toolkit = managedForm.getToolkit();
		form.setText(PDEUIMessages.BrandingPage_title);  
		fillBody(managedForm, toolkit);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), IHelpContextIds.BRANDING_PAGE);
	}
	
	private void fillBody(IManagedForm managedForm, FormToolkit toolkit) {
		Composite body = managedForm.getForm().getBody();
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = true;
		layout.marginWidth = 10;
		layout.verticalSpacing = 15;
		layout.horizontalSpacing = 10;
		body.setLayout(layout);

		// sections
		managedForm.addPart(new SplashSection(this, body));	
		
		Composite comp = toolkit.createComposite(body);
		layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		layout.verticalSpacing = 15;
		comp.setLayout(layout);
		comp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL|GridData.VERTICAL_ALIGN_BEGINNING));
		managedForm.addPart(new WindowImagesSection(this, comp));
		managedForm.addPart(new IntroSection(this, comp));

		managedForm.addPart(new AboutSection(this, body));	
	}


}
