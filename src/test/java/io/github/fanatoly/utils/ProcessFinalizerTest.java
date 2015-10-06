package io.github.fanatoly.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import junit.framework.TestCase;
import org.junit.Rule;
import org.junit.Test;
import java.util.Enumeration;
import java.net.URL;
import static org.junit.Assert.*;

public class ProcessFinalizerTest extends TestCase{

    private final static String ERROR_TEXT = "SPECIAL_STRING";

    public static class TestMainNoThread{
        public static void main(String[] args){
            ProcessFinalizer.setCurrentThreadToFinalize();
            throw new RuntimeException(ERROR_TEXT);
        }
    }

    public static class SleepThread extends Thread{
        Thread parent;
        public SleepThread(Thread parent){
            this.parent = parent;
        }
        @Override public void run(){
             while(true){
                try{
                    Thread.sleep(100);
                    if(parent.getState().equals(Thread.State.TERMINATED))
                        System.out.println(ERROR_TEXT);
                } catch(Exception ex){
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    public static class TestMainWithThread{

    	public static void main(String[] args){
            ProcessFinalizer.setCurrentThreadToFinalize();
            new SleepThread(Thread.currentThread()).start();
    	    throw new RuntimeException(ERROR_TEXT);
    	}
    }


    public static class TestMainNoError{
        public static void main(String[] args){
            ProcessFinalizer.setCurrentThreadToFinalize();
        }
    }

    public static class TestMainWithThreadNoError{
    	public static void main(String[] args){
    	    ProcessFinalizer.setCurrentThreadToFinalize();
            new SleepThread(Thread.currentThread()).start();
            return;
    	}
   }


    private String inferClassPath(Class<?> clazz) throws Exception{
        ClassLoader cl = clazz.getClassLoader();
        String class_file = clazz.getName().replaceAll("\\.", "/") + ".class";
        for(@SuppressWarnings("rawtypes")
            Enumeration itr = cl.getResources(class_file);
            itr.hasMoreElements();) {
            URL url = (URL) itr.nextElement();
            if("file".equals(url.getProtocol())){
                String path = url.getPath();
                System.out.println(path.substring(0, path.lastIndexOf(class_file)));
                return path.substring(0, path.lastIndexOf(class_file));
            }

        }
        return "";
    }

    private Process runClass(Class<?> clazz) throws Exception{
        String separator = System.getProperty("file.separator");
        String classpath = System.getProperty("java.class.path") + ":" + System.getProperty("run.classpath");
        String path = System.getProperty("java.home")
            + separator + "bin" + separator + "java";
        ProcessBuilder processBuilder =
            new ProcessBuilder(path, "-cp",
                               classpath,
                               clazz.getName())
            .redirectErrorStream(true)
            .redirectInput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .redirectOutput(ProcessBuilder.Redirect.PIPE);

        return processBuilder.start();
    }

    @Test
    public  void testErrorWithFinalizerNoThread() throws Exception {
        Process sub = runClass(TestMainNoThread.class);
        BufferedReader subOut = new BufferedReader(new InputStreamReader(sub.getInputStream()));
        boolean expectedOutput = false;
        String line;
        while(((line = subOut.readLine()) != null) && !expectedOutput){
            expectedOutput = line.contains(ERROR_TEXT);
        }

        subOut.close();
        assertTrue("Output contained error text", expectedOutput);

        assertEquals(sub.waitFor(), 1);
    }

    @Test
    public  void testErrorWithFinalizerNoError() throws Exception {
        assertEquals(runClass(TestMainNoError.class).waitFor(), 0);
    }


    public  void testErrorWithFinalizerThread() throws Exception {
        Process sub = runClass(TestMainWithThread.class);
        try{
            assertTrue("Subprocess seems to be running forever", ensureTerminated(sub));
            sub.waitFor();
            assertEquals(1, sub.exitValue());
        }
        finally{
            sub.destroy();
        }
    }

    public  void testErrorWithFinalizerThreadNoError() throws Exception {
        Process sub = runClass(TestMainWithThreadNoError.class);
        try{
            assertTrue("Subprocess seems to be running forever", ensureTerminated(sub));
            sub.waitFor();
            assertEquals(0, sub.exitValue());
        }
        finally{
            sub.destroy();
        }
    }

    boolean ensureTerminated(Process sub) throws Exception{
        BufferedReader subOut = new BufferedReader(new InputStreamReader(sub.getInputStream()));
        int outputCount = 0;
        String line;
        while(((line = subOut.readLine()) != null) && outputCount <= 5){
            if(line.contains(ERROR_TEXT)){
                outputCount++;
            }
        }

        return outputCount <= 5;
    }


}
