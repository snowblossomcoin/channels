package snowblossom.channels;

import io.grpc.stub.StreamObserver;

public class NullStreamer<T> implements StreamObserver<T>
{

  @Override
  public void onCompleted(){}

  @Override
  public void onError(Throwable t){}


  @Override
  public void onNext(T t){}


}
