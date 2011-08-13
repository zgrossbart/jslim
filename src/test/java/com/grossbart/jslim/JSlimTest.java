package com.grossbart.jslim;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import org.junit.Test;
import static org.junit.Assert.*;

import com.google.javascript.jscomp.DiagnosticType;
import com.google.javascript.jscomp.JSError;


public class JSlimTest {
    @Test
    public void basicCompileTest()
        throws IOException
    {
        String basic = readFile("basic.js");
        
        JSlim slim = new JSlim();
        String out = slim.addLib(basic);
        String funcs[] = slim.getKeptFunctions();
        
        assertEquals(1, funcs.length);
        assertEquals("func1", funcs[0]);
    }
    
    @Test
    public void funcPropsTest()
        throws IOException
    {
        String basic = readFile("propFuncs.js");
        
        JSlim slim = new JSlim();
        String out = slim.addLib(basic);
        String funcs[] = slim.getKeptFunctions();
        
        assertEquals(1, funcs.length);
        assertEquals("func1", funcs[0]);
    }
    
    @Test
    public void assignmentTest()
        throws IOException
    {
        String basic = readFile("assignment.js");
        
        JSlim slim = new JSlim();
        String out = slim.addLib(basic);
        String funcs[] = slim.getKeptFunctions();
        
        assertEquals(1, funcs.length);
        assertEquals("func2", funcs[0]);
    }
    
    @Test
    public void argumentTest()
        throws IOException
    {
        String basic = readFile("argument.js");
        
        JSlim slim = new JSlim();
        String out = slim.addLib(basic);
        String funcs[] = slim.getKeptFunctions();
        
        assertEquals(1, funcs.length);
        assertEquals("func2", funcs[0]);
    }
    
    @Test
    public void parseErrorTest()
        throws IOException
    {
        String invalid = readFile("invalid.js");
        
        JSlim slim = new JSlim();
        String output = slim.addLib(invalid);
        
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
