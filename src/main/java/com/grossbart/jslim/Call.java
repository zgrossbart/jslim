package com.grossbart.jslim;

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
