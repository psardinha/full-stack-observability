package com.utils.services;

import jakarta.inject.Inject;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import com.utils.services.boundary.StringUtilsClient;
import com.utils.services.entity.ReverseRequest;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class StringReverserIT {
  @Inject
  @RestClient
  StringUtilsClient client;

  @Test
  void reversesSimpleString() {
    var response = client.reverse(new ReverseRequest("ABC123"));
    assertThat(response.output()).isEqualTo("321CBA");
  }

  @Test
  void reversesEmptyString() {
    var response = client.reverse(new ReverseRequest(""));
    assertThat(response.output()).isEmpty();
  }

  @Test
  void reversesSingleCharacter() {
    var response = client.reverse(new ReverseRequest("A"));
    assertThat(response.output()).isEqualTo("A");
  }

  @Test
  void returnsStringLength() {
    var response = client.length("A-B-c");
    assertThat(response.length()).isEqualTo(5);
  }

  @Test
  void returnsZeroForEmptyStringLength() {
    var response = client.length("");
    assertThat(response.length()).isEqualTo(0);
  }

  @Test
  void returnsZeroWhenStringQueryParamMissing() {
    var response = client.length(null);
    assertThat(response.length()).isEqualTo(0);
  }

  @Test
  void loadTestForRevertOp() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 25; i++) {
      // Get a random integer from 0 to 20
      int randomLength = (int) (Math.random() * 31);
      sb.setLength(0);
      for (int j = 0; j < randomLength; j++) {
        // Append a random alphanumeric character to the string builder
        char randomChar = (char) (Math.random() * 70 + 'A');
        sb.append(randomChar);
      }
      var response = client.reverse(new ReverseRequest(sb.toString()));
      assertThat(response.output()).isEqualTo(sb.reverse().toString());
    } 
  }

  // Repeat previous test for length function
  @Test
  void loadTestForLengthOp() {    
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 25; i++) {
      // Get a random integer from 0 to 20
      int randomLength = (int) (Math.random() * 31);
      sb.setLength(0);
      for (int j = 0; j < randomLength; j++) {
        // Append a random alphanumeric character to the string builder
        char randomChar = (char) (Math.random() * 70 + 'A');
        sb.append(randomChar);
      }
      var response = client.length(sb.toString());
      assertThat(response.length()).isEqualTo(sb.length());
    } 
  }
}
