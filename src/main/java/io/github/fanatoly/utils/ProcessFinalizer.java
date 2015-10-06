package io.github.fanatoly.utils;

import java.lang.Thread;

public class ProcessFinalizer implements Thread.UncaughtExceptionHandler{

    private static class FinalizerThread extends Thread{
        private Thread target;
        public FinalizerThread(Thread target){
            this.target = target;
            setDaemon(true);
        }
        @Override public void run(){
            boolean ended = false;
            while(!ended){
                try{
                    this.target.join();
                    ended = true;
                }catch(InterruptedException ex){

                }
            }
            System.exit(0);

        }
    }

    public static void setCurrentThreadToFinalize(){
        Thread.currentThread().setUncaughtExceptionHandler(new ProcessFinalizer());
        new FinalizerThread(Thread.currentThread()).start();
    }

    @Override public void uncaughtException(Thread t, Throwable ex){
        ex.printStackTrace();
        System.exit(1);
    }
}
