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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;

import com.google.common.collect.Lists;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.ErrorManager;

import org.apache.commons.io.FileUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.kohsuke.args4j.spi.BooleanOptionHandler;

/**
 * The runner handles all argument processing and sets up the precompile process.  
 */
public class JSlimRunner
{
    
    @Option(name = "--help", handler = BooleanOptionHandler.class, usage = "Displays this message")
    private boolean m_displayHelp = false;
    
    /**
     * This enumeration handles the compile level arguments.  It also introduces a new
     * argument to skip the Closure Compiler compilation.
     */
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
      NONE
    }
    
    @Option(name = "--compilation_level",
        usage = "Specifies the compilation level to use. Options: " +
        "WHITESPACE_ONLY, SIMPLE_OPTIMIZATIONS, ADVANCED_OPTIMIZATIONS")
    private SlimCompilationLevel m_compilationLevel = SlimCompilationLevel.SIMPLE_OPTIMIZATIONS;
    
    /**
     * This enumeration handles all of the log level argument for the command line of 
     * this application.
     */
    private enum LogLevel {
        ALL {
            @Override
            public Level getLevel()
            {
                return Level.ALL;
            }
        },

        CONFIG {
            @Override
            public Level getLevel()
            {
                return Level.CONFIG;
            }
        },

        FINE {
            @Override
            public Level getLevel()
            {
                return Level.FINE;
            }
        },

        FINER {
            @Override
            public Level getLevel()
            {
                return Level.FINER;
            }
        },

        FINEST {
            @Override
            public Level getLevel()
            {
                return Level.FINEST;
            }
        },

        INFO {
            @Override
            public Level getLevel()
            {
                return Level.INFO;
            }
        },

        OFF {
            @Override
            public Level getLevel()
            {
                return Level.OFF;
            }
        },

        SEVERE {
            @Override
            public Level getLevel()
            {
                return Level.SEVERE;
            }
        },

        WARNING {
            @Override
            public Level getLevel()
            {
                return Level.WARNING;
            }
        };
        
        public abstract Level getLevel();
    }
    
    @Option(name = "--logging_level",
        usage = "The logging level (standard java.util.logging.Level values) for Compiler progress. " + 
            "Does not control errors or warnings for the JavaScript code under compilation")
    private LogLevel m_loggingLevel = LogLevel.WARNING;
    
    @Option(name = "--js_output_file",
        usage = "Primary output filename. If not specified, output is " +
        "written to stdout")
    private String m_output = null;
    
    @Option(name = "--js",
        usage = "The javascript filename. You may specify multiple")
    private List<String> m_js = Lists.newArrayList();
    
    @Option(name = "--lib_js", usage = "The javascript library filename. You may specify multiple", required = true)
    private List<String> m_libJs = Lists.newArrayList();
    
    @Option(name = "--externs", usage = "The file containing javascript externs. You may specify multiple")
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
                "provide good error messages if the input files are invalid JavaScript.")
    private boolean m_preparse = true;
    
    @Option(name = "--combine_files",
        handler = BooleanOptionHandler.class,
        usage = "Pass this argument to combine the library files and the regular files into a single output file.")
    private boolean m_combine = true;
    
    @Option(name = "--flagfile",
        usage = "A file containing additional command-line options.")
    private String m_flagFile = "";
    
    private StringBuffer m_mainFiles = new StringBuffer();
    
    /**
     * Process the flags file and add the argument values to the current class.
     * 
     * @param out    the output stream for printing errors while parsing the arguments
     * 
     * @exception CmdLineException
     *                   if there's an error parsing the arguments
     * @exception IOException
     *                   if there is an exception reading the file
     */
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
    
    /**
     * Read in the externs file if one had been supplied and add the extern references to 
     * the compiler.
     * 
     * @param slim   the compiler
     * 
     * @exception IOException
     *                   if there's an error reading the externs file
     */
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
    
    /**
     * Get the compilation level for the closure compilation.
     * 
     * @return the complication level or null of the supplied level was SKIP
     */
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
    
    /**
     * Call the prune process.
     * 
     * @exception IOException
     *                   if there's an error reading or writing the files to prune
     */
    private void prune()
        throws IOException
    {
        JSlim slim = new JSlim();
        
        slim.setLoggingLevel(m_loggingLevel.getLevel());
        
        JSlim.getLogger().log(Level.INFO, "Compiling with character set " + m_charset);
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
        
        if (m_combine) {
            /*
             If they want to combine the main files and the library files
             then we just append them to the results here before the compile step.
             */
            result = result + "\n" + m_mainFiles;
        }
        
        CompilationLevel level = getCompilationLevel();
        
        if (level != null) {
            /*
             Then we run the results through the normal compilation process
             to make them even smaller
             */
            JSlim.getLogger().log(Level.INFO, "Starting closure compile with compile level " + level);
            result = JSlim.plainCompile(m_output, result, level);
        }
        
        /*
         Then we can write out the results
         */
        if (m_output == null) {
            System.out.println(result);
        } else {
            File out = new File(m_output).getAbsoluteFile();
            JSlim.getLogger().log(Level.INFO, "Writing to file " + out);
            if (!out.getParentFile().exists()) {
                JSlim.getLogger().log(Level.SEVERE, 
                                      "The specified output directory " + out.getParent() + " does not exist");
                return;
            }
            
            FileUtils.writeStringToFile(out, result);
            
            if (!m_skipGzip) {
                JSlim.getLogger().log(Level.INFO, "Writing GZIPed file");
                JSlim.writeGzip(result, out, m_charset);
            }
        }
        
        
    }
    
    /**
     * Add files for compilation.
     * 
     * @param slim   the compiler instance
     * @param files  the files to add
     * @param isLib  if these files are library files
     * 
     * @return true if the files were properly validated or false otherwise
     * @exception IOException
     *                   if there is an error reading the files
     */
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
            
            if (m_combine && !isLib) {
                m_mainFiles.append(contents + "\n");
            }
            
            if (isLib) {
                JSlim.getLogger().log(Level.INFO, "Adding library file: " + f.getAbsoluteFile());
            } else {
                JSlim.getLogger().log(Level.INFO, "Adding main file: " + f.getAbsoluteFile());
            }
            
            slim.addSourceFile(new JSFile(f.getName(), contents, isLib));
        }
        
        return true;
    }
    
    /**
     * Print the usage of this class.
     * 
     * @param parser the parser of the command line arguments
     */
    private static void printUsage(CmdLineParser parser)
    {
        System.out.println("java JSlimRunner [options...] arguments...\n");
        // print the list of available options
        parser.printUsage(System.out);
        System.out.println();
    }
    

    /**
     * The main entry point.
     * 
     * @param args   the arguments for this process
     */
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
