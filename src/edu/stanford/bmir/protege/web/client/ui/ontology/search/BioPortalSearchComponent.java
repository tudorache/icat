package edu.stanford.bmir.protege.web.client.ui.ontology.search;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.gwtext.client.core.EventObject;
import com.gwtext.client.core.Position;
import com.gwtext.client.data.FieldDef;
import com.gwtext.client.data.Record;
import com.gwtext.client.data.RecordDef;
import com.gwtext.client.data.Store;
import com.gwtext.client.data.StringFieldDef;
import com.gwtext.client.data.XmlReader;
import com.gwtext.client.widgets.Button;
import com.gwtext.client.widgets.Component;
import com.gwtext.client.widgets.MessageBox;
import com.gwtext.client.widgets.Panel;
import com.gwtext.client.widgets.Toolbar;
import com.gwtext.client.widgets.ToolbarButton;
import com.gwtext.client.widgets.ToolbarTextItem;
import com.gwtext.client.widgets.Window;
import com.gwtext.client.widgets.event.ButtonListenerAdapter;
import com.gwtext.client.widgets.form.Field;
import com.gwtext.client.widgets.form.TextField;
import com.gwtext.client.widgets.form.event.TextFieldListenerAdapter;
import com.gwtext.client.widgets.grid.CellMetadata;
import com.gwtext.client.widgets.grid.ColumnConfig;
import com.gwtext.client.widgets.grid.ColumnModel;
import com.gwtext.client.widgets.grid.GridPanel;
import com.gwtext.client.widgets.grid.Renderer;
import com.gwtext.client.widgets.grid.event.GridCellListenerAdapter;
import com.gwtext.client.widgets.layout.FitLayout;

import edu.stanford.bmir.protege.web.client.model.GlobalSettings;
import edu.stanford.bmir.protege.web.client.model.Project;
import edu.stanford.bmir.protege.web.client.rpc.AbstractAsyncHandler;
import edu.stanford.bmir.protege.web.client.rpc.BioPortalAccessManager;
import edu.stanford.bmir.protege.web.client.rpc.data.BioPortalReferenceData;
import edu.stanford.bmir.protege.web.client.rpc.data.BioPortalSearchData;
import edu.stanford.bmir.protege.web.client.rpc.data.EntityData;
import edu.stanford.bmir.protege.web.client.rpc.data.NotesData;
import edu.stanford.bmir.protege.web.client.rpc.data.PropertyEntityData;
import edu.stanford.bmir.protege.web.client.ui.ontology.notes.NoteInputPanel;
import edu.stanford.bmir.protege.web.client.ui.portlet.propertyForm.ReferenceFieldWidget;
import edu.stanford.bmir.protege.web.client.ui.util.UIUtil;

public class BioPortalSearchComponent extends GridPanel {

    private Project project;
    private EntityData currentEntity;
    private EntityData property;

    private Map<String, Object> configPropertiesMap;
    private Store store;

    private TextField searchStringTextField;
    private ToolbarButton searchButton;
    private ToolbarButton searchAllButton;
    private ToolbarButton createDNFRefButton;
    private ToolbarTextItem searchCountText;

    private boolean ignoreSearchAllPressed = false;
    private boolean searchAll = false;
    private boolean replaceExisting = false;
    private final boolean isSingleValued;

    private String currentValue; //TODO: logic is inverted - should not be here but in the widget; import should call a callback


    public BioPortalSearchComponent(Project project, boolean isSingleValued) {
        this(project, null, new PropertyEntityData(null), isSingleValued);
    }

    public BioPortalSearchComponent(Project project, ReferenceFieldWidget referenceFieldWidget,
            PropertyEntityData referenceProperty, boolean isSingleValued) {
        this.project = project;
        this.isSingleValued = isSingleValued;
        createGrid();
    }

    public void setConfigProperties(Map<String, Object> configPropertiesMap) {
        if (configPropertiesMap != null) {
            this.configPropertiesMap = configPropertiesMap;
        } else {
            GWT.log("The argument passed to setConfigurationProperties should not be null!", new NullPointerException(
                    "configPropertiesMap is null"));
            this.configPropertiesMap = new HashMap<String, Object>();
        }
    }

    private void createGrid() {
        XmlReader reader = new XmlReader("searchBean", new RecordDef(new FieldDef[] { new StringFieldDef("contents"),
                new StringFieldDef("recordType"), new StringFieldDef("ontologyDisplayLabel"),
                new StringFieldDef("ontologyVersionId"), new StringFieldDef("preferredName"), new StringFieldDef("conceptIdShort"),
                new StringFieldDef("conceptId")}));

        store = new Store(reader);

        //setup column model
        ColumnConfig conceptIdShortCol = new ColumnConfig("Id", "conceptIdShort");
        ColumnConfig preferredNameCol = new ColumnConfig("Preferred Name", "preferredName");
        preferredNameCol.setId("preferredName");
        ColumnConfig contentsCol = new ColumnConfig("Matched content", "contents");
        ColumnConfig recordTypeCol = new ColumnConfig("Found in", "recordType");
        ColumnConfig ontologyCol = new ColumnConfig("Ontology", "ontologyDisplayLabel");
        ColumnConfig detailsCol = new ColumnConfig(" ", "viewDetails");
        ColumnConfig graphCol = new ColumnConfig(" ", "viewGraph");
        ColumnConfig importCol = new ColumnConfig(" ", "importLink");

        recordTypeCol.setWidth(100);
        ontologyCol.setWidth(150);
        detailsCol.setWidth(25);
        graphCol.setWidth(30);
        importCol.setWidth(60);

        preferredNameCol.setRenderer(new Renderer() {
            public String render(Object value, CellMetadata cellMetadata, Record record, int rowIndex, int colNum,
                    Store store) {
                String text = record.getAsString("preferredName");
                return "<span class=\"bp-search-pref-name\">" + text +"</span>";
            }
        });

        contentsCol.setRenderer(new Renderer() {
            public String render(Object value, CellMetadata cellMetadata, Record record, int rowIndex, int colNum,
                    Store store) {
                String text = record.getAsString("contents");
                return "<span class=\"bp-search-contents\">" + text +"</span>";
            }
        });

        conceptIdShortCol.setRenderer(new Renderer() {
            public String render(Object value, CellMetadata cellMetadata, Record record, int rowIndex, int colNum,
                    Store store) {
                return "<a href= \"" + getBioPortalVisualizeURL() + record.getAsString("ontologyVersionId") + "/"
                + "?conceptid=" + URL.encodeComponent(record.getAsString("conceptIdShort")) + "\" target=\"_blank\">"
                + UIUtil.getShortName(record.getAsString("conceptIdShort")) + "</a>";
            }
        });


        recordTypeCol.setRenderer(new Renderer() {
            public String render(Object value, CellMetadata cellMetadata, Record record, int rowIndex, int colNum,
                    Store store) {
                String type = BioPortalConstants.getRecordTypePrintText(record.getAsString("recordType"));
                return "<span class=\"bp-search-rec-type\">" + type +"</span>";
            }
        });

        ontologyCol.setRenderer(new Renderer() {
            public String render(Object value, CellMetadata cellMetadata, Record record, int rowIndex, int colNum,
                    Store store) {
                return "<a href= \"" + getBioPortalOntologyURL() + record.getAsString("ontologyVersionId")
                        + "\" target=\"_blank\">" + record.getAsString("ontologyDisplayLabel") + "</a>";
            }
        });

        detailsCol.setRenderer(new Renderer() {
            public String render(Object value, CellMetadata cellMetadata, Record record, int rowIndex, int colNum,
                    Store store) {
                return "<img src=\"images/details.png\"></img>";
            }

        });

        graphCol.setRenderer(new Renderer() {
            public String render(Object value, CellMetadata cellMetadata, Record record, int rowIndex, int colNum,
                    Store store) {
                return "<img src=\"images/graph.png\"></img>";
            }

        });

        importCol.setRenderer(new Renderer() {
            public String render(Object value, CellMetadata cellMetadata, final Record record, int rowIndex,
                    int colNum, Store store) {
                // the return string may contain ONLY ONE HTML TAG before the text,
                // otherwise GridCellListener would not receive the onClick event!
                return "<DIV style=\"color:#1542bb;text-decoration:underline;font-weight:bold\">Import</DIV>";
            }
        });
        importCol.setSortable(false);

        ColumnConfig[] columnConfigs = { conceptIdShortCol, preferredNameCol, recordTypeCol, contentsCol, ontologyCol, detailsCol, graphCol, importCol };

        ColumnModel columnModel = new ColumnModel(columnConfigs);
        columnModel.setDefaultSortable(true);

        setHeight(200);
        setStore(store);
        setColumnModel(columnModel);
        setAutoWidth(true);
        stripeRows(true);
        setAutoExpandColumn("preferredName");

        addGridCellListener(new GridCellListenerAdapter() {
            @Override
            public void onCellClick(GridPanel grid, int rowIndex, int colindex, EventObject e) {
                if (grid.getColumnModel().getDataIndex(colindex).equals("importLink")) {
                    if ( UIUtil.checkWriteOperationAllowed(project, true) ) {
                        Record record = grid.getStore().getAt(rowIndex);
                        onImportReference(record);
                    }
                } else if (grid.getColumnModel().getDataIndex(colindex).equals("viewDetails")) {
                    Record record = grid.getStore().getAt(rowIndex);
                    onViewDetails(record);
                } else if (grid.getColumnModel().getDataIndex(colindex).equals("viewGraph")) {
                    Record record = grid.getStore().getAt(rowIndex);
                    onViewGraph(record);
                }
            }
        });

        searchStringTextField = new TextField();
        searchStringTextField.addListener(new TextFieldListenerAdapter() {
            @Override
            public void onSpecialKey(Field field, EventObject e) {
                if (e.getKey() == EventObject.ENTER) {
                    reload();
                }
            }
        });
        searchStringTextField.setWidth(250);

        searchButton = new ToolbarButton(createLinkFont("Search in BioPortal", false));
        searchButton.addListener(new ButtonListenerAdapter() {
            @Override
            public void onClick(Button button, EventObject e) {
                reload();
            }
        });

        Toolbar topToolbar = new Toolbar();
        topToolbar.addText("&nbsp<i>Search for concept</i>:&nbsp&nbsp");
        topToolbar.addElement(searchStringTextField.getElement());
        topToolbar.addSpacer();
        topToolbar.addButton(searchButton);
        setTopToolbar(topToolbar);

        searchAllButton = new ToolbarButton(createLinkFont(BioPortalConstants.SHOW_ALL_BUTTON_TEXT, false));
        searchAllButton.setEnableToggle(true);
        searchAllButton.addListener(new ButtonListenerAdapter() {
            @Override
            public void onToggle(Button button, boolean pressed) {
                searchAll = pressed;
                if (!ignoreSearchAllPressed) {
                    reload();
                }
            }
        });

        createDNFRefButton = new ToolbarButton(createLinkFont(BioPortalConstants.DNF_BUTTON_TEXT, false));
        createDNFRefButton.addListener(new ButtonListenerAdapter() {
            @Override
            public void onClick(Button button, EventObject e) {
                onLeaveAComment();
            }
        });

        searchCountText = new ToolbarTextItem("No results");

        Toolbar toolbar = new Toolbar();
        toolbar.addItem(searchCountText);
        toolbar.addButton(searchAllButton);
        toolbar.addFill();
        toolbar.addButton(createDNFRefButton);
        setBottomToolbar(toolbar);

    }

    private String createLinkFont(String text, boolean alert) {
        final String blue_link = "#1542bb";
        final String red_link = "#bb4215";

        return "<font color='" + (alert ? red_link : blue_link) + "'><b><u>" + text + "</u></b></font>";
    }

    protected void onImportReference(Record record) {
        BioPortalReferenceData bpRefData = createBioPortalReferenceDataFromRecord(record);
        if(replaceExisting && isSingleValued){
            EntityData oldValueEntityData = new EntityData(currentValue);
            BioPortalAccessManager.getInstance().replaceExternalReference(project.getProjectName(), currentEntity.getName(), bpRefData,
                    oldValueEntityData, GlobalSettings.getGlobalSettings().getUserName(),
                    getReplaceReferenceApplyToString(bpRefData, oldValueEntityData),
                    getImportBioPortalConceptHandler());
        } else {
            BioPortalAccessManager.getInstance().createExternalReference(project.getProjectName(), currentEntity.getName(), bpRefData,
                    GlobalSettings.getGlobalSettings().getUserName(),
                    getImportReferenceApplyToString(bpRefData),
                    getImportBioPortalConceptHandler());
            replaceExisting = true;

        }
    }

    protected String getImportReferenceApplyToString(BioPortalReferenceData bpRefData) {
        return UIUtil.getAppliedToTransactionString("Imported reference for " + UIUtil.getDisplayText(currentEntity) +
                " and property " + UIUtil.getDisplayText(getProperty()) +". Reference: " + bpRefData.getPreferredName() + ", code: " + bpRefData.getConceptId(),
                getEntity().getName());
    }

    /*
     * TODO: The apply to string has to be fixed, when we refactor his class.
     * It should show the old value, but currently we only have the old value full name which is not user friendly.
     * This code should not be here, but in the external reference grid, where we have access to all the info.
     */
    protected String getReplaceReferenceApplyToString(BioPortalReferenceData bpRefData, EntityData oldValue) {
        return UIUtil.getAppliedToTransactionString("Replaced reference for " + getEntity().getBrowserText() +
                 " New reference: " + bpRefData.getPreferredName() + ", code: " + bpRefData.getConceptId(),
                getEntity().getName());
    }


    private void onViewDetails(Record record) {
        GWT.log("onViewDetails", null);
        final Window window = new Window();
        window.setWidth(550);
        window.setHeight(500);
        window.setLayout(new FitLayout());
        final Panel panel = new Panel();
        panel.setAutoScroll(true);
        window.add(panel);
        window.show();
        panel.getEl().mask("Loading details...");

        BioPortalSearchData bpSearchData = new BioPortalSearchData();
        initBioPortalSearchData(bpSearchData);
        BioPortalReferenceData bpRefData = createBioPortalReferenceDataFromRecord(record);

        BioPortalAccessManager.getInstance().getBioPortalSearchContentDetails(project.getProjectName(), bpSearchData,
                bpRefData, new AsyncCallback<String>() {
                    public void onFailure(Throwable caught) {
                        panel.getEl().unmask();
                        window.close();
                        MessageBox.alert("Getting details from server failed. Please try it later.<BR>"
                                + "Reason for failure: " + caught.getMessage());
                    }

                    public void onSuccess(String result) {
                        panel.getEl().unmask();
                        panel.setHtml(result);
                    }
                });
    }

    private void onViewGraph(Record record) {
        GWT.log("onViewGraph", null);
        Window window = new Window();
        window.setWidth(800);
        window.setHeight(550);
        String ontologyVersionId = record.getAsString("ontologyVersionId");
        String conceptIdShort = record.getAsString("conceptIdShort");
        String bpRestBaseUrl = getBioPortalRestBaseURL();
        window.add(getViewGraphContent(ontologyVersionId, conceptIdShort, bpRestBaseUrl));
        window.show();
    }

    private Component getViewGraphContent(String ontologyVersionId, String conceptIdShort, String bpRestBaseUrl) {
        Panel html = new Panel();

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<html><body>");
        stringBuilder.append("<iframe " + "width=\"770\" " + "height=\"520\" " + "frameborder=\"0\" "
                + "scrolling=\"no\" " + "marginwidth=\"0\" " + "marginheight=\"0\" "
                + "src=\"http://keg.cs.uvic.ca/ncbo/flexviz/BasicFlexoViz.html" + "?ontology=");
        stringBuilder.append(ontologyVersionId);
        stringBuilder.append("&virtual=false");
        stringBuilder.append("&nodeid=");
        stringBuilder.append(URL.encodeComponent(conceptIdShort));
        stringBuilder.append("&show=Neighborhood");
        stringBuilder.append("&server=");
        stringBuilder.append(bpRestBaseUrl);
        stringBuilder.append("\">" + "</iframe></body></html>");
        /* */
        html.setHtml(stringBuilder.toString());
        return html;
    }

    public static String replaceSpaces(String text) {
        return text.replaceAll(" ", "%20");
    }

    private void onLeaveAComment() {
        createReferenceIfUserComments(project);
    }

    public EntityData getEntity() {
        return currentEntity;
    }

    public void setEntity(EntityData newEntity) {
        setEntity(newEntity, true);
    }

    public void setEntity(EntityData newEntity, boolean refreshUI) {
        if (currentEntity != null && currentEntity.equals(newEntity)) {
            return;
        }
        searchAll = false;
        ignoreSearchAllPressed = true;
        searchAllButton.setPressed(false);
        ignoreSearchAllPressed = false;
        searchCountText.setText("No results");
        currentEntity = newEntity;

        String defSearchString = (String) configPropertiesMap.get(BioPortalConstants.CONFIG_PROPERTY_DEFAULT_SEARCH_STRING);
        if (defSearchString == null) {
            searchStringTextField.setValue(currentEntity.getBrowserText());
        } else if (defSearchString.equals(DefaultSearchStringTypeEnum.None.toString())) {
            searchStringTextField.setValue("");
        } else if (defSearchString.equals(DefaultSearchStringTypeEnum.Entity.toString())) {
            searchStringTextField.setValue(currentEntity.getBrowserText());
        } else if (defSearchString.startsWith(DefaultSearchStringTypeEnum.Property.toString())) {
            /* TODO get the name of the property of the  entity that we wish to display
            String propName = defSearchString.substring(DefaultSearchStringType.Property.toString().length());
            if (propName.equals("BrowserText")) {
                searchStringTextField.setValue(_entity.getBrowserText());
            } else if () {
                ....
            }
            */
        } else {
            searchStringTextField.setValue(defSearchString);
        }
        if (refreshUI) {
            reload();
        }
    }

    public EntityData getProperty() {
        return property;
    }

    public void setProperty(EntityData property) {
        this.property = property;
    }

    public void setReplaceExisting(boolean replace) {
        replaceExisting = replace;
    }

    public void setCurrentValue(String currentValue) {
        this.currentValue = currentValue;
    }

    protected void reload() {
        store.removeAll();

        String searchString = searchStringTextField.getText();
        if (searchString != null && searchString.length() > 0) {
            if (getEl() != null) {
                getEl().mask("Loading search results", true);
            }
            if (configPropertiesMap != null) {
                BioPortalSearchData bpSearchData = new BioPortalSearchData();
                initBioPortalSearchData(bpSearchData);
                BioPortalAccessManager.getInstance().getBioPortalSearchContent(project.getProjectName(), searchString,
                        bpSearchData, new GetSearchURLContentHandler());
            } else {
                GWT.log("configPropertiesMap should have been initialized!", new Exception(
                        "reload() method called before configPropertiesMap has been initialized."));
            }
        }
    }

    private void initBioPortalSearchData(BioPortalSearchData bpSearchData) {
        bpSearchData.setBpRestBaseUrl(getBioPortalRestBaseURL());
        bpSearchData.setBpRestCallSuffix(getBioPortalRestCallSuffix());
        bpSearchData.setSearchOntologyIds((String) configPropertiesMap.get(BioPortalConstants.CONFIG_PROPERTY_SEARCH_ONTOLOGY_IDS));
        bpSearchData.setSearchOptions(getBioPortalSearchOptions());
        bpSearchData.setSearchPageOption(getBioPortalSearchPageOption(searchAll));
    }

    private String getBioPortalRestBaseURL() {
        String res = BioPortalConstants.DEFAULT_BIOPORTAL_REST_BASE_URL;
        if (configPropertiesMap != null) {
            res = (String) configPropertiesMap.get(BioPortalConstants.CONFIG_PROPERTY_BIOPORTAL_REST_BASE_URL);
            if (res == null) {
                res = BioPortalConstants.DEFAULT_BIOPORTAL_REST_BASE_URL;
            }
        }

        return res;
    }

    private String getBioPortalRestCallSuffix() {
        String res = BioPortalConstants.DEFAULT_BIOPORTAL_REST_CALL_SUFFIX;
        if (configPropertiesMap != null) {
            res = (String) configPropertiesMap.get(BioPortalConstants.CONFIG_PROPERTY_BIOPORTAL_REST_CALL_SUFFIX);
            if (res == null) {
                res = BioPortalConstants.DEFAULT_BIOPORTAL_REST_CALL_SUFFIX;
            }
        }

        return res;
    }

    private String getBioPortalOntologyURL() {
        String res = BioPortalConstants.DEFAULT_BIOPORTAL_ONTOLOGY_URL;
        if (configPropertiesMap != null) {
            res = (String) configPropertiesMap.get(BioPortalConstants.CONFIG_PROPERTY_BIOPORTAL_BASE_URL);
            if (res != null) {
                res = res + BioPortalConstants.BP_ONTOLOGY_STR + "/";
            } else {
                res = BioPortalConstants.DEFAULT_BIOPORTAL_ONTOLOGY_URL;
            }
        }

        return res;
    }

    private String getBioPortalVisualizeURL() {
        String res = BioPortalConstants.DEFAULT_BIOPORTAL_VISUALIZE_URL;
        if (configPropertiesMap != null) {
            res = (String) configPropertiesMap.get(BioPortalConstants.CONFIG_PROPERTY_BIOPORTAL_BASE_URL);
            if (res != null) {
                res = res + BioPortalConstants.BP_VISUALIZE_STR + "/";
            } else {
                res = BioPortalConstants.DEFAULT_BIOPORTAL_VISUALIZE_URL;
            }
        }

        return res;
    }

    private String getBioPortalSearchOptions() {
        String res = "";
        if (configPropertiesMap != null) {
            res = (String) configPropertiesMap.get(BioPortalConstants.CONFIG_PROPERTY_SEARCH_OPTIONS);
            if (res == null) {
                res = "";
            }
        }

        return res;
    }

    private String getBioPortalSearchPageOption(boolean all) {
        if (all) {
            //do not restrict search pages
            return null;
        }
        String res = BioPortalConstants.DEFAULT_BIOPORTAL_SEARCH_ONE_PAGE_OPTION;
        if (configPropertiesMap != null) {
            res = (String) configPropertiesMap.get(BioPortalConstants.CONFIG_PROPERTY_SEARCH_ONE_PAGE_OPTION);
            if (res == null) {
                res = BioPortalConstants.DEFAULT_BIOPORTAL_SEARCH_ONE_PAGE_OPTION;
            }
        }

        return res;
    }

    public void createReferenceIfUserComments(final Project project) {

        final Window window = new Window();
        window.setTitle("Comment on reference");
        window.setWidth(600);
        window.setHeight(350);
        window.setMinWidth(300);
        window.setMinHeight(250);
        window.setLayout(new FitLayout());
        window.setPaddings(5);
        window.setButtonAlign(Position.CENTER);

        //window.setCloseAction(Window.HIDE);
        window.setPlain(true);

        EntityData refInstance = null; //refInstance is not created at this point

        final NoteInputPanel nip = new NoteInputPanel(project, "Enter a comment about this reference (optional):",
                false, refInstance, window, new AsyncCallback<NotesData>() {
                    public void onFailure(Throwable caught) {
                        if (caught != null) {
                            MessageBox.alert(caught.getMessage());
                        }
                    }

                    public void onSuccess(NotesData note) {
                        if (note != null
                                && ((note.getSubject() != null && note.getSubject().length() > 0) || (note.getBody() != null && note
                                        .getBody().length() > 0))) {
                            createDNFReference(note);
                        }
                    }
                });
        window.add(nip);

        window.show();
        nip.getMainComponentForFocus().focus();
    }

    public void createDNFReference(NotesData note) {
        GWT.log("onCreateDNFReference", null);
        BioPortalReferenceData bpRefData = new BioPortalReferenceData();

        initBioPortalReferenceData(bpRefData);

        bpRefData.setBpUrl(null);
        bpRefData.setConceptId(BioPortalConstants.DNF_CONCEPT_ID);
        bpRefData.setConceptIdShort(BioPortalConstants.DNF_CONCEPT_ID_SHORT);
        bpRefData.setOntologyVersionId(null);
        bpRefData.setOntologyName(null);
        bpRefData.setPreferredName(BioPortalConstants.DNF_CONCEPT_LABEL);
        bpRefData.setBpUrl(null);//do not use the BP rest URL to find out more information about this concept
        BioPortalAccessManager.getInstance().createExternalReference(
                project.getProjectName(),
                currentEntity.getName(),
                bpRefData,
                GlobalSettings.getGlobalSettings().getUserName(),
                UIUtil.getAppliedToTransactionString("Create a 'Did not find' reference on "
                        + getEntity().getBrowserText() + " "
                        + (String) configPropertiesMap.get(BioPortalConstants.CONFIG_PROPERTY_REFERENCE_PROPERTY), getEntity().getName()),
                getCreateDNFConceptHandler(note));
    }

    public void createReference(String ontologyVersionId, String conceptId, String conceptIdShort, String preferredName, String url) {
        GWT.log("onCreateReference", null);
        BioPortalReferenceData bpRefData = new BioPortalReferenceData();

        initBioPortalReferenceData(bpRefData);

        bpRefData.setBpUrl(url);
        bpRefData.setConceptId(conceptId);
        bpRefData.setConceptIdShort(conceptIdShort);
        bpRefData.setOntologyVersionId(ontologyVersionId);
        bpRefData.setOntologyName(null);
        bpRefData.setPreferredName(preferredName);
        bpRefData.setBpUrl(null);//do not use the BP rest URL to find out more information about this concept
        BioPortalAccessManager.getInstance().createExternalReference(
                project.getProjectName(),
                currentEntity.getName(),
                bpRefData,
                GlobalSettings.getGlobalSettings().getUserName(),
                UIUtil.getAppliedToTransactionString("Created reference on " + getEntity().getBrowserText() + " "
                        + (String) configPropertiesMap.get(BioPortalConstants.CONFIG_PROPERTY_REFERENCE_PROPERTY), getEntity().getName()),
                getCreateManualreferenceHandler());
    }

    public void replaceReference(String ontologyVersionId, String conceptId, String conceptIdShort, String preferredName, String url, String oldInstanceName) {
        GWT.log("onCreateReference", null);
        BioPortalReferenceData bpRefData = new BioPortalReferenceData();

        initBioPortalReferenceData(bpRefData);

        bpRefData.setBpUrl(url);
        bpRefData.setConceptId(conceptId);
        bpRefData.setConceptIdShort(conceptIdShort);
        bpRefData.setOntologyVersionId(ontologyVersionId);
        bpRefData.setOntologyName(null);
        bpRefData.setPreferredName(preferredName);
        bpRefData.setBpUrl(null);//do not use the BP rest URL to find out more information about this concept

        EntityData oldValueEntityData = new EntityData(oldInstanceName);
        BioPortalAccessManager.getInstance().replaceExternalReference(
                project.getProjectName(),
                currentEntity. getName(),
                bpRefData,
                oldValueEntityData,
                GlobalSettings.getGlobalSettings().getUserName(),
                UIUtil.getAppliedToTransactionString("Created reference on " + getEntity().getBrowserText() + " "
                        + (String) configPropertiesMap.get(BioPortalConstants.CONFIG_PROPERTY_REFERENCE_PROPERTY), getEntity().getName()),
                getCreateManualreferenceHandler());
    }

    private BioPortalReferenceData createBioPortalReferenceDataFromRecord(Record record) {
        String ontologyVersionId = record.getAsString("ontologyVersionId");
        String conceptId = record.getAsString("conceptId");
        String conceptIdShort = record.getAsString("conceptIdShort");
        String ontologyName = record.getAsString("ontologyDisplayLabel");
        String preferredName = record.getAsString("preferredName");

        BioPortalReferenceData bpRefData = new BioPortalReferenceData();

        initBioPortalReferenceData(bpRefData);

        String url = getBioPortalVisualizeURL() + ontologyVersionId + "/?conceptid=" + URL.encodeComponent(conceptId);
        bpRefData.setBpUrl(url);

        bpRefData.setConceptId(conceptId);
        bpRefData.setConceptIdShort(conceptIdShort);
        bpRefData.setOntologyVersionId(ontologyVersionId);
        bpRefData.setOntologyName(ontologyName);
        bpRefData.setPreferredName(preferredName);
        bpRefData.setBpRestBaseUrl(getBioPortalRestBaseURL());
        bpRefData.setBpRestCallSuffix(getBioPortalRestCallSuffix());

        return bpRefData;
    }

    private void initBioPortalReferenceData(BioPortalReferenceData bpRefData) {
        bpRefData.setReferenceClassName((String) configPropertiesMap.get(BioPortalConstants.CONFIG_PROPERTY_REFERENCE_CLASS));
        bpRefData.setReferencePropertyName((String) configPropertiesMap.get(BioPortalConstants.CONFIG_PROPERTY_REFERENCE_PROPERTY));
        bpRefData.setUrlPropertyName((String) configPropertiesMap.get(BioPortalConstants.CONFIG_PROPERTY_URL_PROPERTY));
        bpRefData.setOntologyNamePropertyName((String) configPropertiesMap.get(BioPortalConstants.CONFIG_PROPERTY_ONTOLOGY_NAME_PROPERTY));
        bpRefData.setOntologyNameAltPropertyName((String) configPropertiesMap
                .get(BioPortalConstants.CONFIG_PROPERTY_ONTOLOGY_NAME_ALT_PROPERTY));
        bpRefData.setOntologyIdPropertyName((String) configPropertiesMap.get(BioPortalConstants.CONFIG_PROPERTY_ONTOLGY_ID_PROPERTY));
        bpRefData.setConceptIdPropertyName((String) configPropertiesMap.get(BioPortalConstants.CONFIG_PROPERTY_CONCEPT_ID_PROPERTY));
        bpRefData
                .setConceptIdAltPropertyName((String) configPropertiesMap.get(BioPortalConstants.CONFIG_PROPERTY_CONCEPT_ID_ALT_PROPERTY));
        bpRefData.setPreferredLabelPropertyName((String) configPropertiesMap
                .get(BioPortalConstants.CONFIG_PROPERTY_PREFERRED_LABEL_PROPERTY));

        bpRefData.setImportFromOriginalOntology(getImportFromOriginalOntology());
    }

    public boolean getImportFromOriginalOntology() {
        Boolean b = (Boolean) configPropertiesMap.get(BioPortalConstants.CONFIG_PROPERTY_IMPORT_FROM_ORIGINAL_ONTOLOGY);
        return b == null ? BioPortalConstants.DEFAULT_IMPORT_FROM_ORIGINAL_ONTOLOGIES : b.booleanValue();
    }

    class GetSearchURLContentHandler extends AbstractAsyncHandler<String> {
        @Override
        public void handleFailure(Throwable caught) {
            if (getEl() != null) { //how can it be null?
                getEl().unmask();
            }
            GWT.log("Could not retrive BioPortal search results for " + currentEntity, null);
        }

        @Override
        public void handleSuccess(String searchXml) {
            if (getEl() != null) { //how can it be null?
                getEl().unmask();
            }
            store.loadXmlData(searchXml, true);
            searchCountText.setText(store.getTotalCount() + " / " + extractNumResultsTotal(searchXml) + " results shown.");
            searchAllButton.setText(createLinkFont(BioPortalConstants.SHOW_ALL_BUTTON_TEXT,
                    !searchAllButton.isPressed() && extractNumPages(searchXml)>1));
            createDNFRefButton.setText(createLinkFont(BioPortalConstants.DNF_BUTTON_TEXT, store.getTotalCount() == 0));
        }

        private int extractNumPages(final String searchXml) {
        	return extractAttributeValue(searchXml, BioPortalConstants.XML_ELEMENT_NUM_PAGES);
        }

        private int extractNumResultsTotal(final String searchXml) {
        	return extractAttributeValue(searchXml, BioPortalConstants.XML_ELEMENT_NUM_RESULTS_TOTAL);
        }

        private int extractAttributeValue(final String searchXml, final String attrName) {
            final String start_el = "<" + attrName + ">";
            final String end_el = "</" + attrName + ">";
            int res = 0;
            //This is a hacky solution in order to avoid the overhead of using the XML parser.
            int start_idx = searchXml.indexOf(start_el);
            if (start_idx >= 0) {
                int val_idx = start_idx + start_el.length();
                int end_idx = searchXml.indexOf(end_el, val_idx);
                if (end_idx >= 0) {
                    res = Integer.parseInt(searchXml.substring(val_idx, end_idx));
                }
            }
            return res;
        }
    }

    protected AbstractAsyncHandler<EntityData> getImportBioPortalConceptHandler() {
        return new ImportBioPortalConceptHandler();
    }

    class ImportBioPortalConceptHandler extends AbstractAsyncHandler<EntityData> {
        @Override
        public void handleFailure(Throwable caught) {
            if (getEl() != null) {
                getEl().unmask();
            }
            GWT.log("Could not import BioPortal concept for " + currentEntity, null);
            MessageBox.alert("Import operation failed!");
        }

        @Override
        public void handleSuccess(EntityData refInstance) {
            if (getEl() != null) {
                getEl().unmask();
            }
            MessageBox.alert(refInstance != null ? "Import operation SUCCEDED! Reference instance: " + refInstance
                    : "Import operation DID NOT SUCCEDED!");
        }
    }

    protected AbstractAsyncHandler<EntityData> getCreateManualreferenceHandler() {
        return new CreateManualreferenceHandler();
    }

    class CreateManualreferenceHandler extends AbstractAsyncHandler<EntityData> {
        @Override
        public void handleFailure(Throwable caught) {
            getEl().unmask();
            GWT.log("Could not create manual reference for " + currentEntity, null);
            MessageBox.alert("Reference creation failed!");
        }

        @Override
        public void handleSuccess(EntityData refInstance) {
            if (getEl() != null) {
                getEl().unmask();
            }
            MessageBox.alert(refInstance != null ? "Reference creation SUCCEDED! Reference instance: " + refInstance
                    : "Reference creation DID NOT SUCCEDED!");
        }
    }

    protected AbstractAsyncHandler<EntityData> getCreateDNFConceptHandler(NotesData note) {
        return new CreateDNFConceptHandler(note);
    }

    class CreateDNFConceptHandler extends AbstractAsyncHandler<EntityData> {
        private NotesData note;

        public CreateDNFConceptHandler(NotesData note) {
            this.note = note;
        }

        @Override
        public void handleFailure(Throwable caught) {
            if (getEl() != null) {
                getEl().unmask();
            }
            GWT.log("Could not create DNF reference for " + currentEntity, null);
            MessageBox.alert("Reference creation failed!");
        }

        @Override
        public void handleSuccess(EntityData refInstance) {
            if (getEl() != null) {
                getEl().unmask();
            }
            MessageBox.alert(refInstance != null ? "Reference creation SUCCEDED! Reference instance: " + refInstance
                    : "Reference creation DID NOT SUCCEDED!");
            this.note.setAnnotatedEntity(refInstance);
            ReferenceFieldWidget.addUserComment(project, note);
        }
    }
}
