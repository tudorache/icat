package edu.stanford.bmir.protege.web.client.ui.ontology.scripting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.gwtext.client.widgets.layout.AnchorLayout;
import com.gwtext.client.widgets.layout.AnchorLayoutData;

import edu.stanford.bmir.protege.web.client.model.GlobalSettings;
import edu.stanford.bmir.protege.web.client.model.Project;
import edu.stanford.bmir.protege.web.client.rpc.AbstractAsyncHandler;
import edu.stanford.bmir.protege.web.client.rpc.ScriptingServiceManager;
import edu.stanford.bmir.protege.web.client.rpc.data.EntityData;
import edu.stanford.bmir.protege.web.client.rpc.data.scripting.ScriptCommand;
import edu.stanford.bmir.protege.web.client.rpc.data.scripting.ScriptResult;
import edu.stanford.bmir.protege.web.client.ui.portlet.AbstractEntityPortlet;

public class PythonScriptingPortlet extends AbstractEntityPortlet {

	private HTML resultArea;
	private ScrollPanel resultAreaScrollPanel;
	private TextArea commandLine;

	private List<String> cmdHistory = new ArrayList<String>();
	private int currentHistoryIndex = 0;
	
	private CodeCompletion codeCompletion;


	public PythonScriptingPortlet(Project project) {
		super(project);
	}

	@Override
	public void initialize() {
		setTitle("Phyton Scripting");

		setLayout(new AnchorLayout());

		HorizontalPanel actionPanel = new HorizontalPanel();
		actionPanel.add(createClearResultsAnchor());
		actionPanel.setStylePrimaryName("script-action-panel");
		actionPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);

		resultArea = new HTML();
		resultArea.setStylePrimaryName("script-result-area");
		resultAreaScrollPanel = new ScrollPanel(resultArea);

		commandLine = new TextArea();
		commandLine.addKeyUpHandler(getKeyUpHandler());

		add(actionPanel, new AnchorLayoutData("100% 5%"));
		add(resultAreaScrollPanel, new AnchorLayoutData("100% 75%"));
		add(commandLine, new AnchorLayoutData("100% 20%"));
		
		initCodeCompletion();
	}

	private void initCodeCompletion() {
		codeCompletion = new CodeCompletion(new Callback<String>() {
			
			@Override
			public void onDone(String result) {
				appendCodeCompletion(result);
			}
		});
	}


	private Anchor createClearResultsAnchor() {
		Anchor clearResultsAnchor = new Anchor("Clear results");
		clearResultsAnchor.addClickHandler(new ClickHandler() {

			@Override
			public void onClick(ClickEvent event) {
				resultArea.setHTML("");
				commandLine.setFocus(true);
			}
		});
		clearResultsAnchor.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
		return clearResultsAnchor;
	}

	private KeyUpHandler getKeyUpHandler() {
		return new KeyUpHandler() {

			@Override
			public void onKeyUp(KeyUpEvent event) {
				int keyCode = event.getNativeKeyCode();
				if (keyCode == KeyCodes.KEY_ENTER) {
					if (event.isControlKeyDown() || event.isShiftKeyDown()) { // just let it add a new line
					} else {
						event.preventDefault();
						event.stopPropagation();
						
						commandLine.cancelKey();
						
						executeCommand();
					}
				} else if (keyCode == KeyCodes.KEY_TAB) { // just let it add a tab
					event.preventDefault();
					event.stopPropagation();
				} else if (keyCode == KeyCodes.KEY_SPACE && event.isControlKeyDown()) {
					int x = commandLine.getAbsoluteLeft() + 9 * commandLine.getText().length();
					int y = commandLine.getAbsoluteTop() + 14;
					getCodeCompletionSuggestions(x, y);
				} else if (event.isDownArrow()) {
					event.preventDefault();
					event.stopPropagation();

					commandLine.cancelKey();

					setCmdFromHistory(-1);

				} else if (event.isUpArrow()) {
					event.preventDefault();
					event.stopPropagation();

					commandLine.cancelKey();

					setCmdFromHistory(1);
				}
			}
		};
	}



	private void getCodeCompletionSuggestions(int x, int y) {
		ScriptingServiceManager.getInstance().getCodeCompletion(getProject().getProjectName(), getCommand(),
				new GetCodeCompletionSuggestions(getSearchStr(), x, y));
	}

	private String getSearchStr() {
		String cmd = getCommand();
		return cmd.substring(cmd.lastIndexOf(".") + 1);
	}

	private void executeCommand() {
		String cmd = getCommand();

		addToCmdHistory(cmd);
		currentHistoryIndex = 0;

		appendResult("> " + commandDisplayText(cmd));
		commandLine.setText("");
		commandLine.setFocus(true);

		ScriptingServiceManager.getInstance().executePythonScript(getProject().getProjectName(),
				GlobalSettings.getGlobalSettings().getUserName(), new ScriptCommand(cmd), new ExecutePythonScript());
	}

	private String commandDisplayText(String cmd) {
		return "<b>" + cmd + "</b>";
	}

	private void addToCmdHistory(String cmd) {
		cmdHistory.add(0, cmd);
	}

	private void setCmdFromHistory(int incrm) {
		if (currentHistoryIndex < 0) {
			currentHistoryIndex = 0; // check
			return;
		}

		if (currentHistoryIndex >= cmdHistory.size()) {
			currentHistoryIndex = cmdHistory.size() - 1;
			return;
		}

		String currentHistoryCmd = cmdHistory.get(currentHistoryIndex);
		if (currentHistoryCmd == null || currentHistoryCmd.length() == 0) {
			currentHistoryIndex = 0; // check
			return;
		}

		commandLine.setText(currentHistoryCmd);
		currentHistoryIndex = currentHistoryIndex + incrm;
	}

	private void appendResult(String text) {
		appendResult(text, false);
	}

	private void appendResult(String text, boolean isError) {
		if (text == null || text.length() == 0) {
			return;
		}

		text = text.trim();
		text = text.replaceAll("\\n", "<br />");

		if (isError == true) {
			text = "<font color=\"red\">" + text + "</font>";
		}

		String oldText = resultArea.getHTML();
		text = (oldText == null || oldText.length() == 0) ? text : oldText + "<br />" + text;

		resultArea.setHTML(text);
		resultAreaScrollPanel.scrollToBottom();
	}
	
	protected void appendCodeCompletion(String result) {
		commandLine.setText(commandLine.getText() + result); //TODO: just insert at cursor position
		commandLine.setFocus(true);
	}


	private String getCommand() {
		String cmd = commandLine.getText();
		cmd = cmd.trim();
		GWT.log("Command: " + cmd);
		return cmd;
	}

	@Override
	public Collection<EntityData> getSelection() {
		return null;
	}

	@Override
	public void reload() {
		// does nothing for now; maybe reinit jython engine?
	}



	// ***************** Remote method calls ******************

	class ExecutePythonScript extends AbstractAsyncHandler<ScriptResult> {

		@Override
		public void handleFailure(Throwable caught) {
			appendResult("Error at execution: " + caught.getMessage(), true);
		}

		@Override
		public void handleSuccess(ScriptResult result) {
			if (result.hasResult()) {
				appendResult(result.getResult());
			}

			if (result.hasError()) {
				appendResult(result.getError(), true);
			}
		}

	}

	class GetCodeCompletionSuggestions extends AbstractAsyncHandler<List<String>> {

		private String searchStr;
		private int x;
		private int y;

		GetCodeCompletionSuggestions(String searchStr, int x, int y) {
			this.searchStr = searchStr;
			this.x = x;
			this.y = y;
		}

		@Override
		public void handleFailure(Throwable caught) {
			GWT.log("Error at getting Python code completions: " + caught.getMessage());
		}

		@Override
		public void handleSuccess(List<String> results) {
			codeCompletion.displayCodeCompletionResults(results, searchStr, x, y);
		}

	}

	

}
