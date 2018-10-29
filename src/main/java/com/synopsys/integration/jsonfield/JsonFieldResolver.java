package com.synopsys.integration.jsonfield;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class JsonFieldResolver {
    private final Gson gson;

    public JsonFieldResolver(final Gson gson) {
        this.gson = gson;
    }

    public <T> List<T> resolveValues(final String json, final HierarchicalField jsonField) {
        final JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
        final List<JsonElement> foundElements = getElements(jsonObject, jsonField.getFieldPath());

        final List<T> convertedObjects = new ArrayList<>();
        for (final JsonElement element : foundElements) {
            final T convertedElement = gson.fromJson(element, jsonField.getType());
            convertedObjects.add(convertedElement);
        }

        return convertedObjects;
    }

    private List<JsonElement> getElements(final JsonElement element, final List<String> fieldPath) {
        JsonElement currentElement = element;
        int currentPathIndex = 0;

        while (currentPathIndex < fieldPath.size()) {
            final String key = fieldPath.get(currentPathIndex);

            if (currentElement.isJsonObject()) {
                final JsonObject jsonObject = currentElement.getAsJsonObject();
                currentElement = jsonObject.get(key);
                currentPathIndex++;
            } else if (currentElement.isJsonArray()) {
                final JsonArray foundArray = currentElement.getAsJsonArray();
                final List<JsonElement> foundValues = new ArrayList<>(foundArray.size());
                for (final JsonElement arrayElement : foundArray) {
                    foundValues.addAll(getElements(arrayElement, fieldPath.subList(currentPathIndex, fieldPath.size())));
                }
                return foundValues;
            } else {
                return Arrays.asList(currentElement);
            }
        }

        return Arrays.asList(currentElement);
    }

}
