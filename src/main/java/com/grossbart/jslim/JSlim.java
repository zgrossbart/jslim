package com.grossbart.jslim;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.io.FileUtils;

import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSSourceFile;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;

public class JSlim {

    /**
     * @param code JavaScript source code to compile.
     * @return The compiled version of the code.
     */
    public String slim(String code) {
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
        //System.out.println("node.toString(): \n" + node.toStringTree());
        
        //System.out.println("node before change: " + compiler.toSource());
        
        Node n = process(node);
        //System.out.println("n: " + n.toStringTree());
        
        //System.out.println("n.toString(): \n" + n.toStringTree());
        
        // The compiler is responsible for generating the compiled code; it is not
        // accessible via the Result.
        return compiler.toSource();
    }
    
    private Node process(Node node) {
        Iterator<Node> nodes = node.children().iterator();
        
        while (nodes.hasNext()) {
            Node n = nodes.next();
            /*
            //System.out.println("n.getType(): " + n.getType());
            if (n.getType() == Token.CALL && n.getFirstChild().getType() == Token.NAME &&
                n.getFirstChild().getString().equals("alert")) {
                //System.out.println("n.toString(): " + n.toStringTree());
                
                //System.out.println("removing child...");
                n.getParent().detachFromParent();
                //System.out.println("Found the call: " + n.toStringTree());
                //n.getParent().removeChild(n);
                return n;
            }
            
            if (n.getType() == Token.CALL && n.getFirstChild().getType() == Token.GETPROP) {
                System.out.println("Found a function call to " + n.getFirstChild().getLastChild().getString() + 
                                   " on variable " + n.getParent().getFirstChild().getString());
            }
            */
            
            if (n.getType() == Token.BLOCK) {
                block(n);
            }
            
            process(n);
        }
        
        return node;
    }
    
    private Node block(Node block) {
        assert block.getType() == Token.BLOCK;
        
        ArrayList<Node> vars = new ArrayList<Node>();
        
        Iterator<Node> nodes = block.children().iterator();
        
        while (nodes.hasNext()) {
            Node n = nodes.next();
            if (n.getType() == Token.VAR && n.getFirstChild().getType() == Token.NAME) {
                vars.add(n);
            }
        }
        
        for (Node n : vars) {
            System.out.println("n: " + n.getFirstChild().getString());
        }
        
        return block;
    }

    public static void main(String[] args) {
        try {
            String mainJS = FileUtils.readFileToString(new File("main.js"), "UTF-8");
            System.out.println(new JSlim().slim(mainJS));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

