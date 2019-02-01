package de.stuefe.repros.metaspace.internals;
public class Utils {

	static public void createRandomClass(String classname, int sizeFactor) {
		String code = Utils.makeRandomSource(sizeFactor).replaceAll("CLASSNAME", classname);
		boolean success = InMemoryJavaFileManager.theFileManager().compileSingleFile(classname, code);
		assert(success);
	}

	static public String makeRandomSource(int size) {
	
		StringBuilder bld = new StringBuilder();
		bld.append("public class CLASSNAME {\n");
		for (int i = 0; i < size; i ++) {
			bld.append("public int i" + i + " = " + i * 3 + ";\n");
			bld.append("public String s" + i + " = \"hallo" + i + "\";\n");
			bld.append("public byte[] b" + i + " = new byte[] {");
					for (int j = 0; j < 10 + (j % 30); j ++) {
						bld.append("0x" + Integer.toHexString(j % 0x80) + ", ");
					}
					
			bld.append("0x17};\n");
		}
		for (int i = 0; i < size; i ++) {
			bld.append("public int get_i" + i + "() { return i" + i + "; }\n");
			bld.append("public String get_s" + i + "() { return s" + i + "; }\n");
			bld.append("public byte[] get_b" + i + "() { return b" + i + "; }\n");
		}
		bld.append("};");
		return bld.toString();
	}
	
}
