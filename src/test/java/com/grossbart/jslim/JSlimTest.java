package com.grossbart.jslim;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This test class covers the full unit tests for JSlim.
 */
public class JSlimTest
{

    /**
     * The basic compile test does a simple compile.
     * 
     * @exception IOException if there is any error reading the sample file
     */
    @Test
    public void basicCompileTest()
        throws IOException
    {
        JSlim slim = new JSlim();
        String out = slim.addLib("basic.js", readFile("basic.js"));
        String funcs[] = slim.getKeptFunctions();
        
        assertEquals(1, funcs.length);
        assertEquals("func1", funcs[0]);
    }
    
    /**
     * Tests for functional property assignment.
     * 
     * @exception IOException if there is any error reading the sample file
     */
    @Test
    public void funcPropsTest()
        throws IOException
    {
        JSlim slim = new JSlim();
        String out = slim.addLib("propFuncs.js", readFile("propFuncs.js"));
        String funcs[] = slim.getKeptFunctions();
        
        assertEquals(1, funcs.length);
        assertEquals("func1", funcs[0]);
    }
    
    /**
     * Test function assignments.
     * 
     * @exception IOException if there is any error reading the sample file
     */
    @Test
    public void assignmentTest()
        throws IOException
    {
        JSlim slim = new JSlim();
        String out = slim.addLib("assignment.js", readFile("assignment.js"));
        String funcs[] = slim.getKeptFunctions();
        
        assertEquals(1, funcs.length);
        assertEquals("func2", funcs[0]);
    }
    
    /**
     * Test function arguments.
     * 
     * @exception IOException if there is any error reading the sample file
     */
    @Test
    public void argumentTest()
        throws IOException
    {
        JSlim slim = new JSlim();
        String out = slim.addLib("argument.js", readFile("argument.js"));
        String funcs[] = slim.getKeptFunctions();
        
        assertEquals(1, funcs.length);
        assertEquals("func2", funcs[0]);
    }
    
    /**
     * Test functions by reference.
     * 
     * @exception IOException if there is any error reading the sample file
     */
    @Test
    public void referenceTest()
        throws IOException
    {
        JSlim slim = new JSlim();
        String out = slim.addLib("reference.js", readFile("reference.js"));
        String funcs[] = slim.getKeptFunctions();
        
        assertEquals(1, funcs.length);
        assertEquals("func2", funcs[0]);
    }
    
    /**
     * Test functions by reference chain
     * 
     * @exception IOException if there is any error reading the sample file
     */
    @Test
    public void referenceChainTest()
        throws IOException
    {
        JSlim slim = new JSlim();
        String out = slim.addLib("referencechain.js", readFile("referencechain.js"));
        String funcs[] = slim.getKeptFunctions();
        
        assertEquals(1, funcs.length);
        assertEquals("func2", funcs[0]);
    }
    
    /**
     * Test functions as returned objects.
     * 
     * @exception IOException if there is any error reading the sample file
     */
    @Test
    public void functionReturnTest()
        throws IOException
    {
        JSlim slim = new JSlim();
        
        /*
         This test references a function as a property and we can't follow
         that.  This is a place where we need an extern reference.
         */
        slim.addExtern("func2");
        
        String out = slim.addLib("functionreturn.js", readFile("functionreturn.js"));
        String funcs[] = slim.getKeptFunctions();
        
        assertEquals(2, funcs.length);
        assertEquals("func1", funcs[1]);
        assertEquals("func2", funcs[0]);
    }
    
    /**
     * Check the other type of functions as returned objects.
     * 
     * @exception IOException if there is any error reading the sample file
     */
    @Test
    public void functionReturn2Test()
        throws IOException
    {
        JSlim slim = new JSlim();
        
        String out = slim.addLib("functionreturn2.js", readFile("functionreturn2.js"));
        String funcs[] = slim.getKeptFunctions();
        
        assertEquals(1, funcs.length);
        assertEquals("func1", funcs[0]);
        
        /*
         The internally returned function in this test isn't "interesting" to
         our parser so it doesn't show up in the keepers list, but we still
         want to make sure that it is in the output.
         */
        assertTrue("The func2 function should still be in the output", out.indexOf("func2") > -1);
    }
    
    /**
     * Test inline functions.
     * 
     * @exception IOException if there is any error reading the sample file
     */
    @Test
    public void inlineFunctionReturnTest()
        throws IOException
    {
        JSlim slim = new JSlim();
        String out = slim.addLib("inlinefunctionreturn.js", readFile("inlinefunctionreturn.js"));
        String funcs[] = slim.getKeptFunctions();
        
        assertEquals(1, funcs.length);
        assertEquals("func1", funcs[0]);
        
        /*
         Now we need to make sure the newf function is still there
         */
        assertTrue("The newf function should still be in the output", out.indexOf("newf") > -1);
    }
    
    /**
     * Test functions assigned to a reference array.
     * 
     * @exception IOException if there is any error reading the sample file
     */
    @Test
    public void functionRefArrayTest()
        throws IOException
    {
        JSlim slim = new JSlim();
        String out = slim.addLib("functionrefarray.js", readFile("functionrefarray.js"));
        String funcs[] = slim.getKeptFunctions();
        
        assertEquals(1, funcs.length);
        assertEquals("func1", funcs[0]);
    }
    
    /**
     * Test a property assignment chain.
     * 
     * @exception IOException if there is any error reading the sample file
     */
    @Test
    public void propertyAssignmentChainTest()
        throws IOException
    {
        JSlim slim = new JSlim();
        String out = slim.addLib("propertyassignmentchain.js", readFile("propertyassignmentchain.js"));
        String funcs[] = slim.getKeptFunctions();
        
        assertEquals(1, funcs.length);
        assertEquals("func3", funcs[0]);
        
        /*
         Now we need to make sure the func1 function is still there
         */
        assertTrue("The func1 function should still be in the output", out.indexOf("func1") > -1);
    }
    
    /**
     * Check a file with a parse error.
     * 
     * @exception IOException if there is any error reading the sample file
     */
    @Test
    public void parseErrorTest()
        throws IOException
    {
        JSlim slim = new JSlim();
        String output = slim.addLib("invalid.js", readFile("invalid.js"));
        
        //slim.getErrorManager().generateReport();
        
        assertEquals(1, slim.getErrorManager().getErrorCount());
        assertEquals("JSC_TRAILING_COMMA", slim.getErrorManager().getErrors()[0].getType().key);
    }
    
    private String readFile(String name)
        throws IOException
    {
        InputStream in = getClass().getClassLoader().getResourceAsStream(name);
        try {
            return IOUtils.toString(in, "UTF-8");
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
