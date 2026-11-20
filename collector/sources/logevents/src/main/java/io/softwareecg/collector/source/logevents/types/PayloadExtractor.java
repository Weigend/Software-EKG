/*
 * ______________________________________________________________________________
 *
 * Project: Software EKG
 * ______________________________________________________________________________
 *
 * created by: f.lautenschlager
 * creation date: 19.03.14 09:06
 * description: 
 * ______________________________________________________________________________
 *
 * Copyright: (c) Weigend AM, all rights reserved
 * ______________________________________________________________________________
 */
package io.softwareecg.collector.source.logevents.types;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public class PayloadExtractor {


    /**
     * The pattern to extract the names of the payload groups
     */
    private final Pattern payloadPattern = Pattern.compile("xx([A-Za-z]*)xx");

    /**
     * Extracts all payload groups from the log pattern.
     * A payload group is defined within a leading "xx" and an ending "xx".
     * Example: xxENDDATExx or xxUSERIDxx
     *
     * @param logPattern log pattern
     * @return a set with payload property keys
     */
    public Set<String> extractPayloadGroups(String logPattern) {
        Set<String> payloadGroups = new HashSet<>();

        Matcher m = payloadPattern.matcher(logPattern);
        while (m.find()) {
            payloadGroups.add(m.group());
        }
        return payloadGroups;
    }

    /**
     * Extracts the payload properties to a map of properties
     *
     * @param line line content
     * @param logPattern - the log pattern
     * @return a map with the additional properties
     */
    public Map<String, String> extractPayloadProperties(String line, Pattern logPattern) {
        Matcher matcher = logPattern.matcher(line);

        if (!matcher.matches()) {
            return new HashMap<>();
        }

        try {
            Map<String, String> properties = new HashMap<>();
            Set<String> groupNames = extractPayloadGroups(logPattern.toString());

            for (String groupName : groupNames) {
                String groupValue = matcher.group(groupName);
                //If the group value is null, we just put "null" into our properties
                groupValue = groupValue != null ? groupValue : "null";

                properties.put(groupName, groupValue);
            }
            return properties;

        } catch (IllegalStateException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Could not set the payload due to an exception.", e);
        }

    }
}
