package snowblossom.channels;

import java.util.LinkedList;

/**
 * Like a Semaphore, but with expiration
 */
public class TimeSem
{
  private final long expiration_ms;
  private final int max_permits;
  private int avail;

  private LinkedList<Long> outstanding_list = new LinkedList<>();

  public TimeSem(int max_permits, long expiration_ms)
  {
    this.expiration_ms = expiration_ms;
    this.avail = max_permits;
    this.max_permits = max_permits;
  }

  public void release()
  {
    release(1);
  }

  public synchronized void release(int n)
  {
    if (n >= outstanding_list.size())
    {
      outstanding_list.clear();
    }
    else
    {
      for(int i=0; i<n; i++)
      {
        outstanding_list.poll();
      }
    }

    avail=Math.min(avail+n, max_permits);
    this.notifyAll();
  }

  public synchronized boolean tryAcquire()
  {
    maintain();

    if(avail > 0)
    {
      outstanding_list.add(System.currentTimeMillis() + expiration_ms);
      avail--;
      return true;
    }
    return false;
  }

  public synchronized int availablePermits()
  {
    maintain();
    return avail;
  }

  private synchronized void maintain()
  {
    long now = System.currentTimeMillis();
    while (outstanding_list.size() > 0)
    {
      if (outstanding_list.peek() > now)
      {
        return;
      }
      outstanding_list.poll();
      avail=Math.min(avail+1, max_permits);
      this.notifyAll();

    }

  }



}
