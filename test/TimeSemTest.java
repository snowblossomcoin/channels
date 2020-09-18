package channels;

import org.junit.Assert;
import org.junit.Test;
import snowblossom.channels.TimeSem;

public class TimeSemTest
{
  @Test
  public void testReject()
  {
    TimeSem sem = new TimeSem(1, 100);

    Assert.assertTrue(sem.tryAcquire());
    Assert.assertFalse(sem.tryAcquire());
  }

  @Test
  public void testAccept()
  {
    TimeSem sem = new TimeSem(32, 10000);

    Assert.assertEquals(32, sem.availablePermits());

    for(int i=0; i<32; i++)
    {
      Assert.assertTrue(sem.tryAcquire());
    }
    Assert.assertEquals(0, sem.availablePermits());

    Assert.assertFalse(sem.tryAcquire());
    
    Assert.assertEquals(0, sem.availablePermits());

    sem.release();
    Assert.assertEquals(1, sem.availablePermits());

  }

  @Test
  public void testExpire()
    throws Exception
  {
    TimeSem sem = new TimeSem(16, 25);

    for(int i=0; i<16; i++)
    {
      Assert.assertTrue(sem.tryAcquire());
    }
    
    Assert.assertEquals(0, sem.availablePermits());
    Thread.sleep(26);
    Assert.assertEquals(16, sem.availablePermits());


  }

  @Test
  public void testExpirePart()
    throws Exception
  {
    TimeSem sem = new TimeSem(16, 25);

    for(int i=0; i<8; i++)
    {
      Assert.assertTrue(sem.tryAcquire());
    }
    Assert.assertEquals(8, sem.availablePermits());
    Thread.sleep(10);
    Assert.assertEquals(8, sem.availablePermits());

    for(int i=0; i<8; i++)
    {
      Assert.assertTrue(sem.tryAcquire());
    }
    
    Assert.assertEquals(0, sem.availablePermits());
    Thread.sleep(16);
    Assert.assertEquals(8, sem.availablePermits());
  }

  @Test
  public void testExpireRelease()
    throws Exception
  {
    TimeSem sem = new TimeSem(16, 25);

    for(int i=0; i<8; i++)
    {
      Assert.assertTrue(sem.tryAcquire());
    }
    Assert.assertEquals(8, sem.availablePermits());
    Thread.sleep(10);
    Assert.assertEquals(8, sem.availablePermits());

    for(int i=0; i<8; i++)
    {
      Assert.assertTrue(sem.tryAcquire());
    }
    
    Assert.assertEquals(0, sem.availablePermits());

    sem.release(8); // Release the older ones
    Thread.sleep(16);

    // newer ones are still holding
    Assert.assertEquals(8, sem.availablePermits());
  }

  @Test
  public void noExceedMax()
    throws Exception
  {
    TimeSem sem = new TimeSem(8, 25);

    for(int i=0; i<8; i++)
    {
      Assert.assertTrue(sem.tryAcquire());
    }
    Thread.sleep(30);
    sem.release(8);
    Assert.assertEquals(8, sem.availablePermits());

  }






}
