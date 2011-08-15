package com.grossbart.jslim;

/**
 * This class represents a single source file for the compiler, either main source or 
 * library source.
 */
public class JSFile
{
    private String m_name;
    private String m_content;
    
    private boolean m_isLib;
    
    
    /**
     * Create a new JSFile.
     * 
     * @param name    the name of the file
     * @param content the content of the file
     * @param isLib   true if this file is a library file and false otherwise
     */
    public JSFile(String name, String content, boolean isLib)
    {
        m_name = name;
        m_content = content;
        m_isLib = isLib;
    }
    
    /**
     * Get the name of the file.
     * 
     * @return the file name
     */
    public String getName()
    {
        return m_name;
    }
    
    /**
     * Get the content of the file.
     * 
     * @return the file content
     */
    public String getContent()
    {
        return m_content;
    }
    
    /**
     * Indicate if this is a library file or main source file.
     * 
     * @return true if this file is a library file and false otherwise
     */
    public boolean isLib()
    {
        return m_isLib;
    }
    
    public String toString()
    {
        return "JSFile: " + m_name;
    }
}
