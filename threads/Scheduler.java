// Scheduler.java
//	Class to choose the next thread to run.
//
// 	These routines assume that interrupts are already disabled.
//	If interrupts are disabled, we can assume mutual exclusion
//	(since we are on a uniprocessor).
//
// 	NOTE: We can't use Locks to provide mutual exclusion here, since
// 	if we needed to wait for a lock, and the lock was busy, we would 
//	end up calling FindNextToRun(), and that would put us in an 
//	infinite loop.
//
// 	Very simple implementation -- no priorities, straight FIFO.
//	Might need to be improved in later assignments.
//
// Copyright (c) 1992-1993 The Regents of the University of California.
// Copyright (c) 1998 Rice University.
// All rights reserved.  See the COPYRIGHT file for copyright notice and
// limitation of liability and disclaimer of warranty provisions.

//------------------------------------------------------------------------
// Create a handler for scheduled interrupts for Round Robin 
// implementation.
// 
// MP1
//------------------------------------------------------------------------
//class YourHandler 

class Scheduler {

  static private List readyList; // queue of threads that are ready to run, but not running

  // constants for scheduling policies
  static final int POLICY_PRIO_NP = 1;
  static final int POLICY_PRIO_P = 2;
  static final int POLICY_RR = 3;
  static final int POLICY_SJF_NP = 4;
  static final int POLICY_SJF_P = 5;
  static final int POLICY_FCFS = 6;

  static int policy = POLICY_FCFS;

  static public NachosThread threadToBeDestroyed;

  // Initialize the list of ready but not running threads to empty
  static { 
    readyList = new List(); 
  } 

  // Non-preemptive Priority Scheduling: Return the next thread with the highest priority
  public static NachosThread findNextToRun() {
    NachosThread highestPriorityThread = null;
    for (NachosThread thread : readyList) {
        if (highestPriorityThread == null || thread.getPriority() < highestPriorityThread.getPriority()) {
            highestPriorityThread = thread;
        }
    }
    if (highestPriorityThread != null) {
        readyList.remove(highestPriorityThread);
    }
    return highestPriorityThread;
  }

  public static void run(NachosThread nextThread) {
    NachosThread oldThread;
    oldThread = NachosThread.thisThread();

    if (Nachos.USER_PROGRAM) { 
      if (oldThread.space != null) { 
        oldThread.saveUserState(); 
        oldThread.space.saveState();
      }
    }

    // MP1 Round Robin - schedule an interrupt if necessary
    if (policy == POLICY_RR) {
      Interrupt.schedule(new Runnable() {
        public void run() {
          Interrupt.setLevel(Interrupt.INTERRUPT_OFF); 
          Scheduler.yield();
        }
      }, 40, Interrupt.TimerInt); 
    }

    Debug.println('t', "Switching from thread: " + oldThread.getName() + " to thread: " + nextThread.getName());

    synchronized (nextThread) {
      nextThread.setStatus(NachosThread.RUNNING);
      nextThread.notify();
    }
    synchronized (oldThread) {
      while (oldThread.getStatus() != NachosThread.RUNNING) 
        try { oldThread.wait(); } catch (InterruptedException e) {};
    }

    Debug.println('t', "Now in thread: " + NachosThread.thisThread().getName());

    if (threadToBeDestroyed != null) {
      threadToBeDestroyed.stop();
      threadToBeDestroyed = null;
    }

    if (Nachos.USER_PROGRAM) {
      if (oldThread.space != null) {
        oldThread.restoreUserState();
        oldThread.space.restoreState();
      }
    }
  }

  public static void print() {
    System.out.print("Ready list contents:");
    readyList.print();
  }

  public static void setSchedulerPolicy(int p) {
    policy = p;
  }

  // Preemptive Priority Scheduling: Decide if the current thread should be preempted
  public static boolean shouldISwitch(NachosThread current, NachosThread newThread) {
    if (policy == POLICY_PRIO_P) {
        return newThread.getPriority() < current.getPriority();
    }
    if (policy == POLICY_SJF_P) {
        return newThread.getEstimatedRunTime() < current.getEstimatedRunTime();
    }
    return false;
  }
}
