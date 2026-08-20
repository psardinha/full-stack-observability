package com.utils.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.utils.services.boundary.StringUtilsClient;
import com.utils.services.entity.LengthResponse;
import com.utils.services.entity.ReverseRequest;
import com.utils.services.entity.ReverseResponse;

import io.quarkus.runtime.StartupEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class LoadGeneratorTest {
  @Mock
  private StringUtilsClient client;

  @Mock
  private StartupEvent startupEvent;

  private LoadGenerator loadGenerator;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    loadGenerator = new LoadGenerator();
    loadGenerator.client = client;
  }

  @Test
  void onStart_shouldDoNothingWhenContinuousLoadIsDisabled() {

      loadGenerator.withContinuousLoad = false;

      loadGenerator.onStart(startupEvent);

      verifyNoInteractions(client);
  }

  @Test
  void generateLoad_shouldCallReverseAndLength() throws Exception {
    when(client.reverse(any(ReverseRequest.class))).thenReturn(mock(ReverseResponse.class));
    when(client.length(any(String.class))).thenReturn(mock(LengthResponse.class));

    Thread thread = Thread.ofVirtual().start(loadGenerator::generateLoad);
    // Give the generator enough time to perform at least one iteration.
    Thread.sleep(100);

    loadGenerator.stop();
    thread.interrupt();
    thread.join(1000);

    verify(client, atLeastOnce()).reverse(any(ReverseRequest.class));
    verify(client, atLeastOnce()).length(any(String.class));
  }

  @Test
  void generateLoad_shouldSendSameStringToReverseAndLength() throws Exception {
    when(client.reverse(any(ReverseRequest.class))).thenReturn(mock(ReverseResponse.class));
    when(client.length(any(String.class))).thenReturn(mock(LengthResponse.class));

    Thread thread = Thread.ofVirtual().start(loadGenerator::generateLoad);
    Thread.sleep(100);

    loadGenerator.stop();
    thread.interrupt();
    thread.join(1000);

    var reverseCaptor = org.mockito.ArgumentCaptor.forClass(ReverseRequest.class);
    var lengthCaptor = org.mockito.ArgumentCaptor.forClass(String.class);

    verify(client, atLeastOnce()).reverse(reverseCaptor.capture());
    verify(client, atLeastOnce()).length(lengthCaptor.capture());

    String reversedInput =reverseCaptor.getValue().input();
    String lengthInput =lengthCaptor.getValue();
    assertThat(lengthInput).isEqualTo(reversedInput);
  }
}