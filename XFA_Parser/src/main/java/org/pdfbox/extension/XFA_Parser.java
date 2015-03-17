package org.pdfbox.extension;

/*
 * XFA-PDF Parser
 * 
 * By Enrico Anello
 */

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.pdfbox.extension.objects.Field;
import org.pdfbox.extension.objects.Subform;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XFA_Parser {
	
private FileInputStream file;

	private Document domDocument;
	private List<HashMap<String, Integer>> currentSubFormsIndexes = new ArrayList<HashMap<String, Integer>>();
	
	private Subform parsedSubForms = new Subform();
	
	public XFA_Parser(String source) throws FileNotFoundException {
		file = new FileInputStream(source);
	}
	
	public void parse() {
		try {
			this.domDocument = this.getDocument();
			NodeList nodeListForm = this.domDocument.getElementsByTagName("form").item(0).getChildNodes();
			
			this.iterateDom(nodeListForm);
			this.printTree(parsedSubForms);
			System.out.println("Break!");
		} catch (IOException | ParserConfigurationException | SAXException e) {
			e.printStackTrace();
		}

	}
	
	private Document getDocument() throws IOException, ParserConfigurationException, SAXException {
		
		PDDocument document = PDDocument.load(file);
		file.close();
        document.setAllSecurityToBeRemoved(true);
        PDAcroForm form = document.getDocumentCatalog().getAcroForm();
        Document documentXML = form.getXFA().getDocument();
        
        documentXML.getDocumentElement().normalize();
        
        return documentXML;
		
	}
	
	private void iterateDom(NodeList dom) {
		Integer index = 0;
		for (int i = 0; i < dom.getLength(); i++) {
			String nodeName = dom.item(i).getNodeName();
			if (nodeName.equalsIgnoreCase("subform")) {
				String nodeAttr = this.getNameAttributeFromNode(dom.item(i));
				this.parsedSubForms.setIndex(index);
				this.parsedSubForms.setName(nodeAttr);
				this.parsedSubForms.setSubform(this.iterateSubForm(dom.item(i).getChildNodes(), 0));
				index = index + 1;
			}
		}
		this.parsedSubForms = this.iterateFields(this.parsedSubForms, this.clone(this.parsedSubForms));
	}
	
	private Subform iterateFields(Subform subForm, Subform original) {
		if (this.currentSubFormsIndexes.size() < 1) {
			HashMap<String, Integer> hm = new HashMap<String, Integer>();
			hm.put(original.getName(), original.getIndex());
			this.currentSubFormsIndexes.add(hm);
		}
		original.setSubform(new ArrayList<Subform>());
		List<Subform> listSf = new ArrayList<Subform>();
		for (Subform sf : subForm.getSubform()) {
			HashMap<String, Integer> hm = new HashMap<String, Integer>();
			hm.put(sf.getName(), sf.getIndex());
			this.currentSubFormsIndexes.add(hm);
			sf.setFields(this.getFields(this.currentSubFormsIndexes));
			listSf.add(sf);
			iterateFields(sf, this.clone(subForm));
		}
		original.setSubform(listSf);
		this.currentSubFormsIndexes.remove(this.currentSubFormsIndexes.size() - 1);
		return original;
	}
	
	private List<Field> getFields(List<HashMap<String, Integer>> bredCrumbs) {
		NodeList rootData = this.domDocument.getElementsByTagName("xfa:data").item(0).getChildNodes();		
		NodeList itemData = rootData;
		for (HashMap<String, Integer> bc : bredCrumbs) {
			String name = "";
			int index = 0;
			for (String key : bc.keySet()) {
				name = key;
				index = bc.get(key);
			}
			
			int currIndex = 0;
			for (int i = 0; i < itemData.getLength(); i++) {
				if (itemData.item(i).getNodeName().equals(name)) {
					if (index == currIndex) {
						itemData = itemData.item(i).getChildNodes();
						break;
					} else {
						currIndex++;
					}
				}
			}
		}
		
		List<Field> fields = new ArrayList<Field>();
		for (int i = 0; i < itemData.getLength(); i++) {
			Field field = new Field();
			Node node = itemData.item(i);
			field.setName(node.getNodeName());
			if (node.getNodeName().equalsIgnoreCase("q11Table")) {
				System.out.println("DD");
			}
			if (node.getFirstChild() != null) {
				field.setValue(node.getFirstChild().getNodeValue());
			}
			fields.add(field);
		}
		return fields;
	}
	
	private List<Subform> iterateSubForm(NodeList dom, int iterations) {
		List<Subform> subforms = new ArrayList<Subform>();
		
		for (int i = 0; i < dom.getLength(); i++) {
			String nodeName = dom.item(i).getNodeName();
			
			if (nodeName.equalsIgnoreCase("subform")) {
				Subform current = new Subform();
				String nodeAttr = this.getNameAttributeFromNode(dom.item(i));
				current.setName(nodeAttr);
				current.setSubform(this.iterateSubForm(dom.item(i).getChildNodes(), iterations+1));
				current.setIndex(0);
				subforms.add(current);
			}
		}
		for (int i = 0; i < subforms.size(); i++) {
			for (int j = 0; j < i; j++) {
				if (subforms.get(j).getName().equals(subforms.get(i).getName())) {
					Subform temp = this.clone(subforms.get(i));
					temp.setIndex(subforms.get(j).getIndex() + 1);
					subforms.set(i, temp);
				}
			}
		}
		return subforms;
	}
	
	private List<String> getSubFormNames(NodeList dom) {
		List<String> subforms = new ArrayList<String>();
		for (int i = 0; i < dom.getLength(); i++) {
			String nodeName = dom.item(i).getNodeName();
			if (nodeName.equalsIgnoreCase("subform")) {
				String nodeAttr = this.getNameAttributeFromNode(dom.item(i));
				subforms.add(nodeAttr);
			}
		}
		return subforms;
	}
	
	private String getNameAttributeFromNode(Node node) {
		NamedNodeMap attributes = node.getAttributes();
		if (attributes != null) {
			Node attributeNode = attributes.getNamedItem("name");
			if (attributeNode != null) {
				String name = attributes.getNamedItem("name").getNodeValue();
				if (name != null) {
					return name;
				} else {
					return null;
				}
			} else {
				return null;
			}
		} else {
			return null;
		}
	}
	
	public void printTree(Subform sf) {
		this.printTree(sf, 0);
	}
	
	public void printTree(Subform sf, int row) {
		String sep = StringUtils.repeat("-", row);
		System.out.println(sep + " " + sf.getName());
		if (sf.getSubform().size() > 0) {
			for (Subform sf1 : sf.getSubform()) {
				this.printTree(sf1, row + 1);
			}
		}
	}
	
	private Subform clone(Subform sf) {
		return new Subform(sf);
	}

}
