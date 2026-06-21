package com.example.coursesystem.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * AI服务 - 使用 MiMo API (Anthropic兼容格式)
 */
@Service
public class AIService {

    @Value("${mimo.api-key}")
    private String apiKey;

    @Value("${mimo.base-url:https://api.xiaomimimo.com}")
    private String baseUrl;

    @Value("${mimo.model:MiMo}")
    private String model;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private OkHttpClient getClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 非流式调用AI (Anthropic Messages API格式)
     */
    public String callAI(String systemPrompt, String userMessage) {
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", 2000);

            // 禁用thinking，加速响应
            ObjectNode thinking = body.putObject("thinking");
            thinking.put("type", "disabled");

            body.put("system", systemPrompt);

            // messages数组
            ArrayNode messages = body.putArray("messages");
            ObjectNode userMsg = messages.addObject();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);

            String jsonBody = objectMapper.writeValueAsString(body);
            System.out.println(">>> MiMo Request: " + jsonBody);

            RequestBody requestBody = RequestBody.create(
                    jsonBody,
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(baseUrl + "/anthropic/v1/messages")
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("Content-Type", "application/json")
                    .post(requestBody)
                    .build();

            try (Response response = getClient().newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    String errBody = response.body() != null ? response.body().string() : "unknown";
                    return "调用失败：HTTP " + response.code() + " - " + errBody;
                }
                String respBody = response.body().string();
                JsonNode json = objectMapper.readTree(respBody);
                JsonNode content = json.get("content");
                if (content != null && content.isArray()) {
                    // 跳过thinking块，取text块
                    for (JsonNode block : content) {
                        if ("text".equals(block.has("type") ? block.get("type").asText() : "text")) {
                            return block.get("text").asText();
                        }
                    }
                }
                return "生成失败：API返回结果为空";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "调用失败：" + e.getMessage();
        }
    }

    /**
     * 流式调用AI (Anthropic Messages SSE格式)
     */
    public Flux<String> callAIStream(String systemPrompt, String userMessage) {
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

        new Thread(() -> {
            try {
                ObjectNode body = objectMapper.createObjectNode();
                body.put("model", model);
                body.put("max_tokens", 2000);
                body.put("stream", true);

                // 禁用thinking，加速响应
                ObjectNode thinking = body.putObject("thinking");
                thinking.put("type", "disabled");

                body.put("system", systemPrompt);

                ArrayNode messages = body.putArray("messages");
                ObjectNode userMsg = messages.addObject();
                userMsg.put("role", "user");
                userMsg.put("content", userMessage);

                RequestBody requestBody = RequestBody.create(
                        objectMapper.writeValueAsString(body),
                        MediaType.parse("application/json")
                );

                Request request = new Request.Builder()
                        .url(baseUrl + "/anthropic/v1/messages")
                        .addHeader("x-api-key", apiKey)
                        .addHeader("anthropic-version", "2023-06-01")
                        .addHeader("Content-Type", "application/json")
                        .post(requestBody)
                        .build();

                EventSource.Factory factory = EventSources.createFactory(getClient());

                factory.newEventSource(request, new EventSourceListener() {
                    @Override
                    public void onEvent(EventSource eventSource, String id, String type, String data) {
                        try {
                            if ("[DONE]".equals(data)) {
                                sink.tryEmitComplete();
                                return;
                            }
                            JsonNode json = objectMapper.readTree(data);
                            String eventType = json.has("type") ? json.get("type").asText() : "";

                            if ("content_block_delta".equals(eventType)) {
                                JsonNode delta = json.get("delta");
                                if (delta != null) {
                                    String deltaType = delta.has("type") ? delta.get("type").asText() : "";
                                    // text_delta: 输出文本
                                    if ("text_delta".equals(deltaType) && delta.has("text")) {
                                        sink.tryEmitNext(delta.get("text").asText());
                                    }
                                    // thinking_delta: 跳过（MiMo的思考过程）
                                }
                            }
                            if ("message_stop".equals(eventType)) {
                                sink.tryEmitComplete();
                            }
                        } catch (Exception e) {
                            // 忽略解析错误
                        }
                    }

                    @Override
                    public void onFailure(EventSource eventSource, Throwable t, Response response) {
                        if (t != null) {
                            sink.tryEmitError(t);
                        } else {
                            sink.tryEmitComplete();
                        }
                    }

                    @Override
                    public void onClosed(EventSource eventSource) {
                        sink.tryEmitComplete();
                    }
                });

            } catch (Exception e) {
                sink.tryEmitError(e);
            }
        }).start();

        return sink.asFlux();
    }
}
