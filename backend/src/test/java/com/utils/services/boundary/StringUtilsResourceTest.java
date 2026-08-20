package com.utils.services.boundary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.utils.services.control.StringUtils;
import com.utils.services.entity.LengthResponse;
import com.utils.services.entity.ReverseRequest;
import com.utils.services.entity.ReverseResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StringUtilsResourceTest {
  @Mock
  private StringUtils stringUtils;

  private StringUtilsResource resource;

  @BeforeEach
  void setUp() {
    resource = new StringUtilsResource();
    resource.stringUtils = stringUtils;
  }

  @Test
  void reverse_shouldReverseInput() {
    ReverseRequest request = new ReverseRequest("hello");

    when(stringUtils.reverse("hello")).thenReturn("olleh");

    ReverseResponse response = resource.reverse(request);

    assertThat(response).isEqualTo(new ReverseResponse("olleh"));
    verify(stringUtils).reverse("hello");
  }

  @Test
  void reverse_shouldHandleNullRequest() {
    when(stringUtils.reverse(null)).thenReturn(null);

    ReverseResponse response = resource.reverse(null);

    assertThat(response).isEqualTo(new ReverseResponse(null));
    verify(stringUtils).reverse(null);
  }

  @Test
  void reverse_shouldHandleNullInput() {
    ReverseRequest request = new ReverseRequest(null);

    when(stringUtils.reverse(null)).thenReturn(null);

    ReverseResponse response = resource.reverse(request);

    assertThat(response).isEqualTo(new ReverseResponse(null));
    verify(stringUtils).reverse(null);
  }

  @Test
  void length_shouldReturnLength() {
    when(stringUtils.length("hello")).thenReturn(5);

    LengthResponse response = resource.length("hello");

    assertThat(response).isEqualTo(new LengthResponse(5));
    verify(stringUtils).length("hello");
  }

  @Test
  void length_shouldHandleNullString() {
    LengthResponse response = resource.length(null);

    assertThat(response).isEqualTo(new LengthResponse(0));
    verifyNoInteractions(stringUtils);
  }
}
