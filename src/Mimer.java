package snowblossom.channels;

public class Mimer
{
  public static String guessContentType(String path)
  {
    path = path.toLowerCase();
    if (path.endsWith(".ez")) return "application/andrew-inset";
    if (path.endsWith(".anx")) return "application/annodex";
    if (path.endsWith(".atom")) return "application/atom+xml";
    if (path.endsWith(".atomcat")) return "application/atomcat+xml";
    if (path.endsWith(".atomsrv")) return "application/atomserv+xml";
    if (path.endsWith(".lin")) return "application/bbolin";
    if (path.endsWith(".cu")) return "application/cu-seeme";
    if (path.endsWith(".davmount")) return "application/davmount+xml";
    if (path.endsWith(".dcm")) return "application/dicom";
    if (path.endsWith(".tsp")) return "application/dsptype";
    if (path.endsWith(".es")) return "application/ecmascript";
    if (path.endsWith(".otf")) return "application/font-sfnt";
    if (path.endsWith(".ttf")) return "application/font-sfnt";
    if (path.endsWith(".pfr")) return "application/font-tdpfr";
    if (path.endsWith(".woff")) return "application/font-woff";
    if (path.endsWith(".spl")) return "application/futuresplash";
    if (path.endsWith(".gz")) return "application/gzip";
    if (path.endsWith(".hta")) return "application/hta";
    if (path.endsWith(".jar")) return "application/java-archive";
    if (path.endsWith(".ser")) return "application/java-serialized-object";
    if (path.endsWith(".class")) return "application/java-vm";
    if (path.endsWith(".js")) return "application/javascript";
    if (path.endsWith(".json")) return "application/json";
    if (path.endsWith(".m3g")) return "application/m3g";
    if (path.endsWith(".hqx")) return "application/mac-binhex40";
    if (path.endsWith(".cpt")) return "application/mac-compactpro";
    if (path.endsWith(".nb")) return "application/mathematica";
    if (path.endsWith(".nbp")) return "application/mathematica";
    if (path.endsWith(".mbox")) return "application/mbox";
    if (path.endsWith(".mdb")) return "application/msaccess";
    if (path.endsWith(".doc")) return "application/msword";
    if (path.endsWith(".dot")) return "application/msword";
    if (path.endsWith(".mxf")) return "application/mxf";
    if (path.endsWith(".bin")) return "application/octet-stream";
    if (path.endsWith(".deploy")) return "application/octet-stream";
    if (path.endsWith(".msu")) return "application/octet-stream";
    if (path.endsWith(".msp")) return "application/octet-stream";
    if (path.endsWith(".oda")) return "application/oda";
    if (path.endsWith(".opf")) return "application/oebps-package+xml";
    if (path.endsWith(".ogx")) return "application/ogg";
    if (path.endsWith(".one")) return "application/onenote";
    if (path.endsWith(".onetoc2")) return "application/onenote";
    if (path.endsWith(".onetmp")) return "application/onenote";
    if (path.endsWith(".onepkg")) return "application/onenote";
    if (path.endsWith(".pdf")) return "application/pdf";
    if (path.endsWith(".pgp")) return "application/pgp-encrypted";
    if (path.endsWith(".key")) return "application/pgp-keys";
    if (path.endsWith(".sig")) return "application/pgp-signature";
    if (path.endsWith(".prf")) return "application/pics-rules";
    if (path.endsWith(".ps")) return "application/postscript";
    if (path.endsWith(".ai")) return "application/postscript";
    if (path.endsWith(".eps")) return "application/postscript";
    if (path.endsWith(".epsi")) return "application/postscript";
    if (path.endsWith(".epsf")) return "application/postscript";
    if (path.endsWith(".eps2")) return "application/postscript";
    if (path.endsWith(".eps3")) return "application/postscript";
    if (path.endsWith(".rar")) return "application/rar";
    if (path.endsWith(".rdf")) return "application/rdf+xml";
    if (path.endsWith(".rtf")) return "application/rtf";
    if (path.endsWith(".stl")) return "application/sla";
    if (path.endsWith(".smi")) return "application/smil+xml";
    if (path.endsWith(".smil")) return "application/smil+xml";
    if (path.endsWith(".xhtml")) return "application/xhtml+xml";
    if (path.endsWith(".xht")) return "application/xhtml+xml";
    if (path.endsWith(".xml")) return "application/xml";
    if (path.endsWith(".xsd")) return "application/xml";
    if (path.endsWith(".xsl")) return "application/xslt+xml";
    if (path.endsWith(".xslt")) return "application/xslt+xml";
    if (path.endsWith(".xspf")) return "application/xspf+xml";
    if (path.endsWith(".zip")) return "application/zip";
    if (path.endsWith(".apk")) return "application/vnd.android.package-archive";
    if (path.endsWith(".cdy")) return "application/vnd.cinderella";
    if (path.endsWith(".deb")) return "application/vnd.debian.binary-package";
    if (path.endsWith(".ddeb")) return "application/vnd.debian.binary-package";
    if (path.endsWith(".udeb")) return "application/vnd.debian.binary-package";
    if (path.endsWith(".sfd")) return "application/vnd.font-fontforge-sfd";
    if (path.endsWith(".kml")) return "application/vnd.google-earth.kml+xml";
    if (path.endsWith(".kmz")) return "application/vnd.google-earth.kmz";
    if (path.endsWith(".xul")) return "application/vnd.mozilla.xul+xml";
    if (path.endsWith(".xls")) return "application/vnd.ms-excel";
    if (path.endsWith(".xlb")) return "application/vnd.ms-excel";
    if (path.endsWith(".xlt")) return "application/vnd.ms-excel";
    if (path.endsWith(".xlam")) return "application/vnd.ms-excel.addin.macroEnabled.12";
    if (path.endsWith(".xlsb")) return "application/vnd.ms-excel.sheet.binary.macroEnabled.12";
    if (path.endsWith(".xlsm")) return "application/vnd.ms-excel.sheet.macroEnabled.12";
    if (path.endsWith(".xltm")) return "application/vnd.ms-excel.template.macroEnabled.12";
    if (path.endsWith(".eot")) return "application/vnd.ms-fontobject";
    if (path.endsWith(".thmx")) return "application/vnd.ms-officetheme";
    if (path.endsWith(".cat")) return "application/vnd.ms-pki.seccat";
    if (path.endsWith(".ppt")) return "application/vnd.ms-powerpoint";
    if (path.endsWith(".pps")) return "application/vnd.ms-powerpoint";
    if (path.endsWith(".ppam")) return "application/vnd.ms-powerpoint.addin.macroEnabled.12";
    if (path.endsWith(".pptm")) return "application/vnd.ms-powerpoint.presentation.macroEnabled.12";
    if (path.endsWith(".sldm")) return "application/vnd.ms-powerpoint.slide.macroEnabled.12";
    if (path.endsWith(".ppsm")) return "application/vnd.ms-powerpoint.slideshow.macroEnabled.12";
    if (path.endsWith(".potm")) return "application/vnd.ms-powerpoint.template.macroEnabled.12";
    if (path.endsWith(".docm")) return "application/vnd.ms-word.document.macroEnabled.12";
    if (path.endsWith(".dotm")) return "application/vnd.ms-word.template.macroEnabled.12";
    if (path.endsWith(".odc")) return "application/vnd.oasis.opendocument.chart";
    if (path.endsWith(".odb")) return "application/vnd.oasis.opendocument.database";
    if (path.endsWith(".odf")) return "application/vnd.oasis.opendocument.formula";
    if (path.endsWith(".odg")) return "application/vnd.oasis.opendocument.graphics";
    if (path.endsWith(".otg")) return "application/vnd.oasis.opendocument.graphics-template";
    if (path.endsWith(".odi")) return "application/vnd.oasis.opendocument.image";
    if (path.endsWith(".odp")) return "application/vnd.oasis.opendocument.presentation";
    if (path.endsWith(".otp")) return "application/vnd.oasis.opendocument.presentation-template";
    if (path.endsWith(".ods")) return "application/vnd.oasis.opendocument.spreadsheet";
    if (path.endsWith(".ots")) return "application/vnd.oasis.opendocument.spreadsheet-template";
    if (path.endsWith(".odt")) return "application/vnd.oasis.opendocument.text";
    if (path.endsWith(".odm")) return "application/vnd.oasis.opendocument.text-master";
    if (path.endsWith(".ott")) return "application/vnd.oasis.opendocument.text-template";
    if (path.endsWith(".oth")) return "application/vnd.oasis.opendocument.text-web";
    if (path.endsWith(".pptx")) return "application/vnd.openxmlformats-officedocument.presentationml.presentation";
    if (path.endsWith(".sldx")) return "application/vnd.openxmlformats-officedocument.presentationml.slide";
    if (path.endsWith(".ppsx")) return "application/vnd.openxmlformats-officedocument.presentationml.slideshow";
    if (path.endsWith(".potx")) return "application/vnd.openxmlformats-officedocument.presentationml.template";
    if (path.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    if (path.endsWith(".xltx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.template";
    if (path.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    if (path.endsWith(".dotx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.template";
    if (path.endsWith(".cod")) return "application/vnd.rim.cod";
    if (path.endsWith(".mmf")) return "application/vnd.smaf";
    if (path.endsWith(".sdc")) return "application/vnd.stardivision.calc";
    if (path.endsWith(".sds")) return "application/vnd.stardivision.chart";
    if (path.endsWith(".sda")) return "application/vnd.stardivision.draw";
    if (path.endsWith(".sdd")) return "application/vnd.stardivision.impress";
    if (path.endsWith(".sdf")) return "application/vnd.stardivision.math";
    if (path.endsWith(".sdw")) return "application/vnd.stardivision.writer";
    if (path.endsWith(".sgl")) return "application/vnd.stardivision.writer-global";
    if (path.endsWith(".sxc")) return "application/vnd.sun.xml.calc";
    if (path.endsWith(".stc")) return "application/vnd.sun.xml.calc.template";
    if (path.endsWith(".sxd")) return "application/vnd.sun.xml.draw";
    if (path.endsWith(".std")) return "application/vnd.sun.xml.draw.template";
    if (path.endsWith(".sxi")) return "application/vnd.sun.xml.impress";
    if (path.endsWith(".sti")) return "application/vnd.sun.xml.impress.template";
    if (path.endsWith(".sxm")) return "application/vnd.sun.xml.math";
    if (path.endsWith(".sxw")) return "application/vnd.sun.xml.writer";
    if (path.endsWith(".sxg")) return "application/vnd.sun.xml.writer.global";
    if (path.endsWith(".stw")) return "application/vnd.sun.xml.writer.template";
    if (path.endsWith(".sis")) return "application/vnd.symbian.install";
    if (path.endsWith(".cap")) return "application/vnd.tcpdump.pcap";
    if (path.endsWith(".pcap")) return "application/vnd.tcpdump.pcap";
    if (path.endsWith(".vsd")) return "application/vnd.visio";
    if (path.endsWith(".vst")) return "application/vnd.visio";
    if (path.endsWith(".vsw")) return "application/vnd.visio";
    if (path.endsWith(".vss")) return "application/vnd.visio";
    if (path.endsWith(".wbxml")) return "application/vnd.wap.wbxml";
    if (path.endsWith(".wmlc")) return "application/vnd.wap.wmlc";
    if (path.endsWith(".wmlsc")) return "application/vnd.wap.wmlscriptc";
    if (path.endsWith(".wpd")) return "application/vnd.wordperfect";
    if (path.endsWith(".wp5")) return "application/vnd.wordperfect5.1";
    if (path.endsWith(".wk")) return "application/x-123";
    if (path.endsWith(".7z")) return "application/x-7z-compressed";
    if (path.endsWith(".abw")) return "application/x-abiword";
    if (path.endsWith(".dmg")) return "application/x-apple-diskimage";
    if (path.endsWith(".bcpio")) return "application/x-bcpio";
    if (path.endsWith(".torrent")) return "application/x-bittorrent";
    if (path.endsWith(".cab")) return "application/x-cab";
    if (path.endsWith(".cbr")) return "application/x-cbr";
    if (path.endsWith(".cbz")) return "application/x-cbz";
    if (path.endsWith(".cdf")) return "application/x-cdf";
    if (path.endsWith(".cda")) return "application/x-cdf";
    if (path.endsWith(".vcd")) return "application/x-cdlink";
    if (path.endsWith(".pgn")) return "application/x-chess-pgn";
    if (path.endsWith(".mph")) return "application/x-comsol";
    if (path.endsWith(".cpio")) return "application/x-cpio";
    if (path.endsWith(".csh")) return "application/x-csh";
    if (path.endsWith(".deb")) return "application/x-debian-package";
    if (path.endsWith(".udeb")) return "application/x-debian-package";
    if (path.endsWith(".dcr")) return "application/x-director";
    if (path.endsWith(".dir")) return "application/x-director";
    if (path.endsWith(".dxr")) return "application/x-director";
    if (path.endsWith(".dms")) return "application/x-dms";
    if (path.endsWith(".wad")) return "application/x-doom";
    if (path.endsWith(".dvi")) return "application/x-dvi";
    if (path.endsWith(".pfa")) return "application/x-font";
    if (path.endsWith(".pfb")) return "application/x-font";
    if (path.endsWith(".gsf")) return "application/x-font";
    if (path.endsWith(".pcf")) return "application/x-font-pcf";
    if (path.endsWith(".pcf.Z")) return "application/x-font-pcf";
    if (path.endsWith(".mm")) return "application/x-freemind";
    if (path.endsWith(".spl")) return "application/x-futuresplash";
    if (path.endsWith(".gan")) return "application/x-ganttproject";
    if (path.endsWith(".gnumeric")) return "application/x-gnumeric";
    if (path.endsWith(".sgf")) return "application/x-go-sgf";
    if (path.endsWith(".gcf")) return "application/x-graphing-calculator";
    if (path.endsWith(".gtar")) return "application/x-gtar";
    if (path.endsWith(".tgz")) return "application/x-gtar-compressed";
    if (path.endsWith(".taz")) return "application/x-gtar-compressed";
    if (path.endsWith(".hdf")) return "application/x-hdf";
    if (path.endsWith(".hwp")) return "application/x-hwp";
    if (path.endsWith(".ica")) return "application/x-ica";
    if (path.endsWith(".info")) return "application/x-info";
    if (path.endsWith(".ins")) return "application/x-internet-signup";
    if (path.endsWith(".isp")) return "application/x-internet-signup";
    if (path.endsWith(".iii")) return "application/x-iphone";
    if (path.endsWith(".iso")) return "application/x-iso9660-image";
    if (path.endsWith(".jam")) return "application/x-jam";
    if (path.endsWith(".jnlp")) return "application/x-java-jnlp-file";
    if (path.endsWith(".jmz")) return "application/x-jmol";
    if (path.endsWith(".chrt")) return "application/x-kchart";
    if (path.endsWith(".kil")) return "application/x-killustrator";
    if (path.endsWith(".skp")) return "application/x-koan";
    if (path.endsWith(".skd")) return "application/x-koan";
    if (path.endsWith(".skt")) return "application/x-koan";
    if (path.endsWith(".skm")) return "application/x-koan";
    if (path.endsWith(".kpr")) return "application/x-kpresenter";
    if (path.endsWith(".kpt")) return "application/x-kpresenter";
    if (path.endsWith(".ksp")) return "application/x-kspread";
    if (path.endsWith(".kwd")) return "application/x-kword";
    if (path.endsWith(".kwt")) return "application/x-kword";
    if (path.endsWith(".latex")) return "application/x-latex";
    if (path.endsWith(".lha")) return "application/x-lha";
    if (path.endsWith(".lyx")) return "application/x-lyx";
    if (path.endsWith(".lzh")) return "application/x-lzh";
    if (path.endsWith(".lzx")) return "application/x-lzx";
    if (path.endsWith(".frm")) return "application/x-maker";
    if (path.endsWith(".maker")) return "application/x-maker";
    if (path.endsWith(".frame")) return "application/x-maker";
    if (path.endsWith(".fm")) return "application/x-maker";
    if (path.endsWith(".fb")) return "application/x-maker";
    if (path.endsWith(".book")) return "application/x-maker";
    if (path.endsWith(".fbdoc")) return "application/x-maker";
    if (path.endsWith(".mif")) return "application/x-mif";
    if (path.endsWith(".m3u8")) return "application/x-mpegURL";
    if (path.endsWith(".application")) return "application/x-ms-application";
    if (path.endsWith(".manifest")) return "application/x-ms-manifest";
    if (path.endsWith(".wmd")) return "application/x-ms-wmd";
    if (path.endsWith(".wmz")) return "application/x-ms-wmz";
    if (path.endsWith(".com")) return "application/x-msdos-program";
    if (path.endsWith(".exe")) return "application/x-msdos-program";
    if (path.endsWith(".bat")) return "application/x-msdos-program";
    if (path.endsWith(".dll")) return "application/x-msdos-program";
    if (path.endsWith(".msi")) return "application/x-msi";
    if (path.endsWith(".nc")) return "application/x-netcdf";
    if (path.endsWith(".pac")) return "application/x-ns-proxy-autoconfig";
    if (path.endsWith(".nwc")) return "application/x-nwc";
    if (path.endsWith(".o")) return "application/x-object";
    if (path.endsWith(".oza")) return "application/x-oz-application";
    if (path.endsWith(".p7r")) return "application/x-pkcs7-certreqresp";
    if (path.endsWith(".crl")) return "application/x-pkcs7-crl";
    if (path.endsWith(".pyc")) return "application/x-python-code";
    if (path.endsWith(".pyo")) return "application/x-python-code";
    if (path.endsWith(".qgs")) return "application/x-qgis";
    if (path.endsWith(".shp")) return "application/x-qgis";
    if (path.endsWith(".shx")) return "application/x-qgis";
    if (path.endsWith(".qtl")) return "application/x-quicktimeplayer";
    if (path.endsWith(".rdp")) return "application/x-rdp";
    if (path.endsWith(".rpm")) return "application/x-redhat-package-manager";
    if (path.endsWith(".rss")) return "application/x-rss+xml";
    if (path.endsWith(".rb")) return "application/x-ruby";
    if (path.endsWith(".sci")) return "application/x-scilab";
    if (path.endsWith(".sce")) return "application/x-scilab";
    if (path.endsWith(".xcos")) return "application/x-scilab-xcos";
    if (path.endsWith(".sh")) return "application/x-sh";
    if (path.endsWith(".shar")) return "application/x-shar";
    if (path.endsWith(".swf")) return "application/x-shockwave-flash";
    if (path.endsWith(".swfl")) return "application/x-shockwave-flash";
    if (path.endsWith(".scr")) return "application/x-silverlight";
    if (path.endsWith(".sql")) return "application/x-sql";
    if (path.endsWith(".sit")) return "application/x-stuffit";
    if (path.endsWith(".sitx")) return "application/x-stuffit";
    if (path.endsWith(".sv4cpio")) return "application/x-sv4cpio";
    if (path.endsWith(".sv4crc")) return "application/x-sv4crc";
    if (path.endsWith(".tar")) return "application/x-tar";
    if (path.endsWith(".tcl")) return "application/x-tcl";
    if (path.endsWith(".gf")) return "application/x-tex-gf";
    if (path.endsWith(".pk")) return "application/x-tex-pk";
    if (path.endsWith(".texinfo")) return "application/x-texinfo";
    if (path.endsWith(".texi")) return "application/x-texinfo";
    if (path.endsWith(".~")) return "application/x-trash";
    if (path.endsWith(".%")) return "application/x-trash";
    if (path.endsWith(".bak")) return "application/x-trash";
    if (path.endsWith(".old")) return "application/x-trash";
    if (path.endsWith(".sik")) return "application/x-trash";
    if (path.endsWith(".t")) return "application/x-troff";
    if (path.endsWith(".tr")) return "application/x-troff";
    if (path.endsWith(".roff")) return "application/x-troff";
    if (path.endsWith(".man")) return "application/x-troff-man";
    if (path.endsWith(".me")) return "application/x-troff-me";
    if (path.endsWith(".ms")) return "application/x-troff-ms";
    if (path.endsWith(".ustar")) return "application/x-ustar";
    if (path.endsWith(".src")) return "application/x-wais-source";
    if (path.endsWith(".wz")) return "application/x-wingz";
    if (path.endsWith(".crt")) return "application/x-x509-ca-cert";
    if (path.endsWith(".xcf")) return "application/x-xcf";
    if (path.endsWith(".fig")) return "application/x-xfig";
    if (path.endsWith(".xpi")) return "application/x-xpinstall";
    if (path.endsWith(".xz")) return "application/x-xz";
    if (path.endsWith(".amr")) return "audio/amr";
    if (path.endsWith(".awb")) return "audio/amr-wb";
    if (path.endsWith(".axa")) return "audio/annodex";
    if (path.endsWith(".au")) return "audio/basic";
    if (path.endsWith(".snd")) return "audio/basic";
    if (path.endsWith(".csd")) return "audio/csound";
    if (path.endsWith(".orc")) return "audio/csound";
    if (path.endsWith(".sco")) return "audio/csound";
    if (path.endsWith(".flac")) return "audio/flac";
    if (path.endsWith(".mid")) return "audio/midi";
    if (path.endsWith(".midi")) return "audio/midi";
    if (path.endsWith(".kar")) return "audio/midi";
    if (path.endsWith(".mpga")) return "audio/mpeg";
    if (path.endsWith(".mpega")) return "audio/mpeg";
    if (path.endsWith(".mp2")) return "audio/mpeg";
    if (path.endsWith(".mp3")) return "audio/mpeg";
    if (path.endsWith(".m4a")) return "audio/mpeg";
    if (path.endsWith(".m3u")) return "audio/mpegurl";
    if (path.endsWith(".oga")) return "audio/ogg";
    if (path.endsWith(".ogg")) return "audio/ogg";
    if (path.endsWith(".opus")) return "audio/ogg";
    if (path.endsWith(".spx")) return "audio/ogg";
    if (path.endsWith(".sid")) return "audio/prs.sid";
    if (path.endsWith(".aif")) return "audio/x-aiff";
    if (path.endsWith(".aiff")) return "audio/x-aiff";
    if (path.endsWith(".aifc")) return "audio/x-aiff";
    if (path.endsWith(".gsm")) return "audio/x-gsm";
    if (path.endsWith(".m3u")) return "audio/x-mpegurl";
    if (path.endsWith(".wma")) return "audio/x-ms-wma";
    if (path.endsWith(".wax")) return "audio/x-ms-wax";
    if (path.endsWith(".ra")) return "audio/x-pn-realaudio";
    if (path.endsWith(".rm")) return "audio/x-pn-realaudio";
    if (path.endsWith(".ram")) return "audio/x-pn-realaudio";
    if (path.endsWith(".ra")) return "audio/x-realaudio";
    if (path.endsWith(".pls")) return "audio/x-scpls";
    if (path.endsWith(".sd2")) return "audio/x-sd2";
    if (path.endsWith(".wav")) return "audio/x-wav";
    if (path.endsWith(".alc")) return "chemical/x-alchemy";
    if (path.endsWith(".cac")) return "chemical/x-cache";
    if (path.endsWith(".cache")) return "chemical/x-cache";
    if (path.endsWith(".csf")) return "chemical/x-cache-csf";
    if (path.endsWith(".cbin")) return "chemical/x-cactvs-binary";
    if (path.endsWith(".cascii")) return "chemical/x-cactvs-binary";
    if (path.endsWith(".ctab")) return "chemical/x-cactvs-binary";
    if (path.endsWith(".cdx")) return "chemical/x-cdx";
    if (path.endsWith(".cer")) return "chemical/x-cerius";
    if (path.endsWith(".c3d")) return "chemical/x-chem3d";
    if (path.endsWith(".chm")) return "chemical/x-chemdraw";
    if (path.endsWith(".cif")) return "chemical/x-cif";
    if (path.endsWith(".cmdf")) return "chemical/x-cmdf";
    if (path.endsWith(".cml")) return "chemical/x-cml";
    if (path.endsWith(".cpa")) return "chemical/x-compass";
    if (path.endsWith(".bsd")) return "chemical/x-crossfire";
    if (path.endsWith(".csml")) return "chemical/x-csml";
    if (path.endsWith(".csm")) return "chemical/x-csml";
    if (path.endsWith(".ctx")) return "chemical/x-ctx";
    if (path.endsWith(".cxf")) return "chemical/x-cxf";
    if (path.endsWith(".cef")) return "chemical/x-cxf";
    if (path.endsWith(".emb")) return "chemical/x-embl-dl-nucleotide";
    if (path.endsWith(".embl")) return "chemical/x-embl-dl-nucleotide";
    if (path.endsWith(".spc")) return "chemical/x-galactic-spc";
    if (path.endsWith(".inp")) return "chemical/x-gamess-input";
    if (path.endsWith(".gam")) return "chemical/x-gamess-input";
    if (path.endsWith(".gamin")) return "chemical/x-gamess-input";
    if (path.endsWith(".fch")) return "chemical/x-gaussian-checkpoint";
    if (path.endsWith(".fchk")) return "chemical/x-gaussian-checkpoint";
    if (path.endsWith(".cub")) return "chemical/x-gaussian-cube";
    if (path.endsWith(".gau")) return "chemical/x-gaussian-input";
    if (path.endsWith(".gjc")) return "chemical/x-gaussian-input";
    if (path.endsWith(".gjf")) return "chemical/x-gaussian-input";
    if (path.endsWith(".gal")) return "chemical/x-gaussian-log";
    if (path.endsWith(".gcg")) return "chemical/x-gcg8-sequence";
    if (path.endsWith(".gen")) return "chemical/x-genbank";
    if (path.endsWith(".hin")) return "chemical/x-hin";
    if (path.endsWith(".istr")) return "chemical/x-isostar";
    if (path.endsWith(".ist")) return "chemical/x-isostar";
    if (path.endsWith(".jdx")) return "chemical/x-jcamp-dx";
    if (path.endsWith(".dx")) return "chemical/x-jcamp-dx";
    if (path.endsWith(".kin")) return "chemical/x-kinemage";
    if (path.endsWith(".mcm")) return "chemical/x-macmolecule";
    if (path.endsWith(".mmd")) return "chemical/x-macromodel-input";
    if (path.endsWith(".mmod")) return "chemical/x-macromodel-input";
    if (path.endsWith(".mol")) return "chemical/x-mdl-molfile";
    if (path.endsWith(".rd")) return "chemical/x-mdl-rdfile";
    if (path.endsWith(".rxn")) return "chemical/x-mdl-rxnfile";
    if (path.endsWith(".sd")) return "chemical/x-mdl-sdfile";
    if (path.endsWith(".sdf")) return "chemical/x-mdl-sdfile";
    if (path.endsWith(".tgf")) return "chemical/x-mdl-tgf";
    if (path.endsWith(".mcif")) return "chemical/x-mmcif";
    if (path.endsWith(".mol2")) return "chemical/x-mol2";
    if (path.endsWith(".b")) return "chemical/x-molconn-Z";
    if (path.endsWith(".gpt")) return "chemical/x-mopac-graph";
    if (path.endsWith(".mop")) return "chemical/x-mopac-input";
    if (path.endsWith(".mopcrt")) return "chemical/x-mopac-input";
    if (path.endsWith(".mpc")) return "chemical/x-mopac-input";
    if (path.endsWith(".zmt")) return "chemical/x-mopac-input";
    if (path.endsWith(".moo")) return "chemical/x-mopac-out";
    if (path.endsWith(".mvb")) return "chemical/x-mopac-vib";
    if (path.endsWith(".asn")) return "chemical/x-ncbi-asn1";
    if (path.endsWith(".prt")) return "chemical/x-ncbi-asn1-ascii";
    if (path.endsWith(".ent")) return "chemical/x-ncbi-asn1-ascii";
    if (path.endsWith(".val")) return "chemical/x-ncbi-asn1-binary";
    if (path.endsWith(".aso")) return "chemical/x-ncbi-asn1-binary";
    if (path.endsWith(".asn")) return "chemical/x-ncbi-asn1-spec";
    if (path.endsWith(".pdb")) return "chemical/x-pdb";
    if (path.endsWith(".ent")) return "chemical/x-pdb";
    if (path.endsWith(".ros")) return "chemical/x-rosdal";
    if (path.endsWith(".sw")) return "chemical/x-swissprot";
    if (path.endsWith(".vms")) return "chemical/x-vamas-iso14976";
    if (path.endsWith(".vmd")) return "chemical/x-vmd";
    if (path.endsWith(".xtel")) return "chemical/x-xtel";
    if (path.endsWith(".xyz")) return "chemical/x-xyz";
    if (path.endsWith(".gif")) return "image/gif";
    if (path.endsWith(".ief")) return "image/ief";
    if (path.endsWith(".jp2")) return "image/jp2";
    if (path.endsWith(".jpg2")) return "image/jp2";
    if (path.endsWith(".jpeg")) return "image/jpeg";
    if (path.endsWith(".jpg")) return "image/jpeg";
    if (path.endsWith(".jpe")) return "image/jpeg";
    if (path.endsWith(".jpm")) return "image/jpm";
    if (path.endsWith(".jpx")) return "image/jpx";
    if (path.endsWith(".jpf")) return "image/jpx";
    if (path.endsWith(".pcx")) return "image/pcx";
    if (path.endsWith(".png")) return "image/png";
    if (path.endsWith(".svg")) return "image/svg+xml";
    if (path.endsWith(".svgz")) return "image/svg+xml";
    if (path.endsWith(".tiff")) return "image/tiff";
    if (path.endsWith(".tif")) return "image/tiff";
    if (path.endsWith(".djvu")) return "image/vnd.djvu";
    if (path.endsWith(".djv")) return "image/vnd.djvu";
    if (path.endsWith(".ico")) return "image/vnd.microsoft.icon";
    if (path.endsWith(".wbmp")) return "image/vnd.wap.wbmp";
    if (path.endsWith(".cr2")) return "image/x-canon-cr2";
    if (path.endsWith(".crw")) return "image/x-canon-crw";
    if (path.endsWith(".ras")) return "image/x-cmu-raster";
    if (path.endsWith(".cdr")) return "image/x-coreldraw";
    if (path.endsWith(".pat")) return "image/x-coreldrawpattern";
    if (path.endsWith(".cdt")) return "image/x-coreldrawtemplate";
    if (path.endsWith(".cpt")) return "image/x-corelphotopaint";
    if (path.endsWith(".erf")) return "image/x-epson-erf";
    if (path.endsWith(".art")) return "image/x-jg";
    if (path.endsWith(".jng")) return "image/x-jng";
    if (path.endsWith(".bmp")) return "image/x-ms-bmp";
    if (path.endsWith(".nef")) return "image/x-nikon-nef";
    if (path.endsWith(".orf")) return "image/x-olympus-orf";
    if (path.endsWith(".psd")) return "image/x-photoshop";
    if (path.endsWith(".pnm")) return "image/x-portable-anymap";
    if (path.endsWith(".pbm")) return "image/x-portable-bitmap";
    if (path.endsWith(".pgm")) return "image/x-portable-graymap";
    if (path.endsWith(".ppm")) return "image/x-portable-pixmap";
    if (path.endsWith(".rgb")) return "image/x-rgb";
    if (path.endsWith(".xbm")) return "image/x-xbitmap";
    if (path.endsWith(".xpm")) return "image/x-xpixmap";
    if (path.endsWith(".xwd")) return "image/x-xwindowdump";
    if (path.endsWith(".eml")) return "message/rfc822";
    if (path.endsWith(".igs")) return "model/iges";
    if (path.endsWith(".iges")) return "model/iges";
    if (path.endsWith(".msh")) return "model/mesh";
    if (path.endsWith(".mesh")) return "model/mesh";
    if (path.endsWith(".silo")) return "model/mesh";
    if (path.endsWith(".wrl")) return "model/vrml";
    if (path.endsWith(".vrml")) return "model/vrml";
    if (path.endsWith(".x3dv")) return "model/x3d+vrml";
    if (path.endsWith(".x3d")) return "model/x3d+xml";
    if (path.endsWith(".x3db")) return "model/x3d+binary";
    if (path.endsWith(".appcache")) return "text/cache-manifest";
    if (path.endsWith(".ics")) return "text/calendar";
    if (path.endsWith(".icz")) return "text/calendar";
    if (path.endsWith(".css")) return "text/css";
    if (path.endsWith(".csv")) return "text/csv";
    if (path.endsWith(".323")) return "text/h323";
    if (path.endsWith(".html")) return "text/html";
    if (path.endsWith(".htm")) return "text/html";
    if (path.endsWith(".shtml")) return "text/html";
    if (path.endsWith(".uls")) return "text/iuls";
    if (path.endsWith(".mml")) return "text/mathml";
    if (path.endsWith(".md")) return "text/markdown";
    if (path.endsWith(".markdown")) return "text/markdown";
    if (path.endsWith(".asc")) return "text/plain";
    if (path.endsWith(".txt")) return "text/plain";
    if (path.endsWith(".text")) return "text/plain";
    if (path.endsWith(".pot")) return "text/plain";
    if (path.endsWith(".brf")) return "text/plain";
    if (path.endsWith(".srt")) return "text/plain";
    if (path.endsWith(".rtx")) return "text/richtext";
    if (path.endsWith(".sct")) return "text/scriptlet";
    if (path.endsWith(".wsc")) return "text/scriptlet";
    if (path.endsWith(".tm")) return "text/texmacs";
    if (path.endsWith(".tsv")) return "text/tab-separated-values";
    if (path.endsWith(".ttl")) return "text/turtle";
    if (path.endsWith(".vcf")) return "text/vcard";
    if (path.endsWith(".vcard")) return "text/vcard";
    if (path.endsWith(".jad")) return "text/vnd.sun.j2me.app-descriptor";
    if (path.endsWith(".wml")) return "text/vnd.wap.wml";
    if (path.endsWith(".wmls")) return "text/vnd.wap.wmlscript";
    if (path.endsWith(".bib")) return "text/x-bibtex";
    if (path.endsWith(".boo")) return "text/x-boo";
    if (path.endsWith(".h++")) return "text/x-c++hdr";
    if (path.endsWith(".hpp")) return "text/x-c++hdr";
    if (path.endsWith(".hxx")) return "text/x-c++hdr";
    if (path.endsWith(".hh")) return "text/x-c++hdr";
    if (path.endsWith(".c++")) return "text/x-c++src";
    if (path.endsWith(".cpp")) return "text/x-c++src";
    if (path.endsWith(".cxx")) return "text/x-c++src";
    if (path.endsWith(".cc")) return "text/x-c++src";
    if (path.endsWith(".h")) return "text/x-chdr";
    if (path.endsWith(".htc")) return "text/x-component";
    if (path.endsWith(".csh")) return "text/x-csh";
    if (path.endsWith(".c")) return "text/x-csrc";
    if (path.endsWith(".d")) return "text/x-dsrc";
    if (path.endsWith(".diff")) return "text/x-diff";
    if (path.endsWith(".patch")) return "text/x-diff";
    if (path.endsWith(".hs")) return "text/x-haskell";
    if (path.endsWith(".java")) return "text/x-java";
    if (path.endsWith(".ly")) return "text/x-lilypond";
    if (path.endsWith(".lhs")) return "text/x-literate-haskell";
    if (path.endsWith(".moc")) return "text/x-moc";
    if (path.endsWith(".p")) return "text/x-pascal";
    if (path.endsWith(".pas")) return "text/x-pascal";
    if (path.endsWith(".gcd")) return "text/x-pcs-gcd";
    if (path.endsWith(".pl")) return "text/x-perl";
    if (path.endsWith(".pm")) return "text/x-perl";
    if (path.endsWith(".py")) return "text/x-python";
    if (path.endsWith(".scala")) return "text/x-scala";
    if (path.endsWith(".etx")) return "text/x-setext";
    if (path.endsWith(".sfv")) return "text/x-sfv";
    if (path.endsWith(".sh")) return "text/x-sh";
    if (path.endsWith(".tcl")) return "text/x-tcl";
    if (path.endsWith(".tk")) return "text/x-tcl";
    if (path.endsWith(".tex")) return "text/x-tex";
    if (path.endsWith(".ltx")) return "text/x-tex";
    if (path.endsWith(".sty")) return "text/x-tex";
    if (path.endsWith(".cls")) return "text/x-tex";
    if (path.endsWith(".vcs")) return "text/x-vcalendar";
    if (path.endsWith(".3gp")) return "video/3gpp";
    if (path.endsWith(".axv")) return "video/annodex";
    if (path.endsWith(".dl")) return "video/dl";
    if (path.endsWith(".dif")) return "video/dv";
    if (path.endsWith(".dv")) return "video/dv";
    if (path.endsWith(".fli")) return "video/fli";
    if (path.endsWith(".gl")) return "video/gl";
    if (path.endsWith(".mpeg")) return "video/mpeg";
    if (path.endsWith(".mpg")) return "video/mpeg";
    if (path.endsWith(".mpe")) return "video/mpeg";
    if (path.endsWith(".ts")) return "video/MP2T";
    if (path.endsWith(".mp4")) return "video/mp4";
    if (path.endsWith(".qt")) return "video/quicktime";
    if (path.endsWith(".mov")) return "video/quicktime";
    if (path.endsWith(".ogv")) return "video/ogg";
    if (path.endsWith(".webm")) return "video/webm";
    if (path.endsWith(".mxu")) return "video/vnd.mpegurl";
    if (path.endsWith(".flv")) return "video/x-flv";
    if (path.endsWith(".lsf")) return "video/x-la-asf";
    if (path.endsWith(".lsx")) return "video/x-la-asf";
    if (path.endsWith(".mng")) return "video/x-mng";
    if (path.endsWith(".asf")) return "video/x-ms-asf";
    if (path.endsWith(".asx")) return "video/x-ms-asf";
    if (path.endsWith(".wm")) return "video/x-ms-wm";
    if (path.endsWith(".wmv")) return "video/x-ms-wmv";
    if (path.endsWith(".wmx")) return "video/x-ms-wmx";
    if (path.endsWith(".wvx")) return "video/x-ms-wvx";
    if (path.endsWith(".avi")) return "video/x-msvideo";
    if (path.endsWith(".movie")) return "video/x-sgi-movie";
    if (path.endsWith(".mpv")) return "video/x-matroska";
    if (path.endsWith(".mkv")) return "video/x-matroska";
    if (path.endsWith(".ice")) return "x-conference/x-cooltalk";
    if (path.endsWith(".sisx")) return "x-epoc/x-sisx-app";
    if (path.endsWith(".vrm")) return "x-world/x-vrml";
    if (path.endsWith(".vrml")) return "x-world/x-vrml";
    if (path.endsWith(".wrl")) return "x-world/x-vrml";
    return null;
  }

}




