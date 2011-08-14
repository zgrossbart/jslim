package com.grossbart.jslim;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import com.google.common.collect.Lists;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;

public class JSlimRunner {
    
    @Option(name = "--help", handler = BooleanOptionHandler.class, usage = "Displays this message")
    private boolean m_displayHelp = false;
    
    @Option(name = "--js_output_file",
        usage = "Primary output filename. If not specified, output is " +
        "written to stdout")
    private String m_jsOutputFile = "";
    
    @Option(name = "--js",
        usage = "The javascript filename. You may specify multiple")
    private List<String> m_js = Lists.newArrayList();
    
    @Option(name = "--lib_js",
        usage = "The javascript library filename. You may specify multiple",
        required=true)
    private List<String> m_libJs = Lists.newArrayList();
    
    @Option(name = "--externs",
        usage = "The file containing javascript externs. You may specify"
        + " multiple")
    private List<String> externs = Lists.newArrayList();

    public static void main(String[] args) {
        
        JSlimRunner runner = new JSlimRunner();
        
        // parse the command line arguments and options
        CmdLineParser parser = new CmdLineParser(runner);
        parser.setUsageWidth(80); // width of the error display area
        
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.out.println(e.getMessage() + '\n');
            System.out.println("java JSlimRunner [options...] arguments...\n");
            // print the list of available options
            parser.printUsage(System.out);
            System.out.println();
            return;
        }
        
        if (runner.m_displayHelp) {
            parser.printUsage(System.out);
            return;
        }
    }
}
