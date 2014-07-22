package edu.stanford.bmir.protege.web.client.ui.icd.pc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.gwtext.client.widgets.Panel;

import edu.stanford.bmir.protege.web.client.model.Project;
import edu.stanford.bmir.protege.web.client.rpc.AbstractAsyncHandler;
import edu.stanford.bmir.protege.web.client.rpc.ICDServiceManager;
import edu.stanford.bmir.protege.web.client.rpc.data.EntityData;
import edu.stanford.bmir.protege.web.client.rpc.data.icd.PrecoordinationClassExpressionData;
import edu.stanford.bmir.protege.web.client.ui.portlet.PropertyWidget;
import edu.stanford.bmir.protege.web.client.ui.portlet.propertyForm.FormGenerator;
import edu.stanford.bmir.protege.web.client.ui.portlet.propertyForm.WidgetController;

public class PreCoordinationWidgetController extends WidgetController {

	private Project project;
	private Collection<PropertyWidget> widgets = null;

	public PreCoordinationWidgetController(Project project, Panel tabPanel,
			FormGenerator formGenerator) {
		super(tabPanel, formGenerator);
		this.project = project;
		// TODO Auto-generated constructor stub
	}

	
	public void initWidgets() {
		this.widgets = new ArrayList<PropertyWidget>();
	}

	public void setWidgets(Collection<PropertyWidget> widgets) {
		if (widgets == null || widgets.isEmpty()) {
			initWidgets();
		}
		else {
			this.widgets = new ArrayList<PropertyWidget>(widgets);
		}
	}

	public void addWidget(PropertyWidget widget) {
		if (this.widgets == null) {
			initWidgets();
		}
		this.widgets.add(widget);
	}

	protected Collection<PropertyWidget> getWidgets() {
		if (widgets == null) {
			return super.getWidgets();
		}
		else {
			return widgets;
		}
	}

	public void onSuperclassChanged(EntityData newSuperclass) {
		if (newSuperclass != null) {
			//TODO continue here widgetController.hideAllWidgets();
			getPossiblePostcoordinationAxes(newSuperclass);
		}
		else {
			//widgetController.hideAllWidgets();
			hideAllWidgets();
		}
	}


	private void getPossiblePostcoordinationAxes(EntityData superclass) {
		hideAllWidgets();
		ICDServiceManager.getInstance().getListOfSelectedPostCoordinationAxes(
				project.getProjectName(), superclass.getName(), (List<String>) null, new GetPostCoordinationAxesHandler());
		
	}
	
	private class GetPostCoordinationAxesHandler extends AbstractAsyncHandler<List<String>> {

		@Override
		public void handleFailure(Throwable caught) {
			// TODO Auto-generated method stub
			System.out.println("failure");
		}

		@Override
		public void handleSuccess(List<String> result) {
			// TODO Auto-generated method stub
			System.out.println(result.size() + " props: " + result);
			for (String prop : result) {
			//	widgetController.showWidgetForProperty(prop);
				showWidgetForProperty(prop);
			}
		}
		
	}

	public void onSubjectChanged(EntityData subject) {
		if (subject != null) {
			//TODO continue here widgetController.hideAllWidgets();
			getSuperclassValue();
			getPropertyValues(subject);
		}
		else {
			//widgetController.hideAllWidgets();
			hideAllWidgets();
		}
	}

	private void getSuperclassValue() {
		// TODO Auto-generated method stub
		
	}


	private void getPropertyValues(EntityData subject) {
		ICDServiceManager.getInstance().getPreCoordinationClassExpressions(
				project.getProjectName(), subject.getName(), 
						//Arrays.asList("http://who.int/icd#hasSeverity", "http://who.int/icd#timeInLife", "http://who.int/icd#infectiousAgent", "http://who.int/icd#specificAnatomy"),
						getAllProperties(),
				new AsyncCallback<List<PrecoordinationClassExpressionData>>() {
					
					@Override
					public void onSuccess(List<PrecoordinationClassExpressionData> res) {
						System.out.println(res);
						updateWidgetContents(res);
					}
					
					@Override
					public void onFailure(Throwable arg0) {
						System.out.println("Failed getPreCoordinationClassExpressions");
						
					}
				});
	}

	private void updateWidgetContents(
			List<PrecoordinationClassExpressionData> res) {
		List<String> allProperties = getAllProperties();
		for (PrecoordinationClassExpressionData classExprData : res) {
			String property = classExprData.getProperty().getName();
			PropertyWidget widget = getWidgetForProperty(property);
			setWidgetValue(widget, classExprData.getValue(), classExprData.isDefinitional());
			allProperties.remove(property);
		}
		//allProperties contains now those properties for which we did not have values
		//we need to set their values to null
		String ctrlProperty = getControllingWidget().getProperty().getName();
		for (String property : allProperties) {
			PropertyWidget widget = getWidgetForProperty(property);

			if (!ctrlProperty.equals(property)) {
				setWidgetValue(widget, null, false);
			}
		}
	}


	private void setWidgetValue(PropertyWidget widget,
			EntityData entityData, boolean isDefinitional) {
		if (entityData == null) {
			widget.setValues(null);
		}
		else {
			widget.setValues(Collections.singletonList(entityData));
		}

		if (widget instanceof ValueSelectorComponent) {
			((ValueSelectorComponent)widget).setIsDefinitional(isDefinitional);
		}
	}
}
