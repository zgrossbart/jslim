package com.grossbart.jslim;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import org.junit.Test;
import static org.junit.Assert.*;


public class JSlimTest {
    @Test
    public void basicCompileTest()
        throws IOException
    {
        InputStream in = getClass().getClassLoader().getResourceAsStream("basic.js");
        
        try {
            String basic = IOUtils.toString(in, "UTF-8");
            
            JSlim slim = new JSlim();
            System.out.println("slim.addLib(basic): " + slim.addLib(basic));
            String funcs[] = slim.getKeptFunctions();
            
            for (String s : slim.getKeptFunctions()) {
                System.out.println("Kept function: " + s);
            }
            
            assertEquals(funcs.length, 1);
            assertEquals(funcs[0], "func1");
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }
}
