/*
 * This file is part of Jikes RVM (http://jikesrvm.sourceforge.net).
 * The Jikes RVM project is distributed under the Common Public License (CPL).
 * A copy of the license is included in the distribution, and is also
 * available at http://www.opensource.org/licenses/cpl1.0.php
 *
 * (C) Copyright IBM Corp. 2001
 */
package org.jikesrvm.scheduler;

import org.jikesrvm.VM;
import org.jikesrvm.classloader.VM_Method;
import org.jikesrvm.memorymanagers.mminterface.MM_Interface;
import org.jikesrvm.runtime.VM_Magic;
import org.jikesrvm.runtime.VM_Reflection;

/**
 * Finalizer thread.
 *
 * This thread is created by VM_Scheduler.boot() at runtime startup.
 * Its "run" method does the following:
 *    1. yield to the gcwaitqueue, until scheduled by g.c.
 *    2. For all objects on finalize Q, run the finalize() method
 *    3. Go to 1
 *
 * This thread comes out of wait state via notify from the garbage collector  
 */
public class VM_FinalizerThread extends VM_Thread {

   private static final int verbose = 0; // currently goes up to 2

  private final Object[]  none = new Object[0];
  
   public VM_FinalizerThread() {
     super(null);
   }

   public String toString() {
     return "FinalizerThread";
   }

   // Run the finalizer thread (one per RVM)
   //
   public void run() {

     if (verbose >= 1)
       VM_Scheduler.trace("VM_FinalizerThread ", "run routine entered");
     
     try {
       while (true) {
         
         // suspend this thread: it will resume when the garbage collector
         // places objects on the finalizer queue and notifies.
         
         VM_Scheduler.finalizerMutex.lock();
         VM_Thread.yield(VM_Scheduler.finalizerQueue, 
                         VM_Scheduler.finalizerMutex);
         
         if (verbose >= 1)
           VM.sysWriteln("VM_FinalizerThread starting finalization");
         
         while (true) {
           Object o = MM_Interface.getFinalizedObject();
           if (o == null) break;
           if (verbose >= 2) {
             VM.sysWrite("VM_FinalizerThread finalizing object at ", VM_Magic.objectAsAddress(o));
             VM.sysWrite("of type ");
             VM.sysWrite(VM_Magic.getObjectType(o).getDescriptor());
             VM.sysWriteln();
           }
           try {
             VM_Method method = VM_Magic.getObjectType(o).asClass().getFinalizer();
             if (VM.VerifyAssertions) VM._assert(method != null);
             VM_Reflection.invoke(method, o, none);
           }
           catch (Exception e) {
               if (verbose >= 1) VM.sysWriteln("Throwable exception caught for finalize call");
           }
           if (verbose >= 2) VM.sysWriteln("VM_FinalizerThread done with object at ",
                                           VM_Magic.objectAsAddress(o));
         }
         if (verbose >= 1) VM.sysWriteln("VM_FinalizerThread finished finalization");
         
       }          // while (true)
     }
     catch (Exception e) {
       VM.sysWriteln("Unexpected exception thrown in finalizer thread: ", e.toString());
       e.printStackTrace();
     }
     
   }  // run
  
  

}
