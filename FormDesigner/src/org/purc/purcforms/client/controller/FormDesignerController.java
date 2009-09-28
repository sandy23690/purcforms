package org.purc.purcforms.client.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.purc.purcforms.client.AboutDialog;
import org.purc.purcforms.client.CenterPanel;
import org.purc.purcforms.client.Context;
import org.purc.purcforms.client.LeftPanel;
import org.purc.purcforms.client.PurcConstants;
import org.purc.purcforms.client.locale.LocaleText;
import org.purc.purcforms.client.model.FormDef;
import org.purc.purcforms.client.model.ModelConstants;
import org.purc.purcforms.client.util.FormDesignerUtil;
import org.purc.purcforms.client.util.FormUtil;
import org.purc.purcforms.client.util.LanguageUtil;
import org.purc.purcforms.client.view.FormsTreeView;
import org.purc.purcforms.client.view.LocalesDialog;
import org.purc.purcforms.client.view.LoginDialog;
import org.purc.purcforms.client.view.OpenFileDialog;
import org.purc.purcforms.client.view.SaveFileDialog;
import org.purc.purcforms.client.xforms.XformConverter;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.XMLParser;


/**
 * Controls the interactions betwen the menu, toolbar and various views for the form designer.
 * 
 * @author daniel
 *
 */
public class FormDesignerController implements IFormDesignerListener, OpenFileDialogEventListener{

	private CenterPanel centerPanel;
	private LeftPanel leftPanel;
	
	/**
	 * The identifier of the loaded or opened form.
	 */
	private Integer formId;	
	
	/**
	 * The listener to form save events.
	 */
	private IFormSaveListener formSaveListener;
	
	
	private HashMap<Integer,HashMap<String,String>> languageText = new HashMap<Integer,HashMap<String,String>>();

	private static final byte CA_NONE = 0;
	private static final byte CA_LOAD_FORM = 1;
	private static final byte CA_SAVE_FORM = 2;
	private static final byte CA_REFRESH_FORM = 3;
	private static final byte CA_SET_FILE_CONTENTS = 4;

	private static byte currentAction = CA_NONE;

	/**
	 * The dialog box used to logon the server when the user's session expires on the server.
	 */
	private static LoginDialog loginDlg = new LoginDialog();
	
	
	private static FormDesignerController controller;
	private Object refreshObject;

	public FormDesignerController(CenterPanel centerPanel, LeftPanel leftPanel){
		this.leftPanel = leftPanel;
		this.centerPanel = centerPanel;
		
		this.centerPanel.setFormDesignerListener(this);

		controller = this;
	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormActionListener#addNewItem()
	 */
	public void addNewItem() {
		leftPanel.addNewItem();
	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormActionListener#addNewChildItem()
	 */
	public void addNewChildItem() {
		leftPanel.addNewChildItem();
	}
	
	/**
	 * @see org.purc.purcforms.client.controller.IFormDesignerController#printForm()
	 */
	public void printForm(){
		FormDef formDef = centerPanel.getFormDef();
		if(formDef != null)
			printForm(formDef.getName(), centerPanel.getFormInnerHtml());
	}
	
	public static native void printForm(String title,String html) /*-{
		 var win =window.open('','purcforms','width=350,height=250,menubar=1,toolbar=1,status=1,scrollbars=1,resizable=1');
		 win.document.open("text/html","replace");
		 win.document.writeln('<html><head><title>' + title + '</title></head><body bgcolor=white onLoad="self.focus()">'+html+'</body></html>');
		 win.document.close();
	}-*/;

	/**
	 * @see org.purc.purcforms.client.controller.IFormDesignerController#closeForm()
	 */
	public void closeForm() {
		String url = FormUtil.getCloseUrl();
		if(url != null && url.trim().length() > 0)
			Window.Location.replace(url);
	} 

	/**
	 * @see org.purc.purcforms.client.controller.IFormActionListener#deleteSelectedItems()
	 */
	public void deleteSelectedItem() {
		leftPanel.deleteSelectedItem();	
		centerPanel.deleteSelectedItem();
	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormActionListener#moveItemDown()
	 */
	public void moveItemDown() {
		leftPanel.moveItemDown();
	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormActionListener#moveItemUp()
	 */
	public void moveItemUp() {
		leftPanel.moveItemUp();
	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormActionListener#newForm()
	 */
	public void newForm() {
		if(isOfflineMode())
			leftPanel.addNewForm();
	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormActionListener#showAboutInfo()
	 */
	public void openForm() {
		if(isOfflineMode()){
			String xml = centerPanel.getXformsSource();
			
			//Only load layout if in layout mode and no xforms source is supplied.
			if(centerPanel.isInLayoutMode() && (xml == null || xml.trim().length() == 0)){
				xml = centerPanel.getLayoutXml();
				if(xml == null || xml.trim().length() == 0){
					OpenFileDialog dlg = new OpenFileDialog(this,"formopen");
					dlg.center();
				}
			}
			else{
				//Whether in layout mode or not, as long as xforms source is supplied, we load it.
				if(xml != null && xml.trim().length() > 0){
					FormDef formDef = leftPanel.getSelectedForm();
					if(formDef != null)
						refreshFormDeffered();
					else
						openFormDeffered(ModelConstants.NULL_ID,false);
				}
				else{
					OpenFileDialog dlg = new OpenFileDialog(this,"formopen");
					dlg.center();
				}
			}
		}
	}

	public void openFormDeffered(int id, boolean readonly) {
		final int tempFormId = id;
		final boolean tempReadonly = readonly;

		FormUtil.dlg.setText(LocaleText.get("openingForm"));
		FormUtil.dlg.center();

		DeferredCommand.addCommand(new Command(){
			public void execute() {
				try{
					String xml = centerPanel.getXformsSource().trim();
					if(xml.length() > 0){
						FormDef formDef = XformConverter.fromXform2FormDef(xml);
						formDef.setReadOnly(tempReadonly);

						if(tempFormId != ModelConstants.NULL_ID)
							formDef.setId(tempFormId);

						formDef.setXformXml(centerPanel.getXformsSource());
						formDef.setLayoutXml(centerPanel.getLayoutXml());
						leftPanel.loadForm(formDef);
						centerPanel.loadForm(formDef,formDef.getLayoutXml());
						centerPanel.format();
					}
					FormUtil.dlg.hide();
				}
				catch(Exception ex){
					FormUtil.dlg.hide();
					FormUtil.displayException(ex);
				}	
			}
		});
	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormDesignerController#openFormLayout()
	 */
	public void openFormLayout() {
		openFormLayout(true);
	}

	public void openFormLayout(boolean selectTabs) {
		//if(isOfflineMode())
		openFormLayoutDeffered(selectTabs);
	}

	public void openFormLayoutDeffered(boolean selectTabs) {
		final boolean selectTbs = selectTabs;

		FormUtil.dlg.setText(LocaleText.get("openingFormLayout"));
		FormUtil.dlg.center();

		DeferredCommand.addCommand(new Command(){
			public void execute() {
				try{
					centerPanel.openFormLayout(selectTbs);
					FormUtil.dlg.hide();
				}
				catch(Exception ex){
					FormUtil.dlg.hide();
					FormUtil.displayException(ex);
				}	
			}
		});
	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormDesignerController#saveForm()
	 */
	public void saveForm(){
		if(isOfflineMode())
			saveTheForm();
		else{
			currentAction = CA_SAVE_FORM;
			FormUtil.isAuthenticated();
		}
	}

	private void saveTheForm() {
		final FormDef obj = leftPanel.getSelectedForm();
		if(obj.isReadOnly())
			;//return; //TODO I think we should allow saving of form text and layout

		if(!leftPanel.isValidForm())
			return;


		if(obj == null){
			Window.alert(LocaleText.get("selectSaveItem"));
			return;
		}

		if(Context.inLocalizationMode()){
			saveLanguageText(true);
			return;
		}

		FormUtil.dlg.setText(LocaleText.get("savingForm"));
		FormUtil.dlg.center();

		DeferredCommand.addCommand(new Command(){
			public void execute() {
				try{
					centerPanel.commitChanges();

					//TODO Need to preserve user's model and any others.
					String xml = null;
					FormDef formDef = obj;
					if(formDef.getDoc() == null){
						xml = XformConverter.fromFormDef2Xform(formDef);
						xml = FormDesignerUtil.formatXml(xml);
					}
					else{
						formDef.updateDoc(false);
						xml = XformConverter.fromDoc2String(formDef.getDoc());
						xml = FormDesignerUtil.formatXml(xml);
					}

					formDef.setXformXml(xml);
					centerPanel.setXformsSource(xml,formSaveListener == null && isOfflineMode());
					centerPanel.buildLayoutXml();
					//formDef.setLayout(centerPanel.getLayoutXml());

					if(!isOfflineMode())
						saveForm(xml,centerPanel.getLayoutXml());

					boolean saveLocaleText = true;
					if(formSaveListener != null)
						saveLocaleText = formSaveListener.onSaveForm(formDef.getId(), xml, centerPanel.getLayoutXml());

					FormUtil.dlg.hide();

					if(saveLocaleText)
						saveLanguageText(false); //Save text for the default language
				}
				catch(Exception ex){
					FormUtil.dlg.hide();
					FormUtil.displayException(ex);
					return;
				}	
			}
		});
	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormDesignerController#saveFormAs()
	 */
	public void saveFormAs() {
		if(isOfflineMode()){
			final Object obj = leftPanel.getSelectedForm();
			if(obj == null){
				Window.alert(LocaleText.get("selectSaveItem"));
				return;
			}

			FormUtil.dlg.setText(LocaleText.get("savingForm"));
			FormUtil.dlg.center();

			DeferredCommand.addCommand(new Command(){
				public void execute() {
					try{
						String xml = null;
						xml = XformConverter.fromFormDef2Xform((FormDef)obj);
						xml = FormDesignerUtil.formatXml(xml);
						centerPanel.setXformsSource(xml,formSaveListener == null && isOfflineMode());
						FormUtil.dlg.hide();
					}
					catch(Exception ex){
						FormUtil.dlg.hide();
						FormUtil.displayException(ex);
					}	
				}
			});
		}
	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormDesignerController#saveFormLayout()
	 */
	public void saveFormLayout() {
		FormUtil.dlg.setText(LocaleText.get("savingFormLayout"));
		FormUtil.dlg.center();

		DeferredCommand.addCommand(new Command(){
			public void execute() {
				try{
					centerPanel.saveFormLayout();
					FormUtil.dlg.hide();
				}
				catch(Exception ex){
					FormUtil.dlg.hide();
					FormUtil.displayException(ex);
				}	
			}
		});
	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormDesignerController#showAboutInfo()
	 */
	public void showAboutInfo() {
		AboutDialog dlg = new AboutDialog();
		dlg.setAnimationEnabled(true);
		dlg.center();
	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormDesignerController#alignLeft()
	 */
	public void showHelpContents() {
		// TODO Auto-generated method stub

	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormDesignerController#showLanguages()
	 */
	public void showLanguages() {
		LocalesDialog dlg = new LocalesDialog();
		dlg.center();
	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormDesignerController#showOptions()
	 */
	public void showOptions() {
		// TODO Auto-generated method stub

	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormDesignerController#viewToolbar()
	 */
	public void viewToolbar() {
		// TODO Auto-generated method stub

	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormDesignerController#alignLeft()
	 */
	public void alignLeft() {
		centerPanel.alignLeft();
	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormDesignerController#alignRight()
	 */
	public void alignRight() {
		centerPanel.alignRight();
	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormDesignerController#alignTop()
	 */
	public void alignTop() {
		centerPanel.alignTop();
	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormDesignerController#alignBottom()
	 */
	public void alignBottom() {
		centerPanel.alignBottom();
	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormDesignerController#makeSameHeight()
	 */
	public void makeSameHeight() {
		centerPanel.makeSameHeight();
	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormDesignerController#makeSameSize()
	 */
	public void makeSameSize() {
		centerPanel.makeSameSize();
	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormDesignerController#makeSameWidth()
	 */
	public void makeSameWidth() {
		centerPanel.makeSameWidth();
	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormActionListener#copyItem()
	 */
	public void copyItem() {
		if(!Context.isStructureReadOnly()){
			leftPanel.copyItem();
			centerPanel.copyItem();
		}
	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormActionListener#cutItem()
	 */
	public void cutItem() {
		if(!Context.isStructureReadOnly()){
			leftPanel.cutItem();
			centerPanel.cutItem();
		}
	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormActionListener#pasteItem()
	 */
	public void pasteItem() {
		if(!Context.isStructureReadOnly()){
			leftPanel.pasteItem();
			centerPanel.pasteItem();
		}
	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormActionListener#refreshItem()
	 */
	public void refreshItem(){
		if(!Context.isStructureReadOnly())
			leftPanel.refreshItem();
	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormDesignerController#format()
	 */
	public void format() {
		centerPanel.format();
	}

	private void refreshObject() {
		if(refreshObject instanceof FormsTreeView){ //TODO This controller should not know about LeftPanel implementation details.
			if(formId != null){
				FormUtil.dlg.setText(LocaleText.get("refreshingForm"));
				FormUtil.dlg.center();

				DeferredCommand.addCommand(new Command(){
					public void execute() {
						refreshForm();
						FormUtil.dlg.hide();	
					}
				});
			}
			else
				refreshFormDeffered();
		}
		else{
			centerPanel.refresh();
			leftPanel.refresh();
		}
	}

	public void refresh(Object sender) {
		refreshObject = sender;

		if(isOfflineMode())
			refreshObject();
		else{
			currentAction = CA_REFRESH_FORM;
			FormUtil.isAuthenticated();
		}
	}

	private void loadForm(){
		FormUtil.dlg.setText(LocaleText.get("openingForm"));
		FormUtil.dlg.center();

		DeferredCommand.addCommand(new Command(){
			public void execute() {

				String url = FormUtil.getHostPageBaseURL();
				url += FormUtil.getFormDefDownloadUrlSuffix();
				url += FormUtil.getFormIdName()+"="+formId;

				RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,URL.encode(url));

				try{
					builder.sendRequest(null, new RequestCallback(){
						public void onResponseReceived(Request request, Response response){
							String xml = response.getText();
							if(xml == null || xml.length() == 0){
								FormUtil.dlg.hide();
								Window.alert(LocaleText.get("noDataFound"));
								return;
							}

							String xformXml, layoutXml = null;

							int pos = xml.indexOf(PurcConstants.PURCFORMS_FORMDEF_LAYOUT_XML_SEPARATOR);
							if(pos > 0){
								xformXml = xml.substring(0,pos);
								layoutXml = FormUtil.formatXml(xml.substring(pos+PurcConstants.PURCFORMS_FORMDEF_LAYOUT_XML_SEPARATOR.length(), xml.length()));
							}
							else
								xformXml = xml;

							centerPanel.setXformsSource(FormUtil.formatXml(xformXml),false);
							centerPanel.setLayoutXml(layoutXml,false);
							openFormDeffered(formId,false);

							//FormUtil.dlg.hide(); //openFormDeffered above will close it
						}

						public void onError(Request request, Throwable exception){
							FormUtil.dlg.hide();
							FormUtil.displayException(exception);
						}
					});
				}
				catch(RequestException ex){
					FormUtil.dlg.hide();
					FormUtil.displayException(ex);
				}
			}
		});
	}

	/**
	 * Loads or opens a form with a given id.
	 * 
	 * @param frmId the form id.
	 */
	public void loadForm(int frmId){
		this.formId = frmId;

		if(isOfflineMode())
			loadForm();
		else{
			currentAction = CA_LOAD_FORM;
			FormUtil.isAuthenticated();
		}
	}

	public void saveForm(String xformXml, String layoutXml){
		String url = FormUtil.getHostPageBaseURL();
		url += FormUtil.getFormDefUploadUrlSuffix();
		url += FormUtil.getFormIdName()+"="+this.formId;

		RequestBuilder builder = new RequestBuilder(RequestBuilder.POST,URL.encode(url));
		//builder.setHeader("Content_type", "application/x-www-form-urlencoded");

		try{
			String xml = xformXml;
			if(layoutXml != null && layoutXml.trim().length() > 0)
				xml += PurcConstants.PURCFORMS_FORMDEF_LAYOUT_XML_SEPARATOR + layoutXml;

			builder.sendRequest(xml, new RequestCallback(){
				public void onResponseReceived(Request request, Response response){
					Window.alert(LocaleText.get("formSaveSuccess"));
				}

				public void onError(Request request, Throwable exception){
					FormUtil.displayException(exception);
				}
			});
		}
		catch(RequestException ex){
			FormUtil.displayException(ex);
		}
	}

	/**
	 * Checks if the form designer is in offline mode.
	 * 
	 * @return true if in offline mode, else false.
	 */
	public boolean isOfflineMode(){
		return formId == null;
	}

	private void refreshForm(){
		String url = FormUtil.getHostPageBaseURL();
		url += FormUtil.getFormDefRefreshUrlSuffix();
		url += FormUtil.getFormIdName()+"="+this.formId;

		//url += "&uname=Guyzb&pw=daniel123";

		RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,URL.encode(url));

		try{
			builder.sendRequest(null, new RequestCallback(){
				public void onResponseReceived(Request request, Response response){
					String xml = response.getText();
					if(xml == null || xml.length() == 0){
						Window.alert(LocaleText.get("noDataFound"));
						return;
					}

					centerPanel.setXformsSource(xml,false);
					refreshFormDeffered();
				}

				public void onError(Request request, Throwable exception){
					FormUtil.displayException(exception);
				}
			});
		}
		catch(RequestException ex){
			FormUtil.displayException(ex);
		}
	}

	/**
	 * Refreshes the selected from in a deferred command.
	 */
	private void refreshFormDeffered(){
		FormUtil.dlg.setText(LocaleText.get("refreshingForm"));
		FormUtil.dlg.center();

		DeferredCommand.addCommand(new Command(){
			public void execute() {
				try{
					String xml = centerPanel.getXformsSource();
					if(xml != null && xml.trim().length() > 0){
						FormDef formDef = XformConverter.fromXform2FormDef(xml);
						
						FormDef oldFormDef = centerPanel.getFormDef();
						
						//If we are in offline mode, we completely overwrite the form 
						//with the contents of the xforms source tab.
						if(!isOfflineMode())
							formDef.refresh(oldFormDef);
						
						formDef.updateDoc(false);
						xml = formDef.getDoc().toString();

						formDef.setXformXml(FormUtil.formatXml(xml));
						
						formDef.setLayoutXml(oldFormDef.getLayoutXml());
						formDef.setLanguageXml(oldFormDef.getLanguageXml());
						
						leftPanel.refresh(formDef);
						centerPanel.refresh();
					}
					FormUtil.dlg.hide();
				}
				catch(Exception ex){
					FormUtil.dlg.hide();
					FormUtil.displayException(ex);
				}
			}
		});
	}

	/**
	 * Sets the listener to form save events.
	 * 
	 * @param formSaveListener the listener.
	 */
	public void setFormSaveListener(IFormSaveListener formSaveListener){
		this.formSaveListener = formSaveListener;
	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormActionListener#moveUp()
	 */
	public void moveUp(){
		leftPanel.getFormActionListener().moveUp();
	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormActionListener#moveDown()
	 */
	public void moveDown(){
		leftPanel.getFormActionListener().moveUp();
	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormActionListener#moveToParent()
	 */
	public void moveToParent(){
		leftPanel.getFormActionListener().moveToParent();
	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormActionListener#moveToChild()
	 */
	public void moveToChild(){
		leftPanel.getFormActionListener().moveToChild();
	}

	/**
	 * @see org.purc.purcforms.client.controller.OpenFileDialogEventListener#onSetFileContents()
	 */
	public void onSetFileContents(String contents) {
		if(isOfflineMode())
			setFileContents();
		else{
			currentAction = CA_SET_FILE_CONTENTS;
			FormUtil.isAuthenticated();
		}
	}

	private void setFileContents() {

		RequestBuilder builder = new RequestBuilder(RequestBuilder.GET,"formopen");

		try{
			builder.sendRequest(null, new RequestCallback(){
				public void onResponseReceived(Request request, Response response){
					String contents = response.getText();
					if(contents != null && contents.trim().length() > 0){
						if(centerPanel.isInLayoutMode())
							centerPanel.setLayoutXml(contents, false);
						else{
							centerPanel.setXformsSource(contents, true);
							openForm();
						}
					}
				}

				public void onError(Request request, Throwable exception){
					FormUtil.displayException(exception);
				}
			});
		}
		catch(RequestException ex){
			FormUtil.displayException(ex);
		}
	}

	public void saveAs(){
		try{
			String data = (centerPanel.isInLayoutMode() ? centerPanel.getLayoutXml() : centerPanel.getXformsSource());
			if(data == null || data.trim().length() == 0)
				return;

			FormDef formDef = leftPanel.getSelectedForm();
			String fileName = "filename";
			if(formDef != null)
				fileName = formDef.getName();

			if(centerPanel.isInLayoutMode())
				fileName += "-" + LocaleText.get("layout");

			SaveFileDialog dlg = new SaveFileDialog("formsave",data,fileName);
			dlg.center();
		}
		catch(Exception ex){
			FormUtil.displayException(ex);
		}
	}

	/**
	 * @see org.purc.purcforms.client.controller.IFormDesignerListener#openLanguageText()
	 */
	public void openLanguageText(){

		FormUtil.dlg.setText(LocaleText.get("translatingFormLanguage"));
		FormUtil.dlg.center();

		DeferredCommand.addCommand(new Command(){
			public void execute() {
				try{
					int selFormId = -1; String xml = null; 
					FormDef formDef = leftPanel.getSelectedForm();
					if(formDef != null)
						selFormId = formDef.getId();

					List<FormDef> forms = leftPanel.getForms();
					if(forms != null && forms.size() > 0){
						List<FormDef> newForms = new ArrayList<FormDef>();
						for(FormDef form : forms){
							xml = getFormLocaleText(form.getId(),Context.getLocale());
							if(xml != null){
								String xform = FormUtil.formatXml(LanguageUtil.translate(form.getDoc(), xml, true));
								FormDef newFormDef = XformConverter.fromXform2FormDef(xform);
								newFormDef.setXformXml(xform);
								newFormDef.setLayoutXml(FormUtil.formatXml(LanguageUtil.translate(form.getLayoutXml(), xml, false)));
								newFormDef.setLanguageXml(xml);
								newForms.add(newFormDef);

								if(newFormDef.getId() == selFormId)
									formDef = newFormDef;
							}
							else{
								newForms.add(form);
								if(form.getId() == selFormId)
									formDef = form;
							}
						}

						leftPanel.loadForms(newForms, formDef.getId());
					}

					FormUtil.dlg.hide();

					String layoutXml = centerPanel.getLayoutXml();
					if(layoutXml != null && layoutXml.trim().length() > 0)
						openFormLayout(false);
				}
				catch(Exception ex){
					FormUtil.dlg.hide();
					FormUtil.displayException(ex);
				}
			}
		});
	}

	public void saveLanguageText(boolean selectTab){
		saveLanguageTextDeffered(selectTab);
	}

	public void saveLanguageText(){
		saveLanguageTextDeffered(true);
	}

	public void saveLanguageTextDeffered(boolean selectTab){
		final boolean selTab = selectTab;

		FormUtil.dlg.setText(LocaleText.get("savingLanguageText"));
		FormUtil.dlg.center();

		DeferredCommand.addCommand(new Command(){
			public void execute() {
				try{
					centerPanel.saveLanguageText(selTab);
					setLocaleText(centerPanel.getFormDef().getId(),Context.getLocale(), centerPanel.getLanguageXml());
					//languageText.put(Context.getLocale(), centerPanel.getLanguageXml());

					if(formSaveListener != null){
						FormDef formDef = centerPanel.getFormDef();
						String langXml = formDef.getLanguageXml();
						if(langXml != null && langXml.trim().length() > 0){
							Document doc = XMLParser.parse(langXml);
							formSaveListener.onSaveLocaleText(formDef.getId(), LanguageUtil.getXformsLocaleText(doc), LanguageUtil.getLayoutLocaleText(doc));
						}
					}

					FormUtil.dlg.hide();
				}
				catch(Exception ex){
					FormUtil.dlg.hide();
					FormUtil.displayException(ex);
				}	
			}
		});
	}

	public void changeLocale(String locale){
		Context.setLocale(locale);

		String xml = null;
		FormDef formDef = centerPanel.getFormDef();
		if(formDef != null){
			HashMap<String,String> map = languageText.get(formDef.getId());
			if(map != null)
				xml = map.get(locale);

			if(xml == null && map != null)
				xml = map.get(Context.getDefaultLocale());
		}

		centerPanel.setLanguageXml(xml, false);
		openLanguageText();
	}

	private void setLocaleText(Integer formId, String locale, String text){
		HashMap<String,String> map = languageText.get(formId);
		if(map == null){
			map = new HashMap<String,String>();
			languageText.put(formId, map);
		}

		map.put(locale, text);
	}

	private String getFormLocaleText(int formId, String locale){
		HashMap<String,String> map = languageText.get(formId);
		if(map != null)
			return map.get(locale);
		return null;
	}

	public void setLocaleText(Integer formId, String locale, String xform, String layout){
		setLocaleText(formId,locale, LanguageUtil.getLocaleText(xform, layout));
		//languageText.put(locale, LanguageUtil.getLocaleText(xform, layout));
	}

	/**
	 * Sets the default locale used by the form designer.
	 * 
	 * @param locale the locale key.
	 */
	public void setDefaultLocale(String locale){
		Context.setDefaultLocale(locale);
	}

	/**
	 * Embeds the selected xform into xhtml.
	 */
	public void saveAsXhtml(){
		if(isOfflineMode()){
			final Object obj = leftPanel.getSelectedForm();
			if(obj == null){
				Window.alert(LocaleText.get("selectSaveItem"));
				return;
			}

			FormUtil.dlg.setText(LocaleText.get("savingForm"));
			FormUtil.dlg.center();

			DeferredCommand.addCommand(new Command(){
				public void execute() {
					try{
						String xml = null;
						FormDef formDef = new FormDef((FormDef)obj);
						formDef.setDoc(((FormDef)obj).getDoc()); //We want to copy the model xml
						xml = XformConverter.fromFormDef2Xhtml(formDef);
						xml = FormDesignerUtil.formatXml(xml);
						centerPanel.setXformsSource(xml,formSaveListener == null && isOfflineMode());
						FormUtil.dlg.hide();
					}
					catch(Exception ex){
						FormUtil.dlg.hide();
						FormUtil.displayException(ex);
					}	
				}
			});
		}
	}

	/**
	 * This is called from the server after an attempt to authenticate the current
	 * user before they can submit form data.
	 * 
	 * @param authenticated has a value of true if the server has successfully authenticated the user, else false.
	 */
	private static void authenticationCallback(boolean authenticated) {	
		
		//If user has passed authentication, just go on with whatever they wanted to do
		//else just redisplay the login dialog and let them enter correct
		//user name and password.
		if(authenticated){	
			loginDlg.hide();

			if(currentAction == CA_REFRESH_FORM)
				controller.refreshObject();
			else if(currentAction == CA_LOAD_FORM)
				controller.loadForm();
			else if(currentAction == CA_SAVE_FORM)
				controller.saveTheForm();
			else if(currentAction == CA_SET_FILE_CONTENTS)
				controller.setFileContents();

			currentAction = CA_NONE;
		}
		else
			loginDlg.center();
	}
	
	/**
	 * Called to handle form designer global keyboard short cuts.
	 */
	public boolean handleKeyBoardEvent(Event event){
		if(event.getCtrlKey()){
			
			if(event.getKeyCode() == 'S'){
				saveForm();
				//Returning false such that firefox does not try to save the page.
				return false;
			}
			else if(event.getKeyCode() == 'O'){
				openForm();
				return false;
			}
		}
		
		return true;
	}
}
