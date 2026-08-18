package com.utils.services;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import com.utils.services.boundary.StringUtilsClient;
import com.utils.services.entity.ReverseRequest;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class LoadGenerator {
  static final int MEAN_SLEEP_TIME = 250;
  static final System.Logger LOGGER = System.getLogger(LoadGenerator.class.getName());

  @Inject
  @RestClient
  StringUtilsClient client;

  @ConfigProperty(name = "make.load", defaultValue = "false")
  boolean withContinuousLoad;
    
  void onStart(@Observes StartupEvent event) {
    if (! withContinuousLoad)
      return;
    Thread.ofVirtual().name("load-generator").start(this::generateLoad);
 }

 private void generateLoad() {
    StringBuilder sb = new StringBuilder();
    while (true) {
      int randomLength = (int) (Math.random() * 31);
      sb.setLength(0);

      for (int j = 0; j < randomLength; j++)
        sb.append((char) (Math.random() * 70 + 'A'));

      try {
        Thread.sleep((long) (2 *  MEAN_SLEEP_TIME * Math.random()));
      } catch (InterruptedException e) {}
      // result discarded
      client.reverse(new ReverseRequest(sb.toString()));
      
      try {
        Thread.sleep((long) (2 *  MEAN_SLEEP_TIME * Math.random()));
      } catch (InterruptedException e) {}
      // result discarded
      client.length(sb.toString());
    }
  }
}
