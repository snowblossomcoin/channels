package snowblossom.channels;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Simple counter holder for sharing state of some processing across
 * methods.  Everything threadsafe but not nessesarily super fast.
 * If you are counting over thousand things per second expect some slow down.
 */
public class ProcessStatus
{
  private TreeMap<String, Long> count_map = new TreeMap<>();
  private TreeMap<String, String> data_map = new TreeMap<>();

  public void add(String key)
  {
    add(key, 1L);
  }
  public synchronized void add(String key, long v)
  {
    long curr = count_map.getOrDefault(key, 0L);

    count_map.put(key, curr+v);
  }

  public synchronized void set(String key, String val)
  {
    data_map.put(key, val);
  }

  public synchronized Map<String, Long> getCountMap()
  {
    return ImmutableMap.copyOf(count_map);
  }
  public synchronized Map<String, String> getDataMap()
  {
    return ImmutableMap.copyOf(data_map);
  }



  public String getStatusLine()
  {
    Map<String, Long> m = getCountMap();
    StringBuilder sb = new StringBuilder();

    sb.append("{");

    boolean first=true;
    for(String k : m.keySet())
    {
      if (!first)
      {
        sb.append(" ");
      }
      sb.append(k);
      sb.append("=");
      sb.append(m.get(k));
      first=false;
    }
    Map<String, String> dm = getDataMap();
    for(String k : dm.keySet())
    {
      if (!first)
      {
        sb.append("\n");
      }
      sb.append(k);
      sb.append("=");
      sb.append(dm.get(k));
      first=false;
    }

    sb.append("}");
    return sb.toString();
  }

}

