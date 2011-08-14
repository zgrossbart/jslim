package com.grossbart.jslim;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.google.common.collect.Lists;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.ErrorManager;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;

public class JSlimRunner {
    
    @Option(name = "--help", handler = BooleanOptionHandler.class, usage = "Displays this message")
    private boolean m_displayHelp = false;
    
    private enum SlimCompilationLevel {
        
      /**
       * WHITESPACE_ONLY removes comments and extra whitespace in the input JS.
       */
      WHITESPACE_ONLY,
    
      /**
       * SIMPLE_OPTIMIZATIONS performs transformations to the input JS that do not
       * require any changes to JS that depend on the input JS. For example,
       * function arguments are renamed (which should not matter to code that
       * depends on the input JS), but functions themselves are not renamed (which
       * would otherwise require external code to change to use the renamed function
       * names).
       */
      SIMPLE_OPTIMIZATIONS,
    
      /**
       * ADVANCED_OPTIMIZATIONS aggressively reduces code size by renaming function
       * names and variables, removing code which is never called, etc.
       */
      ADVANCED_OPTIMIZATIONS,
        
      /**
       * NONE only prunes the library classes and doesn't run the closure compiler on 
       * the resulting JavaScript file. 
       */
      SKIP
    }
    
    @Option(name = "--compilation_level",
        usage = "Specifies the compilation level to use. Options: " +
        "WHITESPACE_ONLY, SIMPLE_OPTIMIZATIONS, ADVANCED_OPTIMIZATIONS")
    private SlimCompilationLevel m_compilationLevel = SlimCompilationLevel.SIMPLE_OPTIMIZATIONS;
    
    @Option(name = "--js_output_file",
        usage = "Primary output filename. If not specified, output is " +
        "written to stdout")
    private String m_output = null;
    
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
    private List<String> m_externs = Lists.newArrayList();
    
    @Option(name = "--charset",
        usage = "Input and output charset for all files. By default, we " +
                "accept UTF-8 as input and output US_ASCII")
    private String m_charset = "UTF-8";
    
    @Option(name = "--print_tree",
        handler = BooleanOptionHandler.class,
        usage = "Prints out the parse tree and exits")
    private boolean m_printTree = false;
    
    @Option(name = "--skip_gzip",
        handler = BooleanOptionHandler.class,
        usage = "Skip GZIPing the results")
    private boolean m_skipGzip = false;
    
    @Option(name = "--no_validate",
        handler = BooleanOptionHandler.class,
        usage = "Pass this argument to skip the pre-parse file validation step.  This is faster, but won't " +
                "provide good error messages if the input files are invalid JavaScrip.")
    private boolean m_preparse = true;
    
    @Option(name = "--flagfile",
        usage = "A file containing additional command-line options.")
    private String m_flagFile = "";
    
    private void processFlagFile(PrintStream out)
        throws CmdLineException, IOException
    {
        if (m_flagFile == null || m_flagFile.trim().length() == 0) {
            return;
        }
        
        List<String> argsInFile = Lists.newArrayList();
        File flagFileInput = new File(m_flagFile);
        
        String flags = FileUtils.readFileToString(flagFileInput, m_charset);
        
        StringTokenizer tokenizer = new StringTokenizer(flags);
        
        while (tokenizer.hasMoreTokens()) {
            argsInFile.add(tokenizer.nextToken());
        }
        
        m_flagFile = "";
        
        CmdLineParser parserFileArgs = new CmdLineParser(this);
        parserFileArgs.parseArgument(argsInFile.toArray(new String[] {}));
        
        // Currently we are not supporting this (prevent direct/indirect loops)
        if (!m_flagFile.equals("")) {
            out.println("ERROR - Arguments in the file cannot contain --flagfile option.");
        }
    }
    
    private void readExterns(JSlim slim)
        throws IOException
    {
        for (String f : m_externs) {
            File file = new File(f);
            List<String> externs = FileUtils.readLines(file, m_charset);
            
            for (String extern : externs) {
                slim.addExtern(extern);
            }
        }
    }
    
    private CompilationLevel getCompilationLevel()
    {
        if (m_compilationLevel == SlimCompilationLevel.WHITESPACE_ONLY) {
            return CompilationLevel.WHITESPACE_ONLY;
        } else if (m_compilationLevel == SlimCompilationLevel.SIMPLE_OPTIMIZATIONS) {
            return CompilationLevel.SIMPLE_OPTIMIZATIONS;
        } else if (m_compilationLevel == SlimCompilationLevel.ADVANCED_OPTIMIZATIONS) {
            return CompilationLevel.ADVANCED_OPTIMIZATIONS;
        } else {
            return null;
        }
    }
    
    private void prune()
        throws IOException
    {
        JSlim slim = new JSlim();
        slim.setCharset(m_charset);
        slim.setPrintTree(m_printTree);
        
        /*
         First we add the externs
         */
        readExterns(slim);
        
        /*
         Then we add the source files
         */
        if (!addFiles(slim, m_js, false)) {
            return;
        }
        
        if (!addFiles(slim, m_libJs, true)) {
            return;
        }
        
        /*
         Then we can call the prune process
         */
        String result = slim.prune();
        
        CompilationLevel level = getCompilationLevel();
        
        if (level != null) {
            /*
             Then we run the results through the normal compilation process
             to make them even smaller
             */
            result = JSlim.plainCompile(m_output, result, level);
        }
        
        /*
         Then we can write out the results
         */
        if (m_output == null) {
            System.out.println(result);
        } else {
            File out = new File(m_output).getAbsoluteFile();
            System.out.println("out: " + out);
            if (!out.getParentFile().exists()) {
                System.err.println("The specified output directory " + out.getParent() + " does not exist");
                return;
            }
            
            if (!m_skipGzip) {
                JSlim.writeGzip(result, out, m_charset);
            }
        }
        
        
    }
    
    private boolean addFiles(JSlim slim, List<String> files, boolean isLib)
        throws IOException
    {
        for (String file : files) {
            File f = new File(file);
            String contents = FileUtils.readFileToString(f, m_charset);
            
            if (m_preparse) {
                ErrorManager mgr = slim.validate(f.getAbsolutePath(), contents);
                if (mgr.getErrorCount() != 0) {
                    mgr.generateReport();
                    return false;
                }
            }
            
            slim.addSourceFile(new JSFile(f.getName(), contents, isLib));
        }
        
        return true;
    }
    
    private static void printUsage(CmdLineParser parser)
    {
        System.out.println("java JSlimRunner [options...] arguments...\n");
        // print the list of available options
        parser.printUsage(System.out);
        System.out.println();
    }
    

    public static void main(String[] args)
    {
        JSlimRunner runner = new JSlimRunner();
        
        // parse the command line arguments and options
        CmdLineParser parser = new CmdLineParser(runner);
        parser.setUsageWidth(80); // width of the error display area
        
        if (args.length == 0) {
            printUsage(parser);
            return;
        }
        
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.out.println(e.getMessage() + '\n');
            printUsage(parser);
            return;
        }
        
        try {
            runner.processFlagFile(System.out);
            
            if (runner.m_displayHelp) {
                parser.printUsage(System.out);
                return;
            }
            
            runner.prune();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
