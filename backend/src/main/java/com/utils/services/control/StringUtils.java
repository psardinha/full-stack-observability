package com.utils.services.control;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.utils.services.Control;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.trace.Span;
import jakarta.ws.rs.BadRequestException;

@Control
public class StringUtils {
  static final System.Logger LOGGER = System.getLogger(StringUtils.class.getName());
  static final AttributeKey<String> FUNCTION_ATTRIBUTE = AttributeKey.stringKey("function");
  static final AttributeKey<String> SERVICE_VERSION_ATTRIBUTE = AttributeKey.stringKey("service.version");

  @ConfigProperty(name = "quarkus.application.version", defaultValue = "unknown")
  String serviceVersion;

  private LongHistogram replyTimeHistogram = GlobalOpenTelemetry.getMeter("stringUtils.string-utils").
                                                                 histogramBuilder("stringUtils.reply.time").
                                                                 ofLongs().
                                                                 setUnit("ms").
                                                                 setDescription("Reply time for StringUtils operations").
                                                                 build();

  /** 
   * Introduces a simulated processing delay and optionally adds a simulated * peak delay with a 5% probability. 
   * @param probOfPeak whether the additional peak delay should be considered
   * @return true, if the simulated peak delay was applied;
   *         false, otherwise
   */
  private boolean replyTimeTooLong (boolean probOfPeak) {
    long PEAK_SLEEP_TIME = 500,
         MEAN_SLEEP_TIME = 200;
    long interval = probOfPeak && Math.random() < 0.05 ? PEAK_SLEEP_TIME : 0;
    try {
      Thread.sleep(interval + (long) (2 *  MEAN_SLEEP_TIME * Math.random()));
    } catch (InterruptedException e) {}
    return interval > 0;
  }

  /** 
   * Reverses the characters in the supplied string. First character becomes last, second becomes penultimate, etc.
   * Example: "ABC123" becomes "321CBA". If the input is null or empty, it is returned unchanged. 
   * If the input values "ERROR" (case-insensitive), a BadRequestException is thrown to simulate a server-side error. 
   * The method also records the operation duration and enriches the current OpenTelemetry span with service and 
   * baggage information.
   * @param input the string whose characters are to be reversed
   * @return the reversed string, or the original value when the input is null or empty
   * @throws BadRequestException if inputvalues "ERROR"
   */
  public String reverse(String input) {
    long startTime = System.nanoTime();
    try {
      this.enrichCurrentSpanWithVersion();
      String frontendOp = Baggage.current().getEntryValue("operation");
      Span.current().addEvent("BaggageRead",
                              Attributes.builder().put("frontend-operation", frontendOp != null ? frontendOp : "<none>").
                                         build());
      
      // If input equals "ERROR" generate an Exception to demonstrate error capturing in observability tools
      if (input != null && "ERROR".compareToIgnoreCase(input) == 0)
        throw new BadRequestException("Input cannot be 'ERROR'");

      String result = input == null ? null : new StringBuilder(input).reverse().toString();
      if (replyTimeTooLong(true))
        Span.current().addEvent("stringReversed",
                                Attributes.builder().put("string2Reverse", input == null ? "<none>" : input).
                                           put("reversedString", result == null ? "<none>" : result).
                                           put("tooMuchSleepTime", true).
                                           build());
      LOGGER.log(System.Logger.Level.INFO,
                 () -> "{\"source\": \"backend\", \"original\": \"" + 
                       (input == null ? "<none>" : input) + 
                       "\", \"reversed\": \"" + (result == null ? "<none>" : result) + "\"}");
      return result;
    } finally {
            this.recordReplyTime("reverse", startTime);
    }
  }

  /** 
   * Returns the number of characters in the supplied string.
   * A null input is treated as an empty string and therefore the method returns zero.
   * The method records the operation duration and enriches the current OpenTelemetry span with the configured service version.
   * @param input the string whose length is to be returned
   * @return the number of characters in input, or zero if input is null
   */
  public int length(String input) {
    long startTime = System.nanoTime();
    try {
      this.enrichCurrentSpanWithVersion();
      String frontendOp = Baggage.current().getEntryValue("operation");
      Span.current().addEvent("BaggageRead",
                              Attributes.builder().put("frontend-operation", frontendOp != null ? frontendOp : "<none>").
                                         build());
      int result = input == null ? 0 : input.length();
      if (replyTimeTooLong(false))
        Span.current().addEvent("stringLength",
                             Attributes.builder().
                                        put("string2CalcLength", input == null ? "<none>" : input).
                                        put("StringLength", result).
                                        put("tooMuchSleepTime", true).
                                        build());
        LOGGER.log(System.Logger.Level.INFO,
                   () -> "{\"source\": \"backend\", \"original\": \"" + 
                         (input == null ? "<none>" : input) + "\", \"length\": \"" + result + "\"}");
      return result;
    } finally {
      this.recordReplyTime("length", startTime);
    }
  }

  /**
   * Adds the configured service version to the currently active * OpenTelemetry span. 
   * If there is no active span, the OpenTelemetry API handles the operation using the current span context.
   */
  void enrichCurrentSpanWithVersion() {
    Span.current().setAttribute("service.version", this.serviceVersion);
  }


  /**
   * Records the elapsed execution time of a StringUtils operation in the * OpenTelemetry reply-time histogram.
   * The recorded measurement includes the operation name and configured * service version as metric attributes.
   * @param function the name of the operation being measured 
   * @param startTime the start timestamp obtained from {@link System#nanoTime()}
   */
  void recordReplyTime(String function, long startTime) {
    long durationInMs = (System.nanoTime() - startTime) / 1_000_000;
    replyTimeHistogram.record(durationInMs, Attributes.builder().
                                                       put(FUNCTION_ATTRIBUTE, function).
                                                       put(SERVICE_VERSION_ATTRIBUTE, this.serviceVersion).
                                                       build());
  }
}