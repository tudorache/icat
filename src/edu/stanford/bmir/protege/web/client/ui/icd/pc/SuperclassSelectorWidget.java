package edu.stanford.bmir.protege.web.client.ui.icd.pc;

import java.util.Collection;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.gwtext.client.data.DataProxy;
import com.gwtext.client.data.MemoryProxy;

import edu.stanford.bmir.protege.web.client.model.Project;
import edu.stanford.bmir.protege.web.client.model.listener.OntologyListenerAdapter;
import edu.stanford.bmir.protege.web.client.rpc.ICDServiceManager;
import edu.stanford.bmir.protege.web.client.rpc.data.EntityData;
import edu.stanford.bmir.protege.web.client.rpc.data.PropertyEntityData;
import edu.stanford.bmir.protege.web.client.ui.portlet.propertyForm.RemoteValueComboBox;
import edu.stanford.bmir.protege.web.client.ui.util.UIUtil;

public class SuperclassSelectorWidget extends RemoteValueComboBox {

	private FillAllowedValuesCacheHandler fillValuesHandler = null;
	private SuperclassSelectorContainer<SuperclassSelectorWidget> preCoordinationWidget = null;	//reference to the PreCoordinationWidget that contains this, if any
    
    private PreCoordinationWidgetController<?> widgetController;	//external widget controller

	public SuperclassSelectorWidget(Project project) {
		super(project);
	}

	public void setWidgetController(PreCoordinationWidgetController<?> widgetController) {
		this.widgetController = widgetController;
	}
	
	@Override
	public void setup(Map<String, Object> widgetConfiguration,
			PropertyEntityData propertyEntityData) {
		GWT.log("Print setup superclass widget for subject: " + getSubject());
		super.setup(widgetConfiguration, propertyEntityData);
	}
	
	@Override
	protected boolean showCommentButton() {
		return false;
	}
	
	@Override
	public void fillValues() {
		if (isSameSubject() == true) {
			return;
		}
		
		super.fillValues();
		
		setLoadingStatus(true);
		ICDServiceManager.getInstance().getAllSuperEntities(getProject().getProjectName(), getSubject(), getFillValuesHandler());
		widgetController.onSubjectChanged(getSubject());
	}

	@Override
	public void setValues(Collection<EntityData> values) {
		Collection<EntityData> oldValues = getValues();
		super.setValues(values);
		GWT.log("Set values for superclass, subject: " + getSubject() + ", values: " + values);
		EntityData firstValue = null;
		if ( ! values.isEmpty()) {
			firstValue = values.iterator().next();
		}
		if ( UIUtil.differentCollections(oldValues, values) ) {
			selectionChanged(firstValue);
		}
	}	
	
	private FillAllowedValuesCacheHandler getFillValuesHandler() {
		if (fillValuesHandler != null) {
			return fillValuesHandler;
		}
		else {
			return fillValuesHandler = new FillAllowedValuesCacheHandler();
		}
	}

	public void setContainerWidget( SuperclassSelectorContainer<SuperclassSelectorWidget> preCoordinationWidget ) {
		this.preCoordinationWidget  = preCoordinationWidget;
	}

	@Override
	protected void onChangeValue(EntityData subj, Object oldVal, Object newVal) {
		super.onChangeValue(subj, oldVal, newVal);
		selectionChanged((EntityData)newVal);
	}
	
	private void selectionChanged(EntityData newVal) {
		if (preCoordinationWidget != null) {
			preCoordinationWidget.onSuperclassChanged(newVal);
		}
		if (widgetController != null) {
			widgetController.onSuperclassChanged(newVal);
		}
	}
	
	public String getSelection() {
		return getField().getValueAsString();
	}

	@Override
	protected void readConfiguration() {
		//Load widget specific configurations, if necessary
	}

	@Override
	protected DataProxy createProxy() {
        readConfiguration();

		MemoryProxy proxy = new MemoryProxy(new Object[][] {{}});
        return proxy;
	}

	@Override
	protected OntologyListenerAdapter getOntologyListener() {
		//We should watch for superclasses added/removed events,
		//but currently there is no support for such events, and 
		//it is not worth adding it at the moment. People can always
		//refresh the content of the containing portlet if they don't 
		//see newly added superclasses.
		
		return null;
	}

	@Override
	protected void cacheAllowedValues() {
		//TT - do nothing
	}
	
	@Override
	protected String getReplaceValueOperationDescription(EntityData subject, Object oldValue, Object newValue) {
		String oldValueText = oldValue == null || oldValue.toString().length() == 0 ? 
				"(empty)" : "'" + UIUtil.getDisplayText(oldValue) + "'";
		String newValueText = newValue == null || newValue.toString().length() == 0 ? 
				"(empty)" : "'" + UIUtil.getDisplayText(newValue) + "'";
		
		return UIUtil.getAppliedToTransactionString("Edited logical definition for class '" + UIUtil.getDisplayText(subject) + "'." +
			" Replaced the precoordination superclass." +
			" Old value: " + oldValueText +
			". New value: " + newValueText,
			subject.getName());
	}
	
	
	@Override
	protected String getDeleteValueOperationDescription() {
		return UIUtil.getAppliedToTransactionString("Edited logical definition for class '" + UIUtil.getDisplayText(getSubject()) + "'." +
			" Removed the precoordination superclass " +
			UIUtil.getDisplayText(getValues()),
			getSubject().getName());
		
	}
	
	@Override
	protected String getAddValueOperationDescription(EntityData subject, EntityData newVal) {
		 return UIUtil.getAppliedToTransactionString("Edited logical definition for class '" + UIUtil.getDisplayText(getSubject()) + "'." +
					" Set the precoordination superclass to '" +
					UIUtil.getDisplayText(newVal) + "'",
					getSubject().getName());
	}
}
