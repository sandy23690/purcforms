package org.purc.purcforms.client.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.purc.purcforms.client.xforms.XformConverter;

import com.google.gwt.xml.client.Document;
import com.google.gwt.xml.client.Element;


/** The definition of a page in a form or questionaire. 
 * 
 * @author Daniel Kayiwa
 *
 */
public class PageDef implements Serializable{

	/** A list of questions on a page. */
	private Vector questions;

	/** The page number. */
	private int pageNo = ModelConstants.NULL_ID;

	/** The name of the page. */
	private String name = ModelConstants.EMPTY_STRING;

	private Element labelNode;
	private Element groupNode;

	private FormDef parent;

	public PageDef(FormDef parent) {
		this.parent = parent;
	}

	public PageDef(PageDef pageDef,FormDef parent) {
		this(parent);
		setPageNo(pageDef.getPageNo());
		setName(pageDef.getName());
		copyQuestions(pageDef.getQuestions());
	}

	public PageDef(String name, int pageNo,FormDef parent) {
		this(parent);
		setName(name);
		setPageNo(pageNo);
		setQuestions(questions);
	}

	public PageDef(String name, int pageNo,Vector questions,FormDef parent) {
		this(parent);
		setName(name);
		setPageNo(pageNo);
		setQuestions(questions);
	}

	public int getPageNo() {
		return pageNo;
	}

	public void setPageNo(int pageNo) {
		this.pageNo = pageNo;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Vector getQuestions() {
		return questions;
	}

	public FormDef getParent() {
		return parent;
	}

	public void setParent(FormDef parent) {
		this.parent = parent;
	}

	public int getQuestionCount(){
		if(questions == null)
			return 0;
		return questions.size();
	}

	public QuestionDef getQuestionAt(int index){
		if(questions == null)
			return null;
		return (QuestionDef)questions.elementAt(index);
	}

	/**
	 * @return the labelNode
	 */
	public Element getLabelNode() {
		return labelNode;
	}

	/**
	 * @param labelNode the labelNode to set
	 */
	public void setLabelNode(Element labelNode) {
		this.labelNode = labelNode;
	}

	/**
	 * @return the groupNode
	 */
	public Element getGroupNode() {
		return groupNode;
	}

	/**
	 * @param groupNode the groupNode to set
	 */
	public void setGroupNode(Element groupNode) {
		this.groupNode = groupNode;
	}

	public void addQuestion(QuestionDef qtn){
		if(questions == null)
			questions = new Vector();
		questions.addElement(qtn);
		qtn.setParent(this);
	}

	public void setQuestions(Vector questions) {
		this.questions = questions;
	}

	public QuestionDef getQuestion(String varName){
		if(questions == null)
			return null;
		
		for(int i=0; i<getQuestions().size(); i++){
			QuestionDef def = (QuestionDef)getQuestions().elementAt(i);
			if(def.getVariableName().equals(varName))
				return def;
			else{
				/*String binding = def.getVariableName();
				if(varName.endsWith(binding) && parent != null){
					if(!binding.startsWith("/")) 
						binding = "/"+binding;
					binding  = parent.getVariableName() + binding;
					if(!binding.startsWith("/")) 
						binding = "/"+binding;
					if(binding.equals(varName))
						return def;
				}*/
				if(def.getDataType() == QuestionDef.QTN_TYPE_REPEAT){ //TODO Need to make sure this new addition does not introduce bugs
					def = def.getRepeatQtnsDef().getQuestion(varName);
					if(def != null)
						return def;
				}
			}
		}

		return null;
	}

	public QuestionDef getQuestion(int id){
		if(questions == null)
			return null;
		
		for(int i=0; i<getQuestions().size(); i++){
			QuestionDef def = (QuestionDef)getQuestions().elementAt(i);
			if(def.getId() == id)
				return def;
		}

		return null;
	}

	public String toString() {
		return getName();
	}

	private void copyQuestions(Vector questions){
		this.questions = new Vector();
		for(int i=0; i<questions.size(); i++)
			this.questions.addElement(new QuestionDef((QuestionDef)questions.elementAt(i),this));
	}

	public boolean removeQuestion(QuestionDef qtnDef){
		if(qtnDef.getControlNode() != null){
			if(qtnDef.getDataType() == QuestionDef.QTN_TYPE_REPEAT)
				qtnDef.getControlNode().getParentNode().getParentNode().removeChild(qtnDef.getControlNode().getParentNode());
			else
				qtnDef.getControlNode().getParentNode().removeChild(qtnDef.getControlNode());
		}
		if(qtnDef.getDataNode() != null)
			qtnDef.getDataNode().getParentNode().removeChild(qtnDef.getDataNode());
		if(qtnDef.getBindNode() != null && qtnDef.getBindNode() != null)
			qtnDef.getBindNode().getParentNode().removeChild(qtnDef.getBindNode());

		return questions.removeElement(qtnDef);
	}

	public boolean removeQuestionEx(QuestionDef qtnDef){
		return questions.removeElement(qtnDef);
	}

	public void removeAllQuestions(){
		/*for(int i=0; i<questions.size(); i++)
			removeQuestion((QuestionDef)questions.elementAt(i));

		questions.removeAllElements();*/
		if(questions == null)
			return;
		
		while(questions.size() > 0)
			removeQuestion((QuestionDef)questions.elementAt(0));
	}

	public int size(){
		if(questions == null)
			return 0;
		return questions.size();
	}

	public void moveQuestionUp(QuestionDef questionDef){
		int index = questions.indexOf(questionDef);

		questions.remove(questionDef);

		//Not relying on group node because some forms have no groups
		Element controlNode = questionDef.getControlNode();
		Element parentNode = controlNode != null ? (Element)controlNode.getParentNode() : null;
		if(questionDef.getDataType() == QuestionDef.QTN_TYPE_REPEAT && controlNode != null){
			controlNode = (Element)controlNode.getParentNode();
			parentNode = (Element)parentNode.getParentNode();
		}

		if(controlNode != null)
			parentNode.removeChild(controlNode);

		if(questionDef.getDataNode() != null)
			questionDef.getDataNode().getParentNode().removeChild(questionDef.getDataNode());
		if(questionDef.getBindNode() != null)
			questionDef.getBindNode().getParentNode().removeChild(questionDef.getBindNode());

		QuestionDef currentQuestionDef;
		List list = new ArrayList();

		while(questions.size() >= index){
			currentQuestionDef = (QuestionDef)questions.elementAt(index-1);
			list.add(currentQuestionDef);
			questions.remove(currentQuestionDef);
		}

		questions.add(questionDef);
		for(int i=0; i<list.size(); i++){
			if(i == 0 && controlNode != null){
				QuestionDef qtnDef = (QuestionDef)list.get(i);
				if(qtnDef.getControlNode() != null)
					parentNode.insertBefore(controlNode, qtnDef.getControlNode());

				if(qtnDef.getDataNode() != null)
					qtnDef.getDataNode().getParentNode().insertBefore(questionDef.getDataNode(), qtnDef.getDataNode());
				if(qtnDef.getBindNode() != null)
					qtnDef.getBindNode().getParentNode().insertBefore(questionDef.getBindNode(), qtnDef.getBindNode());
			}
			questions.add(list.get(i));
		}
	}

	public void moveQuestionDown(QuestionDef questionDef){
		int index = questions.indexOf(questionDef);	

		questions.remove(questionDef);

		//Not relying on group node because some forms have no groups
		Element controlNode = questionDef.getControlNode();
		Element parentNode = controlNode != null ? (Element)controlNode.getParentNode() : null;
		if(questionDef.getDataType() == QuestionDef.QTN_TYPE_REPEAT && controlNode != null){
			controlNode = (Element)controlNode.getParentNode();
			parentNode = (Element)parentNode.getParentNode();
		}

		if(controlNode != null)
			parentNode.removeChild(questionDef.getControlNode());

		if(questionDef.getDataNode() != null)
			questionDef.getDataNode().getParentNode().removeChild(questionDef.getDataNode());
		if(questionDef.getBindNode() != null)
			questionDef.getBindNode().getParentNode().removeChild(questionDef.getBindNode());

		QuestionDef currentItem; // = parent.getChild(index - 1);
		List list = new ArrayList();

		while(questions.size() > 0 && questions.size() > index){
			currentItem = (QuestionDef)questions.elementAt(index);
			list.add(currentItem);
			questions.remove(currentItem);
		}

		for(int i=0; i<list.size(); i++){
			if(i == 1){
				questions.add(questionDef); //Add after the first item.

				if(controlNode != null){
					QuestionDef qtnDef = (QuestionDef)list.get(i);
					if(qtnDef.getControlNode() != null)
						parentNode.insertBefore(questionDef.getControlNode(), qtnDef.getControlNode());

					if(qtnDef.getDataNode() != null)
						qtnDef.getDataNode().getParentNode().insertBefore(questionDef.getDataNode(), qtnDef.getDataNode());
					if(qtnDef.getBindNode() != null)
						qtnDef.getBindNode().getParentNode().insertBefore(questionDef.getBindNode(), qtnDef.getBindNode());
				}
			}
			questions.add(list.get(i));
		}

		if(list.size() == 1){
			questions.add(questionDef);

			if(controlNode != null){
				if(questionDef.getControlNode() != null)
					parentNode.appendChild(questionDef.getControlNode());

				if(questionDef.getDataNode() != null)
					questionDef.getDataNode().getParentNode().insertBefore(questionDef.getDataNode(), questionDef.getDataNode());
				if(questionDef.getBindNode() != null)
					questionDef.getBindNode().getParentNode().insertBefore(questionDef.getBindNode(), questionDef.getBindNode());
			}
		}
	}

	public boolean contains(QuestionDef qtn){
		return questions.contains(qtn);
	}

	public void updateDoc(Document doc, Element xformsNode, FormDef formDef, Element formNode, Element modelNode, boolean withData){
		if(labelNode == null && groupNode == null && areAllQuestionsNew()) //Must be new page{
			XformConverter.fromPageDef2Xform(this,doc,xformsNode,formDef,formNode,modelNode);

		if(labelNode != null)
			XformConverter.setTextNodeValue(labelNode,name);

		for(int i=0; i<questions.size(); i++){
			QuestionDef questionDef = (QuestionDef)questions.elementAt(i);
			if(questionDef.updateDoc(doc,xformsNode,formDef,formNode,modelNode,(groupNode == null) ? xformsNode : groupNode,true,withData)){
				//for(int k=0; k<i; k++)
				//moveQuestionUp(questionDef);
			}
		}
	}

	private boolean areAllQuestionsNew(){
		for(int i=0; i<questions.size(); i++){
			QuestionDef questionDef = (QuestionDef)questions.elementAt(i);
			if(questionDef.getControlNode() != null)
				return false;
		}
		return true;
	}

	public QuestionDef getQuestionWithText(String text){
		if(questions == null)
			return null;
		
		for(int i=0; i<questions.size(); i++){
			QuestionDef questionDef = (QuestionDef)questions.elementAt(i);
			if(questionDef.getText().equals(text))
				return questionDef;
			else if(questionDef.getDataType() == QuestionDef.QTN_TYPE_REPEAT){ //TODO Need to make sure this new addition does not introduce bugs
				questionDef = questionDef.getRepeatQtnsDef().getQuestionWithText(text);
				if(questionDef != null)
					return questionDef;
			}
		}
		return null;
	}
	
	/**
	 * Updates this pageDef (as the main) with the parameter one
	 * 
	 * @param pageDef
	 */
	public void refresh(PageDef pageDef){
		if(pageNo == pageDef.getPageNo())
			name = pageDef.getName();
		
		int count = pageDef.getQuestionCount();
		for(int index = 0; index < count; index++){
			QuestionDef qtn = pageDef.getQuestionAt(index);
			QuestionDef questionDef = this.getQuestion(qtn.getVariableName());
			if(questionDef == null)
				continue; //Possibly this question was deleted on server
			questionDef.refresh(qtn);
		}
	}
}