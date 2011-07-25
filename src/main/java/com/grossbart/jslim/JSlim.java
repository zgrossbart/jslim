package com.grossbart.jslim;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.commons.io.FileUtils;

import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.JSSourceFile;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;

public class JSlim {

    private LinkedList<Struct> m_stack = new LinkedList<Struct>();
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
        JSSourceFile extern[] = {JSSourceFile.fromCode("externs.js", "")};

        // The dummy input name "input.js" is used here so that any warnings or
        // errors will cite line numbers in terms of input.js.
        JSSourceFile input[] = {JSSourceFile.fromCode("input.js", code)};

        compiler.init(extern, input, options);

        compiler.parse();

        Node node = compiler.getRoot();
        //System.out.println("node.toString(): \n" + node.toStringTree());
        
        //System.out.println("node before change: " + compiler.toSource());
        
        Node n = process(node);
        
        printStack();
        //System.out.println("n: " + n.toStringTree());
        
        //System.out.println("n.toString(): \n" + n.toStringTree());
        
        // The compiler is responsible for generating the compiled code; it is not
        // accessible via the Result.
        return compiler.toSource();
    }
    
    private class Struct {
        private ArrayList<Node> m_vars = new ArrayList<Node>();
        private ArrayList<Node> m_calls = new ArrayList<Node>();
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
            
            if (n.getType() == Token.VAR && n.getFirstChild().getType() == Token.NAME) {
                m_stack.peek().m_vars.add(n);
            } else if (n.getType() == Token.CALL && n.getFirstChild().getType() == Token.GETPROP) {
                m_stack.peek().m_calls.add(n);
            } else if (n.getType() == Token.CALL && n.getFirstChild().getType() == Token.NAME) {
                m_stack.peek().m_calls.add(n);
            }
            
            process(n);
        }
        
        return node;
    }
    
    private void printStack() {
        while (m_stack.size() > 0) {
            Struct s = m_stack.pop();
            
            if (s.m_vars.size() > 0) {
                System.out.println("Variables:");
            }
            for (Node n : s.m_vars) {
                System.out.println("n: " + n.getFirstChild().getString());
            }
            
            if (s.m_calls.size() > 0) {
                System.out.println("\nCalls:");
            }
            for (Node n : s.m_calls) {
                if (n.getFirstChild().getType() == Token.GETPROP) {
                    Node name = n.getFirstChild().getFirstChild();
                    System.out.println(name.getString() + "." + name.getNext().getString() + "()");
                } else if (n.getFirstChild().getType() == Token.NAME) {
                    Node name = n.getFirstChild();
                    System.out.println(name.getString() + "()");
                }
            }
        }
    }
    
    private Node block(Node block) {
        assert block.getType() == Token.BLOCK;
        
        m_stack.push(new Struct());
        
        /*
        if (vars.size() > 0) {
            System.out.println("Variables:");
        }
        for (Node n : vars) {
            System.out.println("n: " + n.getFirstChild().getString());
        }
        
        if (calls.size() > 0) {
            System.out.println("\nCalls:");
        }
        for (Node n : calls) {
            System.out.println("n: " + n.getFirstChild().getString());
        }
        */
        
        return block;
    }

    public static void main(String[] args) {
        try {
            String mainJS = FileUtils.readFileToString(new File("main.js"), "UTF-8");
            new JSlim().slim(mainJS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

