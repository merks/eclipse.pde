/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Common Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.feature;

import java.lang.reflect.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.launching.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.build.*;
import org.eclipse.pde.internal.core.feature.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.internal.ui.wizards.feature.FeatureCustomHandlerPage.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.ide.*;
import org.eclipse.ui.part.*;
import org.eclipse.ui.wizards.newresource.*;

public class NewFeatureProjectWizard extends NewWizard
		implements
			IExecutableExtension {
	public static final String KEY_WTITLE = "NewFeatureWizard.wtitle";
	public static final String MAIN_PAGE_TITLE = "NewFeatureWizard.MainPage.title";
	public static final String MAIN_PAGE_DESC = "NewFeatureWizard.MainPage.desc";
	public static final String DEF_PROJECT_NAME = "project-name";
	public static final String DEF_ID = "feature-id";
	public static final String DEF_NAME = "feature-name";

	public static final String CREATING_PROJECT = "NewFeatureWizard.creatingProject";
	public static final String OVERWRITE_FEATURE = "NewFeatureWizard.overwriteFeature";
	public static final String CREATING_FOLDERS = "NewFeatureWizard.creatingFolders";
	public static final String CREATING_MANIFEST = "NewFeatureWizard.creatingManifest";

	private WizardNewProjectCreationPage mainPage;
	private FeatureSpecPage specPage;
	private PluginListPage pluginListPage;
	private FeatureCustomHandlerPage structurePage;
	private IConfigurationElement config;
	private IProjectProvider provider;
	public class FeatureProjectProvider implements IProjectProvider {
		public FeatureProjectProvider() {
			super();
		}
		public String getProjectName() {
			return mainPage.getProjectName();
		}
		public IProject getProject() {
			return mainPage.getProjectHandle();
		}
		public IPath getLocationPath() {
			return mainPage.getLocationPath();
		}
		public FeatureData getFeatureData() {
			return specPage.getFeatureData();
		}
		public IPluginBase[] getPluginListSelection() {
			if (pluginListPage == null)
				return null;
			return pluginListPage.getSelectedPlugins();
		}
		public IConfigurationElement getConfigElement() {
			return config;
		}
	}
	public NewFeatureProjectWizard() {
		super();
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWFTRPRJ_WIZ);
		setDialogSettings(PDEPlugin.getDefault().getDialogSettings());
		setNeedsProgressMonitor(true);
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
	}
	
	public void addPages() {
		provider = new FeatureProjectProvider();
		mainPage = new WizardNewProjectCreationPage("main");
		mainPage.setTitle(PDEPlugin.getResourceString(MAIN_PAGE_TITLE));
		mainPage.setDescription(PDEPlugin.getResourceString(MAIN_PAGE_DESC));
		String pname = getDefaultValue(DEF_PROJECT_NAME);
		if (pname != null)
			mainPage.setInitialProjectName(pname);
		addPage(mainPage);
		specPage = new FeatureSpecPage(mainPage);
		specPage.setInitialId(getDefaultValue(DEF_ID));
		specPage.setInitialName(getDefaultValue(DEF_NAME));
		addPage(specPage);
		structurePage = new FeatureCustomHandlerPage(provider);
		addPage(structurePage);
		if (hasInterestingProjects()) {
			pluginListPage = new PluginListPage();
			addPage(pluginListPage);
		}
	}

	public boolean canFinish() {
		IWizardPage page = getContainer().getCurrentPage();
		return ((page == specPage && structurePage.isInitialized())
				|| (page == structurePage && page.isPageComplete()) || (page == pluginListPage && page
				.isPageComplete()));
	}

	private boolean hasInterestingProjects() {
		IWorkspace workspace = PDEPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		IProject[] projects = root.getProjects();
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			if (WorkspaceModelManager.isPluginProject(project))
				return true;
		}
		return false;
	}

	public boolean performFinish() {
		final IProject project = ((FeatureProjectProvider) provider)
				.getProject();
		final IPath location = ((FeatureProjectProvider) provider)
				.getLocationPath();
		final FeatureData data = ((FeatureProjectProvider) provider)
				.getFeatureData();
		final IPluginBase[] plugins = ((FeatureProjectProvider) provider)
				.getPluginListSelection() != null
				? ((FeatureProjectProvider) provider).getPluginListSelection()
				: (new IPluginBase[0]);
		IRunnableWithProgress operation = new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) {
				try {
					createFeatureProject(project, location, data, plugins,
							monitor);
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(false, true, operation);
			BasicNewProjectResourceWizard
					.updatePerspective(((FeatureProjectProvider) provider)
							.getConfigElement());
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return true;

	}

	public void setInitializationData(IConfigurationElement config,
			String property, Object data) throws CoreException {
		this.config = config;
	}

	/* feature creation methods */

	protected static void addSourceFolder(String name, IProject project,
			IProgressMonitor monitor) throws CoreException {
		IPath path = project.getFullPath().append(name);
		ensureFolderExists(project, path, monitor);
		monitor.worked(1);
	}

	private static void ensureFolderExists(IProject project, IPath folderPath,
			IProgressMonitor monitor) throws CoreException {
		IWorkspace workspace = project.getWorkspace();

		for (int i = 1; i <= folderPath.segmentCount(); i++) {
			IPath partialPath = folderPath.uptoSegment(i);
			if (!workspace.getRoot().exists(partialPath)) {
				IFolder folder = workspace.getRoot().getFolder(partialPath);
				folder.create(true, true, null);
			}
			monitor.worked(1);
		}

	}
	private void createBuildProperties(IProject project) throws CoreException {
		StructureData structureData = structurePage.getStructureData();
		String fileName = "build.properties";
		IPath path = project.getFullPath().append(fileName);
		IFile file = project.getWorkspace().getRoot().getFile(path);
		if (!file.exists()) {
			WorkspaceBuildModel model = new WorkspaceBuildModel(file);
			IBuildEntry ientry = model.getFactory().createEntry("bin.includes");
			ientry.addToken("feature.xml");
			String library = structureData.getRuntimeLibraryName();
			if (library != null) {
				String source = structureData.getSourceFolderName();
				if (source != null) {
					IBuildEntry entry = model.getFactory().createEntry(
							IBuildEntry.JAR_PREFIX + library);
					if (!source.endsWith("/"))
						source += "/";
					entry.addToken(source);
					ientry.addToken(library);
					model.getBuild().add(entry);
				}
				String output = structureData.getJavaBuildFolderName();
				if (output != null) {
					IBuildEntry entry = model.getFactory().createEntry(
							IBuildPropertiesConstants.PROPERTY_OUTPUT_PREFIX
									+ library);
					if (!output.endsWith("/"))
						output += "/";
					entry.addToken(output);
					model.getBuild().add(entry);
				}
			}

			model.getBuild().add(ientry);
			model.save();
		}
		IDE.setDefaultEditor(file, PDEPlugin.BUILD_EDITOR_ID);
	}

	private IFile createFeatureManifest(IProject project, FeatureData data,
			IPluginBase[] plugins) throws CoreException {
		IFile file = project.getFile("feature.xml");
		WorkspaceFeatureModel model = new WorkspaceFeatureModel();
		model.setFile(file);
		IFeature feature = model.getFeature();
		String name = data.name;
		feature.setLabel(name);
		feature.setId(data.id);
		feature.setVersion(data.version);
		feature.setProviderName(data.provider);

		IFeaturePlugin[] added = new IFeaturePlugin[plugins.length];

		for (int i = 0; i < plugins.length; i++) {
			IPluginBase plugin = plugins[i];
			FeaturePlugin fplugin = (FeaturePlugin) model.getFactory()
					.createPlugin();
			fplugin.loadFrom(plugin);
			added[i] = fplugin;
		}
		feature.addPlugins(added);
		feature.computeImports();
		IFeatureInstallHandler handler = feature.getInstallHandler();
		if (handler == null) {
			handler = feature.getModel().getFactory().createInstallHandler();
			feature.setInstallHandler(handler);
		}
		StructureData structureData = structurePage.getStructureData();
		handler.setLibrary(structureData.getRuntimeLibraryName());

		// Save the model
		model.save();
		model.dispose();
		IDE.setDefaultEditor(file, PDEPlugin.FEATURE_EDITOR_ID);
		return file;
	}

	private void createFeatureProject(IProject project, IPath location,
			FeatureData data, IPluginBase[] plugins, IProgressMonitor monitor)
			throws CoreException {

		monitor.beginTask(PDEPlugin.getResourceString(CREATING_PROJECT), 3);
		StructureData structureData = structurePage.getStructureData();
		boolean overwrite = true;
		if (location.append(project.getName()).toFile().exists()) {
			overwrite = MessageDialog.openQuestion(PDEPlugin
					.getActiveWorkbenchShell(), PDEPlugin
					.getResourceString(KEY_WTITLE), PDEPlugin
					.getResourceString(OVERWRITE_FEATURE));
		}
		if (overwrite) {
			CoreUtility.createProject(project, location, monitor);
			project.open(monitor);
			IProjectDescription desc = project.getWorkspace()
					.newProjectDescription(project.getName());
			desc.setLocation(provider.getLocationPath());
			if (!project.hasNature(PDE.FEATURE_BUILDER_ID))
				CoreUtility.addNatureToProject(project, PDE.FEATURE_NATURE,
						monitor);

			if (!project.hasNature(JavaCore.NATURE_ID)
					&& structureData.hasCustomHandler()) {
				CoreUtility.addNatureToProject(project, JavaCore.NATURE_ID,
						monitor);
				JavaCore.create(project).setOutputLocation(
						project.getFullPath().append(
								structureData.getJavaBuildFolderName()),
						monitor);
				JavaCore
						.create(project)
						.setRawClasspath(
								new IClasspathEntry[]{
										JavaCore.newContainerEntry(new Path(
												JavaRuntime.JRE_CONTAINER)),
										JavaCore
												.newSourceEntry(project
														.getFullPath()
														.append(
																structureData
																		.getSourceFolderName()))},
								monitor);
				addSourceFolder(structureData.getSourceFolderName(), project,
						monitor);
			}

			monitor.subTask(PDEPlugin.getResourceString(CREATING_MANIFEST));
			monitor.worked(1);
			createBuildProperties(project);
			monitor.worked(1);
			// create feature.xml
			IFile file = createFeatureManifest(project, data, plugins);
			monitor.worked(1);
			// open manifest for editing
			openFeatureManifest(file);
		} else {
			project.create(monitor);
			project.open(monitor);
			IFile featureFile = project.getFile("feature.xml");
			if (featureFile.exists())
				openFeatureManifest(featureFile);
			monitor.worked(3);
		}

	}

	private void openFeatureManifest(IFile manifestFile) {
		IWorkbenchPage page = PDEPlugin.getActivePage();
		// Reveal the file first
		final ISelection selection = new StructuredSelection(manifestFile);
		final IWorkbenchPart activePart = page.getActivePart();

		if (activePart instanceof ISetSelectionTarget) {
			getShell().getDisplay().asyncExec(new Runnable() {
				public void run() {
					((ISetSelectionTarget) activePart).selectReveal(selection);
				}
			});
		}
		// Open the editor

		FileEditorInput input = new FileEditorInput(manifestFile);
		String id = PDEPlugin.FEATURE_EDITOR_ID;
		try {
			page.openEditor(input, id);
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}

}