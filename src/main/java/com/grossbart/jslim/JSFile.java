package com.grossbart.jslim;

public class JSFile
{
    private String m_name;
    private String m_content;
    
    private boolean m_isLib;
    
    
    public JSFile(String name, String content, boolean isLib)
    {
        m_name = name;
        m_content = content;
        m_isLib = isLib;
    }
    
    public String getName()
    {
        return m_name;
    }
    
    public String getContent()
    {
        return m_content;
    }
    
    public boolean isLib()
    {
        return m_isLib;
    }
    
    public String toString()
    {
        return "JSFile: " + m_name;
    }
}
