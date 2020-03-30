package snowblossom.channels;

import java.text.DecimalFormat;
import java.util.TreeMap;

import java.io.File;

public class HtmlUtils
{

  public static String getIndex(File dir)
	{
    StringBuilder content=new StringBuilder();

    content.append("<body>");
    content.append(getHeader(dir.getName()));

    TreeMap<String, File> dir_map = new TreeMap<>();
    TreeMap<String, File> file_map = new TreeMap<>();


    for(File f : dir.listFiles())
    {
      if (f.isDirectory())
      {
        dir_map.put(f.getName(), f);
      }
      else
      {
        file_map.put(f.getName(), f);
      }
    }

    if (dir_map.size() > 0)
    {
      content.append("<h4>Directories</h4>\n");
      for(String n : dir_map.keySet())
      {
        content.append(String.format("<li><a href='%s/'>%s</a></li>", n, n));
      }

    }
    if (file_map.size() > 0)
    {
      content.append("<h4>Files</h4>\n");
      for(File sub : file_map.values())
      {
        String name = sub.getName();
        long len = sub.length();
        String pretty_length = getPrettySize(len);
        content.append(String.format("<li><a href='%s'>%s</a> (%s) (%d bytes)</li>", name, name, pretty_length, len));
      }
    }

    content.append("</body></html>");

    return content.toString();

	}

  public static String getHeader(String title)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("<html><meta content='text/html; charset=UTF-8' http-equiv='content-type'>\n");
    sb.append("<head>\n");
    sb.append("<style>\n");
    sb.append("body { background-color: linen; font-family: 'Verdana'; }\n");
    sb.append("li { font-family: 'Courier' }\n");
    sb.append("</style>\n");
    sb.append(String.format("<title>%s</title></head>\n", title));

    return sb.toString();

  }

  public static String getPrettySize(long length)
  {
    double v = length;

    String suffix="B";
    if (Math.abs(v) >= 1000.0) { v /= 1000.0; suffix="KB";}
    if (Math.abs(v) >= 1000.0) { v /= 1000.0; suffix="MB";}
    if (Math.abs(v) >= 1000.0) { v /= 1000.0; suffix="GB";}
    if (Math.abs(v) >= 1000.0) { v /= 1000.0; suffix="TB";}
    if (Math.abs(v) >= 1000.0) { v /= 1000.0; suffix="PB";}
    if (Math.abs(v) >= 1000.0) { v /= 1000.0; suffix="EB";}

    DecimalFormat df = new DecimalFormat("0.00");
    return df.format(v) + " " + suffix;
  }


}
