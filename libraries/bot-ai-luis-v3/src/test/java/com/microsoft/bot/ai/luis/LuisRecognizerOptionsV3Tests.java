package com.microsoft.bot.ai.luis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.microsoft.bot.builder.BotAdapter;
import com.microsoft.bot.builder.RecognizerResult;
import com.microsoft.bot.builder.TurnContext;
import com.microsoft.bot.builder.TurnContextImpl;
import com.microsoft.bot.schema.Activity;
import com.microsoft.bot.schema.ActivityTypes;
import com.microsoft.bot.schema.ConversationReference;
import com.microsoft.bot.schema.ResourceResponse;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;

public class LuisRecognizerOptionsV3Tests {

    @ParameterizedTest
    @ValueSource(strings = {
        "Composite1.json",
        "Composite2.json",
        "Composite3.json",
        "DateTimeReference.json",
        "DynamicListsAndList.json",
        "ExternalEntitiesAndBuiltin.json",
        "ExternalEntitiesAndComposite.json",
        "ExternalEntitiesAndList.json",
        "ExternalEntitiesAndRegex.json",
        "ExternalEntitiesAndSimple.json",
        "ExternalEntitiesAndSimpleOverride.json",
        "GeoPeopleOrdinal.json",
        "Minimal.json",
//        "MinimalWithGeo.json",
        "NoEntitiesInstanceTrue.json",
        "Patterns.json",
        "Prebuilt.json",
        "roles.json",
        "TraceActivity.json",
        "Typed.json",
        "TypedPrebuilt.json",
        "V1DatetimeResolution.json"
    }) // six numbers
    public void shouldParseLuisResponsesCorrectly(String fileName) {
        RecognizerResult  result = null, expected  = null;
        MockWebServer mockWebServer = new MockWebServer();

        try {
            // Get Oracle file
            String path = Paths.get("").toAbsolutePath().toString();
            File file = new File(path + "/src/test/java/com/microsoft/bot/ai/luis/testdata/" + fileName);
            String content = FileUtils.readFileToString(file, "utf-8");

            //Extract V3 response
            ObjectMapper mapper = new ObjectMapper();
            JsonNode testData = mapper.readTree(content);
            JsonNode v3SettingsAndResponse = testData.get("v3");
            JsonNode v3Response = v3SettingsAndResponse.get("response");


            //Extract V3 Test Settings
            JsonNode testSettings = v3SettingsAndResponse.get("options");

            // Set mock response in MockWebServer
            String mockResponse = mapper.writeValueAsString(v3Response);
            mockWebServer.enqueue(new MockResponse()
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .setBody(mockResponse));

            mockWebServer.start();
            String mockapplicationId = "b31aeaf3-3511-495b-a07f-571fc873214b";
            StringBuilder pathToMock = new StringBuilder("/luis/prediction/v3.0/apps/" + mockapplicationId);

            if (testSettings.get("Version") != null ) {
                pathToMock.append(String.format("/versions/%s/predict", testSettings.get("Version").asText()));
            } else {
                pathToMock.append(String.format("/slots/%s/predict", testSettings.get("Slot").asText()));
            }
            pathToMock.append(
                String.format(
                    "?verbose=%s&log=%s&show-all-intents=%s",
                    testSettings.get("IncludeInstanceData").asText(),
                    testSettings.get("Log").asText(),
                    testSettings.get("IncludeAllIntents").asText()
                )
            );

            HttpUrl baseUrl = mockWebServer.url(pathToMock.toString());
            String endpoint = String.format("http://localhost:%s", baseUrl.port());

            // Set LuisRecognizerOptions data
            ObjectReader readerDynamicList = mapper.readerFor(new TypeReference<List<DynamicList>>() {});
            ObjectReader readerExternalentities = mapper.readerFor(new TypeReference<List<ExternalEntity>>() {});
            LuisRecognizerOptionsV3 v3 = new LuisRecognizerOptionsV3(
                new LuisApplication(
                "b31aeaf3-3511-495b-a07f-571fc873214b",
                "b31aeaf3-3511-495b-a07f-571fc873214b",
                    endpoint)) {{
                        includeInstanceData = testSettings.get("IncludeInstanceData").asBoolean();
                        includeAllIntents = testSettings.get("IncludeAllIntents").asBoolean();
                        version = testSettings.get("Version") == null ? null : testSettings.get("Version").asText();
                        dynamicLists = testSettings.get("DynamicLists") == null ? null : readerDynamicList.readValue(testSettings.get("DynamicLists"));
                        externalEntities = testSettings.get("ExternalEntities") == null ? null : readerExternalentities.readValue(testSettings.get("ExternalEntities"));
            }};

            result = v3.recognizeInternal(createContext(testData.get("text").asText()), new OkHttpClient()).get();

            // Build expected result
            expected = mapper.readValue(content, RecognizerResult.class);
            Map<String, JsonNode> properties = expected.getProperties();
            properties.remove("v2");
            properties.remove("v3");

            assertEquals(mapper.writeValueAsString(expected), mapper.writeValueAsString(result));

            RecordedRequest request = mockWebServer.takeRequest();
            assertEquals(String.format("POST %s HTTP/1.1", pathToMock.toString()), request.getRequestLine());
            assertEquals(pathToMock.toString(), request.getPath());

        } catch (InterruptedException | ExecutionException | IOException e) {
            e.printStackTrace();
        } finally {
            try {
                mockWebServer.shutdown();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static TurnContext createContext(String message) {

        Activity activity = new Activity() {
            {
                setText(message);
                setType(ActivityTypes.MESSAGE);
                setChannelId("EmptyContext");
            }
        };

        return new TurnContextImpl(new NotImplementedAdapter(), activity);
    }

    private static class NotImplementedAdapter extends BotAdapter {
        @Override
        public CompletableFuture<ResourceResponse[]> sendActivities(
            TurnContext context,
            List<Activity> activities
        ) {
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<ResourceResponse> updateActivity(
            TurnContext context,
            Activity activity
        ) {
            throw new RuntimeException();
        }

        @Override
        public CompletableFuture<Void> deleteActivity(
            TurnContext context,
            ConversationReference reference
        ) {
            throw new RuntimeException();
        }
    }

}
