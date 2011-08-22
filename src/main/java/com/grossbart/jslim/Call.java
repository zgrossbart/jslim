package com.grossbart.jslim;

/**
 * A Call represents a single function call within either the library JavaScript files
 * or the main JavaScript files.  Each call is just the name of the function and a count
 * of the number of times it is called.
 */
public class Call
{
    private int m_count = 1;
    private String m_name;
    
    /**
     * Create a new Call object.
     * 
     * @param name   the name of this call
     */
    public Call(String name)
    {
        m_name = name;
    }
    
    /**
     * Get the count of this call.
     * 
     * @return the number of times this call was made
     */
    public int getCount()
    {
        return m_count;
    }
    
    /**
     * Increment the call count.
     * 
     * @return the new call count
     */
    public int incCount()
    {
        return m_count++;
    }
    
    /**
     * Decrement the call count.
     * 
     * @return the new call count
     */
    public int decCount()
    {
        return m_count--;
    }
    
    /**
     * Decrement the call count.
     * 
     * @param dec    the amount to decrement
     * 
     * @return the new call count
     */
    public int decCount(int dec)
    {
        m_count -= dec;
        return m_count;
    }
    
    /**
     * Get the name of this function.
     * 
     * @return the name
     */
    public String getName()
    {
        return m_name;
    }
    
    @Override
    public String toString()
    {
        return m_name + ": " + m_count;
    }
}
