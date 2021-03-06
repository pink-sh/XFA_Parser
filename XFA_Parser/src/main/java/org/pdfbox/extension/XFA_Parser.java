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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.pdfbox.extension.objects.XFAField;
import org.pdfbox.extension.objects.XFASubform;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class XFA_Parser {
	
private FileInputStream file;

	private Document domDocument;
	private List<HashMap<String, Integer>> currentSubFormsIndexes = new ArrayList<HashMap<String, Integer>>();
	
	private XFASubform parsedSubForms = new XFASubform();
	
	private List<XFAField> fields = new ArrayList<XFAField>();
	private List<XFASubform> subforms = new ArrayList<XFASubform>();
		
	public XFA_Parser(String source) throws FileNotFoundException {
		file = new FileInputStream(source);
	}
	
	public void parse() {
		try {
			this.domDocument = this.getDocument();
			NodeList nodeListForm = this.domDocument.getElementsByTagName("form").item(0).getChildNodes();
			
			this.iterateDom(nodeListForm);
			//this.printTree(parsedSubForms);
		} catch (IOException | ParserConfigurationException | SAXException e) {
			e.printStackTrace();
		}
	}
	
	public XFASubform getTree() {
		return this.parsedSubForms;
	}
	
	public void printTree(XFASubform sf) {
		this.printTree(sf, 0);
	}
	
	public void printTree() {
		this.printTree(this.parsedSubForms, 0);
	}
	
	public List<XFAField> getFieldById(String id) {		
		if (this.parsedSubForms.getFields() != null && this.parsedSubForms.getFields().size() > 0) {
			for (XFAField f : this.parsedSubForms.getFields()) {
				if (f.getName().equals(id)) {
					this.fields.add(f);
				}
			}
		}
		this.iterateFields(id, this.parsedSubForms.getSubform());
		return this.fields;
	}
	
	public List<XFASubform> getSubFormById(String id) {
		if (this.parsedSubForms.getName().equals(id)) {
			this.subforms.add(this.parsedSubForms);
		}
		if (this.parsedSubForms.getSubform() != null && this.parsedSubForms.getSubform().size() > 0) {
			this.iterateSubForms(id, this.parsedSubForms.getSubform());
		}
		return this.subforms;
	}
	
	public XFAField getFieldByXPath(String path) {
		List<Map<String, Integer>> sp = this.builPathMap(path);
		String[] split = path.split("/");
		String newPath = "";
		for (int i = 0; i < split.length - 1; i++) {
			newPath += split[i] + "/";
		}
		newPath = newPath.substring(0, newPath.length()-1);
		
		XFASubform sf = this.getSubformByXPath(newPath);
		
		if (sf == null) { return null; }
		
		String name = "";
		Integer index = 1;
		for (String key : sp.get(sp.size() - 1).keySet()) {
			name = key;
			index = sp.get(sp.size() - 1).get(key);
		}
		
		Integer counter = 0;
		for (XFAField fld : sf.getFields()) {
			if (fld.getName().equals(name)) {
				counter = counter + 1;
				if (counter == index) {
					return fld;
				}
			}
		}
		
		return null;
	}
	
	public XFASubform getSubformByXPath(String path) {
		List<Map<String, Integer>> sp = this.builPathMap(path);
		
		List<XFASubform> current = this.parsedSubForms.getSubform();
		XFASubform lastSf = new XFASubform();
		for (int i = 0; i < sp.size(); i++) {
			String name = "";
			Integer index = 1;
			for (String key : sp.get(i).keySet()) {
				name = key;
				index = sp.get(i).get(key);
			}
			int counter = 0;
			boolean found = false;
			for (XFASubform cur : current) {
				if (cur.getName().equals(name)) {
					counter = counter + 1;
					if (counter == index) {
						current = cur.getSubform();
						lastSf = cur;
						found = true;
						break;
					}
				}
			}
			if (!found) {
				return null;
			}
		}
		return lastSf;
	}
	
	public int getNumberOfItemsByXPath(String path) {
		List<Map<String, Integer>> sp = this.builPathMap(path);
		String newPath = "";
		for (int i = 0; i < sp.size() - 1; i++) {
			String name = "";
			int index = 1;
			for (String key : sp.get(i).keySet()) {
				name = key;
				index = sp.get(i).get(key);
			}
			newPath = newPath + name + "[" + Integer.toString(index) + "]" + "/";
		}
		XFASubform sf = this.getSubformByXPath(newPath);
		if (sf == null) { return 0; }
		
		String name = "";
		for (String key : sp.get(sp.size()-1).keySet()) {
			name = key;
		}
		
		int counter = 0;
		if (sf.getFields() != null && sf.getFields().size() > 0) {
			List<XFAField> listField = sf.getFields();
			for (XFAField f : listField) {
				if (f.getName().equals(name)) {
					counter = counter + 1;
				}
			}
		}
		return counter;
	}
	
	private void iterateSubForms(String id, List<XFASubform> current) {
		if (current.size() > 0) {
			for (XFASubform sf : current) {
				if (sf.getName() != null) {
					if (sf.getName().equals(id)) {
						this.subforms.add(sf);
					}
				}
				if (sf.getSubform() != null && sf.getSubform().size() > 0) {
					this.iterateSubForms(id, sf.getSubform());
				}
			}
		}
	}
	
	private void iterateFields(String id, List<XFASubform> current) {
		for (XFASubform sf : current) {
			if (sf.getFields() != null && sf.getFields().size() > 0) {
				for (XFAField f : sf.getFields()) {
					if (f.getName() != null) {
						if (f.getName().equals(id)) {
							this.fields.add(f);
						}
					}
				}
			}
			if (sf.getSubform() != null && sf.getSubform().size() > 0) {
				this.iterateFields(id, sf.getSubform());
			}
		}
	}
	
	private Document getDocument() throws IOException, ParserConfigurationException, SAXException {
		PDDocument document = PDDocument.load(file);
		file.close();
		document.setAllSecurityToBeRemoved(true);
		PDAcroForm form = document.getDocumentCatalog().getAcroForm();
		Document documentXML = form.getXFA().getDocument();        
		documentXML.getDocumentElement().normalize();
		document.close();
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
		this.parsedSubForms = this.iterateFields(this.parsedSubForms);
	}
	
	private XFASubform iterateFields (XFASubform subform) {
		XFASubform original = this.clone(subform);
		if (this.currentSubFormsIndexes.size() < 1) {
			HashMap<String, Integer> hm = new HashMap<String, Integer>();
			hm.put(original.getName(), original.getIndex());
			this.currentSubFormsIndexes.add(hm);
		}
		original.setFields(this.getFields(this.currentSubFormsIndexes));
		return this.iterateFields(subform, original);
	}
	
	private XFASubform iterateFields(XFASubform subForm, XFASubform original) {
		if (this.currentSubFormsIndexes.size() < 1) {
			HashMap<String, Integer> hm = new HashMap<String, Integer>();
			hm.put(original.getName(), original.getIndex());
			this.currentSubFormsIndexes.add(hm);
		}
		original.setSubform(new ArrayList<XFASubform>());
		List<XFASubform> listSf = new ArrayList<XFASubform>();
		for (XFASubform sf : subForm.getSubform()) {
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
	
	private List<XFAField> getFields(List<HashMap<String, Integer>> bredCrumbs) {
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
		
		List<XFAField> fields = new ArrayList<XFAField>();
		for (int i = 0; i < itemData.getLength(); i++) {
			XFAField field = new XFAField();
			Node node = itemData.item(i);
			field.setName(node.getNodeName());
			if (node.getFirstChild() != null) {
				String value = "";
				if (node.getFirstChild().getNodeName().equalsIgnoreCase("CheckBox")) {
					for (int x = 0; x < node.getChildNodes().getLength(); x++) {
						if (node.getChildNodes().item(x).getNodeName().equalsIgnoreCase("CheckBox")) {
							value = value + node.getChildNodes().item(x).getFirstChild().getNodeValue().replace(",", "\\,") + ",";
						} else {
							XFAField s = new XFAField();
							if (node.getChildNodes().item(x).getFirstChild() != null) {
								s.setName(node.getChildNodes().item(x).getFirstChild().getNodeName());
								s.setValue(node.getChildNodes().item(x).getFirstChild().getNodeValue());
							} else if (node.getChildNodes().item(x).getNodeName() != null) {
								s.setName(node.getChildNodes().item(x).getNodeName());
								s.setValue(node.getChildNodes().item(x).getNodeValue());
							}
							fields.add(s);
						}
					}
					value = value.replaceAll("(,)*$", "");
				}
				else if (node.getFirstChild().getNodeName().equalsIgnoreCase("value")) {
					for (int x = 0; x < node.getChildNodes().getLength(); x++) {
						//value = value + node.getChildNodes().item(x).getNodeValue().replace(",", "\\,") + ",";
						if (node.getChildNodes().item(x).getNodeName().equalsIgnoreCase("value")) {
							value = value + node.getChildNodes().item(x).getFirstChild().getNodeValue().replace(",", "\\,") + ",";
						} else {
							XFAField s = new XFAField();
							s.setName(node.getChildNodes().item(x).getFirstChild().getNodeName());
							s.setValue(node.getChildNodes().item(x).getFirstChild().getNodeValue());
							fields.add(s);
						}
					}
					value = value.replaceAll("(,)*$", "");
				} else {
					value = node.getFirstChild().getNodeValue();
					if (value == null && node.getFirstChild().getFirstChild() != null) {
						value = node.getFirstChild().getFirstChild().getNodeValue();
					}
				}
				field.setValue(value);
			}
			fields.add(field);
		}
		return fields;
	}
	
	private List<XFASubform> iterateSubForm(NodeList dom, int iterations) {
		List<XFASubform> subforms = new ArrayList<XFASubform>();
		
		for (int i = 0; i < dom.getLength(); i++) {
			String nodeName = dom.item(i).getNodeName();
			
			if (nodeName.equalsIgnoreCase("subform")) {
				XFASubform current = new XFASubform();
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
					XFASubform temp = this.clone(subforms.get(i));
					temp.setIndex(subforms.get(j).getIndex() + 1);
					subforms.set(i, temp);
				}
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
	
	private List<Map<String, Integer>> builPathMap(String path) {
		if (path.startsWith("/")) {
		    path = path.substring(1, path.length());
		}
		String[] split = path.split("/");
		
		List<Map<String, Integer>> sp = new ArrayList<Map<String, Integer>>();
			
		Pattern r = Pattern.compile("\\[(.*?)\\]");
		for (String s : split) {
			Map<String, Integer> current = new HashMap<String, Integer>();
			Matcher m = r.matcher(s);
			if (m.find()) {
				current.put(s.replaceAll("\\[(.*?)\\]", "").trim(), Integer.parseInt(m.group(1)));
			} else {
				current.put(s.trim(), 1);
			}
			sp.add(current);
		}
		
		return sp;
	}
	
	private void printTree(XFASubform sf, int row) {
		String sep = StringUtils.repeat("-", row);
		System.out.println(sep + " " + sf.getName());
		if (sf.getSubform().size() > 0) {
			for (XFASubform sf1 : sf.getSubform()) {
				this.printTree(sf1, row + 1);
			}
		}
	}
	
	private XFASubform clone(XFASubform sf) {
		return new XFASubform(sf);
	}

}
