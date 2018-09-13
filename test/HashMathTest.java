package channels;

import org.junit.Test;
import org.junit.Assert;

import snowblossom.lib.HexUtil;
import snowblossom.channels.HashMath;

import com.google.protobuf.ByteString;

public class HashMathTest
{
  @Test
  public void testAbsBasic()
  {
    ByteString a = HexUtil.hexStringToBytes("03000000");
    ByteString b = HexUtil.hexStringToBytes("010000");

    ByteString d1 = HashMath.getAbsDiff(a,b);
    ByteString d2 = HashMath.getAbsDiff(b,a);

    Assert.assertEquals(d1,d2);

    String d = HexUtil.getHexString(d1);
    Assert.assertEquals("020000", d);
  }

  @Test
  public void testAbsBasic2()
  {
    ByteString a = HexUtil.hexStringToBytes("fe000000");
    ByteString b = HexUtil.hexStringToBytes("ee0000");

    ByteString d1 = HashMath.getAbsDiff(a,b);
    ByteString d2 = HashMath.getAbsDiff(b,a);

    Assert.assertEquals(d1,d2);

    String d = HexUtil.getHexString(d1);
    Assert.assertEquals("100000", d);
  }

  @Test
  public void testAbsRoundTheHorn()
  {
    ByteString a = HexUtil.hexStringToBytes("fe000000");
    ByteString b = HexUtil.hexStringToBytes("010000");

    ByteString d1 = HashMath.getAbsDiff(a,b);
    ByteString d2 = HashMath.getAbsDiff(b,a);

    Assert.assertEquals(d1,d2);

    String d = HexUtil.getHexString(d1);
    Assert.assertEquals("030000", d);
  }

  @Test
  public void shiftRingBasic()
  {
    ByteString a = HexUtil.hexStringToBytes("800000");

    ByteString b = HashMath.shiftHashOnRing(a, 0.25);
    ByteString c = HashMath.shiftHashOnRing(a, 0.50);
    ByteString d = HashMath.shiftHashOnRing(a, 0.75);
    ByteString e = HashMath.shiftHashOnRing(a, 1.00);
    ByteString f = HashMath.shiftHashOnRing(a, 1.25);

    Assert.assertEquals("c00000", HexUtil.getHexString(b));
    Assert.assertEquals("000000", HexUtil.getHexString(c));
    Assert.assertEquals("400000", HexUtil.getHexString(d));
    Assert.assertEquals("800000", HexUtil.getHexString(e));
    Assert.assertEquals("c00000", HexUtil.getHexString(f));

  }

   @Test
  public void shiftRingBasicOffset()
  {
    ByteString a = HexUtil.hexStringToBytes("800134");

    ByteString b = HashMath.shiftHashOnRing(a, 0.25);
    ByteString c = HashMath.shiftHashOnRing(a, 0.50);
    ByteString d = HashMath.shiftHashOnRing(a, 0.75);
    ByteString e = HashMath.shiftHashOnRing(a, 1.00);

    Assert.assertEquals("c00134", HexUtil.getHexString(b));
    Assert.assertEquals("000134", HexUtil.getHexString(c));
    Assert.assertEquals("400134", HexUtil.getHexString(d));
    Assert.assertEquals("800134", HexUtil.getHexString(e));


  }

  @Test
  public void shiftRingNegative()
  {
    ByteString a = HexUtil.hexStringToBytes("800000");

    ByteString b = HashMath.shiftHashOnRing(a, -0.25);
    ByteString c = HashMath.shiftHashOnRing(a, -0.50);
    ByteString d = HashMath.shiftHashOnRing(a, -0.75);
    ByteString e = HashMath.shiftHashOnRing(a, -1.00);
    ByteString f = HashMath.shiftHashOnRing(a, -1.25);

    Assert.assertEquals("400000", HexUtil.getHexString(b));
    Assert.assertEquals("000000", HexUtil.getHexString(c));
    Assert.assertEquals("c00000", HexUtil.getHexString(d));
    Assert.assertEquals("800000", HexUtil.getHexString(e));
    Assert.assertEquals("400000", HexUtil.getHexString(f));

  }
  @Test
  public void shiftRingNegativeOffset()
  {
    ByteString a = HexUtil.hexStringToBytes("8000ef");

    ByteString b = HashMath.shiftHashOnRing(a, -0.25);
    ByteString c = HashMath.shiftHashOnRing(a, -0.50);
    ByteString d = HashMath.shiftHashOnRing(a, -0.75);
    ByteString e = HashMath.shiftHashOnRing(a, -1.00);
    ByteString f = HashMath.shiftHashOnRing(a, -1.25);

    Assert.assertEquals("4000ef", HexUtil.getHexString(b));
    Assert.assertEquals("0000ef", HexUtil.getHexString(c));
    Assert.assertEquals("c000ef", HexUtil.getHexString(d));
    Assert.assertEquals("8000ef", HexUtil.getHexString(e));
    Assert.assertEquals("4000ef", HexUtil.getHexString(f));

  }

  @Test
  public void shiftRingHighVal()
  {
    ByteString a = HexUtil.hexStringToBytes("ffffff");
    ByteString b = HashMath.shiftHashOnRing(a, 1.0);
    ByteString c = HashMath.shiftHashOnRing(a, 1.0/16);

    Assert.assertEquals(a,b);

    Assert.assertEquals("0fffff", HexUtil.getHexString(c));

  }



}

