package org.pdfbox.extension.test;

import java.io.FileNotFoundException;

import org.pdfbox.extension.XFA_Parser;
import org.pdfbox.extension.objects.Field;

public class Test {
	private static String INPUT = "/Users/enrico/Work/git/XFA_Parser/XFA_Parser/src/main/resources/files/CGRFA_dynamic_150312_filled.pdf";

	public static void main(String[] args) {
		
		try {
    		XFA_Parser parser = new XFA_Parser(INPUT);
    		parser.parse();
			System.out.println("Parsed!");
			
			/*List<Field> fields = parser.getFieldById("CheckBox");
			for (Field f : fields) {
				System.out.println(f.getName());
			}
			System.out.println("----------------");
			List<Subform> subforms = parser.getSubFormById("Page48");
			for (Subform sf : subforms) {
				System.out.println(sf.getName());
			}*/
			Field field = parser.getFieldByXPath("Page12/q11Table/Item[2]/q11_02");
			System.out.println(field.getName());
			// TopmostSubform/Page21/q19Table/Item[2]/
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
