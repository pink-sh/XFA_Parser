package org.pdfbox.extension.objects;

import java.util.List;

public class XFASubform {
	private String name;
	private Integer index;
	private List<XFASubform> subform;
	private List<XFAField> fields;
	
	public XFASubform() {}
	public XFASubform(XFASubform sf){
		this.setFields(sf.getFields());
		this.setIndex(sf.getIndex());
		this.setName(sf.getName());
		this.setSubform(sf.getSubform());
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Integer getIndex() {
		return index;
	}
	public void setIndex(Integer index) {
		this.index = index;
	}
	public List<XFASubform> getSubform() {
		return subform;
	}
	public void setSubform(List<XFASubform> subform) {
		this.subform = subform;
	}
	public List<XFAField> getFields() {
		return fields;
	}
	public void setFields(List<XFAField> fields) {
		this.fields = fields;
	}

}
