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
