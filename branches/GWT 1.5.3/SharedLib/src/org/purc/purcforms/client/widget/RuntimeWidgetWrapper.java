package org.purc.purcforms.client.widget;

import java.util.ArrayList;
import java.util.List;

import org.purc.purcforms.client.controller.QuestionChangeListener;
import org.purc.purcforms.client.locale.LocaleText;
import org.purc.purcforms.client.model.FormDef;
import org.purc.purcforms.client.model.OptionDef;
import org.purc.purcforms.client.model.QuestionDef;
import org.purc.purcforms.client.model.ValidationRule;
import org.purc.purcforms.client.util.FormUtil;
import org.zenika.widget.client.datePicker.DatePicker;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FocusListenerAdapter;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.MultiWordSuggestOracle;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.SuggestBox;
import com.google.gwt.user.client.ui.SuggestionEvent;
import com.google.gwt.user.client.ui.SuggestionHandler;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.TextBoxBase;
import com.google.gwt.user.client.ui.Widget;


/**
 * Wraps a widget and gives it capability to be used at run time for data collection.
 * 
 * @author daniel
 *
 */
public class RuntimeWidgetWrapper extends WidgetEx implements QuestionChangeListener{

	/** Widget to display error message icon when the widget's validation fails. */
	protected Image errorImage;

	/** Collection of RadioButton and CheckBox wrapped widgets for a given question. */
	protected List<RuntimeWidgetWrapper> childWidgets;

	/** Listener to edit events. */
	protected EditListener editListener;

	private AbstractImagePrototype errorImageProto;
	
	/** Flag that tells whether this widget is locked and hence doesn't allow editing. */
	private boolean locked = false;

	/** The widget's validation rule. */
	private ValidationRule validationRule;

	/**
	 * Creates a copy of the widget.
	 * 
	 * @param widget the widget to copy.
	 */
	public RuntimeWidgetWrapper(RuntimeWidgetWrapper widget){
		super(widget);

		editListener = widget.getEditListener();
		errorImage = widget.getErrorImage().createImage();
		errorImageProto = widget.getErrorImage();
		errorImage.setTitle(LocaleText.get("requiredErrorMsg"));

		if(widget.getValidationRule() != null)
			validationRule = new ValidationRule(widget.getValidationRule());

		panel.add(this.widget);
		initWidget(panel);
		setupEventListeners();

		if(widget.questionDef != null){ //TODO For long list of options may need to share list
			//If we have a validation rule, then it already has a question copy
			//which we should use if validations are to fire expecially for repeats
			if(validationRule != null)
				questionDef = validationRule.getFormDef().getQuestion(widget.questionDef.getId());
			else
				questionDef = new QuestionDef(widget.questionDef,widget.questionDef.getParent());
		}
	}

	public RuntimeWidgetWrapper(Widget widget,AbstractImagePrototype errorImageProto,EditListener editListener){
		this.widget = widget;
		this.errorImageProto = errorImageProto;
		this.errorImage = errorImageProto.createImage();
		this.editListener = editListener;

		panel.add(widget);
		initWidget(panel);
		setupEventListeners();
		errorImage.setTitle(LocaleText.get("requiredErrorMsg"));

		DOM.sinkEvents(getElement(),DOM.getEventsSunk(getElement()) | Event.MOUSEEVENTS /*| Event.ONCONTEXTMENU | Event.KEYEVENTS*/);
	}

	public AbstractImagePrototype getErrorImage(){
		return errorImageProto;
	}

	/**
	 * Gets the question edit listener.
	 * 
	 * @return the edit listener.
	 */
	public EditListener getEditListener(){
		return editListener;
	}

	@Override
	public void onBrowserEvent(Event event) {
		if(locked)
			event.preventDefault();

		int type = DOM.eventGetType(event);
		if(type == Event.ONMOUSEUP && widget instanceof CheckBox && questionDef != null){
			if(questionDef.getDataType() == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE){
				if(((CheckBox)widget).isChecked())
					((CheckBox)widget).setChecked(false);
			}
		}
	}

	/**
	 * Sets up events listeners.
	 */
	private void setupEventListeners(){
		if(widget instanceof DatePicker){
			((DatePicker)widget).addFocusListener(new FocusListenerAdapter(){
				public void onLostFocus(Widget sender){
					((DatePicker)widget).selectAll();
				}
			});
		}

		if(widget instanceof TextBox)
			setupTextBoxEventListeners();
		else if(widget instanceof CheckBox){
			/*if(childWidgets != null){
				 for(int index=0; index < childWidgets.size(); index++){
					  RuntimeWidgetWrapper childWidget = childWidgets.get(index);
					  ((CheckBox)childWidget.getWrappedWidget()).addClickListener(new ClickListener(){
							public void onClick(Widget sender){
								questionDef.setAnswer(((RuntimeWidgetWrapper)sender.getParent()).getBinding());
								isValid();
								editListener.onValueChanged(null, null, questionDef.getAnswer());
							}
					});
				 }
			}*/

			((CheckBox)widget).addKeyboardListener(new KeyboardListenerAdapter(){
				public void onKeyDown(Widget sender, char keyCode, int modifiers) {
					if(keyCode == KeyboardListener.KEY_ENTER || keyCode == KeyboardListener.KEY_DOWN
							|| keyCode == KeyboardListener.KEY_RIGHT)
						editListener.onMoveToNextWidget((RuntimeWidgetWrapper)panel.getParent());
					else if(keyCode == KeyboardListener.KEY_UP || keyCode == KeyboardListener.KEY_LEFT)
						editListener.onMoveToPrevWidget((RuntimeWidgetWrapper)panel.getParent());
				}
			});
		}
		else if(widget instanceof ListBox){
			((ListBox)widget).addChangeListener(new ChangeListener(){
				public void onChange(Widget sender){
					questionDef.setAnswer(((ListBox)widget).getValue(((ListBox)widget).getSelectedIndex()));
					isValid();
					editListener.onValueChanged((RuntimeWidgetWrapper)panel.getParent());
				}
			});

			((ListBox)widget).addKeyboardListener(new KeyboardListenerAdapter(){
				public void onKeyDown(Widget sender, char keyCode, int modifiers) {
					if(keyCode == KeyboardListener.KEY_ENTER || keyCode == KeyboardListener.KEY_RIGHT)
						editListener.onMoveToNextWidget((RuntimeWidgetWrapper)panel.getParent());
					else if(keyCode == KeyboardListener.KEY_LEFT)
						editListener.onMoveToPrevWidget((RuntimeWidgetWrapper)panel.getParent());
				}//TODO Do we really wanna alter the behaviour of the arrow keys for list boxes?
			});
		}

		DOM.sinkEvents(getElement(),DOM.getEventsSunk(getElement()) | Event.MOUSEEVENTS | Event.ONCONTEXTMENU | Event.KEYEVENTS);
	}

	/**
	 * Sets up text box event listeners.
	 */
	private void setupTextBoxEventListeners(){
		if(widget.getParent() instanceof SuggestBox){
			((SuggestBox)widget.getParent()).addEventHandler(new SuggestionHandler(){
				public void onSuggestionSelected(SuggestionEvent event){
					if(questionDef != null){
						OptionDef optionDef = questionDef.getOptionWithText(getTextBoxAnswer());
						if(optionDef != null)
							questionDef.setAnswer(optionDef.getVariableName());
						else
							questionDef.setAnswer(null);
						isValid();
						editListener.onValueChanged((RuntimeWidgetWrapper)panel.getParent());
					}
				}
			});
		}
		else{
			((TextBox)widget).addChangeListener(new ChangeListener(){
				public void onChange(Widget sender){
					//questionDef.setAnswer(((TextBox)widget).getText());
					if(questionDef != null){
						questionDef.setAnswer(getTextBoxAnswer());
						isValid();
						editListener.onValueChanged((RuntimeWidgetWrapper)panel.getParent());
					}

					if(widget instanceof DatePickerWidget)
						editListener.onMoveToNextWidget((RuntimeWidgetWrapper)panel.getParent());
				}
			});
		}

		((TextBox)widget).addKeyboardListener(new KeyboardListenerAdapter(){
			public void onKeyUp(Widget sender, char keyCode, int modifiers) {
				if(questionDef != null && !(questionDef.getDataType() == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE || questionDef.getDataType() == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE_DYNAMIC)){
					questionDef.setAnswer(getTextBoxAnswer());
					isValid();
					editListener.onValueChanged((RuntimeWidgetWrapper)panel.getParent());
				}
			}

			public void onKeyDown(Widget sender, char keyCode, int modifiers) {
				if(keyCode == KeyboardListener.KEY_ENTER || keyCode == KeyboardListener.KEY_DOWN)
					editListener.onMoveToNextWidget((RuntimeWidgetWrapper)panel.getParent());
				else if(keyCode == KeyboardListener.KEY_UP)
					editListener.onMoveToPrevWidget((RuntimeWidgetWrapper)panel.getParent());
			}

			public void onKeyPress(Widget sender, char keyCode, int modifiers) {
				if(externalSource != null && externalSource.trim().length() > 0){
					((TextBox) sender).cancelKey(); 
					while(panel.getWidgetCount() > 1)
						panel.remove(1);

					if(keyCode == (char) KeyboardListener.KEY_DELETE || keyCode == (char) KeyboardListener.KEY_BACKSPACE){
						((TextBox) sender).setText("");
						if(questionDef != null)
							questionDef.setAnswer(null);

						return;
					}

					Label label = new Label("");
					label.setVisible(false);
					panel.add(label);
					FormUtil.searchExternal(externalSource,String.valueOf(keyCode), widget.getElement(), label.getElement(), widget.getElement());
				}
			}
		});
	}

	/**
	 * Sets the question for the widget.
	 * 
	 * @param questionDef the question definition object.
	 * @param loadWidget set to true to load widget values, else false.
	 */
	public void setQuestionDef(QuestionDef questionDef ,boolean loadWidget){
		this.questionDef = questionDef;

		if(loadWidget)
			loadQuestion();
	}

	/**
	 * Loads values for the widget.
	 */
	public void loadQuestion(){
		if(questionDef == null)
			return;

		//questionDef.clearChangeListeners(); Removed from here because we want to allow more that one widget listen on the same question.
		questionDef.addChangeListener(this);
		questionDef.setAnswer(questionDef.getDefaultValueSubmit());

		String defaultValue = questionDef.getDefaultValue();

		int type = questionDef.getDataType();
		if((type == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE || type == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE_DYNAMIC
				|| type == QuestionDef.QTN_TYPE_LIST_MULTIPLE)
				&& widget instanceof ListBox){
			List options  = questionDef.getOptions();
			int defaultValueIndex = 0;
			ListBox listBox = (ListBox)widget;
			listBox.clear(); //Could be called more than once.

			listBox.addItem("","");
			if(options != null){
				for(int index = 0; index < options.size(); index++){
					OptionDef optionDef = (OptionDef)options.get(index);
					listBox.addItem(optionDef.getText(), optionDef.getVariableName());
					if(optionDef.getVariableName().equalsIgnoreCase(defaultValue))
						defaultValueIndex = index+1;
				}
			}
			listBox.setSelectedIndex(defaultValueIndex);
		}
		else if((type == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE || type == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE_DYNAMIC) && widget instanceof TextBox){ 
			MultiWordSuggestOracle oracle = new MultiWordSuggestOracle();
			FormUtil.loadOptions(questionDef.getOptions(),oracle);
			if(widget.getParent() != null){
				RuntimeWidgetWrapper copyWidget = new RuntimeWidgetWrapper(this);
				panel.remove(widget.getParent());
				panel.remove(widget);

				copyWidgetProperties(copyWidget);
				setWidth(getWidth());
				setHeight(getHeight());
			}

			SuggestBox sgstBox = new SuggestBox(oracle,(TextBox)widget);
			panel.add(sgstBox);
			sgstBox.setTabIndex(((TextBox)widget).getTabIndex());
			setupTextBoxEventListeners();
		}
		else if(type == QuestionDef.QTN_TYPE_BOOLEAN && widget instanceof ListBox){
			ListBox listBox = (ListBox)widget;
			listBox.addItem("","");
			listBox.addItem(QuestionDef.TRUE_DISPLAY_VALUE, QuestionDef.TRUE_VALUE);
			listBox.addItem(QuestionDef.FALSE_DISPLAY_VALUE ,QuestionDef.FALSE_VALUE);
			listBox.setSelectedIndex(0);

			if(defaultValue != null){
				if(defaultValue.equalsIgnoreCase(QuestionDef.TRUE_VALUE))
					listBox.setSelectedIndex(1);
				else if(defaultValue.equalsIgnoreCase(QuestionDef.FALSE_VALUE))
					listBox.setSelectedIndex(2);
			}
		}
		else if(type == QuestionDef.QTN_TYPE_LIST_MULTIPLE && defaultValue != null 
				&& defaultValue.trim().length() > 0&& binding != null 
				&& binding.trim().length() > 0 && widget instanceof CheckBox){
			if(defaultValue.contains(binding))
				((CheckBox)widget).setChecked(true);
		}

		if(widget instanceof TextBoxBase){
			((TextBoxBase)widget).setText(""); //first init just incase we have default value

			if(defaultValue != null && defaultValue.trim().length() > 0){
				if(type == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE){
					OptionDef optionDef = questionDef.getOptionWithValue(defaultValue);
					if(optionDef != null)
						((TextBox)widget).setText(optionDef.getText());
				}
				else{
					if(defaultValue.trim().length() > 0 && questionDef.isDate() && questionDef.isDateFunction(defaultValue))
						defaultValue = questionDef.getDefaultValueDisplay();
					else if(defaultValue.trim().length() > 0 && questionDef.isDate())
						defaultValue = fromSubmit2DisplayDate(defaultValue);

					((TextBoxBase)widget).setText(defaultValue);
				}
			}
		}

		if(questionDef.getDataType() == QuestionDef.QTN_TYPE_REPEAT)
			questionDef.setAnswer("0");

		//TODO Looks like this should be at the end after all widgets are loaded
		//isValid();
		
		if(!questionDef.isEnabled())
			setEnabled(false);
		if(!questionDef.isVisible())
			setVisible(false);
		if(questionDef.isLocked())
			setLocked(true);
	}

	/**
	 * Converts a date,time or dateTime from its xml submit format to display format.
	 * 
	 * @param value the text value in submit format.
	 * @return the value in its display format.
	 */
	private String fromSubmit2DisplayDate(String value){
		try{
			if(questionDef.getDataType() == QuestionDef.QTN_TYPE_TIME)
				return FormUtil.getTimeDisplayFormat().format(FormUtil.getTimeSubmitFormat().parse(value));
			else if(questionDef.getDataType() == QuestionDef.QTN_TYPE_DATE_TIME)
				return FormUtil.getDateTimeDisplayFormat().format(FormUtil.getDateTimeSubmitFormat().parse(value));
			else
				return FormUtil.getDateDisplayFormat().format(FormUtil.getDateSubmitFormat().parse(value));
		}catch(Exception ex){}
		return null;
	}

	/**
	 * Sets whether this widget is enabled.
	 * 
	 * @param enabled <code>true</code> to enable the widget, <code>false</code>
	 *        to disable it.
	 */
	public void setEnabled(boolean enabled){
		if(widget instanceof RadioButton){
			((RadioButton)widget).setEnabled(enabled);
			if(!enabled)
				((RadioButton)widget).setChecked(false);
		}
		else if(widget instanceof CheckBox){
			((CheckBox)widget).setEnabled(enabled);
			if(!enabled)
				((CheckBox)widget).setChecked(false);
		}
		else if(widget instanceof Button)
			((Button)widget).setEnabled(enabled);
		else if(widget instanceof ListBox){
			((ListBox)widget).setEnabled(enabled);
			if(!enabled)
				((ListBox)widget).setSelectedIndex(0);
		}
		else if(widget instanceof TextArea){
			((TextArea)widget).setEnabled(enabled);
			if(!enabled)
				((TextArea)widget).setText(null);
		}
		else if(widget instanceof TextBox){
			((TextBox)widget).setEnabled(enabled);
			if(!enabled)
				((TextBox)widget).setText(null);
		}
		else if(widget instanceof RuntimeGroupWidget)
			((RuntimeGroupWidget)widget).setEnabled(enabled);
	}

	/**
	 * Determines if to allow editing of the widget value.
	 * 
	 * @param locked set to true to prevent editing of the widget value.
	 */
	public void setLocked(boolean locked){
		this.locked = locked;

		if(widget instanceof RuntimeGroupWidget)
			((RuntimeGroupWidget)widget).setLocked(locked);
	}

	/**
	 * Gets the user answer from a TextBox widget.
	 * 
	 * @return the text answer.
	 */
	private String getTextBoxAnswer(){
		String value = ((TextBox)widget).getText();

		try{
			if(questionDef.isDate() && value != null && value.trim().length() > 0){
				if(questionDef.getDataType() == QuestionDef.QTN_TYPE_TIME)
					value = FormUtil.getTimeSubmitFormat().format(FormUtil.getTimeDisplayFormat().parse(value));
				else if(questionDef.getDataType() == QuestionDef.QTN_TYPE_DATE_TIME)
					value = FormUtil.getDateTimeSubmitFormat().format(FormUtil.getDateTimeDisplayFormat().parse(value));
				else 
					value = FormUtil.getDateSubmitFormat().format(FormUtil.getDateDisplayFormat().parse(value));
			}
		}
		catch(Exception ex){
			//If we get a problem parsing date, just return null.
			value = null;
		}

		return value;
	}

	/**
	 * Retrieves the value from the widget to the question definition object for this widget.
	 * 
	 * @param formDef the form to which this widget's question belongs.
	 */
	public void saveValue(FormDef formDef){
		if(questionDef == null){
			if(widget instanceof RuntimeGroupWidget)
				((RuntimeGroupWidget)widget).saveValue(formDef);

			return;
		}

		//These are not used for filling any answers. HTML is used for audio and video
		if((widget instanceof Label || widget instanceof Button) && !(widget instanceof HTML))
			return;

		String defaultValue = questionDef.getDefaultValueSubmit();

		if(widget instanceof TextBox && questionDef.getDataType() == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE){
			OptionDef optionDef = questionDef.getOptionWithText(((TextBox)widget).getText());
			if(optionDef != null)
				questionDef.setAnswer(optionDef.getVariableName());
			else
				questionDef.setAnswer(null);

			//Fire fox clears default values when the widget is disabled. So put it as the answer manually.
			if(defaultValue != null && defaultValue.trim().length() > 0 && !((TextBox)widget).isEnabled()){
				if(questionDef.getAnswer() == null || questionDef.getAnswer().trim().length() == 0)
					questionDef.setAnswer(defaultValue);
			}
		}
		else if(widget instanceof TextBox){
			String answer = getTextBoxAnswer();
			if(externalSource != null && externalSource.trim().length() > 0 &&
					questionDef.getDataType() == QuestionDef.QTN_TYPE_NUMERIC){
				answer = null;

				if(panel.getWidgetCount() == 2){
					Widget wid = panel.getWidget(1);
					if(wid instanceof Label)
						answer = ((Label)wid).getText();
				}
			}

			questionDef.setAnswer(answer);

			//Fire fox clears default values when the widget is disabled. So put it as the answer manually.
			if(defaultValue != null && defaultValue.trim().length() > 0 && !((TextBox)widget).isEnabled()){
				if(questionDef.getAnswer() == null || questionDef.getAnswer().trim().length() == 0)
					questionDef.setAnswer(defaultValue);
			}
		}
		else if(widget instanceof TextArea){
			questionDef.setAnswer(((TextArea)widget).getText());

			//Fire fox clears default values when the widget is disabled. So put it as the answer manually.
			if(defaultValue != null && defaultValue.trim().length() > 0 && !((TextArea)widget).isEnabled()){
				if(questionDef.getAnswer() == null || questionDef.getAnswer().trim().length() == 0)
					questionDef.setAnswer(defaultValue);
			}
		}
		else if(widget instanceof ListBox){
			if(questionDef.getDataType() == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE ||
					questionDef.getDataType() == QuestionDef.QTN_TYPE_BOOLEAN){
				String value = null;
				ListBox lb = (ListBox)widget;
				if(lb.getSelectedIndex() >= 0)
					value = lb.getValue(lb.getSelectedIndex());
				questionDef.setAnswer(value);
			}

			//Fire fox clears default values when the widget is disabled. So put it as the answer manually.
			if(defaultValue != null && defaultValue.trim().length() > 0 && !((ListBox)widget).isEnabled()){
				if(questionDef.getAnswer() == null || questionDef.getAnswer().trim().length() == 0)
					questionDef.setAnswer(defaultValue);
			}
		}
		else if(widget instanceof RadioButton){ //Should be before CheckBox
			if(!(questionDef.getDataType() == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE ||
					questionDef.getDataType() == QuestionDef.QTN_TYPE_BOOLEAN) || childWidgets == null)
				return;

			String value = null;

			if(questionDef.getDataType() == QuestionDef.QTN_TYPE_BOOLEAN)
				value = questionDef.getAnswer();
			else{
				for(int index=0; index < childWidgets.size(); index++){
					RuntimeWidgetWrapper childWidget = childWidgets.get(index);
					String binding = childWidget.getBinding();
					if(((RadioButton)((RuntimeWidgetWrapper)childWidget).getWrappedWidget()).isChecked() && binding != null){
						value = binding;
						break;
					}
				}
			}

			questionDef.setAnswer(value);
		}
		else if(widget instanceof CheckBox){
			if(questionDef.getDataType() != QuestionDef.QTN_TYPE_LIST_MULTIPLE || childWidgets == null)
				return;

			String value = "";
			for(int index=0; index < childWidgets.size(); index++){
				RuntimeWidgetWrapper childWidget = childWidgets.get(index);
				String binding = childWidget.getBinding();
				if(((CheckBox)((RuntimeWidgetWrapper)childWidget).getWrappedWidget()).isChecked() && binding != null){
					if(value.length() != 0)
						value += " ";
					value += binding;
				}
			}

			questionDef.setAnswer(value);
		}
		else if(widget instanceof RuntimeGroupWidget)
			((RuntimeGroupWidget)widget).saveValue(formDef);

		//Repeat widgets have a value for row count which does not go anywhere in the model
		if(!(widget instanceof RuntimeGroupWidget))
			questionDef.updateNodeValue(formDef);
	}

	/**
	 * Adds a CheckBox or RadioButton widget for the question of this widget.
	 * 
	 * @param childWidget the CheckBox or RadioButton widget.
	 */
	public void addChildWidget(RuntimeWidgetWrapper childWidget){
		if(childWidgets == null)
			childWidgets = new ArrayList<RuntimeWidgetWrapper>();
		childWidgets.add(childWidget);

		String defaultValue = questionDef.getDefaultValue();
		int type = questionDef.getDataType();
		if((type == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE ||
				type == QuestionDef.QTN_TYPE_LIST_MULTIPLE)
				&& widget instanceof CheckBox && defaultValue != null){ 
			if(childWidgets.size() == questionDef.getOptions().size()){
				for(int index=0; index < childWidgets.size(); index++){
					RuntimeWidgetWrapper kidWidget = childWidgets.get(index);
					if((type == QuestionDef.QTN_TYPE_LIST_MULTIPLE && defaultValue.contains(kidWidget.getBinding())) ||
							(type == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE && defaultValue.equals(kidWidget.getBinding()))){

						((CheckBox)((RuntimeWidgetWrapper)kidWidget).getWrappedWidget()).setChecked(true);
						if(type == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE)
							break; //for this we can't select more than one.
					}
				}
			}
		}

		((CheckBox)childWidget.getWrappedWidget()).addClickListener(new ClickListener(){
			public void onClick(Widget sender){
				if(questionDef.getDataType() == QuestionDef.QTN_TYPE_LIST_EXCLUSIVE)
					questionDef.setAnswer(((RuntimeWidgetWrapper)sender.getParent().getParent()).getBinding());
				else{
					String answer = "";
					for(int index=0; index < childWidgets.size(); index++){
						RuntimeWidgetWrapper childWidget = childWidgets.get(index);
						String binding = childWidget.getBinding();
						if(((CheckBox)((RuntimeWidgetWrapper)childWidget).getWrappedWidget()).isChecked() && binding != null){
							if(answer.length() > 0)
								answer += " , ";
							answer += binding;
						}
					}
					questionDef.setAnswer(answer);
				}
				isValid();
				editListener.onValueChanged((RuntimeWidgetWrapper)panel.getParent());
			}
		});
	}

	/**
	 * Get's the widget that is wrapped by this widget.
	 */
	public Widget getWrappedWidget(){
		return widget;
	}


	//These taken from question data.
	public boolean isValid(){
		if(widget instanceof Label || widget instanceof Button || questionDef == null || 
				(widget instanceof CheckBox && childWidgets == null)){

			if(widget instanceof RuntimeGroupWidget)
				return ((RuntimeGroupWidget)widget).isValid();

			return true;
		}

		if(questionDef.isRequired() && !this.isAnswered()){
			if(panel.getWidgetCount() < 2)
				panel.add(errorImage);

			errorImage.setTitle(LocaleText.get("requiredErrorMsg"));
			return false;
		}

		if(questionDef.getDataType() == QuestionDef.QTN_TYPE_REPEAT){
			boolean valid = ((RuntimeGroupWidget)widget).isValid();
			if(!valid)
				return false;
		}

		//For some reason the validation rule object, before saving, has a formdef different
		//from the one we are using and hence need to update it
		if(validationRule != null){
			FormDef formDef = questionDef.getParentFormDef();
			if(formDef != validationRule.getFormDef())
				validationRule.setFormDef(formDef);
		}

		if(validationRule != null && !validationRule.isValid()){
			if(panel.getWidgetCount() < 2)
				panel.add(errorImage);

			errorImage.setTitle(validationRule.getErrorMessage());
			return false;
		}
		/*FormDef formDef = null;
		ValidationRule rule = new ValidationRule();
		if(!rule.isValid(formDef)){

		}*/

		if(panel.getWidgetCount() > 1)
			panel.remove(errorImage);
		return true;
	}

	/**
	 * Check if the question represented by this widget has been answered.
	 * 
	 * @return true if answered, else false.
	 */
	public boolean isAnswered(){
		return getAnswer() != null && getAnswer().toString().trim().length() > 0;
	}

	/**
	 * Gets the answer for the question wrapped by the widget.
	 * 
	 * @return the answer.
	 */
	private Object getAnswer() {
		if(questionDef == null)
			return null;

		return questionDef.getAnswer();
	}

	/**
	 * Sets input focus to the widget.
	 * 
	 * @return true if the widget accepts input focus.
	 */
	public boolean setFocus(){
		if(questionDef != null && (!questionDef.isVisible() || !questionDef.isEnabled() || questionDef.isLocked()))
			return false;

		//Browser does not seem to set focus to check boxes and radio buttons

		/*if(widget instanceof RadioButton)
			((RadioButton)widget).setFocus(true);
		else if(widget instanceof CheckBox)
			((CheckBox)widget).setFocus(true);
		else*/ if(widget instanceof ListBox)
			((ListBox)widget).setFocus(true);
		else if(widget instanceof TextArea){
			((TextArea)widget).setFocus(true);
			((TextArea)widget).selectAll();
		}
		else if(widget instanceof TextBox){
			((TextBox)widget).setFocus(true);
			((TextBox)widget).selectAll();
		}
		else if(widget instanceof RuntimeGroupWidget)
			((RuntimeGroupWidget)widget).setFocus();
		else
			return false;
		return true;
	}

	/**
	 * Clears the value or answer entered for this widget.
	 */
	public void clearValue(){
		if(widget instanceof RadioButton)
			((RadioButton)widget).setChecked(false);
		else if(widget instanceof CheckBox)
			((CheckBox)widget).setChecked(false);
		else if(widget instanceof ListBox)
			((ListBox)widget).setSelectedIndex(-1);
		else if(widget instanceof TextArea)
			((TextArea)widget).setText(null);
		else if(widget instanceof TextBox)
			((TextBox)widget).setText(null);
		else if(widget instanceof DatePicker)
			((DatePicker)widget).setText(null);
		else if(widget instanceof Image)
			((Image)widget).setUrl(null);
		else if(widget instanceof RuntimeGroupWidget)
			((RuntimeGroupWidget)widget).clearValue();

		if(questionDef != null)
			questionDef.setAnswer(null);
	}

	/**
	 * @see org.purc.purcforms.client.controller.QuestionChangeListener#onEnabledChanged(QuestionDef, boolean)
	 */
	public void onEnabledChanged(QuestionDef sender,boolean enabled) {
		if(!enabled)
			clearValue();

		setEnabled(enabled);
	}

	/**
	 * @see org.purc.purcforms.client.controller.QuestionChangeListener#onLockedChanged(QuestionDef, boolean)
	 */
	public void onLockedChanged(QuestionDef sender,boolean locked) {
		if(locked)
			clearValue();

		setLocked(locked);
	}

	/**
	 * @see org.purc.purcforms.client.controller.QuestionChangeListener#onRequiredChanged(QuestionDef, boolean)
	 */
	public void onRequiredChanged(QuestionDef sender,boolean required) {
		//As for now we do not set error messages on labels.
		if(!(widget instanceof Label)){
			if(!required && panel.getWidgetCount() > 1)
				panel.remove(errorImage);
			else if(required && panel.getWidgetCount() < 2)
				panel.add(errorImage);
		}
	}

	/**
	 * @see org.purc.purcforms.client.controller.QuestionChangeListener#onVisibleChanged(QuestionDef, boolean)
	 */
	public void onVisibleChanged(QuestionDef sender,boolean visible) {
		if(!visible)
			clearValue();

		setVisible(visible);
	}

	/**
	 * @see org.purc.purcforms.client.controller.QuestionChangeListener#onBindingChanged(QuestionDef, String)
	 */
	public void onBindingChanged(QuestionDef sender,String newValue){
		if(newValue != null && newValue.trim().length() > 0)
			binding = newValue;
	}

	/**
	 * Gets the question wrapped by this widget.
	 * 
	 * @return the question definition object.
	 */
	public QuestionDef getQuestionDef(){
		return questionDef;
	}

	/**
	 * @see org.purc.purcforms.client.controller.QuestionChangeListener#onDataTypeChanged(QuestionDef, int)
	 */
	public void onDataTypeChanged(QuestionDef sender,int dataType){

	}

	/**
	 * Gets this widget's validation rule.
	 *
	 * @return the widget's validation rule.
	 */
	public ValidationRule getValidationRule() {
		return validationRule;
	}

	/**
	 * Sets the widget's validation rule.
	 * 
	 * @param validationRule the validation rule.
	 */
	public void setValidationRule(ValidationRule validationRule) {
		this.validationRule = validationRule;
	}

	/**
	 * @see org.purc.purcforms.client.controller.QuestionChangeListener#onOptionsChanged(QuestionDef, List)
	 */
	public void onOptionsChanged(QuestionDef sender,List<OptionDef> optionList){
		loadQuestion();
	}

	public RuntimeWidgetWrapper getInvalidWidget(){
		if(widget instanceof RuntimeGroupWidget)
			return ((RuntimeGroupWidget)widget).getInvalidWidget();
		return this;
	}

	/**
	 * Checks if this widget accepts input focus.
	 * 
	 * @return true if the widget accepts input focus, else false.
	 */
	public boolean isFocusable(){
		Widget wg = getWrappedWidget();
		return (wg instanceof TextBox || wg instanceof TextArea || wg instanceof DatePicker ||
				wg instanceof CheckBox || wg instanceof RadioButton || 
				wg instanceof RuntimeGroupWidget || wg instanceof ListBox);
	}
}