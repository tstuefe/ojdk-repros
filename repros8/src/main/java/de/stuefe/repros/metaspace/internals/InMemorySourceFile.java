package de.stuefe.repros.metaspace.internals;

import java.io.IOException;

import static de.stuefe.repros.metaspace.internals.InMemoryJavaFileManager.makeURIforClass;

public class InMemorySourceFile extends javax.tools.SimpleJavaFileObject {
	final String code;
	public InMemorySourceFile(String className, String classCode) {
		super(makeURIforClass(className, Kind.SOURCE), Kind.SOURCE);
		code = classCode;
	}
	@Override
	public CharSequence getCharContent(boolean arg0) throws IOException {
		return code;
	}
}
