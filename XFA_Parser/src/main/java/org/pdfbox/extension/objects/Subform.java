package org.pdfbox.extension.objects;

import java.util.List;

public class Subform {
	private String name;
	private Integer index;
	private List<Subform> subform;
	private List<Field> fields;
	
	public Subform() {}
	public Subform(Subform sf){
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
	public List<Subform> getSubform() {
		return subform;
	}
	public void setSubform(List<Subform> subform) {
		this.subform = subform;
	}
	public List<Field> getFields() {
		return fields;
	}
	public void setFields(List<Field> fields) {
		this.fields = fields;
	}

}
