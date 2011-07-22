package com.grossbart.jslim;

import com.google.javascript.rhino.Node;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSSourceFile;

public class JSlim {

  /**
   * @param code JavaScript source code to compile.
   * @return The compiled version of the code.
   */
  public static String compile(String code) {
    Compiler compiler = new Compiler();

    CompilerOptions options = new CompilerOptions();
    // Advanced mode is used here, but additional options could be set, too.
    CompilationLevel.ADVANCED_OPTIMIZATIONS.setOptionsForCompilationLevel(
        options);

    // To get the complete set of externs, the logic in
    // CompilerRunner.getDefaultExterns() should be used here.
    JSSourceFile extern[] = {JSSourceFile.fromCode("externs.js", "function alert(x) {}")};

    // The dummy input name "input.js" is used here so that any warnings or
    // errors will cite line numbers in terms of input.js.
    JSSourceFile input[] = {JSSourceFile.fromCode("input.js", code)};

    compiler.init(extern, input, options);
    
    compiler.parse();
    
    Node node = compiler.getRoot();
    System.out.println("node.toString(): " + node.toString());
    System.out.println("node.getFirstChild(): " + node.getFirstChild());
    System.out.println("node.getFirstChild().getQualifiedName(): " + node.getFirstChild().getQualifiedName());

    // The compiler is responsible for generating the compiled code; it is not
    // accessible via the Result.
    return compiler.toSource();
  }

  public static void main(String[] args) {
    String compiledCode = compile(
        "function hello(name) {" +
          "alert('Hello, ' + name);" +
        "}" +
        "hello('New user');");
    System.out.println(compiledCode);
  }

}

