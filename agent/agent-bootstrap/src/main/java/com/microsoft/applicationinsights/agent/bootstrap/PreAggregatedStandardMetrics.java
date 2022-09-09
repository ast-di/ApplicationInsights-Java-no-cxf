// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.applicationinsights.agent.bootstrap;

import static io.opentelemetry.instrumentation.api.instrumenter.BootstrapSemanticAttributes.IS_PRE_AGGREGATED;
import static io.opentelemetry.instrumentation.api.instrumenter.BootstrapSemanticAttributes.IS_SYNTHETIC;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.BootstrapSemanticAttributes;
import io.opentelemetry.instrumentation.api.instrumenter.UserAgents;
import javax.annotation.Nullable;

public class PreAggregatedStandardMetrics {

  @Nullable private static volatile AttributeGetter attributeGetter;

  public static void setAttributeGetter(AttributeGetter attributeGetter) {
    PreAggregatedStandardMetrics.attributeGetter = attributeGetter;
  }

  public static void applyHttpClientView(
      AttributesBuilder builder,
      Context context,
      Attributes startAttributes,
      Attributes endAttributes) {

    Span span = Span.fromContext(context);
    applyCommon(builder, span);
  }

  public static void applyHttpServerView(
      AttributesBuilder builder,
      Context context,
      Attributes startAttributes,
      Attributes endAttributes) {

    Span span = Span.fromContext(context);
    applyCommon(builder, span);

    // is_synthetic is only applied to server requests
    builder.put(IS_SYNTHETIC, UserAgents.isBot(endAttributes, startAttributes));
  }

  public static void applyRpcClientView(
      AttributesBuilder builder,
      Context context,
      Attributes startAttributes,
      Attributes endAttributes) {

    applyHttpClientView(builder, context, startAttributes, endAttributes);
  }

  public static void applyRpcServerView(
      AttributesBuilder builder,
      Context context,
      Attributes startAttributes,
      Attributes endAttributes) {

    applyHttpServerView(builder, context, startAttributes, endAttributes);
  }

  private static void applyCommon(AttributesBuilder builder, Span span) {

    // this is needed for detecting telemetry signals that will trigger pre-aggregated metrics via
    // auto instrumentations
    span.setAttribute(IS_PRE_AGGREGATED, true);

    if (attributeGetter == null) {
      return;
    }
    String connectionString =
        attributeGetter.get(span, BootstrapSemanticAttributes.CONNECTION_STRING);
    if (connectionString != null) {
      builder.put(BootstrapSemanticAttributes.CONNECTION_STRING, connectionString);
    } else {
      // back compat support
      String instrumentationKey =
          attributeGetter.get(span, BootstrapSemanticAttributes.INSTRUMENTATION_KEY);
      if (instrumentationKey != null) {
        builder.put(BootstrapSemanticAttributes.INSTRUMENTATION_KEY, instrumentationKey);
      }
    }
    String roleName = attributeGetter.get(span, BootstrapSemanticAttributes.ROLE_NAME);
    if (roleName != null) {
      builder.put(BootstrapSemanticAttributes.ROLE_NAME, roleName);
    }
  }

  @FunctionalInterface
  public interface AttributeGetter {
    <T> T get(Span span, AttributeKey<T> key);
  }

  private PreAggregatedStandardMetrics() {}
}