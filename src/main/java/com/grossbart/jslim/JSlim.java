package com.grossbart.jslim;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
    private ArrayList<Node> m_libFuncs = new ArrayList<Node>();
    private ArrayList<Node> m_allFuncs = new ArrayList<Node>();
    private ArrayList<Node> m_keepers = new ArrayList<Node>();
    
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
        CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);

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
            prune();
        }
        
        System.out.println("Removed " + (m_libFuncs.size() - m_keepers.size()) + " out of " + m_libFuncs.size() + " functions.");
        
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
                     n.getFirstChild().getString().length() > 0) ||
                    n.getParent().getType() == Token.ASSIGN) {
                    
                    /*
                     If this function is part of an object list that means
                     it is named and getting passed to a function and most
                     likely getting called without a direct function reference
                     so we have to leave it there.
                     */
                    if (!(n.getParent().getParent().getType() == Token.OBJECTLIT &&
                          n.getParent().getParent().getParent().getType() == Token.CALL) &&
                        getFunctionName(n) != null) {
                        if (isLib) {
                            m_libFuncs.add(n);
                        } else {
                            m_funcs.add(n);
                        }
                    }
                }
            }
            
            process(n, isLib);
        }
        
        return node;
    }
    
    private void addAssign(Node assign)
    {
        addAssign(assign, m_calls);
    }
    
    private void addAssign(Node assign, List<String> calls)
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
            
            addCall(assign.getLastChild().getString(), calls);
        }
    }
    
    private void addCall(String call, List<String> calls)
    {
        if (!calls.contains(call)) {
            calls.add(call);
        }
    }
    
    private void addCallsProp(Node getProp, List<String> calls)
    {
        if (getProp.getLastChild().getType() == Token.STRING) {
            addCall(getProp.getLastChild().getString(), calls);
        }
        
        if (getProp.getFirstChild().getType() == Token.CALL) {
            /*
             Add the function name
             */
            addCall(getProp.getLastChild().getString(), calls);
            
            if (getProp.getFirstChild().getFirstChild().getType() == Token.NAME) {
                addCall(getProp.getFirstChild().getFirstChild().getString(), calls);
            }
        } else if (getProp.getFirstChild().getType() == Token.GETPROP) {
            addCallsProp(getProp.getFirstChild(), calls);
        }
        
        if (getProp.getNext() != null && getProp.getNext().getType() == Token.GETPROP) {
            addCallsProp(getProp.getNext(), calls);
        }
    }
    
    private void addCalls(Node call)
    {
        addCalls(call, m_calls);
    }
    
    private void addCalls(Node call, List<String> calls)
    {
        //assert call.getType() == Token.CALL || call.getType() == Token.NEW;
        
        if (call.getType() == Token.GETPROP) {
            addCallsProp(call, calls);
        } else if (call.getFirstChild().getType() == Token.GETPROP) {
            addCallsProp(call.getFirstChild(), calls);
        } else if (call.getFirstChild().getType() == Token.NAME) {
            Node name = call.getFirstChild();
            addCall(name.getString(), calls);
            System.out.println("name.getString(): " + name.getString());
        }
    }
    
    private void prune() {
        m_allFuncs.addAll(m_funcs);
        m_allFuncs.addAll(m_libFuncs);
        
        for (String call : m_calls) {
            findKeepers(call);
        }
        
        System.out.println("m_keepers: " + m_keepers);
        
        for (Node func : m_libFuncs) {
            if (!m_keepers.contains(func)) {
                removeFunction(func);
            }
        }
    }
    
    private void removeFunction(Node n)
    {
        System.out.println("removeFunction(" + getFunctionName(n) + ")");
        
        if (n.getParent() == null || n.getParent().getParent() == null) {
            /*
             This means the function has already been removed
             */
            return;
        }
        
        if (n.getParent().getType() == Token.STRING) {
            /*
             This is a closure style function like this:
                 myFunc: function()
             */
            //System.out.println("Removing function: " + n.getParent().getString());
            n.getParent().detachFromParent();
        } else if (n.getParent().getType() == Token.ASSIGN) {
            /*
             This is a property assignment function like:
                myObj.func1 = function()
             */
            Node expr = findExprOrVar(n);
            if (expr != null && expr.getType() == Token.EXPR_RESULT && expr.getParent() != null) {
                System.out.println("expr: " + expr);
                expr.detachFromParent();
            }
        } else {
            /*
             This is a standard type of function like this:
                function myFunc()
             */
            //System.out.println("n.toStringTree(): " + n.toStringTree());
            //System.out.println("Removing function: " + n.getFirstChild().getString());
            n.detachFromParent();
        }
    }
    
    private Node findExprOrVar(Node n)
    {
        if (n == null) {
            return null;
        } else if (n.getType() == Token.EXPR_RESULT ||
                   n.getType() == Token.VAR) {
            return n;
        } else {
            return findExprOrVar(n.getParent());
        }
    }
    
    private ArrayList<String> m_examinedCalls = new ArrayList<String>();
    
    private void findKeepers(String call)
    {
        if (m_examinedCalls.contains(call)) {
            /*
             Then we've already examined this call and we can skip it.
             */
            return;
        }
        
        System.out.println("findKeepers(" + call + ")");
        
        m_examinedCalls.add(call);
        
        
        Node funcs[] = findMatchingFunctions(call);
            
        for (Node func : funcs) {
            m_keepers.add(func);
            
            for (String c : findCalls(func)) {
                findKeepers(c);
            }
        }
        
        /*if (call.equals("isDate")) {
            throw new RuntimeException("stop here...");
        }*/
    }
    
    /**
     * Find all of the calls in the given function.
     * 
     * @param func   the function to look in
     * 
     * @return the list of calls
     */
    private String[] findCalls(Node func)
    {
        ArrayList<String> calls = new ArrayList<String>();
        findCalls(func, calls);
        return calls.toArray(new String[calls.size()]);
    }
    
    private void findCalls(Node node, List<String> calls)
    {
        Iterator<Node> nodes = node.children().iterator();
        
        while (nodes.hasNext()) {
            Node n = nodes.next();
            if (n.getType() == Token.CALL || n.getType() == Token.NEW) {
                addCalls(n, calls);
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
                addAssign(n, calls);
            } 
            
            findCalls(n, calls);
        }
    }
    
    private Node[] findFunctions(Node parent)
    {
        ArrayList<Node> funcs = new ArrayList<Node>();
        findFunctions(parent, funcs);
        
        return funcs.toArray(new Node[funcs.size()]);
        
    }
    
    private Node findFunctions(Node node, List<Node> funcs)
    {
        Iterator<Node> nodes = node.children().iterator();
        
        while (nodes.hasNext()) {
            Node n = nodes.next();
            if (n.getType() == Token.FUNCTION) {
                /*
                 We need to check to make sure this is a named
                 function.  If it is an anonymous function then
                 it can't be called directly outside of scope and
                 it is being called locally so we can't remove it.
                 */
                if (n.getParent().getType() == Token.STRING ||
                    (n.getFirstChild().getType() == Token.NAME &&
                     n.getFirstChild().getString() != null &&
                     n.getFirstChild().getString().length() > 0) ||
                    n.getParent().getType() == Token.ASSIGN) {
                    
                    /*
                     If this function is part of an object list that means
                     it is named and getting passed to a function and most
                     likely getting called without a direct function reference
                     so we have to leave it there.
                     */
                    if (!(n.getParent().getParent().getType() == Token.OBJECTLIT &&
                          n.getParent().getParent().getParent().getType() == Token.CALL)) {
                        funcs.add(n);
                    }
                }
            }
            
            findFunctions(n, funcs);
        }
        
        return node;
    }
    
    private List<String> getFunctionNames(Node n)
    {
        /*
         EXPR_RESULT 561 [source_file: input.js]
            ASSIGN 561 [source_file: input.js]
                GETPROP 561 [source_file: input.js]
                    NAME _ 561 [source_file: input.js]
                    STRING functions 561 [source_file: input.js]
                ASSIGN 561 [source_file: input.js]
                    GETPROP 561 [source_file: input.js]
                        NAME _ 561 [source_file: input.js]
                        STRING methods 561 [source_file: input.js]
                    FUNCTION  561 [source_file: input.js]
         */
        ArrayList<String> names = new ArrayList<String>();
        if (n.getType() == Token.FUNCTION) {
            names.add(getFunctionName(n));
        }
        
        if (n.getType() == Token.ASSIGN) {
            names.add(n.getFirstChild().getLastChild().getString());
        }
        
        if (n.getParent().getType() == Token.ASSIGN) {
            names.addAll(getFunctionNames(n.getParent()));
        }
        
        return names;
    }
    
    private String getFunctionName(Node n)
    {
        try {
            if (n.getParent().getType() == Token.ASSIGN) {
                if (n.getParent().getFirstChild().getChildCount() == 0) {
                    /*
                     This is a variable assignment of a function to a
                     variable in the globabl scope.  These functions are
                     just too big in scope so we ignore them.  Example:
                        myVar = function()
                     */
                    return null;
                } else if (n.getParent().getFirstChild().getType() == Token.GETELEM) {
                    /*
                     This is a property assignment function with an array
                     index like this: 
                        jQuery.fn[ "inner" + name ] = function()
     
                     These functions are tricky to remove since we can't
                     depend on just the name when removing them.  We're
                     just leaving them for now.
                     */
                    return null;
                } else {
                    /*
                     This is a property assignment function like:
                        myObj.func1 = function()
                     */
                    return n.getParent().getFirstChild().getLastChild().getString();
                }
            }
            
            if (n.getParent().getType() == Token.STRING) {
                /*
                 This is a closure style function like this:
                     myFunc: function()
                 */
                return n.getParent().getString();
            } else {
                /*
                 This is a standard type of function like this:
                    function myFunc()
                 */
                return n.getFirstChild().getString();
            }
        } catch (Exception e) {
            System.out.println("npe: " + n.toStringTree());
            e.printStackTrace();
            throw new RuntimeException("stop here...");
        }
    }
    
    /**
     * Find all of the functions with the specified name.
     * 
     * @param name   the name of the function to find
     * 
     * @return the functions with this matching name
     */
    private Node[] findMatchingFunctions(String name)
    {
        ArrayList<Node> matches = new ArrayList<Node>();
        
        for (Node n : m_allFuncs) {
            if (getFunctionNames(n).contains(name)) {
                matches.add(n);
            }
        }
        
        return matches.toArray(new Node[matches.size()]);
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
    
    public static String plainCompile(String code) {
        Compiler compiler = new Compiler();
        
        CompilerOptions options = new CompilerOptions();
        // Advanced mode is used here, but additional options could be set, too.
        CompilationLevel.ADVANCED_OPTIMIZATIONS.setOptionsForCompilationLevel(options);
        
        // To get the complete set of externs, the logic in
        // CompilerRunner.getDefaultExterns() should be used here.
        JSSourceFile extern = JSSourceFile.fromCode("externs.js", "function alert(x) {}");
        
        // The dummy input name "input.js" is used here so that any warnings or
        // errors will cite line numbers in terms of input.js.
        JSSourceFile input = JSSourceFile.fromCode("input.js", code);
    
        // compile() returns a Result, but it is not needed here.
        compiler.compile(extern, input, options);
    
        // The compiler is responsible for generating the compiled code; it is not
        // accessible via the Result.
        return compiler.toSource();
    }

    public static void main(String[] args) {
        try {
            JSlim slim = new JSlim ();
            
            String mainJS = FileUtils.readFileToString(new File("main.js"), "UTF-8");
            slim.slim(mainJS, false);
            
            //String libJS = FileUtils.readFileToString(new File("libs/jquery-ui-1.8.14.custom.min.js"), "UTF-8");
            //String libJS = FileUtils.readFileToString(new File("libs/jquery.min.js"), "UTF-8");
            //String libJS = FileUtils.readFileToString(new File("lib.js"), "UTF-8");
            String libJS = FileUtils.readFileToString(new File("libs/jquery-1.6.2.js"), "UTF-8");
            //System.out.println("compiled code: " + slim.addLib(libJS));
            //String libJS = FileUtils.readFileToString(new File("libs/underscore.js"), "UTF-8");
            
            FileUtils.writeStringToFile(new File("out.js"), slim.addLib(libJS));
            //FileUtils.writeStringToFile(new File("out.js"), plainCompile(libJS));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

