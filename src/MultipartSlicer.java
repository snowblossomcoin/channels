package snowblossom.channels;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The apache one seems way to complicated for a simple proposition
 */
public class MultipartSlicer
{
  private InputStream in;

  private static final byte[] clrf_bytes = { 13, 10 };
  private static final ByteString crlf = ByteString.copyFrom(clrf_bytes);

  private final ByteString total_data;
  private final ByteString marker;

  private List<FileData> file_list;

  public MultipartSlicer(InputStream in)
    throws java.io.IOException
  {
    ByteString.Output bout = ByteString.newOutput();

    byte[] buff=new byte[8192];
    while(true)
    {
      int r = in.read(buff);
      if (r < 0) break;

      bout.write(buff, 0, r);
    }
    total_data = bout.toByteString();

    int loc = 0;
    marker = readline(0);
    loc += marker.size() + crlf.size();

    file_list = new ArrayList<>();
    
    while(loc + marker.size() < total_data.size())
    {
      System.out.println("Location: " + loc + " / " + total_data.size());
      loc = readItem(loc);
    }
    file_list = ImmutableList.copyOf(file_list);

  }
  public List<FileData> getFiles()
  {
    return file_list;
  }

  private ByteString readline(int start_idx)
  {
    int end = findNext(start_idx, crlf);
    return total_data.substring(start_idx, end);

  }
  private int readItem(int loc)
  {
    TreeMap<String, String> meta = new TreeMap<>();
    while(true)
    {
      ByteString line = readline(loc);
      loc += line.size() + crlf.size();
      if (line.size() == 0) break;

      extractMeta(meta, line);
    }
    int data_end = findNext(loc, crlf.concat(marker));
    ByteString file_data = total_data.substring(loc, data_end);
    System.out.println("File size: " + file_data.size());
    loc += file_data.size() + marker.size() + 2 * crlf.size();

    file_list.add(new FileData(meta, file_data));
    return loc;
  }

  private int findNext(int start_idx, ByteString search)
  {
    for(int i = start_idx; i<total_data.size(); i++)
    {
      boolean match=true;
      for(int j=0; j<search.size(); j++)
      {
        if (i + j >= total_data.size()) return -1;

        if (total_data.byteAt(i+j) != search.byteAt(j))
        {
          match=false;
          break;
        }
      }
      if (match) return i;
    }
    return -1;

  }

  private void extractMeta(Map<String, String> meta, ByteString line_bs)
  {
    String line = new String(line_bs.toByteArray());
    int colon = line.indexOf(':');
    String key = line.substring(0, colon).trim();
    String value = line.substring(colon+1).trim();

    meta.put(key, value);

    if (key.equals("Content-Disposition"))
    {
      String[] parts = value.split(";");
      for(String part :parts)
      {
        part = part.trim();
        int eq = part.indexOf('=');
        if (eq > 0)
        {
          String key_part = part.substring(0, eq);
          String val_part = part.substring(eq+1).replace('"',' ').trim();
          meta.put(key_part, val_part);

        }

      }
    }

  }


  public class FileData
  {
    public final Map<String, String> meta;
    public final ByteString file_data;

    public FileData(Map<String, String> meta, ByteString file_data)
    {
      this.meta = ImmutableMap.copyOf(meta);
      this.file_data = file_data;
    }

  }

  


}
