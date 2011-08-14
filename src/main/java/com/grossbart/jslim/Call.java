package com.grossbart.jslim;

/**
 * A Call represents a single function call within either the library JavaScript files
 * or the main JavaScript files.
 */
public class Call
{
    private int m_count = 1;
    private String m_name;
    
    public Call(String name)
    {
        m_name = name;
    }
    
    public int getCount()
    {
        return m_count;
    }
    
    public int incCount()
    {
        return m_count++;
    }
    
    public int decCount()
    {
        return m_count--;
    }
    
    public int decCount(int dec)
    {
        return m_count -= dec;
    }
    
    public String getName()
    {
        return m_name;
    }
    
    public String toString()
    {
        return m_name + ": " + m_count;
    }
}
