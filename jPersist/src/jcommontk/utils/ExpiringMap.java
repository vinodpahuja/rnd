/**
 * Copyright (C) 2006 - present Software Sensation Inc.  
 * All Rights Reserved.
 *
 * This file is part of jCommonTk.
 *
 * jCommonTk is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or 
 * FITNESS FOR A PARTICULAR PURPOSE. See the accompanying license 
 * for more details.
 *
 * You should have received a copy of the license along with jCommonTk; if not, 
 * go to http://www.softwaresensation.com and download the latest version.
 */

package jcommontk.utils;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("unchecked")
public class ExpiringMap extends ConcurrentHashMap
  {
    private static final long serialVersionUID = 100L;
  
    long expiration_time;
    boolean stop_thread;
    ExpiringMap thisInstance = this;
    
    public ExpiringMap(long expiration_time)
      {
        this.expiration_time = expiration_time;
        
        if (expiration_time > 0)
          new TimedCleanup().start();
      }
    
    public Object get(Object key)
      {
        TimedMapObject t_obj = (TimedMapObject)super.get(key);
        
        if (t_obj != null)
          return t_obj.getObject();
        
        return null;
      }

    public Object put(Object key, Object obj) 
      {
        TimedMapObject t_obj = (TimedMapObject)super.put(key, new TimedMapObject(obj)); 

        if (t_obj != null)
          return t_obj.getObject();

        return null;
      }

    public Iterator iterator()
      {
        return new Iterator() 
          {
            Iterator it = entrySet().iterator();

            public boolean hasNext() 
              {
                return it.hasNext();
              }

            public Object next() 
              {
                return ((TimedMapObject)it.next()).getObject();
              }

            public void remove() 
              {
                it.remove();
              }
          };
      }
    
    public Enumeration elements()
      {
        return new Enumeration() 
          {
            Enumeration e = elements();

            public boolean hasMoreElements()
              {
                return e.hasMoreElements();
              }
            
            public Object nextElement()
              {
                return ((TimedMapObject)e.nextElement()).getObject();
              }
          };
      }
    
    protected void finalize()
      {
        stop_thread = true;
      }
    
    class TimedCleanup extends Thread
      {
        public void run()
          {
            while (! stop_thread)
              {
                try { sleep(5000); } catch (InterruptedException ex) { } //don't care 
                
                Iterator it = thisInstance.entrySet().iterator();

                while (it.hasNext())
                  {
                    Map.Entry entry = (Map.Entry)it.next();
                    TimedMapObject t_obj = (TimedMapObject)entry.getValue();

                    if (System.currentTimeMillis() - t_obj.getTime() > expiration_time)
                      it.remove();
                  }
              }
          }
      }
    
    static class TimedMapObject
      {
        long time = System.currentTimeMillis();
        Object obj;

        TimedMapObject(Object obj)
          {
            this.obj = obj;
          }
        
        long getTime() { return time; }
        
        Object getObject()
          {
            time = System.currentTimeMillis();
            
            return obj;
          }
      }
    
    public static void main(String[] args)
      {
        ExpiringMap map = new ExpiringMap(1000);
        
        for (int i = 0; i < 100; i++)
          {
            map.put("test" + i, "this is a test" + i);
            System.out.println(map.get("test" + i));
            try { Thread.sleep(500); } catch (InterruptedException e) { }
            System.out.println(map.size());
          }
        
        System.exit(0);
      }
  }

