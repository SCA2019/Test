package org.st.jenkins;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.helper.BuildConsoleStreamListener;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.JobWithDetails;
import com.offbytwo.jenkins.model.QueueItem;
import com.offbytwo.jenkins.model.QueueReference;

public class ST_JenkinsUtil {

	protected static JenkinsServer jenkinsServer;
	private static JobWithDetails job;
	public static final Long TIME_OUT_MILLISECONDS = TimeUnit.MILLISECONDS.convert(1L, TimeUnit.MINUTES);    

  /**
   * The time we wait until we break the beforeSuit method to prevent to wait
   * forever.
   */    
  @org.testng.annotations.Parameters({ "JenkinsServerURL", "UserName", "Password", "JobName"})
  @Test
  public static void triggerJenkinsJobAndWaitForCompletion(String strServerURL,String strUserName, String strPassword,String strJobName) throws TimeoutException, IOException, InterruptedException {
      final long start = System.currentTimeMillis();
      final URI JENKINS_URI = URI.create(strServerURL);
      jenkinsServer = new JenkinsServer(JENKINS_URI,strUserName,strPassword);
      System.out.print("Wait until Jenkins is started...");
      while (!jenkinsServer.isRunning() && !timeOut(start)) {
          try {
              System.out.print(".");
              Thread.sleep(TimeUnit.MILLISECONDS.convert(1L, TimeUnit.SECONDS));
          } catch (InterruptedException e) {
              e.printStackTrace();
          }
      }

      if (!jenkinsServer.isRunning() && timeOut(start)) {
          System.out.println("Failure.");
          throw new TimeoutException("Jenkins startup check has failed. Took more than one minute.");
      }
      else
      {
      	System.out.print("Jenkins is running");	    
      	job = jenkinsServer.getJob(strJobName);
      	QueueReference queueRef = job.build(true);
      	QueueItem queueItem = jenkinsServer.getQueueItem(queueRef);
      	while (queueItem.getExecutable() == null) {
      	       Thread.sleep(200);
      	       queueItem = jenkinsServer.getQueueItem(queueRef);
      	}
      	job = job.details();
      	      	      	
        //job = jenkinsServer.getJob(strJobName);
      	//QueueReference queueRef = job.build(true);
      	//System.out.println("Ref:" + queueRef.getQueueItemUrlPart());

      	//job = jenkinsServer.getJob(strJobName);
      	//QueueItem queueItem = jenkinsServer.getQueueItem(queueRef);
      	while (!queueItem.isCancelled() && job.isInQueue()) {
      	    System.out.println("In Queue " + job.isInQueue());
      	    Thread.sleep(200);
      	    job = jenkinsServer.getJob(strJobName);
      	    queueItem = jenkinsServer.getQueueItem(queueRef);
      	}
      	System.out.println("ended waiting.");

      	System.out.println("cancelled:" + queueItem.isCancelled());

      	if (queueItem.isCancelled()) {
      	    System.out.println("Job has been canceled.");
      	    return;
      	}

      	job = jenkinsServer.getJob(strJobName);
      	Build lastBuild = job.getLastBuild();
 
      	BuildWithDetails buildDetails = lastBuild.details();
          buildDetails.streamConsoleOutput(new BuildConsoleStreamListener(){
              @Override
              public void onData(String s) {
              	System.out.println(s); 
              }

              @Override
              public void finished() {
              	System.out.println("Finished " + strJobName + "+ Build Status " + buildDetails.getResult()); 
              }
          },2, 60);
 
        //int iTotal = job.details().getLastBuild().getTestReport().getTotalCount();
      	//int iPass = job.details().getLastBuild().getTestReport().getTotalCount();
      	//int iFail = job.details().getLastBuild().getTestReport().getTotalCount();      	
      }
  }
  
  /**
   * Check if we have reached timeout related to the
   * {@link #TIME_OUT_MILLISECONDS}.
   * 
   * @param start
   *            The start time in milliseconds.
   * @return true if timeout false otherwise.
   */
  private static boolean timeOut(final long start) {
      boolean result = false;

      long elapsed = System.currentTimeMillis() - start;
      if (elapsed >= TIME_OUT_MILLISECONDS) {
          result = true;
      }

      return result;
  }

   @AfterMethod
   public void afterTest() {
     //ST_WebDriverManager.quitDriver();
   }
}
