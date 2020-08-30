package snowblossom.channels;

import com.google.protobuf.util.JsonFormat;
import snowblossom.channels.proto.EncryptedChannelConfig;
import snowblossom.util.proto.Offer;
import snowblossom.util.proto.OfferCurrency;

public class JsonTest
{
  public static void main(String args[]) throws Exception
  {

    JsonFormat.Printer printer = JsonFormat.printer();

    EncryptedChannelConfig.Builder conf = EncryptedChannelConfig.newBuilder();

    conf.setProtectedPath("/prot/");

    Offer.Builder offer = Offer.newBuilder();
    offer.setOfferMode( Offer.OfferMode.FOREVER_ACCESS );
    offer.putOfferPrice("SNOW", OfferCurrency.newBuilder().setPrice(1.0).setAddress("snow:x").build());
    offer.setOfferId("bzzzz");

    conf.setOffer(offer.build());

    System.out.println(printer.print(conf.build() ));



  }

}
