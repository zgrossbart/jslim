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

    private ArrayList<Node> m_vars = new ArrayList<Node>();
    private ArrayList<String> m_calls = new ArrayList<String>();
    private ArrayList<Node> m_funcs = new ArrayList<Node>();
    
    public String addLib(String code)
    {
        return slim(code, true);
    }
    
    /**
     * @param code JavaScript source code to compile.
     * @return The compiled version of the code.
     */
    public String slim(String code, boolean isLib) {
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
        System.out.println("node.toString(): \n" + node.toStringTree());
        
        //System.out.println("node before change: " + compiler.toSource());
        
        System.out.println("starting process...");
        Node n = process(node, isLib);
        
        System.out.println("Done processing...");
        System.out.println("m_calls: " + m_calls);
        
        if (isLib) {
            printStack();
        }
        //System.out.println("n: " + n.toStringTree());
        
        //System.out.println("n.toString(): \n" + n.toStringTree());
        
        // The compiler is responsible for generating the compiled code; it is not
        // accessible via the Result.
        return compiler.toSource();
    }
    
    private Node process(Node node, boolean isLib) {
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
            
            /*if (n.getType() == Token.BLOCK) {
                block(n);
            }*/
            
            if (n.getType() == Token.VAR && n.getFirstChild().getType() == Token.NAME) {
                m_vars.add(n);
            } else if (n.getType() == Token.CALL || n.getType() == Token.NEW) {
                addCalls(n);
            } else if (n.getType() == Token.ASSIGN ||
                       n.getType() == Token.ASSIGN_BITOR  ||
                       n.getType() == Token.ASSIGN_BITXOR ||
                       n.getType() == Token.ASSIGN_BITAND ||
                       n.getType() == Token.ASSIGN_LSH ||
                       n.getType() == Token.ASSIGN_RSH ||
                       n.getType() == Token.ASSIGN_URSH ||
                       n.getType() == Token.ASSIGN_ADD ||
                       n.getType() == Token.ASSIGN_SUB ||
                       n.getType() == Token.ASSIGN_MUL ||
                       n.getType() == Token.ASSIGN_DIV ||
                       n.getType() == Token.ASSIGN_MOD) {
                /*
                 This is an assignment operator.  
                 */
                addAssign(n);
            } else if (isLib && n.getType() == Token.FUNCTION) {
                /*
                 We need to check to make sure this is a named
                 function.  If it is an anonymous function then
                 it can't be called directly outside of scope and
                 it is being called locally so we can't remove it.
                 */
                if (n.getParent().getType() == Token.STRING ||
                    (n.getFirstChild().getType() == Token.NAME &&
                     n.getFirstChild().getString() != null &&
                     n.getFirstChild().getString().length() > 0)) {
                    m_funcs.add(n);
                }
            }
            
            process(n, isLib);
        }
        
        return node;
    }
    
    private void addAssign(Node assign)
    {
        if (assign.getChildCount() < 2) {
            /*
             This means it was a simple assignment to a constant value
             like var a = "foo" or var b = 5
             */
            return;
        }
        
        if (assign.getLastChild().getType() == Token.NAME) {
            /*
             This means it was assignment to a variable and since all
             variable names might be functions we need to add them to
             our calls list.
             */
            
            addCall(assign.getLastChild().getString());
        }
    }
    
    private void addCall(String call)
    {
        if (!m_calls.contains(call)) {
            m_calls.add(call);
        }
    }
    
    private void addCallsProp(Node getProp)
    {
        if (getProp.getLastChild().getType() == Token.STRING) {
            addCall(getProp.getLastChild().getString());
        }
        
        if (getProp.getFirstChild().getType() == Token.CALL) {
            /*
             Add the function name
             */
            addCall(getProp.getLastChild().getString());
            
            if (getProp.getFirstChild().getFirstChild().getType() == Token.NAME) {
                addCall(getProp.getFirstChild().getFirstChild().getString());
            }
        } else if (getProp.getFirstChild().getType() == Token.GETPROP) {
            addCallsProp(getProp.getFirstChild());
        }
        
        if (getProp.getNext() != null && getProp.getNext().getType() == Token.GETPROP) {
            addCallsProp(getProp.getNext());
        }
    }
    
    private void addCalls(Node call)
    {
        //assert call.getType() == Token.CALL || call.getType() == Token.NEW;
        
        if (call.getType() == Token.GETPROP) {
            addCallsProp(call);
        } else if (call.getFirstChild().getType() == Token.GETPROP) {
            addCallsProp(call.getFirstChild());
        } else if (call.getFirstChild().getType() == Token.NAME) {
            Node name = call.getFirstChild();
            addCall(name.getString());
            System.out.println("name.getString(): " + name.getString());
        }
    }
    
    private void printStack() {
        /*for (String call : m_calls) {
            System.out.println("Call: " + call);
        }*/
        
        for (Node n : m_funcs) {
            if (n.getParent() == null || n.getParent().getParent() == null) {
                /*
                 This means the function has already been removed
                 */
                continue;
            }
            
            if (n.getParent().getType() == Token.STRING) {
                /*
                 This is a closure style function like this:
                     myFunc: function()
                 */
                if (!m_calls.contains(n.getParent().getString())) {
                    System.out.println("Removing function: " + n.getParent().getString());
                    n.getParent().detachFromParent();
                }
            } else {
                /*
                 This is a standard type of function like this:
                    function myFunc()
                 */
                if (!m_calls.contains(n.getFirstChild().getString())) {
                    //System.out.println("n.toStringTree(): " + n.toStringTree());
                    System.out.println("Removing function: " + n.getFirstChild().getString());
                    n.detachFromParent();
                }
            }
        }
    }
    
    private Node block(Node block) {
        assert block.getType() == Token.BLOCK;
        
        //m_stack.push(new Struct());
        
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
            JSlim slim = new JSlim ();
            
            String mainJS = FileUtils.readFileToString(new File("main.js"), "UTF-8");
            slim.slim(mainJS, false);
            
            String libJS = FileUtils.readFileToString(new File("jquery.min.js"), "UTF-8");
            //String libJS = FileUtils.readFileToString(new File("lib.js"), "UTF-8");
            System.out.println("compiled code: " + slim.addLib(libJS));
            
            FileUtils.writeStringToFile(new File("out.js"), slim.addLib(libJS));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

