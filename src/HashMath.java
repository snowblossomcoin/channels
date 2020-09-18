package snowblossom.channels;

import com.google.protobuf.ByteString;
import java.math.BigDecimal;
import java.math.BigInteger;

public class HashMath
{
  public static final ByteString Z = ByteString.copyFrom(new byte[1]);

  public static ByteString getAbsDiff(ByteString a, ByteString b)
  {
    int len = Math.min(a.size(), b.size());
    BigInteger a_bi = new BigInteger(1, a.substring(0, len).toByteArray());
    BigInteger b_bi = new BigInteger(1, b.substring(0, len).toByteArray());

    BigInteger diff_bi = a_bi.subtract(b_bi).abs();
    BigInteger ring_max = getRingMax(len);
    BigInteger half_ring = ring_max.shiftRight(1);

    if (diff_bi.compareTo(half_ring) > 0)
    {
      diff_bi = ring_max.subtract(diff_bi);
    }

    ByteString diff = ByteString.copyFrom(diff_bi.toByteArray());
    while (diff.size() < len)
    {
      diff = Z.concat(diff);
    }

    return diff;
  }

  public static BigInteger getDiff(ByteString a, ByteString b)
  {
    int len = Math.min(a.size(), b.size());
    BigInteger a_bi = new BigInteger(1, a.substring(0, len).toByteArray());
    BigInteger b_bi = new BigInteger(1, b.substring(0, len).toByteArray());

    BigInteger diff_bi = b_bi.subtract(a_bi);
    BigInteger ring_max = getRingMax(len);
    BigInteger half_ring = ring_max.shiftRight(1);

    if (diff_bi.compareTo(half_ring) > 0)
    {
      diff_bi = diff_bi.subtract(ring_max);
    }
    if (diff_bi.negate().compareTo(half_ring) > 0)
    {
      diff_bi = ring_max.add(diff_bi);
    }

    return diff_bi;

  }

  public static ByteString shiftHashOnRing(ByteString start, double movement)
  {
    int len = start.size();

    BigInteger start_bi = getAsInt(start);
    BigInteger ring_max = getRingMax(len);
    BigInteger movement_val =  new BigDecimal(ring_max).multiply(new BigDecimal(movement)).toBigInteger();

    BigInteger new_val = start_bi.add(movement_val).mod(ring_max);

    ByteString val = ByteString.copyFrom(new_val.toByteArray());

    // If the highest bit is one, 2s complement will put an extra
    // byte on the front to not confuse the sign bit. bah.
    if (val.size() > len)
    {
      val = val.substring(1);
    }
    while(val.size() < len)
    {
      val = Z.concat(val);
    }
    return val;
  }

  public static BigInteger getAsInt(ByteString v)
  {
    return new BigInteger(1, v.toByteArray());
  }

  public static BigInteger getRingMax(int len)
  {
    byte[] max_bytes = new byte[len+1];
    max_bytes[0]=1;
    BigInteger ring_max = new BigInteger(1, max_bytes);
    return ring_max;

  }

}
