package com.utils.services.control;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.BadRequestException;

class StringUtilsTest {
  private StringUtils stringUtils;

  @BeforeEach
  void setUp() {
    stringUtils = new StringUtils();
    stringUtils.serviceVersion = "test";
  }

  @Test
  void reversesString() {
    assertThat(stringUtils.reverse("ABC123")).isEqualTo("321CBA");
  }

  @Test
  void reversesSingleCharacter() {
    assertThat(stringUtils.reverse("A")).isEqualTo("A");
  }

  @Test
  void reversesEmptyString() {
    assertThat(stringUtils.reverse("")).isEmpty();
  }

  @Test
  void returnsNullWhenReversingNull() {
    assertThat(stringUtils.reverse(null)).isNull();
  }

  @Test
  void throwsExceptionForErrorInput() {
    assertThatThrownBy(() -> stringUtils.reverse("ERROR")).isInstanceOf(BadRequestException.class).
                                                                  hasMessage("Input cannot be 'ERROR'");
  }

  @Test
  void errorInputIsCaseInsensitive() {
    assertThatThrownBy(() -> stringUtils.reverse("error")).isInstanceOf(BadRequestException.class).
                                                                  hasMessage("Input cannot be 'ERROR'");
  }

  @Test
  void returnsStringLength() {
    assertThat(stringUtils.length("A-B-c")).isEqualTo(5);
  }

  @Test
  void returnsZeroForEmptyStringLength() {
    assertThat(stringUtils.length("")).isZero();
  }

  @Test
  void returnsZeroForNullLength() {
    assertThat(stringUtils.length(null)).isZero();
  }

  @Test
  void returnsSingleCharacterLength() {
    assertThat(stringUtils.length("A")).isEqualTo(1);
  }
}
