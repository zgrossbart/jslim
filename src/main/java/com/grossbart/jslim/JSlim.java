/******************************************************************************* 
 * 
 * Copyright 2011 Zack Grossbart 
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/
package com.grossbart.jslim;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;

import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.ErrorManager;
import com.google.javascript.jscomp.JSSourceFile;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;

/**
 * JSlim is a static code analysis tool for JavaScript.  It looks at a JavaScript program
 * and a set of library files it uses and creates a new library with only the functions
 * which are actually used.
 */
public class JSlim 
{
    /** 
     * Set of options that can be used with the --formatting flag. 
     */
    public enum FormattingOption
    {
        /** 
         * The options for pretty printing 
         */
        PRETTY_PRINT, 

        /** 
         * The options for printing input delimeters
         */
        PRINT_INPUT_DELIMITER;
        
        private void applyToOptions(CompilerOptions options)
        {
            switch (this) {
            case PRETTY_PRINT:
                options.prettyPrint = true;
                break;

            case PRINT_INPUT_DELIMITER:
                options.printInputDelimiter = true;
                break;
            default:
                throw new RuntimeException("Unknown formatting option: " + this);
            }
        }
    }
    
    private static final Logger LOGGER = Logger.getLogger(JSlim.class.getName());
    
    static {
        LOGGER.setUseParentHandlers(false);
        LOGGER.addHandler(new SlimConsoleHandler());
    }
    
    /**
     * Set the logging level for this process.  Most messages only show up with INFO logging.
     * 
     * @param level  the log level
     */
    public static void setLoggingLevel(Level level)
    {
        LOGGER.setLevel(level);
    }
    
    /**
     * Get the logger for this compile process.
     * 
     * @return the logger
     */
    public static Logger getLogger()
    {
        return LOGGER;
    }
    
    private List<Node> m_vars = new ArrayList<Node>();
    private List<Call> m_calls = new ArrayList<Call>();
    private List<Call> m_examinedCalls = new ArrayList<Call>();
    
    private List<Node> m_funcs = new ArrayList<Node>();
    private List<Node> m_libFuncs = new ArrayList<Node>();
    private List<Node> m_allFuncs = new ArrayList<Node>();
    private List<Node> m_keepers = new ArrayList<Node>();
    
    private List<JSFile> m_files = new ArrayList<JSFile>();
    
    private ErrorManager m_errMgr;
    private int m_funcCount;
    
    private String m_charset = "UTF-8";
    private boolean m_printTree = false;
    private FormattingOption m_formattingOptions;
    
    /**
     * Set the formatting options for this compiler.
     * 
     * @param options the formatting options
     */
    public void setFormattingOptions(FormattingOption options)
    {
        m_formattingOptions = options;
    }
    
    /**
     * `Add the library contents to the compiler and prune them.
     * 
     * @param name   the file name of the added library
     * @param code   the code contents 
     * @param compLevel the compilation level 
     * 
     * @return the pruned file
     */
    protected String addLib(String name, String code, CompilationLevel compLevel)
    {
        return slim(name, code, true, compLevel);
    }
    
    /**
     * Add a source file for compilation.
     * 
     * @param file   the file to compile
     */
    public void addSourceFile(JSFile file)
    {
        m_files.add(file);
    }
    
    /**
     * Prune all of the files which have been added to this compiler instance. 
     *  
     * @param compLevel the compilation level 
     * 
     * @return the pruned result of this precompile
     */
    public String prune(CompilationLevel compLevel)
    {
        StringBuffer sb = new StringBuffer();
        
        for (JSFile file : m_files) {
            if (file.isLib()) {
                sb.append(file.getContent() + "\n");
            } else {
                slim(file.getName(), file.getContent(), false, compLevel);
            }
        }
        
        return addLib("combined_lib.js", sb.toString(), compLevel);
    }
    
    /**
     * Validate the specified JavaScript file
     * 
     * @param name    the name of the file
     * @param content the file contents
     * @param formattingOptions the formtting options for this compile
     * 
     * @return the error manager containing any errors from the specified file
     */
    public static ErrorManager validate(String name, String content, FormattingOption formattingOptions)
    {
        Compiler compiler = new Compiler();

        CompilerOptions options = new CompilerOptions();
        // Advanced mode is used here, but additional options could be set, too.
        CompilationLevel.SIMPLE_OPTIMIZATIONS.setOptionsForCompilationLevel(options);

        // To get the complete set of externs, the logic in
        // CompilerRunner.getDefaultExterns() should be used here.
        JSSourceFile extern[] = {JSSourceFile.fromCode("externs.js", "")};

        // The dummy input name "input.js" is used here so that any warnings or
        // errors will cite line numbers in terms of input.js.
        JSSourceFile input[] = {JSSourceFile.fromCode(name, content)};
        
        if (formattingOptions != null) {
            formattingOptions.applyToOptions(options);
        }

        compiler.init(extern, input, options);

        compiler.parse();
        return compiler.getErrorManager();
    }
    
    /**
     * Parse, compile, and slim the specified code
     * 
     * @param name      the name of the file to slim
     * @param code      JavaScript source code to compile.
     * @param isLib     true if this is a library file and false otherwise
     * @param compLevel the compilation level
     * 
     * @return The compiled version of the code.
     */
    private String slim(String name, String code, boolean isLib, CompilationLevel compLevel)
    {
        Compiler compiler = new Compiler();

        CompilerOptions options = new CompilerOptions();
        if (compLevel != null) {
            // Advanced mode is used here, but additional options could be set, too.
            compLevel.setOptionsForCompilationLevel(options);
        }

        // To get the complete set of externs, the logic in
        // CompilerRunner.getDefaultExterns() should be used here.
        JSSourceFile extern[] = {JSSourceFile.fromCode("externs.js", "")};

        // The dummy input name "input.js" is used here so that any warnings or
        // errors will cite line numbers in terms of input.js.
        JSSourceFile input[] = {JSSourceFile.fromCode(name, code)};
        
        if (m_formattingOptions != null) {
            m_formattingOptions.applyToOptions(options);
        }

        compiler.init(extern, input, options);

        compiler.parse();
        m_errMgr = compiler.getErrorManager();
        
        if (m_errMgr.getErrorCount() > 0) {
            /*
             Then there were errors parsing the file and we can't
             prune anything. 
             */
            return "";
        }

        Node node = compiler.getRoot();
        if (m_printTree) {
            System.out.println("Tree before pruning:");
            System.out.println(node.toStringTree());
        }
        
        //System.out.println("node before change: " + compiler.toSource());
        
        LOGGER.log(Level.INFO, "starting process...");
        Node n = process(node, isLib);
        
        LOGGER.log(Level.INFO, "Done processing...");
        LOGGER.log(Level.FINE, "m_calls: " + m_calls);
        
        m_funcCount = m_libFuncs.size();
        
        if (isLib) {
            LOGGER.log(Level.INFO, "Starting pruneTree phase 1.");
            pruneTree();
            
            LOGGER.log(Level.INFO, "Starting pruneTree phase 2.");
            pruneTree();
        }
        
        if (m_funcCount > 0) {
            System.out.println("Removed " + (m_funcCount - m_keepers.size()) + " out of " + m_funcCount + " named functions.");
        }
        
        if (m_printTree) {
            System.out.println("Tree after pruning:");
            System.out.println(node.toStringTree());
        }
        
        // The compiler is responsible for generating the compiled code; it is not
        // accessible via the Result.
        return compiler.toSource();
    }
    
    /**
     * Process this particular node looking for calls, interesting functions, and 
     * variables.
     * 
     * @param node   the node to process
     * @param isLib  true if this node is from a library file and false otherwise
     * 
     * @return the original node reference
     */
    private Node process(Node node, boolean isLib)
    {
        Iterator<Node> nodes = node.children().iterator();
        
        while (nodes.hasNext()) {
            Node n = nodes.next();
            
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
            } else if (isLib && n.getType() == Token.FUNCTION &&
                       isInterestingFunction(n)) {
                if (isLib) {
                    m_libFuncs.add(n);
                } else {
                    m_funcs.add(n);
                }
                
                Node parent = n.getParent();
                while (parent != null && parent.getType() == Token.ASSIGN) {
                    if (parent.getFirstChild().getNext().getType() != Token.FUNCTION) {
                        if (isLib) {
                            m_libFuncs.add(parent);
                        } else {
                            m_funcs.add(parent);
                        }
                    }
                    
                    parent = parent.getParent();
                }
            }
            
            process(n, isLib);
        }
        
        return node;
    }
    
    private Node findInterestingFunctionParent(Node n) 
    {
        if (n == null) {
            return null;
        } else if (isInterestingFunction(n)) {
            return n;
        } else {
            return findInterestingFunctionParent(n.getParent());
        }
    }
    
    /**
     * This method determines if the specified function is interesting.  In our case interesting
     * means it is a potentatial candidate for removal.  There are many reasons the function
     * might not be a good cadidate.  For example, anonymous functions are never removed since
     * they are almost always used and there is no way to track if they are used or not.
     * 
     * @param n      the function to check
     * 
     * @return true if the function is interesting and false otherwise
     */
    private boolean isInterestingFunction(Node n)
    {
        if (n.getType() != Token.FUNCTION) {
            /*
             If this node isn't a function then it definitely isn't an
             interesting function
             */
            return false;
        }
        
        if (n.getParent().getType() == Token.ASSIGN &&
            n.getParent().getParent().getType() == Token.RETURN) {
            /*
             Then this is a function getting returned from another
             function and that makes it really difficult to determine
             if the function is being called because it is never
             called directly by name
             */
            return false;
        }
        
        /*
         We need to check to make sure this is a named
         function.  If it is an anonymous function then
         it can't be called directly outside of scope and
         it is probably being called locally so we can't remove it.
         */
        if (n.getParent().getType() == Token.STRING ||
            (n.getFirstChild().getType() == Token.NAME &&
             n.getFirstChild().getString() != null &&
             n.getFirstChild().getString().length() > 0) ||
            n.getParent().getType() == Token.ASSIGN) {
            
            /*
             If the function doesn't have a name we can identify then it is anonymous and
             we can't tell if anyone is calling it.
             */
            if (getFunctionName(n) != null) {
                /*
                 If this function has a direct parent which is another function instead of
                 a block or a property then it is probably being created to get returned from
                 the functions and therefore only has a name in he scope of that function.
                 It might be possible to change the mapping to the parent function, but we
                 can't understand that right now and there might me multiple functions within
                 this one specific function.
                 */
                if (!(n.getParent().getType() == Token.BLOCK && n.getParent().getParent().getType() == Token.FUNCTION)) {
                    return true;
                }
            }
        }
        
        return false;
        
    }
    
    /**
     * Add an assignment call to our list of calls.
     * 
     * @param assign the assignment node to add
     */
    private void addAssign(Node assign)
    {
        addAssign(assign, m_calls);
    }
    
    /**
     * Add an assignment call to the specified list of calls or increment the count if
     * that assignment is already there..
     * 
     * @param assign the assignment node to add
     * @param calls  the list of calls to add this assignment to
     */
    private void addAssign(Node assign, List<Call> calls)
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
            
            addCall(assign.getLastChild().getString(), assign, calls);
        } else if (assign.getFirstChild().getType() == Token.GETELEM &&
                   assign.getLastChild().getLastChild() != null &&
                   assign.getLastChild().getLastChild().getType() == Token.STRING) {
            /*
             This means it is an assignment to an array element like:
                 res[toString] = R._path2string;
             */
            addCall(assign.getLastChild().getLastChild().getString(), assign, calls);
        }
    }
    
    /**
     * Add a call to the specified list of calls or increment the call count if the call
     * is already in the list.
     * 
     * @param call     the call to add
     * @param callNode the Node representing this call
     * @param calls    the list to add it to
     */
    private void addCall(String call, Node callNode, List<Call> calls)
    {
        if (callMatchesParentfunction(call, callNode)) {
            /*
             If this is a call to a function with the same name then it
             is probably recursion and we shouldn't count it.  This is a
             little dangerous because you could be in function f and call
             f on a separate object, but it is an unlikely case and that
             will require an external reference.
             */
            return;
        }
        
        Call c = getCall(call, calls);
        
        if (c == null) {
            c = new Call(call);
            calls.add(c);
        } else {
            /*
             If the call is already there then we just increment
             the count
             */
            c.incCount();
        }
    }
    
    /**
     * Get the call object for the call with the specified name.
     * 
     * @param name   the call name to look for
     * @param calls  the list of calls to look in
     * 
     * @return the call if it was in the list of null if it wasn't
     */
    private static Call getCall(String name, List<Call> calls)
    {
        for (Call call : calls) {
            if (call.getName().equals(name)) {
                return call;
            }
        }
        
        return null;
    }
    
    /**
     * Add a call with the specified get property node.
     * 
     * @param getProp the node to add
     * @param calls   the list of calls to add it to
     */
    private void addCallsProp(Node getProp, List<Call> calls)
    {
        if (getProp.getLastChild().getType() == Token.STRING) {
            addCall(getProp.getLastChild().getString(), getProp, calls);
        }
        
        if (getProp.getFirstChild().getType() == Token.CALL) {
            /*
             Add the function name
             */
            addCall(getProp.getLastChild().getString(), getProp, calls);
            
            if (getProp.getFirstChild().getFirstChild().getType() == Token.NAME) {
                addCall(getProp.getFirstChild().getFirstChild().getString(), getProp, calls);
            }
        } else if (getProp.getFirstChild().getType() == Token.GETPROP) {
            addCallsProp(getProp.getFirstChild(), calls);
        }
        
        if (getProp.getNext() != null && getProp.getNext().getType() == Token.GETPROP) {
            addCallsProp(getProp.getNext(), calls);
        }
    }
    
    /**
     * Add all calls underneath the specified node.
     * 
     * @param call   the call to look in
     */
    private void addCalls(Node call)
    {
        addCalls(call, m_calls);
    }
    
    private boolean callMatchesParentfunction(String call, Node callNode)
    {
        Node f = findInterestingFunctionParent(callNode);
        
        if (f != null) {
            if (getFunctionName(f).equals(call)) {
                /*
                 Then the call name matches the direct parent name
                 */
                return true;
            } else if (f.getParent() != null && f.getParent().getType() == Token.ASSIGN) {
                
                /*
                 This this function has an assignment chain like:
                 _.reduceRight = _.foldr = function...
                 So we have to walk up the chain
                 */
                Node parent = f.getParent();
                while (parent != null && parent.getType() == Token.ASSIGN) {
                    if (parent.getFirstChild().getType() == Token.GETPROP && 
                        call.equals(parent.getFirstChild().getFirstChild().getNext().getString())) {
                        return true;
                    }
                    
                    parent = parent.getParent();
                }
            }
        } 
        
        return false;
    }
    
    /**
     * Add all calls underneath the specified node.
     * 
     * @param call   the call to look in
     * @param calls  the list to add the call to
     */
    private void addCalls(Node call, List<Call> calls)
    {
        if (call.getType() == Token.GETPROP) {
            addCallsProp(call, calls);
        } else if (call.getFirstChild().getType() == Token.GETPROP) {
            addCallsProp(call.getFirstChild(), calls);
        } else if (call.getFirstChild().getType() == Token.NAME) {
            Node name = call.getFirstChild();
            addCall(name.getString(), name, calls);
            LOGGER.log(Level.FINE, "name.getString(): " + name.getString());
        } else if (call.getFirstChild().getType() == Token.GETELEM) {
            /*
             This is a call using the array index to get the function
             property like this:
     
             obj['hello']();
             */
            String c = getConcatenatedStringIndex(call.getFirstChild());
            if (c != null) {
                addCall(c, call, calls);
            }
        }
    }
    
    private String getConcatenatedStringIndex(Node getElem)
    {
        if (getElem.getFirstChild().getNext().getType() == Token.STRING) {
            /*
             Then this is a simple string reference like obj['hello']
             and we can just return the string
             */
            return getElem.getFirstChild().getNext().getString();
        } else if (getElem.getFirstChild().getNext().getType() == Token.ADD) {
            /*
             Then this is a concatenated string like obj['h' + 'el' + 'lo']
             */
            
            StringBuffer sb = new StringBuffer();
            Node current = getElem.getFirstChild().getNext();
            while (current != null) {
                if (current.getFirstChild().getType() == Token.ADD) {
                    String s = getString(current.getFirstChild().getNext());
                    if (s != null) {
                        sb.insert(0, s);
                    } else {
                        return null;
                    }
                    current = current.getFirstChild();
                } else if (current.getFirstChild().getType() == Token.STRING) {
                    String s = getString(current.getFirstChild().getNext());
                    if (s != null) {
                        sb.insert(0, s);
                    } else {
                        return null;
                    }
                    
                    s = getString(current.getFirstChild());
                    if (s != null) {
                        sb.insert(0, s);
                    } else {
                        return null;
                    }
                    current = null;
                } else {
                    current = null;
                }
            }
            
            return sb.toString();
        } else {
            /*
             Then this was some more complex type of string like
             obj[('h' + 'el' + 'lo').substring(2)].  We can't evaluate
             that string with just static evaluation so the user
             will have to declare an external for that.
             */
            return null;
        }
    }
    
    private String getString(Node n)
    {
        if (n.getType() == Token.STRING) {
            return n.getString();
        } else if (n.getType() == Token.NUMBER) {
            double num = n.getDouble();
            int inum = (int) num;
            if (inum == num) {
                return "" + inum;
            } else {
                return "" + num;
            }
        } else {
            return null;
        }
    }
    
    /**
     * Use all the collected information to prune the tree and remove unused functions.
     */
    private void pruneTree()
    {
        m_allFuncs.addAll(m_funcs);
        m_allFuncs.addAll(m_libFuncs);
        
        for (Call call : m_calls) {
            findKeepers(call);
        }
        
        LOGGER.log(Level.FINE, "m_keepers: " + m_keepers);
        
        for (int i = m_libFuncs.size() - 1; i > -1; i--) {
            Node func = m_libFuncs.get(i);
            
            if (!m_keepers.contains(func)) {
                removeCalledKeepers(func);
                removeFunction(func);
                m_libFuncs.remove(func);
            }
        }
        
        LOGGER.log(Level.INFO, "Keeping the following functions:");
        for (Node f : m_libFuncs) {
            LOGGER.log(Level.INFO, "func: " + getFunctionName(f));
        }
    }
    
    /**
     * If we're removing a function then all of the calls within that function to other
     * functions (and so on recursively) can be removed from our call count.  This method
     * finds all of them and does just that.
     * 
     * @param func   the function which will be removed
     */
    private void removeCalledKeepers(Node func)
    {
        Call calls[] = findCalls(func);
        for (Call call : calls) {
            Call orig = getCall(call.getName(), m_calls);
            orig.decCount(call.getCount());
            
            if (orig.getCount() < 1) {
                Node f = findFunction(orig.getName());
                if (f != null) {
                    m_keepers.remove(f);
                }
            }
        }
    }
    
    /**
     * Find the function in our list of known functions with the specified name.
     * 
     * @param name   the name of the function to find
     * 
     * @return the function with the specified name or null if that function isn't in our list
     */
    private Node findFunction(String name)
    {
        for (Node f : m_libFuncs) {
            if (getFunctionName(f).equals(name)) {
                return f;
            }
        }
        
        return null;
    }
    
    /**
     * Remove all functions with the specified name from the tree.
     * 
     * @param func   the function name to remove
     */
    private void removeFunction(String func)
    {
        for (Node f : m_libFuncs) {
            if (getFunctionName(f).equals(func)) {
                removeFunction(f);
            }
        }
    }
    
    /**
     * Remove the function at the specified node.
     * 
     * @param n      the node to remove
     */
    private void removeFunction(Node n)
    {
        LOGGER.log(Level.INFO, "removeFunction(" + getFunctionName(n) + ")");
        
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
        } else if (n.getParent().getType() == Token.ASSIGN || n.getParent().getType() == Token.EXPR_RESULT) {
            /*
             This is a property assignment function like:
                myObj.func1 = function()
             */
            Node expr = findExprOrVar(n);
            if (expr != null && expr.getType() == Token.EXPR_RESULT && expr.getParent() != null) {
                LOGGER.log(Level.FINE, "expr: " + expr);
                expr.detachFromParent();
            }
        } else {
            /*
             This is a standard type of function like this:
                function myFunc()
             */
            //println("n.toStringTree(): " + n.toStringTree());
            //println("Removing function: " + n.getFirstChild().getString());
            n.detachFromParent();
        }
    }
    
    /**
     * Find the closest expression result or variable declaration token parent of the 
     * specified node.
     * 
     * @param n      the child node to look for
     * 
     * @return the closest variable or expression result parent or null if there isn't one
     */
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
    
    /**
     * This method recurses all the functions and finds all the calls to actual functions
     * and adds them to the list of keepers.
     * 
     * @param call   the call to look for
     */
    private void findKeepers(Call call)
    {
        if (getCall(call.getName(), m_examinedCalls) != null) {
            /*
             Then we've already examined this call and we can skip it.
             */
            return;
        }
        
        //call.incCount();
        
        LOGGER.log(Level.FINE, "findKeepers(" + call + ")");
        
        m_examinedCalls.add(call);
        
        
        Node funcs[] = findMatchingFunctions(call.getName());
            
        for (Node func : funcs) {
            m_keepers.add(func);
            LOGGER.log(Level.FINE, "func: " + getFunctionName(func));
            
            for (Call c : findCalls(func)) {
                findKeepers(c);
            }
        }
    }
    
    /**
     * Find all of the calls in the given function.
     * 
     * @param func   the function to look in
     * 
     * @return the list of calls
     */
    private Call[] findCalls(Node func)
    {
        ArrayList<Call> calls = new ArrayList<Call>();
        findCalls(func, calls);
        return calls.toArray(new Call[calls.size()]);
    }
    
    /**
     * Find all of the calls in the given function.
     * 
     * @param node   the node to look in
     * @param calls  the list of calls to add the function to
     */
    private void findCalls(Node node, List<Call> calls)
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
    
    /**
     * Find all of the function nodes which are children, direct or otherwise, of the 
     * specified node.
     * 
     * @param parent the node to look for functions under
     * 
     * @return the array of functions found under this node (this array is never null)
     */
    private Node[] findFunctions(Node parent)
    {
        ArrayList<Node> funcs = new ArrayList<Node>();
        findFunctions(parent, funcs);
        
        return funcs.toArray(new Node[funcs.size()]);
        
    }
    
    /**
     * Find all of the function node which are children, direct or otherwise, of the
     * specified node and add them to the specified list.
     * 
     * @param node   the node to for functions under
     * @param funcs  the list of functions to add the references to
     * 
     * @return the array of functions found under this node (this array is never null)
     */
    private Node findFunctions(Node node, List<Node> funcs)
    {
        Iterator<Node> nodes = node.children().iterator();
        
        while (nodes.hasNext()) {
            Node n = nodes.next();
            if (n.getType() == Token.FUNCTION && 
                isInterestingFunction(n)) {
                funcs.add(n);
            }
            
            findFunctions(n, funcs);
        }
        
        return node;
    }
    
    /**
     * Get a list of the names of all of the functions under this specific node.  This method
     * does not recurse into all children, but is used for unravelling function changes.
     * 
     * @param n      the node to look under
     * 
     * @return the list of function names in this chaing
     */
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
            if (n.getFirstChild().getType() == Token.GETELEM) {
                String c = getConcatenatedStringIndex(n.getFirstChild());
                if (c != null) {
                    names.add(c);
                }
            } else if (n.getFirstChild().getLastChild() != null) {
                names.add(n.getFirstChild().getLastChild().getString());
            }
        }
        
        if (n.getParent().getType() == Token.ASSIGN) {
            names.addAll(getFunctionNames(n.getParent()));
        }
        
        return names;
    }
    
    /**
     * Get the name of the function at the specified node if this node represents an
     * interesting function.
     * 
     * @param n      the node to look under
     * 
     * @return the name of this function
     */
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
                    String c = getConcatenatedStringIndex(n.getParent().getFirstChild());
                    if (c != null) {
                        return c;
                    } else {
                        return null;
                    }
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
                if (n.getFirstChild().getType() == Token.GETPROP) {
                    /*
                     This is a chain function assignment
                     */
                    return n.getFirstChild().getFirstChild().getNext().getString();
                } else {
                    /*
                     This is a standard type of function like this:
                        function myFunc()
                     */
                    return n.getFirstChild().getString();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "npe: " + n.toStringTree());
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
    
    /**
     * <p>
     * Add an external reference to this compiler.  
     * </p>
     * 
     * <p>
     * There are some types of function reference which the compiler can't follow.  In those
     * cases the calling code must either declare those functions as external references or
     * they will be pruned from the tree.  This reference is just a simple string since 
     * this processor doesn't handle object associations.
     * </p>
     * 
     * @param extern the external reference to add
     */
    public void addExtern(String extern)
    {
        if (extern != null && extern.trim().length() > 0) {
            m_calls.add(new Call(extern));
        }
    }
    
    /**
     * Call the Google Closure Compiler to perform a plain compilation without any pruning.
     * This is normally the last step after pruning.
     * 
     * @param name   the name of the file to compile
     * @param code   the code contents of the file to compile
     * @param level  the compilation level for this compile 
     * @param formattingOptions the formtting options for this compile 
     * 
     * @return the compiled contents
     */
    public static String plainCompile(String name, String code, CompilationLevel level, FormattingOption formattingOptions)
    {
        Compiler compiler = new Compiler();
        
        compiler.setLoggingLevel(LOGGER.getLevel());
        
        Logger.getLogger("com.google.javascript.jscomp").setUseParentHandlers(false);
        Logger.getLogger("com.google.javascript.jscomp").addHandler(new SlimConsoleHandler());
        
        CompilerOptions options = new CompilerOptions();
        // Advanced mode is used here, but additional options could be set, too.
        level.setOptionsForCompilationLevel(options);
        
        if (formattingOptions != null) {
            formattingOptions.applyToOptions(options);
        }
        
        // To get the complete set of externs, the logic in
        // CompilerRunner.getDefaultExterns() should be used here.
        JSSourceFile extern = JSSourceFile.fromCode("externs.js", "");
        
        // The dummy input name "input.js" is used here so that any warnings or
        // errors will cite line numbers in terms of input.js.
        if (name == null) {
            name = "System.out.js";
        }
        JSSourceFile input = JSSourceFile.fromCode(name, code);
    
        // compile() returns a Result, but it is not needed here.
        compiler.compile(extern, input, options);
    
        // The compiler is responsible for generating the compiled code; it is not
        // accessible via the Result.
        return compiler.toSource();
    }
    
    /**
     * Get the list of kept functions after the pruning operation.
     * 
     * @return an array of the names of all the functions which were kept after the prune operation
     */
    public String[] getKeptFunctions()
    {
        ArrayList<String> funcs = new ArrayList<String>();
        for (Node n : m_keepers) {
            funcs.add(getFunctionName(n));
        }
        
        return funcs.toArray(new String[funcs.size()]);
    }
    
    /**
     * This method returns the total number of named or "interesting" functions found in the 
     * library files.  This count ignores anonymous functions and other functions this tool
     * doesn't analyze.
     * 
     * @return the total number of functions.
     */
    public int getTotalFunctionCount()
    {
        return m_funcCount;
    }
    
    /**
     * Get the charset used by this compiler.
     * 
     * @return the charset
     */
    public String getCharset()
    {
        return m_charset;
    }
    
    /**
     * Set the charset used by this compiler.
     * 
     * @param charset the charset
     */
    public void setCharset(String charset)
    {
        m_charset = charset;
    }
    
    /**
     * Determine of this compiler should print out the AST tree.
     * 
     * @return true if it should print the tree and false otherwise
     */
    public boolean shouldPrintTree()
    {
        return m_printTree;
    }
    
    /**
     * Set if this compiler should print the AST tree.
     * 
     * @param printTree true if it should print the tree and false otherwise
     */
    public void setPrintTree(boolean printTree)
    {
        m_printTree = printTree;
    }
    
    /**
     * Get the error manager for this compilation.  The error manager is never null, but it
     * can return a zero error count.
     * 
     * @return the error manager
     */
    public ErrorManager getErrorManager()
    {
        return m_errMgr;
    }
    
    /**
     * Write the specified file and a GZIPed file with the same name and a .gz extension.
     * 
     * @param contents the contents of the file
     * @param file     the file location to write
     * @param charset  the charset to use when writing the file
     * 
     * @exception IOException if there is an error writing the GZIPed file
     */
    public static void writeGzip(String contents, File file, String charset)
        throws IOException
    {
        FileOutputStream out = new FileOutputStream(new File(file.getParentFile(), file.getName() + ".gz"));
        
        try {
            GZIPOutputStream zipOut = new GZIPOutputStream(out);
            OutputStreamWriter out2 = new OutputStreamWriter(new BufferedOutputStream(zipOut), charset);
    
            IOUtils.write(contents, out2);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
    
    /*public static void main(String[] args)
    {
        try {
            JSlim slim = new JSlim();

            File in = new File("main.js");

            String mainJS = FileUtils.readFileToString(in, "UTF-8");
            //String mainJS = FileUtils.readFileToString(new File("libs/easing/easing.js"), "UTF-8");
            //slim.slim(mainJS, false);

            //String libJS = FileUtils.readFileToString(new File("libs/jquery-ui-1.8.14.custom.min.js"), "UTF-8");
            //String libJS = FileUtils.readFileToString(new File("libs/jquery.min.js"), "UTF-8");
            //String libJS = FileUtils.readFileToString(new File("lib.js"), "UTF-8");
            //String libJS = FileUtils.readFileToString(new File("libs/jquery-1.6.2.js"), "UTF-8");
            //String libJS = FileUtils.readFileToString(new File("libs/easing/raphael.js"), "UTF-8");
            //String libJS = FileUtils.readFileToString(new File("libs/chart/raphael.js"), "UTF-8");
            //println("compiled code: " + slim.addLib(libJS));

            slim.addSourceFile(new JSFile("main.js", mainJS, false));

            slim.addSourceFile(new JSFile("jquery-1.6.2.js", FileUtils.readFileToString(new File("libs/jquery-1.6.2.js"), "UTF-8"), true));
            slim.addSourceFile(new JSFile("underscore.js", FileUtils.readFileToString(new File("libs/underscore.js"), "UTF-8"), true));

            //slim.addSourceFile(new JSFile("modernizr-2.0.6.js", FileUtils.readFileToString(new File("libs/modernizr/modernizr-2.0.6.js"), "UTF-8"), true));

            File out = new File("out.js");
            JSlim.writeGzip(plainCompile("out.js", slim.prune(), CompilationLevel.SIMPLE_OPTIMIZATIONS), out, "UTF-8");
            //FileUtils.writeStringToFile(new File("out.js"), plainCompile(libJS));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/
}


/**
 * This little console handler makes it possible to send Java logging to System.out 
 * instead of System.err.
 */
class SlimConsoleHandler extends ConsoleHandler
{
    protected void setOutputStream(OutputStream out) throws SecurityException
    {
        super.setOutputStream(System.out);
    }
}
