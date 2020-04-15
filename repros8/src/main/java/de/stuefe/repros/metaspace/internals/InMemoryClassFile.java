package de.stuefe.repros.metaspace.internals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.tools.SimpleJavaFileObject;

public class InMemoryClassFile extends SimpleJavaFileObject {
	private String name;
	private ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
	
	InMemoryClassFile(String className) {
		super(InMemoryJavaFileManager.makeURIforClass(className, Kind.CLASS), Kind.CLASS);
		name = className;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public OutputStream openOutputStream() throws IOException {
		return byteArrayOutputStream;
	}
	
	byte[] getBytes() {
		return byteArrayOutputStream.toByteArray();
	}
	
}
