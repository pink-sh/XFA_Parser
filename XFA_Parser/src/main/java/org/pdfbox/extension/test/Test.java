package org.pdfbox.extension.test;

import java.io.FileNotFoundException;

import org.pdfbox.extension.XFA_Parser;

public class Test {
	private static String INPUT = "/Users/enrico/Desktop/eclipse/workspace/XFA_Parser/src/main/resources/files/CGRFA_dynamic_150312_filled.pdf";

	public static void main(String[] args) {
		
		try {
    		XFA_Parser parser = new XFA_Parser(INPUT);
    		parser.parse();
			System.out.println("Parsed!");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
