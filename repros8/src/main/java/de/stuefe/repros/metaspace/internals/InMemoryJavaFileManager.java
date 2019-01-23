package de.stuefe.repros.metaspace.internals;

import javax.tools.*;
import javax.tools.JavaFileObject.Kind;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryJavaFileManager extends javax.tools.ForwardingJavaFileManager<JavaFileManager> {

    static JavaCompiler _compiler;
    static DiagnosticCollector<JavaFileObject> _diagnostics;

    public static InMemoryJavaFileManager theFileManager() {
        return _theFileManager;
    }

    private static InMemoryJavaFileManager _theFileManager;

    static {
        _compiler = ToolProvider.getSystemJavaCompiler();
        _diagnostics = new DiagnosticCollector<>();
        JavaFileManager parent = _compiler.getStandardFileManager(_diagnostics, null, null);
        _theFileManager = new InMemoryJavaFileManager(parent);
    }

    public boolean compileSingleFile(String classname, String body) {
        InMemorySourceFile jo = new InMemorySourceFile(classname, body);
        return compileSingleFile(jo);
    }

    public boolean compileSingleFile(JavaFileObject fileToCompile) {
        ArrayList<JavaFileObject> l = new ArrayList();
        l.add(fileToCompile);
        return compileMultipleFiles(l);
    }

    public boolean compileMultipleFiles(List<JavaFileObject> filesToCompile) {
        JavaCompiler.CompilationTask task = _compiler.getTask(null, this, _diagnostics, null, null, filesToCompile);
        if (!task.call())  {
            System.err.println("Compilation failed:");
            _diagnostics.getDiagnostics().forEach(n -> System.err.println(n));
            return false;
        }
        return true;
    }

    private final Map<URI, InMemorySourceFile> sources = new HashMap<URI, InMemorySourceFile>();
	private final Map<URI, InMemoryClassFile> classes = new HashMap<URI, InMemoryClassFile>();

	static URI makeURIforClass(String classname, Kind kind) {
		URI uri = null;
		try {
			uri = new URI("MEM:///" + classname.replace('.', '/') + kind.extension);
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return uri;
	}

	private InMemorySourceFile getSourceFile(String classname, boolean createIfMissing) {
		URI uri = makeURIforClass(classname, Kind.SOURCE);
		InMemorySourceFile o = sources.get(uri);
		if (o == null && createIfMissing) {
			o = new InMemorySourceFile(classname, "");
			sources.put(uri, o);
		}
		return o;
	}
	
	InMemoryClassFile getClassFile(String classname, boolean createIfMissing) {
		URI uri = makeURIforClass(classname, Kind.CLASS);
		InMemoryClassFile o = classes.get(uri);
		if (o == null && createIfMissing) {
			o = new InMemoryClassFile(classname);
			classes.put(uri, o);
		}
		return o;
	}
	
	public InMemoryJavaFileManager(JavaFileManager parent) {
		super(parent);
	}

	private JavaFileObject getJavaFileForInputOrOutput(Location location, String classname, Kind kind, boolean is_input) throws IOException {
        JavaFileObject o = null;
        switch (kind) {
            case SOURCE: o = getSourceFile(classname, is_input == false); break;
            case CLASS: o = getClassFile(classname, is_input == false); break;
            default:
                break;
        }
        return o;
    }

	@Override
	public JavaFileObject getJavaFileForInput(Location location, String classname, Kind kind) throws IOException {
		JavaFileObject o = getJavaFileForInputOrOutput(location, classname, kind, true);
		if (o == null) {
			o = super.getJavaFileForInput(location, classname, kind);
		}
		return o;

	}

	@Override
	public JavaFileObject getJavaFileForOutput(Location location, String classname, Kind kind, FileObject arg3)
			throws IOException {
        JavaFileObject o = getJavaFileForInputOrOutput(location, classname, kind, false);
        return o;
	}
	
}
