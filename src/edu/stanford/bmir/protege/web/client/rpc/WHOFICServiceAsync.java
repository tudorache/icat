package edu.stanford.bmir.protege.web.client.rpc;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import edu.stanford.bmir.protege.web.client.rpc.data.EntityData;
import edu.stanford.bmir.protege.web.client.rpc.data.EntityPropertyValues;
import edu.stanford.bmir.protege.web.client.rpc.data.EntityPropertyValuesList;
import edu.stanford.bmir.protege.web.client.rpc.data.SubclassEntityData;
import edu.stanford.bmir.protege.web.client.rpc.data.icd.AllowedPostcoordinationValuesData;
import edu.stanford.bmir.protege.web.client.rpc.data.icd.PrecoordinationClassExpressionData;
import edu.stanford.bmir.protege.web.client.rpc.data.icd.ScaleInfoData;

public interface WHOFICServiceAsync {
	
	void getEntity(String projectName, String entityName, AsyncCallback<EntityData> cb);

    void getEntityPropertyValuesForLinearization(String projectName, List<String> entities, String property,
            List<String> reifiedProps, int[] subjectEntityColumns, AsyncCallback<List<EntityPropertyValuesList>> cb);

    void getSecondaryAndInheritedTags(String projectName, String clsName, AsyncCallback<List<EntityPropertyValues>> cb);

    void getSubclasses(String projectName, String className, AsyncCallback<List<SubclassEntityData>> cb);

    void changeIndexType(String projectName, String subject, String indexEntity, List<String> reifiedProps,
            String indexType, AsyncCallback<EntityPropertyValues> cb);

    void changeInclusionFlagForIndex(String projectName, String subject, String indexEntity, List<String> reifiedProps,
            boolean isInclusionFlag, AsyncCallback<EntityPropertyValues> callback);

    void removeBaseIndexTerm(String projectName, String entityName,
            String value, String user, String operationDescription, AsyncCallback<Void> cb);

    void getListOfSelectedPostCoordinationAxes(String projectName,
			String entity, List<String> reifiedProps, AsyncCallback<List<String>> cb);

    void getEntityPropertyValuesForPostCoordinationAxes(String projectName, List<String> entities, List<String> properties,
    		List<String> reifiedProps, AsyncCallback<List<EntityPropertyValues>> cb);

    void addAllowedPostCoordinationAxis(String projectName, String subject,
			String postcoordinationEntity, String postcoordinationProperty,
			boolean isRequiredFlag, AsyncCallback<Boolean> cb);

    void removeAllowedPostCoordinationAxis(String projectName, String subject,
			String postcoordinationEntity, String postcoordinationProperty, AsyncCallback<Boolean> cb);

	void getPostCoordinationAxesScales(String projectName,
			List<String> properties, AsyncCallback<List<ScaleInfoData>> cb);

	void getAllSuperEntities(String projectName, EntityData entity,
			AsyncCallback<List<EntityData>> callback);

	void removeLogicalDefinitionSuperclass(String projectName, String entity, String precoordSuperclass,
			AsyncCallback<Void> cb);

	void removeLogicalDefinitionForSuperclass(String projectName, String entity, String precoordSuperclass,
			AsyncCallback<Void> callback);

	@Deprecated
	void getPreCoordinationClassExpressions(String projectName,
			String entity, List<String> properties,
			AsyncCallback<List<PrecoordinationClassExpressionData>> cb);

	void getPreCoordinationClassExpressions(String projectName, 
			String entity, String precoordSuperclass, List<String> properties, 
			AsyncCallback<List<PrecoordinationClassExpressionData>> cb);

	void getAllowedPostCoordinationValues(String projectName, String precoordSuperclassName,
			List<String> customScaleProperties,
			List<String> treeValueProperties,
			List<String> fixedScaleProperties,
			AsyncCallback<List<AllowedPostcoordinationValuesData>> cb);

	void setPrecoordinationPropertyValue(String projectName, String entity, String property, String precoordSuperclass,
			EntityData oldValue, EntityData newValue, boolean isDefinitional, String user, String operationDescription,
			AsyncCallback<Boolean> cb);

	void changeIsDefinitionalFlag(String projectName, String entity, String precoordSuperclassName,
			String property, boolean isDefinitionalFlag,
			AsyncCallback<Boolean> cb);

    void reorderSiblings(String projectName, String movedClass, String targetClass, boolean isBelow, String parent,
            AsyncCallback<Boolean> cb);

	void createInternalReference(String projectName, EntityData entity,
			String referenceClassName,
			String referencePropertyName,
			String referencedValuePropertyName,
			EntityData referencedEntity, String user,
			String operationDescription, AsyncCallback<EntityData> callback);

}
